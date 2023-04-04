@echo off
setlocal
pushd %~dp0

set classes=../../ZezeJava/build/classes/java/main
start "Master" java -Dlogname=Master -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.Master.Main

set classes=../../ZezeJava/build/classes/java/main
start "Manager0" java -Dlogname=Manager0 -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.Dbh2Manager manager0 zeze.xml

set classes=../../ZezeJava/build/classes/java/main
start "Manager1" java -Dlogname=Manager1 -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.Dbh2Manager manager1 zeze.xml

set classes=../../ZezeJava/build/classes/java/main
start "Manager2" java -Dlogname=Manager2 -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.Dbh2Manager manager2 zeze.xml

