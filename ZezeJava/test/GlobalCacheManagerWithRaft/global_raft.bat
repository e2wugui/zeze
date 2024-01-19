@echo off
setlocal
pushd %~dp0

java -ea -Dlogname=GlobalRaft -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.3.15-SNAPSHOT.jar GlobalRaft.TestGlobalCacheMgrWithRaft

pause
