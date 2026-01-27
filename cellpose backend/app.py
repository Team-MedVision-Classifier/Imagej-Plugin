from fastapi import FastAPI, UploadFile, File, Response, Query
import subprocess
import tempfile
import os
import json
import logging
import sys
from pathlib import Path

# Setup Main Logger
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("CellposeAPI")

app = FastAPI()

# --- DYNAMIC PATH CONFIGURATION ---
BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models"
CELLPOSE_31_DIR = MODELS_DIR / "Cellpose 3.1"
CELLPOSE_SAM_DIR = MODELS_DIR / "CellposeSAM"

is_windows = sys.platform.startswith("win")
venv_bin = "Scripts" if is_windows else "bin"
python_exec = "python.exe" if is_windows else "python"

VENV_V3 = BASE_DIR / "venv_v3" / venv_bin / python_exec
VENV_V4 = BASE_DIR / "venv_v4" / venv_bin / python_exec

# Map model types to their respective virtual environments
ENV_MAPPING = {
    "Cellpose3.1": str(VENV_V3),
    "CellposeSAM": str(VENV_V4)
}


@app.get("/getModels")
async def get_models():
    """
    Retrieve available models organized by category.
    Returns models from Cellpose 3.1 and CellposeSAM directories.
    """
    models_response = {
        "Cellpose3.1": [],
        "CellposeSAM": []
    }
    
    # Scan Cellpose 3.1 directory
    if CELLPOSE_31_DIR.exists():
        try:
            for item in CELLPOSE_31_DIR.iterdir():
                if item.is_file() or item.is_dir():
                    models_response["Cellpose3.1"].append(item.name)
        except Exception as e:
            logger.error(f"Error scanning Cellpose 3.1 directory: {e}")
    
    # Scan CellposeSAM directory
    if CELLPOSE_SAM_DIR.exists():
        try:
            for item in CELLPOSE_SAM_DIR.iterdir():
                if item.is_file() or item.is_dir():
                    models_response["CellposeSAM"].append(item.name)
        except Exception as e:
            logger.error(f"Error scanning CellposeSAM directory: {e}")
    
    return models_response


@app.post("/segment")
async def segment(
        image: UploadFile = File(...),
        model_type: str = Query(..., enum=["Cellpose3.1", "CellposeSAM"]),
        model_name: str = Query(...),
        diameter: float = 0.0,
        channels: str = "0,0",
        use_gpu: bool = Query(False),
        batch_size: int = Query(64),
        resample: bool = Query(False),
        normalize: bool = Query(True),
        flow_threshold: float = Query(0.4),
        cellprob_threshold: float = Query(0.0),
        percentile_low: float = Query(1.0),
        percentile_high: float = Query(99.0),
        tile_norm: int = Query(0)
):
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tmp:
        contents = await image.read()
        tmp.write(contents)
        tmp_path = tmp.name

    try:
        python_exec = ENV_MAPPING.get(model_type)
        if not python_exec:
            return Response("Server Error: Model misconfigured.", status_code=500)

        logger.info(f"🚀 Dispatching {model_type}/{model_name} job...")

        worker_path = os.path.join(os.path.dirname(__file__), "worker.py")

        cmd = [
            python_exec, worker_path,
            "--image", tmp_path,
            "--model_type", model_type,
            "--model_name", model_name,
            "--diameter", str(diameter),
            "--channels", channels,
            "--batch_size", str(batch_size),
            "--flow_threshold", str(flow_threshold),
            "--cellprob_threshold", str(cellprob_threshold)
        ]
        if use_gpu:
            cmd.append("--use_gpu")
        if resample:
            cmd.append("--resample")
        if not normalize:
            cmd.append("--no_normalize")
        else:
            cmd.extend(["--percentile_low", str(percentile_low)])
            cmd.extend(["--percentile_high", str(percentile_high)])
            cmd.extend(["--tile_norm", str(tile_norm)])
        result = subprocess.check_output(cmd, timeout=600)

        output_json = json.loads(result.decode("utf-8"))

        if output_json["status"] == "success":
            return Response(content=output_json["data"], media_type="text/plain")
        else:
            return Response(content=output_json["message"], status_code=500)

    except subprocess.TimeoutExpired:
        return Response("Processing timed out.", status_code=504)

    except subprocess.CalledProcessError as e:
        # If the worker crashes, the error logs will have already printed to the console
        # because we are not capturing them anymore.
        logger.error(f"Worker crashed with return code {e.returncode}")
        return Response("Internal Worker Error", status_code=500)

    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)