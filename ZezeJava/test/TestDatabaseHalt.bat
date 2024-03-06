@echo off
setlocal
pushd %~dp0

:redo
java -Dlogname=TestDatabaseHalt -cp ../ZezeJavaTest/lib/*;../ZezeJavaTest/build/classes/java/main Temp.TestDatabaseHalt
if %errorlevel% == 1314 goto redo

pause
