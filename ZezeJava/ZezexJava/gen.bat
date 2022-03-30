@echo off
setlocal
pushd %~dp0

..\..\publish\Gen.exe solution.xml
..\..\publish\Gen.exe solution.linkd.xml

rem ..\..\Gen\bin\Debug\net6.0\Gen.exe solution.xml
rem ..\..\Gen\bin\Debug\net6.0\Gen.exe solution.linkd.xml

pause

