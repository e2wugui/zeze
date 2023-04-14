@echo off
setlocal
pushd %~dp0

start "ServiceManagerServer" ^
java -Dlogname=service ^
-Xlog:gc=info,gc+heap=info:service_gc.log:time ^
-cp .;lib\* ^
Zeze.Services.ServiceManagerServer ^
-port 5001

start "GlobalCacheManagerAsyncServer" ^
java -Dlogname=global ^
-Xlog:gc=info,gc+heap=info:global_gc.log:time ^
-cp .;lib\* ^
Zeze.Services.GlobalCacheManagerAsyncServer ^
-port 5002
-tryNextSync
