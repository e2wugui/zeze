
@echo off
setlocal
pushd %~dp0

cd ..\..
call gradlew.bat :ZezeJavaTest:compileHotJava
cd ZezeJavaTest\hot

mkdir distributes
cd ..\build\classes\java\hot
jar -c -f ../../../../hot/distributes/Temp.interface.jar Temp/IModuleInterface.class
jar -c -f ../../../../hot/distributes/Temp.jar Temp/ModuleTemp.class

