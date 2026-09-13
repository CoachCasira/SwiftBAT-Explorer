@echo off
setlocal EnableExtensions

set "MAVEN_VERSION=3.9.16"
set "WRAPPER_BASE=%USERPROFILE%\.m2\wrapper\dists\swiftbat-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%WRAPPER_BASE%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
set "MAVEN_ZIP=%WRAPPER_BASE%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://dlcdn.apache.org/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"

if exist "%MAVEN_CMD%" goto RUN_MAVEN

echo [INFO] Maven %MAVEN_VERSION% non trovato. Download iniziale in corso...
if not exist "%WRAPPER_BASE%" mkdir "%WRAPPER_BASE%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue';" ^
  "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%';" ^
  "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%WRAPPER_BASE%' -Force;" ^
  "Remove-Item '%MAVEN_ZIP%' -Force"

if errorlevel 1 (
    echo [ERRORE] Download o estrazione di Maven non riusciti.
    exit /b 1
)

:RUN_MAVEN
call "%MAVEN_CMD%" %*
exit /b %ERRORLEVEL%
