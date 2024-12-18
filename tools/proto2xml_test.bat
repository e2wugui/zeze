@echo off
setlocal
pushd %~dp0

chcp 65001 >nul

luajit proto2xml.lua -solutionName package0 -moduleId 123 proto2xml_test1.proto proto2xml_test1.xml

echo.
pause
