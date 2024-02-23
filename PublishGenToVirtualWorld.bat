@echo off
setlocal
pushd %~dp0

rem del /s/q ..\virtualworld\common\gen\* 2>nul

dotnet publish Gen -c Release -r win-x64 --self-contained -o ..\virtualworld\common\gen
copy /y UnitTest\NLog.config ..\virtualworld\common\gen\

pause
