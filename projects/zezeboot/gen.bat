@echo off
setlocal
pushd %~dp0

set PATH=%~dp0\gen;%~dp0\..\..\publish;%PATH%

echo -------- gen link.xml ...
Gen.exe protocol\link.xml

echo -------- gen server.xml ...
Gen.exe protocol\server.xml

echo -------- gen client.xml ...
Gen.exe protocol\client.xml

echo -------- done
pause
