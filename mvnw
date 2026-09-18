#!/usr/bin/env sh
set -eu
MAVEN_VERSION="3.9.16"
WRAPPER_BASE="${HOME}/.m2/wrapper/dists/swiftbat-maven-${MAVEN_VERSION}"
MAVEN_HOME="${WRAPPER_BASE}/apache-maven-${MAVEN_VERSION}"
MAVEN_CMD="${MAVEN_HOME}/bin/mvn"
MAVEN_ARCHIVE="${WRAPPER_BASE}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MAVEN_URL="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

if [ ! -x "$MAVEN_CMD" ]; then
  mkdir -p "$WRAPPER_BASE"
  echo "[INFO] Download iniziale di Maven ${MAVEN_VERSION}..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$MAVEN_URL" -o "$MAVEN_ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$MAVEN_URL" -O "$MAVEN_ARCHIVE"
  else
    echo "[ERRORE] Servono curl o wget per il primo avvio." >&2
    exit 1
  fi
  tar -xzf "$MAVEN_ARCHIVE" -C "$WRAPPER_BASE"
  rm -f "$MAVEN_ARCHIVE"
fi

exec "$MAVEN_CMD" "$@"
