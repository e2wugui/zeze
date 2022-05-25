
@echo off

setlocal enabledelayedexpansion
set libs=
for /f %%i in ('dir /b lib')  do (
	if "!libs!" == "" (set libs=lib\%%i) else (set libs=!libs!;lib\%%i)
) 

set classes=../ZezeJava/build/classes/java/main;client/build/classes/java/main
java -cp %classes%;%libs% Zege.Program
