---
title: "配置参考"
description: "Zeze XML 配置的完整元素与属性速查：全局、数据库、表、服务、网络握手"
category: reference
order: 8
---

本文是 Zeze **XML 配置**的完整参考——覆盖 `<zeze>` 全局属性、`DatabaseConf`、`TableConf`、`ServiceConf`、网络握手、Acceptor/Connector、GlobalCacheManagers、ServiceManager 等所有配置元素及其属性，供运维和调优时随查随用。数据库选型见 [选配数据库](../manual/06-choosing-database.md)，上线检查见 [上线清单](../manual/08-production-checklist.md)。

## `<zeze>` 根元素

根元素 `<zeze>` 携带全局配置属性：

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `ServerId` | — | 分布式必需。Server 为正整数，Linkd 可为负数 |
| `Name` | — | 未使用，供扩展 |
| `CheckpointPeriod` | `60000` | Checkpoint 周期（毫秒） |
| `CheckpointMode` | `Table` | `Table`（按表批量刷写，默认，分布式必须）/ `Immediately`（每事务立即，源码中标注为不可用） |
| `CheckpointFlushMode` | `MultiThreadMerge` | `SingleThread` / `MultiThread` / `SingleThreadMerge` / `MultiThreadMerge`（推荐，默认） |
| `CheckpointModeTableFlushSetCount` | `50` | |
| `CheckpointTransactionPeriod` | `300000` | |
| `NoDatabase` | `false` | Linkd 等设 `true` |
| `GlobalCacheManagerHostNameOrAddress` | — | 单台 ip / 多台 `ip1:port1;ip2:port2` / Raft 版见 `GlobalCacheManagersConf` / 空不启用 |
| `GlobalCacheManagerPort` | — | |
| `ServiceManager` | `""` | `""` 默认单点 / `raft` / `disable` |
| `OnlineLogoutDelay` | `60000` | |
| `WorkerThreads` | `0`（0 时按 `processors*30` 自动设置） | 普通业务线程池容量 |
| `ScheduledThreads` | `0`（0 时按 `processors` 自动设置） | 调度线程池容量 |
| `ProcessReturnErrorLogLevel` | `INFO` | |
| `AllowReadWhenRecordNotAccessed` | `true` | |
| `FastRedoWhenConflict` | `false` | |
| `AutoResetTable` | `false` | |
| `DelayRemoveHourStart` | `3` | |
| `DelayRemoveHourEnd` | `7` | |
| `DelayRemoveDays` | `7` | |
| `OfflineTimerLimit` | `200` | |
| `ProviderThreshold` | `2000` | |
| `ProviderOverload` | `4000` | |
| `HotWorkingDir` | `""`（空串，即当前目录） | 热更新工作目录 |
| `HotDistributeDir` | `distributes` | 热更新分发目录 |
| `DeadLockBreakerPeriod` | `60000` | |
| `ProcedureLockWatcherMin` | `50` | |
| `AppVersion` | — | 应用版本（表名后缀 `@AppMainVersion` 用到） |
| `History` | — | |

```xml
<zeze ServerId="1"
      CheckpointPeriod="60000"
      CheckpointMode="Table"
      CheckpointFlushMode="MultiThreadMerge"
      GlobalCacheManagerHostNameOrAddress="127.0.0.1"
      GlobalCacheManagerPort="5533">
    ...
</zeze>
```

---

## `<DatabaseConf>` 数据库配置

| 属性 | 必填 | 说明 |
|------|------|------|
| `Name` | 是 | 库名。**空串表示默认库** |
| `DatabaseType` | 是 | 见下方类型表 |
| `DatabaseUrl` | 是 | 连接串，见下方示例 |
| `DatabaseName` | 否 | 库名（MongoDB 等需要） |
| `DistTxn` | 否 | 分布式事务，仅 TiKV |
| `DisableOperates` | 否 | 禁用的操作 |

### DatabaseType 取值

| 类型 | 说明 |
|------|------|
| `Memory` | 纯内存 |
| `MySql` | MySQL |
| `PostgreSQL` | PostgreSQL |
| `MongoDb` | MongoDB |
| `SqlServer` | SQL Server |
| `Tikv` | TiKV |
| `RocksDb` | RocksDB |
| `Redis` | Redis |
| `DynamoDb` | DynamoDB（**当前版本工厂未接线，配置即抛异常，不可用**） |
| `Dbh2` | Dbh2 |

### DatabaseUrl 示例

| DatabaseType | DatabaseUrl 示例 |
|--------------|------------------|
| `Memory` | 空 |
| `MySql` | `jdbc:mysql://127.0.0.1:3306/db` |
| `PostgreSQL` | `jdbc:postgresql://127.0.0.1:5432/db` |
| `MongoDb` | `mongodb://127.0.0.1:27017`（配合 `DatabaseName`） |
| `RocksDb` | `./rocksdb_data`（路径） |
| `Tikv` | `172.21.15.68:2379` |
| `Dbh2` | `dbh2://127.0.0.1:10999/dbh2_unittest` |

### `<DruidConf>` 子节点（关系型连接池）

| 属性 | 说明 |
|------|------|
| `DriverClassName` | JDBC 驱动类名 |
| `UserName` | 用户名 |
| `Password` | 密码 |
| `InitialSize` | 初始连接数 |
| `MinIdle` | 最小空闲 |
| `MaxActive` | 最大活跃 |
| `MaxWait` | 最大等待 |
| `MaxOpenPreparedStatements` | 最大预编译语句 |
| `PhyMaxUseCount` | 物理连接最大使用次数 |
| `PhyTimeoutMillis` | 物理连接超时 |

### `<DynamoConf>` 子节点

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `region` | `CN_NORTH_1` | 区域 |

```xml
<DatabaseConf Name="" DatabaseType="MySql"
              DatabaseUrl="jdbc:mysql://127.0.0.1:3306/game">
    <DruidConf DriverClassName="com.mysql.cj.jdbc.Driver"
               UserName="root" Password="***"
               InitialSize="5" MinIdle="5" MaxActive="50"/>
</DatabaseConf>

<DatabaseConf Name="" DatabaseType="RocksDB" DatabaseUrl="./rocksdb_data"/>

<DatabaseConf Name="" DatabaseType="Memory"/>
```

---

## `<TableConf>` 表配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `Name` | — | 表名。**空串表示默认配置**（应用于所有表） |
| `CacheCapacity` | — | **最重要**。建议设为预期在线人数；实际 = `CacheCapacity × CacheFactor` |
| `CacheFactor` | `5.0` | 容量因子 |
| `CacheInitialCapacity` | — | 缓存初始容量 |
| `CacheNewAccessHotThreshold` | — | 新热点段阈值 |
| `CacheCleanPeriod` | — | 缓存清理周期 |
| `CacheNewLruHotPeriod` | — | 新 LRU 热点周期 |
| `CacheMaxLruInitialCapacity` | — | LRU 最大初始容量 |
| `CacheCleanPeriodWhenExceedCapacity` | — | 超容量时清理周期 |
| `CheckpointWhenCommit` | `false` | 提交时即刷写，适用关键数据 |
| `DatabaseName` | — | 指定该表使用的库 |
| `DatabaseOldName` | — | 旧库名（数据迁移） |
| `DatabaseOldMode` | — | 旧库模式（数据迁移） |

```xml
<!-- 默认配置（应用于所有表） -->
<TableConf Name="" CacheCapacity="10000" CacheFactor="5.0"/>

<!-- 关键数据：提交即刷写 -->
<TableConf Name="tAccount" CheckpointWhenCommit="true"/>

<!-- 指定库 -->
<TableConf Name="tLog" DatabaseName="logDb"/>
```

---

## `<ServiceConf>` 网络服务配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `Name` | — | 服务名 |
| `NoDelay` | — | 游戏建议 `true` |
| `SendBuffer` | — | 发送缓冲，支持 `K`/`M` 后缀 |
| `ReceiveBuffer` | — | 接收缓冲，支持 `K`/`M` 后缀 |
| `InputBufferMaxProtocolSize` | `2M` | 最大协议大小，防攻击 |
| `OutputBufferMaxSize` | — | 输出缓冲最大 |
| `Backlog` | — | 连接队列 |
| `maxConnections` | `1024` | 最大连接数 |
| `CloseWhenMissHandle` | — | 无处理器时关闭连接 |
| `TimeThrottle` | — | 流控（`queue` / `counter`） |
| `TimeThrottleSeconds` | — | 流控时间窗口 |
| `TimeThrottleLimit` | — | 流控限额 |
| `TimeThrottleBandwidth` | — | 带宽流控 |
| `OverBandwidth` | — | 超带宽阈值 |
| `OverBandwidthFusingRate` | `1.0` | 超带宽熔断率 |
| `OverBandwidthNormalRate` | `0.7` | 超带宽正常率 |
| `DhGroups` | — | 握手 DH 组 |
| `SecureIp` | — | 安全 IP |
| `RsaPubKey` | — | RSA 公钥 |
| `RsaPriKeyFile` | — | RSA 私钥文件 |
| `CompressS2c` | — | 服务端到客户端压缩（`0` 关 / `1` MPPC / `2` Zstd） |
| `CompressC2s` | — | 客户端到服务端压缩 |
| `EncryptType` | — | 加密类型（`0` 关 / `1` AES） |
| `KeepCheckPeriod` | — | 心跳检查周期 |
| `KeepRecvTimeout` | — | 接收超时 |
| `KeepSendTimeout` | — | 发送超时 |
| `HaProxyKey` | — | HAProxy 密钥 |

### 流控

| 机制 | 说明 |
|------|------|
| `TimeThrottle` | `queue` / `counter` 两种限流 |
| `OverBandwidth` | 超带宽阈值，配合熔断率/正常率 |

### 握手 / 加密

| 属性 | 说明 |
|------|------|
| `DhGroups` | DH 握手组 |
| `SecureIp` | 安全 IP 白名单 |
| `CompressS2c` / `CompressC2s` | 压缩（`0`/`1` MPPC/`2` Zstd） |
| `EncryptType` | 加密（`0` 关 / `1` AES） |
| `KeepCheckPeriod` / `KeepRecvTimeout` / `KeepSendTimeout` | 心跳 |
| `HaProxyKey` | HAProxy 代理密钥 |

```xml
<ServiceConf Name="GameServer" NoDelay="true"
             SendBuffer="1M" ReceiveBuffer="1M"
             InputBufferMaxProtocolSize="2M"
             maxConnections="1024"
             CompressS2c="2" EncryptType="1"/>
```

---

## `<Acceptor>` 监听端点

| 属性 | 说明 |
|------|------|
| `Ip` | 监听 IP，支持 `@internal` / `@external` |
| `Port` | 端口 |

```xml
<Acceptor Ip="@external" Port="20000"/>
<Acceptor Ip="@internal" Port="20001"/>
```

## `<Connector>` 连接端点

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `HostNameOrAddress` | — | 主机名或地址 |
| `Port` | — | 端口 |
| `IsAutoReconnect` | — | 是否自动重连 |
| `MaxReconnectDelay` | `8` | 最大重连延迟（**XML 中单位为秒**，框架内部解析时 `×1000` 转为毫秒；最小 1000 毫秒） |

```xml
<Connector HostNameOrAddress="127.0.0.1" Port="5533" IsAutoReconnect="true"/>
```

## `<Websocket>` WebSocket 端点

| 属性 | 说明 |
|------|------|
| `Name` | 名称 |
| `Path` | 路径 |
| `Ip` | IP |
| `Port` | 端口 |

```xml
<Websocket Name="wsGame" Path="/ws" Ip="0.0.0.0" Port="20002"/>
```

---

## `<GlobalCacheManagersConf>` 全局缓存管理器（Raft 版）

多台时配多个 `<host>`，**顺序须一致**：

```xml
<GlobalCacheManagersConf>
    <host name="global.raft.xml"/>
    <!-- 多台：顺序必须各实例一致 -->
    <host name="global2.raft.xml"/>
</GlobalCacheManagersConf>
```

## `<ServiceManagerConf>` 服务管理器

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `sessionName` | — | 会话名 |
| `raftXml` | — | Raft 配置 |
| `loginTimeout` | `12000` | 登录超时（毫秒） |

```xml
<ServiceManagerConf sessionName="default" raftXml="sm.raft.xml" loginTimeout="12000"/>
```

---

## `<Property>` Java 系统属性

设置 Java 系统属性（等价 `-D`）：

| 属性 | 说明 |
|------|------|
| `Key` | 属性名 |
| `Value` | 属性值 |

```xml
<Property Key="zeze.debug" Value="true"/>
```

## `<CustomizeConf>` 自定义配置

自定义配置节点，由 `Config.parseCustomize` 解析。

---

## 完整示例骨架

```xml
<?xml version="1.0" encoding="UTF-8"?>
<zeze ServerId="1"
      CheckpointPeriod="60000"
      CheckpointMode="Table"
      CheckpointFlushMode="MultiThreadMerge"
      GlobalCacheManagerHostNameOrAddress="127.0.0.1"
      GlobalCacheManagerPort="5533"
      OnlineLogoutDelay="60000"
      AppVersion="1">

    <!-- 数据库：默认 MySQL + 日志库 -->
    <DatabaseConf Name="" DatabaseType="MySql"
                  DatabaseUrl="jdbc:mysql://127.0.0.1:3306/game">
        <DruidConf DriverClassName="com.mysql.cj.jdbc.Driver"
                   UserName="root" Password="***"
                   InitialSize="5" MinIdle="5" MaxActive="50"/>
    </DatabaseConf>

    <DatabaseConf Name="logDb" DatabaseType="MySql"
                  DatabaseUrl="jdbc:mysql://127.0.0.1:3306/gamelog"/>

    <!-- 表配置 -->
    <TableConf Name="" CacheCapacity="10000" CacheFactor="5.0"/>
    <TableConf Name="tAccount" CheckpointWhenCommit="true"/>
    <TableConf Name="tLog" DatabaseName="logDb"/>

    <!-- 网络服务 -->
    <ServiceConf Name="GameServer" NoDelay="true"
                 SendBuffer="1M" ReceiveBuffer="1M"
                 InputBufferMaxProtocolSize="2M"
                 maxConnections="1024"
                 CompressS2c="2" EncryptType="1"/>

    <!-- 监听端点 -->
    <Acceptor Ip="@external" Port="20000"/>
    <Acceptor Ip="@internal" Port="20001"/>

    <!-- 连接 GlobalCacheManager -->
    <Connector HostNameOrAddress="127.0.0.1" Port="5533" IsAutoReconnect="true"/>

    <!-- WebSocket -->
    <Websocket Name="wsGame" Path="/ws" Ip="0.0.0.0" Port="20002"/>

    <!-- Raft 版 GCM -->
    <GlobalCacheManagersConf>
        <host name="global.raft.xml"/>
    </GlobalCacheManagersConf>

    <!-- 服务管理器 -->
    <ServiceManagerConf sessionName="default" raftXml="sm.raft.xml" loginTimeout="12000"/>

    <!-- Java 系统属性 -->
    <Property Key="zeze.debug" Value="false"/>

</zeze>
```

## 相关文档

- [选配数据库](../manual/06-choosing-database.md) — DatabaseType 如何选
- [上线清单](../manual/08-production-checklist.md) — 生产环境配置检查
- [数据库概览](./db-overview.md) — 各数据库后端特性
- [Provider-Linkd 架构](./arch-provider-linkd.md) — ServerId、Linkd 配置
