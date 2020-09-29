
这是个游戏的简单实现（目标是实现一般即时游戏的主要模块）。

主要用来做Zeze的性能测试。

也可以作为一个新游戏的模板。
这个的修改不保证兼容，需要的话，拷贝一份出去。

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
