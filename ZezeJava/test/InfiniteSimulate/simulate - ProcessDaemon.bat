@echo off
setlocal
pushd %~dp0

rem -ea -XX:NativeMemoryTracking=detail
java -Dlogname=SimulateWithDaemon -cp .;..\..\ZezeJavaTest\lib\* Zeze.Services.Daemon ^
     -Dlogname=Simulate           -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.5.1-SNAPSHOT.jar Infinite.Simulate

pause
