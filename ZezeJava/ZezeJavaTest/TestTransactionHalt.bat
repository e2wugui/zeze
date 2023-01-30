@echo off
setlocal
pushd %~dp0

:begin

echo --------------------------------
java -cp .;build\libs\*;lib\* Temp.TestTransactionHalt

rem start /b java -cp .;build\libs\*;lib\* Temp.TestTransactionHalt
rem ping 127.1 -n 3 >nul
rem for /f "tokens=1" %%i in ('jps.exe^|find /i "TestTransactionHalt"') do taskkill /f /pid %%i

goto begin
