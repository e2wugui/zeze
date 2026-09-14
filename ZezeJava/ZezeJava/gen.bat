@echo off
setlocal
pushd %~dp0

..\..\publish\Gen.exe solution.zeze.xml

rem ..\..\Gen\bin\Debug\net10.0\Gen.exe solution.zeze.xml

pause
