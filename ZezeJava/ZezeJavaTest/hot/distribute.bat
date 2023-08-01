
@echo off
setlocal
pushd %~dp0

cd ..\build\classes\java\main
jar -c -f ../../../../hot/Temp.Interface.jar Temp/IModuleInterface.class
jar -c -f ../../../../hot/Temp.jar Temp/ModuleA.class

