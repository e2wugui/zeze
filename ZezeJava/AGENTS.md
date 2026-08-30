# ZezeJava 开发指南

## 运行 ZezeJavaTest

测试已在 `ZezeJavaTest` 的 **test 源集**（JUnit 6 / Jupiter，`src` 与 `Gen` 一起挂在 test 源集，二者互相引用不能拆分）。

```bat
:: 在 ZezeJava 目录下，只需 JDK 21，不需要任何手工启动的服务：
gradlew.bat :ZezeJavaTest:test             :: 快速自包含测试（@Fast 标注的类，无外部依赖）
gradlew.bat :ZezeJavaTest:integrationTest  :: 全量功能测试（自动在进程内启动 SM/GCM，不含fast和bench）
gradlew.bat :ZezeJavaTest:bench            :: 吞吐基准（@Bench 标注的类）
```

## 单类/单方法验证（--tests）

**用通配符（或简单类名）形式**，三个测试任务通用：

```bat
gradlew.bat :ZezeJavaTest:test --tests "*TestToken"              :: 单类（类需 @Fast）
gradlew.bat :ZezeJavaTest:test --tests "*TestToken.testToken"    :: 单方法
gradlew.bat :ZezeJavaTest:integrationTest --tests "*TestCsQueue" :: 单类（类不带 @Fast/@Bench）
gradlew.bat :ZezeJavaTest:bench --tests "*DiffLockAndNoLock"     :: 单类（类需 @Bench）
```

坑：**完整包名+类名、且不带通配符**的形式（如 `--tests "UnitTest.Zeze.Component.TestToken"`
或 `...TestToken.testToken`）会误报 `No tests found for given includes`，即使类存在、标签正确。
简单类名 `TestToken`、通配符 `*TestToken`、`*pkg.*ClassName` 均正常。

另外类的标签必须匹配任务的标签过滤，否则通配符形式同样报 No tests found：
test 只跑 @Fast；integrationTest 只跑不带 fast/bench 标签的；bench 只跑 @Bench。
