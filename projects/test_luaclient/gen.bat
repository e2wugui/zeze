@echo off
setlocal
pushd %~dp0

rem NativeAOT 原生单文件版（由根 publish/ 提供）
..\..\publish\Gen.exe

pause
