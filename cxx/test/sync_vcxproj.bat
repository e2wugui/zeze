@echo off
setlocal
pushd %~dp0

..\..\ZezeJava\test\InfiniteSimulate\luajit.exe sync_vcxproj.lua

pause
