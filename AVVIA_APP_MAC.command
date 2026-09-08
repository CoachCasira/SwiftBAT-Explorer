#!/bin/bash
set -e
cd "$(dirname "$0")"
echo "SwiftBAT Explorer 1.3.0 - Spectral Preview - avvio macOS"
echo "Java rilevata:"
java -version
chmod +x ./mvnw
./mvnw clean javafx:run
