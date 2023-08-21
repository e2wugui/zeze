@echo off
setlocal
pushd %~dp0

copy /Y server\src\Game\Equip\ModuleEquip.java server\ModuleEquip.java

pause

