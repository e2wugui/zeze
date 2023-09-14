@echo off
setlocal
pushd %~dp0

set zeze_src_dir=..
set project_dir=.
set gen=..\Gen\bin\Debug\net6.0\Gen.exe

REM --------------------------------------------------------------
REM conf+cs 系列化支持代码输出
REM --------------------------------------------------------------

%gen% -c ExportConf -ZezeSrcDir %zeze_src_dir%

REM ---------------------------------------------------------------
REM conf+cs+net 网络，模块，App等客户端框架需要的代码输出
REM --------------------------------------------------------------

md %project_dir%\Zeze 2> nul

REM Serialize在上面的ExportConf中目录已经创建了。
xcopy /Y %zeze_src_dir%\Zeze\Serialize\Vector3.cs %project_dir%\Zeze\Serialize

md %project_dir%\Zeze\Net 2> nul
xcopy /Y %zeze_src_dir%\Zeze\Net %project_dir%\Zeze\Net

xcopy /Y %zeze_src_dir%\Zeze\IModule.cs %project_dir%\Zeze\
xcopy /Y %zeze_src_dir%\Zeze\AppBase.cs %project_dir%\Zeze\
xcopy /Y %zeze_src_dir%\Zeze\Config.cs  %project_dir%\Zeze\
rem xcopy /Y %zeze_src_dir%\Zeze\Application.txt  %project_dir%\Zeze\Application.cs
IF NOT EXIST "%project_dir%\Zeze\MyLog.cs" copy %zeze_src_dir%\Zeze\MyLog.cs  %project_dir%\Zeze\MyLog.cs

md %project_dir%\Zeze\Util
xcopy /Y %zeze_src_dir%\Zeze\Util\ResultCode.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Mission.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Scheduler.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\AtomicLong.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Reflect.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Time.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Comparer.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\DispatchModeAttribute.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Logger.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\LoggerNLog.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Random.cs %project_dir%\Zeze\Util\

md %project_dir%\Zeze\Services
xcopy /Y %zeze_src_dir%\Zeze\Services\Handshake.cs %project_dir%\Zeze\Services\

md %project_dir%\Zeze\Transaction
xcopy /Y %zeze_src_dir%\Zeze\Transaction\TransactionLevel.cs %project_dir%\Zeze\Transaction\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Log.cs %project_dir%\Zeze\Transaction\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\DispatchMode.cs %project_dir%\Zeze\Transaction\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\ChangesRecord.cs %project_dir%\Zeze\Transaction\

md %project_dir%\Zeze\Transaction\Collections
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogBean.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogList.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogList1.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogList2.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogMap.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogMap1.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogMap2.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogSet.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogSet1.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\LogOne.cs %project_dir%\Zeze\Transaction\Collections\
xcopy /Y %zeze_src_dir%\Zeze\Transaction\Collections\CollApply.cs %project_dir%\Zeze\Transaction\Collections\

REM ---------------------------------------------------------------
REM Zege 需要的代码输出
REM --------------------------------------------------------------

xcopy /Y %zeze_src_dir%\Zeze\Util\Cert.cs %project_dir%\Zeze\Util\

echo 1. 在项目中定义宏 USE_CONFCS
echo 2. 如果项目包好NLog，请定义宏 HAS_NLOG
echo    如果项目有自己的Log管理，请定义宏 HAS_MYLOG，并修改 Zeze.MyLog.cs，实现相应的方法。
echo    如果项目禁止库代码记录log，不需要定义任何宏

pause
