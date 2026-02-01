@echo off
setlocal enabledelayedexpansion

set "BASE_DIR=%~dp0"
cd /d "%BASE_DIR%"

where python >nul 2>nul
if errorlevel 1 (
  echo Error: python not found. Install Python 3.x first.
  exit /b 1
)

if not exist "venv_v3\Scripts\python.exe" (
  python -m venv venv_v3
)

venv_v3\Scripts\python -m pip install --upgrade pip
venv_v3\Scripts\pip install "cellpose==3.1.1.2" "numpy<2" opencv-python-headless

if not exist "venv_v4\Scripts\python.exe" (
  python -m venv venv_v4
)

venv_v4\Scripts\python -m pip install --upgrade pip
venv_v4\Scripts\pip install cellpose "numpy<2" opencv-python-headless fastapi[standard] uvicorn python-multipart

venv_v4\Scripts\uvicorn app:app --host 0.0.0.0 --port 8000
