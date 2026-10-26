#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
USER_CACHE="$HOME/.swiftbat-explorer/cache"
STAGING="$(mktemp -d "${TMPDIR:-/tmp}/swiftbat-offline.XXXXXX")"
trap 'rm -rf "$STAGING"' EXIT
mkdir -p "$STAGING/offline-data" "$ROOT/dist"

# Copy any existing curated data first, then union the local NASA products.
if [ -d "$ROOT/offline-data" ]; then
  rsync -a "$ROOT/offline-data/" "$STAGING/offline-data/"
fi
for category in v1 scientific spectroscopy-v1; do
  if [ -d "$USER_CACHE/$category" ]; then
    mkdir -p "$STAGING/offline-data/$category"
    rsync -a --ignore-existing "$USER_CACHE/$category/" "$STAGING/offline-data/$category/"
  fi
done

number=0
if [ -d "$STAGING/offline-data/v1" ]; then
  number="$(find "$STAGING/offline-data/v1" -mindepth 2 -maxdepth 2 -name manifest.properties -type f | wc -l | tr -d ' ')"
fi
if [ "$number" -eq 0 ]; then
  echo "[ERRORE] Nessun GRB salvato: esportazione annullata."
  exit 1
fi

output="$ROOT/dist/SwiftBAT-Offline-Archive.zip"
rm -f "$output"
(cd "$STAGING" && ditto -c -k --norsrc --keepParent offline-data "$output")

echo "Archivio creato: $output"
echo "Manifest GRB trovati: $number"
echo "Copia lo ZIP sul PC Windows ed estrailo NELLA CARTELLA del progetto SwiftBAT Explorer."
echo "Deve risultare il percorso offline-data/v1/GRB.../manifest.properties"
echo "Conserva l'archivio su un supporto sicuro; contiene dati NASA pubblici."
