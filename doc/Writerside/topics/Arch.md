# Arch

Arch详细说明和使用。

## Provider &amp; Module
在Arch中，Provider是真正的逻辑实现服务，它躲在Linkd后面。用一套协议和Linkd交互。
下面的Server是一个Provider的实现，用来实现主逻辑。Server后端有一个共享的数据库
（如，mysql）。其他自定义的Provider实现Arch没有直接提供支持。自己根据需要使用
Provider协议和Linkd交互即可。
1.	每个Provider是一个project，独立进程。
2.	Module是逻辑划分单位，里面可以包含Bean，协议，数据（表）。Linkd转发的时候，同一
      个Module的协议会派发到同一个Provider实例。每个Module也可以看作是一个微服务。
3.	整个系统可以包含多个Provider。Provider包含多个Module实现。Module有一个唯一编号，
      在整个系统内唯一，即每个Module只能在一个Provider内实现。
4.	一个Provider可能存在多个实际运行进程。此时同一个Module会运行在多个进程中，但没
      有违反上一点。Module的唯一性是代码在Provider-Project中，实际运行可以是多进程。
5.	Provider 的 Service.type 一般都是 "server"，但是它主动连接 linkd，并注册支持的Module。
6.	绑定亲缘性。当linkd给client选择provider-instance时，会把该Provider支持的Module
      全部都一起绑定到client.sesion中。

## Linkd

连接负载分配服务器。

* LinkdApp

管理Arch模块内部依赖类的引用。也完成一些初始化工作。

* LinkdProvider

Linkd面向内部Provider（GameServer是一个Provider）实现模块。包含负载选择，Provider
协议的处理。Provider协议是定义内部服务怎么跟Linkd通讯的协议。

* LinkdProviderService

Linkd面向内部Provider的网络模块，管理跟内部Provider之间的网络连接和网络事件派发。

* LinkdService

Linkd面向客户端的网络模块，管理客户端连接。dispatchUnknownProtocol是这个服务总的
转发协议的处理入口，应用可以重载这个方法实现自己特殊的转发规则。比如一个群组聊天
的派发，根据hash(GroupId)，固定的转发到相应的服务器上。实现这个需要Decode出协
议，并根据参数按特殊规则选择服务器。为了避免Decode完整协议，有个办法就是所有的
群组协议开头都有固定的公共参数，如GroupId，Linkd这里可以偷偷的只偷出这一部分来
进行处理。

## Linkd Initialize

### 定义服务(solution.linkd.xml)
```
<project name="linkd" gendir="." scriptdir="src" platform="java">
　　<service name="LinkdService" handle="server"
　　base="Zeze.Arch.LinkdService">
　　<module ref="Linkd"/>
　　</service>
　　<service name="ProviderService" handle="client"
　　base="Zeze.Arch.LinkdProviderService">
　　</service>
</project>
```
Linkd服务需要使用Arch里面的两个服务，在base里面指定。如果应用需要对网络事件进
行拦截处理，在生成的服务类LinkdService,ProviderService中重载相应的函数。Linkd服务
作为一个项目，可以自由的增加自己的模块。参考 zeze/ZezeJava/Zege/solution.linkd.xml
### 配置（linkd.xml）
```
<?xml version="1.0" encoding="utf-8"?>
<zeze
GlobalCacheManagerPort="5002"
CheckpointPeriod="0"
ServerId="-1"
>
　　Linkd通常不需要数据库，但是验证可能要，先占个坑。
　　<DatabaseConf Name="" DatabaseType="Memory" DatabaseUrl=""/>
　　<ServiceConf Name="LinkdService"
　　InputBufferMaxProtocolSize="2097152"
　　SocketLogLevel="Trace">
　　<Acceptor Port="5100"/>
　　</ServiceConf>
　　<ServiceConf Name="ProviderService"
　　InputBufferMaxProtocolSize="2097152"
　　SocketLogLevel="Trace">
　　如果linkd运行在双网（内外网）机器上，这里可以配置Ip为内部网络的地址，不允许
外部连接。
　　<Acceptor Ip="" Port="5101"/>
　　</ServiceConf>
　　<ServiceConf Name="Zeze.Services.ServiceManager.Agent">
　　<Connector HostNameOrAddress="127.0.0.1" Port="5001"/>
　　</ServiceConf>
</zeze>
```
### Linkd需要的Arch模块初始化
下面的初始化代码部分是生成的，应用需要加入Arch模块的初始化，当然也可以加入任意
自己需要的初始化。Arch模块作为全局变量定义在App中。
```
public void Start(String conf) throws Throwable {
    var config = Config.Load(conf);
    // 生成的初始化
    CreateZeze(config);
    CreateService();
    // Arch模块初始化
    LinkdProvider = new Zeze.Arch.LinkdProvider();
    LinkdApp = new Zeze.Arch.LinkdApp("Zege.Linkd", Zeze, LinkdProvider,
    ProviderService, LinkdService, LoadConfig());
    // 生成的初始化
    CreateModules();
    Zeze.Start(); // 启动数据库
    StartModules(); // 启动模块，装载配置什么的。
    // 设置Session生成器
    AsyncSocket.setSessionIdGenFunc(PersistentAtomicLong.getOrAdd(
    LinkdApp.GetName())::next);
    StartService(); // 启动网络
    // 注册服务。
    LinkdApp.RegisterService(null);
}
```

## Server
Provider的主实现。一个App框架中只有一个主Server。在主Server中实现主要的应用逻
辑。Arch框架不包括自定义的Provider实现。

### ProviderApp

管理Arch模块内部依赖类的引用。也完成一些初始化工作。

### ProviderImplement

处理Linkd转发的来自客户端的请求。客户端请求通过Provider协议包装。

### ProviderService

跟Linkd通讯的网络模块。

### ProviderDirect

处理Server之间互联协议。主要包括ModuleRedirect相关处理。

### ProviderDirectService

Server之间互联网络模块。

### ProviderModuleBinds

Server转发相关配置处理。

### Online

基于账号的在线管理模块。提供给任意在线用户发送消息的接口。

* 本机数据

账号登录在某一个Server实例上时，可以保存一个仅当前Server可见的本机数据。本机数据
每个登录保存一份。

    * SetLocalBean 设置本机数据
    * GetLocalBean 获取本机数据
    * WalkLocal 遍历本机所有的数据，必须在事务外执行。

* 在线事件
  * LoginEvents登录发生时触发。
  * ReloginEvents Relogin发生时触发。这个是可选的功能，如果应用不需要这个逻辑，
  不要在客户端发送Relogin协议即可。这个协议的功能和最小化数据同步相关。当客户端
  异常断线（没有主动发送Logout协议）时，服务器不会马上删除用户的在线状态，会保持
  一段时间。如果用户在这段时间内Relogin，就可以把这段时间内发生的数据变化同步给客
  户端，而且仅同步差异数据。这样就可以快速Relogin。这个功能需要同步差异数据，还需
  要配合ReliableNotify，ChangeListener一起使用。
  * LogoutEvents登出时触发。登出事件可能丢失，在服务器关闭时补发一个Logout的处理
  非常困难（因为程序还可能异常关闭）。现在的逻辑是下一次Login发生时，发现上一个
  Login没有Logout，就补发一个额外的Logout事件。这个特性虽然完成了Login，Logout
  的匹配，但只能用于跟时间无关的逻辑中。如果逻辑需要Login，Logout匹配来完成时间
  统计，由于补发的Logout可能很迟，已经没法正确即使。这种需求需要注意，请自行处理。
  *	LocalRemoveEvents LocalData被删除时触发。
* ReliableNotify（在线可靠消息）
  在线可靠消息被保存在在线数据中，它发送给客户端并需要确认。没有确认的消息在Relogin
  时会被同步给客户端。
  * AddReliableNotifyMark 从这个点开始，启用参数listenerName指定的消息，即调用
    了这个之后的，SendReliableNotifyXXX才真正生效。
  *	RemoveReliableNotifyMark 停止参数listenerName指定的可靠消息。
  *	SendReliableNotify 发送可靠消息。
  *	SendReliableNotifyWhileCommit 事务成功时发送可靠消息。
  *	SendReliableNotifyWhileRollback 事务失败时发送可靠消息。
* SendToLogin（给某个登录发送消息）
  给某个登录发送消息，只有这个登录能收到。
  * SendEmbed 直接在当前事务中执行发送消息逻辑，回滚不会发送。
  * Send 开启一个新的事务发送消息，可以在事务外使用。
  * SendWhileCommit 事务提交时发送消息。
  * SendWhileRollback 事务回滚时发送消息。
* SentToAccount（给某个账号发送消息）
  给某个账号发送消息，这个账号所有的登录都能收到。账号发送方法提供了回调函数。应用
  可以在回调中挨个处理每个登录，进行特殊的处理。没有提供回调函数时(参数是null)，默
  认实现给账号所有的登录发送消息。
```
  SendAccountsEmbed 直接在当前事务内执行发送消息，回滚不发送。
  SendAccount 开启一个新的事务给单个账号发送消息，可以在事务外使用。
  SendAccounts 开启一个新的事务给多个账号发送消息，可以在事务外使用。
  SendAccountWhileCommit 事务提交时发送。
  SendAccountsWhileCommit 事务提交时发送。
  SendAccountWhileRollback 事务回滚时发送消息。
  SendAccountsWhileRollback 事务回滚时发送消息。
```
* Transmit

在分布式架构中，用户被分到了多台Server实例中。当用户A需要查询用户B的数据，发
送给用户X（实际上通常X就是A）。虽然每个Server实例都能直接看到所有用户的数据，
但是如果这个数据查询量大，并且改动非常频繁，那么此时从异机查询会导致Cache失效，
命中率下降。所以提供了这个功能。
```
    Transmit(string account, string clientId, string actionName, string target,
    Serializable parameter = null)
    •	Sender: Account,ClientId 请求发起者，数据结果发送给他。
    •	actionName：具体的查询操作，需要注册。
    •	Target：查询目标用户，
    •	Parameter：查询参数，可选。
```
Transmit会查找目标用户在哪台Server实例中，然后把请求转到他所在的server中执行。
执行的结果直接发回给Sender，不会返回Sender所在服务器。这样，修改和查询都发生在
Target用户所在的服务器，Cache命中率极高。而且整个操作代价不大，仅仅多一个Rpc
转发。
```
    Transmit
    TransmitWhileCommit
    TransmitWhileRollback
```
* Broadcast
  整个世界广播，慎用！

### LoadConfig
负载配置。
### RedirectHash
这是一个注解，被标记的函数会被重定向到另一个Server进程执行。重定向使用第一个函
数的参数hash进行选择。
```
@RedirectHash(ConcurrentLevelSource="getConcurrentLevel(arg1.getRankType())")
public RedirectFuture<Long> updateRank(int hash, BConcurrentKey keyHint, long roleId, 
long value, Binary valueEx) 
```
ConcurrentLevelSource用来获得总的hash分组的数量。
Hash 选择依据，在上面这个例子，hash = roleId.hashCode();
这个功能的使用后面全球同服会继续说明。
### RedirectAll
Server之间直连协议的一种。给所有的分组数据发送广播请求，并处理结果（可能）。有点
像MapReduce。这是RedirectHash的分组广播的注解，用来发送或者接收所有分组的数据。
```
@RedirectAll
protected RedirectAllFuture<RRankList> getRankAll(int hashCount, BConcurrentKey keyHint)
```
### RedirectToServer
这是一个注解，把标记的函数重定向到另一个Server进程执行。相当于一种便利的Rpc机
制。
```
@RedirectToServer
public void redirectNotify(int serverId, String account) throws Throwable {
    // some operate
}
```
ServerA里调用redirectNotify，实际会执行参数serverId所指定的服务器实例（有可能就是
ServerA）。

## Server Initialize
### 定义服务(solution.xml)
```
<project name="server" gendir="." scriptdir="src" platform="java" GenTables="">
    <!--
    这里引用的模块不该定义协议，定义了也不会被生成，一般为纯逻辑或者数据库模块。
    <module ref="CommonModule"/>
    -->
    <!-- service 生成到 solution 名字空间下 -->
    <service name="Server" handle="server" base="Zeze.Arch.ProviderService">
        <module ref="User"/>
        <module ref="Friend"/>
        <module ref="Message"/>
    </service>
    
    <ModuleStartOrder>
    </ModuleStartOrder>
    
    <service name="ServerDirect" handle="server,client" 
        base="Zeze.Arch.ProviderDirectService">
    </service>
</project>
```
Server需要使用Arch中的两个网络模块。在base中指定。需要拦截网路事件，可以在生成
的类中重载相应函数。
### 配置(server.xml)
```
<?xml version="1.0" encoding="utf-8"?>
    <!--
    GlobalCacheManagerHostNameOrAddress: server 启用 cache-sync，必须指定。所有的 
    server 必须配置一样。
    ServerId   每个 server 必须配置不一样，范围 [0, AutoKeyLocalStep)
    AutoKeyLocalStep: 自增长步长。server 实例数量上限。
    -->
    <zeze
        GlobalCacheManagerHostNameOrAddress="127.0.0.1"
        GlobalCacheManagerPort="5002"
        CheckpointPeriod="60000"
        ServerId="0"
    >
    先配一个内存数据库，调试。
    <DatabaseConf Name="" DatabaseType="Memory" DatabaseUrl=""/>
    
    <ServiceConf Name="Server" 
        InputBufferMaxProtocolSize="2097152" 
        SocketLogLevel="Trace">
    </ServiceConf>
    
    <ServiceConf Name="Zeze.Services.ServiceManager.Agent">
        <Connector HostNameOrAddress="127.0.0.1" Port="5001"/>
    </ServiceConf>
    
    <ServiceConf Name="ServerDirect" 
        InputBufferMaxProtocolSize="2097152" 
        SocketLogLevel="Trace">
        <Acceptor Ip="" Port="5102"/>
    </ServiceConf>
</zeze>
```
### Server需要的Arch模块初始化
下面的初始化代码部分是生成的，应用需要加入Arch模块的初始化，当然也可以加入任意
自己需要的初始化。Arch模块作为全局变量定义在App中。
```
public void Start(String conf) throws Throwable {
    var config = Config.Load(conf);
    CreateZeze(config);
    CreateService();
    // 初始化Arch模块。
    Provider = new Zeze.Arch.ProviderWithOnline();
    ProviderDirect = new Zeze.Arch.ProviderDirect();
    ProviderApp = new Zeze.Arch.ProviderApp(Zeze, Provider, Server,
    "Zege.Server.Module#", ProviderDirect, ServerDirect,
    "Zege.Linkd", LoadConfig());
    // 初始化可选的Arch模块
    Provider.Online = GenModule.Instance.ReplaceModuleInstance(
    this, new Online(this));
            // 初始化可选的Zeze模块
    LinkedMaps = new LinkedMap.Module(Zeze);
    DepartmentTrees = new DepartmentTree.Module(Zeze, LinkedMaps);
    // 生成的代码
    CreateModules();
    Zeze.Start(); // 启动数据库
    StartModules(); // 启动模块，装载配置什么的。
    // Online初始化
    Provider.Online.Start();
    // 其他初始化
    PersistentAtomicLong socketSessionIdGen = PersistentAtomicLong.getOrAdd("Zege.Server." 
    + Zeze.getConfig().getServerId());
    AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);
    StartService(); // 启动网络
    ProviderApp.StartLast(ProviderModuleBinds.Load(), Modules);
}
```
## 发现Linkd,Server流程
* Linkd.Startup
```
{
　　ServiceName = “Game.Linkd”; // 应用自定义。
　　ServiceIdentity = Ip + “:” + Port; // Linkd监听地址和端口，接受来自Provider的连接。
　　RegisterService(ServiceName, ServiceIdentity, Ip, Port, extra);
　　// Linkd一开始不会订阅Server的服务信息，在后面的发现流程中才会订阅。
}
```
* Server.Startup
```
{
　　// 每个模块作为一个服务。下面注册和订阅模块服务。
　　// 每个Server的CurrentModules是可以配置的。
　　Foreach (var Module in CurrentModules)
　　{
　　ServiceName = “Game.Server.Module#” + Module.Id; // 名字前缀应用自定义。
　　ServiceIdentity = Zeze.Config.ServerId; // Zeze服务器编号。
　　// ProvideIp, ProviderPort：Server监听地址和端口，接受来自其他Server的连接。
　　RegisterService(ServiceName, ServiceIdentity, ProvideIp, ProviderPort, extra);
　　SubscribeService(ServiceName, Module.SubscribeType);
　　}
　　// 订阅Linkd服务器列表
　　SubscribeService(“Game.Linkd”, Simple);
　　// 报告一次初始负载。
　　Load.Report(0, 0); // 0 online 0 new
}
```
* Server发现Linkd
```
1.	Linkd.Startup向ServiceManager注册自己。
2.	ServiceManager广播，Server会收到。
3.	Server连接Linkd。
4.	Server向Linkd发送Bind协议，马上绑定自己支持的静态模块。
5.	Linkd收到Bind。完成模块绑定，并订阅(SubscribeService)该模块服务。
```
* 发现Server
```
1.	Server.Startup向ServiceManager注册自己支持的所有模块服务。
2.	ServiceManager广播，所有订阅者（Linkd和Server）都会收到。
3.	Linkd收到Server列表变更不做任何处理，只是等待Server连接过来并处理Bind。
4.	Server收到待定的列表时，开始连接新的服务器。
5.	连接建立后，主动方发送自己的地址和端口给被动方，设置本地状态为Ready。
6.	连接建立后，被动放收到主动方的地址和端口，设置本地状态为Ready。
```

## Session &amp; UserState
1.	AsyncSocket.UserState连接上下文，一般在连接创建的时候初始化，用来保存跟链接相关的
      自定义状态，用来实现Session功能。
2.	Protocol.UserState 协议上下文，从某个连接收到的协议的上下文，默认设置为该连接的
      UserState。框架根据自己的需要可以设置新的Protocol.UserState。
3.	Procedure.UserState存储过程上下文，为协议的处理而创建的存储过程，默认设置为该协议
      的UserState。框架可以根据自己的需要修改存储过程的UserState。

See ProviderImplement. ProcessDispatch

## Provider Protocol
### Bind
Provider发送给Linkd，绑定自己支持的Module。
1.	静态绑定
      绑定时不指定客户端会话，绑定信息存储在Linkd的ProviderSession中。Linkd收到
      静态绑定内的模块的第一个协议请求时，根据这个模块的配置进行负载选择，选择
      一个Provider实例派发请求，同时所有静态绑定的模块都和客户端会话关联起来。
      以后所有静态绑定的模块的请求都会派发给这个Provider实例。
      TODO静态绑定现在被主Server用来注册通用模块。目前静态绑定只有一个唯一的
      集合，所以不能跨越多个Provider。考虑支持多个集合的静态绑定。
2.	动态绑定
      绑定模块到客户端会话。以后这个模块的所有协议都会被Linkd派发给这个Provider。
      这个模块也被称作动态模块。

### UnBind
Provider发送给Linkd，解除绑定。

### Subscribe
Provider发送给Linkd，通知Linkd订阅动态绑定的模块的服务信息。动态绑定的模块没有
只能绑定到某个客户端会话，在绑定前，Linkd一无所知。某些Linkd的功能可能需要动态
模块的服务信息。这时Provider可以通过Subsrcibe协议告知Linkd。这个能力是可选的，
目前Arch实现了这点，但没有真正用到它，算是未来扩充Linkd功能的准备。

### Kick
Provider发送给Linkd，踢掉某个客户端会话。

### Send
Provider发送给Linkd，转发协议给指定的客户端。

### Broadcast
Provider发送给Linkd，向Linkd内所有客户端会话广播。

### SetUserState
Provider发送给Linkd，设置客户端会话的状态。这个状态以后在Linkd发送客户端会话相
关的请求给Provider时，原样带上。这个功能一般用来实现LoginSession。

### Dispatch
Linkd发送给Provider，转发客户端请求。

### LinkBroken
Linkd发送给Provider，报告客户端连接断开。

### AnnounceLinkInfo
Linkd发送给Provider，通告Linkd的信息。这是一条保留协议，目前没有报告任何信息。

### AnnounceProviderInfo
Provider发送给Linkd，报告Provider的基本信息。

## 全系统维护启动顺序
1.	启动ServiceManage
2.	启动GlobalCacheManager
3.	Linkd Server任意顺序启动。

## 全系统维护停止顺序
1.	停止ServiceManager 必须
2.	Linkd.AcceptorClient.close() 阻止新用户进来
3.	Server发送广播通知用户下线
4.	Server等待一定时间后关闭
5.	Linkd关闭
6.	GlobalCacheManager关闭

## ProtocolRef

Linkd一般是没有数据库的，它提供给客户端使用的服务LinkdService里面的协议一般需要
转给其他系统实现，如果有少量的协议需要转给GameServer处理（比如Auth协议），可以
使用protocolref把相关协议的处理句柄定义到GameServer的模块内。比如LinkdService的
协议Auth进行验证服务，可以在GameServer的模块User内定义&lt;protocolref
ref=”Linkd.Auth”&gt;，然后Linkd的ProcessAuthRequest把请求转给GameServer并在其中处
理。
Linkd可能对协议的转发一般是透明的，有时候需要拦截客户端协议，填充一些只有Linkd
知道的信息（比如填充Linkd看到的客户端的Ip），然后再Dispatch给GameServer。此时可
以在linkd的任意模块内定义&lt;protocolref import=”GameServer.SomeNeedIpReq”&gt;。
这个定 义将把GameServer的相关协议以及Bean引入到linkd内，然后Linkd的定制
LinkdService.DispatchUnknownProtocol，拦截这条协议，并填充，再重新Encode，再调用
基类方法派发(super.DispatchUnknownProtocol)。

## Linkd-GameServer内部信息服务
当应用自己的linkd有信息需要暴露给GameServer查询，或者linkd需要查询GameServer
的信息，可以定义一个自己的模块，并把这个模块引入ProviderService。这样linkd和
GameServer之间就可以互相提供服务了。这个功能的例子，比如，linkd提供操纵黑名单的
功能。

## 影响Linkd选择Provider的开关

* ProviderService. setDisableChoiceFromLinks

禁止或允许linkd选择自己。一般用于优雅的关闭服务器，设置了这个选项以后，新的登录
不会再分配过来，然后等这台服务器上现有的登录处理的差不多了，就可以安全关闭了。

* maxAppVersion

应用定义自己的版本号，如 enum AppVersion { Version = 1 }，这个版本号每个版本手动修
改。在初始化的时候，通过Schemas的方法setAppPublishVersion设置到zeze中。Zeze.Arch
会自动报告给Linkd，linkd只会给最新版本的服务器派发新登录。这个可用于慢慢的一台一
台方式重启更新服务器，保证在更新过程中，新登录不会被派发到老版本中。

* 节流过载保护

这个功能是自动的。当provider过载时，linkd自动不会转发新的登录过来。Online组件会
注册过载配置的线程池，当线程池堆积的任务超过配置值，会设置节流过载，并通过
ServiceManager报告自己的负载，Linkd订阅得到负载以后会根据它的负载决定派发。相关
配置Config. providerThreshold = 3000, Config.providerOverload = 5000.

* 服务发现

Zeze的逻辑服务主要实现在Server中，是以模块为单位注册和发现的。在Server启动准备
好之前，一般需要避免请求到达。默认情况下，当用户在App.Start()里面调用
providerApp.startLast()之后，服务就会被其他使用的地方发现，请求就能到达了。但是Server
有一些复杂初始化可能会在startLast之后初始化，此时服务还没有完全准备好。Server被
Linkd发现，完全准备好，并能处理新的来自客户端用户的请求，提供了一些设置方法。
禁止在程序完全准备好之前Linkd派发新的用户请求。
```
App.Start() {
    …
    ProviderService.initDisableChoice(true); // 需要在startService()之前设置。
    …
    startService();
    …
    providerApp.startLast();
    … // startLast之后的初始化
    ProviderService.setDisableChoiceFromLinks(false); // 开门放狗。
}
```

* Redirect服务准备好的说明

Redirect是Server之间直连的服务，虽然服务发现也遵循模块注册（startLast），但是Server
之间不容易实现明确的禁止选择策略，所以Redirect也会碰到服务没有准备好的问题。对这
个问题，暂时不提供解决方案，仅提供几条说明。
1.	redirect推荐按不可靠服务来使用。就是服务不存在或者失败时，调用者应该可以忽略
      错误。按这个原则，启动过程中出错或者拒绝也是合理的了。
2.	对于按ServerId注册到自己的注册中心，然后RedirectToServer方式使用的服务，定义
      为应用自行处理。比如地图服务器分配地图实例的Redirect，服务启动完全准备好才注
      册到自己的分配中心，中途避免新的分配进来。这样可以绕过Zeze不提供Redirect手
      动控制客户端请求的问题。
3.	startLast才会开启模块注册，所以此后服务器（模块）才会被其他服务器知道。按这个
      规则，推荐redirect的实现，在startLast之前准备好。对于无法这样处理的Redirect
      服务，也就是说startLast之后的初始化对于redirect来说，需要自己在收到请求后处理
      并拒绝。

