# Component

Component是Zeze内建模块；

## DelayRemove
```
remove(TableX<K, ?> table, K key)
```
可以直接使用这个静态方法，或者通过Table.delayRemove使用。通过这里的remove删除
的记录只会被加入延迟队列，然后在过一段时间才会被真的删除。这个功能最初的原因是为
了解决基于记录的队列（Zeze.Collections.LinkedMap）的记录锁外并发遍历和删除的问题。
LinkedMap用记录存储Node，并在Node中记录前后Node的记录的Key，每个Node存
储一定量的Item。这个LinkedMap可以存储很大的Item数量。当遍历LinkedMap时，不
能在一个事务内访问所有的记录（即Node），必须挨个处理记录，这样当下一个记录刚好
被删除，由于当前Node还指向被删除的记录，那么如果马上删除记录，获取下一个记录时，
会得到null，就不能继续遍历了。有了DelayRemove，当LinkedMap要删除记录时，先更
新前后记录的索引，更新后执行的遍历只遍历新的记录，并发的遍历也能继续进行。一定时
间以后，这个记录才会被真正删除。这个可以看作是一个简单垃圾回收机制。这个模块不需
要初始化。

## AutoKey
AutoKey用来分配系统内唯一的Id（long类型）。仅保证唯一，不保证顺序。这个模块由Zeze
初始化, 不需要初始化。可通过Zeze.Application.AutoKeys.GetOrAdd(autoKeyName)得到
AutoKey实例，然后访问实例的方法NextId得到下一个Id。保存GetOrAdd的返回值，以
后继续使用，可以稍微提高点效率。

## RedoQueue
Zeze系统内提供了事务。但实际服务器架构中可能存在非Zeze的系统，而且其中的某些操
作需要跨系统的事务支持。目前Zeze没有向外部系统提供事务支持。在符合一定条件下，
可以用一个简单机制（而不是跨系统的事务）来达到目的。举个游戏的例子，一般游戏架构
中有个独立的账号充值系统，完成充值的时候需要在游戏（ZezeApplication）内给玩家发放
物品，这样的操作是可以重做的，只要游戏没有返回成功，充值系统可以一直重试。这就是
RedoQueue完成的功能。RedoQueue提供了一个可自定义任务内容的框架，完成任务同步
和重试。
* RedoQueue分成两个部分。
1.	RedoQueue(Client) 部分给非ZezeApplication用，它把加入队列的任务直接本地存储到
      RocksDb中，同时把任务发送给ZezeApplication进行处理。
2.	RedoQueueServer 部分在ZezeApplication中执行。具体任务执行的操作需要应用自己注册。
      RedoQueueServer记录了队列的已完成任务编号，如果出现了回档（已完成任务编号变成
      以前的），队列会从当前没有完成的任务编号开始，继续处理整个队列。这就是叫做Redo
      的原因。
* RedoQueue主要接口
```
void add(int taskType, Zeze.Serialize.Serializable taskParam)
      taskType, taskParam完全由应用自己定义。
```
* RedoQueueServer主要接口
```
void register(String queue, int type, Predicate<Binary> task)
      queue 队列名字，一个RedoQueueServer支持多个Client。
```

* RedoQueueServer初始化
```
      MyApp.RedoQueueServer = new RedoQueueServer(zeze);
      MyApp.RedoQueueServer.Start();
```

## Timer
### Auto Named Timer
```
String Schedule(delay); 一定延迟后执行一次。返回TimerId。
String Schedule(delay, period); 一定延迟后开始按间隔执行。
String Schedule(cron); crontab风格定时器配置。
// a)自动命名的定时器。每次调用调度函数都会注册新的定时器，内部自动命名。自动
// 命名的定时器的名字以‘@’开头。
// b)持久化。重启以后Timer会继续调度。
// c)分布式。一开始在注册所在的Server上执行，当Server非法宕机会被调度到其他Server上。
// d)真正的调度用ScheduledThreadPool.schedule实现。
```

### Named Timer
```
boolean ScheduleNamed(string name, ...);
// 全局Timer，每个名字只有一份实例。如果注册时改名字已经存在，名字存在会返回false。
// 一般来说这个timer跑在注册它时所在的server实例上。但有可能被调度到其他server实例
// 上。
```

### Online Timer
```
String ScheduleOnline(userid, …);
```
这些Timer和用户绑定，支持账号或者RoleId。仅在满足相关在线状态时才生效，具有一定
的自动生命期管理。
1. 仅在在线时允许注册，下线全部自动失效。
2. 非持久化！在线timer是把保存在内存里的，直接使用语言自带的Timer调度器实
现。生命期跟随ModuleOnline.LocalData走。有这样的特性：用户登录在Server A
上；没有正常下线(Logout)；又登录到Server B上；此时Server B会向Server A发
一个Kick Local；然后Server A上的online-timer也会结束。这个流程不是原子的，
仅是最终保证；也就是说某个特别的时刻，AB上同时都有该timer。
3. 数量不限！由登录在一个server进程上的最高在线用户自动形成限制。
4. 仅存在于登录所在的Server。
5. 生命期和ModuleOnline.LocalData一致。

### Offline Timer
```
String ScheduleOffline(userid, …);
```
这些Timer和用户绑定，支持账号或者RoleId。仅在满足相关在线状态时才生效，具有一定
的自动生命期管理。
1. 仅在下线时允许注册，上线全部失效。一般在登出事件中，注册所有的Offline Timer。
下线期间允许继续注册，但估计不常见。
2. 持久化！
3. 数量有限制。每个用户用一个Offline Bean存储它所有的定时器。
4. 持续限制！Offline Timer不能一直持续，需要次数或者时间限制。
5. 只允许在一台Server“下线”，即Offline Timer都在一台上注册和调度。当在下线在
ServerA，然后又登录到ServerB时，ServerA的Timer会被去取消。
f)	生命期和ModuleOnline.LocalData相反。Offline Bean在ModuleOnline的登录事件
中删除，内嵌到登录事务中，这样可以保持数据一致性。但取消ThreadPool的任务
通过@Redirect通知。为了处理Redirect可能丢失的问题，需要在Offline Timer中
记录Login.Version，并且在触发定期回调时检查定时器版本号是否和当前登录版本
号一致。
