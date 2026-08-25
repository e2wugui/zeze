# ZezeJava 开发指南

## 运行 ZezeJavaTest

1. （可选）运行仓库根目录的 `gen_use_publish.bat`——仅在需要重新生成代码时执行，已生成过可跳过。
2. 运行 `test/build.bat`。
3. 运行 `test/service & global.bat`（启动 ServiceManager 5001 与 GlobalCacheManager 5002）。
4. 运行 `test/service & global.another.bat`。
5. 用 IDEA 打开 `ZezeJava` 目录，右键 `ZezeJavaTest/src`，选择 "Run 'All Tests'"。

说明：测试代码在 `ZezeJavaTest/src` 主源集（JUnit 4），`gradle test` 任务不会发现它们；
进程内组网类测试（如 `Zezex.TestOnlineSpec`）依赖第 3、4 步启动的服务，否则会报
`Connection refused`（5001/5002）而失败。
