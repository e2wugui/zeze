@echo off
setlocal
pushd %~dp0

rem -ea -XX:NativeMemoryTracking=detail
java -Dlogname=Simulate -DuseUnlimitedVirtualThread=false -Dloglevel=INFO -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.4.12.jar Infinite.Simulate

pause
