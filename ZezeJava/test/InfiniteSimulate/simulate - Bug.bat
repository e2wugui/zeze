@echo off
setlocal
pushd %~dp0

rem -ea -XX:NativeMemoryTracking=detail
java -Dlogname=TwoTestBug -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.0.10-SNAPSHOT.jar;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.0.10-SNAPSHOT.jar Infinite.TwoTestBug

pause
