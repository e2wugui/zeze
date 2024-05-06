# Misc

## 多数据库支持
Zeze支持配置多个后端数据库，向应用提供逻辑上的一个数据库，支持跨数据库事务。在
zeze.xml中为每个数据库提供一个DatabaseConf 配置。多个数据库需要用不同 Name 区
分。然后在 TableConf 中使用属性 DatabaseName 把表格分配到某个数据库中。一个表只
能属于一个数据库。

## 从老的数据库中装载数据
当使用某些嵌入式数据库（比如bdb）时，如果某个数据库文件很大，但是活跃数据可能又
不多，每次备份它比较费时。可以考虑把表格移到新的数据库，然后系统在新库中找不到记
录时，自动从老库中装载数据。这样，老库是只读的，不用每次备份。TableConf 中使用属
性 DatabaseOldName 指明老的数据库，同时属性 DatabaseOldMode 设为 1。可以很好
支持增量备份的数据库不需要使用这个特性。

## 同一个进程内多个Zeze.Application之间的事务
XXX 已经被废弃，不再支持，需要的喊！写在这里作为备注。
一般来说，事务仅仅访问一个 Zeze.Application 的数据库表格。如果需要在多个
Zeze.Application 之间支持事务。应用直接访问不同 App.Module。里面的表格即可完成事
务支持。不过由于事务提交(Checkpoint)默认是在一个 Zeze.Application中执行的，为了让
事务提交也原子化。需要在App.Start前设置统一Checkpoint。
设置代码例子：
```
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
```

## 统计
Zeze统计了几乎所有各种可能的情况。c#可以通过宏完全关闭统计。这个主要为了以后便
于进行性能分析。
