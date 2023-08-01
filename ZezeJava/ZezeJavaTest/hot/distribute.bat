
@echo off
setlocal
pushd %~dp0

cd ..\build\classes\java\main
jar -c -f ../../../../hot/interfaces/m.jar Temp/IModuleInterface.class
jar -c -f ../../../../hot/modules/m.jar Temp/ModuleA.class

