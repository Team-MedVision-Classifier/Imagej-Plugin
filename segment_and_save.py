import argparse
import shutil
from pathlib import Path

import numpy as np
import requests
import tifffile
from roifile import ImagejRoi, ROI_TYPE, ROI_OPTIONS, roiwrite


def parse_roi_lines(text: str):
    rois = []
    for idx, line in enumerate(text.splitlines(), start=1):
        line = line.strip()
        if not line:
            continue
        parts = [p.strip() for p in line.split(",") if p.strip()]
        if len(parts) < 4 or len(parts) % 2 != 0:
            continue
        points = []
        for i in range(0, len(parts), 2):
            try:
                x = float(parts[i])
                y = float(parts[i + 1])
                points.append([x, y])
            except ValueError:
                points = []
                break
        if not points:
            continue
        roi = ImagejRoi.frompoints(points)
        roi.roitype = ROI_TYPE.POLYGON
        roi.name = f"roi_{idx:04d}"
        roi.options &= ~ROI_OPTIONS.SHOW_LABELS
        roi.options &= ~ROI_OPTIONS.OVERLAY_LABELS
        roi.options &= ~ROI_OPTIONS.OVERLAY_NAMES
        rois.append(roi)
    return rois


def _mask_to_lines_with_cellpose(mask: np.ndarray):
    try:
        from cellpose import utils
    except Exception:
        return None

    outlines = utils.outlines_list(mask)
    lines = []
    for outline in outlines:
        roi_coords = ",".join([f"{p[0]},{p[1]}" for p in outline])
        lines.append(roi_coords)
    return "\n".join(lines)


def _mask_to_lines_with_cv2(mask: np.ndarray):
    try:
        import cv2
    except Exception:
        return ""

    lines = []
    labels = np.unique(mask)
    labels = labels[labels != 0]
    for label in labels:
        binary = (mask == label).astype(np.uint8)
        if not isinstance(binary, np.ndarray):
            binary = np.asarray(binary, dtype=np.uint8)
        contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
        if not contours:
            continue
        contour = max(contours, key=cv2.contourArea)
        if len(contour) < 3:
            continue
        points = contour[:, 0, :]
        roi_coords = ",".join([f"{p[0]},{p[1]}" for p in points])
        lines.append(roi_coords)
    return "\n".join(lines)


def convert_npy_to_server_format(npy_path: Path):
    mask = np.load(npy_path, allow_pickle=True)
    if isinstance(mask, np.ndarray) and mask.dtype == object:
        if mask.shape == ():
            mask = mask.item()

    if isinstance(mask, dict):
        if "masks" in mask:
            mask = mask["masks"]
        else:
            return ""

    if isinstance(mask, (list, tuple)):
        if len(mask) == 0:
            return ""
        mask = mask[0]

    mask = np.asarray(mask)
    mask = np.squeeze(mask)
    if mask.ndim > 2:
        mask = mask[0]

    text = _mask_to_lines_with_cellpose(mask)
    if text is None:
        text = _mask_to_lines_with_cv2(mask)
    return text


def call_segment_api(api_url: str, image_path: Path, model_type: str, model_name: str):
    params = {
        "model_type": model_type,
        "model_name": model_name,
        "channels": "0",
        "diameter": 85,
        "use_gpu": True,
        "batch_size": 8,
        "resample": True,
        "flow_threshold": 0.4,
        "cellprob_threshold": 0,
        "normalize": True,
        "tile_norm": 0,
        "percentile_low": 1,
        "percentile_high": 99,
    }
    with image_path.open("rb") as f:
        files = {"image": (image_path.name, f, "image/tiff")}
        response = requests.post(api_url, params=params, files=files, timeout=600)
    response.raise_for_status()
    return response.text


def save_with_overlays(out_path: Path, image, rois, axes: str | None, ijmeta: dict | None, resolution):
    metadata = dict(ijmeta or {})
    if rois:
        metadata["Overlays"] = [r.tobytes() for r in rois]

    write_kwargs = {
        "imagej": True,
        "metadata": metadata,
    }
    if axes:
        write_kwargs["metadata"]["axes"] = axes
    if resolution:
        write_kwargs["resolution"] = resolution

    tifffile.imwrite(out_path, image, **write_kwargs)


def save_rois_zip(out_dir: Path, base_name: str, rois, suffix: str):
    if not rois:
        return
    zip_path = out_dir / f"{base_name}_{suffix}.zip"
    roiwrite(zip_path, rois, mode="w")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--api-url",
        default="http://127.0.0.1:8000/segment",
        help="Cellpose API endpoint",
    )
    parser.add_argument(
        "--image",
        default=None,
        help="Optional single image filename in project root",
    )
    args = parser.parse_args()

    base_dir = Path(__file__).resolve().parent
    outputs_dir = base_dir / "outputs"
    # Map output folders to (model_type, model_name) tuples
    targets = {
        "ddq": ("Cellpose3.1", "ddq_model"),
        "sam": ("CellposeSAM", "cpsam"),
        "samfinetuned": ("CellposeSAM", "cellpose_sam_custom"),
    }

    if args.image:
        image_paths = [base_dir / args.image]
    else:
        image_paths = sorted(base_dir.glob("*.tif"))

    if not image_paths:
        raise FileNotFoundError("No .tif files found in project root")

    for image_path in image_paths:
        if not image_path.exists():
            raise FileNotFoundError(f"Image not found: {image_path}")

        file_root = image_path.stem
        file_output_dir = outputs_dir / file_root

        for folder in list(targets.keys()) + ["original"]:
            (file_output_dir / folder).mkdir(parents=True, exist_ok=True)

        shutil.copy2(image_path, file_output_dir / "original" / image_path.name)

        with tifffile.TiffFile(image_path) as tif:
            series = tif.series[0]
            image = series.asarray()
            axes = series.axes
            ijmeta = tif.imagej_metadata or {}
            resolution = tif.pages[0].resolution if tif.pages else None

        for folder, (model_type, model_name) in targets.items():
            roi_text = call_segment_api(args.api_url, image_path, model_type, model_name)
            rois = parse_roi_lines(roi_text)
            out_path = file_output_dir / folder / image_path.name
            save_with_overlays(out_path, image, rois, axes, ijmeta, resolution)
            save_rois_zip(file_output_dir / folder, file_root, rois, "rois")

        seg_path = image_path.with_name(f"{file_root}_seg.npy")
        if seg_path.exists():
            gt_dir = file_output_dir / "groundtruths"
            gt_dir.mkdir(parents=True, exist_ok=True)

            gt_text = convert_npy_to_server_format(seg_path)
            gt_text_path = gt_dir / f"{file_root}_groundtruth.txt"
            gt_text_path.write_text(gt_text)

            gt_rois = parse_roi_lines(gt_text)
            gt_overlay_path = gt_dir / f"{file_root}_groundtruth.tif"
            save_with_overlays(gt_overlay_path, image, gt_rois, axes, ijmeta, resolution)
            save_rois_zip(gt_dir, file_root, gt_rois, "groundtruth_rois")


if __name__ == "__main__":
    main()
