#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
minimum="${1:-208}"
for utility in unzip awk grep; do
  if ! command -v "$utility" >/dev/null 2>&1; then
    echo "[ERRORE] Comando richiesto non trovato: $utility"
    exit 2
  fi
done

declare -a counts=()
for package in SwiftBAT-Explorer-Relatori-Windows SwiftBAT-Explorer-Esperti-macOS; do
  zip="$ROOT/dist/$package.zip"
  if [ ! -f "$zip" ]; then
    echo "[ERRORE] Pacchetto mancante: $zip"
    exit 1
  fi
  entries="$(unzip -Z -1 "$zip")"
  # A GRB is usable only when a manifest and an ASCII or FITS product coexist.
  names="$(printf '%s\n' "$entries" | grep -E '^SwiftBAT Explorer/Applicazione/offline-data/v1/[^/]+/manifest.properties$' || true)"
  valid=0
  while IFS= read -r manifest; do
    [ -n "$manifest" ] || continue
    directory="${manifest%manifest.properties}"
    if printf '%s\n' "$entries" | grep -Fqx "${directory}ascii-4chan-1s.dat" ||
       printf '%s\n' "$entries" | grep -Fqx "${directory}fits-1chan-1s.lc"; then
      valid=$((valid + 1))
    fi
  done <<< "$names"
  counts+=("$valid")
  echo "$package: $valid GRB scientificamente disponibili offline"
  if [ "$valid" -lt "$minimum" ]; then
    echo "[ERRORE] Sono richiesti almeno $minimum GRB. Trasferire prima i dati del Mac."
    exit 1
  fi
done
if [ "${counts[0]}" -ne "${counts[1]}" ]; then
  echo "[ERRORE] Gli ZIP non contengono lo stesso numero di GRB."
  exit 1
fi
echo "[OK] Pacchetti verificati, archivio offline presente su entrambe le piattaforme."
