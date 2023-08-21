
@echo off
setlocal
pushd %~dp0

set zeze_classes=../../ZezeJava/build/classes/java/main;../../ZezeJava/build/resources/main

start java -cp %zeze_classes%;./hotrun/server.jar;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program

set linkd_classes=%zeze_classes%;../linkd/build/classes/java/main
start java -cp %linkd_classes%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program

set client_classes=%zeze_classes%;../client/build/classes/java/main
java -cp %client_classes%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program
