@echo off
setlocal
pushd %~dp0

rem ..\..\publish\Gen.exe solution.xml
rem ..\..\publish\Gen.exe solution.linkd.xml

copy /Y server\ModuleEquip.hot.java server\src\Game\Equip\ModuleEquip.java
..\..\Gen\bin\Debug\net6.0\Gen.exe solution.hot.xml
..\..\Gen\bin\Debug\net6.0\Gen.exe solution.linkd.xml

pause

