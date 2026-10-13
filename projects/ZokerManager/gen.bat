@echo off
setlocal
pushd %~dp0

..\..\publish\Gen.exe solution.zoker.xml
rem ..\..\Gen\bin\Debug\net10.0\Gen.exe solution.zoker.xml

pause
