---
title: "环境搭建"
sidebar:
  order: 0
---

本文介绍搭建 Zeze 开发环境所需的工具、依赖引入方式以及代码生成工具的使用，帮助你快速做好开发准备。

## JDK 要求

Zeze 要求 **JDK 11** 及以上版本。框架本身的 Maven 编译目标为 Java 11，但 Gradle 构建配置使用 Java 21。推荐使用 JDK 21：

- 推荐：[Adoptium Temurin JDK 21](https://adoptium.net/zh-CN/temurin/archive/?version=21)
- 备选：[Oracle OpenJDK 21](https://jdk.java.net/21/)

安装完成后，验证 Java 版本：

```bash
java -version
```

## 初始项目模板

推荐使用官方脚手架 **zezeboot** 快速创建项目：

- 仓库地址：[https://gitee.com/dwing/zezeboot](https://gitee.com/dwing/zezeboot)

```bash
git clone https://gitee.com/dwing/zezeboot.git
```

zezeboot 包含预配置的项目结构、solution.xml 模板和构建脚本，可作为开发的起点。

## 引入 Zeze 依赖

Zeze 以 `com.zezeno:zeze-java` 坐标发布到 Maven 中央仓库，可作为普通库依赖引入。

### Maven

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.zezeno</groupId>
    <artifactId>zeze-java</artifactId>
    <version>1.6.3-SNAPSHOT</version>
</dependency>
```

### Gradle

在 `build.gradle` 中添加：

```groovy
dependencies {
    implementation 'com.zezeno:zeze-java:1.6.3-SNAPSHOT'
}
```

如果项目与 Zeze 源码在同一多模块构建中，可直接引用子项目：

```groovy
dependencies {
    implementation project(":ZezeJava")
}
```

## 核心依赖说明

Zeze 的大部分第三方依赖声明为 **provided** scope（Gradle 中为 `compileOnly`），不会传递到你的项目中。应用需要根据实际使用的功能，自行引入对应的依赖。以下列出常见的可选依赖：

| 功能 | Maven 坐标 | 说明 |
|------|-----------|------|
| 网络层 | `io.netty:netty-codec-http:4.1.132.Final` | 基于 Netty 的网络通信 |
| 日志 | `org.slf4j:slf4j-api:2.0.17` | SLF4J 日志门面 |
| MySQL | `com.mysql:mysql-connector-j:8.4.0` | MySQL 数据库驱动 |
| PostgreSQL | `org.postgresql:postgresql:42.7.10` | PostgreSQL 数据库驱动 |
| 连接池 | `com.alibaba:druid:1.2.28` | Druid JDBC 连接池 |
| MongoDB | `org.mongodb:mongodb-driver-sync:5.6.5` | MongoDB 驱动 |
| Redis | `redis.clients:jedis:5.2.0` | Jedis 客户端 |
| TiKV | `org.tikv:tikv-client-java:3.3.5` | TiKV 客户端 |
| RocksDB | `org.rocksdb:rocksdbjni:10.10.1.1` | 嵌入式 KV 存储 |
| DynamoDB | `com.amazonaws:aws-java-sdk-dynamodb:1.12.797` | AWS DynamoDB |
| FoundationDB | `org.foundationdb:fdb-java:7.4.6` | FoundationDB |

例如，使用 MySQL 数据库和 Netty 网络层时，Maven 配置如下：

```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-codec-http</artifactId>
    <version>4.1.132.Final</version>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.28</version>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.4.0</version>
    <scope>runtime</scope>
</dependency>
```

## 代码生成工具

Zeze 使用 **Gen.exe** 代码生成工具，根据 → solution-xml 定义生成 Bean、Table、Protocol、Rpc 等代码。该工具基于 .NET 8 构建。

### 安装 .NET SDK

代码生成工具需要 **.NET 8 SDK**：

- 下载地址：[https://dotnet.microsoft.com/download/dotnet/8.0](https://dotnet.microsoft.com/download/dotnet/8.0)

### 编译 Gen 工具

在 Zeze 源码根目录下编译：

```bash
dotnet build Zeze.sln
```

编译成功后，`Gen/bin/Debug/net8.0/Gen.exe` 即为可用的代码生成工具。

### 运行代码生成

在包含 `solution.xml` 的目录下执行：

```bash
# 生成服务端代码
../../Gen/bin/Debug/net8.0/Gen.exe solution.xml

# 生成客户端代码
../../Gen/bin/Debug/net8.0/Gen.exe solution.client.xml

# 生成 Linkd 代码
../../Gen/bin/Debug/net8.0/Gen.exe solution.linkd.xml
```

Windows 下可直接运行 `gen.bat` 脚本。生成的代码默认放在 `Gen/` 目录中，不应手动编辑。

## IDE 配置（IntelliJ IDEA）

推荐使用 **IntelliJ IDEA**（社区版即可，2023.3 以上）进行 Java 开发。

1. 打开 IDEA，选择 `File -> Open`，选中 Zeze 源码中的 `ZezeJava` 目录
2. IDEA 会自动识别 Gradle 或 Maven 项目结构并导入
3. 项目包含 4 个子模块：`ZezeJava`（核心框架）、`ZezeJavaTest`（测试）、`ZezexJava:client`、`ZezexJava:server`、`ZezexJava:linkd`
4. 如果编译时提示找不到某些类，需要先执行代码生成（见上节）
5. 配置 `Project Structure -> Project -> SDK`，选择 JDK 11 或更高版本

更多开发指导请参考 → quick-start。
