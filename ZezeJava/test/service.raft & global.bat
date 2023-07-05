@echo off
setlocal
pushd %~dp0

set classes=../ZezeJava/build/classes/java/main;../ZezeJava/build/resources/main

start "ServiceManagerServerRaft" java -Dlogname=ServiceManagerServerRaft -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.ServiceManagerServer -raft RunAllNodes
start "GlobalCacheManagerAsyncServer" java -Dlogname=GlobalCacheManagerAsyncServer -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.GlobalCacheManagerAsyncServer
