@echo off
setlocal
pushd %~dp0

rem -ea -XX:NativeMemoryTracking=detail
java -Dlogname=SimulateWithDaemon -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.0.9-SNAPSHOT.jar Zeze.Services.Daemon ^
     -Dlogname=Simulate           -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.0.9-SNAPSHOT.jar;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.0.9-SNAPSHOT.jar Infinite.Simulate

pause
