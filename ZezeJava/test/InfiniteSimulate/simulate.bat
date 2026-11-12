@echo off
setlocal
pushd %~dp0

rem -ea -XX:NativeMemoryTracking=detail
rem classpath: lib\* (Zeze main jar + deps + copyTestLib junit jars) + test sourceset classes/resources.
rem NOT build\libs\ZezeJavaTest-*.jar - it is an empty shell (no main sourceset code, MANIFEST only);
rem test classes live in build\classes\java\test. Missing junit makes Verify fail with
rem NoClassDefFoundError: main exits but JVM hangs on non-daemon threads (no ShutdownHook, no round 2).
rem ascii-only comments: cmd parses this file as ANSI, utf-8 chinese gets shredded into commands.
java -Dlogname=Simulate -DuseUnlimitedVirtualThread=false -Dloglevel=INFO -cp .;..\..\ZezeJavaTest\lib\*;..\..\ZezeJavaTest\build\classes\java\test;..\..\ZezeJavaTest\build\resources\test Infinite.Simulate

pause
