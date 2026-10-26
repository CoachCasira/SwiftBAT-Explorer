@echo off
setlocal
echo SwiftBAT Explorer - diagnostica Population Analysis
echo Avvia SwiftBAT Explorer, apri Population Analysis e poi premi un tasto.
pause >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0DIAGNOSTICA_POPULATION_WINDOWS.ps1"
echo.
pause
