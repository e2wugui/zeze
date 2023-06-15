@echo off
setlocal
pushd %~dp0

rem start ..\gradlew.bat start_linkd

set classes_linkd=linkd/build/classes/java/main;../ZezeJava/build/classes/java/main
start "linkd" java -Dlogname=linkd -cp .;%classes_linkd%;../ZezeJava/lib/*;. Zege.Program

rem start ..\gradlew.bat start_server

set classes_server=server/build/classes/java/main;../ZezeJava/build/classes/java/main
start "server" java -Dlogname=server -cp .;%classes_server%;../ZezeJava/lib/*;. Zege.Program
