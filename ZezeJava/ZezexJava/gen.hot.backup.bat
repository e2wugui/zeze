@echo off
setlocal
pushd %~dp0

rem ..\..\publish\Gen.exe solution.xml
rem ..\..\publish\Gen.exe solution.linkd.xml

copy /Y server\src\Game\Equip\ModuleEquip.java server\ModuleEquip.hot.java

pause

