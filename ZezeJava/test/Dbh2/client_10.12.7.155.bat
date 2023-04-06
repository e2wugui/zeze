@echo off
setlocal
pushd %~dp0

set classes=../../ZezeJava/build/classes/java/main
java -Dlogname=Client -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.BenchClient -masterIp 10.12.7.155
