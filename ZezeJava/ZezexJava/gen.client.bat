@echo off
setlocal
pushd %~dp0

..\..\Gen\bin\Debug\net8.0\Gen.exe solution.client.xml

pause

