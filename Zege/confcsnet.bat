
set zeze_src_dir=..
set project_dir=.
set gen=..\Gen\bin\Debug\net6.0\Gen.exe

%gen% -c ExportConf -ZezeSrcDir %zeze_src_dir%

md %project_dir%\Zeze

md %project_dir%\Zeze\Net
xcopy /Y %zeze_src_dir%\Zeze\Net %project_dir%\Zeze\Net

xcopy /Y %zeze_src_dir%\Zeze\IModule.cs %project_dir%\Zeze\
xcopy /Y %zeze_src_dir%\Zeze\AppBase.cs %project_dir%\Zeze\
xcopy /Y %zeze_src_dir%\Zeze\Config.cs  %project_dir%\Zeze\
xcopy /Y %zeze_src_dir%\Zeze\Config.cs  %project_dir%\Zeze\

md %project_dir%\Zeze\Util
xcopy /Y %zeze_src_dir%\Zeze\Util\ResultCode.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Mission.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Scheduler.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\AtomicLong.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\FixedHash.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Reflect.cs %project_dir%\Zeze\Util\
xcopy /Y %zeze_src_dir%\Zeze\Util\Time.cs %project_dir%\Zeze\Util\

md %project_dir%\Zeze\Services
xcopy /Y %zeze_src_dir%\Zeze\Services\Handshake.cs %project_dir%\Zeze\Services\
xcopy /Y %zeze_src_dir%\Zeze\Services\ToLuaService.cs %project_dir%\Zeze\Services\

md %project_dir%\Zeze\Transaction
xcopy /Y %zeze_src_dir%\Zeze\Transaction\TransactionLevel.cs %project_dir%\Zeze\Transaction\
