@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title SwiftBAT Explorer - Avvio

echo ============================================================
echo   SwiftBAT Explorer 1.2.0 - Event Horizon
echo ============================================================
echo.
echo Verifica Java...
where java >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERRORE] Java non e' disponibile nel PATH.
    echo Installare un JDK 17 e riprovare.
    pause
    exit /b 1
)

set "JAVA_VERSION="
for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do if not defined JAVA_VERSION set "JAVA_VERSION=%%~V"

if not defined JAVA_VERSION (
    echo.
    echo [ERRORE] Non riesco a determinare la versione di Java installata.
    echo Installare un JDK 17 e riprovare.
    pause
    exit /b 1
)

for /f "tokens=1,2 delims=." %%A in ("!JAVA_VERSION!") do (
    set "JAVA_MAJOR=%%A"
    if "%%A"=="1" set "JAVA_MAJOR=%%B"
)

echo Java rilevato: !JAVA_VERSION!
if !JAVA_MAJOR! LSS 17 (
    echo.
    echo [ERRORE] La versione installata e' troppo vecchia: Java !JAVA_VERSION!.
    echo SwiftBAT Explorer richiede il JDK 17 o successivo.
    pause
    exit /b 1
)

where javac >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERRORE] E' presente solo il runtime Java, ma manca il compilatore javac.
    echo Installare il JDK 17 completo e riprovare.
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
