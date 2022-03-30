@echo off
setlocal
pushd %~dp0

..\..\zeze\publish\Gen.exe solution.xml
..\..\zeze\publish\Gen.exe solution.linkd.xml

rem ..\..\zeze\Gen\bin\Debug\net6.0\Gen.exe solution.xml
rem ..\..\zeze\Gen\bin\Debug\net6.0\Gen.exe solution.linkd.xml

pause

