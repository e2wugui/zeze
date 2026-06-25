@echo off
setlocal
pushd %~dp0

set classes_servicemanager=../../zeze/ZezeJava/ZezeJava/build/classes/java/main
start "ServiceManagerServer" java -Dlogname=ServiceManagerServer -cp %classes_servicemanager%;../../zeze/ZezeJava/ZezeJava/lib/*;. Zeze.Services.ServiceManagerServer

set classes_global=../../zeze/ZezeJava/ZezeJava/build/classes/java/main
start "GlobalCacheManagerAsyncServer" java -Dlogname=GlobalCacheManagerAsyncServer -cp %classes_global%;../../zeze/ZezeJava/ZezeJava/lib/*;. Zeze.Services.GlobalCacheManagerAsyncServer

rem start ..\gradlew.bat start_linkd
Sleep 1

set classes_linkd=linkd/build/classes/java/main;../../zeze/ZezeJava/ZezeJava/build/classes/java/main
start "linkd" java -Dlogname=linkd -cp .;%classes_linkd%;../../zeze/ZezeJava/ZezeJava/lib/*;. Zege.LinkdProgram

rem start ..\gradlew.bat start_server

set classes_server=server/build/classes/java/main;../../zeze/ZezeJava/ZezeJava/build/classes/java/main
start "server" java -Dlogname=server -cp .;%classes_server%;../../zeze/ZezeJava/ZezeJava/lib/*;. Zege.Program
