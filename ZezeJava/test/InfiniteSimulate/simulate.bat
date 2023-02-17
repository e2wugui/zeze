@echo off
setlocal
pushd %~dp0

rem -ea -XX:NativeMemoryTracking=detail
java -Dlogname=Simulate -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.1.7-SNAPSHOT.jar;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.1.7-SNAPSHOT.jar Infinite.Simulate

pause
