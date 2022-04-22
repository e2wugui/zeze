@echo off
setlocal
pushd %~dp0

cd ..

java -DGenFileSrcRoot=ZezexJava/server/src   -cp TestRaft;ZezeJava\build\classes\java\main;ZezeJava\lib\*;ZezexJava\server\build\classes\java\main Program
echo.
java -DGenFileSrcRoot=ZezeJava/src/main/java -cp TestRaft;ZezeJava\build\classes\java\main;ZezeJava\lib\* Zeze.Builtin.RedirectGenMain

pause
