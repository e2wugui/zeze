@echo off
setlocal
pushd %~dp0

title client

set classes=client/build/classes/java/main;../../zeze/ZezeJava/ZezeJava/build/classes/java/main
java -Dlogname=client -cp %classes%;../../zeze/ZezeJava/ZezeJava/lib/*;. Zege.LinkdProgram
