@echo off
setlocal
pushd %~dp0

echo csharp gen ...

Gen\bin\Debug\net6.0\Gen solution.zeze.xml

Gen\bin\Debug\net6.0\Gen -c genr -GenRedirect Zeze\RedirectOverride

cd Sample
..\Gen\bin\Debug\net6.0\Gen.exe
cd ..

cd UnitTest
..\Gen\bin\Debug\net6.0\Gen.exe
cd ..

echo java gen ...

cd ZezeJava\Zege
..\..\Gen\bin\Debug\net6.0\Gen.exe solution.xml
..\..\Gen\bin\Debug\net6.0\Gen.exe solution.linkd.xml
cd ..\..

cd ZezeJava\ZezeJava
..\..\Gen\bin\Debug\net6.0\Gen.exe solution.zeze.xml
cd ..\..

cd ZezeJava\ZezeJavaTest
..\..\Gen\bin\Debug\net6.0\Gen.exe
cd ..\..

cd ZezeJava\ZezexJava
..\..\Gen\bin\Debug\net6.0\Gen.exe solution.xml
..\..\Gen\bin\Debug\net6.0\Gen.exe solution.linkd.xml
cd ..\..

echo gen done!
pause