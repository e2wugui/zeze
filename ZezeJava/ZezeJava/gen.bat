@echo off
setlocal
pushd %~dp0

..\..\publish\Gen.exe solution.zeze.xml

rem ..\..\Gen\bin\Debug\net6.0\Gen.exe solution.zeze.xml

pause
