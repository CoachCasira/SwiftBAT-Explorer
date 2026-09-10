#!/bin/bash
set -e
cd "$(dirname "$0")"

TARGET_BRANCH="feature/analysis-population-1.3.0"
echo "SwiftBAT Explorer 1.3.0 - avvio macOS"

# Il launcher non contatta GitHub durante l'avvio.
# In questo modo non vengono mai richiesti username/password e l'app parte
# direttamente dalla copia locale. Gli aggiornamenti si fanno da GitHub Desktop
# (Pull origin) prima di avviare l'app quando serve.
if [ -d .git ]; then
  CURRENT_BRANCH="$(git branch --show-current 2>/dev/null || true)"
  echo "Branch locale: ${CURRENT_BRANCH:-sconosciuto}"
  if [ -n "$CURRENT_BRANCH" ] && [ "$CURRENT_BRANCH" != "$TARGET_BRANCH" ]; then
    echo "ATTENZIONE: branch attivo diverso da $TARGET_BRANCH"
  fi
  echo "Commit in avvio: $(git rev-parse --short HEAD 2>/dev/null || echo sconosciuto)"
fi

echo "Java rilevata:"
java -version
chmod +x ./mvnw
./mvnw clean javafx:run
