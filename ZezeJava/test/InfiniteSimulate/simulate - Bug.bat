@echo off
setlocal
pushd %~dp0

rem -ea -XX:NativeMemoryTracking=detail
java -Dlogname=TwoTestBug -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.2.12-SNAPSHOT.jar;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.2.12-SNAPSHOT.jar Infinite.TwoTestBug

pause
