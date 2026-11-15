$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
$repoRoot = (Resolve-Path (Join-Path $taskRoot '../../..')).Path
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9'
$buildDir = Join-Path $taskRoot 'build'
New-Item -ItemType Directory -Force -Path $buildDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $taskRoot '../record-lifecycle/build') | Out-Null

# Keep XML capture inside the same named mutex. Mutex acquisition is recursive on
# this thread; the shared entry remains the sole entry point that invokes Gradle.
$captureGate = [System.Threading.Mutex]::new($false, 'Local\ZezeJavaFormalGradle')
$captured = $false
try {
    while (-not $captured) {
        try { $captured = $captureGate.WaitOne(1000) }
        catch [System.Threading.AbandonedMutexException] { $captured = $true }
    }
    Push-Location $repoRoot
    try {
        & (Join-Path $taskRoot '../run-gradle.ps1') -GradleArgs @(
            ':ZezeJavaTest:test', '--tests', '*TestFormalRollback',
            '--tests', '*TestFastAdmissionGuard', '--console=plain')
        $resultCode = $LASTEXITCODE
        foreach ($testClass in @('Zeze.Transaction.TestFormalRollback', 'Zeze.TestFastAdmissionGuard')) {
            $xmlPath = Join-Path $repoRoot "ZezeJava/ZezeJavaTest/build/test-results/test/TEST-$testClass.xml"
            if (Test-Path -LiteralPath $xmlPath) {
                Copy-Item -LiteralPath $xmlPath -Destination $buildDir
                Copy-Item -LiteralPath $xmlPath -Destination (Join-Path $taskRoot '../record-lifecycle/build')
            }
        }
        if ($resultCode -ne 0) { throw "Gradle failed: $resultCode" }
        & python (Join-Path $taskRoot 'capture-java.py')
        if ($LASTEXITCODE -ne 0) { throw 'Java XML evidence validation failed' }
    } finally { Pop-Location }
} finally {
    if ($captured) { $captureGate.ReleaseMutex() }
    $captureGate.Dispose()
}
