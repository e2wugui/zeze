@echo off
setlocal
pushd %~dp0

..\..\zeze\publish\Gen.exe -c ExportZezex -Lang java -ClientPlatform cs+lua -SolutionName wm

pause
