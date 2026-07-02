@echo off
setlocal
pushd %~dp0

..\..\..\publish\Gen.exe world.xml

pause
