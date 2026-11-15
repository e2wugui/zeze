# S6：缓存权限、会话故障与 fencing

运行环境为JDK 21、Python 3，不需要外部数据库。工具使用 [工具说明](../serializability/README.md#工具与运行) 中的共用固定版本，运行前检查完整性。

在仓库根目录运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9'
python ZezeJava/formal/cache-coherence/check.py
& .\ZezeJava\formal\run-gradle.ps1 -GradleArgs @(':ZezeJavaTest:test','--tests','*TestFormalCacheCoherence','--console=plain','--no-configuration-cache','--init-script','formal/cache-coherence/capture-java.gradle')
python ZezeJava/formal/cache-coherence/archive-java.py
```

Java构建经过共享mutex入口。Windows禁止直接执行脚本时，可使用 `powershell.exe -NoProfile -ExecutionPolicy Bypass -Command` 调用。

`check.py` 每个配置使用单worker、独立metadir、1500MB heap，单项480秒超时，超时或工具错误均失败。正常配置必须exit 0并有完成标志；负对照/边界反例/路径见证必须exit 12且精确匹配指定性质。完整日志及结果在本地生成，`.cfg`由脚本重建，也可手动运行TLC。

模型分工：

负对照、边界反例、Java XML、运行结果及来源信息都在本地生成，不纳入版本控制。

- `Permissions.tla`：单 key，GCM 账本、本地有效权限、客户端持锁、Acquire/Reduce、错误与超时、迟到处理、Flush/Load 版本交接。2～3 客户端，每端最多1～2次请求、每端最多1次修改。
- `Sessions.tla`：一次会话世代转换，Disconnect/Kick、Login/ReLogin、NormalClose、延迟清理与旧世代隔离。清理的本地 fencing 作为外部接口契约。
- `Lease.tla`：期限内释放/退出、服务端独立回收、进程暂停后恢复，以及额外恢复 fencing 契约的效果。
- `HaltBudget.tla`：释放超时后 checkpoint-before-halt 的额外宽限与 GCM 回收截止时间相撞。

这些是分层有限模型，尚未机械证明其组合；没有 Java 精化证明、TLAPS 任意规模证明或全框架证明。所有配置显式关闭 TLC 的终态死锁报告，只检查安全性质与路径可达性，不能据此声称无死锁、无饥饿、每次请求最终成功。

Flush成功假设后端提交已对后续Load可见；共享后端、同键互斥和守护及时执行是模型的接口前提。Java测试使用真实Application、Table/Bean、共享Memory后端和loopback TCP RPC；裸协议客户端只用于控制Reduce应答。暂停见证刻意停止本地watchdog并推进GCM观察时间，不是操作系统进程暂停测试。halt预算见证调用真实helper，但替换终止回调，不会终止测试JVM。
