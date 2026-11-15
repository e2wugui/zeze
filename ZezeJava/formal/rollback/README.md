# 重现 S1

在仓库根运行（Python 3.12、JDK 21；不需要外部服务）：

```powershell
python ZezeJava/formal/rollback/check.py --java 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9\bin\java.exe'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ZezeJava/formal/rollback/run-java.ps1
```

`run-java.ps1` 仍只通过 `formal/run-gradle.ps1` 启动 Gradle；外层递归获取同一命名 mutex，保证在共享 XML 被下一个聊天的测试覆盖前归档。脚本不更改系统执行策略。若 Gradle 用户缓存锁被沙箱限制，需按执行环境申请缓存写入权限，不删缓存。

TLC使用 [工具说明](../serializability/README.md#工具与运行) 中的共用固定版本，运行前检查完整性。

检查入口固定单 worker、seed=1、fp=0，每项独立 metadir。正常项必须退出 0 且报告完整结束；负对照和见证必须退出 12 且命中指定不变量。每项默认超时180秒，超时算未完成。无公平性，无活性断言，`CHECK_DEADLOCK FALSE` 不作为无死锁证据。

`build/<case>/` 保存生成配置、TLC日志和状态，`traces/` 保存负对照与路径见证。Java XML、Gradle日志和检查摘要均为本地生成产物，不纳入版本控制。
