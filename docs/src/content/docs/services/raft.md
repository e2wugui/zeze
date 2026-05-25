---
title: "Raft 共识实现"
sidebar:
  order: 3
---

Zeze 实现了完整的 **Raft 共识算法**，用于构建高可用的有状态服务。Raft 保证在少数节点故障的情况下，集群仍然能够正确地提交和复制日志，维持数据一致性。Zeze 的 ServiceManager 和 GlobalCacheManager 均支持 Raft 模式部署。

## 核心组件

### Raft

`Zeze.Raft.Raft` 是 Raft 算法的核心实现，管理节点的状态转换（Follower、Candidate、Leader）、日志复制和选举超时。

```java
// 创建 Raft 实例
var stateMachine = new MyStateMachine();
var raft = new Raft(stateMachine, nodeName, raftConfig);
```

Raft 的关键状态：

- **Follower**：被动接收 Leader 的日志复制和心跳
- **Candidate**：发起选举，请求其他节点投票
- **Leader**：处理客户端请求，将日志复制到所有 Follower

Raft 通过低精度定时器（`lowPrecisionTimer`）驱动选举超时和心跳发送。`leaderHeartbeatTimer` 控制心跳间隔，`electionTimeout` 使用随机化策略避免选举活锁。

### StateMachine

`Zeze.Raft.StateMachine` 是应用状态机的抽象基类，用户需要继承并实现以下接口：

```java
public abstract class StateMachine extends ReentrantLock {
    // 将状态机快照保存到指定路径
    public abstract SnapshotResult snapshot(String path) throws Exception;

    // 从快照文件恢复状态机
    public abstract void loadSnapshot(String path) throws Exception;

    // 重置状态机（可选，用于无快照时的初始化）
    public void reset() { }
}
```

StateMachine 还负责注册 **Log 工厂**，将 Log 类型 ID 映射到构造函数：

```java
public class MyStateMachine extends StateMachine {
    public MyStateMachine() {
        addFactory(AddCountLog.TypeId_, AddCountLog::new);
    }

    @Override
    public SnapshotResult snapshot(String path) throws Exception {
        // 序列化状态到文件
    }

    @Override
    public void loadSnapshot(String path) throws Exception {
        // 从文件恢复状态
    }
}
```

### Log

`Zeze.Raft.Log` 是 Raft 日志条目的抽象基类。每个 Log 代表一次状态机操作。用户需要实现 `apply` 方法和 `typeId` 属性：

```java
public class AddCountLog extends Log {
    private int delta;

    public AddCountLog(IRaftRpc req) {
        super(req);
    }

    @Override
    public long typeId() {
        return TypeId_; // 在 StateMachine 中唯一
    }

    @Override
    public void apply(RaftLog holder, StateMachine sm) throws Exception {
        ((MyStateMachine)sm).count += delta;
    }
}
```

Log 通过 `unique`（`UniqueRequestId`）字段实现重复请求检测。当 `uniqueRequestExpiredDays`（默认 7 天）内的重复请求到达时，Raft 直接返回已缓存的结果（`RaftApplied`），而不是重复执行。

### Server

`Zeze.Raft.Server` 是 Raft 节点的网络层，继承自 `HandshakeBoth`，同时配置 Acceptor（接收其他节点连接）和 Connector（连接其他节点）。它处理以下 Raft 协议：

- **AppendEntries**：Leader 向 Follower 复制日志
- **RequestVote**：Candidate 请求投票
- **InstallSnapshot**：Leader 向落后较多的 Follower 发送完整快照

### Agent

`Zeze.Raft.Agent` 是 Raft 集群的客户端，管理 Leader 连接，自动处理 Leader 切换和请求重发：

```java
var agent = new Agent("MyApp", raftConfig, config);
agent.setOnSetLeader(this::onLeaderChanged);
agent.start();
```

Agent 的关键行为：

- **Leader 跟踪**：维护当前 Leader 的连接，Leader 变更时自动切换
- **请求重发**：发送到旧 Leader 的请求会失败并自动重发到新 Leader
- **Pending 管理**：未完成的请求保存在 pending 队列中，Leader 切换时重新发送

### ProxyAgent

当多个 Raft 实例运行在同一进程内时，可以使用 `ProxyAgent` 共享网络连接。`ProxyAgent` 通过 `ProxyServer` 将请求转发到实际的 Raft 节点，减少连接开销。

## 选举与日志复制

### 选举流程

1. Follower 在 `electionTimeout`（`leaderHeartbeatTimer + 100 + random(0, electionRandomMax)`）内未收到 Leader 心跳
2. 转换为 Candidate，自增 term，向所有节点发送 `RequestVote`
3. 获得多数票后转换为 Leader，开始发送心跳和日志复制

### 日志复制

Leader 通过 `AppendEntries` 将日志复制到所有 Follower：

- `nextIndex`：下一个要发送给该 Follower 的日志索引
- `matchIndex`：已确认复制到该 Follower 的最高日志索引
- `maxAppendEntriesCount`（默认 500）：每次 AppendEntries 最大打包日志数

当日志被复制到多数节点后，Leader 将其标记为已提交（committed），并通过 `apply` 应用到状态机。

## 快照机制

当日志数量超过 `snapshotLogCount`（默认 1,000,000）时，Raft 触发快照：

1. Leader 锁定状态机，将当前状态序列化到文件
2. 记录 `lastIncludedIndex` 和 `lastIncludedTerm`
3. 截断已快照的日志，释放磁盘空间
4. 对于落后较多的 Follower，Leader 通过 `InstallSnapshot` 发送完整快照

`snapshotCommitDelayed` 配置可以在快照后延时提交，用于保护日志不被过早截断。

## RaftConfig 配置

RaftConfig 通过 XML 文件加载，支持以下关键参数：

```xml
<raft Name="127.0.0.1_10004"
      DbHome="raft_data"
      AppendEntriesTimeout="2000"
      LeaderHeartbeatTimer="2200"
      ElectionRandomMax="300"
      MaxAppendEntriesCount="500"
      SnapshotLogCount="1000000"
      BackgroundApplyCount="500"
      UniqueRequestExpiredDays="7">
    <node Host="127.0.0.1" Port="10004" />
    <node Host="127.0.0.1" Port="10005" />
    <node Host="127.0.0.1" Port="10006" />
</raft>
```

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `AppendEntriesTimeout` | 2000 | 日志复制超时（毫秒） |
| `LeaderHeartbeatTimer` | 2200 | 心跳间隔，须大于 AppendEntriesTimeout + 200 |
| `ElectionRandomMax` | 300 | 选举超时随机上限 |
| `MaxAppendEntriesCount` | 500 | 每次复制最大日志数 |
| `SnapshotLogCount` | 1000000 | 触发快照的日志数量阈值 |
| `SnapshotCommitDelayed` | false | 快照后是否延时提交 |
| `BackgroundApplyCount` | 500 | 后台应用日志的批量大小 |
| `UniqueRequestExpiredDays` | 7 | 重复请求检测过期天数 |

## 与其他组件的关系

- **ServiceManager**（[服务发现](./service-manager.md)）：ServiceManagerWithRaft 基于 Raft 实现高可用。
- **GlobalCacheManager**（[缓存同步](./global-cache-manager.md)）：GlobalCacheManagerWithRaft 基于 Raft 实现持久化。

## 参考文档

可阅读 `zeze/Zeze/Raft/` 下的 `Raft.mhtml`、`raft.pdf`、`OngaroPhD.pdf`。

## RaftRpc ResultCode 含义

| ResultCode | 含义 |
|---|---|
| `RaftApplied` | Raft 发现请求是重发的，但已经成功处理过 |
| `RaftExpired` | 请求过期了，无法判断是否被成功处理 |
