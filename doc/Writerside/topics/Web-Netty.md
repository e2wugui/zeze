# 第十九章 Web(Netty)

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
