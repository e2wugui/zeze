@echo off
setlocal
pushd %~dp0

luajit.exe change_solution_name.lua %*

pause
