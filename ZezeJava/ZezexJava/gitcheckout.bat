@echo off
setlocal
pushd %~dp0

cd "%1"

set tag=%2

if "%tag%" == "" (
	for /f %%x in ('"git describe --tags --abbrev=0"') do set tag=%%x
)

if "%tag%" == "" goto ERROREXIT

git checkout "%tag%"
if NOT errorlevel 0 goto ERROREXIT

exit /B 0

:ERROREXIT

echo ERROR

exit /B 1
