@echo off
setlocal
pushd %~dp0

cd ..
call gradlew.bat build copyJar :ZezeJavaTest:copyTestLib

pause
