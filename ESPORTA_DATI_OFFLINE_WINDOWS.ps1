param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$userCache = Join-Path $env:USERPROFILE '.swiftbat-explorer\cache'
$staging = Join-Path ([IO.Path]::GetTempPath()) ("swiftbat-offline-" + [guid]::NewGuid().ToString('N'))
$archiveRoot = Join-Path $staging 'offline-data'
$dist = Join-Path $root 'dist'
$target = Join-Path $dist 'SwiftBAT-Offline-Archive-Windows.zip'

try {
    New-Item -ItemType Directory -Force -Path $archiveRoot, $dist | Out-Null
    $existing = Join-Path $root 'offline-data'
    if (Test-Path -LiteralPath $existing -PathType Container) {
        Copy-Item -LiteralPath $existing -Destination $archiveRoot -Recurse -Force -ErrorAction Stop
        # Copy-Item into an existing directory nests source name under it.
        $nested = Join-Path $archiveRoot 'offline-data'
        if (Test-Path -LiteralPath $nested -PathType Container) {
            foreach ($item in Get-ChildItem -LiteralPath $nested -Force) {
                Move-Item -LiteralPath $item.FullName -Destination $archiveRoot -Force
            }
            Remove-Item -LiteralPath $nested -Recurse -Force
        }
    }
    foreach ($category in @('v1','scientific','spectroscopy-v1')) {
        $source = Join-Path $userCache $category
        if (-not (Test-Path -LiteralPath $source -PathType Container)) { continue }
        $destination = Join-Path $archiveRoot $category
        New-Item -ItemType Directory -Force -Path $destination | Out-Null
        foreach ($file in Get-ChildItem -LiteralPath $source -Recurse -File -Force) {
            $relative = $file.FullName.Substring($source.Length + 1)
            $output = Join-Path $destination $relative
            if (Test-Path -LiteralPath $output -PathType Leaf) { continue }
            New-Item -ItemType Directory -Force -Path (Split-Path -Parent $output) | Out-Null
            Copy-Item -LiteralPath $file.FullName -Destination $output
        }
    }
    $catalogFolder = Join-Path $archiveRoot 'v1'
    $count = 0
    if (Test-Path -LiteralPath $catalogFolder -PathType Container) {
        $count = @(Get-ChildItem -LiteralPath $catalogFolder -Recurse -File -Filter 'manifest.properties').Count
    }
    if ($count -eq 0) { throw 'Nessun prodotto scientifico offline da esportare.' }
    if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Force }
    Compress-Archive -LiteralPath $archiveRoot -DestinationPath $target -CompressionLevel Optimal
    Write-Host "Archivio unificato pronto: $target"
    Write-Host "Manifest GRB inclusi: $count"
    Write-Host 'Per sincronizzare il Mac, estrarre lo ZIP nella cartella del progetto SwiftBAT Explorer.'
} finally {
    if (Test-Path -LiteralPath $staging) {
        Remove-Item -LiteralPath $staging -Recurse -Force
    }
}
