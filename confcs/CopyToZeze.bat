@echo off
setlocal
pushd %~dp0

set Zeze=..\Zeze
set confcs=.\Zeze

copy /y %confcs%\Config.cs                       %Zeze%\
copy /y %confcs%\IModule.cs                      %Zeze%\
copy /y %confcs%\AppBase.cs                      %Zeze%\

copy /y %confcs%\Net\*.*                         %Zeze%\Net\

copy /y %confcs%\Serialize\*.*                   %Zeze%\Serialize\

copy /y %confcs%\Services\Handshake.cs           %Zeze%\Services\
copy /y %confcs%\Services\ToLuaService2.cs       %Zeze%\Services\

copy /y %confcs%\Transaction\ChangesRecord.cs    %Zeze%\Transaction\
copy /y %confcs%\Transaction\DispatchMode.cs     %Zeze%\Transaction\
copy /y %confcs%\Transaction\Log.cs              %Zeze%\Transaction\
copy /y %confcs%\Transaction\TransactionLevel.cs %Zeze%\Transaction\

copy /y %confcs%\Transaction\Collections\CollApply.cs %Zeze%\Transaction\Collections\
copy /y %confcs%\Transaction\Collections\Log*.cs      %Zeze%\Transaction\Collections\

copy /y %confcs%\Util\AtomicLong.cs              %Zeze%\Util\
copy /y %confcs%\Util\BeanFactory.cs             %Zeze%\Util\
copy /y %confcs%\Util\Comparer.cs                %Zeze%\Util\
copy /y %confcs%\Util\ConfBean.cs                %Zeze%\Util\
copy /y %confcs%\Util\DispatchModeAttribute.cs   %Zeze%\Util\
copy /y %confcs%\Util\FixedHash.cs               %Zeze%\Util\
copy /y %confcs%\Util\Logger.cs                  %Zeze%\Util\
copy /y %confcs%\Util\Mission.cs                 %Zeze%\Util\
copy /y %confcs%\Util\Random.cs                  %Zeze%\Util\
copy /y %confcs%\Util\Reflect.cs                 %Zeze%\Util\
copy /y %confcs%\Util\ResultCode.cs              %Zeze%\Util\
copy /y %confcs%\Util\Scheduler.cs               %Zeze%\Util\
copy /y %confcs%\Util\Str.cs                     %Zeze%\Util\
copy /y %confcs%\Util\Time.cs                    %Zeze%\Util\
rem copy /y %confcs%\Util\UnityHelpers.cs        %Zeze%\Util\

pause
