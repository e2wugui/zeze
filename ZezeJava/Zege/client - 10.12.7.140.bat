@echo off
setlocal
pushd %~dp0

title client

set classes=client/build/classes/java/main;../ZezeJava/build/classes/java/main
java -Dlogname=client -cp %classes%;../ZezeJava/lib/*;. Zege.Program 10.12.7.140:5100 10.12.7.140:5103

