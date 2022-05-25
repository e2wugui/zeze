@echo off
setlocal
pushd %~dp0

echo %~dp0

cd ../test
call build.bat

cd /d %~dp0
dir

mkdir lib
copy ../test/lib/* lib/

pause
