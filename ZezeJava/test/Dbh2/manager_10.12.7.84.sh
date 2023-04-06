#!/bin/bash

cd `dirname $0`

IP=10.12.7.84

# nohup java -Dlogname=Master  -cp .:lib/* Zeze.Dbh2.Master.Main zeze_$IP.xml &
# sleep 2
nohup java -Dlogname=Manager -cp .:lib/* Zeze.Dbh2.Dbh2Manager manager_$IP zeze_$IP.xml &
