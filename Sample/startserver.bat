
start ..\GlobalCacheManager\bin\Debug\net8.0\GlobalCacheManager.exe
start ..\ServiceManager\bin\Debug\net8.0\ServiceManager.exe

copy /y linkd\linkd.xml .\linkd\bin\Debug\net8.0\
copy /y server\zeze.xml .\server\bin\Debug\net8.0\serverd.xml

start .\linkd\bin\Debug\net8.0\linkd.exe
start .\server\bin\Debug\net8.0\server.exe -AutoKeyLocalId 0

pause

goto end
@rem 基础测试完成之后再测试更多gs实例。使用相同的配置文件，除了AutoKeyLocalId，这个需要唯一，通过参数设置。
.\server\bin\Debug\net8.0\server.exe -AutoKeyLocalId 1
:end
