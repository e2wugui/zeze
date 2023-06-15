@echo off
setlocal
pushd %~dp0

set classes_servicemanager=../ZezeJava/build/classes/java/main
start "ServiceManagerServer" java -Dlogname=ServiceManagerServer -cp %classes_servicemanager%;../ZezeJava/lib/*;. Zeze.Services.ServiceManagerServer

set classes_global=../ZezeJava/build/classes/java/main
start "GlobalCacheManagerAsyncServer" java -Dlogname=GlobalCacheManagerAsyncServer -cp %classes_global%;../ZezeJava/lib/*;. Zeze.Services.GlobalCacheManagerAsyncServer

rem start ..\gradlew.bat start_linkd

set classes_linkd=linkd/build/classes/java/main;../ZezeJava/build/classes/java/main
start "linkd" java -Dlogname=linkd -cp .;%classes_linkd%;../ZezeJava/lib/*;. Zege.Program

rem start ..\gradlew.bat start_server

set classes_server=server/build/classes/java/main;../ZezeJava/build/classes/java/main
start "server" java -Dlogname=server -cp .;%classes_server%;../ZezeJava/lib/*;. Zege.Program
