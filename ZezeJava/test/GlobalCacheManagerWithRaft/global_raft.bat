@echo off
setlocal
pushd %~dp0

java -ea -Dlogname=GlobalRaft -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.1.1-SNAPSHOT.jar;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-1.1.1-SNAPSHOT.jar GlobalRaft.TestGlobalCacheMgrWithRaft

pause