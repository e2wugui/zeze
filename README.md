# 目标

	0) 尽可能减少实现业务逻辑时需要的技术手段。降低应用开发难度。降低成本。
	   让实现代码离业务逻辑尽可能近，几乎能对照起来。
	1) 尽可能严格保护数据不损坏。
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

	14) 协议存储过程处理结果返回值规划建议
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

#### 更多说明

	【不要捕捉异常】
	如果你需要处理错误，捕捉处理完以后，再次抛出。

	0) AutoKey：自增长key，仅支持 long 类型。
	   游戏经常需要分区，分成不同的服务器，然后又需要把人数降低以后的服务器合并。
	   如果对表格的key没有一定规划，合并的时候就很复杂。
	   提供一个自增长key，一开始就对规划范围内的服务器分配唯一的key，合并表格就不会冲突。
	   配置参考：UnitTest\zeze.xml
	   属性 AutoKeyLocalId="0" 本地服务器的Id，所有服务器内唯一，用过的也不能再次使用。
	   属性 AutoKeyLocalStep="4096" 自增长key每次增加步长，也是可以创建的服务器最大数量。
	   规划好上面两个参数，自增长key就会提供区内唯一的id。
	   【AutoKeyLocalId】
	   当启用缓存同步以后，这个配置也作为每个服务实例的唯一Id。

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

	3) 多个 Zeze.Application 之间的事务
	   一般来说，事务仅仅访问一个 Zeze.Application 的数据库表格。
	   如果需要在多个 Zeze.Application 之间支持事务。应用直接访问不同 App.Module 
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
	   这时候需要做一定的规划。Game2 提供了 ModuleRedirect 和 Transmit 支持用于提高命中率。
	   详细请阅读下面的文档或者代码。
	   a) Game2\linkd.provider.txt
	   b) Game2\README.md 
	   c) Game2\server\Game\Login\Onlines.cs

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

QQ群：118321800
