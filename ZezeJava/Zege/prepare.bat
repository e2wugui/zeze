@echo off
setlocal
pushd %~dp0

echo ===================================
echo       可能需要按几次任意键!
echo ===================================

cd ../test
call build.bat

cd /d %~dp0
dir

mkdir lib
xcopy /Y ..\ZezeJava\lib lib

pause
