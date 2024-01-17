@echo off
setlocal
pushd %~dp0

..\..\Gen\bin\Debug\net8.0\Gen.exe world.xml
rem ..\..\Gen\bin\Debug\net8.0\Gen.exe -debug world.xml

pause
