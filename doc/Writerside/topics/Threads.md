# Threads

## Zeze框架相关线程说明

### ZezeTaskPool
| 名字          | 说明                                         |
|-------------|--------------------------------------------|
| 线程名         | ZezeTaskPool-X 序号从1开始                      |
| 所有者         | Zeze.Util.Task.threadPoolDefault           |
| 获取方法        | Zeze.Util.Task.getThreadPool()             |
| 线程数量        | zeze.xml根元素的属性"WorkerThreads" 默认: CPU核数*30 |
| 任务队列        | LinkedBlockingQueue                        |
| 优先级         | 普通                                         |
| 后台线程        | 是                                          |

用途
1. Zeze.Util.Task.run/Run
2. Zeze.UtilTaskOneByOneByKey.Execute |
线程数量
GlobalCacheManager服务器和ServiceManager服务器默认240

### ZezeScheduledPool
| 名字        | 说明                                            |
|-----------|-----------------------------------------------|
| 线程名       | ZezeScheduledPool-X 序号从1开始                    |
| 所有者       | Zeze.Util.Task.threadPoolScheduled            |
| 获取方法      | Zeze.Util.Task.getScheduledThreadPool()       |
| 线程数量      | zeze.xml根元素的属性"ScheduledThreads" 默认: CPU核数*15 |
| 优先级       | 普通                                            |
| 后台线程      | 是                                             |

用途
1. Zeze.Util.Task.schedule

线程数量
GlobalCacheManager服务器和ServiceManager服务器默认120

### ZezeCriticalPool
| 名字        | 说明                                         |
|-----------|--------------------------------------------|
| 线程名       | ZezeCriticalPool-X 序号从1开始                  |
| 所有者       | Zeze.Util.Task.threadPoolCritical          |
| 取方法       | Zeze.Util.Task.getCriticalThreadPool()     |
| 线程数量      | CachedThreadPool(自动缩放,最小0个,无上限,60秒闲置线程会回收) |
| 任务队列      | SynchronousQueue                           |
| 优先级       | 普通                                         |
| 后台线程      | 是                                          |

用途
1. GlobalCacheServer的客户端(Zeze.Transaction.GlobalClient)处理协议
2. ServiceManager的客户端(Zeze.Services.ServiceManager.AgentClient)处理协议
3. Raft客户端(Zeze.Raft.Agent.NetClient)处理LeaderIs协议和RPC的回复
4. 其它不能排队导致饥饿的关键任务

### Selector
| 名字         | 说明                                             |
|------------|------------------------------------------------|
| 线程名        | Selector-X 序号从0开始                              |
| 所有者        | Zeze.Net.Selectors.SelectorList                |
| 获取方法       | 无, 只能通过Zeze.Net.Selectors.choice()依次选择Selector |
| 线程数量       | min(CPU核数,8)                                   |
| 优先级        | 普通                                             |
| 后台线程       | 是                                              |

用途:
1. selector.select -> SelectorHandle(AsyncSocket) -> Service -> Dispatch

### Checkpoint
| 名字                        | 说明                                                       |
|---------------------------|----------------------------------------------------------|
| 线程名    Checkpoint-X 服务器ID |
| 所有者                       | Zeze.Transaction.Checkpoint.CheckpointThread             | 
| 获取方法                      | 无, 只能通过Zeze.Transaction.Checkpoint.AddActionAndPulse添加任务 |
| 线程数量                      | 每个服一个单线程(非线程池)                                           |
| 任务队列                      | LinkedBlockingQueue                                      |
| 优先级                       | 普通                                                       |
| 后台线程                      |  是                                                       |

用途:
1. 定时执行checkpoint相关任务

### Raft
| 名字     | 说明                                         |
|--------|--------------------------------------------|
| 线程名    | Raft-X-Y X: 线程池序号(从1开始) Y: 线程序号(从1开始)      |
| 所有者    | Zeze.Raft.Raft.ImportantThreadPool         |
| 获取方法   | Zeze.Raft.Raft.getImportantThreadPool()    |
| 线程数量   | CachedThreadPool(自动缩放,最小0个,无上限,60秒闲置线程会回收) |
| 任务队列   | SynchronousQueue                           |
| 优先级    | 普通+2                                       |
| 后台线程   | 是                                          |

用途:
1. 处理Raft相关的重要协议

### AchillesHeelDaemon
| 名字     | 说明                                  |
|--------|-------------------------------------|
| 线程名    | AchillesHeelDaemon                  |
| 所有者    | Zeze.Transaction.AchillesHeelDaemon |
| 线程数量   | 1                                   |
| 优先级    | 普通+2                                |
| 后台线程   |  是                                  |

用途:
1. GlobalAgent的定时任务

### main
| 名字       | 说明   |
|----------|------|
| 线程名      | main |
| 线程数量     | 1    |
| 优先级      | 普通   |
| 后台线程     |  否   |

用途:
1. 主线程, 初始化后一直等待直到进程中断
