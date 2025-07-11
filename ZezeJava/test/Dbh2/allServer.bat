@echo off
setlocal
pushd %~dp0

set classes=.;../../ZezeJava/lib/*;../../ZezeJava/build/classes/java/main;../../ZezeJava/build/resources/main

start "Master"   java -Dlogname=master   -Xmx1g -DZeze.Dbh2.Master.stat=100 -cp %classes% Zeze.Dbh2.Master.Main zeze.xml
ping -n 3 127.1 > nul
start "Manager0" java -Dlogname=manager0 -Xmx1g -DZeze.Dbh2.Server.stat=100 -DrocksdbCache=1g -cp %classes% Zeze.Dbh2.Dbh2Manager manager0 zeze0.xml
start "Manager1" java -Dlogname=manager1 -Xmx1g -DZeze.Dbh2.Server.stat=100 -DrocksdbCache=1g -cp %classes% Zeze.Dbh2.Dbh2Manager manager1 zeze1.xml
start "Manager2" java -Dlogname=manager2 -Xmx1g -DZeze.Dbh2.Server.stat=100 -DrocksdbCache=1g -cp %classes% Zeze.Dbh2.Dbh2Manager manager2 zeze2.xml
