# Inside Zeze

## 协议基础
zeze框架所用的协议是为TCP payload的流特性而设计的，一条协议紧接下一条协议在流
中传输。每条协议的头部结构都能确定这条协议的完整长度，这样就能从数据流中方便地先
取出一条完整的协议数据再去进一步解析其中的内容。为了方便在解析内容前确定解析者，
协议头部还需类型字段。因此一条协议的序列化结构设计如下(见Zeze.Net.Protocol::Decode)：
```
　　uint32 moduleId // 模块ID
　　uint32 protocolId // 协议ID
　　uint32 size // 协议数据长度
　　byte[size] data // 协议数据
```
其中uint32的序列化是小端字节序，后面提到的固定长度整数和浮点数类型均指小端字节
序。

模块ID = 0用于定义框架内部协议，而具体应用应该从1开始定义。每个模块都有完整的
32位整数范围的协议ID可用。

协议类型是由高32位的moduleId和低32位的protocolId组合成64位的整数。
框架根据协议类型找到注册过的协议处理器(Zeze.Net.Service.ProtocolFactoryHandle)，处理
器包括协议对象的构造工厂、处理方法和事务类型。处理时先用构造工厂构造出对象，再用
协议数据调用对象的Decode方法反序列化出协议内的各个成员，最后根据事务类型选择合
适的方式调用协议处理方法并传入协议对象。

除了普通协议(Zeze.Net.Protocol)，还有一类协议叫RPC(Zeze.Net.Rpc)。普通协议只包含一
个bean类(Argument)；而RPC包含两个bean类(Argument和Result)。普通协议只能单向
请求，而RPC能往返发送请求(Argument)和回复(Result)。

```
普通协议的序列化结构如下：
　　varint ResultCode // 协议处理结果状态(其实对普通协议来说可能没什么用)
　　Bean Argument // 协议请求的bean对象(包含各请求字段)
RPC的序列化结构如下：
　　bool IsRequest // true:请求; false:回复
　　varint SessionId // 请求和回复的SessionId相同，用于回复时找到对应的请求，由
发送方确保唯一
　　varint ResultCode // 协议处理结果状态
　　Bean ArgumentOrResult // 根据IsRequest决定序列化Argument还是Result
还有一类特殊的RPC叫RaftRpc(Zeze.Raft.RaftRpc)，其序列化结构在RPC的
ArgumentOrResult前插入：
　　UniqueRequestId Unique // 唯一的请求ID。用于确定重发时去重，而SessionId在
重发时仍然会更新
　　    string ClientId // 客户端标识
　　    varint RequestId // 唯一的请求ID的整数值
　　varint CreateTime // RaftRpc的创建时间，重发时不会改变
```

　　模块和协议在一个或若干个xml配置文件中手动定义，由框架的Gen工具生成模块类、
协议/RPC类、bean类等代码。其中bean类包含各字段的序列化(Encode/Decode)方法，各
种数据类型(包括上面提到的varint、bool和string)的序列化规则详见框架中的文档
(doc/zeze序列化标准定义.txt)。

　　无论是客户端和服务器之间的协议还是服务器之间的协议都按照以上规则传输请求和
回复，如果需要对协议加密或压缩，则应在协议序列化之后、反序列化之前转换数据。

　　在框架的封装下，发送协议只需构造协议对象并调用其Send方法即可异步发送；处理
协议只需在生成的模块类里找到生成的协议处理空方法，在里面编写对协议对象参数的处理。
如果发送RPC请求，可调用SendForWait获取Future并同步等待，或者调用带
responseHandle的Send方法异步处理回复。接收RPC请求并处理后必须调用该RPC的
SendResult方法回复到请求方。请求方收到RPC回复时，框架会根据对应的请求RPC中有
Future则SetResult打断同步等待，有ResponseHandle则调用其方法处理回复。

　　发送RPC请求时，框架会生成新的SessionId并记录该RPC，以备接收该RPC的回复时
找到对应的请求RPC。收到回复时框架会给对应的请求RPC赋上Result bean再进一步处理。
每个RPC都有指定的超时时间，如果请求方发现某RPC超时则设置其ResultCode为
Zeze.Transaction.Procedure.Timeout再处理其Future或ResponseHandle。之后如果再收
到该RPC的回复则因无法找到对应SessionId的请求RPC而在日志中报错并忽略其处理。

## 客户端登录流程
(一) 网络连接
```
(1) linkd:Zezex.LinkdService(默认11000端口) <= 客户端 (只有此连接配置了加密和压
缩)
(2) linkd:Zezex.ProviderService(默认21000端口) <=
server:Zeze.Arch.ProviderService
(3) ServiceManagerServer(默认5001端口) <=
linkd:Zeze.Services.ServiceManager.AgentClient
(4) ServiceManagerServer(默认5001端口) <=
server:Zeze.Services.ServiceManager.AgentClient
(5) Zeze.Services.GlobalCacheManagerAsyncServer$ServerService(默认5002端口) 对
于单server暂时没有用
(6) 以上网络连接均使用下面的通用握手协议和流程。
```

(二) 通用的握手协议和流程(Zeze.Services.Handshake*)

1. TCP握手成功后，服务端先发送SHandShake0给客户端，其中唯一的参数(bool
EnableEncrypt)表示是否启用加密(可配置)。不过旧版框架没有这个协议，由客户端自己决
定是否加密(应该跟服务器保持一致的配置)。
2. 客户端收到SHandShake0后，如果EnableEncrypt=false，则跳到下面的步骤(6)。
3. 发送CHandshake给服务器，其中dh_group默认为1(可配置)，dh_data是2^本地生
成的随机大整数与dh_group指定大整数常量的模(详见Zeze.Services.Handshake.Helper)。
4. 服务端收到CHandshake后，计算dh_data^本地生成的随机大整数与dh_group指定大整数
常量的模，经过HmacMd5的结果作为加密key，加密算法为"AES/ECB/NoPadding"，默认同
时启用双向压缩(可配置)。先启用网络输入的加密压缩，然后发送SHandshake给客户端，
其中dh_data是2^本地生成的随机大整数与dh_group指定大整数常量的模，
s2cNeedCompress和c2sNeedCompress表示是否启用双向压缩(均可配置)，发送后即开启网
络输出的压缩加密(详见Zeze.Services.HandshakeBase)。
5. 客户端收到SHandshake后，同服务器的算法计算出一致的加密key并按协议参数中的
s2cNeedCompress和c2sNeedCompress确定是否启用双向压缩。
6. 客户端发送CHandshakeDone给服务器，并回调Service的OnHandshakeDone通知协议层握
手成功。
7. 服务端根据配置和当前的加密状态做验证，成功后回调Service的OnHandshakeDone通知协
议层握手成功。

(三) 客户端应用层登录流程

1. 以上的协议层握手成功后，客户端给linkd发送认证请求(RPC:Zezex.Linkd.Auth)，请
求包含账号名及token。
2. linkd认证成功后，回复客户端成功(通过RPC回复ResultCode，目前总是成功，没有
失败流程)。
3. 客户端确认认证成功后，开始通过linkd跟server通信(通过Dispatch/Send封装协议)
4. 客户端先向server发送角色列表请求。 
5. server回复到客户端角色列表后，客户端判断如果角色列表为空，则继续向server请
求创建角色，否则跳到步骤(7)。 
6. server创建角色后回复客户端成功，失败则通过指定的错误ResultCode回复。 
7. 客户端选择已有角色后，向server发送登录角色请求，请求包含登录的角色ID。 
8. server确认登录角色后回复客户端成功，失败则通过指定的错误ResultCode回复。 
9. 后续客户端还会跟server继续主动获取角色信息及进入世界等协议/RPC。

## 客户端与server的协议包装
1. linkd的LinkdService在收到linkd未知的协议时会封装成
Zeze.Builtin.Provider.Dispatch协议，根据未知协议的模块ID找到所属的server服务
器并发送给它，如果找不到则回复客户端Zeze.Builtin.LinkdBase.ReportError然后关闭
连接。 
2. server收到Dispatch协议后，根据其中的协议类型找到ProtocolFactoryHandle，创
建协议对象、反序列化并根据事务执行方式调用内部协议的处理方法，其中协议的
UserState记录了所属的linkd及其连接客户端的linksid、账号名等上下文。 
3. server通过linkd向客户端发协议(包括回复RPC)时，需要把协议封装成
Zeze.Builtin.Provider.Send协议并发送给linkd，linkd在处理Send协议时根据其中的
linksid向客户端发送内部协议数据。一个Send协议支持附带多个linksid实现小范围广
播。 
4. server需要全服广播协议时，把该协议封装成Zeze.Builtin.Provider.Broadcast协议，
然后广播给所有连接的linkd。

## 客户端登出流程
1. 客户端向服务器发送登出请求(Zeze.Builtin.Online.Logout)。 
2. 如果服务器找不到上下文所属的角色ID和登录状态则回复失败；否则向所属linkd发
送Zeze.Builtin.Provider.SetUserState协议清除linkd的状态，再回复Logout成功并清
除登录状态，然后让各模块处理角色登出。 
3. 客户端收到Logout回复成功后，主动断开网络连接。

## 服务器之间的协议
1. 服务器可以有多个linkd和多个server。其中所有的linkd跟所有的server都有连接
(通过ServiceManager注册和订阅)。而linkd之间没有连接，server之间有完全连接(总
是小的serverId主动连接大的serverId)。 
2. 服务器之间的连接通常不配置加密和压缩，协议和RPC直接发送无需包装。 
3. linkd注册的服务信息：服务名:"Game.Linkd"; 服务
ID:"@{providerIp}:{providerPort}"。
4. server注册的服务信息: 服务名:"Game.Server.Module#{moduleId}"; 服务
ID:"{serverId}"。 
5. server订阅"Game.Linkd"服务名，得到所有linkd的provider的IP和端口并主动连接，
server在协议层握手成功后主动给各linkd依次发送
Zeze.Builtin.Provider.AnnounceProviderInfo协议、
RPC:Zeze.Builtin.Provider.Bind(静态模块)和
RPC:Zeze.Builtin.Provider.Subscribe(动态模块)。 
6. server订阅"Game.Server.Module#{moduleId}"各模块的服务名，得到所有server的
providerDirect的IP和端口并主动连接比自己serverId大的server，在协议层握手成功
后主动连接方给对方发送Zeze.Builtin.Provider.AnnounceProviderInfo协议。

## 协议的字段兼容性
1. 所有的数值类型(byte,short,int,long,float,double)之间互相兼容(运行时透明自动
转换)，转换规则跟所用编程语言的强转相同。所以注意高位截断等问题，通常应该从小范
围类型改成大范围类型。 
2. bool类型跟数值类型也互相兼容，bool转数值成0和1，数值转bool会用"!=0"来取结
果。 
3. binary和string类型互相兼容，string的序列化是用UTF-8编码成binary，binary
转string时如果遇到无法UTF-8解码时会抛异常。Lua的string因为无需编解码所以跟
binary完全兼容。 
4. bean类型的兼容性只看字段ID和字段类型，与bean的命名和类型ID无关。序列化数
据里缺失的字段会当成默认值(0,false,空binary,空string,空容器,所有字段均为默认值
的bean,内容是EmptyBean的DynamicBean)。如果序列化数据里有当前bean类型中不存在
的字段ID，则直接忽略，再序列化时就会丢弃该字段。 
5. dynamic类型只是附带类型ID而不是根据配置选择具体类型名的bean，根据配置指定
的方法用类型ID取得具体的bean类型再反序列化。如果改成bean类型，则忽略类型ID
直接按指定的具体bean类型反序列化。如果从bean类型改成dynamic类型，则用默认0
当作dynamic的类型ID。 
6. 序列容器(list,set,array)互相兼容，容器内类型的兼容性同上。 
7. 关联容器(map)内key和value的类型兼容性同上。 
8. 以上没提到兼容性和转换规则的转换均不支持，遇到则用默认值取代旧值。 
9. 更多相关规则详见框架中的文档(doc/zeze序列化标准定义.txt)
10. 关于bean增减字段的建议：按字段ID的顺序从1开始自增地分配和扩展；删除字段
不要直接删除，可以修改字段名或注释来表示“临时不再使用”的含义，方便保留数据库中
已有的数据不丢失，以备之后再恢复使用，也防止增加字段时重用该字段ID引发取出旧数
据的混乱。如果有彻底全服删库的机会，可以删除不会再用的字段，此时也可以顺便重新整
理所有的字段ID。

## ModuleRedirect流程
1. App类在创建模块时会调用ReplaceModuleInstance方法，它发现模块类中有修饰了以
下几种注解的转发方法就会使用转发类替代原模块类，它会先判断当前类路径中是否已经存
在转发类，如果没有再为此模块生成转发类代码；生成后不会自动生成文件，而是配置了
JVM属性"GenFileSrcRoot=输出路径"才会输出到文件，命名为"Redirect_模块全名.java"，
其中"模块全名"中的点号转成了下划线。生成的类继承原模块类并重写所有转发方法，其构
造方法会注册所有转发方法全名(包名:方法名)及对应的处理方法。通常App类在创建并
Replace所有模块后，判断配置了GenFileSrcRoot就直接退出，用于只生成代码而不启动
服务。除RedirectAll外其它各转发类型的转发流程大同小异。一共三种转发类型如下： 
2. Zeze.Arch.RedirectToServer：单发给首个参数"int serverId"的模块执行该方法并回
复结果，找不到serverId会抛异常，serverId也可以是当前的server，此时会自动同步调
用原有的方法。返回类型可以是void或Zeze.Arch.RedirectFuture<自定义结果类型或
Long(resultCode)>。自定义结果类型必须是public的，传输结果的字段也必须是public
的且支持普通值类型或可序列化(Zeze.Serialize.Serializable)的类型，请求方法的其它
参数也有此要求。自定义结果类型可选包含特定的字段"long resultCode"用于接收执行结
果的状态。执行者的server在收到转发请求
(Zeze.Builtin.ProviderDirect.ModuleRedirect)时会运行模块原有的方法代码，通过
RedirectFuture.finish传入自定义结果类型或Long(resultCode)回复请求者的server，
可异步回复，方法返回不会自动回复。请求者的server可对返回的RedirectFuture调用
await来同步等待结果，或者调用then传入回调来异步处理结果，这两个方法可连接使用，
如"...(...).then(...).await()"。获取到结果应先判断resultCode是否超时。 
3. Zeze.Arch.RedirectHash：类似RedirectToServer，只是首个参数换成"int hash"，
根据hash值选取一个提供了该模块的server。其中该注解可附带一个
ConcurrentLevelSource字符串参数，表示获取并发级别的来源代码，该代码会嵌入到生成
代码中，并发级别默认是1。如果并发级别<=1，则选取server的规则是在提供了该模块的
服务列表中以一致性hash算法(见Zeze.Util.ConsistentHash)选取一个server；如果并发
级别>1，则先对hash以并发级别取余并hash后再用一致性hash算法选取一个server。 
4. Zeze.Arch.RedirectAll：用于广播给多个提供该模块的server执行并收集所有结果。
要求首个参数必须是"int hash"，请求发起者传入该参数表示并发级别，并发级别多于
server总数时某些server需要处理多次，各执行者可从hash参数得到[0，hash-1]中某一
值。方法的返回值必须是RedirectAllFuture<继承RedirectResult的自定义类型>，自定
义结果类型的要求类似RedirectToServer，但不必定义resultCode可以从其父类
RedirectResult中获取hash和resultCode。执行者的server运行原方法时返回
"RedirectAllFuture.result(传入自定义结果类型)"来同步回复结果，如需异步则返回
"RedirectAllFuture.<自定义结果类型>async()"，其返回对象在其它方法中调用其
"asyncResult(传入自定义结果类型)"方法来完成回复。请求者的server可对返回的
RedirectAllFuture调用await来同步等待结果，调用onResult来处理每个结果，调用
onAllDone来得到RedirectAllContext对象，可获得所有结果。这三个方法可连接使用，
如"...(...).onResult(...).await().onAllDone(...)"。获取到每个结果都应先判断
resultCode是否超时。 
5. 以上各种转发方式的用法示例见框架项目中的Game.Rank.ModuleRank。 
6. ModuleRedirect协议不通过linkd转发，而是在启动时通过ServiceManager注册和订
阅，获得所有server提供的模块信息，直接在servers之间建立连接并收发ModuleRedirect。

## 多线程基础
使用Zeze几乎不需要了解多线程相关知识，深入理解Zeze则需要掌握一定的多线程知识。
Zeze使用了Mutex,Condition,ReadWriteLock,volatile,Future(TaskCompleteSource)。多线程
有系统提供的同步原语可用，可以分析公共数据是否都具有合适的保护，执行流程是否死锁。
普通多线程还是可以进行有效分析的，相对异步来说要简单点。一般应用内死锁自己都可以
避免，但是框架有时需要在锁内执行用户操作，这很可能带来死锁风险。异步问题一般都通
过状态处理，比如Zeze/Raft/就是异步的，这个更难分析。多线程、异步问题的本质一样的，
这里有个我叫做“时间窗口敏感性“的东西，也就是说你能主动察觉到某些地方可能有问题，
需要通过加同步原语保护或者加状态检测。《UNIX网络编程 卷2 进程间通信 第2版》旧
版里面的信号安全的处理（由于信号这个东西基本没人用了，新版不知道有没有被删除），
可以锻炼自己的时间窗口敏感性。最后举个异步的例子：
系统有Login，Logout两个操作，当Logout的执行滞后了，在新的Login之后才执行，那
它就会出问题，这个问题无法用锁（也不能在事务内保护）简单解决，一个可行的处理方案
是使用Login系列号，Logout时判断当前的Login状态（系列号）根自己的不一样，忽略
这个Logout的执行。

## 事务怎么实现回滚
修改数据时在当前事务中记录日志，在事务提交时，提交日志。Zeze只在Transaction中提
供GetLog，PutLog两个接口，另外为不同的修改提供了几个日志类。日志的访问和创建都
在Bean的生成代码中实现。下面是一个简单的Bean的生成代码例子（不完整）：
```
public final class Simple extends Zeze.Transaction.Bean {
    private int _int1; // com aa
    public int getInt1() {
        if (!isManaged())
            return _int1;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _int1;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__int1)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _int1;
    }

    public void setInt1(int value) {
        if (!isManaged()) {
            _int1 = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__int1(this, 1, value));
    }

    private static final class Log__int1 extends Zeze.Transaction.Logs.LogInt {
    public Log__int1(Simple bean, int varId, int value) {
        super(bean, varId, value);
    }

    @Override
    public void Commit() {
        ((Simple)getBelong())._int1 = Value; }
    }
}
```
完成的类型和容器的日志使用例子可以到zeze\ZezeJava\ZezeJavaTest下运行gen.bat，并
查看zeze\ZezeJava\ZezeJavaTest\Gen\demo\Module1\Value.java

## 事务和乐观锁
存储过程执行过程中不加锁，所有修改仅当前事务可见。提交的时候对所有访问的记录排序
并且加锁并进行冲突检查。

## 乐观锁并发原理
事务内所有访问（读写）的记录在冲突检查时需要确保Timestamp没有变化。事务成功时，
相当于独占所访问的记录。这个并发策略是严格，但显然是正确的。
乐观锁算法要点
1. 排序加锁，实际上所访问的记录存储在SortedDictionary中。
2. 加锁后检查冲突，即数据是否改变。冲突则重做事务。
3. 冲突重做时保持已经得到的锁，这样在冲突非常严重时，第二次执行事务一般都能成功，而
不会陷入一直冲突，事务永远没法完成的情况。
4. 重做保持锁，但重做过程中所访问的记录可能发生变化，所以重做仍然需要再次执行
lockAndCheck逻辑，并且处理所访问的记录发生变更的问题。
5. 逻辑处理返回错误码或者异常时也需要检查lockAndCheck，因为乐观锁在实际处理逻辑时
没有加锁，可能存在并发原因导致本来不应该发生逻辑错误，此时的仍然需要加锁并完成冲
突检查，如果冲突了，也需要重做。
6. 完整的实现参见 Zeze.Transaction.Transaction. lockAndCheck._

## 一致性缓存（分布式事务）
多台服务器共享后台数据库。每台服务器拥有自己的缓存。一致性缓存就是维护多台服务器
之间缓存的一致性。zeze一致性缓存和CPU-Cache-Memory的结构很像。所以参考了CPU
的MESI协议自己实现了一个锁分配机制。这个一致性缓存思路是非常暴力的，核心出发点
就是Global是全能的，知道所有东西。所以算法也非常直接简单。使用了类似MESI的状态
名，记录分成读写不可用三种状态。不可用表示记录本地还没有权限，读状态允许同时存在
于多台服务器缓存中，写状态只允许在一台服务器中。在一致性缓存之上，每一台服务器的
事务就能像自己独占所有数据一样，完成本地事务即可。这就是基于一致性缓存的分布式事
务。

### 锁管理流程
参考了CPU缓存同步算法（MESI），使用了其中3个状态：Modify,Share,Invalid。当主逻辑
服务器需要访问或修改数据时，向全局锁管理服务器（下面用Global称呼）申请Modify或
Share锁。Global知道所有记录的锁的分布状态。它根据申请的锁，向现拥有者发送相应的
降级请求；拥有者释放锁，并把数据刷新到后端服务器后，才给Global返回结果；Global
登记申请者的锁状态，给申请者返回结果。

### 锁管理算法要点
1. Global在多个记录上并发执行Acquire操作。对单个记录，所有的申请排队互斥，一个一个
处理。
2. Global处理Acquire时，除了死锁检测会马上返回失败，多数情况下都会返回成功。
3. 如果拥有锁的逻辑服务器（下面简称Server）没有响应Reduce请求（超时），此时实际发
生的情况没法预测，Acquire会失败。
4. 允许Global认为记录权限已经分配个某个Server，但Server实际上没有（比如Server重启
了）或拥有较低权限。所以，Server处理Reduce，必须能正确处理Recude的目标状态和自
己实际状态，并且返回成功。
5. Server在多个记录上并发处理权限申请；对单个记录，所有的Acquire排队。对同一个记录，
不会同时发送Acquire给Global。
6. 实现：Zeze\Services\GlobalCacheManager.cs
a) AcquireModify,
b) AcquireShare
c) Release
7. 实现：Zeze\Transaction\Table.cs
a) ReduceShare
b) ReduceInvalid
c) Load

## 持久化模式
CheckpointMode .Period：定时保存修改到后端数据库，如果保存前进程异常退出，修改会
丢失，相当于上一次保存以来的所有事务回滚，数据不会被破坏。这个模式仅用于单机模式，
不能和一致性缓存一起使用。

CheckpointMode .Immediately：事务提交的时候马上保存到后端数据库。【废弃，是否恢复
以后再说】

CheckpointMode .Table：可以配置某些表是重要的，当事务访问的表有重要的时，这个事
务的所有修改会马上保存。事务访问的表全部不是重要的，这些修改定时保存。这个模式适
用范围比较广。由于zeze是KV数据库，一个记录即使只变了一部分，也需要整体写入，
存在一定浪费（系列化和io成本）。定时保存可以降低后端数据库的写入量。仅把真正重要
的表配成马上保存。

主要实现： Zeze/Transaction/Checkpoint.cs Zeze/Transaction/RelativeRecordSet.cs

## TableCache
1. TableCache是Zeze性能的根本，需要配置合适的容量。 
2. 事务中删除记录不会马上从TableCache中删除，这样可以实现负Cache。 
3. TableCache定时执行清理操作，按Lru规则删除最老的记录。 
4. 记录正在被访问、脏的、新鲜的等状态时不会被清理。
