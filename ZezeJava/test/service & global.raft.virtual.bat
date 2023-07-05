@echo off
setlocal
pushd %~dp0

set classes=../ZezeJava/build/classes/java/main;../ZezeJava/build/resources/main

start "ServiceManagerServer" java -Dlogname=ServiceManagerServer -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.ServiceManagerServer
start "GlobalCacheManagerRaft" "C:\Program Files\Eclipse Adoptium\jdk-19.0.0.30-hotspot\bin\java" -Dlogname=GlobalCacheManagerRaft --enable-preview -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.GlobalCacheManagerServer -raft RunAllNodes
