@echo off
setlocal
pushd %~dp0

..\..\..\publish\Gen.exe solution.xml
..\..\..\publish\Gen.exe solution.linkd.xml

pause
