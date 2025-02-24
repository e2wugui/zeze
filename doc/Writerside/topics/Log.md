# Log

本章介绍Zeze日志的配置选项，定制方法。

## Task 日志
所有经Task启动的任务在执行失败的时候会记录日志。记录日志使用Task.ILogAction接口。
这个接口实现保存在全局变量Task.logAction中，被Task.logAndStatistics方法调用。
* 设置变量Task.logAction为null将禁止日志记录。
* 可以通过自己实现ILogAction并设置到变量Task.logAction中，就可以定制自己的日志。
* 变量初始值和接口定义如下：
```
public static ILogAction logAction = Task::DefaultLogAction;

@FunctionalInterface
public interface ILogAction {
    void run(@Nullable Throwable ex, long result, @Nullable Protocol<?> p, @NotNull String actionName);
}
```

## Procedure 日志
所有的存储过程执行失败的日志都会自动记录。记录日志使用Procedure.ILogAction接口。
这个接口实现保存在全局变量Procedure.logAction中。
* 设置变量Procedure.logAction为null将禁止日志记录。
* 可以通过自己实现ILogAction并设置到变量Procedure.logAction中，就可以定制自己的日志。
* 变量初始值和接口定义如下：
```
public static @Nullable ILogAction logAction = Procedure::defaultLogAction;

@FunctionalInterface
public interface ILogAction {
    void run(@Nullable Throwable ex, long result, @NotNull Procedure p, @NotNull String message);
}
```

## Protocol 日志
所有经由Net收发的协议根据选项输出到日志文件中。可以通过JVM参数进行配置。
* -DprotocolLog=INFO 设置协议日志的级别(log4j2定义的级别)，默认是OFF(无日志)。
* -DprotocolLogExcept=typeId,typeId,... 协议日志排除的协议类型，多个类型用半角逗号分隔，默认不排除。通常用于排除频繁且对调试分析没什么帮助的协议，如位置同步相关的。

更多协议日志相关的内容详见[Net.md](Net.md)

## 统计日志
统计日志主要用于性能分析，默认是开启的(对性能影响不大)，开启后会定时输出上一段时间统计的结果到日志中(INFO级别)。
统计默认包含协议收发、Task和事务运行、数据表读写等，允许提供自定义数据源(见PerfCounter类提供的方法)。可以通过JVM参数进行配置。
* -DperfPeriod=100 定时输出日志的时间周期(秒)，默认100。如果值<1则当做1。
* -DperfCount=20 每组统计排序输出最重要的条目数，默认20。如果值<=0则表示禁用统计和输出。

Application类构造时会自动开启统计日志，如果不使用Application，可调用下面方法开启:
```
PerfCounter.instance.tryStartScheduledLog();
```

#### 日志输出说明
首行日志如下(高亮部分是输出的日志,其余为说明)
```
PerfCounter: count last 100002ms:
```
首行日志，描述统计的是前多少毫秒
统计内容分4组如下:
```
 [load: 296ms 0.03% free/total/max: 583/1098/8172M committed/free/all: 1545/19075+23727/32683+40363M]
```
第1组为负载统计，上面的296ms是当前进程在此统计时间内使用的CPU时间，0.03%是此CPU时间占所有CPU资源的百分比。
之后是两组内存统计，前一组free/total/max分别表示当前JVM堆空间的当前空闲大小/当前堆空间大小/堆空间上限；
后一组committed/free/all分别表示当前进程的总虚拟内存分配量/剩余物理内存+剩余页面文件大小/总物理内存+总页面文件大小。
```
 [run: 8593, 1055ms]
```
第2组为Task.call,Task.run,Task.schedule的统计(包括事务的执行)次数和总运行时间，下面的条目按总运行时间排序:
```
  Zeze.Builtin.Provider.Dispatch: 492ms = 113 * 4,358,488ns
  action名: 该action的总耗时(毫秒) = 执行次数 * 平均单次时间(纳秒)
  ......
 [recv: 265, 13K, 49ms]
```
第3组为网络接收协议的数量,总字节大小,处理所有协议占IO线程的总耗时(毫秒)，下面的条目按总耗时排序:
```
  Zeze.Builtin.Provider.Dispatch: 20ms = 113 * 180,824ns,75B
  协议类: 该类协议占IO线程的总耗时(毫秒) = 数量 * 平均单个协议的耗时(纳秒)，平均单个协议的字节大小(包括协议头)
  ......
 [send: 206, 347K]
```
第4组为网络发送协议的数量，总字节大小，不包括Send方法返回失败的(网络连接失效,缓冲区溢出)，下面的条目按总大小排序:
```
  Zeze.Builtin.Provider.Send: 175K = 92 * 1,903B
  协议类: 该类协议的发送字节大小 = 数量 * 平均单个协议的字节大小(包括协议头)
  ......
```
#### 其它说明
1. 统计的时间值只是占用线程的时间，包括其中各种等待的调用(如等锁,同步等IO等),可能不与CPU开销一致。如果只需要统计CPU开销,可使用Async Profiler、Visual VM等统计工具。
2. PerfCounter会自动统计Dispatch和Send内部封装的协议。
3. PerfCounter有addRunInfo,addRecvInfo,addSendInfo三个方法可以提供额外的数据供统计。
4. PerfCounter通常用instance单例就够了，也可以创建新实例做完全自定义的统计。
5. 也可以不启动定时统计输出，而是手动调用getLogAndReset()获取统计信息并重置统计数据。
6. 可通过PerfCounter.cancelScheduledLog方法停止定时输出日志。
7. 可通过下面3个方法忽略指定key或协议ID的统计:
```
PerfCounter.instance.addExcludeRunKey(String key)
PerfCounter.instance.addExcludeRunKey(Class<?> key)
PerfCounter.instance.addExcludeProtocolTypeId(long typeId)
```

## 流量统计日志
用于输出网络(Service)上下行流量的定时统计到日志中(INFO级)，需要通过下面的JVM参数配置开启:
* -D服务名.stat=定时输出的秒数 指定此参数且秒数>0时开启流量统计，服务名即传给Service构造方法时的name参数。

#### 输出说明
```
Service: 服务名.stat: select=SN/ST, recv=RS/RC, send=SS/SC, sendRaw=SR, sockets=SO, ops=OP, outBuf=OB
```
上述两个大写字母代表的数值都是定时间隔期间累计的数值,含义分别如下:
- SN: 服务所属selectors的所有线程调用select的次数(不同服务有可能共享相同的selectors)
- ST: 服务所属selectors的线程数(不同服务有可能共享相同的selectors)
- RS: 服务中所有socket的接收字节数(压缩加密后)
- RC: 服务中所有socket的接收调用次数
- SS: 服务中所有socket的发送字节数(压缩加密后)
- SC: 服务中所有socket的发送调用次数
- SR: 服务中所有socket的发送调用字节数(压缩加密前)
- SO: 服务中所有socket的数量
- OP: 服务中所有socket的等待执行队列的命令总数(绝大多数是发送命令)
- OB: 服务中所有socket的待发送数据的总大小(包括压缩加密前后所有的)

## log4j2.xml 默认配置和选项
发布的zeze jar包内含有一个默认的log4j2.xml配置文件，会被log4j2默认加载(在classpath里)。可以通过JVM参数进行配置。
* -Dlogpath=log 这是日志文件的路径，相对当前目录，默认是log。
* -Dlogname=gs 这是日志文件的名字，默认是zeze。
* -Dloglevel=INFO 设置日志过滤的根级别，小于此级别的日志不会输出，默认是DEBUG。
* -Dlogconsole=Null 不输出日志到stdout，一般用于正式部署服务器时，默认是输出。
* -Dlog4j.configurationFile=path/log4j2.xml 使用自定义的log4j2配置(以上参数可能不再有效)。
