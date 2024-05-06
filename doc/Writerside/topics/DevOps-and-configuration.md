# 运维和配置

所有的下面出现的具体配置值都是程序默认值。除非特殊目的和某些重要的需要关注的配置，
建议直接使用默认值。不提供配置就是默认值。
Arch的linkd，server的配置文件的绝大部分内容都建议一致，实际上zeze配置设计的一个
目标是某个服务的所有分布实例都共享一份配置文件，当然由于其中的ServerId配置必须不
同，使得这个目标打了折扣。
总的来说，除了ServerId每个服务实例必须不同，其他配置应该都一样。这句话在硬件差别
比较大，需要对局部机器的配置进行调整除外。

## 分布式
```
Server配置如：<zeze ServerId="0" …>…</zeze>，其中ServerId时分布式每个服务实例的
编号，必须唯一。这是server配置中唯一的每一台需要不同的配置。
Linkd配置如：<zeze ServerId="-1" …>…</zeze>，linkd实际上不需要ServerId，它默认使
用”ProviderIp:ProviderPort”作为自己名字。所以可以全部linkd实例的配置都是用-1。但有
些应用可能想通过ServerId区分，这时建议-1，-2，-3，…方式进行配置。也有一些人采用
正数，但需要注意不要和Server.ServerId重叠，也要注意以后server扩容以后是否达到保留
的linkd的正数的Id。
```

## 事务持久化
```
<zeze … 
　　CheckpointPeriod="60000"
　　CheckpointMode="Table"
　　CheckpointFlushMode="MultiThreadMerge"
　　CheckpointModeTableFlushConcurrent="2"
　　checkpointModeTableFlushSetCount=”50”>
</zeze>
```
这些配置都是默认值。一般应用可能需要调整CheckpointPeriod，其他几个配置都是不必要
的。

## CheckpointPeriod &amp; CheckpointMode
Zeze对多数事务采用定时持久化的策略，CheckpointPeriod时定时器的间隔。
CheckpointMode配置持久化模式，有Period，Immediately，Table三种模式。其中Period
用于单机模式，就是单纯的定时持久化，在合适的CheckpointPeriod下效率很高。
Immediately是所有的事务都立即持久化，效率最差，一般特殊情况下可能采用，不建议。
Table是根据事务的关联性，以事务的关联集合为单位持久化，实际上也是定时的，和Period
一样也使用CheckpointPeriod配置定时间隔持久化。分布式模式（启用了Global）下，必
须使用Table持久化模式。事务的持久化还有一个特殊的模式，可以把部分表配成马上持久
化，这个表相关的事务完成的时候就持久化，当事务返回就表示持久化已经完成了。这个参
见后面TableConf.CheckpontWhenCommit。

## 持久化的线程模式
CheckpointFlushMode和CheckpointModeTableFlushConcurrent配置持久化的线程模式。
CheckpointFlushMode有如下这些取值
1. SingleThread,
单线程持久化。
2. MultiThread,
多线程持久化。
3. SingleThreadMerge,
单线程合并持久化。合并表示持久化时会合并多个事务为一个后端数据库事务，一
般能提高效率。
4. MultiThreadMerge
多线程合并持久化。合并表示持久化时会合并多个事务为一个后端数据库事务，一
般能提高效率。
CheckpointModeTableFlushConcurrent 多线程持久化时的线程数量。
checkpointModeTableFlushSetCount时合并时把几个zeze事务合并成一个后端数据库事务。

## 其他全局配置
```
<zeze … Name=”” …></zeze>下面的配置都配在这个位置。
```
* Name = ""

配置的名字。Zeze内部没有使用这个配置，仅仅作为可能的扩展定义在这里。

* ScheduledThreads

Zeze.Util.Task使用后面的计算公式得到threadPoolScheduled 的线程池工作线程
数量：Math.max(ScheduledThreads, Runtime.getRuntime().availableProcessors())。
如果Task初始化是没有传入Application，则使用数量8.

* WorkerThreads

Zeze.Util.Task使用后面的计算公式得到threadPoolDefault的线程池工作线程数量：
Math.max(WorkerThreads, Runtime.getRuntime().availableProcessors() * 30)。如果
Task初始化是没有传入Application，则使用数量240.

* CompletionPortThreads

Java版没有用这个配置，c#版用来设置完成端口（异步网络）的工作线程数量。

* ProcessReturnErrorLogLevel = Level.INFO

存储过程和Task任务处理的默认日志动作中使用这个配置记录当处理返回值是错
误码时的日志级别。

* NoDatabase = false

Zeze是否启用数据库支持的开关。linkd这样的不需要数据库的应用用这个关闭数
据库的初始化。

* AllowReadWhenRecordNotAccessed = true

允许读取没有在事务内访问过的记录，或者允许事务外读取记录。这个选项现在不
再使用，总是允许了。

* AllowSchemasReuseVariableIdWithSameType = true

是否允许Bean的Variable定义反悔。Bean的Variable的Id是不能重复的，即使
删除Variable，也不能在新的Variable里面使用以前用过的Id。但是如果Variable
删除，以后又原样恢复，此时不认为新的Variable重用了旧的，而是认为是删除的
反悔。这个配置控制反悔都不允许，更加严格。

* FastRedoWhenConflict = false

是否在冲突的时候快速重做。事务在执行过程中访问数据，能检测到部分冲突，这
个选项设为true使得一旦检测到冲突发生，马上触发重做。没有打开这个选项（默
认是这样的），事务会继续执行直到最后锁定并且检测来完成冲突检测。两者的区
别是快速重做没有锁定，可能会浪费更多cpu，最坏的结果是事务一直重试最后失
败。除非发现false模式确实满足不了需求，否则不需要打开这个选项。

* OnlineLogoutDelay = 60_000

在线模块在发现客户端断开连接，但没有主动Logout，此时会进行一段时间的延
迟，才自动执行Logout操作。这里设置它的延时时间。

* DelayRemoveHourStart = 3

延迟删除记录模块执行定时器的设置。设置是每天几点到几点之间。这里时开始时
间。参见DelayRemoveHourEnd。

* DelayRemoveHourEnd = 7

延迟删除记录模块执行定时器的设置。设置是每天几点到几点之间。这里时结束时
间。参见DelayRemoveHourStart。

* DelayRemoveDays = 7

延迟删除记录模块保留数据的时间，默认一周，即只删除一周前的记录。

* ProcedureStatisticsReportPeriod = 60000

存储过程统计信息报告间隔。

* TableStatisticsReportPeriod = 60000

表格统计信息报告间隔。

* OfflineTimerLimit = 200

在线模块中离线定时器最大限制。

* ProviderThreshold = 3000

Arch.Provider忙碌定义。即测试任务延迟了3000ms以后就会把服务器定义为忙碌。
这个会最终报告给linkd，影响linkd的任务派发。

* ProviderOverload = 5000

Arch.Provider熔断定义。即测试任务延迟了5000ms以后就会把服务器定义为熔断。
这个会最终报告给linkd，禁止linkd的派发任务过来。

* Dbh2LocalCommit = true

Dbh2客户端使用的提交模式。Dbh2LocalCommit为true表示使用嵌入到本地进程
的提交模式，false表示使用进程外独立的提交服务。

* HotWorkingDir = ""

启用了模块热更，配置程序的工作目录。默认是当前目录。

* HotDistributeDir = "distributes"

启用了模块热更，配置发布目录。热更系统会自动从这个目录装载升级新发布的模
块。

## DatabaseConf
### 必要配置
下面三个DatabaseConf参数必须配置。
* Name = ""
数据库的名字。为空的表示默认数据库。当配置多个数据库时，其他数据库必须命名，并通
过TableConf.DatabaseName指名表所属的配置。
* DatabaseType = “Memory”
数据库的类型。支持Memory，MySql，SqlServer，Tikv，RocksDB，DynamoDB，Dbh2。
1.	Memory
内存数据库，一般用于单元测试，这样重启的时候不用清除数据库。
2.	MySql
MySql以及兼容MySql的数据库。持久化的。
3.	SqlServer
SqlServer，持久化的。
4.	Tikv
Tikv，持久化的。
5.	RocksDB
RocksDB，内嵌的，持久化的。仅用于单机模式，不能用于分布式。
6.	DynamoDB
DynamoDB，持久化的。
7.	Dbh2
Dbh2，持久化的。
* DatabaseUrl = ""
根据上面的DatabaseType配置数据库的参数。每种数据库详细url配置请参考相应的文档，
下面给出一些例子。
```
1.	Memory
<…DatabaseUrl=""/>
2.	MySql
<…DatabaseUrl=”jdbc:mysql://localhost:3306/devtest?user=dev&amp;password=devte
st12345&amp;useSSL=false&amp;serverTimezone=UTC&amp;allowPublicKeyRetrieval
=true”/>
3.	SqlServer
<…DatabaseUrl=”Server=(localdb)\MSSQLLocalDB;Integrated Security=true”/>
4.	Tikv
<…DatabaseUrl=” 172.21.15.68:2379”/>
5.	RocksDB
<…DatabaseUrl=”RocksDBDir”/>
6.	DynamoDB
未完成连接实验，TODO
7.	Dbh2
<…DatabaseUrl=”dbh2://127.0.0.1:10999/dbh2_unittest”/>
```

### Jdbc连接池配置
Zeze的jdbc连接池使用阿里的Druid，目前支持mysql, sqlserver，实际上所有的支持jdbc
的数据应该都可以使用这个配置。这个配置值没有指定的时候默认在程序内部都为null，
zeze将使用Druid的默认配置。另外这里的部分配置在DatabaseUrl里面也可以配置。关于
Druid的配置请上网翻阅指南。

* DriverClassName

Jdbc Driver Class Name，没有指定则自动搜索。

* InitialSize

配置初始化连接池大小。

* MaxActive

连接池最大活跃数量。

* MaxIdle

连接最大空闲时间。

* MinIdle

连接最小空闲时间。

* MaxWait

配置获取连接等待超时的时间。

* PhyMaxUseCount

配置一个连接最大使用次数，避免长时间使用相同连接造成服务器端负载不均衡。

* PhyTimeoutMillis

物理连接超时。当前连接的时长如果超过这个配置则丢弃连接。

* MaxOpenPreparedStatements

限制最大的打开的PreparedStatement数量。

* UserName

用户名。没有指定则从DatabaseUrl中获取。

* Password

密码。没有指定则从DatabaseUrl中获取。

### DynamoConf
仅dynamodb使用。
TODO。

### DistTxn = false
是否启用分布式事务，仅用于TiKV。

### DisableOperates = false
这里的数据库操作是几个用来支持系统检测存储过程，由于兼容性等原因，可能需要关闭这
些操作。通过这个配置可以开关数据库操作。

### PrepareMaxTime = 10_000
当存在多个后端数据库时，zeze使用接近两段式方式持久化事务。这个配置用来指定最大
的准备时间（第一阶段）。超时的化，全部回滚，不提交。这个特性目前没有实现，先保留
在这里。

## TableConf
### 表的默认配置
```
<TableConf CacheCapacity="20000"/>
```
不提供Name，即Name为空，此时表示表的默认配置。当表没有提供特别的配置时，都使
用这里定义的配置。

### 表的重要配置

* CacheCapacity

表配置中最重要的是CacheCapacity，影响命中率和内存的使用，需要一定的规划。对于在
线式服务，一般建议把CacheCapacity配置为规划的在线量。比如某个游戏服务器计划每台
在线玩家2万，那就把它配为2万。这可以确保所有的在线玩家相关的数据都能命中。但
我们知道命中率可以在配置低于在线量时，也能达到很高。比如1万可能就能提供足够的命
中率。这个和应用实际业务执行情况有关，为了简单起见，直接定义为在线量是个好主意。
当某个表的数据很大，2w的配置可能导致占用太大的内存，此时使用指定表名的配置，单
独把它的CacheCapacity设置小一点。如&lt;TableConf Name="demo_Module1_tBigBean" 
CacheCapacity="1000"/&gt;，当然配置需要满足性能（命中率），否则只有提高机器配置了。
这样的配置建议在实际项目比较完整的时候再来调整，不要一开始就去规划。下面的一个配
置实际上会放大CacheCapacity，使得大数据对象问题缓解，一般来说不需要特别关注了。

### 表的其他配置

除非必要，不建议调整下面的配置。配置例子
```
<TableConf Name="demo_Module1_tSample" 
CacheCapacity="1000" CacheInitialCapacity =”0” CacheNewAccessHotThreshold=”0”…/>
```
* CacheInitialCapacity = 0

这是TableCache相关的一个配置，用来配置缓存的初始化容量。Zeze这样使用这个配置：
Math.max(CacheInitialCapacity, 31);修改这个配置到预计的容量，可以迅速进入内存稳定期，
避免启动后多次内存分配。这个参数最终是java.ConcurrentHashMap在使用，请参考相关
文档。一般来说这个配置是个素数。

* CacheNewAccessHotThreshold = 0

这是TableCache相关的一个配置，用来配置缓存的热点集合的阈值。访问很少的时候不创
建新的热点。这个选项没什么意思，一般保持0即可。

* CacheFactor = 5.0

这是TableCache相关的一个配置，用来计算实际缓存容量。实际缓存容量为： CacheCapacity 
CacheFactor。这里使用加倍因子，是由于TableCache使用了SoftRefenence，会在内存紧
张的时候，把数据对象持久化到硬盘上，使得实际的内存占用减少很多，可以缓存的对象数
量能得到大大的提升。由于SoftRefenence的效果，实际上上面提到的CacheCapacity在大
数据对象的时候需要调低需求大大降低，使得配置变得简单。一般来说，TableConf只需要
配置默认的，不再需要单独为每个表调整，大大简化运维管理。

* CacheCleanPeriod = 10000

这是TableCache相关的一个配置，用来配置缓存清理的定时器的间隔。

* CacheNewLruHotPeriod = 10000

这是TableCache相关的一个配置，用来配置Lru热点集合创建的间隔。增加这个配置可以
减少内存变动，但清理速度降低。

* CacheMaxLruInitialCapacity = 100000

这是TableCache相关的一个配置，用来限制缓存初始化容量，防止出现太大值，浪费内存。
这个配置算是一个保护限制。

* CacheCleanPeriodWhenExceedCapacity = 1000

这是TableCache相关的一个配置，用来配置当缓存达到最大容量时清理的速度。实际上当
缓存达到最大容量时，会在循环中使用这个配置sleep，然后马上继续清理，加快清理速度。
为了并发测试目的，可以把它配成0，但实际使用最好大于0。这个策略最大的作用是防止
应用把数据装进缓存的速度太高，避免清理来不及，导致内存溢出。

* CheckpointWhenCommit = false

这是TableCache相关的一个配置，用来控制持久化的时机。当设置为true时，这张表相关
联的所有修改马上持久化到后端数据库。比如游戏系统中，存储账户元宝的表，它的修改事
务通常来源于其他第三方的支付系统，当事务执行完毕，告诉第三方系统结果前，最好结果
是已经持久化的。否则由于zeze默认采用定时持久化策略，如果持久化前断电或进程异常
退出，会导致事务结果丢失。虽然对于zeze来说，事务结果丢失没有破坏完整性，但对于
第三方系统，丢失肯定会造成用户充值丢失，最终只能通过日志恢复数据，而且肯定麻烦客
服了。这个配置属于数据产品策略，但为了简化zeze的配置，放在了运维配置里面。这点
需要注意。

* DatabaseName = ""
这是TableCache相关的一个配置，用来指定表属于哪一个后端数据库。Zeze支持多个后端
数据库，表只能属于一个数据库。名字为空的数据库是默认数据库，所有的表默认都属于默
认数据库。这个配置严格来说是开发策略，但运维继续把数据库进行划分，把表配到不同的
数据库中，好像也说得过去，但，个人不建议运维修改这个配置。这个配置更多信息参考上
面的DatabaseConf。

* DatabaseOldName = ""

这是TableCache相关的一个配置，用来指定表原来属于哪个数据库。这个功能实际上不是
提供动态切换数据库的能力，而是倒数据的能力。具体需求是，由于游戏不停的合服，导致
数据库表的很庞大，它的全备份时间也变得过大，需要极大的io，最终影响到了游戏的运行。
而一个游戏服内的活跃玩家占比是很小的。所以提供了这个功能：把某一个备份配置为旧数
据库(DatabaseOldName)，所有的表还是新的Database，但是新数据库为空，zeze会在新
库中没有数据时，自动去旧数据库读取，并导入新数据库，这样新数据库只有活跃玩家的数
据，备份什么就很容易，整体性能也很高。当后端数据库的备份没有这个问题时，不需要使
用这个功能。总的来说，除非必要，不要使用这个功能。启用这个功能还需要打开
DatabaseOldMode配置，见下。

* DatabaseOldMode

这是TableCache相关的一个配置，用来启用倒库功能。详见DatabaseOldName配置的说
明。

## ServiceConf
网络服务配置。
### 默认网络配置
用来配置默认网络选项，Name为空字符串。当其他某个名字的ServiceConf没有配置时，
就会使用默认网络选项。由于ServiceConf一般需要配置自己的Connector,Acceptor，所以
几乎总是有自己名字的配置选项。这个默认选项的用途就不大了，但是也有点用。当程序动
态构造Service，并且动态构建Connector，Acceptor时，就会用到默认选项，也可以具有
一定的手工调整配置的能力。
```
<ServiceConf Name=" "
	NoDelay=”true”
	SendBuffer=”1M”
	…
/>
```

### 有名字的网络服务配置例子
```
<ServiceConf Name="Zeze.Services.ServiceManager.Agent"
	NoDelay=”true”
	SendBuffer=”1M”
	…
>
		<Connector HostNameOrAddress="127.0.0.1" Port="5001"/>
</ServiceConf>
```
跟默认网络配置相比，就是指定了Name，上面的例子是配置ServiceManager客户端的网
络。其中Connector配置了客户端需要连接的服务器ServiceManager的地址和端口。

### Connector
Connector是ServiceConf的内部节点，用来配置客户端的一个连接。每个Connector只维
护管理一个连接，根据选项会自动在连接断开的时候决定是否重连。下面IsAutoReconnect，
MaxReconnectDelay的配置都是默认值，一般可以不修改。Connector创建的连接的网络选
项使用它所在ServiceConf里面的网络配置。
```
<ServiceConf Name="Zeze.Services.ServiceManager.Agent"
	NoDelay=”true”
	SendBuffer=”1M”
	…
<Connector HostNameOrAddress="127.0.0.1" Port="5001"
	IsAutoReconnect=”true”
	MaxReconnectDelay=“8000”
/>
</ServiceConf>
```

### Acceptor
Acceptor是ServiceConf的内部节点，用来配置一个ServerSocket，它会监听并接受新连接。
每个Acceptor维护一个ServerSocket。Acceptor接受的连接的网络选项使用它所在的
ServiceConf里面的网络配置。注意：由于java版没有实现异步Dns查找，Acceptor.Ip真的
只能是Ip，不能是HostName。为了简化配置，Ip可以设置为@internal或@external。在双
网卡，其中一个网卡配置了公共互联网地址，一个网卡配置了私有网络地址。@internal会
自动查找私有网络地址并设置进去，@external会自动使用公共互联网地址。
```
<ServiceConf Name="TestServer" CompressC2s="1">
	<Acceptor Ip="127.0.0.1" Port="7777"/>
</ServiceConf>
```

### maxConnections
```
<ServiceConf Name="TestServer" CompressC2s="1"
　　maxConnections=”1024”>
	<Acceptor Ip="127.0.0.1" Port="7777"/>
</ServiceConf>
```
每个Service最大连接限制，默认值是1024，一般情况下都适合。但是对于linkd等网关性
质服务，一般需要改的更大。注意：由于历史原因，这个选项的名字是小写开头的。

### SocketOptions
```
<ServiceConf Name="Zeze.Services.ServiceManager.Agent"
	NoDelay=”true” 这就是SocketOptions
	SendBuffer=”1M” 这就是SocketOptions
	…
```
所有的SocketOptions都有默认值，当没有设置时，有一些会使用操作系统的网络默认配置。
#### 操作系统网络相关选项
* NoDelay
* SendBuffer // 不指定的话由操作系统提供默认值
* ReceiveBuffer; // 不指定的话由操作系统提供默认值
* Backlog = 128; // 只有 ServerSocket 使用
这些选项是操作系统提供的，一般不需要修改。让操作系统决定，即操作系统管理员配
置即可。
#### Zeze网络相关选项
* InputBufferMaxProtocolSize = “2M”

最大协议包的大小。协议需要完整收到才解析和处理，所以需要缓存。这是个安
全选项。防止出现攻击占用大量内存。

* OutputBufferMaxSize = “2M”

最大发送协议堆积大小. 用于Service.checkOverflow

* TimeThrottle

选择请求包以及流量限速实现。现有两个实现：queue,counter。Queue实现按时
戳把短时间内的包以及大小保存在一个短的队列中，跟随时间检测流量，精度更
高，但会占用更多内存。Counter实现更简单，但精度差一些。这个实现的功能是
限制在一个短的时间（几秒）内允许通过多少个包，允许通过多少流量。没有配
置时关闭限速检测。

* TimeThrottleSeconds

限速的时间范围，一般几秒。没有配置时关闭限速检测。默认关闭。

* TimeThrottleLimit

限速的协议数量。没有配置时关闭限速检测。默认关闭。

* TimeThrottleBandwidth

限速的带宽配置。没有配置时关闭限速检测。默认关闭。

* OverBandwidth

连接带宽限速，超过时默认实现是丢弃协议。这个带宽限速应用可以重载，实现
自己的策略。这里仅提供统计和参数配置。没有配置这个选项，表示关闭限速检
查。默认discard实现是在带宽占用比率超过OverBandwidthFusingRate时丢协议，
低于OverBandwidthNormalRate继续处理，处于两者之间，回调一个应用可以自
定义的callback继续判断，没有自定义回调，丢弃。

* OverBandwidthFusingRate = 1.0

带宽占用比率大于这个值，接近满载，正常实现discard应该要丢协议了。

* OverBandwidthNormalRate = 0.7

带宽占用比率低于这个值，完全不丢弃

* CloseWhenMissHandle = false

协议没人处理时，是否关闭连接。

### HandshakeOptions
* DhGroups

Dh密钥交换算法里面Group的配置。强烈不建议修改。

* SecureIp

当服务器（Acceptor）运行在防火墙后面是，把客户端看到的外部Ip配置到这个
变量里面。

* CompressS2c=”0”

服务器到客户端是否启用压缩。0表示不压缩。1表示使用mppc算法压缩。2表
示使用zstd算法压缩。

* CompressC2s=”0”

客户端到服务器是否启用压缩。取值0，1，2，含义见上。

* EncryptType=”0”

加密类型选择。0表示不加密。1表示Aes加密。

* KeepCheckPeriod=”0”

检查所有socket是否有发送或接收超时的检查周期(秒). 0表示禁用。

* KeepRecvTimeout=”0”

检查距上次接收的超时时间(秒). 0表示禁用。

* KeepSendTimeout=”0”

检查距上次发送的超时时间(秒). 0表示禁用, 只有主动连接方会使用。

## GlobalCacheManagersConf
为了避免切换raft版时重新配置的麻烦，即使没有使用，这个配置也可以保留在配置文件中。
通过GlobalCacheManagerHostNameOrAddress进行切换。
```
<zeze … 
　　GlobalCacheManagerHostNameOrAddress="GlobalCacheManagersConf"
    GlobalCacheManagerPort="5002">
	<GlobalCacheManagersConf>
		<host name="global.raft.xml"/>
	</GlobalCacheManagersConf>
</zeze>
```
由于历史原因GlobalCacheManagerHostNameOrAddress的格式比较丰富。当只有一台
Global服务器时，这里配置Global服务器的主机地址，GlobalCacheManagerPort配置它的
端口。当存在多台Global服务器时，
GlobalCacheManagerHostNameOrAddress=”ip1:port1;ip2:port2;ip3:port3”。当使用raft版本
的Global服务器时，这里配置“GlobalCacheManagersConf”。而GlobalCacheManagersConf
配置是个xml子节点，其中&lt;host name=”global.raft.xml”&gt;指定一台raft版Global的raft配
置文件，有多台时，配置多个host子节点。

## ServiceManagerConf
为了避免切换raft版时重新配置的麻烦，即使没有使用，这个配置也可以保留在配置文件中。
通过Zeze.ServiceManager配置进行切换。如下：
```
<zeze … ServiceManager="raft">
	<ServiceManagerConf raftXml="servicemanager.raft.xml"/>
</zeze>
```
上面ServiceManager配置默认为空，表示启用单点版本的ServiceManager服务器，这个服
务器的ServiceConf的名字是Zeze.Services.ServiceManager.Agent。取值”raft”表示启用
ServiceManagerConf，其中的raftXml是raft的配置文件名。

## porlardb-x2.0 Database URL额外参数
seUnicode=true&characterEncoding=utf8
