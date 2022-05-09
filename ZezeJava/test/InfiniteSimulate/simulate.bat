@echo off
setlocal
pushd %~dp0

java -ea -Dlogname=Simulate -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJava\build\libs\ZezeJava-0.9.22-SNAPSHOT.jar;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-0.9.22-SNAPSHOT.jar Infinite.Simulate

pause
