@echo off
setlocal
pushd %~dp0

cd ../ZezeJavaTest

:redo
java -Dlogname=TestDatabaseHalt -Dloglevel=INFO -cp ../ZezeJavaTest/lib/*;../ZezeJavaTest/build/classes/java/main Temp.TestDatabaseHalt
if %errorlevel% == 1314 goto redo

pause
