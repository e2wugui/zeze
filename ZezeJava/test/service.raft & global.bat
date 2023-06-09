@echo off
setlocal
pushd %~dp0

set classes=../ZezeJava/build/classes/java/main
start "ServiceManagerServerRaft" java -Dlogname=ServiceManagerServerRaft -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.ServiceManagerServer -raft RunAllNodes

set classes=../ZezeJava/build/classes/java/main
start "GlobalCacheManagerAsyncServer" java -Dlogname=GlobalCacheManagerAsyncServer -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.GlobalCacheManagerAsyncServer
