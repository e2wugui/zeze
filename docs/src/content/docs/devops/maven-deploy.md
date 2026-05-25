---
title: "Maven 中央仓库发布"
sidebar:
  order: 2
---

Zeze Java 框架通过 Maven 中央仓库（Maven Central）分发。本文介绍从环境准备到完成发布的完整流程，包括 Sonatype 账号配置、GPG 签名和 `central-publishing-maven-plugin` 的使用。

## 前置准备

### 1. 注册 Sonatype 账号

前往 [central.sonatype.com](https://central.sonatype.com/) 注册账号（推荐使用 GitHub 账号认证）。完成以下步骤：

- 为发布包的**域名**做 TXT 记录认证（设置后可用 `nslookup -type=TXT 域名` 验证）。
- 获取 **上传 Token**（User Token）。

### 2. 配置 Maven settings.xml

在 `%USERPROFILE%\.m2\settings.xml` 中配置 Sonatype 上传 Token：

```xml
<servers>
  <server>
    <id>central</id>
    <username><!-- token username --></username>
    <password><!-- token password --></password>
  </server>
</servers>
```

> `<id>` 必须与 `pom.xml` 中 `central-publishing-maven-plugin` 的 `publishingServerId` 一致。

### 3. 安装 GPG 并生成密钥

从 [gpg4win.org](https://www.gpg4win.org/) 下载安装 GPG，生成密钥对并上传到公钥服务器：

```bash
gpg --full-generate-key
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

## pom.xml 配置说明

### pom.xml 关键配置

当前坐标：`com.zezeno:zeze-java:1.6.3-SNAPSHOT`。

**distributionManagement** 定义发布目标仓库，正式版本发布到 Sonatype 中央仓库：

```xml
<distributionManagement>
  <snapshotRepository>
    <id>snapshots</id>
    <url>http://10.12.7.230:9081/repository/maven-snapshots/</url>
  </snapshotRepository>
  <repository>
    <id>central</id>
    <name>central-releases</name>
    <url>https://s01.oss.sonatype.org/</url>
  </repository>
</distributionManagement>
```

**GPG 签名插件**（`maven-gpg-plugin:3.2.7`）在 `verify` 阶段对构件签名。

**central-publishing-maven-plugin**（`0.6.0`）替代旧版 `nexus-staging-maven-plugin`：

```xml
<plugin>
  <groupId>org.sonatype.central</groupId>
  <artifactId>central-publishing-maven-plugin</artifactId>
  <version>0.6.0</version>
  <extensions>true</extensions>
  <configuration>
    <publishingServerId>central</publishingServerId>
    <autoPublish>false</autoPublish>
  </configuration>
</plugin>
```

| 参数 | 说明 |
|------|------|
| `publishingServerId` | 对应 `settings.xml` 中的 server id |
| `autoPublish` | 设为 `false` 需手动在网站确认发布 |

中央仓库还要求发布**源码包**（`maven-source-plugin:3.3.1`）和 **Javadoc 包**（`maven-javadoc-plugin:3.10.1`）。

## 发布流程

### 执行部署

```bash
cd ZezeJava/ZezeJava
mvn clean deploy
```

该命令依次执行：编译 -> 测试 -> GPG 签名 -> 打包源码/Javadoc -> 上传到 Sonatype。

### 手动发布

部署完成后，登录 [central.sonatype.com](https://central.sonatype.com/)：

1. 找到刚上传的 Deployment。
2. 检查构件内容无误后点击 **Publish**。
3. 发布后约 10 分钟内可在 Maven Central 搜索到。

## 版本管理

- **SNAPSHOT** 版本（如 `1.6.3-SNAPSHOT`）用于开发，发布到 `snapshotRepository`。
- **正式版本** 去除 `-SNAPSHOT` 后缀后发布到中央仓库。
- 发布后的版本不可修改，如需修正需升版本号重新发布。

## 参考

- [Sonatype 官方发布指南](https://central.sonatype.com/publish)
- [Maven GPG Plugin](https://maven.apache.org/plugins/maven-gpg-plugin/)
- [central-publishing-maven-plugin](https://central.sonatype.com/publish/publish-guide/#maven)
