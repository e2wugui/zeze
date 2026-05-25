# DOC_PLAN.md — Zeze 文档生成计划

> 本文件是 Phase 1 的产出，定义文档树、每篇文档的 Scope（应读代码）和生成顺序。
> 后续 Phase 2 中每次 AI 调用应参照本文件中的条目执行。

---

## 一、文档树总览

保持 Astro/Starlight 的目录结构不变，按 `autogenerate` 组织侧边栏。
现有 60+ 文件经合并精简后约 **35 篇**。

```
docs/src/content/docs/
├── index.mdx                          # 首页（已有，微调）
├── getting-started/
│   ├── preface.md                     # 前言：Zeze 是什么、设计哲学
│   ├── quick-start.md                 # 快速上手：3 步走（定义→生成→使用）
│   ├── theory.md                      # 理论基础：乐观锁、MVCC、缓存一致性
│   └── setup.md                       # 环境搭建：JDK/Maven/Gradle/IDE
├── core/
│   ├── bean.md                        # Bean 数据模型：类型系统、版本兼容
│   ├── solution-xml.md                # Solution.xml 完整参考
│   ├── transaction.md                 # 事务系统：Procedure、重做、WhileCommit
│   ├── serialize.md                   # 序列化协议：编解码格式、跨语言
│   ├── table.md                       # Table 存储接口：CRUD、缓存、持久化
│   ├── listener.md                    # ChangeListener：数据变更监听
│   └── third-party-interactions.md    # 事务中操作外部系统
├── architecture/
│   ├── arch.md                        # Provider-Linkd 架构全貌
│   ├── online.md                      # Online 在线管理：可靠消息、Transmit
│   ├── redirect.md                    # Redirect 系列：Hash/All/ToServer
│   ├── net.md                         # 网络层：Service/Protocol/Rpc/WebSocket
│   └── session.md                     # Session 与 UserState 生命周期
├── database/
│   ├── overview.md                    # 数据库抽象层总览：Database/Storage/Table
│   ├── relational.md                  # MySQL/PostgreSQL 关系型映射
│   ├── nosql.md                       # MongoDB/Redis/DynamoDB/FDB/TiKV
│   ├── rocksdb.md                     # RocksDB 嵌入式引擎
│   └── dbh2.md                        # Dbh2 分布式数据库层
├── collections/
│   ├── overview.md                    # 持久化集合总览
│   ├── queue.md                       # Queue 持久队列
│   ├── linked-map.md                  # LinkedMap 有序映射
│   ├── department-tree.md             # DepartmentTree 部门树
│   ├── chashmap.md                    # CHashMap 一致性哈希
│   ├── bool-list.md                   # BoolList 位图列表
│   └── dag.md                         # DAG 有向无环图
├── services/
│   ├── service-manager.md             # ServiceManager 服务发现
│   ├── global-cache-manager.md        # GlobalCacheManager 缓存同步
│   ├── raft.md                        # Raft 共识实现
│   ├── timer.md                       # Timer 定时器组件
│   ├── autokey.md                     # AutoKey 自增ID
│   └── delay-remove.md               # DelayRemove 延迟删除
├── game/
│   ├── overview.md                    # 游戏模块总览
│   ├── bag.md                         # 背包系统
│   ├── rank.md                        # 排行榜
│   ├── login-queue.md                 # 登录队列
│   └── task.md                        # 任务系统
├── advanced/
│   ├── hot-reload.md                  # 热更新：ClassReloader、Hot模块
│   ├── threads.md                     # 线程模型：线程池、Checkpoint、调度
│   ├── mq.md                          # 消息队列：MQ/RocketMQ 集成
│   ├── onz.md                         # Onz 分布式事务编排
│   ├── history.md                     # History 数据溯源
│   ├── metrics.md                     # Prometheus 监控集成
│   └── performance.md                 # 性能调优指南
├── devops/
│   ├── configuration.md               # 完整配置参考（XML）
│   ├── maven-deploy.md                # Maven 中央仓库发布
│   └── release-and-update.md          # 版本发布与滚动更新
├── multi-language/
│   ├── cxx-client.md                  # C++ 客户端接入
│   ├── typescript-client.md           # TypeScript 客户端接入
│   └── csharp-client.md               # C# 客户端接入
└── draft/                             # 草稿区（保留）
```

---

## 二、生成顺序与分组

按依赖关系排序，先基础后上层。同组内可并行生成。

| 批次 | 文档 | 理由 |
|------|------|------|
| **1** | preface, theory, quick-start, setup | 入门组，无代码依赖，定义整体认知框架 |
| **2** | bean, serialize, solution-xml, table | 数据层，最基础的抽象 |
| **3** | transaction, listener, third-party-interactions | 事务层，依赖数据层 |
| **4** | arch, net, session, online, redirect | 架构层，依赖事务层 |
| **5** | database/* | 数据库层，依赖 table |
| **6** | services/* | 服务层，依赖架构层 |
| **7** | collections/* | 集合层，依赖 table+transaction |
| **8** | game/* | 游戏模块，依赖 services+collections |
| **9** | advanced/* | 高级话题，依赖前面全部 |
| **10** | devops/*, multi-language/* | 运维与多语言，独立性强 |
| **11** | index.mdx | 首页，最后微调以确保链接有效 |

---

## 三、逐篇 Scope 定义

### 批次 1：入门指南

#### preface.md — 前言
- **目标读者**：首次接触 Zeze 的开发者
- **范围**：Zeze 解决什么问题、设计哲学、与同类框架对比
- **应读代码**：`Zeze.Application.java`（入口）、`README.md`
- **现有基线**：当前 preface.md 仅 5 行，需完全重写
- **不涉及**：具体 API、配置细节
- **交叉引用**：→ quick-start, theory

#### theory.md — 理论基础
- **目标读者**：想理解 Zeze 内部原理的开发者
- **范围**：乐观锁原理、MVCC、缓存一致性协议（GlobalCacheManager 角色）、为什么不会死锁
- **应读代码**：
  - `Zeze.Transaction.Transaction.java`
  - `Zeze.Transaction.Procedure.java`
  - `Zeze.Transaction.Locks.java`
  - `Zeze.Transaction.TransactionLevel.java`
- **现有基线**：当前仅标题骨架，需完全重写
- **交叉引用**：→ transaction(深入), performance

#### quick-start.md — 快速上手
- **目标读者**：想立刻动手的开发者
- **范围**：定义 XML → 生成代码 → 写逻辑 的三步流程，包含完整可运行示例
- **应读代码**：
  - `ZezeJava/ZezexJava/server/src/` 下的 Login 模块示例
  - `ZezeJava/ZezexJava/solution.xml`（前 30 行的简单部分）
- **现有基线**：现有 405 行，内容充实但混杂了 C#/Java 两种环境。重写聚焦 Java
- **交叉引用**：→ solution-xml, bean, transaction, arch

#### setup.md — 环境搭建
- **目标读者**：准备开发的开发者
- **范围**：JDK 安装、Maven/Gradle 配置、IDE 设置、依赖引入、Gen 工具使用
- **应读代码**：`ZezeJava/ZezeJava/pom.xml`（依赖声明）
- **现有基线**：当前散布在 quick-start 中，拆出独立文档
- **交叉引用**：→ quick-start

---

### 批次 2：数据层

#### bean.md — Bean 数据模型
- **目标读者**：使用 Zeze 定义数据的开发者
- **范围**：Bean 类型系统（int/long/string/list/map/set/动态 Bean）、variable id 规则、版本兼容、嵌套 Bean、GenericBean
- **应读代码**：
  - `Zeze.Transaction.Bean.java`
  - `Zeze.Transaction.DynamicBean.java`
  - `Zeze.Serialize.Serializable.java`
- **现有基线**：75 行，偏薄，需扩充类型参考和版本兼容规则
- **交叉引用**：→ solution-xml, serialize

#### solution-xml.md — Solution.xml 参考
- **目标读者**：定义数据模型和协议的开发者
- **范围**：完整 XML Schema 参考——solution/module/bean/variable/table/rpc/protocol/project/service，所有属性和取值
- **应读代码**：
  - `ZezeJava/ZezexJava/solution.xml`（完整参考）
  - `ZezeJava/ZezexJava/solution.linkd.xml`
  - `ZezeJava/ZezeJavaTest/solution.xml`
- **现有基线**：102 行，需大量扩充为完整参考
- **交叉引用**：→ bean, transaction, arch

#### serialize.md — 序列化协议
- **目标读者**：需要跨语言对接的开发者
- **范围**：Zeze 二进制编解码格式、各类型编码规则、跨语言一致性保证
- **应读代码**：
  - `Zeze.Serialize.ByteBuffer.java`
  - `Zeze.Serialize.IByteBuffer.java`
- **现有基线**：180 行，质量较好，微调补充即可
- **交叉引用**：→ bean, cxx-client, typescript-client

#### table.md — Table 存储接口
- **目标读者**：读写持久化数据的开发者
- **范围**：Table CRUD API（get/getOrAdd/put/remove）、缓存策略（内存/数据库同步）、TableReadOnly、TableDynamic、Walk 遍历
- **应读代码**：
  - `Zeze.Transaction.Table.java`
  - `Zeze.Transaction.TableX.java`
  - `Zeze.Transaction.TableCache.java`
  - `Zeze.Transaction.Storage.java`
  - `Zeze.Transaction.Record.java`
  - `Zeze.Transaction.Record1.java`
- **现有基线**：无独立文档，内容散在 transaction.md 中，需拆出
- **交叉引用**：→ transaction, database/overview

---

### 批次 3：事务层

#### transaction.md — 事务系统
- **目标读者**：编写业务逻辑的开发者
- **范围**：存储过程概念、TransactionLevel 配置、嵌套存储过程、WhileCommit/WhileRollback、重做语义、自定义日志、返回值约定、@DispatchMode
- **应读代码**：
  - `Zeze.Transaction.Transaction.java`
  - `Zeze.Transaction.Procedure.java`
  - `Zeze.Transaction.TransactionLevel.java`
  - `Zeze.Transaction.Savepoint.java`
  - `Zeze.Transaction.Log.java`
  - `Zeze.Util.TransactionLevelAnnotation.java`
  - `Zeze.Util.DispatchModeAnnotation.java`
- **现有基线**：180 行，内容较完整，需补充 @DispatchMode 和 Savepoint
- **交叉引用**：→ theory, table, listener, third-party-interactions

#### listener.md — ChangeListener
- **目标读者**：需要监听数据变更的开发者
- **范围**：ChangeListener 注册、变更类型（Insert/Update/Remove）、与 WhileCommit 的配合
- **应读代码**：
  - `Zeze.Transaction.ChangeListener.java`
  - `Zeze.Transaction.ChangeListenerMap.java`
  - `Zeze.Transaction.Changes.java`
- **现有基线**：149 行，质量可，微调
- **交叉引用**：→ transaction, online

#### third-party-interactions.md — 第三方交互
- **目标读者**：需要调用外部系统的开发者
- **范围**：事务边界内操作非 Zeze 数据的最佳实践
- **应读代码**：无特定文件（方法论文档）
- **现有基线**：87 行，质量可，微调
- **交叉引用**：→ transaction

---

### 批次 4：架构层

#### arch.md — Provider-Linkd 架构
- **目标读者**：搭建分布式服务的架构师
- **范围**：Provider/Linkd/ServiceManager/GlobalCacheManager 角色与交互、启动停止顺序、负载均衡、绑定亲缘性、静态/动态绑定、ProtocolRef
- **应读代码**：
  - `Zeze.Arch.ProviderApp.java`
  - `Zeze.Arch.LinkdApp.java`
  - `Zeze.Arch.ProviderImplement.java`
  - `Zeze.Arch.LinkdProvider.java`
  - `Zeze.Arch.ProviderService.java`
  - `Zeze.Arch.LinkdService.java`
  - `Zeze.Arch.ProviderModuleBinds.java`
  - `Zeze.Arch.LoadConfig.java`
  - `Zeze.Arch.ProviderOverload.java`
- **现有基线**：546 行，内容非常充实，需重新组织结构、去除冗余
- **交叉引用**：→ net, session, online, redirect, service-manager

#### net.md — 网络层
- **目标读者**：需要理解网络通信机制的开发者
- **范围**：Service 抽象、Protocol/Rpc 基类、连接管理（Acceptor/Connector）、压缩加密、WebSocket、SSL 握手
- **应读代码**：
  - `Zeze.Net.Service.java`
  - `Zeze.Net.Protocol.java`
  - `Zeze.Net.Rpc.java`
  - `Zeze.Net.AsyncSocket.java`
  - `Zeze.Net.Acceptor.java`
  - `Zeze.Net.Connector.java`
  - `Zeze.Net.Websocket.java`
  - `Zeze.Services.HandshakeServer.java`
  - `Zeze.Services.HandshakeClient.java`
- **现有基线**：待评估，需从现有内容重建
- **交叉引用**：→ arch, serialize

#### session.md — Session 与 UserState
- **目标读者**：管理连接状态的开发者
- **范围**：AsyncSocket.UserState → Protocol.UserState → Procedure.UserState 传递链、ProviderUserSession、LinkdUserSession
- **应读代码**：
  - `Zeze.Arch.ProviderUserSession.java`
  - `Zeze.Arch.LinkdUserSession.java`
  - `Zeze.Net.AsyncSocket.java`（UserState 相关方法）
- **现有基线**：无独立文档，内容在 arch.md 中一节，拆出
- **交叉引用**：→ arch, online

#### online.md — Online 在线管理
- **目标读者**：实现在线功能的开发者
- **范围**：Online API 全貌——本机数据、登录/登出事件、ReliableNotify、SendToLogin/SendToAccount、Transmit、Broadcast
- **应读代码**：
  - `Zeze.Arch.Online.java`
  - `Zeze.Arch.OnlineSend.java`
  - `Zeze.Arch.AbstractOnline.java`
  - `Zeze.Builtin.Game.Online/`（Bean 定义）
- **现有基线**：内容在 arch.md 中内嵌，需独立成文
- **交叉引用**：→ arch, listener, redirect

#### redirect.md — Redirect 系列
- **目标读者**：实现跨服调用的开发者
- **范围**：@RedirectHash、@RedirectAll、@RedirectToServer 三种模式的使用方法、参数约定、返回值处理
- **应读代码**：
  - `Zeze.Arch.RedirectHash.java`
  - `Zeze.Arch.RedirectAll.java`
  - `Zeze.Arch.RedirectToServer.java`
  - `Zeze.Arch.RedirectBase.java`
  - `Zeze.Arch.RedirectFuture.java`
  - `Zeze.Arch.RedirectAllFuture.java`
- **现有基线**：内容在 arch.md 中分散，需独立成文
- **交叉引用**：→ arch, online

---

### 批次 5：数据库层

#### database/overview.md — 数据库抽象层总览
- **目标读者**：选择和配置数据库的架构师
- **范围**：Database 抽象、Storage 层、Table-Storage-Database 关系、DatabaseType 选择、多数据库混合配置
- **应读代码**：
  - `Zeze.Transaction.Database.java`
  - `Zeze.Transaction.Storage.java`
  - `Zeze.Transaction.DatabaseMemory.java`
  - `Zeze.Config.java`（DatabaseConf 解析部分）
- **现有基线**：无，新建
- **交叉引用**：→ table, configuration

#### database/relational.md — 关系型数据库
- **目标读者**：使用 MySQL/PostgreSQL 的开发者
- **范围**：JDBC 配置、关系映射模式（二维表 vs KV）、连接池（Druid）
- **应读代码**：
  - `Zeze.Transaction.DatabaseJdbc.java`
  - `Zeze.Transaction.DatabaseMySql.java`
  - `Zeze.Transaction.DatabasePostgreSQL.java`
  - `Zeze.Transaction.DatabaseRelationalMapping.java`
- **现有基线**：无，新建
- **交叉引用**：→ database/overview, configuration

#### database/nosql.md — NoSQL 数据库
- **目标读者**：使用 MongoDB/Redis/DynamoDB 等的开发者
- **范围**：各 NoSQL 的配置方式、特性差异
- **应读代码**：
  - `Zeze.Transaction.DatabaseMongoDb.java`
  - `Zeze.Transaction.DatabaseRedis.java`
  - `Zeze.Transaction.DatabaseDynamoDb.java`
  - `Zeze.Transaction.DatabaseTikv.java`
- **现有基线**：无，新建
- **交叉引用**：→ database/overview

#### database/rocksdb.md — RocksDB
- **目标读者**：使用嵌入式 KV 的开发者
- **范围**：RocksDB 配置、性能调优参数
- **应读代码**：
  - `Zeze.Transaction.DatabaseRocksDb.java`
- **现有基线**：无，新建
- **交叉引用**：→ database/overview

#### database/dbh2.md — Dbh2
- **目标读者**：使用 Dbh2 分布式数据库的开发者
- **范围**：Dbh2 架构、配置、分桶、Raft 集成
- **应读代码**：
  - `Zeze.Dbh2.Dbh2.java`
  - `Zeze.Dbh2.Dbh2Manager.java`
  - `Zeze.Dbh2.Commit.java`
  - `Zeze.Dbh2.Bucket.java`
- **现有基线**：100 行，需扩充
- **交叉引用**：→ database/overview, raft

---

### 批次 6：服务层

#### services/service-manager.md — ServiceManager
- **目标读者**：部署分布式服务的运维/开发者
- **范围**：服务注册与发现、Subscribe 模式、Raft 模式
- **应读代码**：
  - `Zeze.Services.ServiceManagerServer.java`
  - `Zeze.Services.ServiceManagerWithRaft.java`
  - `Zeze.Services.ServiceManagerAgentWithRaft.java`
- **现有基线**：在 services.md 中一节，拆出独立
- **交叉引用**：→ arch, raft

#### services/global-cache-manager.md — GlobalCacheManager
- **目标读者**：理解缓存一致性的开发者
- **范围**：缓存同步协议、单机/分布式模式、性能影响
- **应读代码**：
  - `Zeze.Services.GlobalCacheManagerServer.java`
  - `Zeze.Services.GlobalCacheManagerWithRaft.java`
  - `Zeze.Services.GlobalCacheManagerAsyncServer.java`
  - `Zeze.Transaction.GlobalAgent.java`
  - `Zeze.Transaction.GlobalClient.java`
- **现有基线**：在 services.md 中一节，拆出独立
- **交叉引用**：→ theory, raft, performance

#### services/raft.md — Raft
- **目标读者**：使用 Raft 服务的开发者
- **范围**：Raft 实现、StateMachine 接口、选举、日志复制、快照
- **应读代码**：
  - `Zeze.Raft.Raft.java`
  - `Zeze.Raft.Server.java`
  - `Zeze.Raft.Agent.java`
  - `Zeze.Raft.StateMachine.java`
  - `Zeze.Raft.Log.java`
  - `Zeze.Raft.RaftConfig.java`
- **现有基线**：114 行，需扩充 StateMachine 接口和配置
- **交叉引用**：→ service-manager, global-cache-manager

#### services/timer.md — Timer
- **目标读者**：使用定时器的开发者
- **范围**：Timer 注册（Simple/Cron/周期）、Account/Role 级别、Online 集成、离线 Timer
- **应读代码**：
  - `Zeze.Component.AbstractTimer.java`
  - `Zeze.Component.Timer.java`
  - `Zeze.Component.TimerHandle.java`
  - `Zeze.Component.TimerContext.java`
- **现有基线**：在 game/timer.md 中有部分，需扩展为通用文档
- **交叉引用**：→ transaction, online

#### services/autokey.md — AutoKey
- **目标读者**：需要分布式自增 ID 的开发者
- **范围**：AutoKey 原理、步长配置、ServerId 关系
- **应读代码**：
  - `Zeze.Component.AbstractAutoKey.java`
  - `Zeze.Component.AutoKey.java`
- **现有基线**：在 components/overview.md 中一节，拆出
- **交叉引用**：→ configuration

#### services/delay-remove.md — DelayRemove
- **目标读者**：需要延迟清理数据的开发者
- **范围**：DelayRemove 使用场景和 API
- **应读代码**：
  - `Zeze.Component.AbstractDelayRemove.java`
  - `Zeze.Component.DelayRemove.java`
- **现有基线**：在 components/overview.md 中一节，拆出
- **交叉引用**：→ timer

---

### 批次 7：集合层

#### collections/overview.md — 持久化集合总览
- **目标读者**：需要使用 Zeze 集合的开发者
- **范围**：集合模块统一初始化、与 Table 的关系
- **应读代码**：`Zeze.Collections.BeanFactory.java`
- **现有基线**：在 components/overview.md 中
- **交叉引用**：→ 各集合文档

#### collections/queue.md — Queue
- **目标读者**：使用持久队列的开发者
- **范围**：Queue API（FIFO/LIFO）、并发安全、示例
- **应读代码**：
  - `Zeze.Collections.AbstractQueue.java`
  - `Zeze.Collections.Queue.java`
- **现有基线**：319 行，质量好，微调
- **交叉引用**：→ collections/overview

#### collections/linked-map.md — LinkedMap
- **目标读者**：使用有序映射的开发者
- **范围**：LinkedMap API、遍历、清理
- **应读代码**：
  - `Zeze.Collections.AbstractLinkedMap.java`
  - `Zeze.Collections.LinkedMap.java`
- **现有基线**：存在 chashmap.md 但标记 [Detail By AI]
- **交叉引用**：→ collections/overview

#### collections/department-tree.md — DepartmentTree
- **目标读者**：实现组织架构的开发者
- **范围**：DepartmentTree API、树操作
- **应读代码**：
  - `Zeze.Collections.AbstractDepartmentTree.java`
  - `Zeze.Collections.DepartmentTree.java`
- **现有基线**：存在但较薄
- **交叉引用**：→ collections/overview, linked-map

#### collections/chashmap.md — CHashMap
- **目标读者**：需要分布式一致性哈希的开发者
- **范围**：CHashMap 原理与 API
- **应读代码**：`Zeze.Collections.CHashMap.java`
- **现有基线**：存在
- **交叉引用**：→ collections/overview

#### collections/bool-list.md — BoolList
- **目标读者**：需要位图的开发者
- **范围**：BoolList API、位操作
- **应读代码**：`Zeze.Collections.AbstractBoolList.java`, `Zeze.Collections.BoolList.java`
- **现有基线**：存在
- **交叉引用**：→ collections/overview

#### collections/dag.md — DAG
- **目标读者**：需要 DAG 的开发者
- **范围**：DAG 有向无环图的增删查
- **应读代码**：`Zeze.Collections.AbstractDAG.java`, `Zeze.Collections.DAG.java`
- **现有基线**：无，新建
- **交叉引用**：→ collections/overview

---

### 批次 8：游戏模块

#### game/overview.md — 游戏模块总览
- **目标读者**：游戏服务端开发者
- **范围**：Game 模块（Bag/Rank/Online）统一架构、ProviderWithOnline 初始化
- **应读代码**：
  - `Zeze.Game.ProviderWithOnline.java`
  - `Zeze.Game.ProviderLoadWithOnline.java`
- **现有基线**：game.md 仅 34 行，需大幅扩充
- **交叉引用**：→ arch, online

#### game/bag.md — 背包系统
- **目标读者**：实现背包的开发者
- **范围**：Bag API、物品增删移、容量管理
- **应读代码**：
  - `Zeze.Game.AbstractBag.java`
  - `Zeze.Game.Bag.java`
  - `Zeze.Builtin.Game.Bag/`
- **现有基线**：无独立文档
- **交叉引用**：→ game/overview, bean

#### game/rank.md — 排行榜
- **目标读者**：实现排行榜的开发者
- **范围**：Rank API、并发分区、RedirectAll 查询
- **应读代码**：
  - `Zeze.Game.AbstractRank.java`
  - `Zeze.Game.Rank.java`
  - `Zeze.Builtin.Game.Rank/`
- **现有基线**：无独立文档
- **交叉引用**：→ game/overview, redirect, collections

#### game/login-queue.md — 登录队列
- **目标读者**：实现登录排队的开发者
- **范围**：LoginQueue 使用和配置
- **应读代码**：
  - `Zeze.Services.AbstractLoginQueue.java`
  - `Zeze.Services.LoginQueue.java`
  - `Zeze.Services.LoginQueueServer.java`
- **现有基线**：有文档，需更新
- **交叉引用**：→ arch, online

#### game/task.md — 任务系统
- **目标读者**：实现任务系统的开发者
- **范围**：Task 模块使用
- **应读代码**：`Zeze.Builtin.Collections.Queue/`（任务队列基于 Queue）
- **现有基线**：有文档，需更新
- **交叉引用**：→ collections/queue

---

### 批次 9：高级话题

#### advanced/hot-reload.md — 热更新
- **目标读者**：需要不停服更新的开发者
- **范围**：ClassReloader 原理、Hot 模块（HotAgent/HotDistribute/HotService）、热更新流程
- **应读代码**：
  - `Zeze.Util.ClassReloader.java`
  - `Zeze.Hot.HotAgent.java`
  - `Zeze.Hot.HotDistribute.java`
  - `Zeze.Hot.HotService.java`
  - `Zeze.Hot.HotModule.java`
  - `Zeze.Hot.DistributeManager.java`
- **现有基线**：散布在 reload-class.md 和 reload-run.md 中，需合并重建
- **交叉引用**：→ arch, devops/release-and-update

#### advanced/threads.md — 线程模型
- **目标读者**：性能调优的开发者
- **范围**：线程池分类、Selector、Checkpoint 线程、Raft 线程、任务调度
- **应读代码**：
  - `Zeze.Util.Task.java`
  - `Zeze.Transaction.Checkpoint.java`
  - `Zeze.Net.Selector.java`
  - `Zeze.Component.AbstractThreading.java`
- **现有基线**：119 行，质量可，需补充 Task API
- **交叉引用**：→ transaction, performance

#### advanced/mq.md — 消息队列
- **目标读者**：使用 MQ 的开发者
- **范围**：Zeze 内置 MQ、RocketMQ 集成、生产者/消费者
- **应读代码**：
  - `Zeze.MQ.MQ.java`
  - `Zeze.MQ.MQManager.java`
  - `Zeze.MQ.MQProducer.java`
  - `Zeze.MQ.MQConsumer.java`
- **现有基线**：draft/MQ.md 草稿，需重建
- **交叉引用**：→ transaction, third-party-interactions

#### advanced/onz.md — Onz 分布式事务编排
- **目标读者**：实现 Saga 等分布式事务模式的开发者
- **范围**：Onz 模块——Saga、Procedure 编排
- **应读代码**：
  - `Zeze.Onz.Onz.java`
  - `Zeze.Onz.OnzSaga.java`
  - `Zeze.Onz.OnzProcedure.java`
  - `Zeze.Onz.OnzTransaction.java`
- **现有基线**：onz.md 存在，需更新
- **交叉引用**：→ transaction, raft

#### advanced/history.md — History 数据溯源
- **目标读者**：需要审计日志的开发者
- **范围**：History 模块、Apply 机制
- **应读代码**：
  - `Zeze.History.History.java`
  - `Zeze.History.HistoryModule.java`
  - `Zeze.History.ApplyHelper.java`
- **现有基线**：无，新建
- **交叉引用**：→ transaction, listener

#### advanced/metrics.md — Prometheus 监控
- **目标读者**：运维人员
- **范围**：Prometheus 集成、指标暴露、Dashboard 配置
- **应读代码**：
  - `Zeze.Util.PrometheusCounter.java`
  - `Zeze.Util.ZezeCounter.java`
  - `Zeze.Services.AbstractStatistics.java`
- **现有基线**：metrics-dashboard.md 存在，需更新
- **交叉引用**：→ devops/configuration, threads

#### advanced/performance.md — 性能调优
- **目标读者**：需要优化性能的架构师
- **范围**：缓存命中率、Checkpoint 策略、全局并发度、RocksDB 调优、Benchmark 数据
- **应读代码**：
  - `Zeze.Transaction.Checkpoint.java`
  - `Zeze.Transaction.Profiler.java`
  - `Zeze.Transaction.ProcedureStatistics.java`
- **现有基线**：83 行，需扩充调优建议
- **交叉引用**：→ threads, database/rocksdb, global-cache-manager

---

### 批次 10：运维与多语言

#### devops/configuration.md — 配置参考
- **目标读者**：运维人员
- **范围**：完整 XML 配置参考——zeze/DatabaseConf/ServiceConf/Acceptor/Connector 所有属性
- **应读代码**：
  - `Zeze.Config.java`
  - `Zeze.Net.ServiceConf.java`
  - `Zeze.Net.SocketOptions.java`
- **现有基线**：devops-and-configuration.md 595 行，内容充实，重组结构
- **交叉引用**：→ 所有配置相关文档

#### devops/maven-deploy.md — Maven 发布
- **目标读者**：框架维护者
- **范围**：Sonatype 发布流程、GPG 签名、版本管理
- **应读代码**：`ZezeJava/ZezeJava/pom.xml`（distributionManagement 部分）
- **现有基线**：已有，微调
- **交叉引用**：→ setup

#### devops/release-and-update.md — 版本发布
- **目标读者**：运维人员
- **范围**：滚动更新流程、优雅停止、版本兼容
- **应读代码**：
  - `Zeze.Arch.ProviderService.java`（setDisableChoiceFromLinks）
  - `Zeze.Schemas.java`（版本相关）
- **现有基线**：已有，需更新
- **交叉引用**：→ arch, hot-reload

#### multi-language/cxx-client.md — C++ 客户端
- **目标读者**：使用 C++ 接入的开发者
- **范围**：C++ 库结构、ByteBuffer、Net、Protocol、编译接入
- **应读代码**：
  - `cxx/Makefile`
  - `cxx/ByteBuffer.cpp`
  - `cxx/Net.cpp`
  - `cxx/Protocol.cpp`
- **现有基线**：无，新建
- **交叉引用**：→ serialize, net

#### multi-language/typescript-client.md — TypeScript 客户端
- **目标读者**：使用 TS 接入的前端开发者
- **范围**：TS 库结构、编译、接入方式
- **应读代码**：
  - `TypeScript/package.json`
  - `TypeScript/tsconfig.json`
  - `TypeScript/ByteBuffer.ts`
- **现有基线**：无，新建
- **交叉引用**：→ serialize

#### multi-language/csharp-client.md — C# 客户端
- **目标读者**：Unity 开发者
- **范围**：C# 库结构、Unity 接入、代码生成
- **应读代码**：
  - `confcs/solution.xml`
- **现有基线**：无，新建
- **交叉引用**：→ serialize, solution-xml

---

### 批次 11：首页

#### index.mdx — 首页
- **目标读者**：所有访客
- **范围**：项目简介、核心特性卡片、文档导航
- **现有基线**：已有，需更新链接指向新的文档结构
- **交叉引用**：指向所有一级目录的入口文档

---

## 四、统一模板

每篇文档生成时使用以下 frontmatter 和结构：

```markdown
---
title: "文档标题"
sidebar:
  order: N
---

<!-- 1-2 句概述，回答"这篇文档解决什么问题" -->

## 标题2
<!-- 按逻辑递进组织，每节回答一个明确问题 -->

## 代码示例
<!-- Java 代码块，使用真实 API 签名 -->

## 配置参考（如适用）
<!-- XML 或属性表格 -->

## 注意事项（如适用）
<!-- 常见陷阱、最佳实践 -->

## 相关文档
<!-- 交叉引用列表 -->
```

---

## 五、术语统一表

| 统一用语 | 避免使用 |
|----------|---------|
| 存储过程 | 存储过程函数、事务函数 |
| 乐观锁 | 乐观并发控制 |
| 缓存一致性 | 一致性缓存 |
| 模块 (Module) | 组件（指 solution.xml 中的 module） |
| 协议 (Protocol) | 消息、包 |
| Bean | 数据结构、数据模型 |
| Table | 数据表、表 |
| Linkd | 网关、代理 |
| Provider | 服务端、游戏服务端 |
| Server（首字母大写） | 指代码中的 Service 实例 |
| RedirectHash | redirect-hash、哈希重定向 |
| WhileCommit | while-commit、提交回调 |
