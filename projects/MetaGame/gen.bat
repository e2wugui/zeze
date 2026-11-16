@echo off
setlocal
for /f "tokens=1,* delims==" %%A in ('findstr /b /c:"zezeVersion=" "%~dp0gradle.properties"') do set "zezeGenVersion=%%B"
if not defined zezeGenVersion (
    echo Missing zezeVersion in "%~dp0gradle.properties". 1>&2
    exit /b 1
)

set "zezeGenDir=%~dp0build\gen\v%zezeGenVersion%"
set "zezeGenExe=%zezeGenDir%\Gen.exe"
if not exist "%zezeGenExe%" (
    if not exist "%zezeGenDir%\" mkdir "%zezeGenDir%"
    call :extractGen
    if errorlevel 1 exit /b 1
)

if not "%~1"=="" goto generate
pushd "%~dp0" || exit /b 1
call :generate solution.xml solution.linkd.xml solution.server.xml
set "genExitCode=%errorlevel%"
popd
exit /b %genExitCode%

:generate
if "%~1"=="" exit /b 0
echo -------- Gen %~1 ...
"%zezeGenExe%" "%~1"
if errorlevel 1 exit /b %errorlevel%
shift
goto generate

:extractGen
set "zezeGenTemp=%zezeGenDir%\Gen.%RANDOM%.%RANDOM%.tmp"
git -C "%~dp0." cat-file blob "refs/tags/v%zezeGenVersion%:publish/Gen.exe" > "%zezeGenTemp%"
if errorlevel 1 (
    if exist "%zezeGenTemp%" del "%zezeGenTemp%"
    echo Gen.exe unavailable for tag v%zezeGenVersion%. Fetch that tag and retry. 1>&2
    exit /b 1
)
move /y "%zezeGenTemp%" "%zezeGenExe%" >nul
exit /b %errorlevel%
