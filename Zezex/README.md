
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

. Online.Transmit
  查询目标角色数据并发送给客户端。
  这里请求会在目标角色在线时，转发给所在的provider（gs）。
  目标角色不在线，就根据目标roleId.GetHashCode，固定选择一个provider并转发。
  这样做的目的是提高cache命中率。

. ModuleRedirect

  这个主要目的是为了提高共享模块的 cache 命中率问题。see linkd.provider.txt 排行榜相关问题。

  【例子和说明】
  [ModuleRedirect()]
  public virtual TaskCompletionSource<int> RunSomeMethod(int param, Game.TransactionModes mode = Game.TransactionModes.ExecuteInAnotherThread)
  {
    int hash = Game.ModuleRedirect.GetChoiceHashCode();
    return App.Zeze.Run(() => SomeMethod(hash, param), nameof(SomeMethod), mode, hash);
  }
  1) [ModuleRedirect()]: 注解声明，表明需要转发支持，去掉就不会被转发。
  2) virtual:            必须的，否则生成的时候会抛异常报错。
  3) mode:               这个参数可选，不提供的话，ModuleRedirect 默认按 ExecuteInAnotherThread 处理。不支持ExecuteInAnotherThread的实现是没必要声明ModuleRedirect的。
  4) return:             TaskCompletionSource<int>，调用者可以等待调用完成； 或者void。【推荐 void，不关心处理结果】。等待其他存储过程结束是很危险的，可能会导致死锁。
  5) RunSomeMethod：     方法名必须以"Run"开头，否则生成代码时会报错。这是为了区分普通模块接口和可能起一个新事务的接口。

  *) 接口方法的实现功能一般为：计算hash；传递一下参数给真正的实现；决定使用什么模式执行存储过程；决定是否使用TaskOneByOne（App.Zeze.Run的最后一个参数）。
     具体的实现逻辑在后面这个方法中。

  protected int SomeMethod(int hash, int param)
  {
    // 真正的实现代码
    return Procedure.Success;
  }
  1) SomeMethod: 开放接口名字去掉"Run"，剩下的部分。
  2) return:     返回存储过程处理结果，跟协议处理 ProcessXXX 一样的定义。
  3) params:    【第一个是数据分组hash】；其他是自定义参数，和真正开放方法的参数一致，但不包括最后的mode。
  4) 注意:       这个函数被调用时可能，上下文中可能没有 Login.Session。如果实现需要这个了再说。
  5) protected:  子类能调用，建议不开放。

  【总结】
   规范要求实现分成两个方法，这是为了实现简单，也保持灵活，还有效率。
   ModuleRedirect 通过生成模块的子类来实现转发请求。
   如果仅通过调用 base.RunSomeMethod 来实现，mode就不能省掉。因为底层需要根据情况修改mode的值。

   【接口方法返回数据的两种方案】
   1) 同步模式，通过 out ref 参数返回数据。内部需要等待实际存储过程执行完毕。有死锁风险。
   2) 异步模式，通过增加一个回调，"Action<int> resultCallback"。内部需要返回数据时，回调这个接口。
      这个模式使用时，要注意，resultCallback一般在另一个事务中回调。注意表中的数据跨事务传递的问题。

. ModuleRedirectWithHash

  根据指定的 hash 转发请求。第一个参数必须是int hash。
  内部转发的时候使用指定的 hash，而不是根据Session计算。
  除此外，其他和 ModuleRedirect 一样。

. ModuleRedirectAll
  遍历处理所有的 hash 分组。执行的效果和MapReduce类似。这里更加专用化。
  由于每个 hash 分组都可能有返回值，所以不能使用ref|out返回数据，只能使用callback。每个分组分别回调。
  a) ModuleRedirectAll 的接口方法参数如：(..., Action<...> onHashResult, Action onHashEnd)
    onHashResult 如 Action<long, int, int, ...> 用来处理hash分组的结果。hash分组的处理没有返回值时，不需要这个参数。
    1) 第一个模板参数是long sessionId，用来区分不同的调用。
    2) 第二个模板参数是int hash，用来区分不同的hash分组。
    3) 第三个模板参数是int returncode，hash分组的处理结果，只有Success时，自定义参数才有效。
    4) ... 自定义参数。
    onHashEnd 类型必须是 Action<ModuleRedirectAllContext>，当所有的hash分组都处理完的时候回调，不关心处理完成情况时，可以不定义这个参数。
  b) ModuleRedirectAll 的实现方法参数如：(long sessionId, int hash, ..., Action<...> onHashResult)
    实现方法不需要 onHashEnd 参数。
    sample: see Game/ModuleRank/RunGetRank

 【ModuleRedirect 汇总方案选择】
  当需要遍历所有的hash分组时，可以使用 ModuleRedirectAll，也可以直接从数据库中读取。
  采用 ModuleRedirectAll 时，可以把hash分组的读也分配到相关的配置服务器中，不会破坏缓存，具有很高的命中率。
  具体采用哪种方案，需要根据具体需求来决定。一般建议如下：
  1) 当分组数据量不大的时候直接从数据库中装载，不使用ModureRedirect。see server\Game\ModuleRank.GetRank.
  2）当分组数据量比较大，但是处理结果的数据量比较小，此时ModuleRedirectAll比较适合。
  3) 原则上所有的读取结果都可以在本地缓存，定时更新，此时上面两种方案都比较高效。如果需求不能使用定时缓存，那么ModuleRedirectAll更合适。

. client 选择 unity+ts
  尽量采用 rpc？

. map 动态绑定

test git tag and checkout