@echo off
setlocal
pushd %~dp0

cd ..

java -DGenFileSrcRoot=ZezeJava/src/main/java -cp ZezeJava;ZezeJava\build\classes\java\main;ZezeJava\build\resources\main;ZezeJava\lib\* Zeze.Util.RedirectGenMain

echo.

cd ZezexJava\server
java -DGenFileSrcRoot=src -cp build\classes\java\main;..\..\ZezeJava;..\..\ZezeJava\build\classes\java\main;..\..\ZezeJava\build\resources\main;..\..\ZezeJavaTest\lib\* Program

pause
