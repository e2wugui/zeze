@echo off
setlocal
pushd %~dp0

call gradlew.bat build copyJar

pause
