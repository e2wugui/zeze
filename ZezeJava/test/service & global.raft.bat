@echo off
setlocal
pushd %~dp0

set classes=../ZezeJava/build/classes/java/main;../ZezeJava/build/resources/main

start "ServiceManagerServer" java -Dlogname=ServiceManagerServer -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.ServiceManagerServer
start "GlobalCacheManagerRaft" java -Dlogname=GlobalCacheManagerRaft -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.GlobalCacheManagerServer -raft RunAllNodes -raftConf ./GlobalRaft/global.raft.xml
