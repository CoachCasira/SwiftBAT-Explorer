#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="$SCRIPT_DIR/dist"
PACKAGE_NAME="SwiftBAT-Explorer-Esperti-macOS"
ZIP_FILE="$DIST_DIR/${PACKAGE_NAME}.zip"
SHA_FILE="$DIST_DIR/${PACKAGE_NAME}.sha256.txt"

if [ "$(uname -s)" != "Darwin" ]; then
  echo "[ERRORE] Questo script di confezionamento va eseguito su macOS."
  exit 2
fi

for tool in ditto shasum rsync; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "[ERRORE] Comando macOS richiesto non trovato: $tool"
    exit 3
  fi
done

if [ ! -f "$SCRIPT_DIR/AVVIA_ESPERTI_MAC.command" ]; then
  echo "[ERRORE] Manca AVVIA_ESPERTI_MAC.command."
  exit 4
fi

tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/swiftbat-experts-package.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT

package_dir="$tmp_dir/SwiftBAT Explorer"
mkdir -p "$DIST_DIR"

echo "Creo il pacchetto per gli esperti..."
rsync -a --exclude='/.git/' --exclude='/.github/' --exclude='/.swiftbat-runtime/' \
  --exclude='/target/' --exclude='/dist/' --exclude='/.idea/' \
  --exclude='/.vscode/' "$SCRIPT_DIR/" "$package_dir/"

rm -rf   "$package_dir/.git"   "$package_dir/.github"   "$package_dir/.swiftbat-runtime"   "$package_dir/target"   "$package_dir/dist"   "$package_dir/.idea"   "$package_dir/.vscode"

rm -f   "$package_dir/.DS_Store"   "$package_dir/AVVIA_APP_MAC.command"   "$package_dir/CREA_APP_MAC.command"   "$package_dir/CREA_PACCHETTO_ESPERTI_MAC.command"   "$package_dir/AVVIA_APP.bat"   "$package_dir/CREA_APP_WINDOWS.bat" \
  "$package_dir/AVVIA_ESPERTI_WINDOWS.bat" \
  "$package_dir/AVVIA_ESPERTI_WINDOWS.ps1" \
  "$package_dir/CREA_PACCHETTO_RELATORI_WINDOWS_MAC.command" \
  "$package_dir/CREA_PACCHETTI_DISTRIBUZIONE_MAC.command" \
  "$package_dir/ISTRUZIONI_RELATORI_WINDOWS.md"

mv "$package_dir/AVVIA_ESPERTI_MAC.command" "$package_dir/AVVIA_SWIFTBAT.command"
chmod 755 "$package_dir/AVVIA_SWIFTBAT.command"
[ -f "$package_dir/mvnw" ] && chmod 755 "$package_dir/mvnw" || true

rm -f "$ZIP_FILE" "$SHA_FILE"
ditto -c -k --sequesterRsrc --keepParent "$package_dir" "$ZIP_FILE"
(
  cd "$DIST_DIR"
  shasum -a 256 "${PACKAGE_NAME}.zip" > "${PACKAGE_NAME}.sha256.txt"
)

echo
echo "Pacchetto pronto:"
echo "  $ZIP_FILE"
echo "Checksum:"
echo "  $SHA_FILE"
echo
echo "Per simulare il computer di un esperto:"
echo "1. estrai lo ZIP in una cartella nuova;"
echo "2. apri la cartella 'SwiftBAT Explorer';"
echo "3. fai doppio clic su AVVIA_SWIFTBAT.command."
echo
echo "Il launcher ignora Java/Maven installati nel sistema e usa il proprio ambiente locale."
