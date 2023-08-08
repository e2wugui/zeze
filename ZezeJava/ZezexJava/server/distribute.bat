@echo off
setlocal
pushd %~dp0

rd /s /q hot 2>nul
mkdir hot\distributes

set classes=../../ZezeJava/build/classes/java/main;build/classes/java/main
java -cp %classes%;../../ZezeJava/lib/* Zeze.Hot.Distribute

pause
