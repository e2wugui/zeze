@echo off
setlocal
pushd %~dp0

mvn -X clean deploy

pause
