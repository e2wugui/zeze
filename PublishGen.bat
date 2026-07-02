@echo off
setlocal
pushd %~dp0

del /s/q publish\* 2>nul

dotnet publish Gen -c Release -r win-x64 -o publish
del publish\Gen.pdb 2>nul

pause
