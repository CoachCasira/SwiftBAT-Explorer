@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title SwiftBAT Explorer - Avvio

echo ============================================================
echo   SwiftBAT Explorer 1.2.0 - Event Horizon
echo ============================================================
echo.
echo Verifica Java...
java --version
if errorlevel 1 (
    echo.
    echo [ERRORE] Java non e' disponibile nel PATH.
    echo Installare o configurare Java 17 e riprovare.
    pause
    exit /b 1
)

echo.
echo Connessione Internet necessaria:
echo - al primo avvio per Maven e le dipendenze;
echo - durante l'uso per il catalogo e i dati Swift/BAT.
echo.
call "%~dp0mvnw.cmd" -DskipTests javafx:run
if errorlevel 1 (
    echo.
    echo [ERRORE] L'applicazione non e' stata avviata correttamente.
    echo Copiare l'intero messaggio del terminale per la diagnosi.
)
pause
