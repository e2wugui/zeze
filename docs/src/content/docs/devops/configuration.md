---
title: "配置参考"
sidebar:
  order: 1
---

Zeze 的配置通过 XML 文件（默认 `zeze.xml`）管理。除非特殊说明，所有配置值都可以使用默认值。建议同一个服务的所有分布式实例共享一份配置文件（仅 `ServerId` 必须不同）。

## zeze 根元素

`<zeze>` 是配置文件的根节点，所有全局属性和子节点都定义在其中。

```xml
<zeze
    Name=""
    ServerId="0"
    CheckpointPeriod="60000"
    CheckpointMode="Table"
    CheckpointFlushMode="MultiThreadMerge"
    CheckpointModeTableFlushSetCount="50"
    CheckpointTransactionPeriod="300000"
    NoDatabase="false"
    GlobalCacheManagerHostNameOrAddress=""
    GlobalCacheManagerPort="5002"
    ServiceManager=""
    OnlineLogoutDelay="60000"
    WorkerThreads="0"
    ScheduledThreads="0"
    ProcessReturnErrorLogLevel="INFO"
    AllowReadWhenRecordNotAccessed="true"
    AllowSchemasReuseVariableIdWithSameType="true"
    FastRedoWhenConflict="false"
    AutoResetTable="false"
    DelayRemoveHourStart="3"
    DelayRemoveHourEnd="7"
    DelayRemoveDays="7"
    OfflineTimerLimit="200"
    Dbh2LocalCommit="true"
    ProviderThreshold="2000"
    ProviderOverload="4000"
    ProcedureStatisticsReportPeriod="60000"
    TableStatisticsReportPeriod="60000"
    HotWorkingDir=""
    HotDistributeDir="distributes"
    DeadLockBreakerPeriod="60000"
    ProcedureLockWatcherMin="50"
    AppVersion=""
    History=""
>
    <!-- 子节点 -->
</zeze>
```

### 全局属性详解

#### ServerId

**分布式必需**。每个服务实例的唯一编号。Server 使用正整数（0, 1, 2, ...），Linkd 可使用负数（-1, -2, ...）以避免与 Server 冲突。

#### Name

配置名称。Zeze 内部未使用，仅供扩展。

#### CheckpointPeriod

事务持久化的定时间隔（毫秒）。默认 60000（1 分钟）。CheckpointMode 为 `Table` 时按此间隔触发脏数据持久化。

#### CheckpointMode

持久化模式。可选值：

| 值 | 说明 |
|---|---|
| `Table` | 按事务关联集合为单位持久化。**分布式模式必须使用**。实际上也是定时触发，使用 CheckpointPeriod 间隔 |
| `Immediately` | 每个事务立即持久化，性能最差，一般不建议 |

#### CheckpointFlushMode

持久化的线程模式。可选值：

| 值 | 说明 |
|---|---|
| `SingleThread` | 单线程持久化 |
| `MultiThread` | 多线程持久化 |
| `SingleThreadMerge` | 单线程合并持久化，合并多个 Zeze 事务为一个后端数据库事务 |
| `MultiThreadMerge` | **多线程合并持久化（推荐）**，性能最优 |

#### CheckpointModeTableFlushSetCount

合并模式下一次合并的 Zeze 事务数量。默认 50。

#### CheckpointTransactionPeriod

事务 checkpoint 周期。默认 300000（5 分钟）。

#### NoDatabase

是否关闭数据库支持。Linkd 等不需要数据库的服务设为 `true`。

#### GlobalCacheManagerHostNameOrAddress

全局缓存管理器地址。格式丰富：

- 单台：`"192.168.1.1"`
- 多台：`"ip1:port1;ip2:port2;ip3:port3"`
- Raft 版：`"GlobalCacheManagersConf"`（引用子节点配置）
- 空字符串：不启用分布式缓存同步

#### GlobalCacheManagerPort

全局缓存管理器端口。当使用单台地址模式时生效。

#### ServiceManager

服务发现模式。可选值：

| 值 | 说明 |
|---|---|
| `""` | 默认，启用单点版 ServiceManager |
| `"raft"` | 启用 Raft 版 ServiceManager，配合 ServiceManagerConf 使用 |
| `"disable"` | 禁用服务发现 |

#### OnlineLogoutDelay

客户端断线后延迟自动登出的时间（毫秒）。默认 60000（1 分钟）。在此期间客户端可以重连（ReLogin）。

#### WorkerThreads

普通线程池工作线程数量。实际使用 `max(WorkerThreads, availableProcessors * 30)`。

#### ScheduledThreads

调度线程池工作线程数量。实际使用 `max(ScheduledThreads, availableProcessors)`。

#### ProcessReturnErrorLogLevel

存储过程返回错误码时的日志级别。默认 `INFO`。

#### FastRedoWhenConflict

是否在事务冲突时立即重做。默认 `false`，即事务执行到结束后再检测冲突。开启后可能增加 CPU 开销。

#### AutoResetTable

是否自动重置表状态。默认 `false`。

#### DelayRemoveHourStart / DelayRemoveHourEnd / DelayRemoveDays

延迟删除记录的配置。定义每天的执行时间段（默认凌晨 3 点到 7 点），保留数据天数（默认 7 天）。

#### ProviderThreshold / ProviderOverload

Arch Provider 负载控制。任务延迟超过 Threshold（默认 2000ms）标记为忙碌，超过 Overload（默认 4000ms）标记为过载（熔断）。

#### HotWorkingDir / HotDistributeDir

热更新配置。`HotWorkingDir` 为工作目录，`HotDistributeDir` 为发布文件子目录名。

#### DeadLockBreakerPeriod

死锁检测器周期（毫秒）。默认 60000。

#### ProcedureLockWatcherMin

存储过程锁等待监控阈值。默认 50。

#### AppVersion

应用版本号。用于灰度发布和版本控制。

#### History

历史记录配置。非空时启用事务历史记录功能。

## DatabaseConf

数据库配置节点。支持多个数据库，通过 Name 区分。

```xml
<DatabaseConf Name=""
    DatabaseType="Memory"
    DatabaseUrl=""
    DatabaseName="zeze_mongodb"
    DistTxn="false"
    DisableOperates="false">
    <DruidConf DriverClassName="" UserName="" Password=""
        InitialSize="" MinIdle="" MaxActive="" MaxWait=""
        MaxOpenPreparedStatements="" PhyMaxUseCount="" PhyTimeoutMillis=""/>
    <DynamoConf region="CN_NORTH_1"/>
</DatabaseConf>
```

### DatabaseConf 属性

#### Name

数据库名称。空字符串表示默认数据库。所有未指定 DatabaseName 的表都归属于默认数据库。

#### DatabaseType

数据库类型。可选值：

| 类型 | 说明 |
|------|------|
| `Memory` | 内存数据库，用于测试 |
| `MySql` | MySQL 及兼容数据库 |
| `PostgreSQL` | PostgreSQL |
| `MongoDb` | MongoDB |
| `SqlServer` | SQL Server |
| `Tikv` | TiKV |
| `RocksDb` | RocksDB，仅单机模式，不能与 GlobalCacheManager 同时使用 |
| `Redis` | Redis |
| `DynamoDb` | Amazon DynamoDB |
| `Dbh2` | Zeze Dbh2 分布式数据库 |

#### DatabaseUrl

数据库连接参数。示例：

```xml
<!-- Memory -->
<DatabaseConf DatabaseType="Memory" DatabaseUrl=""/>

<!-- MySQL -->
<DatabaseConf DatabaseType="MySql"
    DatabaseUrl="jdbc:mysql://localhost:3306/devtest?user=dev&amp;password=devtest12345&amp;useSSL=false"/>

<!-- PostgreSQL -->
<DatabaseConf DatabaseType="PostgreSQL"
    DatabaseUrl="jdbc:postgresql://localhost:5432/devtest?user=dev&amp;password=devtest12345"/>

<!-- MongoDB -->
<DatabaseConf DatabaseType="MongoDb" DatabaseName="mygame"
    DatabaseUrl="mongodb://localhost:27017"/>

<!-- RocksDB -->
<DatabaseConf DatabaseType="RocksDb" DatabaseUrl="/data/rocksdb"/>

<!-- TiKV -->
<DatabaseConf DatabaseType="Tikv" DatabaseUrl="172.21.15.68:2379"/>

<!-- Dbh2 -->
<DatabaseConf DatabaseType="Dbh2" DatabaseUrl="dbh2://127.0.0.1:10999/dbh2_unittest"/>
```

#### DatabaseName

数据库名称。默认 `zeze_mongodb`。主要用于 MongoDB 等。

#### DistTxn

是否启用分布式事务。仅用于 TiKV。默认 `false`。

#### DisableOperates

是否关闭数据库操作（用于系统检测存储过程）。默认 `false`。

### DruidConf 子节点

JDBC 连接池配置（MySQL、SQLServer、PostgreSQL 等使用）。未指定的参数使用 Druid 默认值。

| 参数 | 说明 |
|------|------|
| `DriverClassName` | JDBC 驱动类名，未指定则自动搜索 |
| `UserName` | 用户名，未指定则从 DatabaseUrl 获取 |
| `Password` | 密码，未指定则从 DatabaseUrl 获取 |
| `InitialSize` | 初始化连接池大小 |
| `MinIdle` | 最小空闲连接数 |
| `MaxActive` | 最大活跃连接数 |
| `MaxWait` | 获取连接等待超时（毫秒） |
| `MaxOpenPreparedStatements` | 最大打开的 PreparedStatement 数量 |
| `PhyMaxUseCount` | 单个连接最大使用次数 |
| `PhyTimeoutMillis` | 物理连接超时（毫秒） |

### DynamoConf 子节点

仅 DynamoDB 使用。`region` 配置 AWS 区域，默认 `CN_NORTH_1`。

## TableConf

表配置节点。可以配置默认表（Name 为空）和指定表。

```xml
<!-- 默认表配置 -->
<TableConf CacheCapacity="20000" CacheFactor="5.0"/>

<!-- 指定表配置 -->
<TableConf Name="demo_Module1_tSample"
    CacheCapacity="1000"
    CacheInitialCapacity="0"
    CacheNewAccessHotThreshold="0"
    CacheFactor="5.0"
    CacheCleanPeriod="10000"
    CacheNewLruHotPeriod="10000"
    CacheMaxLruInitialCapacity="100000"
    CacheCleanPeriodWhenExceedCapacity="1000"
    CheckpointWhenCommit="false"
    DatabaseName=""
    DatabaseOldName=""
    DatabaseOldMode=""/>
```

### TableConf 属性详解

#### CacheCapacity

缓存容量（记录数）。**最重要的配置**。建议配置为预期在线人数。实际容量为 `CacheCapacity * CacheFactor`。

#### CacheFactor

缓存放大因子。默认 5.0。使用 `SoftReference` 实现的内存优化使得实际缓存容量可以远超 CacheCapacity。

#### CacheInitialCapacity

HashMap 初始容量。默认 0（实际使用 `max(0, 31)`）。配置为预计容量可以减少内存重新分配。

#### CacheCleanPeriod

缓存清理定时器间隔（毫秒）。默认 10000。

#### CacheCleanPeriodWhenExceedCapacity

超容量时的加速清理间隔（毫秒）。默认 1000。设为 0 可最大化清理速度但增加 CPU 开销。

#### CacheNewLruHotPeriod

LRU 热点集合创建间隔（毫秒）。默认 10000。

#### CacheMaxLruInitialCapacity

缓存初始容量上限。默认 100000。防止配置过大值导致内存浪费。

#### CheckpointWhenCommit

事务提交时是否立即持久化该表的数据。默认 `false`。适用于需要强持久化保证的关键数据（如充值货币表）。

#### DatabaseName

指定表所属的数据库名称。空字符串表示默认数据库。

#### DatabaseOldName / DatabaseOldMode

旧数据库配置，用于数据迁移场景。启用后，新库中查不到的数据会自动从旧库读取并导入。

## ServiceConf

网络服务配置。

```xml
<!-- 默认网络配置（Name 为空） -->
<ServiceConf Name=""
    NoDelay="true"
    SendBuffer="1M"
    ReceiveBuffer="1M"
    InputBufferMaxProtocolSize="2M"
    OutputBufferMaxSize="2M"
    Backlog="128"
    maxConnections="1024"
    CloseWhenMissHandle="false"
    TimeThrottle=""
    TimeThrottleSeconds=""
    TimeThrottleLimit=""
    TimeThrottleBandwidth=""
    OverBandwidth=""
    OverBandwidthFusingRate="1.0"
    OverBandwidthNormalRate="0.7"
    DhGroups=""
    SecureIp=""
    RsaPubKey=""
    RsaPriKeyFile=""
    CompressS2c="0"
    CompressC2s="0"
    EncryptType="0"
    KeepCheckPeriod="0"
    KeepRecvTimeout="0"
    KeepSendTimeout="0"
    HaProxyKey="">
    <Acceptor Ip="127.0.0.1" Port="7777"/>
    <Connector HostNameOrAddress="127.0.0.1" Port="5001"
        IsAutoReconnect="true" MaxReconnectDelay="8000"/>
</ServiceConf>
```

### ServiceConf 属性详解

#### Name

服务名称。空字符串表示默认配置。命名服务通过名称引用，如 `"Zeze.Services.ServiceManager.Agent"`。

#### maxConnections

最大连接数。默认 1024。Linkd 等网关服务需要调大。

### SocketOptions 属性

#### NoDelay

TCP `TCP_NODELAY` 选项。默认由系统决定。建议游戏服务设为 `true`。

#### SendBuffer / ReceiveBuffer

TCP 发送/接收缓冲区大小。支持 K/M 后缀，如 `"1M"`。不指定则使用系统默认值。

#### Backlog

ServerSocket 的 backlog 参数。默认 128。仅 Acceptor 使用。

#### InputBufferMaxProtocolSize

最大协议包大小。默认 2M。安全选项，防止攻击占用大量内存。

#### OutputBufferMaxSize

最大发送堆积大小。默认 2M。用于 `Service.checkOverflow`。

#### CloseWhenMissHandle

协议没有处理器时是否关闭连接。默认 `false`。

### 流量控制属性

#### TimeThrottle

限速实现选择。可选 `"queue"`（精确）或 `"counter"`（轻量）。不配置则关闭限速。

#### TimeThrottleSeconds

限速时间窗口（秒）。

#### TimeThrottleLimit

限速时间窗口内允许的最大协议数量。

#### TimeThrottleBandwidth

限速时间窗口内允许的最大带宽（字节数）。

#### OverBandwidth

连接带宽限速阈值（字节数）。超过时按策略丢弃协议。

#### OverBandwidthFusingRate

熔断比率。默认 1.0。带宽占用超过此比率时丢弃协议。

#### OverBandwidthNormalRate

恢复正常比率。默认 0.7。带宽占用低于此比率时停止丢弃。

### HandshakeOptions 属性

#### DhGroups

DH 密钥交换算法的 Group 配置。不建议修改。

#### SecureIp

外部 IP 地址。服务器在防火墙后面时，配置客户端看到的外部 IP。

#### CompressS2c / CompressC2s

压缩选项。`0` 不压缩，`1` MPPC 算法，`2` Zstd 算法。

#### EncryptType

加密类型。`0` 不加密，`1` AES 加密。

#### KeepCheckPeriod / KeepRecvTimeout / KeepSendTimeout

连接保活检查。`KeepCheckPeriod` 为检查周期（秒），`KeepRecvTimeout` 为接收超时，`KeepSendTimeout` 为发送超时。0 表示禁用。

#### HaProxyKey

HAProxy 协议密钥。配置后启用 HAProxy Protocol 支持。

### Acceptor 子节点

配置服务端监听。

```xml
<Acceptor Ip="127.0.0.1" Port="7777"/>
<!-- 特殊 IP -->
<Acceptor Ip="@internal" Port="7777"/>  <!-- 自动使用私有网络地址 -->
<Acceptor Ip="@external" Port="7777"/>  <!-- 自动使用公共网络地址 -->
```

| 属性 | 说明 |
|------|------|
| `Ip` | 监听 IP。支持 `@internal`（内网）和 `@external`（外网）快捷方式 |
| `Port` | 监听端口 |

### Connector 子节点

配置客户端连接。

```xml
<Connector HostNameOrAddress="127.0.0.1" Port="5001"
    IsAutoReconnect="true" MaxReconnectDelay="8000"/>
```

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `HostNameOrAddress` | -- | 目标服务器地址 |
| `Port` | -- | 目标服务器端口 |
| `IsAutoReconnect` | `true` | 断线后是否自动重连 |
| `MaxReconnectDelay` | `8000` | 最大重连延迟（毫秒） |

### Websocket 子节点

配置 WebSocket 支持。

```xml
<Websocket Name="ws" Path="/" Ip="0.0.0.0" Port="8080"/>
```

## GlobalCacheManagersConf

全局缓存管理器集群配置。用于分布式缓存同步。

```xml
<zeze GlobalCacheManagerHostNameOrAddress="GlobalCacheManagersConf"
      GlobalCacheManagerPort="5002">
    <GlobalCacheManagersConf>
        <host name="global.raft.xml"/>
        <!-- 多台时配置多个 host -->
    </GlobalCacheManagersConf>
</zeze>
```

每个 `<host>` 的 `name` 属性指定一台 Raft 版 Global 的配置文件名。所有服务器的 host 顺序必须保持一致。

## ServiceManagerConf

服务发现 Raft 配置。

```xml
<zeze ServiceManager="raft">
    <ServiceManagerConf sessionName="" raftXml="servicemanager.raft.xml" loginTimeout="12000"/>
</zeze>
```

| 属性 | 说明 |
|------|------|
| `sessionName` | 会话名称 |
| `raftXml` | Raft 配置文件名 |
| `loginTimeout` | 登录超时（毫秒），默认 12000 |

## Property 子节点

设置 Java 系统属性。

```xml
<Property Key="some.property" Value="some.value"/>
```

## CustomizeConf 子节点

自定义配置节点，供应用扩展使用。

```xml
<CustomizeConf Name="MyCustomConfig">
    <!-- 自定义内容 -->
</CustomizeConf>
```

应用通过 `Config.parseCustomize` 解析自定义配置。
