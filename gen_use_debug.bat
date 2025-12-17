@echo off
setlocal
pushd %~dp0

set PATH=%~dp0\Gen\bin\Debug\net8.0;%PATH%

echo -------- Gen component.confcs.client.xml ...
Gen.exe component.confcs.client.xml

cd UnitTest
echo -------- Gen UnitTest\solution.xml ...
Gen.exe solution.xml
cd ..

cd confcs
echo -------- Gen confcs\solution.xml ...
Gen.exe solution.xml
rem echo -------- ExportConf ...
rem Gen.exe -c ExportConf -ZezeSrcDir ..
cd ..

cd ZezeJava\ZezeJava
echo -------- Gen ZezeJava\ZezeJava\solution.zeze.xml ...
Gen.exe solution.zeze.xml
cd ..\..

cd ZezeJava\ZezeJavaTest
echo -------- Gen ZezeJava\ZezeJavaTest\solution.xml ...
Gen.exe solution.xml
cd ..\..

cd ZezeJava\ZezexJava
echo -------- Gen ZezeJava\ZezexJava\solution.xml ...
Gen.exe solution.client.xml
Gen.exe solution.xml
echo -------- Gen ZezeJava\ZezexJava\solution.linkd.xml ...
Gen.exe solution.linkd.xml
cd ..\..

cd python
echo -------- Gen python\solution.xml ...
Gen.exe solution.xml
cd ..

echo -------- Gen done!
pause
