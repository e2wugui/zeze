
@echo off
setlocal
pushd %~dp0

cd ..\build\classes\java\main
jar -c -f ../../../../hot/distributes/Temp.interface.jar Temp/IModuleInterface.class
jar -c -f ../../../../hot/distributes/Temp.jar Temp/ModuleA.class

