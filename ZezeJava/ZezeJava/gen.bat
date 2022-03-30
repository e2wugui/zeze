@echo off
setlocal
pushd %~dp0

rem ..\..\zeze\publish\Gen.exe solution.zeze.xml

..\..\zeze\Gen\bin\Debug\net6.0\Gen.exe solution.zeze.xml

pause
