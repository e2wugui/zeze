
@echo off
echo %~dp0

cd %~dp0\GlobalCacheManager\bin\Release\net6.0
start "GlobalCacheManager.exe" GlobalCacheManager.exe

cd %~dp0\ServiceManager\bin\Release\net6.0
start "ServiceManager.exe" ServiceManager.exe

