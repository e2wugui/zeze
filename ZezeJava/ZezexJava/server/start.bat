@echo off
setlocal
pushd %~dp0

if not exist hot (
	cd ..
	call distribute.bat
	cd server
)
if not exist hotrun (
	move hot hotrun
)

set zeze_classes=../../ZezeJava/build/classes/java/main;../../ZezeJava/build/resources/main

start "server" java -Dlogname=server -cp %zeze_classes%;./hotrun/server.jar;../../ZezeJavaTest/lib/* Program

set linkd_classes=%zeze_classes%;../linkd/build/classes/java/main
start "linkd" java -Dlogname=linkd -cp %linkd_classes%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program

timeout 10
set client_classes=%zeze_classes%;../client/build/classes/java/main
java -Dlogname=client -cp %client_classes%;../../ZezeJavaTest/lib/jgrapht-core-1.5.2.jar;../../ZezeJava/lib/* Program
