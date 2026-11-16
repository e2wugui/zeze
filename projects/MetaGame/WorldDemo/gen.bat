@echo off
setlocal
pushd "%~dp0" || exit /b 1
call "%~dp0..\gen.bat" world.xml
set "genExitCode=%errorlevel%"
popd
exit /b %genExitCode%
