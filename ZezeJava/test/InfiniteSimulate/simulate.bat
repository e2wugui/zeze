@echo off
setlocal
pushd %~dp0

rem -ea -XX:NativeMemoryTracking=detail
java -Dlogname=Simulate -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.0.5-SNAPSHOT.jar;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.0.5-SNAPSHOT.jar Infinite.Simulate

pause
