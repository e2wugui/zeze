# S5 复核入口

模型使用 S4 的运行期状态机，`Recovery.tla` 显式复用其 Crash/Restart 定义。仓库根目录：

```powershell
python ZezeJava/formal/recovery/check.py
```

可加 `--only crash` 定向复核。工具哈希、JDK21、完成判定与 Java / 红色复现的共享锁命令见 `../checkpoint/README.md`。配置、完整反例及结果身份分别在 `configs/`、`build/<配置>/tlc.log`、`verification.json`。进程测试包含在 `TestFormalCheckpoint.hardCrashAndRestartRetainOnlyTheCommittedBackend`，无需外部服务。
