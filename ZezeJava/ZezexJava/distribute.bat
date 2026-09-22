@echo off
setlocal
pushd %~dp0

rem 8 hot modules live in the "hot" source set (src-hot + Gen filtered); compile both first.
rem (%~dp0 absolute form: NoDefaultCurrentDirectoryInExePath=1 skips CWD in exe resolution)
call "%~dp0..\gradlew.bat" :ZezexJava:server:compileJava :ZezexJava:server:compileHotJava
if errorlevel 1 goto :fail

rd /s /q server\hot 2>nul
mkdir server\hot\distributes
echo. > server\hot\distributes\.gitkeep

rem run from server dir; relative paths below are relative to it
cd server

rem staging = main (cold classes + interfaces) + hot (8 module impl + gen) merged,
rem same layout the old full main output had, so Distribute.pack sees complete module dirs.
rd /s /q build\classes\java\hotstage 2>nul
mkdir build\classes\java\hotstage
xcopy /e /i /y build\classes\java\main build\classes\java\hotstage >nul
if errorlevel 1 goto :fail
xcopy /e /i /y build\classes\java\hot  build\classes\java\hotstage >nul
if errorlevel 1 goto :fail

set classes=../../ZezeJava/build/classes/java/main;build/classes/java/hotstage

echo packing...
java -cp %classes%;../../ZezeJavaTest/lib/* Zeze.Hot.Distribute -privateBean -app Game.App -workingDir hot -classes build/classes/java/hotstage -providerModuleBinds ../provider.module.binds.xml -config server.xml
if errorlevel 1 goto :fail
echo OK

cd ..
goto :end

:fail
cd ..
echo FAILED
exit /b 1

:end
pause
