@echo off
setlocal EnableExtensions
title SwiftBAT Explorer - Creazione pacchetti macOS e Windows
echo ============================================================
echo  SwiftBAT Explorer - Generazione ZIP macOS e Windows
echo ============================================================
echo.
if not exist "%~dp0CREA_PACCHETTI_DISTRIBUZIONE_WINDOWS.ps1" (
  echo [ERRORE] Script Windows di distribuzione mancante.
  pause
  exit /b 1
)
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0CREA_PACCHETTI_DISTRIBUZIONE_WINDOWS.ps1"
set "RESULT=%ERRORLEVEL%"
echo.
if "%RESULT%"=="0" (
  echo [OK] Trovi entrambi gli ZIP nella cartella dist.
) else (
  echo [ERRORE] Creazione dei pacchetti non riuscita. Codice: %RESULT%
)
echo.
pause
exit /b %RESULT%
