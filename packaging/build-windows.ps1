$ErrorActionPreference = "Stop"

$ProjectDir = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectDir

if (Get-Command mvn -ErrorAction SilentlyContinue) {
    & mvn -B clean verify
} else {
    & "$ProjectDir\mvnw.cmd" -B clean verify
}
if ($LASTEXITCODE -ne 0) {
    throw "La build Maven non e riuscita."
}

$InputDir = Join-Path $ProjectDir "target\jpackage-input"
$OutputDir = Join-Path $ProjectDir "dist"
$AppDir = Join-Path $OutputDir "SwiftBAT Explorer"
$AppJar = "swiftbat-explorer-1.2.0.jar"
$PortableZip = Join-Path $OutputDir "SwiftBAT-Explorer-Windows-x64-portable.zip"
$InstallerTempDir = Join-Path $OutputDir "installer"
$InstallerFile = Join-Path $OutputDir "SwiftBAT-Explorer-Setup-1.2.0.exe"
$ChecksumFile = Join-Path $OutputDir "SwiftBAT-Explorer-Setup-1.2.0.sha256.txt"
$IconFile = Join-Path $PSScriptRoot "app-icon.ico"

if (-not (Test-Path $IconFile)) {
    if (-not (Get-Command magick -ErrorAction SilentlyContinue)) {
        throw "ImageMagick non e disponibile: impossibile generare l'icona Windows."
    }
    & magick `
        (Join-Path $ProjectDir "src\main\resources\app-icon.png") `
        -define "icon:auto-resize=256,128,64,48,32,16" `
        $IconFile
    if ($LASTEXITCODE -ne 0) {
        throw "Generazione dell'icona Windows non riuscita."
    }
}

Copy-Item (Join-Path $ProjectDir "target\$AppJar") (Join-Path $InputDir $AppJar) -Force
if (Test-Path $AppDir) {
    Remove-Item $AppDir -Recurse -Force
}
if (Test-Path $PortableZip) {
    Remove-Item $PortableZip -Force
}
if (Test-Path $InstallerTempDir) {
    Remove-Item $InstallerTempDir -Recurse -Force
}
if (Test-Path $InstallerFile) {
    Remove-Item $InstallerFile -Force
}
if (Test-Path $ChecksumFile) {
    Remove-Item $ChecksumFile -Force
}
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
New-Item -ItemType Directory -Path $InstallerTempDir -Force | Out-Null

& jpackage `
    --type app-image `
    --dest $OutputDir `
    --input $InputDir `
    --name "SwiftBAT Explorer" `
    --main-jar $AppJar `
    --main-class it.casiraghi.swiftbat.Launcher `
    --app-version 1.2.0 `
    --vendor "Matteo Casiraghi" `
    --description "Esplorazione e confronto dei dati Swift/BAT GRB" `
    --copyright "2026 Matteo Casiraghi" `
    --icon $IconFile `
    --java-options "-Dfile.encoding=UTF-8"
if ($LASTEXITCODE -ne 0) {
    throw "jpackage non e riuscito a creare l'applicazione Windows."
}

Compress-Archive -Path $AppDir -DestinationPath $PortableZip -CompressionLevel Optimal

& jpackage `
    --type exe `
    --dest $InstallerTempDir `
    --name "SwiftBAT Explorer" `
    --app-image $AppDir `
    --app-version 1.2.0 `
    --vendor "Matteo Casiraghi" `
    --description "Esplorazione e confronto dei dati Swift/BAT GRB" `
    --copyright "2026 Matteo Casiraghi" `
    --license-file (Join-Path $ProjectDir "LICENSE.txt") `
    --win-per-user-install `
    --win-dir-chooser `
    --win-menu `
    --win-menu-group "SwiftBAT Explorer" `
    --win-shortcut `
    --win-upgrade-uuid "6e7b6c0a-bf03-49a5-a49f-30950f3e7833"
if ($LASTEXITCODE -ne 0) {
    throw "jpackage non e riuscito a creare l'installer Windows."
}

$GeneratedInstallers = @(Get-ChildItem -Path $InstallerTempDir -Filter "*.exe" -File)
if ($GeneratedInstallers.Count -ne 1) {
    throw "Non e stato trovato un unico installer Windows nella cartella di output."
}
Move-Item $GeneratedInstallers[0].FullName $InstallerFile -Force

$InstallerHash = (Get-FileHash -Path $InstallerFile -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -Path $ChecksumFile -Value "$InstallerHash  SwiftBAT-Explorer-Setup-1.2.0.exe" -Encoding ascii

Write-Host "Installer autosufficiente creato: $InstallerFile"
Write-Host "Pacchetto portabile creato: $PortableZip"
