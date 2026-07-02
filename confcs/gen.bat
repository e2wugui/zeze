@echo off
setlocal
pushd %~dp0

..\publish\Gen.exe
rem ..\publish\Gen.exe -c ExportConf -ZezeSrcDir ..

pause
