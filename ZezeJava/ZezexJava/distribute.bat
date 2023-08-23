@echo off
setlocal
pushd %~dp0

rd /s /q server\hot 2>nul
mkdir server\hot\distributes

set classes=../ZezeJava/build/classes/java/main;server/build/classes/java/main

echo ´ò°ü...
java -cp %classes%;../ZezeJava/lib/* Zeze.Hot.Distribute -privateBean -app Game.App -workingDir server/hot -classes  server/build/classes/java/main
echo OK

pause
