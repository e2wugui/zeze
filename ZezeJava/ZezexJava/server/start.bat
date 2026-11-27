@echo off
setlocal
pushd %~dp0

set zeze_classes=../../ZezeJava/build/classes/java/main;../../ZezeJava/build/resources/main

set server_classes=%zeze_classes%;./build/classes/java/main
start "server" java -Dlogname=server -cp %server_classes%;../../ZezeJavaTest/lib/* Program

set linkd_classes=%zeze_classes%;../linkd/build/classes/java/main
start "linkd" java -Dlogname=linkd -cp %linkd_classes%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program

timeout 10
set client_classes=%zeze_classes%;../client/build/classes/java/main
java -Dlogname=client -cp %client_classes%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program
