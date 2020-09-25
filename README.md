# zeze

zeze 是一个支持事务的应用框架。
让应用的数据访问在事务中执行。
另外也提供了一个简单的网络框架。

#### 安装教程

Zeze 是一个类库，所有的核心功能都在这里。我还不知道c#的类库怎么加入应用。
Gen 是一个控制台程序。编译好以后，用来生成代码。
GlobalCacheManager 是一个控制台程序。当多个Application共享数据库时，用来管理Cache同步，参见后面"特殊模式"的第4点。

#### 使用说明

1. 定义自己解决方案相关内容，包含数据类型、协议、数据库表格等。
   参考：UnitTest\solution.xml
2. 使用 Gen 生成代码。
3. 在生成的Module类中，实现应用协议，使用数据库表格访问数据等。
4. 配置。
   参考：UnitTest\zeze.xml
   一般来说，开始的话，需要提供一个数据库配置。不提供配置的话，数据库是内存的。

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

4. Cache同步：多个 Zeze.Application 访问同一个后端数据库
   一般的模式是后端数据库仅被一个 Zeze.Application 访问。如果需要多个App访问一个数据库，需要开启Cache同步功能。
   1) 启动 GlobalCacheManager
   2) 配置 zeze.xml 的属性：GlobalCacheManagerHostNameOrAddress="127.0.0.1" GlobalCacheManagerPort="5555"
      配置参考：UnitTest\zeze.xml
   *) 注意，不支持多个使用同一个 GlobalCacheManager 同步的Cache的 Zeze.Application 之间的事务。参见上面的第3点。
      因为 Cache 同步需要同步记录的持有状态，如果此时 Application 使用同一个 Checkpoint，记录同步就需要等待自己，会死锁。
   *) 注意，需要确保Cache同步的所有App都访问相同的Database。

#### TODO

每事务提交
网络：压缩、加密、解密


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
