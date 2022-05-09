@echo off
setlocal
pushd %~dp0

java -ea -Dlogname=GlobalRaft -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJava\build\libs\ZezeJava-0.9.22-SNAPSHOT.jar;..\..\ZezeJavaTest\build\libs\ZezeJavaTest-0.9.22-SNAPSHOT.jar GlobalRaft.TestGlobalCacheMgrWithRaft

pause