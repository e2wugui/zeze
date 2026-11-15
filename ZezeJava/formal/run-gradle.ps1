param(
    [Parameter(Mandatory = $true)]
    [string[]] $GradleArgs
)

# The parallel verification chats share one checkout and Gradle output directories.
$ErrorActionPreference = 'Stop'
$gate = [System.Threading.Mutex]::new($false, 'Local\ZezeJavaFormalGradle')
$acquired = $false
$pushed = $false
try {
    $waited = 0
    while (-not $acquired) {
        try { $acquired = $gate.WaitOne(1000) }
        catch [System.Threading.AbandonedMutexException] { $acquired = $true }
        if (-not $acquired) {
            $waited++
            if ($waited % 30 -eq 0) { Write-Host "Waiting for shared Gradle outputs ($waited seconds)." }
        }
    }
    Push-Location (Split-Path -Parent $PSScriptRoot)
    $pushed = $true
    & .\gradlew.bat @GradleArgs
    $gradleExit = $LASTEXITCODE
}
finally {
    if ($pushed) { Pop-Location }
    if ($acquired) { $gate.ReleaseMutex() }
    $gate.Dispose()
}
exit $gradleExit
