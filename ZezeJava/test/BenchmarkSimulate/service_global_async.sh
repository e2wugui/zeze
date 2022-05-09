#!/bin/bash

cd `dirname $0`

nohup java -Dlogname=ServiceManagerServer          -cp .:lib/* Zeze.Services.ServiceManagerServer          -port 5001 1> service_stdout.log 2> service_stderr.log &
nohup java -Dlogname=GlobalCacheManagerAsyncServer -cp .:lib/* Zeze.Services.GlobalCacheManagerAsyncServer -port 5555 1> global_stdout.log  2> global_stderr.log  &
