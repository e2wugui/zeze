@echo off
setlocal
pushd %~dp0

java -cp .;build\libs\*;lib\* -DZeze.Database.ClearInUse=true Temp.TestTransactionHalt ClearData

set loop=1
:begin

echo "=============== %loop% ==============="
set /a loop=loop+1
java -cp .;build\libs\*;lib\* -DZeze.Database.ClearInUse=true Temp.TestTransactionHalt
IF %ERRORLEVEL% NEQ 0 (
  echo _____________________________________
  goto end
)

rem start /b java -cp .;build\libs\*;lib\* Temp.TestTransactionHalt
rem ping 127.1 -n 3 >nul
rem for /f "tokens=1" %%i in ('jps.exe^|find /i "TestTransactionHalt"') do taskkill /f /pid %%i

goto begin
:end
pause
