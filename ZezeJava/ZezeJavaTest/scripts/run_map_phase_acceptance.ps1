#requires -Version 7.0
<#
.SYNOPSIS
Run one fixed Map2 acceptance stage and preserve its raw output.
.EXAMPLE
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
./run_map_phase_acceptance.ps1 -Root D:\work\mygit\zeze\ZezeJava -Tag final-matrix-a1 -Stage Matrix
.NOTES
Use a fresh Tag for each invocation. Run revisions/stages serially on the same machine and JDK.
Matrix is a one-fork screening run; combine it with independent fork/steady-state repeats.
JMH fail-fast (-foe true) is configured by the repository's current ZezeJavaTest/build.gradle.
Completion confirms command exit status only, not performance acceptance or equivalence.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string] $Root,

    [Parameter(Mandatory)]
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9._-]*$')]
    [string] $Tag,

    [Parameter(Mandatory)]
    [ValidateSet('Functional', 'Matrix', 'Multi', 'Macro')]
    [string] $Stage,

    [string] $GradleExecutable
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

$rootItem = Get-Item -LiteralPath $Root
if (-not $rootItem.PSIsContainer -or $rootItem.PSProvider.Name -ne 'FileSystem') {
    throw "Root must be the ZezeJava filesystem directory: $Root"
}
$resolvedRoot = $rootItem.FullName
if (-not (Test-Path -LiteralPath (Join-Path $resolvedRoot 'ZezeJavaTest/build.gradle') -PathType Leaf)) {
    throw "Root does not contain ZezeJavaTest/build.gradle: $resolvedRoot"
}
if ([string]::IsNullOrWhiteSpace($GradleExecutable)) {
    $GradleExecutable = Join-Path $resolvedRoot 'gradlew.bat'
} elseif (-not [System.IO.Path]::IsPathRooted($GradleExecutable)) {
    $GradleExecutable = Join-Path $resolvedRoot $GradleExecutable
}
if (-not (Test-Path -LiteralPath $GradleExecutable -PathType Leaf)) {
    throw "Gradle executable does not exist: $GradleExecutable"
}
$resolvedGradle = (Get-Item -LiteralPath $GradleExecutable).FullName
if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME) -or
        -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe') -PathType Leaf)) {
    throw 'Set JAVA_HOME to the intended JDK before running this script.'
}

$resultsDirectory = Join-Path $resolvedRoot 'ZezeJavaTest/bench-results'
$logPath = Join-Path $resultsDirectory "$Tag.log"
foreach ($existing in @($logPath, (Join-Path $resultsDirectory $Tag), (Join-Path $resultsDirectory "jmh-$Tag.json"))) {
    if (Test-Path -LiteralPath $existing) {
        throw "Tag already has output; use a fresh Tag to preserve raw results: $existing"
    }
}
New-Item -ItemType Directory -Path $resultsDirectory -Force | Out-Null
@(
    "Stage: $Stage"
    "Tag: $Tag"
    "Root: $resolvedRoot"
    "Gradle: $resolvedGradle"
    "JAVA_HOME: $env:JAVA_HOME"
    "Started: $([DateTimeOffset]::Now.ToString('o'))"
    'Profile: performance heap=2g, backend=ktt; JMH=-da; Macro=-ea.'
    'Exit status is not a performance verdict. Raw JSON remains in bench-results.'
) | Set-Content -LiteralPath $logPath -Encoding utf8

function Show-LogTail {
    Get-Content -LiteralPath $logPath -Tail 50 | ForEach-Object { Write-Host $_ }
}

function Invoke-LoggedGradle {
    param([Parameter(Mandatory)][string[]] $TaskArguments)

    # Each array element is passed as one argument. Do not Invoke-Expression or build a shell command.
    $gradleArguments = @('--offline', '--no-parallel', '--console=plain') + $TaskArguments
    "Arguments: $($gradleArguments | ConvertTo-Json -Compress)" | Add-Content -LiteralPath $logPath
    Write-Host "Running $Tag ($Stage): $($TaskArguments[0]); log: $logPath"
    & $resolvedGradle @gradleArguments 2>&1 | Out-File -FilePath $logPath -Append -Encoding utf8
    $gradleExitCode = $LASTEXITCODE
    if ($gradleExitCode -ne 0) {
        throw "Gradle exited $gradleExitCode during $Stage; log: $logPath"
    }
}

function Copy-ValidationXml {
    param(
        [Parameter(Mandatory)]
        [ValidateSet('test', 'integrationTest', 'benchCore', 'benchGame')]
        [string] $TaskName
    )

    $sourceDirectory = Join-Path $resolvedRoot "ZezeJavaTest/build/test-results/$TaskName"
    if (-not (Test-Path -LiteralPath $sourceDirectory -PathType Container)) {
        throw "Missing validation XML directory for ${TaskName}: $sourceDirectory"
    }
    $xmlFiles = @(Get-ChildItem -LiteralPath $sourceDirectory -Filter 'TEST-*.xml' -File)
    if ($xmlFiles.Count -eq 0) {
        throw "No TEST-*.xml validation results for ${TaskName}: $sourceDirectory"
    }
    $destination = Join-Path $resultsDirectory "$Tag/_validation/$TaskName"
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
    foreach ($xmlFile in $xmlFiles) {
        Copy-Item -LiteralPath $xmlFile.FullName -Destination (Join-Path $destination $xmlFile.Name)
    }
    "Validation XML: $($xmlFiles.Count) file(s) from $TaskName copied to $destination" |
        Add-Content -LiteralPath $logPath
}

$jmhProfile = @(
    ':ZezeJavaTest:jmh'
    "-PbenchTag=$Tag"
    '-PjmhWi=5'
    '-PjmhI=5'
    '-PjmhW=1s'
    '-PjmhR=1s'
    '-PjmhJvmArgsAppend=-Xms2g -Xmx2g -da -Dzeze.collections.backend=ktt'
)

Push-Location -LiteralPath $resolvedRoot
try {
    switch ($Stage) {
        'Functional' {
            Invoke-LoggedGradle -TaskArguments @(
                ':ZezeJavaTest:test'
                '--rerun'
            )
            Copy-ValidationXml -TaskName test
            Invoke-LoggedGradle -TaskArguments @(
                ':ZezeJavaTest:integrationTest'
                '--rerun'
            )
            Copy-ValidationXml -TaskName integrationTest
            Invoke-LoggedGradle -TaskArguments @(':ZezeJavaTest:compileJmhJava')
        }
        'Matrix' {
            # Current default parameters: 32 MapBean rows + 8 GTable rows. This is screening, not a fork repeat.
            Invoke-LoggedGradle -TaskArguments ($jmhProfile + @(
                '-PjmhFilter=CommitBench'
                '-PjmhFork=1'
                '-PjmhExtraArgs=-prof gc'
            ))
        }
        'Multi' {
            Invoke-LoggedGradle -TaskArguments ($jmhProfile + @(
                '-PjmhFilter=MapBeanCommitBench'
                '-PjmhFork=2'
                '-PjmhExtraArgs=-prof gc -p changeCount=64 -p entries=1024 -p consumers=HISTORY_CAPTURE,BOTH'
            ))
        }
        'Macro' {
            $macroProfile = @(
                "-PbenchTag=$Tag"
                '-PbenchHeap=2g'
                '-PcollBackend=ktt'
                '-PbenchJvmExtra=-Xms2g -ea -DuseVirtualThread=true -Dzeze.collections.backend=ktt'
                '--rerun'
            )
            # Separate invocations make each --tests unambiguously local to its own Test task.
            Invoke-LoggedGradle -TaskArguments (@(
                ':ZezeJavaTest:benchCore', '--tests', '*DBasicContainerTransaction'
            ) + $macroProfile)
            Copy-ValidationXml -TaskName benchCore
            Invoke-LoggedGradle -TaskArguments (@(
                ':ZezeJavaTest:benchGame', '--tests', '*LMmoGameSim'
                '-PgamePlayers=5000'
                '-PgameK=10'
                '-PgameMinutes=10'
                '-PgameWarmups=2'
                '-PgameRounds=3'
                '-PgameCheckpointPeriodMs=30000'
            ) + $macroProfile)
            Copy-ValidationXml -TaskName benchGame
        }
    }
} catch {
    "Failed: $($_.Exception.Message)" | Add-Content -LiteralPath $logPath
    Show-LogTail
    throw
} finally {
    Pop-Location
}

"Finished: $([DateTimeOffset]::Now.ToString('o'))" | Add-Content -LiteralPath $logPath
Write-Host "Completed $Tag; log: $logPath"
