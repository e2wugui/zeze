#!/bin/bash

cd `dirname $0`

java -Dlogname=client -Xmx4g -cp .:lib/* Zeze.Dbh2.BenchClient -valueSize 100 -masterPort 10999 -tableNumber 1 -tableAccess 1 -threadNumber 16
