#!/bin/bash

cd `dirname $0`

IP=10.12.7.51

mkdir -p log 2> /dev/null

nohup java -Dlogname=master   -DZeze.Dbh2.Master.stat=10 -Dlog4j.configurationFile=log4j2_noconsole.xml -cp .:lib/* Zeze.Dbh2.Master.Main zeze.xml 1> log/master_stdout.log 2> log/master_stderr.log &
sleep 2
nohup java -Dlogname=manager0 -DZeze.Dbh2.Server.stat=10 -Dlog4j.configurationFile=log4j2_noconsole.xml -cp .:lib/* Zeze.Dbh2.Dbh2Manager manager0 zeze.xml 1> log/manager0_stdout.log 2> log/manager0_stderr.log &
nohup java -Dlogname=manager1 -DZeze.Dbh2.Server.stat=10 -Dlog4j.configurationFile=log4j2_noconsole.xml -cp .:lib/* Zeze.Dbh2.Dbh2Manager manager1 zeze.xml 1> log/manager1_stdout.log 2> log/manager1_stderr.log &
nohup java -Dlogname=manager2 -DZeze.Dbh2.Server.stat=10 -Dlog4j.configurationFile=log4j2_noconsole.xml -cp .:lib/* Zeze.Dbh2.Dbh2Manager manager2 zeze.xml 1> log/manager2_stdout.log 2> log/manager2_stderr.log &
