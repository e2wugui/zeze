---
title: "关系型数据库（MySQL/PostgreSQL）"
description: "Zeze 通过 JDBC 接入 MySQL 与 PostgreSQL，支持 KV 模式与关系映射模式"
category: reference
order: 21
---

> 本文档说明 Zeze 如何通过 JDBC 接入 MySQL 与 PostgreSQL，覆盖两种数据组织模式（KV 模式 / 关系映射模式）、连接 URL 示例、Druid 连接池配置与依赖坐标，供需要 SQL 运维能力或跨表关系查询的场景参考。

## 支持的实现

| 实现 | 接入方式 | KV 模式 | 关系映射模式 |
|------|----------|---------|--------------|
| `DatabaseMySql` | JDBC | ✅ | ✅ |
| `DatabasePostgreSQL` | JDBC | ✅ | ✅ |
| `DatabaseSqlServer` | JDBC | ✅ | ❌（仅 KV，需自备 mssql 驱动） |

## 两种数据组织模式

### 1. KV 模式

key 与 value 均为 `ByteBuffer` 二进制，**所有数据库通用**。表结构为一张 `(key 列, value 列)` 的两列表。

| 维度 | 说明 |
|------|------|
| key | `ByteBuffer` 二进制 |
| value | `ByteBuffer` 二进制 |
| 表结构 | `(key, value)` 两列 |
| 适用 | 所有库通用，无需建索引、无需 SQL |

### 2. 关系映射模式（RelationalMapping）

Bean 字段映射为表列，支持**按字段建索引**与 SQL 查询。仅 MySQL / PostgreSQL 实现，需实现 `DatabaseRelationalMapping` 接口的 `openRelationalTable(table)`。

| 维度 | 说明 |
|------|------|
| 映射方式 | Bean 字段 → 表列 |
| key / value | `SQLStatement` |
| 索引 | 支持按字段建索引 |
| 查询 | 支持 SQL 查询 |
| 适用范围 | 仅 MySQL / PostgreSQL |

## 启用关系映射

在 table 上启用：

```xml
<table name="demo_Module1_Account" RelationalMapping="true"/>
```

或在 bean 上启用，让生成的代码包含关系映射类：

```xml
<bean name="Account" MappingClass="true"/>
```

## JDBC 配置

### MySQL

```xml
<DatabaseConf Name="default"
              DatabaseType="MySql"
              DatabaseUrl="jdbc:mysql://localhost:3306/zeze?user=root&amp;password=123456&amp;useSSL=false&amp;serverTimezone=UTC"/>
```

### PostgreSQL

```xml
<DatabaseConf Name="default"
              DatabaseType="PostgreSQL"
              DatabaseUrl="jdbc:postgresql://localhost:5432/devtest?user=dev&amp;password=devtest12345"/>
```

## Druid 连接池配置

JDBC 数据库支持 Druid 连接池。**没有 `<DruidConf>` 子元素**——Druid 的属性直接挂在 `<DatabaseConf>` 元素上：

```xml
<DatabaseConf Name="default"
              DatabaseType="MySql"
              DatabaseUrl="jdbc:mysql://..."
              DriverClassName="com.mysql.cj.jdbc.Driver"
              UserName="root"
              Password="123456"
              InitialSize="5"
              MinIdle="5"
              MaxActive="20"
              MaxWait="60000"
              MaxOpenPreparedStatements="20"
              PhyMaxUseCount="10000"
              PhyTimeoutMillis="600000"/>
```

| 属性 | 说明 |
|------|------|
| `DriverClassName` | JDBC 驱动类名 |
| `UserName` | 数据库用户名 |
| `Password` | 数据库密码 |
| `InitialSize` | 初始连接数 |
| `MinIdle` | 最小空闲连接 |
| `MaxActive` | 最大活跃连接 |
| `MaxWait` | 获取连接最大等待（毫秒） |
| `MaxOpenPreparedStatements` | 最大打开的 PreparedStatement 数 |
| `PhyMaxUseCount` | 物理连接最大使用次数 |
| `PhyTimeoutMillis` | 物理连接超时（毫秒） |

## 依赖坐标

| 依赖 | 版本 | 用途 |
|------|------|------|
| `com.mysql:mysql-connector-j` | `8.4.0` | MySQL JDBC 驱动 |
| `org.postgresql:postgresql` | `42.7.11` | PostgreSQL JDBC 驱动 |
| `com.alibaba:druid` | `1.2.28` | Druid 连接池 |

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.4.0</version>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.11</version>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.28</version>
</dependency>
```

## 适用场景

| 场景 | 选择 |
|------|------|
| 需要 SQL 运维能力（手工查数、报表） | 关系映射模式 |
| 跨表关系查询 | 关系映射模式 |
| 已有 MySQL 运维团队，要求工具链成熟 | MySQL |
| 已有 PostgreSQL 运维团队 | PostgreSQL |

## 相关文档

- 数据库总览：[./db-overview.md](./db-overview.md)
- 配置参考：[./configuration.md](./configuration.md)
