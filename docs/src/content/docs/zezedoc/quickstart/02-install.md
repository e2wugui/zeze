---
title: "环境搭建"
description: "JDK、代码生成工具、Maven 依赖、IDE 配置，零基础起步"
category: quickstart
order: 2
---

# 环境搭建

> 读完这篇，你的机器就具备开发 Zeze 应用的全部条件。

搭建 Zeze 开发环境需要四样东西：**JDK**、**代码生成器**（一个 .NET 工具）、**构建工具**（Maven 或 Gradle）和 **IDE**（推荐 IntelliJ IDEA）。

## 1. 安装 JDK

Zeze 的编译目标是 **Java 21**（`sourceCompatibility` / `targetCompatibility` 均为 21），因此需要 **JDK 21 及以上**：

- 推荐：[Adoptium Temurin JDK 21](https://adoptium.net/zh-CN/temurin/archive/?version=21)
- 备选：[Oracle OpenJDK 21](https://jdk.java.net/21/)

安装后验证：

```bash
java -version
```

## 2. 用现成模板起步（强烈推荐）

官方脚手架 **zezeboot** 已经预配好了项目结构、`solution.xml` 模板和构建脚本，是最省事的起点：

```bash
git clone https://gitee.com/dwing/zezeboot.git
```

下面的步骤如果你用 zezeboot，大部分已经替你做好了。

## 3. 安装代码生成器

Zeze 的核心工作流是「**XML 定义数据 → 生成代码 → 写业务**」，生成器是一个基于 **.NET 10** 的工具 `Gen.exe`。

**装 .NET 10 SDK**：下载地址 <https://dotnet.microsoft.com/download/dotnet/10.0>

**编译生成器**（在 Zeze 源码根目录）：

```bash
dotnet build Zeze.slnx
# 或只编译 Gen 工程
dotnet build Gen
```

编译成功后，可执行文件在 `Gen/bin/Debug/net10.0/Gen.exe`。

> 生成器是跨项目通用的，编一次到处用。后续生成代码只需在含 `solution.xml` 的目录下执行 `Gen.exe solution.xml`。

## 4. 引入 Zeze 依赖

Zeze 以 `com.zezeno:zeze-java` 发布到 Maven 中央仓库。

### Maven

```xml
<dependency>
    <groupId>com.zezeno</groupId>
    <artifactId>zeze-java</artifactId>
    <version>1.7.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
dependencies {
    implementation 'com.zezeno:zeze-java:1.7.0-SNAPSHOT'
}
```

如果你的项目和 Zeze 源码在同一个多模块构建里，也可以直接引用子项目：

```groovy
dependencies {
    implementation project(":ZezeJava")
}
```

> **注意 provided scope**：Zeze 的大部分第三方依赖声明为 `provided`（Gradle 里是 `compileOnly`），不会传递到你的项目。你用了哪个功能（哪个数据库、Netty 等），就自己引入对应依赖。常见依赖见下表，完整列表参见 [配置参考](../reference/configuration.md)。

| 功能 | Maven 坐标 |
|------|-----------|
| 网络层 | `io.netty:netty-codec-http:4.1.135.Final` |
| 日志 | `org.slf4j:slf4j-api:2.0.18` |
| MySQL | `com.mysql:mysql-connector-j:8.4.0` |
| PostgreSQL | `org.postgresql:postgresql:42.7.11` |
| 连接池 | `com.alibaba:druid:1.2.28` |
| MongoDB | `org.mongodb:mongodb-driver-sync:5.8.0` |
| Redis | `redis.clients:jedis:5.2.0` |
| RocksDB | `org.rocksdb:rocksdbjni:10.10.1.1` |
| TiKV | `org.tikv:tikv-client-java:3.3.5` |

例如用 MySQL + Netty 时的 Maven 配置：

```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-codec-http</artifactId>
    <version>4.1.135.Final</version>
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

## 5. 配置 IDE

推荐 **IntelliJ IDEA**（社区版即可，2023.3 以上）：

1. `File → Open`，选中项目根目录或 `ZezeJava` 目录
2. IDEA 自动识别 Maven / Gradle 项目并导入
3. `Project Structure → Project → SDK` 选 JDK 21 或更高版本
4. 如果编译时报"找不到类"，多半是还没跑过代码生成——先执行 `Gen.exe solution.xml`

## 6. 跑一次代码生成（确认工具链通）

在含 `solution.xml` 的目录下：

```bash
# 生成服务端代码
/path/to/Gen.exe solution.xml

# 客户端、Linkd 各有自己的 solution 文件
/path/to/Gen.exe solution.client.xml
/path/to/Gen.exe solution.linkd.xml
```

Windows 下可直接用项目里的 `gen.bat`。生成的代码默认放在 `Gen/` 目录，**不要手动编辑**——下次生成会覆盖。

---

环境就绪了，下一篇我们用 5 分钟搭一个能跑的应用：[第一个应用](./03-first-app.md)。
