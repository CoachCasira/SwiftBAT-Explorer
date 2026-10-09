#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="$SCRIPT_DIR/dist"
PACKAGE_NAME="SwiftBAT-Explorer-Relatori-Windows"
ZIP_FILE="$DIST_DIR/$PACKAGE_NAME.zip"
SHA_FILE="$DIST_DIR/$PACKAGE_NAME.sha256.txt"

if [ "$(uname -s)" != "Darwin" ]; then
  echo "[ERRORE] Il pacchetto Windows viene preparato da macOS con ditto."
  exit 2
fi
for tool in ditto shasum awk rsync; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "[ERRORE] Manca il comando necessario: $tool"
    exit 3
  fi
done
for path in "AVVIA_ESPERTI_WINDOWS.ps1" "pom.xml" "src/main/java"; do
  if [ ! -e "$SCRIPT_DIR/$path" ]; then
    echo "[ERRORE] Pacchetto incompleto: manca $path"
    exit 4
  fi
done

tmp_dir="$(mktemp -d "/tmp/swiftbat-windows-package.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT
package_dir="$tmp_dir/SwiftBAT Explorer"
app_dir="$package_dir/Applicazione"
mkdir -p "$app_dir" "$DIST_DIR"

echo "Creo lo ZIP Windows per i relatori..."
rsync -a --exclude='/.git/' --exclude='/.github/' --exclude='/.swiftbat-runtime/' \
  --exclude='/target/' --exclude='/dist/' --exclude='/.idea/' \
  --exclude='/.vscode/' --exclude='.DS_Store' "$SCRIPT_DIR/" "$app_dir/"
rm -rf "$app_dir/.git" "$app_dir/.github" "$app_dir/.swiftbat-runtime" "$app_dir/target" \
  "$app_dir/dist" "$app_dir/.idea" "$app_dir/.vscode"
rm -f "$app_dir/.DS_Store" "$app_dir/AVVIA_APP.bat" \
  "$app_dir/AVVIA_APP_MAC.command" "$app_dir/AVVIA_ESPERTI_MAC.command" "$app_dir/AVVIA_ESPERTI_WINDOWS.bat" \
  "$app_dir/CREA_APP_MAC.command" "$app_dir/CREA_PACCHETTO_ESPERTI_MAC.command" \
  "$app_dir/CREA_PACCHETTO_RELATORI_WINDOWS_MAC.command" \
  "$app_dir/CREA_PACCHETTI_DISTRIBUZIONE_MAC.command" \
  "$app_dir/CREA_PACCHETTI_DISTRIBUZIONE_WINDOWS.bat" \
  "$app_dir/CREA_PACCHETTI_DISTRIBUZIONE_WINDOWS.ps1" \
  "$app_dir/ISTRUZIONI_ESPERTI_MAC.md" \
  "$app_dir/BUILD_ESEGUIBILI.md"

# Tutto il codice e' nella cartella Applicazione; un solo .bat all'esterno.
cat > "$tmp_dir/launcher-lf.bat" <<'LAUNCHER'
@echo off
setlocal EnableExtensions
title SwiftBAT Explorer 1.3.0 - Windows
if not exist "%~dp0Applicazione\AVVIA_ESPERTI_WINDOWS.ps1" (
  echo [ERRORE] Manca la cartella Applicazione o il launcher interno.
  pause
  exit /b 1
)
cd /d "%~dp0Applicazione"
if errorlevel 1 (
  echo [ERRORE] Non riesco ad aprire la cartella Applicazione.
  pause
  exit /b 1
)
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0Applicazione\AVVIA_ESPERTI_WINDOWS.ps1" %*
set "RESULT=%ERRORLEVEL%"
if not "%RESULT%"=="0" (
  echo.
  echo [ERRORE] Avvio non riuscito. Consulta il log nella cartella Applicazione.
  pause
)
exit /b %RESULT%
LAUNCHER
awk '{sub(/\r$/, ""); printf "%s\r\n", $0}' \
  "$tmp_dir/launcher-lf.bat" > "$package_dir/AVVIA_SWIFTBAT.bat"

rm -f "$ZIP_FILE" "$SHA_FILE"
ditto -c -k --norsrc --keepParent "$package_dir" "$ZIP_FILE"
(
  cd "$DIST_DIR"
  shasum -a 256 "$PACKAGE_NAME.zip" > "$PACKAGE_NAME.sha256.txt"
)

echo
echo "Pacchetto Windows creato:"
echo "  $ZIP_FILE"
echo "Checksum:"
echo "  $SHA_FILE"
echo "Il relatore estrae lo ZIP e apre AVVIA_SWIFTBAT.bat."
