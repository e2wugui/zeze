@echo off
setlocal
pushd %~dp0

rem set classes=.;../../ZezeJava/build/classes/java/main;../../ZezeJava/lib/*
rem java -Dlogname=client -cp %classes% Zeze.Dbh2.BenchClient -masterIp 10.12.7.155 -masterPort 10999 -valueSize 100 -tableNumber 32 -threadNumber 8 -tableAccess 4

java -Dlogname=client2 -cp .;lib/* Zeze.Dbh2.BenchClient -masterIp 10.12.7.155 -masterPort 10999 -valueSize 100 -tableNumber 32 -threadNumber 8 -tableAccess 4
