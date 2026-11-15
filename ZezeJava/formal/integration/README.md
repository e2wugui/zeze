# 组合验证与修复复核

本目录核对事务、权限交接、刷盘、重新加载及有界致命退出之间的接口。模型将保存、权限回收与新节点获权分开，区分正常配置、负对照、路径见证和边界反例。

执行：

```powershell
python ZezeJava/formal/integration/check.py --java 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9\bin\java.exe'
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9'
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "& .\ZezeJava\formal\run-gradle.ps1 -GradleArgs @('-I', 'formal/integration/validation.gradle', ':ZezeJavaTest:test', '--tests', '*TestSharedCacheCommitHandoff', '--tests', '*TestGlobalFatalSaveWindow', '--console=plain')"
```

TLC使用共用固定版本，单worker、seed1、fp0。正常项需退出0并输出完整成功；负对照/见证需退出12并违反指定不变量。超时或其他错误失败退出。日志、生成配置、Java XML/HTML及检查摘要均在 `build/` 本地生成，不纳入版本控制。

`Handoff` 固定两节点、两条记录、每节点一次调用、最多两轮尝试；有三个程序：同键自增、写两条与读两条、写偏斜。正常 commit 是条件性的本地原子发布接口；不展开最终日志逐字段应用、网络重连、进程crash或跨组件等待环。因而不能代替S0/S1的提交论证、S6的会话/守护时限或S5的恢复结论。`CHECK_DEADLOCK FALSE`，不声称无死锁或活性。

编码只做序列化、不执行newProcedure，不建模编码期间发起业务事务。

`FatalSave` 对应已有Release预算内的最终保存窗口：关提交准入、排空已准入发布、收集并写入、失败重试、截止退出，然后才允许按GCM期限回收。它依赖watchdog及时执行，以及未完成后端请求不会在旧进程终止后迟到生效。取消这两个前提的配置保留为边界反例，不代表已经实现后端fencing或无限故障下零丢失。
