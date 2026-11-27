@echo off
setlocal
pushd %~dp0

rem ..\..\Gen\bin\Debug\net8.0\Gen.exe solution.xml
rem ..\..\Gen\bin\Debug\net8.0\Gen.exe solution.linkd.xml
..\..\publish\Gen.exe solution.xml
..\..\publish\Gen.exe solution.linkd.xml

pause

