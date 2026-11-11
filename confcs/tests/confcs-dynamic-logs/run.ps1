param([string]$Gradle)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
if (!$Gradle) { $Gradle = "$root/ZezeJava/gradlew.bat" }
Push-Location $PSScriptRoot
try {
    & "$root/publish/Gen.exe" solution.xml
    if ($LASTEXITCODE -ne 0) { throw 'Fixture generation failed' }
    & $Gradle -p "$root/ZezeJava" --no-configuration-cache -I "$PSScriptRoot/fixture.init.gradle" :ZezeJava:writeConfCsChangesFixture
    if ($LASTEXITCODE -ne 0) { throw 'Java fixture failed' }
    dotnet build ConfCsDynamicLogs.csproj -c Release
    if ($LASTEXITCODE -ne 0) { throw 'C# fixture build failed' }
    dotnet bin/Release/net10.0/ConfCsDynamicLogs.dll obj
    if ($LASTEXITCODE -ne 0) { throw 'C# fixture failed' }
} finally {
    Pop-Location
}
