---
title: "关系型数据库"
sidebar:
  order: 2
---

Zeze 通过 JDBC 支持 MySQL 和 PostgreSQL，提供两种存储模式：**KV 模式**（二进制序列化）和 **关系映射模式**（Bean 字段映射为表列）。两种模式可在同一个数据库实例中混合使用。

## 架构层次

```
DatabaseJdbc (抽象基类，封装 Druid 连接池)
  +-- DatabaseMySql       (实现 DatabaseRelationalMapping)
  +-- DatabasePostgreSQL   (实现 DatabaseRelationalMapping)
```

## 连接池配置（Druid）

Zeze 使用阿里巴巴 Druid 作为 JDBC 连接池，参数通过 `<DatabaseConf>` XML 属性配置：

```xml
<DatabaseConf DatabaseType="MySql"
  DatabaseUrl="jdbc:mysql://localhost:3306/zeze?user=root&amp;password=123456&amp;useSSL=false&amp;serverTimezone=UTC"
  DriverClassName="com.mysql.cj.jdbc.Driver"
  UserName="root" Password="123456"
  InitialSize="4" MinIdle="4" MaxActive="16"
  MaxOpenPreparedStatements="256" />
```

| 属性 | 默认值 | 说明 |
|---|---|---|
| `DriverClassName` | 自动检测 | MySQL: `com.mysql.cj.jdbc.Driver`，PostgreSQL: `org.postgresql.Driver` |
| `UserName` / `Password` | null | 也可包含在 URL 中 |
| `InitialSize` | 4 | 初始连接数 |
| `MinIdle` | 4 | 最小空闲连接数 |
| `MaxActive` | 8 | 最大活跃连接数 |
| `MaxWait` | -1（无限） | 获取连接超时（毫秒） |
| `MaxOpenPreparedStatements` | 256 | PreparedStatement 缓存数量 |

框架强制启用 `PoolPreparedStatements` 和 `KillWhenSocketReadTimeout`。

## KV 模式

每张表创建为两列表（`id` + `value`），key/value 均为 Zeze 二进制序列化后的字节数组：

```sql
-- MySQL
CREATE TABLE IF NOT EXISTS t (id VARBINARY(2712) PRIMARY KEY, value LONGBLOB NOT NULL);
-- PostgreSQL
CREATE TABLE IF NOT EXISTS t (id BYTEA PRIMARY KEY, value BYTEA NOT NULL);
```

| 操作 | MySQL | PostgreSQL |
|---|---|---|
| 替换 | `REPLACE t VALUE(?,?)` | `INSERT INTO t VALUES(?,?) ON CONFLICT(id) DO UPDATE SET value=EXCLUDED.value` |

## 关系映射模式（RelationalMapping）

将 Bean 字段映射为关系表列，数据可通过 SQL 直接查询和索引。通过 `DatabaseRelationalMapping` 接口实现。`Storage` 构造时自动检测并选择映射模式。

### 字段类型映射

| Zeze 类型 | MySQL | PostgreSQL |
|---|---|---|
| `bool` | `BOOL` | `boolean` |
| `byte` / `short` | `TINYINT` / `SMALLINT` | `smallint` |
| `int` | `INT` | `integer` |
| `long` | `BIGINT` | `bigint` |
| `float` / `double` | `FLOAT` / `DOUBLE` | `real` / `double precision` |
| `binary` | `BLOB` | `bytea` |
| `string` / 集合类型 | `TEXT` | `text` |

### 自动建表与结构迁移

打开表时自动执行 `CREATE TABLE IF NOT EXISTS`。Bean 定义变化后框架自动执行 `ALTER TABLE`（MySQL 用 `ADD/DROP/CHANGE COLUMN`，PostgreSQL 需分步执行类型修改和重命名，并重建主键）。

## 实例管理存储过程

首次连接自动创建：`_ZezeDataWithVersion_`（版本化数据）、`_ZezeInstances_`（活跃实例）表及 `_ZezeSaveDataWithSameVersion_`、`_ZezeSetInUse_`、`_ZezeClearInUse_` 存储过程。`Operates` 还提供 `tryLock()`/`unlock()` 实现基于数据库的分布式锁。

## 直接 SQL 查询

```java
database.relationalSql("SELECT * FROM my_table WHERE status = 1", (rs) -> {
    String name = rs.getString("name");
});
```

## XML 配置示例

```xml
<!-- MySQL -->
<zeze ServerId="1" CheckpointPeriod="60000">
  <DatabaseConf DatabaseType="MySql"
    DatabaseUrl="jdbc:mysql://db-master:3306/zeze?useSSL=false&amp;serverTimezone=Asia/Shanghai"
    UserName="zeze" Password="pwd"
    InitialSize="8" MinIdle="8" MaxActive="32" />
  <TableConf CacheCapacity="20000" />
</zeze>

<!-- PostgreSQL -->
<zeze ServerId="1">
  <DatabaseConf DatabaseType="PostgreSQL"
    DatabaseUrl="jdbc:postgresql://pg-server:5432/zeze"
    UserName="postgres" Password="pwd"
    InitialSize="4" MinIdle="4" MaxActive="16" />
</zeze>

<!-- 禁用实例管理（只读副本） -->
<DatabaseConf Name="readonly" DatabaseType="MySql"
  DatabaseUrl="jdbc:mysql://replica:3306/zeze?user=reader&amp;password=pwd"
  DisableOperates="true" />
```

## 性能建议

1. **连接池**：`MaxActive` 应根据数据库 `max_connections` 和应用并发量设置，MySQL 默认上限 151。
2. **PreparedStatement 缓存**：默认 256 条，表数量多时可适当增大。
3. **模式选择**：不需要 SQL 查询的表优先用 KV 模式，关系映射有额外的编解码开销。
4. **Key 长度**：统一上限 2712 字节（取 PostgreSQL 限制）。
5. **Checkpoint**：默认 60 秒间隔，根据持久性要求调整。
6. **热点分离**：热点表可配置到 Redis，冷数据留在关系数据库，通过多数据库混合配置实现。
