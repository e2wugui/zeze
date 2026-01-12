@echo off
setlocal
pushd %~dp0

rd /s /q server\hot 2>nul
mkdir server\hot\distributes
echo. > server\hot\distributes\.gitkeep

rem 必须cd到启动目录执行，配置目录有依赖。
cd server

set classes=../../ZezeJava/build/classes/java/main;build/classes/java/main

echo 打包...
java -cp %classes%;../../ZezeJavaTest/lib/* Zeze.Hot.Distribute -privateBean -app Game.App -workingDir hot -classes  build/classes/java/main -providerModuleBinds ../provider.module.binds.xml -config server.xml
echo OK

cd ..

pause
