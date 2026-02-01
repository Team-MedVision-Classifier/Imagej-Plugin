#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$BASE_DIR"

PYTHON_BIN="python3"

if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
  echo "Error: python3 not found. Install Python 3.x first." >&2
  exit 1
fi

# Create venv for Cellpose 3.1 if missing
if [ ! -d "venv_v3" ]; then
  "$PYTHON_BIN" -m venv venv_v3
fi

# Install dependencies for v3
source venv_v3/bin/activate
pip install --upgrade pip
pip install "cellpose==3.1.1.2" "numpy<2" opencv-python-headless
pip check || true
deactivate

# Create venv for Cellpose SAM if missing
if [ ! -d "venv_v4" ]; then
  "$PYTHON_BIN" -m venv venv_v4
fi

# Install dependencies for v4
source venv_v4/bin/activate
pip install --upgrade pip
pip install cellpose "numpy<2" opencv-python-headless fastapi[standard] uvicorn python-multipart
pip check || true
deactivate

# Run backend
exec ./venv_v4/bin/uvicorn app:app --host 0.0.0.0 --port 8000
