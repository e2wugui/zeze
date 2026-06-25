## ZezeBoot

使用zeze框架的初始项目模板

#### 快速上手

- 安装好`git`, `JDK`, `IDEA`等开发工具(尽量选择新版本,JDK版本至少11推荐21以上), 确保可在任意目录执行`git`命令.
- 从此版本库clone到本地, 以下默认在此版本库根目录下执行.
- 运行`gen.bat`, 生成基本的框架代码.
- (可选)运行`build.bat`, 可直接在命令行构建整个项目并复制依赖的jar文件.
- 用`IDEA`打开根目录, 即可开始开发和测试.

#### 启动项目

- 启动服务管理(服务发现)`Service Manager`: `start_service_manager.bat`.
- 启动全局记录权限管理`Global Server`: `start_global_server.bat`. 如果只启动单个server进程,可不启动`Global Server`.
- 以上两个服务进程可以常驻,都是zeze框架内置无需改动; 下面的进程需要进一步开发,可随时单独启停,没有严格的先后顺序.
- 启动`link`服务: `start_link.bat`. 也可以在IDEA中调试启动`link`模块中的`ZezeBootLink`类.
- 启动`server`服务: `start_server.bat`. 也可以在IDEA中调试启动`server`模块中的`ZezeBootServer`类.
- 启动`client`测试客户端: `start_client.bat`. 也可以在IDEA中调试启动`client`模块中的`ZezeBootClient`类.
- `server`服务启动后, 可在浏览器中访问`http://127.0.0.1:8080/Zeze/Builtin/DbWeb/ListTable`查看数据库.
- `client_cs`目录是C#版本的客户端,功能同`client`, 基于`.Net 4.7.1`,以适配Unity引擎.
- 友好停止服务进程的方法是在命令行窗口中按`Ctrl+C`, 日志输出`ShutdownHook end`后再关闭命令行窗口.

#### 修改项目名(ZezeBoot)

- 此功能暂时只支持Windows系统.
- 从本git版本库取出没有其它文件的干净版本(可用`git archive`命令或`TortoiseGit`提供的`Export`功能打包,再解压到一个新目录).
- 启动`tool\change_solution_name.bat`.
- 在提示`new solution english name: `后输入新的项目名(只允许用全半角的英文字母和数字,且首字符必须是字母).
- 然后脚本就会修改相关代码配置中的项目名,并修改几个目录名.
- 完成后,再运行`gen.bat`和`build.bat`生成代码和构建编译.

#### 如何从零开始搭建初始项目

- 安装好`git`, `JDK`, `Gradle`, `IDEA`等开发工具(尽量选择新版本), 确保可在任意目录执行`git`和`gradle`命令.
- 创建一个空的版本库, clone到本地, 以下默认在此版本库根目录下执行.
- 创建初始的`README`, `LICENSE`, `.gitignore`等文件.
- 新建一个空的`build.gradle`文件.
- 根目录下运行`gradle wrapper`, 生成wrapper相关目录和文件.
- `gradle\wrapper\gradle-wrapper.properties`文件中`distributionUrl=`后面的链接可改成下面腾讯云的镜像链接,这样国内下载Gradle会快些:
  `https\://mirrors.cloud.tencent.com/gradle/gradle-8.7-bin.zip`
- 创建`zeze`目录, 放入zeze框架构建后的jar及源代码文件.
- 创建`gen`目录, 放入zeze框架中的生成代码工具.
- 创建`protocol`目录, 放入协议定义的xml格式文件.
- 创建`gen.bat`文件, 内容是根据`protocol`目录里协议文件生成代码, 用`gen`目录里的工具生成代码.
- 运行`gen.bat`, 生成基本的框架代码.
- 编辑`build.gradle`文件, 写入项目通用的构建配置.
- 创建`settings.gradle`文件, 写入项目名和各子模块的路径.
- 创建`link`,`server`,`client`三个目录, 在这些目录下各创建`build.gradle`文件, 写入各子模块特殊的构建配置.
- 创建`build.bat`文件, 内容是调用`gradlew.bat build copyJar`命令构建整个项目并复制依赖的jar文件.
- 创建`start_service_manager.bat`,`start_global_server.bat`,`start_link.bat`,`start_server.bat`,`start_client.bat`文件, 内容是各模块的启动脚本.
- 创建`zeze_link.xml`,`zeze_server.xml`,`zeze_client.xml`,`provider.module.binds.xml`文件, 内容是`link`,`server`,`client`的框架运行配置和模块绑定配置.
- 用`IDEA`打开根目录, 即可开始开发和测试.
