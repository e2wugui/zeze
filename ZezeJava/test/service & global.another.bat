@echo off
setlocal
pushd %~dp0

set classes=../ZezeJava/build/classes/java/main;../ZezeJava/build/resources/main

start "ServiceManagerServer2" java -Dlogname=ServiceManagerServer2 -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.ServiceManagerServer -port 5011 -autokeys autokeys2
start "GlobalCacheManagerAsyncServer2" java -Dlogname=GlobalCacheManagerAsyncServer2 -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.GlobalCacheManagerAsyncServer -port 5012
