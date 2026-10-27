# Verify both deliverables without unpacking them or requiring Maven/Java.
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File .\VERIFICA_PACCHETTI_WINDOWS.ps1 -MinimumGrbs 208
param([int]$MinimumGrbs = 208)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$dist = Join-Path $root 'dist'
$expected = @('SwiftBAT-Explorer-Relatori-Windows', 'SwiftBAT-Explorer-Esperti-macOS')
$counts = @{}
foreach ($name in $expected) {
    $zip = Join-Path $dist ($name + '.zip')
    if (-not (Test-Path -LiteralPath $zip)) {
        throw "Pacchetto mancante: $zip. Crea prima entrambi gli ZIP."
    }

    $stream = [System.IO.File]::OpenRead($zip)
    try {
        $archive = [System.IO.Compression.ZipArchive]::new(
            $stream,[System.IO.Compression.ZipArchiveMode]::Read,$true)
        try {
            $entries = @{}
            $duplicates = New-Object 'System.Collections.Generic.List[string]'
            foreach ($entry in $archive.Entries) {
                if ($entries.ContainsKey($entry.FullName)) {
                    $duplicates.Add($entry.FullName)
                } else {
                    $entries.Add($entry.FullName, $entry)
                }
            }
            if ($duplicates.Count -gt 0) {
                throw "$name contiene $($duplicates.Count) percorsi ZIP duplicati."
            }
            # Assicura che le due distribuzioni includano l'introduzione
            # approvata (Canvas procedurale + ricomparsa della Home).
            $appPrefix = 'SwiftBAT Explorer/Applicazione/'
            $openingFiles = @(
                'src/main/java/it/casiraghi/swiftbat/ui/BlackHoleOpening.java',
                'src/main/java/it/casiraghi/swiftbat/ui/HomeEntranceReveal.java',
                'src/main/java/it/casiraghi/swiftbat/ui/components/OpeningBlackHolePane.java',
                'src/main/java/it/casiraghi/swiftbat/ui/HomePage.java',
                'src/main/resources/stability-final.css'
            )
            foreach ($required in $openingFiles) {
                if (-not $entries.ContainsKey($appPrefix + $required)) {
                    throw "$name non include l'apertura cinematografica: $required"
                }
            }
            Write-Host "  Apertura cinematografica: [OK]"
            $prefix = 'SwiftBAT Explorer/Applicazione/offline-data/v1/'
            $manifests = @($entries.Keys | Where-Object {
                $_.StartsWith($prefix) -and $_.EndsWith('/manifest.properties')
            })
            $usable = 0
            $asciiOnly = 0
            $fitsOnly = 0
            $both = 0
            foreach ($manifest in $manifests) {
                $folder = $manifest.Substring(0,$manifest.Length - 'manifest.properties'.Length)
                $hasAscii = $entries.ContainsKey($folder + 'ascii-4chan-1s.dat')
                $hasFits  = $entries.ContainsKey($folder + 'fits-1chan-1s.lc')
                if ($hasAscii -or $hasFits) {
                    $usable++
                    if ($hasAscii -and $hasFits) { $both++ }
                    elseif ($hasAscii) { $asciiOnly++ }
                    else { $fitsOnly++ }
                }
            }
            $counts[$name] = $usable
            Write-Host ("{0}: {1} GRB utilizzabili (ASCII+FITS: {2}; solo ASCII: {3}; solo FITS: {4})" -f
                $name,$usable,$both,$asciiOnly,$fitsOnly)
            $scientificPrefix = 'SwiftBAT Explorer/Applicazione/offline-data/scientific/'
            $hasSummary = $entries.ContainsKey($scientificPrefix + 'summary_general.txt')
            $hasRedshift = $entries.ContainsKey($scientificPrefix + 'GRBlist_redshift_BAT.txt')
            Write-Host "  Tabella coordinate NASA: $hasSummary; tabella redshift: $hasRedshift"
            if ($usable -lt $MinimumGrbs) {
                throw ("$name include soltanto $usable GRB: richiesti almeno $MinimumGrbs. " +
                       "Importa prima l'archivio scientifico del Mac nel progetto.")
            }
        } finally { $archive.Dispose() }
    } finally { $stream.Dispose() }
}
if ($counts[$expected[0]] -ne $counts[$expected[1]]) {
    throw 'I pacchetti per Windows e macOS non contengono lo stesso numero di GRB.'
}
Write-Host '[OK] Entrambi i pacchetti includono il numero minimo richiesto di curve offline.' -ForegroundColor Green
