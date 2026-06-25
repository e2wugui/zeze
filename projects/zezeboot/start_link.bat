@echo off
setlocal
pushd %~dp0

title ZezeBootLink

md log 2>nul

java ^
-Dlogname=link ^
-Xms128m ^
-Xmx512m ^
-XX:MaxGCPauseMillis=100 ^
-XX:MaxDirectMemorySize=1G ^
-XX:+HeapDumpOnOutOfMemoryError ^
-Xlog:gc=info,gc+heap=info:log/link_gc.log:time ^
-Dsun.stdout.encoding=gbk ^
-Dsun.stderr.encoding=gbk ^
-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector ^
-cp link/lib/*;link/build/libs/*;. ZezeBootLink %1 %2 %3 %4 %5 %6 %7 %8 %9


rem for JDK 24+: --sun-misc-unsafe-memory-access=allow ^
