
############################
### 这个例子已经不维护了 ###
############################

这是个游戏的简单例子。

. 建议命名规范
  Bean用'B'开头。
  客户端发送服务器处理的协议用'C'开头。
  服务器发送客户端处理的协议用'S'开头。
  Table用't'开头, 并且小写。

. 客户端连接方式
  1 直接连接服务器，没有特殊需求的话，建议采用这种方式。
  2 连接 Linkd 服务，通过 Linkd 转发协议给后端 Provider。
    考虑增加一个默认的连接转发服务器(Zeze.Services.Linkd)
    这个sample修改成转发模式，需要修改：
    a) 用户账号验证功能移到Linkd中
    b) Game.Login.Session的初始化
    c) 在解析转发的协议时设置Protocol.UserState。
    d) Game.Login.Onlines 发送协议改成转发方式

. Protocol.ResultCode
  考虑到很多时候协议返回结果时，需要设置一个错误码。
  所以在协议里面定义了一个 int ResultCode，总是发送这个参数。

. Table
  Table 定义在 Module 中，默认是私有的。需要公开的话增加访问属性（不建议）。
  Table 成员变量名前加 '_' 字符，代码中可以比较容易识别出来哪些在访问表格。
  * 不能创建自己的 Table 实例，并注册到 Zeze.Application 中。这会导致不可预料的结果。
  * 多个 Zeze.Application 实例访问同一个数据库，需要开启 Cache 同步。see ..\README.md:特殊模式:.4

. 怎么给客户端发送协议
  Game.Login.Session 一般发送给自己
  Game.Login.Onlines 发送给其他玩家
  SendWhileCommit SendWhileRollback 用来在事务成功或者回滚的时候发送

. 全局数据的并发优化
  比如全局排行榜数据的修改。具体数据变化的同时修改排行榜，放在一个事务中执行。
  这虽然很好（可以一起回滚），但是带来并发效率问题。本来具体数据的访问并发度很高，
  现在全部都和全局排行榜关联起来了，可能会造成并发效率大大下降。
  建议采用分成两个事务来修改，处理具体事务时调用 Zeze.Transaction.RunWhileCommit，
  在事务成功时提交一个新的事务来修改排行榜。这种方式修改排行榜时，要注意乱序的问题。
  所以最好使用Zeze.Util.TaskOneByOneByKey把修改排行榜的事务排队。
  提交的操作是在原来具体事务的锁内操作的，所以没有乱序的问题。

. lisenter 问题
  cache 同步和 lisenter 的功能有点问题。如果玩家登录在A服，B服改了数据，直接连接GameGs的方式就notify不到。
  有个方案是还采用link，另外online表也持有化（cache同步能得到）。这样不管在哪个gs上修改数据，
  都能找到玩家所在的link并且发送change-notify。
