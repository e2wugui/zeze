@echo off
setlocal
pushd %~dp0

start ..\gradlew.bat startServiceManager
start ..\gradlew.bat startGlobalCacheManager

rem start "ServiceManagerServer"     java -Dlogname=ServiceManagerServer     -cp .;lib\*;build\classes\java\main Zeze.Services.ServiceManagerServer
rem start "GlobalCacheManagerServer" java -Dlogname=GlobalCacheManagerServer -cp .;lib\*;build\classes\java\main Zeze.Services.GlobalCacheManagerServer
rem start "GlobalCacheManagerServer" java -Dlogname=GlobalCacheManagerServer -cp .;lib\*;build\classes\java\main Zeze.Services.GlobalCacheManagerServer -raft RunAllNodes
