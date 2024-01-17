@echo off
setlocal
pushd %~dp0

..\Gen\bin\Debug\net8.0\Gen.exe
rem ..\Gen\bin\Debug\net8.0\Gen.exe -c ExportConf -ZezeSrcDir ..

pause
