
@echo off
setlocal
pushd %~dp0

cd ..\..
call gradlew.bat build copyJar
cd ZezexJava\server

call distribute.bat
copy hot\modules\Game.Equip.jar hotrun\distributes\
copy hot\interfaces\Game.Equip.interface.jar hotrun\distributes\
copy hot\__hot_schemas__Game.jar hotrun\distributes\

echo "" > hotrun\distributes\ready

