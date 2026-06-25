@echo off
setlocal
pushd %~dp0

echo -------- Gen solution.xml ...
..\zeze\publish\Gen.exe solution.xml

echo -------- Gen solution.linkd.xml ...
..\zeze\publish\Gen.exe solution.linkd.xml

echo -------- Gen solution.server.xml ...
..\zeze\publish\Gen.exe solution.server.xml

pause
