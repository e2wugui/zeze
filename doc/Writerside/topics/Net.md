# 第十一章 Net

Zeze有一个很小的自己的网络实现。麻雀虽小五脏俱全。
Net是一个异步模式的网络实现。发送数据不会阻塞，立即返回。接收数据由底层解码成协
议并派发到线程池执行。

## AsyncSocket
对系统网络层进行必要的包装，它向应用提供发送数据的接口。

## Service
* 创建AsyncSocket。
* 管理AsyncSocket；
* 管理网络配置；
* 处理它的网络事件；
* 管理自己能处理的协议；

## Inherit Service
Zeze应用通过继承Service的方式使用Net模块。
1.	Service所有的处理函数都在网络线程中直接处理。所以，重载实现不能阻塞。
2.	Service的重载函数通常需要调用基类的方法，除非特殊情况。

## Protocol
Zeze协议带有一个Argument。Argument是一个Bean。
编码规范：
```
Header=(ModuleId,ProtocolId)
ParameterSize
ParameterBinary=(ResultCode,EncodedArgument)
Decode：解码实现方法。
```

## 接收数据处理流程（Dispatch）
1.	AsyncSocket 从系统接收数据
2.	进行解密解压（可选）
3.	Call Service.OnSocketProcessInputBuffer（可重载实现自己全新的协议规范）
4.	Call Protocol.Decode
5.	Call Protocol.Dispatch（手动编写的协议子类可重载，自动生成的协议代码总是会被覆盖，没
      法写重载实现代码，这个重载就没有意义）
6.	Call Service.DispatchProtocol（协议派发主要重载接口，有默认实现）

## Rpc
```
Rpc有Argument,Result两个参数。
Rpc is Protocol。
编码规范：
Header=(ModuleId,ProtocolId)
ParameterSize
ParameterBinary=(IsRequest,SessionId,ResultCode,ArgumentOrResult)
Rpc提供同步等待、异步回调两种方式处理结果。
```

## Connector
客户端连接器。一个Connector管理一个客户端连接，根据配置处理自动重连。Connector
配置在Service.Config中，可以动态增删。

## Acceptor
服务器监听器。开启一个bind，listen的server socket，并接收新的客户端连接。

## ServiceConf
```
<ServiceConf Name="Zeze.Services.ServiceManager.Agent">
　　  <Connector HostNameOrAddress="127.0.0.1" Port="5001"/>
</ServiceConf>
```
* Name 是Solution.xml里面配置的名字。
* Connector 连接器配置。可以包含多个。
* Acceptor 可以和Connector一起存在，这个例子没有啦。
* 对一个Service来说，不管是来自Connector的连接还是来自Acceptor的连接，都享受一样的服务。

## Protocol Serialize Performance
由于为了在协议层和数据库存储层共享结构(Bean)，现在协议系列化在服务器端也是按支持
事务的模式实现数据结构的。支持事务的容器性能会差一些。比如现在Provider发送给Linkd
的Send协议非常频繁。Send协议的参数BSend有个容器变量，造成性能可能不足。历史
上，这个容器类型是Set，系列化性能太差，改成了List。一般情况下改成List之后性能足
够了。但如果你还想继续优化。可以选择手动方式。
1.	实现自己的协议的参数MyBean。
2.	新建一个MyProtocol extends Protocol&lt;MyBean&gt;。
3.	程序启动过程中，在框架默认的协议注册完成以后。
4.	删除旧的协议处理ProtocolFactoryHandle。
5.	注册自己MyProtocol。
