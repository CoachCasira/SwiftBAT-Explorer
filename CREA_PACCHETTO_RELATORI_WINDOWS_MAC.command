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
for tool in ditto shasum awk; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "[ERRORE] Manca il comando necessario: $tool"
    exit 3
  fi
done
for path in "AVVIA_ESPERTI_WINDOWS.bat" "AVVIA_ESPERTI_WINDOWS.ps1" "pom.xml" "src/main/java"; do
  if [ ! -e "$SCRIPT_DIR/$path" ]; then
    echo "[ERRORE] Pacchetto incompleto: manca $path"
    exit 4
  fi
done

tmp_dir="$(mktemp -d "/tmp/swiftbat-windows-package.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT
package_dir="$tmp_dir/SwiftBAT Explorer"
mkdir -p "$DIST_DIR"

echo "Creo lo ZIP Windows per i relatori..."
ditto --norsrc "$SCRIPT_DIR" "$package_dir"
rm -rf "$package_dir/.git" "$package_dir/.github" "$package_dir/target" \
  "$package_dir/dist" "$package_dir/.idea" "$package_dir/.vscode"
rm -f "$package_dir/.DS_Store" "$package_dir/AVVIA_APP.bat" \
  "$package_dir/AVVIA_APP_MAC.command" "$package_dir/AVVIA_ESPERTI_MAC.command" \
  "$package_dir/CREA_APP_MAC.command" "$package_dir/CREA_PACCHETTO_ESPERTI_MAC.command" \
  "$package_dir/CREA_PACCHETTO_RELATORI_WINDOWS_MAC.command" \
  "$package_dir/CREA_PACCHETTI_DISTRIBUZIONE_MAC.command" \
  "$package_dir/ISTRUZIONI_ESPERTI_MAC.md" \
  "$package_dir/BUILD_ESEGUIBILI.md"

mv "$package_dir/AVVIA_ESPERTI_WINDOWS.bat" "$package_dir/AVVIA_SWIFTBAT.bat"
# cmd.exe deve ricevere terminatori di riga Windows CRLF.
awk '{sub(/\r$/, ""); printf "%s\r\n", $0}' \
  "$package_dir/AVVIA_SWIFTBAT.bat" > "$tmp_dir/launcher-crlf.bat"
mv "$tmp_dir/launcher-crlf.bat" "$package_dir/AVVIA_SWIFTBAT.bat"

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
