# Use Python 3.10 slim as base
FROM python:3.10-slim

WORKDIR /app

# 1. Install System Dependencies (OpenCV requires libgl1)
RUN apt-get update && apt-get install -y \
    libgl1 libglib2.0-0 git \
    && rm -rf /var/lib/apt/lists/*

# 2. Setup Model Directory
RUN mkdir -p /app/models/custom && chmod -R 777 /app/models
ENV CELLPOSE_LOCAL_MODELS_PATH="/app/models"

# 3. Create Virtual Environment for Cellpose 3 (V3)
# Pinning to 3.1.1.2 for legacy/stable support
RUN python -m venv /app/venv_v3
RUN /app/venv_v3/bin/pip install "cellpose==3.1.1.2" "numpy<2" opencv-python-headless

# 4. Create Virtual Environment for Cellpose 4 (SAM)
# Installs latest cellpose
RUN python -m venv /app/venv_v4
RUN /app/venv_v4/bin/pip install cellpose "numpy<2" opencv-python-headless

# 5. Install API Dependencies (into v4 env, which runs the server)
COPY requirements.txt .
# We enforce numpy<2 here again to ensure requirements.txt doesn't upgrade it
RUN /app/venv_v4/bin/pip install -r requirements.txt "numpy<2"

# 6. Copy Custom Models
# Ensure these files are in your folder before building!
COPY ddq_model /app/models/custom/ddq_model
COPY cellpose_sam_custom /app/models/custom/cellpose_sam_custom

# 7. Copy Application Scripts
COPY download_model.py .
COPY worker.py .
COPY app.py .

# 8. Pre-download Base Models (Runs in both envs to cache correctly)
RUN /app/venv_v3/bin/python download_model.py
RUN /app/venv_v4/bin/python download_model.py

# 9. Start Server
CMD ["/app/venv_v4/bin/uvicorn", "app:app", "--host", "0.0.0.0", "--port", "7860"]