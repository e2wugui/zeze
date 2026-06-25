@echo off
setlocal
pushd %~dp0

title ZezeBootServer

md log 2>nul

java ^
-Dlogname=server ^
-Xms256m ^
-Xmx1g ^
-XX:MaxGCPauseMillis=200 ^
-XX:AutoBoxCacheMax=65536 ^
-XX:SoftRefLRUPolicyMSPerMB=1000 ^
-XX:+HeapDumpOnOutOfMemoryError ^
-Xlog:gc=info,gc+heap=info:log/server_gc.log:time ^
--enable-native-access=ALL-UNNAMED ^
-Dsun.stdout.encoding=gbk ^
-Dsun.stderr.encoding=gbk ^
-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector ^
-cp server/lib/*;server/build/libs/*;. ZezeBootServer %1 %2 %3 %4 %5 %6 %7 %8 %9


rem for JDK 24+: --sun-misc-unsafe-memory-access=allow ^
