# Creazione dei due ZIP distributivi direttamente da Windows 10/11.
# Solo PowerShell 5.1 e .NET integrati: non richiede Java, Maven o Python.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$dist = Join-Path $root 'dist'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$ascii = [System.Text.Encoding]::ASCII
$lf = [string][char]10
$crlf = ([string][char]13) + $lf
Add-Type -AssemblyName System.IO.Compression

foreach ($name in @('pom.xml','src\main\java','AVVIA_ESPERTI_MAC.command','AVVIA_ESPERTI_WINDOWS.ps1')) {
    if (-not (Test-Path -LiteralPath (Join-Path $root $name))) {
        throw "Progetto incompleto, manca $name. Avvia lo script dalla cartella del repository."
    }
}

$excludeDirs = @('.git','.github','.swiftbat-runtime','target','dist','.idea',
    '.vscode','.settings','.classpath','.project','node_modules','__pycache__')
$excludeBoth = @('CREA_PACCHETTI_DISTRIBUZIONE_WINDOWS.bat',
    'CREA_PACCHETTI_DISTRIBUZIONE_WINDOWS.ps1',
    'CREA_PACCHETTI_DISTRIBUZIONE_MAC.command',
    'CREA_PACCHETTO_ESPERTI_MAC.command',
    'CREA_PACCHETTO_RELATORI_WINDOWS_MAC.command')
$excludeMac = $excludeBoth + @('AVVIA_APP_MAC.command','CREA_APP_MAC.command',
    'AVVIA_APP.bat','CREA_APP_WINDOWS.bat','AVVIA_ESPERTI_WINDOWS.bat',
    'AVVIA_ESPERTI_WINDOWS.ps1','ISTRUZIONI_RELATORI_WINDOWS.md')
$excludeWindows = $excludeBoth + @('AVVIA_APP.bat','AVVIA_APP_MAC.command',
    'AVVIA_ESPERTI_MAC.command','AVVIA_ESPERTI_WINDOWS.bat',
    'CREA_APP_MAC.command','BUILD_ESEGUIBILI.md','ISTRUZIONI_ESPERTI_MAC.md')

function Get-PackageFiles([string[]]$excluded) {
    $collected = New-Object 'System.Collections.Generic.List[System.IO.FileInfo]'
    foreach ($item in Get-ChildItem -LiteralPath $root -Force) {
        if ($item.PSIsContainer) {
            if ($excludeDirs -contains $item.Name) { continue }
            foreach ($file in Get-ChildItem -LiteralPath $item.FullName -Recurse -File -Force) {
                $relative = $file.FullName.Substring($root.Length + 1)
                $segments = $relative -split '[\/\\]'
                if ($file.Name -eq '.DS_Store' -or $file.Extension -eq '.pyc') { continue }
                $skip = $false
                foreach ($segment in $segments) {
                    if ($excludeDirs -contains $segment) { $skip = $true; break }
                }
                if (-not $skip) { $collected.Add($file) }
            }
        } elseif ($excluded -notcontains $item.Name -and $item.Name -ne '.DS_Store') {
            $collected.Add($item)
        }
    }
    return @($collected | Sort-Object FullName)
}

function Add-Entry {
    param(
        [System.IO.Compression.ZipArchive]$Archive,
        [string]$Name,
        [byte[]]$Data,
        [string]$FilePath,
        [bool]$Executable = $false
    )
    $entry = $Archive.CreateEntry($Name,[System.IO.Compression.CompressionLevel]::Optimal)
    # POSIX: regular file (0100000) + 0755 per .command/mvnw, altrimenti 0644.
    $mode = if ($Executable) { 33261 } else { 33188 }
    $attr = [uint32]($mode * 65536 + 32)
    $entry.ExternalAttributes = [BitConverter]::ToInt32([BitConverter]::GetBytes($attr),0)
    $stream = $entry.Open()
    try {
        if ($null -ne $Data) { $stream.Write($Data,0,$Data.Length) }
        else {
            $source = [System.IO.File]::OpenRead($FilePath)
            try { $source.CopyTo($stream) }
            finally { $source.Dispose() }
        }
    } finally { $stream.Dispose() }
}

function Mark-ZipAsUnix([string]$path) {
    # Il producer .NET su Windows imposta OS=0 (DOS): Finder rischia di perdere chmod.
    # La scrittura dei bit Unix richiede anche "version made by" con OS=3.
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $end = $bytes.Length - 22
    if ($end -lt 0 -or [BitConverter]::ToUInt32($bytes,$end) -ne [uint32]0x06054b50) {
        throw "Header ZIP invalido: $path"
    }
    $count = [BitConverter]::ToUInt16($bytes,$end + 10)
    $position = [int][BitConverter]::ToUInt32($bytes,$end + 16)
    for ($i=0; $i -lt $count; $i++) {
        if ([BitConverter]::ToUInt32($bytes,$position) -ne [uint32]0x02014b50) {
            throw "Directory centrale ZIP invalida all'elemento $i"
        }
        $bytes[$position + 5] = 3
        $nameSize = [BitConverter]::ToUInt16($bytes,$position + 28)
        $extraSize = [BitConverter]::ToUInt16($bytes,$position + 30)
        $commentSize = [BitConverter]::ToUInt16($bytes,$position + 32)
        $position += 46 + $nameSize + $extraSize + $commentSize
    }
    [System.IO.File]::WriteAllBytes($path,$bytes)
}

$macStarter = @'
#!/bin/bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
INTERNAL="$ROOT_DIR/Applicazione/AVVIA_ESPERTI_MAC.command"
if [ ! -f "$INTERNAL" ]; then
  echo "[ERRORE] Manca Applicazione/AVVIA_ESPERTI_MAC.command."
  read -r -p "Premi INVIO per chiudere..." _unused || true
  exit 1
fi
exec /bin/bash "$INTERNAL" "$@"
'@
$windowsStarter = @'
@echo off
setlocal EnableExtensions
title SwiftBAT Explorer 1.3.0 - Windows
if not exist "%~dp0Applicazione\AVVIA_ESPERTI_WINDOWS.ps1" (
  echo [ERRORE] Manca la cartella Applicazione o il launcher interno.
  pause
  exit /b 1
)
cd /d "%~dp0Applicazione"
if errorlevel 1 (
  echo [ERRORE] Non riesco ad aprire la cartella Applicazione.
  pause
  exit /b 1
)
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0Applicazione\AVVIA_ESPERTI_WINDOWS.ps1" %*
set "RESULT=%ERRORLEVEL%"
if not "%RESULT%"=="0" (
  echo.
  echo [ERRORE] Avvio non riuscito. Consulta il log nella cartella Applicazione.
  pause
)
exit /b %RESULT%
'@

function Build-Zip {
    param([string]$Name,[string]$Launcher,[string]$Starter,
          [string[]]$Excluded,[bool]$IsMac)
    Write-Host ("Creo {0}.zip..." -f $Name)
    $zipPath = Join-Path $dist ($Name + '.zip')
    $tmp = Join-Path $dist ($Name + '.tmp.zip')
    $sha = Join-Path $dist ($Name + '.sha256.txt')
    if (Test-Path -LiteralPath $tmp) { Remove-Item -LiteralPath $tmp -Force }
    $fileList = @(Get-PackageFiles $Excluded)
    $stream = $null
    $archive = $null
    try {
        $stream = [System.IO.File]::Open($tmp,[System.IO.FileMode]::CreateNew)
        $archive = [System.IO.Compression.ZipArchive]::new(
            $stream,[System.IO.Compression.ZipArchiveMode]::Create,$false)
        $text = $Starter.Replace($crlf,$lf).Replace([string][char]13,$lf).TrimEnd([char]13,[char]10) + $lf
        if (-not $IsMac) { $text = $text.Replace($lf,$crlf) }
        $encoding = if ($IsMac) { $utf8 } else { $ascii }
        Add-Entry -Archive $archive -Name ("SwiftBAT Explorer/" + $Launcher) -Data $encoding.GetBytes($text) -Executable $IsMac
        foreach ($file in $fileList) {
            $relative = $file.FullName.Substring($root.Length + 1).Replace('\','/')
            $nameInZip = 'SwiftBAT Explorer/Applicazione/' + $relative
            $unixScript = $IsMac -and ($file.Extension -eq '.command' -or
                $file.Extension -eq '.sh' -or $relative -eq 'mvnw')
            if ($unixScript) {
                $text = [System.IO.File]::ReadAllText($file.FullName,$utf8)
                $text = $text.Replace($crlf,$lf).Replace([string][char]13,$lf)
                Add-Entry -Archive $archive -Name $nameInZip -Data $utf8.GetBytes($text) -Executable $true
            } else {
                Add-Entry -Archive $archive -Name $nameInZip -Data $null -FilePath $file.FullName
            }
        }
    } finally {
        if ($null -ne $archive) { $archive.Dispose() }
        elseif ($null -ne $stream) { $stream.Dispose() }
    }
    try {
        if ($IsMac) { Mark-ZipAsUnix $tmp }
        if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
        Move-Item -LiteralPath $tmp -Destination $zipPath
        $digest = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
        [System.IO.File]::WriteAllText($sha,($digest + '  ' + $Name + '.zip' + $lf),$ascii)
        Write-Host ("Pronto: {0}" -f $zipPath)
    } finally {
        if (Test-Path -LiteralPath $tmp) { Remove-Item -LiteralPath $tmp -Force }
    }
}
if (-not (Test-Path -LiteralPath $dist)) {
    New-Item -ItemType Directory -Path $dist -Force | Out-Null
}
Write-Host '============================================================'
Write-Host ' SwiftBAT Explorer - distribuzione macOS/Windows da Windows'
Write-Host '============================================================'
Build-Zip -Name 'SwiftBAT-Explorer-Esperti-macOS' -Launcher 'AVVIA_SWIFTBAT.command' -Starter $macStarter -Excluded $excludeMac -IsMac $true
Build-Zip -Name 'SwiftBAT-Explorer-Relatori-Windows' -Launcher 'AVVIA_SWIFTBAT.bat' -Starter $windowsStarter -Excluded $excludeWindows -IsMac $false
Write-Host '[OK] Entrambi gli ZIP e i checksum SHA-256 sono in dist/'
