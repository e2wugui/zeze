@echo off
setlocal
pushd %~dp0

java -ea -Dlogname=GlobalRaft -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.5.5.jar GlobalRaft.TestGlobalCacheMgrWithRaft

pause
