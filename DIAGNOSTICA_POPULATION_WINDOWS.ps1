param(
    [int]$JavaProcessId = 0
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$destination = Join-Path $root ("dist\diagnostics\Population-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
New-Item -ItemType Directory -Path $destination -Force | Out-Null

$jcmd = Get-Command "jcmd.exe" -ErrorAction SilentlyContinue
if ($null -eq $jcmd -and $env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\jcmd.exe"
    if (Test-Path -LiteralPath $candidate) {
        $jcmd = Get-Item -LiteralPath $candidate
    }
}
if ($null -eq $jcmd) {
    Write-Host "[ERRORE] jcmd.exe non trovato. Serve il JDK (non solo il JRE)." -ForegroundColor Red
    Write-Host "Imposta JAVA_HOME al JDK 17+ o inserisci jcmd nel PATH."
    exit 1
}
$command = if ($jcmd -is [System.Management.Automation.CommandInfo]) { $jcmd.Source } else { $jcmd.FullName }

$allJava = @(Get-CimInstance Win32_Process |
    Where-Object { $_.Name -in @("java.exe", "javaw.exe") })
$allJava | Select-Object ProcessId, Name, CommandLine |
    Format-List | Out-File (Join-Path $destination "java-processes.txt") -Encoding utf8

if ($JavaProcessId -le 0) {
    $candidates = @($allJava |
        Where-Object { $_.CommandLine -match "(?i)swiftbat|casiraghi\.swiftbat" } |
        Sort-Object CreationDate -Descending)
    if ($candidates.Count -eq 0) {
        Write-Host "Nessun processo SwiftBAT individuato. Avvia l'app e rilancia lo script." -ForegroundColor Yellow
        Write-Host "Processi Java elencati in: $destination\java-processes.txt"
        & $command -l
        exit 1
    }
    $JavaProcessId = [int]$candidates[0].ProcessId
}

Write-Host "Diagnostica del processo Java PID $JavaProcessId..."
Write-Host "Apri Population Analysis prima di lanciare questo script; acquisisce due snapshot."

for ($i = 1; $i -le 2; $i++) {
    $threadFile = Join-Path $destination ("threads-" + $i + ".txt")
    $heapFile = Join-Path $destination ("heap-" + $i + ".txt")
    & $command $JavaProcessId Thread.print -l 2>&1 | Out-File -FilePath $threadFile -Encoding utf8
    & $command $JavaProcessId GC.heap_info 2>&1 | Out-File -FilePath $heapFile -Encoding utf8
    if ($i -eq 1) { Start-Sleep -Seconds 2 }
}
& $command $JavaProcessId VM.command_line 2>&1 |
    Out-File (Join-Path $destination "vm-options.txt") -Encoding utf8

Write-Host ""
Write-Host "Diagnostica salvata in: $destination" -ForegroundColor Green
Write-Host "Invia threads-1.txt, threads-2.txt e heap-1.txt/heap-2.txt."
Write-Host "I file possono contenere percorsi del tuo computer: controllali prima di condividerli."
