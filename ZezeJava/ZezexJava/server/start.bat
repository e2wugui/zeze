
@echo off
setlocal
pushd %~dp0

set zeze_classes=../../ZezeJava/build/classes/java/main;../../ZezeJava/build/resources/main

start "server" java -cp %zeze_classes%;./hotrun/server.jar;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program

set linkd_classes=%zeze_classes%;../linkd/build/classes/java/main
start "linkd" java -cp %linkd_classes%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program

timeout 10
set client_classes=%zeze_classes%;../client/build/classes/java/main
java -cp %client_classes%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program
