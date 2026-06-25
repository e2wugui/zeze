@echo off
setlocal
pushd %~dp0

..\..\zeze\Gen\bin\Debug\net8.0\Gen.exe solution.xml
..\..\zeze\Gen\bin\Debug\net8.0\Gen.exe solution.linkd.xml

pause
