# zeze

   zeze 是一个支持事务的应用框架。
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

#### 安装教程

Zeze 是一个类库，所有的核心功能都在这里。
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

. Sample
  see Game\Readme.txt
  Game\game.sln

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

5. Lua（主要是为了客户端）
   1) cs+lua
   参考应用场景：Uniti3d。
   现在使用了KeraLua包装库，可以根据需要增加其他lua库。
   支持部分协议在cs层处理，部分在lua层处理。
   使用例子：zeze\UnitTestClient\Program.cs。

   2) cxx+lua 
   参考应用场景：Unreal以及其他支持cxx的平台。
   使用标准lua-库，需要自己下载并加到项目中，设置好include-path。
   不支持在cxx层处理协议，有需要再增加。
   需要把zeze\cxx目录下的所有文件加到项目中。设置好include-path。
   使用例子：zeze\UnitTestClientCxx\main.cpp

   3) TODO
   现在网络层的Connect调用在原生语言层调用，估计需要增加Lua层创建网络连接的能力。

#### TODO

每事务提交

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
