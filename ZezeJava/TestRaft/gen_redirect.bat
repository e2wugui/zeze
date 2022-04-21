@echo off
setlocal
pushd %~dp0

cd ..

java -DGenFileSrcRoot=ZezexJava/server/src -cp TestRaft;ZezeJava\build\classes\java\main;ZezeJava\lib\*;ZezexJava\server\build\classes\java\main Program
java -DGenFileSrcRoot=ZezeJavaTest/src     -cp TestRaft;ZezeJava\build\classes\java\main;ZezeJavaTest\lib\*;ZezeJavaTest\build\classes\java\main UnitTest.Zeze.Game.TestRank

pause
