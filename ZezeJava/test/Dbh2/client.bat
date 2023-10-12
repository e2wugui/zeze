@echo off
setlocal
pushd %~dp0

set classes=.;../../ZezeJava/lib/*;../../ZezeJava/build/classes/java/main;../../ZezeJava/build/resources/main

java -Dlogname=client -Dloglevel=INFO -Xmx4g -cp %classes% Zeze.Dbh2.BenchClient -valueSize 100 -masterPort 10999 -tableNumber 1 -tableAccess 1 -threadNumber 64
