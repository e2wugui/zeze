@echo off
setlocal
pushd %~dp0

java -ea -Dlogname=GlobalRaft -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.2.22-SNAPSHOT.jar GlobalRaft.TestGlobalCacheMgrWithRaft

pause
