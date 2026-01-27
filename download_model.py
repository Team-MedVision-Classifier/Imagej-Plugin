import os
import sys

# Force cache path
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(BASE_DIR, "models")
CUSTOM_MODEL_DIR = os.path.join(MODELS_DIR, "custom")

# Tell Cellpose where to look
os.environ["CELLPOSE_LOCAL_MODELS_PATH"] = MODELS_DIR

try:
    import cellpose
    version = cellpose.__version__
    print(f"🔍 Running in Cellpose v{version}")
    from cellpose import models

    # V3 Logic
    if version.startswith("3."):
        print("⏳ Downloading Cellpose 3 (cyto3)...")
        models.Cellpose(gpu=False, model_type='cyto3')

    # V4 Logic (SAM)
    else:
        print("⏳ Downloading Cellpose-SAM (cpsam)...")
        models.CellposeModel(gpu=False, model_type='cpsam')

    print("✅ Download complete!")

except Exception as e:
    print(f"❌ Error: {e}")
    sys.exit(1)