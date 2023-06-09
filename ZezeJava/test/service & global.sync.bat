@echo off
setlocal
pushd %~dp0

set classes=../ZezeJava/build/classes/java/main
start "ServiceManagerServer" java -Dlogname=ServiceManagerServer -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.ServiceManagerServer

set classes=../ZezeJava/build/classes/java/main
start "GlobalCacheManagerServer" java -Dlogname=GlobalCacheManagerServer -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.GlobalCacheManagerServer
