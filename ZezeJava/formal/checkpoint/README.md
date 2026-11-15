# S4 Checkpoint 复核入口

在仓库根目录执行：

```powershell
python ZezeJava/formal/checkpoint/check.py
```

默认执行 S4 与 S5。仅 S4：`--section checkpoint`；仅指定名称：`--only stop`。脚本校验共用 jar 的固定 SHA256，固定 JDK21，单 worker、每轮独立 metadir。正常检查必须完整结束；负对照必须以指定性质失败；工具错误、超时均为失败。日志和完整反例在 `build/<配置>/tlc.log`；配置保存在 `configs/`；源码/工具身份及结果在 `verification.json`。定向运行的结果在 `build/results-<筛选>.json`，不覆盖全量清单。

TLC jar使用 [工具说明](../serializability/README.md#工具与运行) 中的共用固定版本，运行前检查完整性。

Java 常规入口（必须通过共享锁）：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9'
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "& .\ZezeJava\formal\run-gradle.ps1 -GradleArgs @(':ZezeJavaTest:test','--tests','*TestFormalCheckpoint','--console=plain')"
```

模型日志、检查摘要和Java XML/HTML均在本地生成，不纳入版本控制。当前相关Java测试集合可以通过 `python ZezeJava/formal/checkpoint/validate.py` 运行。


约定：encode只负责序列化，不执行newProcedure。模型与Java验证不包含编码中发起业务事务，也不为其增加快照元数据或入口保护。
