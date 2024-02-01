@echo off
call setup_windows.bat
.\python\python.exe -m poetry run python standalone_test.py
pause
