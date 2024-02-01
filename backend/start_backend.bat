@echo off
call setup_windows.bat
start python/python.exe -m poetry run python run.py
pause
