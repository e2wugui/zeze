# Net

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

## 协议接收处理流程

```
Selector.run()
 AsyncSocket.doHandle(SelectionKey)
  AsyncSocket.processReceive(SocketChannel)
*  Service.OnSocketProcessInputBuffer(AsyncSocket, ByteBuffer)   用于直接处理网络数据流(没有任何分包处理)
    Protocol.decode(Service, AsyncSocket, ByteBuffer)   static方法,用(moduleId+protocolId+size+data)分包,但还没反序列化data
*    Service.dispatchUnknownProtocol(AsyncSocket, int moduleId, int protocolId, ByteBuffer)   分支,条件:未注册(不再向下处理)
*    Service.dispatchProtocol(long typeId, ByteBuffer, ProtocolFactoryHandle, AsyncSocket)   最底层的已注册协议处理,这里decodeProtocol出协议对象后再继续处理
      Protocol/Rpc.handle(Service, ProtocolFactoryHandle)   分支,条件:握手协议同步处理;事务类型包装事务后Task.run处理
       ProtocolHandle.handle(p/rpc)
      Protocol/Rpc.dispatch(Service, ProtocolFactoryHandle)   分支,条件:非握手非事务类型
*      Service.dispatchProtocol(Protocol, ProtocolFactoryHandle)   分支,条件:Protocol和Rpc请求
        Protocol.handle(Service, ProtocolFactoryHandle)   通过Task.run处理
         ProtocolHandle.handle(p)
*      Service.dispatchRpcResponse(P rpc, ProtocolHandle<P>, ProtocolFactoryHandle)   分支,条件:Rpc回复且没有设置future,这里会把请求context的responseHandle传进第2个参数,第3个参数只用Level和Mode
        ProtocolHandle.handle(rpc)   通过Task.runRpcResponse处理,事务类型包装事务后Task.runRpcResponse处理

以上前面带"*"表示可以重载实现
```

## 协议日志

协议日志通过加JVM参数来开启,如: -DprotocolLog=DEBUG
可通过加JVM参数来排除不需要输出日志的协议类型(TypeId),如:
-DprotocolLogExcept=3504939016,42955910777365
以上参考Zeze.Net.AsyncSocket的定义
以下是所有协议日志的格式:

### 通过Zeze.Net.AsyncSocket.Send(Protocol)直接发协议
SEND:连接sessionId RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
SEND:连接sessionId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
SEND:连接sessionId 协议类名 协议Bean内容 // 发送协议
SEND:连接sessionId 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议

### 通过Zeze.Net.Protocol.decode接收协议
RECV:连接sessionId RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
RECV:连接sessionId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
RECV:连接sessionId 协议类名 协议Bean内容 // 发送协议
RECV:连接sessionId 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议
如果"协议类名/RPC类名"未知,会用"moduleId:protocolId"代替,同时Bean内容会用"header[bean大小]"代替

### link通过Zeze.Arch.LinkdProvider.ProcessSendRequest处理的Send协议并转发里面的协议给客户端
Send:连接sessionId RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
Send:连接sessionId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:连接sessionId 协议类名 协议Bean内容 // 发送协议
Send:连接sessionId 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议
多组"连接sessionId"会以"sessionId,sessionId,..."方式输出,多于10个sessionId会以"[sessionId数量]"方式输出

### link通过Zeze.Arch.LinkdProvider.ProcessBroadcast处理的Broadcast协议并广播里面的协议给客户端
Broc:客户端连接数 RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
Broc:客户端连接数 RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Broc:客户端连接数 协议类名 协议Bean内容 // 发送协议
Broc:客户端连接数 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议

### Provider通过Zeze.Arch.ProviderImplement.ProcessDispatch接收封装成Dispatch的协议
Recv:roleId RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
Recv:roleId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Recv:roleId 协议类名 协议Bean内容 // 发送协议
Recv:roleId 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议
如果当前roleId无效,则用负的linkSid代替

### Provider通过Zeze.Arch.ProviderUserSession.sendResponse(Protocol)发封装成Send的协议
Send:roleId RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
Send:roleId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:roleId 协议类名 协议Bean内容 // 发送协议
Send:roleId 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议
如果当前roleId无效,则用负的linkSid代替

### Provider通过Zeze.Game.Online.send发封装成Send的协议
Send:roleId RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
Send:roleId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:roleId 协议类名 协议Bean内容 // 发送协议
Send:roleId 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议
多组"roleId"会以"roleId,roleId,..."方式输出

### Provider通过Zeze.Game.Online.sendReliableNotify发封装成SReliableNotify的协议
Send:roleId:listenerName RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
Send:roleId:listenerName RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:roleId:listenerName 协议类名 协议Bean内容 // 发送协议
Send:roleId:listenerName 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议

### Provider通过Zeze.Arch/Game.Online.broadcast(Protocol)发封装成Broadcast的协议
Broc:link连接数 RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
Broc:link连接数 RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Broc:link连接数 协议类名 协议Bean内容 // 发送协议
Broc:link连接数 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议

### Provider通过Zeze.Arch.Online.send发封装成Send的协议
Send:account,clientId RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
Send:account,clientId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:account,clientId 协议类名 协议Bean内容 // 发送协议
Send:account,clientId 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议
多组"account,clientId"会以"account,clientId;account,clientId;..."方式输出

### Provider通过Zeze.Arch.Online.sendAccount/sendAccounts发封装成Send的协议
Send:account RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
Send:account RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:account 协议类名 协议Bean内容 // 发送协议
Send:account 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议
多组"account"会以"account,account,..."方式输出

### Provider通过Zeze.Arch.Online.sendReliableNotify发封装成SReliableNotify的协议
Send:account,clientId:listenerName RPC类名:RPC的sessionId 请求Bean内容 // 发送RPC请求
Send:account,clientId:listenerName RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:account,clientId:listenerName 协议类名 协议Bean内容 // 发送协议
Send:account,clientId:listenerName 协议类名>resultCode 协议Bean内容 // 发送带resultCode的协议

### 协议日志开启建议
开启协议日志需要加JVM参数, 推荐的设置方法:
linkd: -DprotocolLog=DEBUG
gs: -DprotocolLog=DEBUG -DprotocolLogExcept=47280285301785,47281226998238
上面两组数字是需要排除的两个协议ID分别是Dispatch和Send, 因为只需要输出里面包装的协议日志就够了

### link协议日志举例
AsyncSocket: RECV:115004 1:291877964:2 1[1] 收到客户端发来的协议,网络sessionId是115004,moduleId=1,protocolId=291877964,RPC的sessionId=2
AsyncSocket: SEND:115003 Dispatch Zeze.Builtin.Provider.BDispatch: {... 包装这个协议成Dispatch发给gs(网络sessionId=115003)
AsyncSocket: RECV:115003 Send:135 Zeze.Builtin.Provider.BSend: {... 收到gs的Send协议(RPC的sessionId=135)
AsyncSocket: Send:115004 1:291877964:2 0[179] 发给客户端Send里包装的协议,协议类型和sessionId跟客户端发来的一样
AsyncSocket: SEND:115003 Send:135>0 Zeze.Builtin.Provider.BSendResult: {... 给gs回复Send协议,resultCode=0

### Provider协议日志的举例
AsyncSocket: Recv:257 GetMapLiveList:26 ()
AsyncSocket: Send:257 GetMapLiveList:26>0 wm.Map.BLiveList: {...
上面的257是角色ID, 26是RPC的sessionId, >0表示返回的resultCode=0.
