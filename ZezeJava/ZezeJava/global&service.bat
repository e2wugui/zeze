@echo off
setlocal
pushd %~dp0

start ..\gradlew.bat startServiceManager
start ..\gradlew.bat startGlobalCacheManagerAsync
rem start ..\gradlew.bat startGlobalCacheManager

rem start "ServiceManagerServer"          java -Dlogname=ServiceManagerServer          -cp lib\*;build\classes\java\main;build\resources\main Zeze.Services.ServiceManagerServer
rem start "GlobalCacheManagerServer"      java -Dlogname=GlobalCacheManagerServer      -cp lib\*;build\classes\java\main;build\resources\main Zeze.Services.GlobalCacheManagerServer
rem start "GlobalCacheManagerServer"      java -Dlogname=GlobalCacheManagerServer      -cp lib\*;build\classes\java\main;build\resources\main Zeze.Services.GlobalCacheManagerServer -raft RunAllNodes
rem start "GlobalCacheManagerAsyncServer" java -Dlogname=GlobalCacheManagerAsyncServer -cp lib\*;build\classes\java\main;build\resources\main Zeze.Services.GlobalCacheManagerAsyncServer
