@echo off
setlocal
pushd %~dp0

rem -ea -XX:NativeMemoryTracking=detail
java -Dlogname=SimulateWithDaemon -cp .;..\..\ZezeJavaTest\lib\* Zeze.Services.Daemon ^
     java -Dlogname=Simulate      -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJavaTest\build\classes\java\test;..\..\ZezeJavaTest\build\resources\test Infinite.Simulate

pause
