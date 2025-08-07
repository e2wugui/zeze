@echo off
setlocal
pushd %~dp0

set classes=../ZezeJava/build/classes/java/main;../ZezeJava/build/resources/main

start "LoginQueue" java -Dlogname=LoginQueue -cp %classes%;../ZezeJava/lib/*;. Zeze.Services.LoginQueue
