@echo off
setlocal
pushd %~dp0

cd ..

java -DGenFileSrcRoot=ZezexJava/server/src   -cp ZezeJava;ZezeJava\build\classes\java\main;ZezeJava\lib\*;ZezexJava\server\build\classes\java\main Program
echo.
java -DGenFileSrcRoot=ZezeJava/src/main/java -cp ZezeJava;ZezeJava\build\classes\java\main;ZezeJava\lib\* Zeze.Util.RedirectGenMain

pause
