@echo off
setlocal
pushd %~dp0

luajit.exe analyse_threads.lua

pause
