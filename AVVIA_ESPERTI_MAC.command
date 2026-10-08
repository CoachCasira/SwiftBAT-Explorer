#!/bin/bash
set -euo pipefail

APP_NAME="SwiftBAT Explorer 1.3.0"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CACHE_ROOT="${HOME}/Library/Caches/SwiftBAT-Explorer/expert-runtime"
JDK_ROOT="${CACHE_ROOT}/jdk-17"
MAVEN_VERSION="3.9.16"
MAVEN_HOME="${CACHE_ROOT}/apache-maven-${MAVEN_VERSION}"
M2_REPO="${CACHE_ROOT}/m2-repository"
LOG_DIR="${HOME}/Library/Logs/SwiftBAT Explorer"
LOG_FILE="${LOG_DIR}/expert-launcher.log"

mkdir -p "$CACHE_ROOT" "$M2_REPO" "$LOG_DIR"
touch "$LOG_FILE"
exec > >(tee -a "$LOG_FILE") 2>&1

finish() {
  code=$?
  if [ "$code" -ne 0 ]; then
    echo
    echo "============================================================"
    echo "SwiftBAT Explorer non e' riuscito ad avviarsi."
    echo "Codice errore: $code"
    echo "Log: $LOG_FILE"
    echo "============================================================"
    echo
    read -r -p "Premi INVIO per chiudere questa finestra..." _unused || true
  fi
}
trap finish EXIT

echo
echo "============================================================"
echo "  $APP_NAME - avvio per esperti macOS"
echo "============================================================"
echo
echo "Questa procedura NON installa Java o Maven nel sistema."
echo "L'ambiente necessario viene salvato solo nella cache dell'utente."
echo

if [ "$(uname -s)" != "Darwin" ]; then
  echo "[ERRORE] Questo launcher e' destinato esclusivamente a macOS."
  exit 2
fi

for tool in curl tar ditto find; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "[ERRORE] Comando macOS richiesto non trovato: $tool"
    exit 3
  fi
done

case "$(uname -m)" in
  arm64)
    ADOPTIUM_ARCH="aarch64"
    MAC_ARCH_LABEL="Apple Silicon"
    ;;
  x86_64)
    ADOPTIUM_ARCH="x64"
    MAC_ARCH_LABEL="Intel"
    ;;
  *)
    echo "[ERRORE] Architettura Mac non supportata: $(uname -m)"
    exit 4
    ;;
esac

echo "Mac rilevato: $MAC_ARCH_LABEL ($(uname -m))"

download_jdk() {
  echo
  echo "[1/3] Preparo Java 17 locale..."
  tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/swiftbat-jdk.XXXXXX")"
  archive="$tmp_dir/temurin17.tar.gz"
  extract_dir="$tmp_dir/extracted"
  mkdir -p "$extract_dir"

  jdk_url="https://api.adoptium.net/v3/binary/latest/17/ga/mac/${ADOPTIUM_ARCH}/jdk/hotspot/normal/eclipse"
  echo "Scarico Eclipse Temurin JDK 17 per $MAC_ARCH_LABEL..."
  curl -fL --retry 3 --retry-delay 2 --connect-timeout 20 --progress-bar "$jdk_url" -o "$archive"

  if ! tar -tzf "$archive" >/dev/null 2>&1; then
    echo "[ERRORE] Il pacchetto Java scaricato non e' valido."
    rm -rf "$tmp_dir"
    exit 5
  fi

  tar -xzf "$archive" -C "$extract_dir"
  jdk_home="$(find "$extract_dir" -type d -path "*/Contents/Home" -print -quit)"
  if [ -z "$jdk_home" ] || [ ! -x "$jdk_home/bin/java" ]; then
    echo "[ERRORE] Non trovo Java nel pacchetto Temurin scaricato."
    rm -rf "$tmp_dir"
    exit 6
  fi

  rm -rf "${JDK_ROOT}.next"
  ditto "$jdk_home" "${JDK_ROOT}.next"
  rm -rf "$JDK_ROOT"
  mv "${JDK_ROOT}.next" "$JDK_ROOT"
  rm -rf "$tmp_dir"
}

if [ ! -x "$JDK_ROOT/bin/java" ]; then
  download_jdk
else
  if ! "$JDK_ROOT/bin/java" -version 2>&1 | head -n 1 | grep -Eq '"17([."]|$)'; then
    echo "La cache Java esistente non e' Java 17: la ricreo."
    rm -rf "$JDK_ROOT"
    download_jdk
  else
    echo
    echo "[1/3] Java 17 locale gia' disponibile."
  fi
fi

export JAVA_HOME="$JDK_ROOT"
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_OPTS="-Dfile.encoding=UTF-8"

echo "Java usata:"
"$JAVA_HOME/bin/java" -version

download_maven() {
  echo
  echo "[2/3] Preparo Maven locale..."
  tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/swiftbat-maven.XXXXXX")"
  archive="$tmp_dir/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
  primary_url="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
  fallback_url="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

  echo "Scarico Apache Maven $MAVEN_VERSION..."
  if ! curl -fL --retry 2 --retry-delay 2 --connect-timeout 20 --progress-bar "$primary_url" -o "$archive"; then
    echo "Mirror principale non disponibile, provo l'archivio Apache..."
    curl -fL --retry 3 --retry-delay 2 --connect-timeout 20 --progress-bar "$fallback_url" -o "$archive"
  fi

  if ! tar -tzf "$archive" >/dev/null 2>&1; then
    echo "[ERRORE] Il pacchetto Maven scaricato non e' valido."
    rm -rf "$tmp_dir"
    exit 7
  fi

  tar -xzf "$archive" -C "$tmp_dir"
  if [ ! -x "$tmp_dir/apache-maven-${MAVEN_VERSION}/bin/mvn" ]; then
    echo "[ERRORE] Maven non e' stato estratto correttamente."
    rm -rf "$tmp_dir"
    exit 8
  fi

  rm -rf "${MAVEN_HOME}.next"
  mv "$tmp_dir/apache-maven-${MAVEN_VERSION}" "${MAVEN_HOME}.next"
  rm -rf "$MAVEN_HOME"
  mv "${MAVEN_HOME}.next" "$MAVEN_HOME"
  rm -rf "$tmp_dir"
}

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  download_maven
else
  echo
  echo "[2/3] Maven $MAVEN_VERSION locale gia' disponibile."
fi

echo "Maven usata:"
"$MAVEN_HOME/bin/mvn" -version | head -n 4

if [ ! -f "$SCRIPT_DIR/pom.xml" ] || [ ! -d "$SCRIPT_DIR/src/main/java" ]; then
  echo "[ERRORE] La cartella dell'app non e' completa."
  echo "Mancano pom.xml o src/main/java."
  exit 9
fi

echo
echo "[3/3] Avvio SwiftBAT Explorer..."
echo "Al primo avvio Maven scarichera' le librerie necessarie."
echo "Gli avvii successivi useranno la cache locale e saranno piu' rapidi."
echo

cd "$SCRIPT_DIR"
"$MAVEN_HOME/bin/mvn"   --batch-mode   -Dmaven.repo.local="$M2_REPO"   -DskipTests   compile javafx:run
