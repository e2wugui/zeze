
这是个游戏的简单实现（目标是实现一般即时游戏的主要模块）。

主要用来做Zeze的性能测试。

也可以作为一个新游戏的模板。
这个的修改不保证兼容，需要的话，拷贝一份出去。

. 建议命名规范
  Bean用'B'开头。
  客户端发送服务器处理的协议用'C'开头。
  服务器发送客户端处理的协议用'S'开头。
  Table用't'开头。

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
