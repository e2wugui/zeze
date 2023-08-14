@echo off
setlocal
pushd %~dp0

set Zeze=..\Zeze
set confcs=.\Zeze

rem md %confcs%\Zeze                         2> nul
rem md %confcs%\Zeze\Net                     2> nul
rem md %confcs%\Zeze\Serialize               2> nul
rem md %confcs%\Zeze\Services                2> nul
rem md %confcs%\Zeze\Transaction             2> nul
rem md %confcs%\Zeze\Transaction\Collections 2> nul
rem md %confcs%\Zeze\Util                    2> nul

copy /y %Zeze%\Config.cs                       %confcs%\
copy /y %Zeze%\IModule.cs                      %confcs%\
copy /y %Zeze%\AppBase.cs                      %confcs%\

copy /y %Zeze%\Net\*.*                         %confcs%\Net\

copy /y %Zeze%\Serialize\*.*                   %confcs%\Serialize\

copy /y %Zeze%\Services\Handshake.cs           %confcs%\Services\
copy /y %Zeze%\Services\ToLuaService2.cs       %confcs%\Services\

copy /y %Zeze%\Transaction\ChangesRecord.cs    %confcs%\Transaction\
copy /y %Zeze%\Transaction\DispatchMode.cs     %confcs%\Transaction\
copy /y %Zeze%\Transaction\Log.cs              %confcs%\Transaction\
copy /y %Zeze%\Transaction\TransactionLevel.cs %confcs%\Transaction\

copy /y %Zeze%\Transaction\Collections\CollApply.cs %confcs%\Transaction\Collections\
copy /y %Zeze%\Transaction\Collections\Log*.cs      %confcs%\Transaction\Collections\

copy /y %Zeze%\Util\AtomicLong.cs              %confcs%\Util\
copy /y %Zeze%\Util\BeanFactory.cs             %confcs%\Util\
copy /y %Zeze%\Util\Comparer.cs                %confcs%\Util\
copy /y %Zeze%\Util\ConfBean.cs                %confcs%\Util\
copy /y %Zeze%\Util\DispatchModeAttribute.cs   %confcs%\Util\
copy /y %Zeze%\Util\FixedHash.cs               %confcs%\Util\
copy /y %Zeze%\Util\Logger.cs                  %confcs%\Util\
copy /y %Zeze%\Util\Mission.cs                 %confcs%\Util\
copy /y %Zeze%\Util\Reflect.cs                 %confcs%\Util\
copy /y %Zeze%\Util\ResultCode.cs              %confcs%\Util\
copy /y %Zeze%\Util\Scheduler.cs               %confcs%\Util\
copy /y %Zeze%\Util\Str.cs                     %confcs%\Util\
copy /y %Zeze%\Util\Time.cs                    %confcs%\Util\
rem copy /y %Zeze%\Util\UnityHelpers.cs        %confcs%\Util\

pause
