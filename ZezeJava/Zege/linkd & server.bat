@echo off
setlocal
pushd %~dp0

rem start ..\gradlew.bat start_linkd

set classes=linkd/build/classes/java/main;../ZezeJava/build/classes/java/main
start "linkd" java -Dlogname=linkd -cp %classes%;../ZezeJava/lib/*;. Zege.Program

rem start ..\gradlew.bat start_server

set classes=server/build/classes/java/main;../ZezeJava/build/classes/java/main
start "server" java -Dlogname=server -cp %classes%;../ZezeJava/lib/*;. Zege.Program
