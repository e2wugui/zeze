@echo off
setlocal
pushd %~dp0

set PATH=%~dp0\..\publish;%PATH%

Gen.exe solution.xml

echo -------- Gen done!
pause
