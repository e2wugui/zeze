# 目标

	0) 尽可能减少实现业务逻辑时需要的技术手段。降低应用开发难度。降低成本。
	   让实现代码离业务逻辑尽可能近，几乎能对照起来。
	1) 严格保护数据不损坏。
	2) 解决扩容问题，可以很容易构建成千上万台机器集群的服务。
	3) 解决高可用性问题，达到7x24小时不间断工作。
	4) 简单直接的编程接口，直接融入编程语言。

# 主要特性

	0) 事务
	   提供一般意义上的数据库的事务。
	   如果操作失败，会把放弃所有修改，把数据恢复到修改前的状态。
	   a) 异常安全
	      假设代码中任何地方都可能抛出异常，那么在没有事务支持的环境中
	      写出正确的代码是非常辛苦的。而现在的语言多数都是会抛出异常。
	   b) 更容易模块化
	      跨模块调用前可以先修改数据，调用后发现逻辑条件不符合，可以回滚。
	      更容易按需求定义模块接口，更容易实现代码尽量接近业务描述。

	1) 基于缓存同步的分布式事务
	   缓存同步跟 CPU 的 MESI（缓存一致性协议）类似。
	   这个功能由底层实现，应用除了配置不需要访问任何接口。
	   这是集群扩容的基础。编程的时候，就跟自己独占数据一样。
	   以后增加运行实例即可达到扩容目的。

	2) 简单网络库
	   真的非常简单，稍作封装后，对一般逻辑开发完全隐藏网络细节。
	   a) Protocol
	   b) Rpc

	3) 数据库封装
	   自动持久化到后端数据库。对应用来说，可以当作数据库不存在。
	   支持的数据库（以后根据需要添加，很容易）。
	   a) SQLServer
	   b) Mysql
	   c) Tikv

	4) Raft
	   用来支持高可靠性。

	5) 很容易就能访问所有数据
	   拥有所有数据，不会碰到到处要数据的问题。
	   实现逻辑很清晰。

	6) 乐观锁
	   不会死锁。重试会浪费cpu。但一般cpu是富裕的。

#### 实现导读

	1) 事务和乐观锁
	   存储过程执行过程中不加锁，所有修改仅当前事务可见。
	   提交的时候对所有访问的记录排序并且加锁并进行冲突检查。
	   核心算法：Zeze/Transaction/Transaction.cs -> _lock_and_check_()
	   【并发正确性】
	   事务内所有访问（读写）的记录在冲突检查时需要确保Timestamp没有变化。
	   事务成功，相当于独占所有访问的记录，这个并发策略是严格，但显然是正确的。
	   暂时不考虑实现其他并发级别。

	2) 缓存同步
	   参考了CPU缓存同步算法（MESI），使用了其中3个状态：Modify,Share,Invalid。
	   当主逻辑服务器需要访问或修改数据时，向全局权限分配服务器（GlobalCacheManager）申请M或S权限。
	   GlobalCacheManager知道所有记录的权限的分布状态。它根据申请的权限，向现拥有者发送降级请求，
	   然后给申请者返回合适结果。
	   核心算法：
	   Zeze/Services/GlobalCacheManager.cs -> AcquireModify, AcquireShare, Release
	   Zeze/Transaction/Table.cs -> ReduceShare, ReduceInvalid, FindInCacheOrStorage
	   当主逻辑服务器收到降级请求时，会把相关记录保存到后端数据库以后才给GlobalCacheManager返回结果。see 下面的持久化模式。
	   【当前实现规则】
	   a) GlobalCacheManager在多个记录上并发执行Acquire操作。对单个记录，所有的申请排队，一个一个处理。
	   b) GlobalCacheManager处理Acquire时，除了死锁检测会马上返回失败，正常情况下会返回成功。
	      如果逻辑服务器没有响应Reduce请求（超时），此时实际发生的情况没法预测，Acquire会失败。
	      see zeze\GlobalCacheManager\Cleanup.txt
	   c) 允许GlobalCacheManager认为记录权限已经分配个某个逻辑服务器，但逻辑服务器实际上没有（比如逻辑服务器不正常重启了）或拥有较低权限。
	      所以，逻辑服务器处理Reduce，必须能正确处理Recude的目标状态和自己实际状态，并且返回成功。
	   d) 逻辑服务器在多个记录上并发处理权限申请；对单个记录，所有的Acquire排队。对同一个记录，不会同时发送Acquire给GlobalCacheManager。

	3) 持久化模式
	   Period 定时保存修改到后端数据库，如果保存前进程异常退出，修改会丢失，相当于上一次保存以来的所有事务回滚，数据不会被破坏。
           Immediately 事务提交的时候马上保存到后端数据库。
           Table 可以选择部份表，当事务包含这些表时会马上保存，否则定时保存。这个模式适用范围比较广。
	   核心算法：
	   Zeze/Transaction/Checkpoint.cs
	   Zeze/Transaction/RelativeRecordSet.cs

	4) TableCache.Cleanup

#### 安装教程

	0) Zeze 是一个类库，所有的核心功能都在这里。
	   a) 服务器开发建议直接把Zeze\Zeze.csproj加到自己的sln种。
	      直接引用源码项目。
	   b) 客户端（unity），建议直接拷贝Zeze下的源代码进项目；
	      也可以自己编译相应平台的版本，发布到 unity plugins 下。

	1) Gen
	   是一个控制台程序。编译好以后，用来生成代码。

	2) GlobalCacheManager
	   是一个控制台程序。当多个Application共享数据库时，用来管理缓存同步，

	3) ServiceManager
	   管理服务注册和订阅。
	   让服务器集群更加容易。

#### 使用说明

	【不要捕捉异常】
	如果你需要处理错误，捕捉处理完以后，再次抛出。

	0) 定义数据结构(Bean)
	   在xml中定义，使用Gen生成代码。
	   a) 支持基本类型和容器。
	      打开文件'zeze\UnitTest\solution.xml' 查找 '<bean name="Value">'。
	      这个的例子包含了所有的基本类型和几个容器实例。
	   b) 支持数据结构变更。
	      可以随时增删变量，而不需要数据库转换操作。
	   c) Bean.dynamic
	      用来实现多态类的数据保存。

	1) 定义协议(protocol or rpc)
	   打开文件'zeze\UnitTest\solution.xml' 查找 'protocol' or 'rpc'。

	2) 定义数据库表格(table)
	   打开文件'zeze\UnitTest\solution.xml' 查找 'table'。

	3) 定义模块(module)
	   模块是用来包含Bean,Protocol,Rpc,Table的。
	   打开文件'zeze\UnitTest\solution.xml' 查找 'module'。

	4) 定义项目(project)
	   一个项目对应一个进程。在这里定义service。
	   服务器项目目前仅支持c#。
	   客户端项目目前支持c#，c++；
	   支持的脚本ts，lua。
	   打开文件'zeze\UnitTest\solution.xml' 查找 'project'。

	5) 数据变更订阅
	   有限支持数据变更订阅（ChangeListener）。
	   不管修改数据的操作有多少个，当数据发生变更时，回调Listener。
	   那么不管有多少个地方（方式）修改该数据，只需要一个地方处理数据变更。
	   一般用于客户端数据同步。
	   【注意】除了数据同步，不建议在业务逻辑实现中使用这个特性。

	6) 支持后端多数据库
	   用来避免后端数据库性能不足。
	   由于Zeze的性能来源主要来自本地cache的命中，一般不会发生后端数据库性能不足。

	7) 网络连接支持加密压缩。
	   使用 Diffie–Hellman key exchange 交换密匙。

	8) 配置（zeze.xml）
	   参考：Game\zeze.xml; UnitTest\zeze.xml;Game2\zeze.xml
	   一般来说，开始需要提供一个数据库配置，其他都可以用默认的。
	   不提供配置的话，数据库是内存的。

	9) 什么时候创建存储过程（Zeze.NewProcedure）
	   现在框架默认为每个协议创建存储过程，一般来说不再需要自己创建。
	   如果你想要事务部分失败的时候不回滚整个事务，那就需要嵌套事务，
	   此时需要创建自己的存储过程并判断执行结果。
	   int nestProcedureResult = Zeze.NewProcedure(myaction, "myactionname").Call();
	   // check nestProcedureResult

	10) Bean More
	    纯粹的数据对象，里面可以包含Bean，容器。容器里面又可以包含Bean）
	   【reference】
	    所有的 bean 引用不允许重复，不允许有环（TODO Gen的时候检测环）。
	   【null】
	    所有的 bean 引用不会为 null，使用的时候不需要判断，可以简化代码。
	   【Assign & Copy】
	    赋值和拷贝。
	   【Managed】
	    Bean被加入Table或者被加入一个已经Managed状态的Bean的容器中之前是非Managed状态，
	    此时修改Bean不会被记录日志。Managed状态一旦设置，就不会恢复，即使你从Table中
	    或者容器中删除它。当你从Table或者容器中删除后要再次加入进去，需要Copy一次。
	    Managed状态只能被设置一次，参考上面的reference说明。如果你想加入重复的对象，
	    使用 Bean.Copy 方法复制一份。
	   【binary】
	    Zeze.Net.Binary，创建以后不能修改，只能整个替换。
	   【dynamic】
	    Bean的变量可以是动态的，可以在里面保存不同的bean。
	    see Game\solutions.xml：Game.Bag.BItem的定义。
	    dynamic 在声明支持的Bean类型时，可以指定所属Bean的范围内唯一的TypeId。
	    这个Id会被持久化。如果没有指定TypeId，则默认使用 Bean.TypeId。
	    【Bean.TypeId】
	     Bean.TypeId 默认使用Zeze.Transaction.Bean.Hash64(Bean.FullName)生成。
	     冲突时，可以手动指定一个。
	     Bean的FullName发生变化，而又想兼容旧的数据时，需要手动把TypeId设置成旧的hash值。

	11) Protocol.id Rpc.id
	   默认使用Zeze.Transaction.Bean.Hash16(Protocol.FullName)生成。
	   冲突时，需要手动指定一个。


	12) Session & UserState
	    AsyncSocket.UserState
	    连接上下文
	    Protocol.UserState
	    从某个连接收到的所有协议的上下文，默认从连接上下文复制引用。
	    Procedure.UserState
	    当为协议处理创建存储过程执行事务时，默认从协议上下文复制引用。

	    see Game\game.sln, Game2\game2.sln

	13) 日志和统计
	    记录了几乎所有的错误日志。
	    统计了几乎所有各种可能的情况。（可以通过宏完全关闭）。
	    Zeze 记录日志的时候会把 UserState.ToString 也记录进去。
	    应用可以在自己的UserState对象实现类中添加更多上下文信息。
	    比如, Login.Session.SetLastError("detail");
	    这样写的时候只需要返回错误，不用每个地方自己记录日志。

	14) 协议和存储过程处理结果返回值规划建议
	    0  Success
	    <0 Used By Zeze 
	    >0 User Defined. 自定义错误码时可以这样 (Module.Id << 16) | CodeInModule。
	    注意协议存储过程返回值使用同一个定义空间。

	【不要捕捉异常】
	如果你需要处理错误，捕捉处理完以后，再次抛出。

	15) Zeze.Net.Service
	    网络的所有事件都通过这个类回调。
	    通过重载并override需要的方法进行特殊处理。
	    【线程】
	    网络事件直接在io-thread中回调，不要在回调中执行可能阻塞的操作。
	    如果需要请创建新的Task。

	    例子：
	    Zeze.Services.GlobalCacheManger,
	    Zeze.Services.ServiceManager,
	    Game2\linkd\gnet\LinkdService,Game2\linkd\gnet\ProviderService
	    Game2\server\Game\Server,

	16) Zeze.Util.Task
	    这个辅助类提供执行并记录日志和统计的功能。
	    如果需要创建自己的Task，建议使用。

	17) 一些建议
	    a) 定义模块级别的枚举(see Zezex/solutions.xml)。
	    一般用于模块处理构造全局唯一错误码。可以使用辅助函数Zeze.IModule.ReturnCode构造。
	    b) 客户端通讯尽可能使用Rpc。
	    框架在处理返回错误的时候自动发送Rpc的结果（rpc.SendResultCode(rc);）
	    异常的时候也会返回错误码（系统保留的负数的错误码）。
	    所以一般处理流程只需要在正常的时候设置自定义rpc的正常结果参数并调用rpc.SendResult()；
	    错误的时候直接return errorcode即可。基本没有需求需要catch。

	18) 事务提交模式
	    a) CheckpointMode = Period
	    定时批量提交。可以缓存多次修改，一次提交。事务并发只依赖记录锁。
	    b) CheckpointMode = Immediately
	    每事务提交。事务执行完毕返回时，就意味着数据已经提交到后端存储数据库了。
	    重要应用可能需要这种模式。事务并发只依赖记录锁。
	    c) CheckpointMode = Table
	    可以在Table中启用选项 CheckpointWhenCommit="true"
	    当事务访问的记录启用上面的选项（根据所属的Table得到配置）以后，按 Immediately 提交，否则按 Period 提交。
	    事务并发：执行逻辑的时候依赖记录锁，Commit的时候需要锁记录所属的改名记录集合。并发性有一定降低。
	    这个模式比较灵活，适用面更广点。
	    d) CheckpointMode = PeriodNoFlushLock
	    定时批量提交。不需要FlushReadWriteLock。采用Table模式的实现方法，去掉全局FlushReadWriteLock。
	    它相当于配成Table模式，然后所有的Table都配置CheckpointWhenCommit="false"(默认）。 
	    这个模式单独配置没有什么意义，写在这里仅仅为了说明。
	    *)
	    配置
	    zeze.xml::CheckpointMode zeze.xml::CheckpointPeriod
	    性能考量
	    由于zeze的数据存储在后端数据库采用KV存储，一个记录只变了一部分，也需要整体写入，如果每次写入，存在一定浪费（流量，io）。
	    后端数据库支持的写入量。Period模式可以大大降低后端数据库需要支持的写入量。也使得浪费降低。
	    【注意】
	    选择不同模式应该主要根据需求来决定。
	    但是也要注意性能考量。
	    当在一个进程中同时运行多个Zeze.Application，并且运行同时访问多个App的事务时，
	    需要每个Application.Config.CheckpointMode 一致，这个目前没有很有效的检测手段，
	    所以是没有检查的。

#### 更多说明

	【不要捕捉异常】
	如果你需要处理错误，捕捉处理完以后，再次抛出。

	0) AutoKey：自增长key，仅支持 long 类型。
	   游戏经常需要分区，分成不同的服务器，然后又需要把人数降低以后的服务器合并。
	   如果对表格的key没有一定规划，合并的时候就很复杂。
	   提供一个自增长key，一开始就对规划范围内的服务器分配唯一的key，合并表格就不会冲突。
	   这在游戏分服运营，但是又需要合并时有用。
	   对于启用缓存同步，提供全球同服之类的系统，不需要使用这个。

	1) 多数据库支持
	   提供多个 DatabaseConf 配置。多个数据库需要用不同 Name 区分。
	   然后在 TableConf 中使用属性 DatabaseName 把表格分配到某个数据库中。
	   配置参考：UnitTest\zeze.xml

	2) 从老的数据库中装载数据
	   当使用某些嵌入式数据库（比如bdb）时，如果某个数据库文件很大，但是活跃数据可能又不多，
	   每次备份它比较费时。可以考虑把表格移到新的数据库，然后系统在新库中找不到记录时，
	   自动从老库中装载数据。这样，老库是只读的，不用每次备份。
	   TableConf 中使用属性 DatabaseOldName 指明老的数据库，把属性 DatabaseOldMode 设为 1。
	   当需要时，Zeze 就会自动从老库中装载记录。
	   配置参考：UnitTest\zeze.xml

	3) 同一个进程内多个 Zeze.Application 之间的事务（XXX 已经被废弃，不再支持，需要的喊！）
	   一般来说，事务仅仅访问一个 Zeze.Application 的数据库表格。
	   如果需要在多个 Zeze.Application 之间支持事务。应用直接访问不同 App.Module。
	   里面的表格即可完成事务支持。不过由于事务提交(Checkpoint)默认是在一个 Zeze.Application
	   中执行的，为了让事务提交也原子化。需要在App.Start前设置统一Checkpoint。
	   设置代码例子：

	   Zeze.Checkpoint checkpoint = new Zeze.Checkpoint();
	   // 把多个App的数据库加入到Checkpoint中。
	   checkpoint.Add(demo1.App.Zeze.Databases.Values);
	   checkpoint.Add(demo2.App.Zeze.Databases.Values);
	   // 设置App的Checkpoint。
	   demo1.App.Zeze.Checkpoint = checkpoint;
	   demo2.App.Zeze.Checkpoint = checkpoint;
	   // 启动App。必须在启动前设置。
	   demo1.App.Start();
	   demo2.App.Start();

	4) 缓存同步
	   多个 Zeze.Application 实例访问同一个后端数据库
	   一般的模式是后端数据库仅被一个 Zeze.Application 访问。
	   如果需要多个实例访问同一个数据库，需要开启缓存同步功能。
	   1) 启动 GlobalCacheManager
	   2) 配置 zeze.xml 的属性：GlobalCacheManagerHostNameOrAddress="127.0.0.1" GlobalCacheManagerPort="5555"
	      配置参考：UnitTest\zeze.xml
	   *) 注意，不支持多个使用同一个 GlobalCacheManager 同步的Cache的 Zeze.Application 之间的事务。
	      参见上面的第3点。因为 Cache 同步需要同步记录的持有状态，如果此时 Application 使用同一个 Checkpoint，
	      记录同步就需要等待自己，会死锁。
	   *) 由于逻辑服务器和GlobalCacheManager之间的连接非常重要，所以它们应该运行在一个可靠的网络中，
	      一般来说就是运行在一个机房中。
   
	5) 客户端使用Unity(csharp)+TypeScript
	   a) 把 zeze/Zeze 发布到你的项目，直接拷贝代码或者需要自己编译发布二进制。
	   b) 把 zeze/TypeScript/ts/ 下的 zeze.ts 拷贝到你的 typescript 源码目录。
	      依赖 npm install https://github.com/inexorabletash/text-encoding.git
	   c) 把 zeze/Zeze/Services/ToTypeScriptService.cs 文件中 #if USE_PUERTS 宏内的代码拷贝到你的c#源码目录下的
	      ToTypeScriptService.cs 文件中。当然这里可以另起一个文件名。
	      把 typeof(ToTypeScriptService) 加到 puerts 的 Bindings 列表中。
	      然后使用 puerts 的 unity 插件菜单生成代码。
	   d) 定义 solutions.xml 时，ts客户端要处理的协议的 handle 设置为 clientscript.
	      使用 gen 生成协议和框架代码。
	   e) 例子可以看看 https://gitee.com/e2wugui/zeze-unity.git
	      不知道怎么发布依赖，现在测试运行是把encoding.js encoding-indexes.js 拷贝到output下。
	      其中 encoding.js 改名为 text-encoding.js。

	6) 客户端使用Unreal(cxx)+TypeScript
	   a) 把zeze\cxx下的所有代码拷贝到你的源码目录并且加到项目中。除了Lua相关的几个文件。
	   b) 把 zeze/TypeScript/ts/ 下的 zeze.ts 拷贝到你的 typescript 源码目录。
	      依赖 npm install https://github.com/inexorabletash/text-encoding.git
	   c) 安装puerts，并且生成ue.d.ts。
	   d) 定义 solutions.xml 时，ts客户端要处理的协议的 handle 设置为 clientscript.
	      使用 gen 生成协议和框架代码。
	   e) zeze\cxx\ToTypeScriptService.h 里面的宏 ZEZEUNREAL_API 改成你的项目的宏名字。
	   f) 例子 https://gitee.com/e2wugui/ZezeUnreal.git
	      不知道怎么把依赖库(text-encoding)发布到unreal中给puerts用，可以考虑把encoding.js encoding-indexes.js
	      拷贝到Content\JavaScript\下面，其中 encoding.js 改名为 text-encoding.js。

	7) 客户端使用Unity(csharp)+lua
	   a) 需要选择你的Lua-Bind的类库，实现一个ILua实现（参考 Zeze.Service.ToLuaService.cs）。
	   b) 定义 solutions.xml 时，客户端要处理的协议的 handle 设置为 clientscript.
	   c) 使用例子：zeze\UnitTestClient\Program.cs。

	8) 客户端使用Unreal(cxx)+lua
	   a) 依赖lualib, 需要设置includepath
	   b) 直接把cxx下的所有代码加到项目中。除了ToTypeScript相关的。
	   c) 定义 solutions.xml 时，客户端要处理的协议的 handle 设置为 clientscript.
	   d) 使用例子：zeze\UnitTestClientCxx\main.cpp

	9) 缓存同步 More
	   【Zeze 的性能来自缓存的命中率。】
	   当启用缓存同步以后，如果由于不同服务实例之间不停争抢数据，可能会造成命中率下降。
	   这时候需要做一定的规划。Zezex 提供了 ModuleRedirect 和 Transmit 支持用于提高命中率。
	   详细请阅读下面的文档或者代码。
	   a) Zezex\linkd.provider.txt
	   b) Zezex\README.md 
	   c) Zezex\server\Game\Login\Onlines.cs

	【不要捕捉异常】
	如果你需要处理错误，捕捉处理完以后，再次抛出。

#### 事务的划分

	0. 这是并发编程里面最根本也最重要的问题。

	1. 最基本的划分规则应该根据需求来决定操作是否放在一个事务中。

	2. 一般框架中有Event的模式，此时要注意Event的执行是否需嵌套在触发的事务中执行。
	   建议是除非这是需求决定的（参见上一条），应该启动Event派发放到另外的事务中。
	   这里有两种派发选择，全部派发一个事务或者每一次派发一个事务，也可能派发在事务外执行。
	   例子：ChangeListener的使用。Zeze.Util.EventDispatcher

#### 历史

	写程序一开始，我就对检查状态并修改数据感到很困惑。特别是程序复杂分模块以后，
	此时检查所有的状态，最后修改数据，就需要每个模块状态检查代码提取出来提前一起
	判断。所以一直希望能有个事务环境，在碰到状态不正确时，回滚所有的修改，把数据
	恢复到开始的时候。2007年的时候，开始做游戏，就用java写了个xdb，在程序中支持事务。
	这个版本在需要访问数据时，马上加锁。由于访问数据的顺序跟逻辑相关，就有可能死锁。
	当时的解决方法是使用java的死锁检测，发现死锁就打断重做。或者程序员在一个事务
	开始的时候把所有需要访问的数据的锁都提前（这是可以排序）锁上。
	死锁就成为xdb最大的问题，也是xdb不大好用的地方。
	2013年的时候，当时的同事 pirunxi 提出了乐观锁：所有的数据修改先仅在本事务中可见，
	执行完了以后（此时可以知道所有的数据，就可以排序加锁，就不会死锁了），
	加锁和判断数据状态，不冲突的话，事务成功，冲突的话保持已有的锁重做。
	这个方案解决了死锁问题，系统易用性大大提升。
	在2014-2017年间，pirunxi 实现了好多个基于乐观锁的版本。
	我大概是2015年开始参与讨论。
	今年（2020）新冠疫情期间，老婆孩子不在身边，我闲着没事。
	有一次就问了 pirunxi 最新版本的情况。
	然后（闲着没事）就写了 Zeze 这个版本，
	这是我的第一个 c# 程序。
	正如当时xdb是我的第一个java程序。

#### 联系

	微信群: Zeze交流群
	需要找群成员邀请？

#### 性能

	0. Cache命中率
	   Zeze 直接使用本进程内的Cache。在Cache命中的情况下，没有任何远程访问。此时性能可以达到最高。
	   Zeze 的性能核心就是Cache命中率。

	1. 记录大小
	   Zeze 使用后台 key-value 数据库保存数据。记录读取和写入是作为整体保存到后台数据库的。
	   如果记录太大，只修改里面的少量数据，也需要整个记录一起保存到后台。这里有一定的系列化开销。
	   需要分析需求选择合适的记录大小。一般来说应用得到需求都是按模块给的，开始的时候数据按模块划分即可。
	   模块太大时最好分成子模块，或者模块内分成多个小一点的记录。

	2. 记录大小Ex
	   记录可以包含容器，一般需要设定合适上限。如果数据需要很大，那么应用可能需要自己在key-value记录的
	   基础上实现list（多个记录来保存数据），然后自己实现遍历，增删等操作。

	3. 缓存同步和分布式事务
	   这个是全球同服的基础。
	   当需要根据用户量增长不停增加服务器时，可能都有个疑问：吞吐量能提高吗？
	   如果全部的请求都要求互斥的访问同一个数据，那么系统吞吐量怎么弄都是是上不去的。
	   我相信这个世界是天然并发的。
	   一般来说用户请求都访问自己的数据（局部数据）。多个请求是可以并发的。

	4. 全局模块的并发
	   全球同服的系统里面有些模块可能是全局的。这些请求都访问同一个数据，肯定是互斥申请锁排队执行。
	   最高性能就是单个线程全速运行的事务数，这是有上限的。随着用户增长，请求量可能超过上限。
	   此时需要采取一些方案提高数据的并发度。
	   a) 记录数据很大，可以分成小块。
	   b) 把数据分成多份（如果可以）。比如某个公司账号有大量的并发转账请求，此时可以建多个子账号。
	      转入操作根据转入者Id的Hash选择某个子账号，这样转入就并发了。
	      转出操作也按这个规则找到开始的子账号。由于该子账号可能金额不够，这是按顺序继续扣后面的子账号。
	      此时转出访问了多个记录，这是没问题的。但是多数情况应该只需要访问一个子账号，不够的情况肯定是少的。
	      读取操作可以分别显示子账号，或者统计一下。读取会导致执行转入账号的服务器的Cache降级到Share。
	      读请求很多的时候，可以用一个定时更新的cache减少实际的数据访问量。

	5. 全局模块的Cache命中率和ModuleRedirect（在Zezex中）
	   see Zezex/README.md
	   see Zezex/linkd.provider.txt

	6. 按需行动
	   如果可以预见请求量，并且代价不大时，可以一开始就优化并发性能。
	   否则可以等到请求量大到快无法支撑了再来优化。
	   一开始实现一个支持任意请求量是没有必要的。
	   计算机都是在有限资源有限时间解决问题。

#### 建议

	. 存储Bean的定义和协议Bean的定义最好不要为了重用而特意定义成一份
	  当存储和协议的结构【确实】一样时，允许使用同一个Bean定义。
	  一般情况下，两者最好使用独立的Bean定义。
	  建议协议可以包含存储Bean，但不要反过来使用。

	. 尽量把逻辑服务器设计成无状态的
	  尽量把所有的数据都定义到数据库中，让zeze管理所有数据。
	  即使是仅用于一个逻辑服务器的数据也可以存储到后台数据库。
	  这带来的好处是数据量不会局限于内存容量（因为cache会自动管理），后台数据库可以看成容量几乎无限；
	  热更新服务器等事情也会变得简单；请求派发（通过linkd）也会简单。

	. 定时器：Timeout & Timer
	  定时器本质上是程序执行状态，这个破坏了逻辑服务器的无状态性。
	  在大规模集群的情况下，定时器的实现需要解决两个问题：
	  a) 一个定时器应该只在一个逻辑服务器检测。多个检测实例在最终处理事务时忽略重复是可行，但存在浪费。
	  b) 调度定时器的逻辑服务器死掉以后，怎么发现和重新分配到其他逻辑服务器上。
	  一般建议：
	  a) 定时器数据持久化时建议存储到期（或者下一次时间）的绝对时间，不要存储1小时这种相对时间。
	     这种方式可以利用底层已有的调度注册一次，不用轮询。
	  b) 定时器实际调度时尽量应该和在线用户相关，不要在全部用户上实现。
	     也就是说用户上线时，把他需要的定时器进行调度，此时可以判断绝对时间，把已经超时的处理掉。
	  c) 某些情况下定时器可以交给客户端实现，服务器只校验。比如对于现时使用（不是装备也不会影响buf）的物品，
	     服务器就可以不做实际调度，仅在使用物品的时候检查绝对到期时间。如果用户作弊或者时间不一致，以服务器为准。
	  d) 对于系统级别的定时器，最好都不要轮询。这种定时器看需求了，真的很多，会有相当的负载。实现时尽量注意。
	  需求分类分析：
	  a) 对于用户相关定时器。用户登录时选定一台服务器，所有相关定时器都注册到这台服务器。
	     如果这台服务器关闭，用户需要重新登录并选择新的一台服务器，再次进行相关注册。
	  b) 对于系统界别（比如定时活动）定时器。所有的服务器都同时进行定时器判断，忽略服务器之间的时间差，这个问题不大。
	     考虑到不间断运行，这种定时器如果从配置中读取，最好支持运行期重新加载。可以考虑把这种定时器配置存到后台数据库。
	  c) 其他类别。具体问题具体分析了。
	  综上所述，zeze不准备为定时器提供支持了。好像也没什么可做的了。

	. 请求数据时，尽可能使用Rpc。

	. Procedure Protocol Action Func 调度
	  可以使用Zeze.Util.Task包装一下上面的几种任务。内部会处理错误，记录日志，统计等。可以带来一些便利。
	  Zeze.Util.Task.Run  把任务包装以后加到线程池执行
	  Zeze.Util.Task.Call 包装任务，在当前线程执行。
	  有需要，可以增加新的包装方法。增加的方法有依赖的话，可以在自己的项目中增加包装类。
	
	. Service.DispatchProtocol & Service.DispatchRpcResponse ...
	  所有网络上收到的协议可以重载这两个方法使用自己的策略调度。
	  一般来说，默认的调度策略够用了。

	. Zeze.Applicateion.TaskOneByOneByKey
	  这是个对相同的key只允许一个任务在执行的调度器。如果应用需要对某一类请求排队处理，就可以直接建议使用。
	  比如对同一个用户的请求进行排队。

	. Transaction Api
	  a) GetLog & PutLog
	     自定义日志。
	  b) RunWhileCommit
	     事务结束并且提交后执行。此时还在锁内，可以让操作和事务原子化。
	  c) RunWhileRollback
	     事务结束并且回滚后执行。此时还在锁内，可以让操作和事务原子化。
	  bc) RunWhileCommit RunWhileRollback 提交的Action的限制（会抛异常）
	      不能修改数据；
	      不能再次提交新的Action;
	      不能嵌套执行事务；

	. 事务内创建的 Zeze.Application.NewProcedure 的执行方式
	  var newp = Zeze.Application.NewProcedure(...);
	  a) newp.Call(); 嵌套执行，新建的存储过程可以独立回滚，但是提交是作为一个完整的事务一起提交的。
	  b) Zeze.Util.Task.Run(newp); 在新的线程中执行存储过程。
	
	. Events (######## 事务的划分)
	  a) 如果事件需要和当前事务一起提交回滚: 直接调用实现者的方法。
	     这种模式不建议使用动态订阅的模式，最好就不使用管理类，直接把需要执行的Handle调用写在触发点。
	     这样能很直观的看出来总共有哪些handle。
	  b) 如果事件需要和当前事务一起提交，但是event-handle允许失败：使用嵌套事务方式执行。
	  c) 如果事件和当前事务没有直接关联（或者仅仅传递一下参数）：只用Zeze.Util.Task.Run执行。
	     传递参数的时候注意不能把Table内的Bean的引用（beankey的引用可以）直接传过去。
	     一般来说这种情况是不必要的，因为新的事务可以直接查询，此时只需要传递Table.Key即可。
	  d) events派发的时候一个handle失败是否影响其他handle的派发。
	     一般建议不要扩大影响，也就是说每个handle派发采用同步调用（直接invoke）需要try。
	     Zeze.Util.Task.Run 已经处理了错误，所以就是独立，不影响的。
	  *) 划分应该需求驱动。

	. 事务的划分 Again
	  虽然说事务的划分应该根据需求来定，但很多时候，提需求的人也不一定说得清。
	  服务器都是收到一个请求开始处理数据，这样每个请求的处理就可以看作一个事务。
	  这样就不用费太多脑细胞去考虑划分的问题。但是要下面几个例子要注意。
	  a) 当请求需要对队伍中的多个用户发放奖励时，一般来说一个用户的奖励发放失败不应该影响其他用户。
	     此时就需要分到多个事务，或者使用嵌套事务。
	  b) 当对所有好友（家族成员，帮派成员）进行遍历处理时，也要注意。
	  *) 总的来说就是需要遍历（广播）之类的操作都需要注意一下。
	  *) Events很多时候也是一种遍历(广播)。
	  *) 并发优化：如果请求操作很复杂，访问很多数据，冲突比较严重的时候，需要仔细考虑需求，进行事务划分细化。
	  *) 广播需要一个事务例子：创建角色的时候，需要调用所有需要初始化的模块进行处理，此时不管需要初始多少个模块，都需要在一个事务内。

	. TableCache.Capacity
	  一个事务中操作的同一个表的记录数超过Capacity，这个事务处理时间又超出CacheNewLruHotPeriod时间（超过后就可能会被Clean），
	  这导致事务最后lock_and_check时发现记录已经被Clean，然后重做，最终有可能永远完成不了。
	  所以！注意！
	  这个问题可以在记录内记录一个标记，保证新装载的记录至少用过一次才会被Clean，保证至少完成一次事务。
	  但为了这个问题做这些修改，感觉不值得，就不考虑了。

	. AllowDirtyWhenAllRead SelectCopy SelectDirty
	  AllowDirtyWhenAllRead
	  当事务中所有的操作都是读操作并且事务级别为这个，那么事务将不进行原子性检查，直接成功，不会发生重做。
	  具有很高的并发性。非原子性的例子：事务Writer修改两个变量V1,V2（最简单的，来自同一个Bean的两个变量，包括来自两个记录或者来自两个表）；
	  事务DirtyRead先读取V1，再读取V2，那么读到的V1可能事务Writer修改前的，读到的V2可能是事务Writer修改后的；也就是说两个变量没有原子化。
	  这个级别一般用于用户仅仅查询数据用来显示，并不关心数据之间的原子性，也没有关联两个变量的逻辑判断，可以大大提高并发性。
	  SelectCopy
	  在记录读锁内获得记录的拷贝，如果上面例子的两个V1,V2都在一个记录内，那么原子性得到保证。但是V1,V2在两个记录内（或者两个表），
	  仍然没有原子保证。这个方法可以在事务外使用。Zezex/Game/Login/Onlines 给在线用户发送消息时，可以使用这个方法安全的在事务外执行，
	  因为Online.Status需要的两个变量(LinkName,LinkSid)都在一个记录内，不会发生读到一个修改后的LinkName，而LinkSid又是旧的问题。
	  SelectDirty（还未提供）
	  一般用于事务外，直接返回数据引用，记录锁外直接读取数据。和AllowDirtyWhenAllRead一样，没有原子性保证。
	  * AllowDirtyWhenAllRead SelectDirty
	  使用时，读取record.var以后，再次读取，值可能发生了变化，所以对同一个var，最好仅读取一次。当然也为了效率考虑，
	  一个变量如果后面需要重用，自己先保存一下。AllowDirtyWhenAllRead 现在除了可以在NewProcedure时设置，也配置到了Protocol中，
	  可以后期优化并发时加上配置，先不用关心。【注意：最好保持同一个变量仅读取一次】

	. 缓存同步 & CheckpointMode.Table
	  由于技术原因，启用缓存同步不能使用CheckpointMode.Period。

	. CheckpointMode.Table 的并行优化
	  在这个模式下，多个事务访问的记录当存在交叉时，会被关联到一起进行Checkpoint。关联越分散，并行度越高。
	  按自然的方式划分事务，一般具有足够好的并行度。但还是需要注意某些全局模块访问。
	  比如有个全局统计数据，非常多的事务都需要读写这个数据进行逻辑判断，那么这些事务都会被关联起来，
	  降低Checkpoint并行度。这时候，提高并行度就需要进行额外处理。根据自己的事务划分需求，看看是否能把读写
	  分到另外的事务中执行，本事务根据结果进行处理。
	  Checkpoint并行度属于优化，开发初期可以不用关心。
