#!/bin/bash

cd `dirname $0`

nohup java -Dlogname=service \
-Xlog:gc=info,gc+heap=info:service_gc.log:time \
-cp .:lib/* \
Zeze.Services.ServiceManagerServer \
-port 5001 \
1> service_stdout.log \
2> service_stderr.log &

nohup java -Dlogname=global \
-Xlog:gc=info,gc+heap=info:global_gc.log:time \
-cp .:lib/* \
Zeze.Services.GlobalCacheManagerAsyncServer \
-port 5555 \
1> global_stdout.log \
2> global_stderr.log &
