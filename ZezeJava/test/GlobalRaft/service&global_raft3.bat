@echo off
setlocal
pushd %~dp0

start "ServiceManagerServer"   java -Dlogname=ServiceManagerServer   -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.5.6-SNAPSHOT.jar Zeze.Services.ServiceManagerServer
start "GlobalCacheManagerRaft" java -Dlogname=GlobalCacheManagerRaft -cp .;..\..\ZezeJava\lib\*;..\..\ZezeJava\build\libs\ZezeJava-1.5.6-SNAPSHOT.jar Zeze.Services.GlobalCacheManagerServer -raft RunAllNodes
