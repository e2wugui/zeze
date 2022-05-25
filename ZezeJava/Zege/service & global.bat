@echo off
setlocal
pushd %~dp0

set classes=../ZezeJava/build/classes/java/main
start "ServiceManagerServer" java -Dlogname=ServiceManagerServer -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.ServiceManagerServer

set classes=../ZezeJava/build/classes/java/main
start "GlobalCacheManagerAsyncServer" java -Dlogname=GlobalCacheManagerAsyncServer -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.GlobalCacheManagerAsyncServer
