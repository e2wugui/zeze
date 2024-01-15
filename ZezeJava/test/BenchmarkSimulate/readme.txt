构建方法:
1. 确保构建了zeze/Gen, 并生成代码: zeze/ZezeJava/ZezeJavaTest/gen.bat
2. 构建ZezeJava和ZezeJavaTest: zeze/ZezeJava/test/build.bat
3. 从:
   zeze/ZezeJava/ZezexJava/lib/
   zeze/ZezeJava/ZezeJavaTest/build/libs/
   复制以下所需的jar到: zeze/ZezeJava/test/BenchmarkSimulate/lib/
   log4j-api-*.jar
   log4j-core-*.jar
   pcollections-*.jar
   rocksdbjni-*.jar
   ZezeJava-*.jar
   ZezeJavaTest-*.jar

打包文件:
本目录中所有版本库中的文件以及lib目录

运行方法:
Windows: 先配置即将运行的*.bat文件中的参数, 然后先运行 service_global_async.bat, 再运行 gs1.bat, gs2.bat, ...
Linux:   先配置即将运行的*.sh 文件中的参数, 然后先运行 service_global_async.sh , 再运行 gs1.sh , gs2.sh , ...
其中: gs3,gs4等启动脚本可从 gs1.bat/sh 复制并修改其中的 SERVER_ID 参数
