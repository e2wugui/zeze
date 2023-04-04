@echo off
setlocal
pushd %~dp0

set classes=../../ZezeJava/build/classes/java/main
java -Dlogname=Client -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.Bench.Client
