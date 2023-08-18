@echo off
setlocal
pushd %~dp0

rd /s /q hot 2>nul
mkdir hot\distributes

set classes=../../ZezeJava/build/classes/java/main;build/classes/java/main

echo "打包 TODO Gen需要生成一个方法（生成到App.java里面)，指出热更模块，由这个方法调用Zeze.Hot.Distribute(需要重构)"

java -cp %classes%;../../ZezeJava/lib/* Zeze.Hot.Distribute -privateBean -app Game.App

pause
