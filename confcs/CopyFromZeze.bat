@echo off
setlocal
pushd %~dp0

set Zeze=..\Zeze
set confcs=.\Zeze

md %confcs%\Net 2> nul
md %confcs%\Serialize 2> nul
rem md %confcs%\Services 2> nul
md %confcs%\Util 2> nul

rem copy /y %Zeze%\AppBase.cs                      %confcs%\
rem copy /y %Zeze%\Application.txt                 %confcs%\Application.cs
rem copy /y %Zeze%\Config.cs                       %confcs%\
rem copy /y %Zeze%\IModule.cs                      %confcs%\

copy /y %Zeze%\Net\*.*                         %confcs%\Net\

copy /y %Zeze%\Serialize\*.*                   %confcs%\Serialize\

rem copy /y %Zeze%\Services\Handshake.cs           %confcs%\Services\

copy /y %Zeze%\Util\ConfBean.cs                %confcs%\Util\
copy /y %Zeze%\Util\FixedHash.cs               %confcs%\Util\
copy /y %Zeze%\Util\Str.cs                     %confcs%\Util\

pause
