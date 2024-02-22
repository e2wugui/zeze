@echo off
setlocal
pushd %~dp0

set PATH=%~dp0\publish;%PATH%

echo -------- Gen solution.zeze.xml ...
Gen.exe solution.zeze.xml
echo -------- GenRedirect Zeze\RedirectOverride ...
Gen.exe -c genr -GenRedirect Zeze\RedirectOverride

cd Sample
echo -------- Gen Sample\solution.xml ...
Gen.exe solution.xml
echo -------- Gen Sample\solution.linkd.xml ...
Gen.exe solution.linkd.xml
cd ..

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
