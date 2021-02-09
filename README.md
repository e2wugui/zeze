# zeze

	zeze 是一个支持事务的应用框架。
	为什么要事务？
	假设代码中任何地方都可能抛出异常，那么在没有事务支持的环境中写出正确的代码是非常辛苦的。
	有了事务支持，异常会导致事务失败，但不会损坏数据。

	让应用的数据访问在事务中执行。
	另外也提供了一个简单的网络框架。
	主要特性
	1) 支持事务，如果操作失败，会把放弃所有修改，把数据恢复到修改前的状态。
	2) 支持自动持久化到后端数据库，目前支持SQLServer，Mysql。
	3) 支持数据结构(Bean)变更，增删变量，而不需要转换旧库。
	4) 支持dynamic，变量类型为多个其他指定的的Bean类型，可以用来包装实现数据（bean）到多态继承类的映射。
	5) 支持后端多数据库，用来避免后端数据库性能不足。由于Zeze的性能来源主要来自本地cache的命中，一般不会发生后端数据库性能不足。
	6) 支持多个逻辑服务器共享一个后端数据库（自动cache同步），可以用来实现全球同服。逻辑服务器性能不足，直接扩充就可以了。
	7) 网络连接支持加密压缩。使用 Diffie–Hellman key exchange 交换密匙。
	8) 客户端网络库目前支持c++，cs，支持协议生成到lua，在lua层实现逻辑。
	9) 有限支持数据变更订阅（ChangeListener）和客户端数据同步。对于map，set类型的变量，可以得到本次事务中的改变，
	   这样仅需要把改变同步给客户端。最重要的是使用了这种方式同步数据，那么不管有多少个地方（方式）修改该数据，只需要一个地方同步数据。

#### 安装教程

	Zeze 是一个类库，所有的核心功能都在这里。
		1 服务器开发建议直接把Zeze\Zeze.csproj加到自己的sln种。直接引用源码项目。
		2 客户端（unity），建议直接拷贝Zeze下的源代码进项目；也可以自己编译相应平台的版本，发布到 unity plugins 下。
	Gen 是一个控制台程序。编译好以后，用来生成代码。
	GlobalCacheManager 是一个控制台程序。当多个Application共享数据库时，用来管理Cache同步，参见后面"特殊模式"的第4点。

#### 使用说明

	. 定义自己解决方案相关内容，包含数据类型(bean)、协议(protocol)、数据库表格(table)等。
	  参考：Game\solution.xml; UnitTest\solution.xml

	. 使用 Gen.exe 生成代码。

	. 在生成的Module类中，实现应用协议，访问数据库表完成逻辑操作。

	. 配置（zeze.xml）
	  参考：Game\zeze.xml; UnitTest\zeze.xml
	  一般来说，开始需要提供一个数据库配置，其他都可以用默认的。
	  不提供配置的话，数据库是内存的。

	. 什么时候创建存储过程（Zeze.NewProcedure）
	  现在框架默认为每个协议创建存储过程，一般来说不再需要自己创建。
	  如果你想要事务部分失败的时候不回滚整个事务，那就需要嵌套事务，此时需要创建自己的存储过程并判断执行结果。
	  int nestProcedureResult = Zeze.NewProcedure(myaction, "myactionname").Call();
	  // check nestProcedureResult

	. Bean（纯粹的数据对象，里面可以包含Bean，容器。容器里面又可以包含Bean）
	  reference：所有的 bean 引用不允许重复，不允许有环（TODO Gen的时候检测环）。
	  null：所有的 bean 引用不会为 null，使用的时候不需要判断，可以简化代码。
	  Assign：Bean 中包含的Bean和容器的引用没有 setter。如果需要整个对象赋值，使用 Bean.Assign 方法。
	  Managed：Bean被加入Table或者被加入一个已经Managed状态的Bean的容器中之前是非Managed状态，
	    此时修改Bean不会被记录日志。Managed状态一旦设置，就不会恢复，即使你从Table中或者容器中删除它。
	    当你从Table或者容器中删除后要再次加入进去，需要Copy一次。
	    Managed状态只能被设置一次，参考上面的reference说明。如果你想加入重复的对象，使用 Bean.Copy 方法复制一份。
	  binary：这个类型的内部实现是byte[]，由于直接引用数组没法进行修改保护，所以目前限制binary不能直接被容器包含，
	    只能定义在Bean中，并且提供特殊的属性和方法进行访问。
	  dynamic: Bean的变量可以是动态的，可以在里面保存不同的bean。这个变量属性可读，有专门的写(setter)方法，名字为variablenameSet，
	    see Game\solutions.xml：Game.Bag.BItem的定义。

	.Bean.TypeId
	  默认使用Zeze.Transaction.Bean.Hash64(Bean.FullName)生成。
	  冲突时，需要指定一个。或者改变Bean.FullName，而又想解析旧的数据时，设置成旧的hash值。

	.Protocol.id Rpc.id
	  默认使用Zeze.Transaction.Bean.Hash16(Protocol.FullName)生成。
	  冲突时，需要指定一个。

	XXX Do Not Catch Exception
	  原则上不要捕捉异常。如果你实在需要，捕捉处理以后，请再次抛出。

	. UserState
	  AsyncSocket.UserState 连接上下文
	  Protocol.UserState 从某个连接收到的所有协议的上下文，默认从连接上下文复制引用。
	  Transaction.RootProcedure.UserState 当为协议处理创建存储过程执行事务时，默认从协议上下文复制引用。
	  这个上下文应该从应用整体考虑来使用，一般来说对于服务器，用来保存用户的登录会话（Login.Session）。
	  see Game\game.sln, Game2\game2.sln

	. 错误日志
	  当协议（或者存储过程）执行抛出异常或者返回值不是Procedure.Success时，Zeze会记录错误日志，并为每一种返回值统计。
	  Zeze 记录日志的时候会把 UserState.ToString 也记录进去。应用可以在自己的UserState对象实现类中添加更多上下文信息。
	  比如, Login.Session.SetLastError("detail");
	  这样写的时候只需要返回错误，不用每个地方自己记录日志。

	. 协议存储过程处理结果返回值规划建议
	  0  Success
	  <0 Used By Zeze 
	  >0 User Defined. 自定义错误码时可以这样 (Module.Id << 16) | CodeInModule。
	  注意协议存储过程返回值使用同一个定义空间。

#### 特殊模式

	0. AutoKey：自增长key，仅支持 long 类型。
	   游戏经常需要分区，分成不同的服务器，然后又需要把人数降低以后的服务器合并。如果对表格的key没有一定规划，合并的时候就很复杂。
	   提供一个自增长key，一开始就对规划范围内的服务器分配唯一的key，合并表格就不会冲突。
	   配置参考：UnitTest\zeze.xml
	   属性 AutoKeyLocalId="0" 本地服务器的Id，所有服务器内唯一，用过的也不能再次使用。
	   属性 AutoKeyLocalStep="4096" 自增长key每次增加步长，也是可以创建的服务器最大数量。
	   规划好上面两个参数，自增长key就会提供区内所有的id唯一。

	1. 多数据库支持
	   提供多个 DatabaseConf 配置。多个数据库需要用不同 Name 区分。
	   然后在 TableConf 中使用属性 DatabaseName 把表格分配到某个数据库中。
	   配置参考：UnitTest\zeze.xml

	2. 从老的数据库中装载数据
	   当使用某些嵌入式数据库（比如bdb）时，如果某个数据库文件很大，但是活跃数据可能又不多，每次备份它比较费时。
	   可以考虑把表格移到新的数据库，然后系统在新库中找不到记录时，自动从老库中装载数据。
	   这样，老库是只读的，不用每次备份。
	   TableConf 中使用属性 DatabaseOldName 指明老的数据库，把属性 DatabaseOldMode 设为 1。当需要时，Zeze 就会自动从老库中装载记录。
	   配置参考：UnitTest\zeze.xml

	3. 多个 Zeze.Application 之间的事务
	   一般来说，事务仅仅访问一个 Zeze.Application 的数据库表格。
	   如果需要在多个 Zeze.Application 之间支持事务。应用直接访问不同 App.Module 里面的表格即可完成事务支持。
	   不过由于事务提交(Checkpoint)默认是在一个 Zeze.Application 中执行的，为了让事务提交也原子化。需要在App.Start前设置统一Checkpoint。
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

	4. Cache同步：多个 Zeze.Application 实例访问同一个后端数据库
	   一般的模式是后端数据库仅被一个 Zeze.Application 访问。如果需要多个App访问一个数据库，需要开启Cache同步功能。
	   1) 启动 GlobalCacheManager
	   2) 配置 zeze.xml 的属性：GlobalCacheManagerHostNameOrAddress="127.0.0.1" GlobalCacheManagerPort="5555"
	      配置参考：UnitTest\zeze.xml
	   *) 注意，不支持多个使用同一个 GlobalCacheManager 同步的Cache的 Zeze.Application 之间的事务。参见上面的第3点。
	      因为 Cache 同步需要同步记录的持有状态，如果此时 Application 使用同一个 Checkpoint，记录同步就需要等待自己，会死锁。
	   *) 由于逻辑服务器和GlobalCacheManager之间的连接非常重要，所以它们应该运行在一个可靠的网络中，一般来说就是运行在一个机房中。
   
	5. 客户端使用Unity(csharp)+TypeScript
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
	      不知道怎么发布依赖，现在测试运行是把encoding.js encoding-indexes.js 拷贝到output下。其中 encoding.js 改名为 text-encoding.js。

	6. 客户端使用Unreal(cxx)+TypeScript
	   a) 把zeze\cxx下的所有代码拷贝到你的源码目录并且加到项目中。除了Lua相关的几个文件。
	   b) 把 zeze/TypeScript/ts/ 下的 zeze.ts 拷贝到你的 typescript 源码目录。
	      依赖 npm install https://github.com/inexorabletash/text-encoding.git
	   c) 安装puerts，并且生成ue.d.ts。
	   d) 定义 solutions.xml 时，ts客户端要处理的协议的 handle 设置为 clientscript.
	      使用 gen 生成协议和框架代码。
	   e) zeze\cxx\ToTypeScriptService.h 里面的宏 ZEZEUNREAL_API 改成你的项目的宏名字。
	   f) 例子 https://gitee.com/e2wugui/ZezeUnreal.git
	      不知道怎么把依赖库(text-encoding)发布到unreal中给puerts用，可以考虑把encoding.js encoding-indexes.js拷贝到Content\JavaScript\下面，
	      其中 encoding.js 改名为 text-encoding.js。

	7. 客户端使用Unity(csharp)+lua
	   a) 需要选择你的Lua-Bind的类库，实现一个ILua实现（参考 Zeze.Service.ToLuaService.cs）。
	   b) 定义 solutions.xml 时，客户端要处理的协议的 handle 设置为 clientscript.
	   c) 使用例子：zeze\UnitTestClient\Program.cs。

	8. 客户端使用Unreal(cxx)+lua
	   a) 依赖lualib, 需要设置includepath
	   b) 直接把cxx下的所有代码加到项目中。除了ToTypeScript相关的。
	   c) 定义 solutions.xml 时，客户端要处理的协议的 handle 设置为 clientscript.
	   d) 使用例子：zeze\UnitTestClientCxx\main.cpp

	9. ChangeListener 和可靠数据同步
	   数据同步。使用这个功能可以只需要监听一次，以后任何修改都会得到通知。
	   问题：
	     GetData 和 ChangeNotify 之间的原子性。
	   解决方案：
	     核心是先保存再确认。
	     在 Online.Data 里面增加一个 Queue，MarkNameSet。
	     GetData 同时设置 MarkNameSet.Add(ListenerName)
	     OnChange: Queue.Add(Notify), if (Online) Send(Notify)
	     Confirm: 推进ConfirmCount。
	     ReLogin: 同步 Queue（同时可能推进ConfirmCount）。
	     采用这个方案也顺便解决了断线重连的问题。仅需同步Queue。

	*. 其他参考
	   "Game/游戏使用方案建议.txt"

#### 事务的划分

	0. 这是并发编程里面最根本也最重要的问题。

	1. 最基本的划分规则应该根据需求来决定操作是否放在一个事务中。

	2. 一般框架中有Event的模式，此时要注意Event的执行是否需嵌套在触发的事务中执行。
	   建议是除非这是需求决定的（参见上一条），应该启动Event派发放到另外的事务中。
	   这里有两种派发选择，全部派发一个事务或者每一次派发一个事务，也可能派发在事务外执行。
	   例子：ChangeListener的使用。

#### 表中的数据跨事务传递的问题

	把一个事务中从表中得到的数据引用传给另一个事务会引起不可预知的问题。
	以前xdb做了检测保护，现在Zeze还没有这个机制。
	当需要传递数据时，可以使用下面两种方式。
	1）使用 Bean.Copy
	2）传递 table.key，等必要的传值参数，新事务重新查表。

#### 历史

	写程序一开始，我就对检查状态并修改数据感到很困惑。特别是程序复杂分模块以后，此时检查所有的状态，最后修改数据，就需要每个模块状态检查代码提取出来提前一起判断。
	所以一直希望能有个事务环境，在碰到状态不正确时，回滚所有的修改，把数据恢复到开始的时候。
	2007年的时候，开始做游戏，就用java写了个xdb，在程序中支持事务。这个版本在需要访问数据时，马上加锁。由于访问数据的顺序跟逻辑相关，就有可能死锁。
	当时的解决方法是使用java的死锁检测，发现死锁就打断重做。或者程序员在一个事务开始的时候把所有需要访问的数据的锁都提前（这是可以排序）锁上。
	死锁就成为xdb最大的问题，也是xdb不大好用的地方。
	2013年的时候，当时的同事 pirunxi 提出了乐观锁：所有的数据修改先仅在本事务中可见，执行完了以后（此时可以知道所有的数据，就可以排序加锁，就不会死锁了），
	加锁和判断数据状态，不冲突的话，事务成功，冲突的话保持已有的锁重做。这个方案解决了死锁问题，系统易用性大大提升。
	在2014-2017年间，pirunxi 实现了好多个基于乐观锁的版本。我大概是2015年开始参与讨论。
	今年（2020）新冠疫情期间，老婆孩子不在身边，我闲着没事。有一次就问了 pirunxi 最新版本的情况。
	然后（闲着没事）就写了 Zeze 这个版本，这是我的第一个 c# 程序。正如当时xdb是我的第一个java程序。

#### 联系

QQ群：118321800
