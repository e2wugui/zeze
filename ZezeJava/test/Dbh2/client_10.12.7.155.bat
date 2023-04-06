@echo off
setlocal
pushd %~dp0

set classes=../../ZezeJava/build/classes/java/main
java -Dlogname=Client -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.BenchClient -masterIp 10.12.7.155 -masterPort 11000 -valueSize 100 -tableNumber 32 -threadNumber 8 -tableAccess 4
