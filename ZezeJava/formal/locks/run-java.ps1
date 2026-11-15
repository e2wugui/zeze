$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9'
& (Join-Path $PSScriptRoot '..\run-gradle.ps1') -GradleArgs @(
    ':ZezeJavaTest:test', '--tests', '*TestFormalLockProtocol',
    '--tests', '*TestFastAdmissionGuard', '--console=plain',
    '--init-script', (Join-Path $PSScriptRoot 'java-evidence.gradle'))
exit $LASTEXITCODE
