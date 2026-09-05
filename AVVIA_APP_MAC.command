#!/bin/bash
set -e
cd "$(dirname "$0")"
echo "SwiftBAT Explorer 1.2.0 - avvio macOS"
echo "Java rilevata:"
java -version
chmod +x ./mvnw
./mvnw javafx:run
