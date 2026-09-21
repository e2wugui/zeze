# 20 轮压测结案报告 — HEAD c6549c784（2026-09-21 22:24 → 09-22 04:16）

单轮 ~1010-1200s。**test 池近干净（11/20 轮，全部已知族+3 孤例）；integrationTest 20/20 恒定 9 红 + bench 20/20 恒定 1 红 = 单一确定性根因。**
（本报告为交接文档，修复方向由 stallboy 裁定；用户裁定本批不动代码。）

## 一、确定性根因：878351fd4 冷抢载守卫 × Zezex 测试环境错配

### 因果链

```
878351fd4 HotModule 新增 fail-fast：
  HotModule 构造器 loadClass(模块类) 走双亲委派，命中冷 classpath 同名类
  （moduleClass.getClassLoader() != this）即抛
  IllegalStateException: hot module shadowed by cold classpath
    （HotModule.java:49-51，守卫设计正确：冷身份会让热更静默失效）

× ZezeJavaTest/server.xml:14  HotWorkingDir="../ZezexJava/server/hot"
    → hot/modules/ 下 8 个模块 jar（Game.Buf/Login/Item/Fight/Skill/Equip/
      Timer/LongSet.jar，文件日期 2026-01-08）被 HotManager 构造器
      loadExistModules 在每次 Game.App.Start 装载

× 这 8 个模块的源同时在 ZezexJava/server/src/Game/**（编译进
    integrationTest/bench 的 runtime classpath）

→ Game.App.Start → createModules → new HotManager → new HotModule(Game.Buf)
  → loadClass 命中 AppClassLoader 的冷类 → 必抛，每轮 100%
```

### 失败分布（每轮同影像）

| 类 | 方法 | 错误 | 性质 |
|---|---|---|---|
| ModuleRedirectRank | testRedirect | hot module shadowed | 直接红 |
| TestGameTimer | testRoleTimer1 | hot module shadowed | 直接红 |
| TestGameTimer | testRoleTimer2 | App Not Start: eUninitialized | 直接红次生 |
| TestOnline | test3 | bind 0.0.0.0:12000 | **级联红** |
| TestOnlineSpec | testOnlineSpec | bind 0.0.0.0:12000 | **级联红** |
| TestRoleTimer | ×4 方法 | bind 0.0.0.0:12000 | **级联红** |
| bench: BenchRoleTimer | testBenchmark | App Not Start: eUninitialized | 同根（bench JVM） |

### 级联机制（1 红变 7 红）

五类 XML 时间戳显示 1.7s 内连败（ModuleRedirectRank 14:39:32.3 → TestRoleTimer 14:39:33.99，round1）：

1. ModuleRedirectRank / TestGameTimer 的 `prepareNewEnvironment` 在 `servers.get(i).Start()` 抛出——但 **links（linkd，12000+i）已先行 Start**（ZezexTestEnv.java:83-86，link 先 server 后）；
2. TestGameTimer 类级共享 env 的 `stopAll()`（ZezexTestEnv.java:156-186）：servers 循环 `stopBeforeModules()` 在半启动 app 上抛 "App Not Start: eUninitialized"，**循环无 try/catch → links 循环（:168-174）没跑到 → linkd 12000 泄漏**（finally 只释放了 LoginQueue 5020/5021）；
3. 同 JVM 后续所有类 prepareNewEnvironment 在 `links.get(0).Start` 即 bind 失败，秒败。

### 关键佐证

- **提交自述验证只跑了 `:ZezeJavaTest:test`（fast 池）——Zezex 类全部不带 @Fast，只在 integrationTest/bench 跑**，恰好是验证盲区；
- jar 日期 2026-01-08：该环境的热模块至少从 1 月起一直是"冷身份静默运行"（守卫抓到的正是这个潜伏错配，属于守卫的设计目的，不是守卫误报）；
- 同批 881e69e43（上批四修）在 test 池 0 复发，可排除干扰。

### 修复方向（供裁定）

| 方案 | 内容 | 代价/风险 |
|---|---|---|
| A. src-hot 迁移（正解） | 按 878351fd4 对 ZezeJavaTest Temp demo 的同款模式，ZezexJava/server 8 个热模块源迁独立源集，不进冷 classpath | 工程量大：8 模块+gen.hot 管线+interfaces；但从此热模块在测试里是真热身份 |
| B. 守卫加开关 | HotModule 守卫加 system property 降级 warn | 改产品码、与提交 fail-fast 意图冲突 |
| C. 测试环境清 jar | 不可行——App 的热模块经 HotManager.initialize 注册，无 jar 即缺模块，App 起不来 | — |
| 独立小修（与 A/B 正交） | ZezexTestEnv：prepareNewEnvironment 失败路径停已启动 links；stopAll servers 循环逐个 try/catch 保证 links 释放 | 测试基建加固，防"1 红变 7 红"，建议无论如何都做 |

## 二、test 池（11/20 轮，全部已知族或孤例）

- GCM 饥饿族（AcquirePendingReset/ReleaseRemovedReset setUp 300s）×14 — 声明已知负载族
- testWebSocket ×2（r3/r10）— 已知 JDK
- Fnd770DeadlockBreaker r3 ×1 — 已知 escape-race 族的 parked-state 断言点
- **Fnd736RaftTableLruLifecycle r8 ×1 孤例**：`expected:<3> but was:<4>`——driveRotation 观测到 nodes1 后 reset，恰有 1 个在途轮转任务迟到入队；断言未容忍取消竞态（与 H2AbortedStreamCleanup 释放瞬态同族假红）。与 FlushSet 无关
- **Fnd849ConnectorRestartDuringStopWindow r16 ×1 孤例**：resolve 失败窗 socket 未闭，观察项

## 三、修复验证状态

- ✅ 上批（e81e42537）四修 0 复发：dbHome 预清理、异步建连同步点、H2 释放等待（test 池层面）
- ✅ FlushSet onz 聚集补发（85b9b35b6）盯点零红
- ⚠️ **246ada19d（LoginQueueClient start 复位，上批 TestOnline.test3 修复）本批零覆盖**——test3 在到达登录流程前就死于 bind 级联，仍属未验证
- ⚠️ Zezex 全族（Online/OnlineSpec/RoleTimer/GameTimer/ws 客户端路径）本批实质未跑

证据：results.txt / full.log / results_r{N}_{task}/（失败轮全量 XML），上批归档 prev-e81e42537/。
