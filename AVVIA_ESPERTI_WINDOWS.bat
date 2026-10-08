@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title SwiftBAT Explorer 1.3.0 - Windows

echo ============================================================
echo   SwiftBAT Explorer 1.3.0 - Avvio per Windows
echo ============================================================
echo.
if not exist "%~dp0AVVIA_ESPERTI_WINDOWS.ps1" (
  echo [ERRORE] Manca AVVIA_ESPERTI_WINDOWS.ps1 nella cartella.
  pause
  exit /b 1
)
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0AVVIA_ESPERTI_WINDOWS.ps1"
set "RESULT=%ERRORLEVEL%"
if not "%RESULT%"=="0" (
  echo.
  echo [ERRORE] Avvio non riuscito. Consulta il log indicato sopra.
  pause
)
exit /b %RESULT%
