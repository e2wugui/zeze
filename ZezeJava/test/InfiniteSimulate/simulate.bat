@echo off
setlocal
pushd %~dp0

rem -ea -XX:NativeMemoryTracking=detail
java -Dlogname=Simulate -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.4.5-SNAPSHOT.jar Infinite.Simulate

pause
