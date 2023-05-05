#!/bin/bash

cd `dirname $0`

PID=`ps x | grep java | grep logname=manager2 | grep -v grep | awk '{print $1}'`
if [ $PID ]; then kill $PID; fi

PID=`ps x | grep java | grep logname=manager1 | grep -v grep | awk '{print $1}'`
if [ $PID ]; then kill $PID; fi

PID=`ps x | grep java | grep logname=manager0 | grep -v grep | awk '{print $1}'`
if [ $PID ]; then kill $PID; fi

PID=`ps x | grep java | grep logname=master | grep -v grep | awk '{print $1}'`
if [ $PID ]; then kill $PID; fi
