param(
    [switch]$PrepareOnly
)

# Windows PowerShell 5.1, disponibile di serie in Windows 10 e 11.
# Non usa Java e Maven di sistema; runtime, librerie e log restano accanto all'app.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$cacheRoot = Join-Path $scriptDir '.swiftbat-runtime'
$jdkRoot = Join-Path $cacheRoot 'jdk-17'
$mavenVersion = '3.9.16'
$mavenHome = Join-Path $cacheRoot ("apache-maven-" + $mavenVersion)
$m2Repo = Join-Path $cacheRoot 'm2-repository'
$logDir = Join-Path $cacheRoot 'logs'
$tmpRoot = Join-Path $cacheRoot 'tmp'
$logFile = Join-Path $logDir 'expert-launcher.log'
$transcribing = $false

function Get-Download {
    param([string]$Url, [string]$Destination)
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        try {
            Invoke-WebRequest -Uri $Url -OutFile $Destination -UseBasicParsing -MaximumRedirection 10
            return
        } catch {
            if ($attempt -eq 3) { throw }
            Write-Host ("Download fallito; riprovo ({0}/3)..." -f $attempt)
            Start-Sleep -Seconds (2 * $attempt)
        }
    }
}

function Install-Jdk {
    Write-Host '[1/3] Scarico Eclipse Temurin JDK 17 per Windows x64...'
    $tmp = Join-Path $tmpRoot ("swiftbat-jdk-" + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $tmp -Force | Out-Null
    try {
        $zip = Join-Path $tmp 'temurin17.zip'
        $unpack = Join-Path $tmp 'unpack'
        $url = 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse'
        Get-Download -Url $url -Destination $zip
        Expand-Archive -LiteralPath $zip -DestinationPath $unpack -Force
        $candidates = @(Get-ChildItem -LiteralPath $unpack -Directory |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe') -PathType Leaf })
        if ($candidates.Count -ne 1) {
            throw 'Archivio Temurin inatteso: impossibile individuare il JDK.'
        }
        $versionOutput = & (Join-Path $candidates[0].FullName 'bin\java.exe') --version | Out-String
        if ($LASTEXITCODE -ne 0 -or $versionOutput -notmatch '(?m)^openjdk 17[\. ]') {
            throw 'Il JDK estratto non e'' Java 17 funzionante.'
        }
        if (Test-Path -LiteralPath $jdkRoot) {
            Remove-Item -LiteralPath $jdkRoot -Recurse -Force
        }
        Move-Item -LiteralPath $candidates[0].FullName -Destination $jdkRoot
    } finally {
        Remove-Item -LiteralPath $tmp -Recurse -Force -ErrorAction SilentlyContinue
    }
}

function Install-Maven {
    Write-Host ("[2/3] Scarico Apache Maven {0}..." -f $mavenVersion)
    $tmp = Join-Path $tmpRoot ("swiftbat-maven-" + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $tmp -Force | Out-Null
    try {
        $filename = "apache-maven-$mavenVersion-bin.zip"
        $zip = Join-Path $tmp $filename
        $shaFile = "$zip.sha512"
        $locations = @(
            "https://dlcdn.apache.org/maven/maven-3/$mavenVersion/binaries",
            "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries"
        )
        $downloaded = $false
        foreach ($base in $locations) {
            try {
                Get-Download -Url "$base/$filename" -Destination $zip
                Get-Download -Url "$base/$filename.sha512" -Destination $shaFile
                $expected = ((Get-Content -LiteralPath $shaFile -Raw).Trim() -split '\s+')[0].ToUpperInvariant()
                $actual = (Get-FileHash -LiteralPath $zip -Algorithm SHA512).Hash.ToUpperInvariant()
                if ($expected -notmatch '^[A-F0-9]{128}$' -or $actual -ne $expected) {
                    throw 'Checksum SHA-512 Maven non valido.'
                }
                $downloaded = $true
                break
            } catch {
                Write-Host ("Mirror non disponibile o archivio non integro: {0}" -f $base)
            }
        }
        if (-not $downloaded) {
            throw 'Impossibile scaricare e verificare Apache Maven.'
        }
        $unpack = Join-Path $tmp 'unpack'
        Expand-Archive -LiteralPath $zip -DestinationPath $unpack -Force
        $extracted = Join-Path $unpack ("apache-maven-" + $mavenVersion)
        if (-not (Test-Path -LiteralPath (Join-Path $extracted 'bin\mvn.cmd') -PathType Leaf)) {
            throw 'Archivio Maven incompleto.'
        }
        if (Test-Path -LiteralPath $mavenHome) {
            Remove-Item -LiteralPath $mavenHome -Recurse -Force
        }
        Move-Item -LiteralPath $extracted -Destination $mavenHome
    } finally {
        Remove-Item -LiteralPath $tmp -Recurse -Force -ErrorAction SilentlyContinue
    }
}

try {
    if ($env:OS -ne 'Windows_NT') {
        throw 'Questo launcher funziona soltanto su Windows.'
    }
    if (-not [Environment]::Is64BitOperatingSystem) {
        throw 'E'' richiesto Windows a 64 bit.'
    }
    $nativeArch = if ($env:PROCESSOR_ARCHITEW6432) { $env:PROCESSOR_ARCHITEW6432 } else { $env:PROCESSOR_ARCHITECTURE }
    if ($nativeArch -ne 'AMD64') {
        throw "Architettura $nativeArch non supportata: usare Windows x64 (Intel/AMD)."
    }
    if (-not (Test-Path -LiteralPath (Join-Path $scriptDir 'pom.xml') -PathType Leaf) -or
        -not (Test-Path -LiteralPath (Join-Path $scriptDir 'src\main\java') -PathType Container)) {
        throw 'Pacchetto incompleto: mancano pom.xml o src\main\java.'
    }

    New-Item -ItemType Directory -Path $cacheRoot, $m2Repo, $logDir, $tmpRoot -Force | Out-Null
    $env:TEMP = $tmpRoot
    $env:TMP = $tmpRoot
    Start-Transcript -Path $logFile -Append | Out-Null
    $transcribing = $true
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

    Write-Host '============================================================'
    Write-Host ' SwiftBAT Explorer 1.3.0 - ambiente Windows isolato'
    Write-Host '============================================================'
    Write-Host 'Non servono Java, Maven, GitHub o privilegi amministrativi.'
    Write-Host ("Ambiente nella cartella applicazione: {0}" -f $cacheRoot)
    Write-Host ''

    $javaExe = Join-Path $jdkRoot 'bin\java.exe'
    $needsJdk = -not (Test-Path -LiteralPath $javaExe -PathType Leaf)
    if (-not $needsJdk) {
        try {
            $existingVersion = & $javaExe --version | Out-String
            $needsJdk = ($LASTEXITCODE -ne 0 -or $existingVersion -notmatch '(?m)^openjdk 17[\. ]')
        } catch {
            $needsJdk = $true
        }
    }
    if ($needsJdk) { Install-Jdk } else { Write-Host '[1/3] Java 17 gia'' disponibile nella cartella dell'app.' }

    $env:JAVA_HOME = $jdkRoot
    $env:PATH = (Join-Path $jdkRoot 'bin') + ';' + $env:PATH
    $env:MAVEN_OPTS = '-Dfile.encoding=UTF-8'
    & $javaExe --version
    if ($LASTEXITCODE -ne 0) { throw 'Java 17 non si avvia.' }

    $mavenCmd = Join-Path $mavenHome 'bin\mvn.cmd'
    if (-not (Test-Path -LiteralPath $mavenCmd -PathType Leaf)) {
        Install-Maven
    } else {
        Write-Host ("[2/3] Maven {0} gia' disponibile nella cartella dell'app." -f $mavenVersion)
    }
    & $mavenCmd -version
    if ($LASTEXITCODE -ne 0) { throw 'Maven non si avvia.' }

    if ($PrepareOnly) {
        Write-Host 'Preparazione JDK + Maven completata (test senza interfaccia grafica).'
    } else {
        Write-Host ''
        Write-Host '[3/3] Compilo e avvio SwiftBAT Explorer...'
        Write-Host 'Al primo avvio saranno scaricate le librerie JavaFX necessarie.'
        Push-Location $scriptDir
        try {
            & $mavenCmd '--batch-mode' "-Dmaven.repo.local=$m2Repo" '-DskipTests' 'compile' 'javafx:run'
            if ($LASTEXITCODE -ne 0) { throw "Maven ha restituito il codice $LASTEXITCODE." }
        } finally {
            Pop-Location
        }
    }
} catch {
    Write-Host ''
    Write-Host ('[ERRORE] ' + $_.Exception.Message) -ForegroundColor Red
    Write-Host ('Log diagnostico: ' + $logFile)
    exit 1
} finally {
    if ($transcribing) { Stop-Transcript | Out-Null }
}
