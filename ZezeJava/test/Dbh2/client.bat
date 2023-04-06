@echo off
setlocal
pushd %~dp0

set classes=../../ZezeJava/build/classes/java/main
java -Dlogname=Client -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.BenchClient -valueSize 100 -masterPort 11000
