@echo off
setlocal
pushd %~dp0

echo -------- gen link.xml ...
..\..\publish\Gen.exe protocol\link.xml

echo -------- gen server.xml ...
..\..\publish\Gen.exe protocol\server.xml

echo -------- gen client.xml ...
..\..\publish\Gen.exe protocol\client.xml

echo -------- done
pause
