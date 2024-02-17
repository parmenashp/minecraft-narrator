@echo off
call setup_windows.bat
.\python\python.exe -m poetry run uvicorn "src.main:app" --port 5000
pause
