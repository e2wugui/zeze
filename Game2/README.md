
游戏

. 建议命名规范
  Bean用'B'开头。
  客户端发送服务器处理的协议用'C'开头。
  服务器发送客户端处理的协议用'S'开头。
  Table用't'开头, 并且小写。

. linkd 转发

. 采用 cache-sync
  原则上不定义和使用本服相关数据。
  online 数据也持久化。

. ReliableNotify 可靠数据同步
  采用 ChangeListener；断线重连不需要重新装载所有数据。
  server使用接口：
  1) 在客户端下载数据的事务里面 调用 App.Game_Login.Onlines.AddReliableNotifyMark 设置标记。
  2) 在 ChangeListener 里面调用 App.Game_Login.Onlines.SendReliableNotify 发送协议。
  3) 不需要同步时，调用 App.Game_Login.Onlines.RemoveReliableNotifyMark
  同步实现其他相关协议：
  1) Game.Login.CRelogin 断线重连
  2) Game.Login.SReliableNotify 发送给客户端的可靠消息打包
  3) Game.Login.CReliableNotifyConfirm 客户端确认

. ModuleRedirect
  这个主要是提高共享模块cache命中率问题。see linkd.provider.txt 排行榜相关问题。
  可以转发的方法规范
  [ModuleRedirect()]
  public virtual TaskCompletionSource<int> SomeMethod(params..., Game.TransactionModes mode = Game.TransactionModes.ExecuteInAnotherThread)
  1) 注解声明需要转发
  2) virtual 必须的，否则生成的时候会抛异常报错。
  3) mode 这个参数可选，不提供的话，默认仅支持Game.TransactionModes.ExecuteInAnotherThread模式。

. client 选择 unity+ts
  尽量采用 rpc？

. map 动态绑定怎么实现
