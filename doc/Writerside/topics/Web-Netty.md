# Web(Netty)

可嵌入Server的Web模块，提供开发简单的Web服务。这样系统就有了让浏览器成为终
端的能力。对于一个有点规模的系统，这个能力是比较重要的。至少系统的在线管理功能是
很适合使用浏览器的。这个模块包装了Netty提供的Http解析和网络服务，主要的实现都
在名字空间Zeze.Netty下面。下面介绍主要的类和功能。

## Netty
Netty的启动关闭以及服务管理。主要方法有addServer, start, stop。

## HttpServer
Web网络服务管理类，实现Netty接口ChannelInitializer&lt;SocketChannel&gt;，初始化和处理
Web请求的派发。请求派发给HttpExchange处理。主要方法addHandler。

## HttpExchane
HttpExchange是核心包装类，是Zeze-Web应用主要使用的类。它实现Netty Http请求聚
合，简化上层的使用。另外提供一些发送Web-Response的便利辅助函数。

## HttpHandler
Web请求处理接口。包含如下的定义。
```
public int MaxContentLength = 8192; // -1 表示不限制，按流处理。
public TransactionLevel Level = TransactionLevel.Serializable; // 事务级别
public DispatchMode Mode = DispatchMode.Normal; // 线程派发模式
// 1. Request-Response处理模式
// 普通请求处理函数，不是流处理方式时，如果需要内部会自动把流合并到一个请求里面。
public HttpFullRequestHandle FullRequestHandle;
// 2. 异步流处理模式。
// 上行流处理函数。
public HttpBeginStreamHandle BeginStreamHandle;
public HttpStreamContentHandle StreamContentHandle;
public HttpEndStreamHandle EndStreamHandle;
说明：
•	不同处理模式实现相应的Handle。
•	这个Handler可以手动创建并注册到HttpServer中。
•	这个Handler也可以在solutions.xml中定义，把相应Handle映射到模块的实现类中。
•	注册Handler需要指定一个key，即web-uri-path。
•	Web-uri-path注册只支持一一对应，不支持往path-parent方向搜索。
•	入门例子Zeze.Netty.Netty.java::main
```
## 接入Nginx
使用Nginx作为Web-Netty的网关，Zeze提供了一些手段动态更新Nginx的服务器列表。

### 主动注册到 Consul 中
需要符合如下条件：
1. 使用Consul。
2. Nginx 编入模块 nginx-upsync-module。
3. 把你的Web-Netty服务主动注册到Consul中。

Consul和nginx-upsync-module相关配置请查看相关文档。下面举例说明注册的操作。

注册到Consule例子
“”“
    // 启动初始化
    var netty = new Netty(nThreads);
    var consul = new Consul();
    var server = new HttpServer();

    server.start(netty, port).sync();
    consul.register("YourServiceName", server); // 注册服务，这里的服务名字需要和Nginx配置一致。
    
    // 停止程序
    consul.stop(); 
    server.close();
    netty.close();
”“”

### 修改Nginx配置并重新加载

* 在每一个Nginx所在的服务器运行Exporter。
* 你的Web-Netty服务主动注册到Zeze的ServiceManager中。
* 如果Nginx网关服务器没有java环境，可以自行尝试把Exporter编成native程序。这样便于发布。

运行Exporter例子
"""
java -cp zeze.jar Zeze.Service.ServiceManager.Exporter -e Zeze.Services.ServiceManager.ExporterNginxConfig nginx.config.file 0 -s YourServiceName
"""

注册服务例子
“”“
    // 启动初始化
    var netty = new Netty(nThreads);
    var server = new HttpServer();

    server.start(netty, port).sync();
    server.publishService("YourServiceName"); // 注册，这需要Zeze.Application环境。还有另一个版本的publishService可用。
    
    // 停止程序
    server.close();
    netty.close();
”“”

### 通过http接口直接修改Nginx内部服务列表
需要符合如下条件：
1. Nginx 编入模块 nginx-http-dyups-module。配置请参考相关文档。
2. 把你的Web-Netty服务主动注册到Zeze的ServiceManager中。注册方式同上一种方式。
3. 在每一个Nginx所在的服务器运行Exporter。

运行Exporter
"""
java -cp zeze.jar Zeze.Service.ServiceManager.Exporter -e Zeze.Services.ServiceManager.ExporterNginxHttp url 0 -s YourServiceName
"""

一般nginx-http-dyups-module的http网络接口是配置在127.0.0.1上，所以需要每Nginx运行一个Exporter。
如果http网络配置在开放Ip上，可以只运行一个Exporter，通过重复指定"-e"参数广播服务列表。
"""
java -cp Exporter -e ExporterNginxHttp url1 0 -e  ExporterNginxHttp url2 0 -s YourServiceName
"""

nginx-http-dyups-module接收到的服务器列表是保存在Nginx内存中的，重启会丢失。这样在Exporter输出前，
有一段时间Nginx的服务列表是空的。这是可以结合ExporterNginxConfig，同时把服务列表修改到Nginx的配置文件中。
当然这种方式需要每Nginx运行一个Exporter。
"""
java -cp Exporter -e ExporterNginxHttp url 0 -e  ExporterNginxConfig nginx.config.file 0 -s YourServiceName
"""
