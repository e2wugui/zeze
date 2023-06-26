@echo off
setlocal
pushd %~dp0

set Zeze=..\Zeze
set confcs=.\Zeze

copy /y %Zeze%\Application.txt                 %confcs%\Application.cs
copy /y %Zeze%\Config.cs                       %confcs%\
copy /y %Zeze%\IModule.cs                      %confcs%\

copy /y %Zeze%\Net\*.*                         %confcs%\Net\

copy /y %Zeze%\Serialize\*.*                   %confcs%\Serialize\

copy /y %Zeze%\Services\Handshake.cs           %confcs%\Services\

copy /y %Zeze%\Transaction\DispatchMode.cs     %confcs%\Transaction\
copy /y %Zeze%\Transaction\TransactionLevel.cs %confcs%\Transaction\

copy /y %Zeze%\Util\AtomicLong.cs              %confcs%\Util\
copy /y %Zeze%\Util\ConfBean.cs                %confcs%\Util\
copy /y %Zeze%\Util\FixedHash.cs               %confcs%\Util\
copy /y %Zeze%\Util\Mission.cs                 %confcs%\Util\
copy /y %Zeze%\Util\ResultCode.cs              %confcs%\Util\
copy /y %Zeze%\Util\Scheduler.cs               %confcs%\Util\
copy /y %Zeze%\Util\Str.cs                     %confcs%\Util\
copy /y %Zeze%\Util\Time.cs                    %confcs%\Util\

pause
