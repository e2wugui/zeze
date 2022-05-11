@echo off
setlocal
pushd %~dp0

start "ServiceManagerServer" ^
java -Dlogname=ServiceManagerServer ^
-Xlog:gc=info,gc+heap=info:ServiceManagerServer_gc.log:time ^
-cp .;lib\* ^
Zeze.Services.ServiceManagerServer ^
-port 5001

start "GlobalCacheManagerAsyncServer" ^
java -Dlogname=GlobalCacheManagerAsyncServer ^
-Xlog:gc=info,gc+heap=info:GlobalCacheManagerAsyncServer_gc.log:time ^
-cp .;lib\* ^
Zeze.Services.GlobalCacheManagerAsyncServer ^
-port 5555
-tryNextSync
