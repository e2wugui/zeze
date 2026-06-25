@echo off
setlocal
pushd %~dp0

title ZezeBootClient

md log 2>nul

java ^
-Dlogname=client ^
-Xms128m ^
-Xmx512m ^
-XX:MaxGCPauseMillis=200 ^
-XX:+HeapDumpOnOutOfMemoryError ^
-Xlog:gc=info,gc+heap=info:log/client_gc.log:time ^
-Dsun.stdout.encoding=gbk ^
-Dsun.stderr.encoding=gbk ^
-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector ^
-cp client/lib/*;client/build/libs/*;. ZezeBootClient %1 %2 %3 %4 %5 %6 %7 %8 %9


rem for JDK 24+: --sun-misc-unsafe-memory-access=allow ^
