@echo off
setlocal
pushd %~dp0

rem start ..\gradlew.bat start_linkd

set classes_linkd=linkd/build/classes/java/main;../../zeze/ZezeJava/ZezeJava/build/classes/java/main
start "linkd" java -Dlogname=linkd -cp .;%classes_linkd%;../../zeze/ZezeJava/ZezeJava/lib/*;. Zege.LinkdProgram

rem start ..\gradlew.bat start_server

set classes_server=server/build/classes/java/main;../../zeze/ZezeJava/ZezeJava/build/classes/java/main
start "server" java -Dlogname=server -cp .;%classes_server%;../../zeze/ZezeJava/ZezeJava/lib/*;. Zege.Program
