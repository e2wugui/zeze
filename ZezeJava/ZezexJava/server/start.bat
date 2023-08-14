
@echo off
setlocal
pushd %~dp0

set classes=../../ZezeJava/build/classes/java/main;../../ZezeJava/build/resources/main

java -cp %classes%;./hotrun/server.jar;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program
