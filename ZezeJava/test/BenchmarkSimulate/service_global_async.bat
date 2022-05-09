@echo off
setlocal
pushd %~dp0

start "ServiceManagerServer"          java -Dlogname=ServiceManagerServer          -cp .;lib\* Zeze.Services.ServiceManagerServer          -port 5001
start "GlobalCacheManagerAsyncServer" java -Dlogname=GlobalCacheManagerAsyncServer -cp .;lib\* Zeze.Services.GlobalCacheManagerAsyncServer -port 5555
