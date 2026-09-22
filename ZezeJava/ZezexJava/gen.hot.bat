@echo off
setlocal
pushd %~dp0

copy /Y server\ModuleEquip.hot.java server\src-hot\Game\Equip\ModuleEquip.java
rem ..\..\Gen\bin\Debug\net8.0\Gen.exe solution.hot.xml
rem ..\..\Gen\bin\Debug\net8.0\Gen.exe solution.linkd.xml
..\..\publish\Gen.exe solution.xml
..\..\publish\Gen.exe solution.linkd.xml

pause

