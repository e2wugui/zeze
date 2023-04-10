@echo off
setlocal
pushd %~dp0

rem set classes=../../ZezeJava/build/classes/java/main
rem java -Dlogname=Client -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.BenchClient -masterIp 10.12.7.155 -masterPort 10999 -valueSize 100 -tableNumber 32 -threadNumber 8 -tableAccess 4

java -Dlogname=Client2 -cp .;lib/* Zeze.Dbh2.BenchClient -masterIp 10.12.7.155 -masterPort 10999 -valueSize 100 -tableNumber 32 -threadNumber 8 -tableAccess 4
