# ZezeJava

- 不要自动更新 `BENCHMARK.md`；基准跑法与结论以它为准。
- 测试需 JDK 21，无需外部服务：
  - `gradlew.bat :ZezeJavaTest:test`：仅 `@Fast`。
  - `gradlew.bat :ZezeJavaTest:integrationTest`：排除 fast/bench。
  - `gradlew.bat :ZezeJavaTest:bench`：仅 `@Bench`。
- `--tests` 用简单类名或通配符，如 `--tests "*TestToken.testToken"`；测试类标签须匹配任务。
- `@Fast` 须支持类级并行；serverId 与固定内存库 url 按 `harness/FastServerIds` 登记，禁止共享默认空 url。
- 所有 ChangeListener 须在业务事务开始前注册，运行期间不得新增。
- 测试类名使用描述性行为名，不带 bug、审计或轮次编号。
- 同一部署的所有 Application 与 GCM 必须共用同一后端数据库；多 app/GCM/持久化测试也须显式同库。
- 空安全统一使用 JSpecify：包级 `@NullMarked`，可空项用 `org.jspecify.annotations.Nullable`；不得为消除告警改变空值语义或机械添加 `Objects.requireNonNull`。
- 修改 Gen 后运行 `Gen/PublishGen.bat` 和根目录 `gen_use_publish.bat`，不要手改生成文件。
- 一个 bug 一个提交；提交信息简述问题证据、关键修复和实际验证，未执行的不写。
