@echo off
setlocal
pushd %~dp0

rem del /s/q publish\* 2>nul

dotnet publish Gen -c Release -r win-x64 --self-contained -o publish
copy /y UnitTest\NLog.config publish\

pause
