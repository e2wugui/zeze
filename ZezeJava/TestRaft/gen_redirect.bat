@echo off
setlocal
pushd %~dp0

cd ..
java -DGenFileSrcRoot=ZezexJava/server/src -cp .;ZezeJava\lib\*;ZezeJava\build\classes\java\main;ZezexJava\server\build\classes\java\main Program

pause
