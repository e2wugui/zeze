# ZezeJava形式验证

本目录包含事务、回滚、锁、Record生命周期、Checkpoint与恢复、GCM权限及交接组合的TLA+有限模型、检查配置和复现脚本。Java对照测试位于 `ZezeJavaTest/src/`。

范围与前提以各报告为准。约定encode只序列化、不执行newProcedure；不建模该约定外行为，也不为其增加生产保护。TLC有限检查、负对照、可达性见证、源码条件论证和真实Java测试分别登记；不把有限模型通过称为全框架机械证明。

| 目录 | 验证对象 |
|---|---|
| [serializability](serializability/README.md) | 单JVM事务提交历史的可串行化 |
| [rollback](rollback/README.md) | 保存点、回滚与重试 |
| [locks](locks/README.md) | 锁序、资源释放及有条件推进 |
| [record-lifecycle](record-lifecycle/README.md) | 淘汰、失效、重载和旧Record身份 |
| [checkpoint](checkpoint/README.md) | 脏组刷盘、重试与停机 |
| [recovery](recovery/README.md) | 进程崩溃后的耐久性边界 |
| [cache-coherence](cache-coherence/README.md) | GCM权限、会话及失联恢复 |
| [integration](integration/README.md) | 权限交接、持久可见性及有界致命退出 |

复现时先按 [工具说明](serializability/README.md#工具与运行) 准备固定TLC jar。各目录README提供正常、负对照与见证运行入口。Java需JDK21，设置 `JAVA_HOME` 可选择本地JDK；Gradle使用 `run-gradle.ps1` 共享mutex入口。执行当前相关Java测试集合：

```powershell
python ZezeJava/formal/checkpoint/validate.py
```

检查结果、日志、状态空间、Java XML/HTML及运行时来源信息均为本地生成产物，不纳入版本控制。正常配置须完整结束；负对照和路径见证须命中指定性质；工具错误及超时不算通过。
