## 开发期 build 准备

1. 运行```gen.bat```生成内置的结构，协议、数据库表等访问代码，在```solution.zeze.xml```改变时运行。
2. 运行```genRedirect.bat```生成内置的redirect相关代码，在代码中@Redirct相关注解改变时运行。

- 以上脚本生成的代码都已提交git，所以主要在开发期间运行这些脚本
- 发布jar前也可以运行以上脚本，来检验```solution.zeze.xml```跟代码是否一致，检验redirect标记是否与生成代码一致


## 使用自己编译的zeze 

1. 运行 mvn install -Dgpg.skip=true，安装到本地mvn
2. 然后build.gradle中改为 implementation “com.zezeno:zeze-java:x.y.z-SNAPSHOT”
x.x.x-SNAPSHOT是maven官方库没有的版本。