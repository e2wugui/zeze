@echo off
setlocal
pushd %~dp0

md log 2>nul

java ^
-Dlogname=GlobalCacheManagerAsyncServer ^
-Xms256m ^
-Xmx1g ^
-XX:MaxGCPauseMillis=100 ^
-XX:+HeapDumpOnOutOfMemoryError ^
-Xlog:gc=info,gc+heap=info:log/GlobalCacheManagerAsyncServer_gc.log:time ^
-Dsun.stdout.encoding=gbk ^
-Dsun.stderr.encoding=gbk ^
-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector ^
-cp lib/*;. Zeze.Services.GlobalCacheManagerAsyncServer %1 %2 %3 %4 %5 %6 %7 %8 %9


rem for JDK 24+: --sun-misc-unsafe-memory-access=allow ^
