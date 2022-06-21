@echo off
setlocal
pushd %~dp0

java -ea -XX:NativeMemoryTracking=detail -Dlogname=Simulate -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJava\build\libs\ZezeJava-0.10.0-SNAPSHOT.jar;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-0.10.0-SNAPSHOT.jar Infinite.Simulate

pause
