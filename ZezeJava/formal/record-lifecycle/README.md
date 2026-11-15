# 重现 S3

仓库根运行：

```powershell
python ZezeJava/formal/record-lifecycle/check.py --java 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9\bin\java.exe'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ZezeJava/formal/rollback/run-java.ps1
```

Java 与 S1 共用 `TestFormalRollback`，但其中3项是本目录的生命周期时序，不能重复累计为另外11项测试。脚本经共享 mutex 入口运行并在锁内拷贝 XML 到两个所属 build 目录。工具固定哈希、成功/负对照判定、超时规则和产物格式同 [S1 复现说明](../rollback/README.md)。不需要外部 GCM/SM，本目录的 Invalid 状态由测试在真实锁保护下显式注入，不声称验证远端权限协议。

正常项仅检查安全性；没有公平性、活性或无死锁结论。运行结果及 `traces/` 下的负对照与见证均在本地生成，不纳入版本控制。
