#!/bin/bash

cd `dirname $0`

if [[ $1 == -* ]]; then SIG=$1; shift; else SIG=-15; fi

PID=`jps -l | grep -m 1 Benchmark.Simulate | awk '{print $1}'`
if [ -z $PID ]; then
	echo 'Simulate is not running'
else
	kill $SIG $PID
	echo 'Simulate has been stopped'
fi

PID=`jps -l | grep -m 1 Benchmark.Simulate | awk '{print $1}'`
if [ -z $PID ]; then
	echo 'Simulate is not running'
else
	kill $SIG $PID
	echo 'Simulate has been stopped'
fi

PID=`jps -l | grep Zeze.Services.GlobalCacheManagerAsyncServer | awk '{print $1}'`
if [ -z $PID ]; then
	echo 'GlobalCacheManagerAsyncServer is not running'
else
	kill $SIG $PID
	echo 'GlobalCacheManagerAsyncServer has been stopped'
fi

PID=`jps -l | grep Zeze.Services.ServiceManagerServer | awk '{print $1}'`
if [ -z $PID ]; then
	echo 'ServiceManagerServer is not running'
else
	kill $SIG $PID
	echo 'ServiceManagerServer has been stopped'
fi
