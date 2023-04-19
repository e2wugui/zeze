@echo off
setlocal
pushd %~dp0

set classes=.;../../ZezeJava/build/classes/java/main;../../ZezeJava/lib/*

java -Dlogname=client -cp %classes% Zeze.Dbh2.BenchClient -valueSize 100 -masterPort 10999 -tableNumber 1 -tableAccess 1 -threadNumber 16
