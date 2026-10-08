#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "============================================================"
echo "  SwiftBAT Explorer - pacchetti macOS e Windows"
echo "============================================================"
echo
bash "$SCRIPT_DIR/CREA_PACCHETTO_ESPERTI_MAC.command"
echo
bash "$SCRIPT_DIR/CREA_PACCHETTO_RELATORI_WINDOWS_MAC.command"
echo
echo "Entrambi gli ZIP sono pronti nella cartella dist/."
echo "Ricorda: verifica l'avvio su Mac e su Windows prima dell'invio."
