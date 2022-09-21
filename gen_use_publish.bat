@echo off
setlocal
pushd %~dp0

set PATH=%~dp0\publish;%PATH%

echo csharp gen ...

Gen.exe solution.zeze.xml
Gen.exe -c genr -GenRedirect Zeze\RedirectOverride

cd Sample
Gen.exe solution.xml
Gen.exe solution.linkd.xml
cd ..

cd UnitTest
Gen.exe solution.xml
cd ..

cd confcs
Gen.exe solution.xml
Gen.exe -c ExportConf -ZezeSrcDir ..
cd ..

echo java gen ...

cd ZezeJava\ZezeJava
Gen.exe solution.zeze.xml
cd ..\..

cd ZezeJava\ZezeJavaTest
Gen.exe solution.xml
cd ..\..

cd ZezeJava\ZezexJava
Gen.exe solution.xml
Gen.exe solution.linkd.xml
cd ..\..

cd ZezeJava\Zege
Gen.exe solution.xml
Gen.exe solution.linkd.xml
cd ..\..

echo gen done!
pause
