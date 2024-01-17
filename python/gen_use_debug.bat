@echo off
setlocal
pushd %~dp0

set PATH=%~dp0\..\Gen\bin\Debug\net8.0;%PATH%

Gen.exe solution.xml

echo -------- Gen done!
pause
