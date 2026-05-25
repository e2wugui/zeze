---
title: "RocksDB 嵌入式引擎"
sidebar:
  order: 4
---

RocksDB 是 Facebook（Meta）开源的高性能嵌入式 KV 存储引擎，基于 LevelDB 演进而来。Zeze 将 RocksDB 作为核心存储后端之一，同时也是内部组件（如 Dbh2、Checkpoint 等）的基础依赖。

## 特点

- **嵌入式设计**: 作为库直接嵌入应用进程，无需独立部署数据库服务，消除网络开销。
- **高性能本地存储**: 基于 LSM-Tree（Log-Structured Merge-Tree）架构，写入性能优异，适合写密集场景。
- **ACID 事务**: 通过 WriteBatch 实现原子写入，保证数据一致性。
- **多表支持**: 单个 RocksDB 实例通过 Column Family 机制支持多张逻辑表。
- **全功能遍历**: 支持正序、逆序以及分页遍历，是所有数据库适配器中遍历功能最完整的实现。

## 配置方式

```xml
<DatabaseConf Name=""
    DatabaseType="RocksDb"
    DatabaseUrl="db"/>
```

- **DatabaseUrl**: 数据存储目录路径。如果为空，默认使用 `"db"`。路径相对于应用工作目录。
- Zeze 还会为每个命名的 DatabaseConf 创建独立的 RocksDB 实例。

### 配置示例

单数据库配置：

```xml
<zeze>
    <DatabaseConf Name="" DatabaseType="RocksDb" DatabaseUrl="db"/>
</zeze>
```

多数据库配置（不同数据库使用不同目录）：

```xml
<zeze>
    <DatabaseConf Name="hot" DatabaseType="RocksDb" DatabaseUrl="db_hot"/>
    <DatabaseConf Name="cold" DatabaseType="RocksDb" DatabaseUrl="db_cold"/>
</zeze>
```

## 内部实现

### 表与事务

- 每张 Zeze 表通过 `RocksDatabase.Table`（基于 Column Family）实现，使用 `getOrAddTable` 方法按需创建。
- 事务通过 `RocksDbTrans` 实现，内部使用 `WriteBatch` 进行批量原子写入。提交时可选加锁以保证测试场景下的严格串行化。
- 支持批量打开多张表（`openTables`），减少锁竞争。

### Schema 版本管理

- 使用独立表 `Zeze_OperatesRocksDb_Schemas` 存储版本数据。
- 由于 RocksDB 是独占式访问，`setInUse`/`clearInUse` 操作由进程打开时自动保证，无需额外逻辑。

## 多平台 JNI 依赖

RocksDB 通过 JNI 调用原生 C++ 库。Zeze 的 Maven 依赖中包含了以下平台的原生库：

| 平台 | 架构 | 说明 |
|------|------|------|
| Windows | x86_64 | `win64` |
| Linux | x86_64 | `linux64` |
| macOS | x86_64 | `osx` |

在 Maven 中通过 `rocksdbjni` 依赖自动引入，无需手动安装。

## 性能调优建议

1. **存储路径**: 将 `DatabaseUrl` 指向 SSD 或 NVMe 磁盘，充分发挥 RocksDB 的写入性能。
2. **WriteBatch 批量提交**: 尽量在事务中合并多次写操作，减少提交次数。
3. **定期 Compact**: 对于大量删除操作后，建议触发 `compact` 回收空间，避免读取性能下降。
4. **WAL 同步**: 默认使用异步写入（`WriteOptions` 不带 sync）。如需更高持久性保证，可启用同步写入（`getSyncWriteOptions`），但会降低写入吞吐。
5. **连接池大小**: Zeze 使用 Batch 对象池（`borrowBatch`）复用 WriteBatch，减少 GC 压力。

## 适用场景

| 场景 | 说明 |
|------|------|
| 单机开发/测试 | 无需外部数据库依赖，零配置启动 |
| 本地缓存数据库 | 作为 Zeze 全局缓存管理的本地存储后端 |
| 嵌入式部署 | 不允许部署独立数据库服务的环境 |
| Dbh2 底层存储 | Dbh2 分布式数据库的每个 Bucket 内部使用 RocksDB |
| Checkpoint 存储 | Zeze 事务检查点数据的持久化 |

> **注意**: RocksDB 实例为独占式打开，同一目录同一时间只能被一个进程访问。多个 Zeze 实例需使用不同的 `DatabaseUrl` 目录。
