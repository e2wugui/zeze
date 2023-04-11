@echo off
setlocal
pushd %~dp0

set classes=../../ZezeJava/build/classes/java/main
start "Master" java -Dlogname=Master -DZeze.Dbh2.Master.stat=10 -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.Master.Main zeze.xml

ping -n 3 127.1 > nul

set classes=../../ZezeJava/build/classes/java/main
start "Manager0" java -Dlogname=Manager0 -DZeze.Dbh2.Server.stat=10 -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.Dbh2Manager manager0 zeze.xml

set classes=../../ZezeJava/build/classes/java/main
start "Manager1" java -Dlogname=Manager1 -DZeze.Dbh2.Server.stat=10 -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.Dbh2Manager manager1 zeze.xml

set classes=../../ZezeJava/build/classes/java/main
start "Manager2" java -Dlogname=Manager2 -DZeze.Dbh2.Server.stat=10 -cp %classes%;../../ZezeJava/lib/*;. Zeze.Dbh2.Dbh2Manager manager2 zeze.xml
