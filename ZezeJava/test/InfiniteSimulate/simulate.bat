@echo off
setlocal
pushd %~dp0

rem -ea -XX:NativeMemoryTracking=detail
java -Dlogname=Simulate -DuseUnlimitedVirtualThread=false -Dloglevel=INFO -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-2.0.1-SNAPSHOT.jar Infinite.Simulate

pause
