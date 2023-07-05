#!/bin/bash

cd `dirname $0`

IP=10.12.7.140

# nohup java -Dlogname=master  -DZeze.Dbh2.Master.stat=10 -Dlogconsole=Null -cp .:lib/* Zeze.Dbh2.Master.Main zeze_$IP.xml 1> log/master_stdout.log 2> log/master_stderr.log &
# sleep 2
nohup java -Dlogname=manager -DZeze.Dbh2.Server.stat=10 -Dlogconsole=Null -cp .:lib/* Zeze.Dbh2.Dbh2Manager manager_$IP zeze_$IP.xml 1> log/manager_stdout.log 2> log/manager_stderr.log &
