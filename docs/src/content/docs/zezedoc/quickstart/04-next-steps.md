---
title: "接下来读什么"
description: "学完 Quickstart 后的学习路径指引"
category: quickstart
order: 4
---

# 接下来读什么

> 这篇帮你规划下一步，根据你的目标挑最近的入口。

你已经能定义数据、生成代码、写业务逻辑，也见识了事务自动回滚的威力。接下来往哪走，取决于你想解决什么问题。

## 我想先搞懂「为什么」

强烈建议至少读这一篇：**[Zeze 解决的三大痛点](../manual/01-the-pain.md)**。它会用转账、加好友、死锁这些具体场景，把 Zeze 存在的根本理由讲透。读完你会清楚知道哪些项目该用 Zeze、哪些不该。

然后读 **[Zeze 如何工作](../manual/02-how-zeze-works.md)**，建立心智模型：一致性缓存、乐观锁、存储过程，以及一个绝妙的类比——Zeze 之于服务器，就像 CPU 缓存之于内存。

## 我想系统地学会用

按顺序读 **Manual 指南**（8 篇），这是本套文档的阅读主体：

1. [三大痛点](../manual/01-the-pain.md) — 核心动机
2. [如何工作](../manual/02-how-zeze-works.md) — 心智模型
3. [定义数据](../manual/03-defining-data.md) — Bean、Table、XML
4. [编写业务逻辑](../manual/04-writing-logic.md) — 存储过程、回滚、副作用
5. [走向分布式](../manual/05-going-distributed.md) — Provider-Linkd、全球同服
6. [选配数据库](../manual/06-choosing-database.md) — 各种后端的取舍
7. [不懂多线程也能写并发程序](../manual/07-multithreading-without-fear.md)
8. [上线清单](../manual/08-production-checklist.md) — 配置、容量、过载、停服

## 我要查具体 API 或配置

直奔 **[Reference 详细参考](../reference/index.md)**，按主题查：

- 写 XML 时 → [solution.xml 参考](../reference/solution-xml.md)
- 用事务时 → [事务参考](../reference/transaction.md)
- 读写数据时 → [Table 参考](../reference/table.md)
- 配服务器时 → [配置参考](../reference/configuration.md)
- 搭分布式时 → [Provider-Linkd 架构](../reference/arch-provider-linkd.md)

## 我做游戏

- [游戏模块总览](../reference/game-overview.md) — Online、背包、排行榜
- [Online 在线管理](../reference/arch-online.md) — 登录登出、可靠消息、跨服转发
- [Timer 定时器](../reference/svc-timer.md) — 角色/离线定时器

## 我接客户端

- [C# / Unity 客户端](../reference/client-csharp.md)
- [C++ 客户端](../reference/client-cpp.md)
- [TypeScript 客户端](../reference/client-typescript.md)

---

> **小建议**：Manual 的前两篇（痛点 + 工作原理）是理解 Zeze 的钥匙，无论你之后做什么方向，都值得花半小时读完。Reference 是工具书，用到再查即可。
