#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if command -v mvn >/dev/null 2>&1; then
  mvn -B clean verify
else
  chmod +x ./mvnw
  ./mvnw -B clean verify
fi

INPUT_DIR="$SCRIPT_DIR/target/jpackage-input"
OUTPUT_DIR="$SCRIPT_DIR/dist"
ICONSET_DIR="$SCRIPT_DIR/target/SwiftBAT.iconset"
ICNS_FILE="$SCRIPT_DIR/target/SwiftBAT.icns"
APP_VERSION="1.3.0"
APP_JAR="swiftbat-explorer-$APP_VERSION.jar"

cp "$SCRIPT_DIR/target/$APP_JAR" "$INPUT_DIR/$APP_JAR"
rm -rf "$OUTPUT_DIR/SwiftBAT Explorer.app" "$ICONSET_DIR"
mkdir -p "$OUTPUT_DIR" "$ICONSET_DIR"

sips -z 16 16     src/main/resources/app-icon.png --out "$ICONSET_DIR/icon_16x16.png" >/dev/null
sips -z 32 32     src/main/resources/app-icon.png --out "$ICONSET_DIR/icon_16x16@2x.png" >/dev/null
sips -z 32 32     src/main/resources/app-icon.png --out "$ICONSET_DIR/icon_32x32.png" >/dev/null
sips -z 64 64     src/main/resources/app-icon.png --out "$ICONSET_DIR/icon_32x32@2x.png" >/dev/null
sips -z 128 128   src/main/resources/app-icon.png --out "$ICONSET_DIR/icon_128x128.png" >/dev/null
sips -z 256 256   src/main/resources/app-icon.png --out "$ICONSET_DIR/icon_128x128@2x.png" >/dev/null
sips -z 256 256   src/main/resources/app-icon.png --out "$ICONSET_DIR/icon_256x256.png" >/dev/null
sips -z 512 512   src/main/resources/app-icon.png --out "$ICONSET_DIR/icon_256x256@2x.png" >/dev/null
sips -z 512 512   src/main/resources/app-icon.png --out "$ICONSET_DIR/icon_512x512.png" >/dev/null
sips -z 1024 1024 src/main/resources/app-icon.png --out "$ICONSET_DIR/icon_512x512@2x.png" >/dev/null
iconutil -c icns "$ICONSET_DIR" -o "$ICNS_FILE"

jpackage \
  --type app-image \
  --dest "$OUTPUT_DIR" \
  --input "$INPUT_DIR" \
  --name "SwiftBAT Explorer" \
  --main-jar "$APP_JAR" \
  --main-class it.casiraghi.swiftbat.Launcher \
  --app-version "$APP_VERSION" \
  --vendor "Matteo Casiraghi" \
  --description "Esplorazione e confronto dei dati Swift/BAT GRB" \
  --copyright "2026 Matteo Casiraghi" \
  --icon "$ICNS_FILE" \
  --mac-package-identifier it.casiraghi.swiftbat.explorer \
  --mac-package-name "SwiftBAT" \
  --java-options "-Dfile.encoding=UTF-8"

codesign --force --deep --sign - "$OUTPUT_DIR/SwiftBAT Explorer.app"
codesign --verify --deep --strict "$OUTPUT_DIR/SwiftBAT Explorer.app"

case "$(uname -m)" in
  arm64) PACKAGE_ARCH="arm64" ;;
  x86_64) PACKAGE_ARCH="x64" ;;
  *) PACKAGE_ARCH="$(uname -m)" ;;
esac

ZIP_FILE="$OUTPUT_DIR/SwiftBAT-Explorer-macOS-$PACKAGE_ARCH.zip"
rm -f "$ZIP_FILE"
ditto -c -k --sequesterRsrc --keepParent "$OUTPUT_DIR/SwiftBAT Explorer.app" "$ZIP_FILE"

echo
echo "Pacchetto creato: $ZIP_FILE"
