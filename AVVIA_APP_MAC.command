#!/bin/bash
set -e
cd "$(dirname "$0")"

TARGET_BRANCH="feature/analysis-population-1.3.0"
echo "SwiftBAT Explorer 1.3.0 - avvio macOS"

# During thesis development, keep a clean checkout aligned with the remote
# branch before compiling. If the machine is offline or there are local edits,
# the launcher keeps the local files instead of overwriting work.
if [ -d .git ]; then
  CURRENT_BRANCH="$(git branch --show-current 2>/dev/null || true)"
  echo "Branch locale: ${CURRENT_BRANCH:-sconosciuto}"
  if [ "$CURRENT_BRANCH" = "$TARGET_BRANCH" ]; then
    if git fetch --quiet origin "$TARGET_BRANCH"; then
      LOCAL_SHA="$(git rev-parse HEAD)"
      REMOTE_SHA="$(git rev-parse "origin/$TARGET_BRANCH")"
      if [ "$LOCAL_SHA" != "$REMOTE_SHA" ]; then
        if git diff --quiet && git diff --cached --quiet; then
          echo "Aggiornamento del branch alla versione GitHub più recente..."
          if ! git merge --ff-only "origin/$TARGET_BRANCH"; then
            echo "Aggiornamento automatico non possibile; avvio la copia locale."
          fi
        else
          echo "Modifiche locali rilevate: non eseguo il pull automatico."
        fi
      fi
    else
      echo "GitHub non raggiungibile: avvio la copia locale."
    fi
  else
    echo "ATTENZIONE: branch attivo diverso da $TARGET_BRANCH"
  fi
  echo "Commit in avvio: $(git rev-parse --short HEAD 2>/dev/null || echo sconosciuto)"
fi

echo "Java rilevata:"
java -version
chmod +x ./mvnw
./mvnw clean javafx:run
