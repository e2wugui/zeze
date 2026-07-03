# Gen —— XML → 多语言代码生成器

读取若干 `solution*.xml` 定义，编译成内存对象模型，为多种目标平台（C# / Java / Lua / TypeScript / C++ / Python）生成协议、Bean、Table、Module 代码。本身是一个 .NET 10 命令行程序 `Gen.exe`。

> 生成的产物在各项目的 `Gen/`、`gen/` 目录下，**不要手改**，重新生成会覆盖。

## 构建

```bat
dotnet build
```

产出 `Gen\bin\Debug\net10.0\Gen.exe`，供仓库根目录的 `gen_use_debug.bat` 等脚本调用。

发布原生单文件（NativeAOT）：

```bat
:: 仓库根目录
PublishGen.bat
:: 等价于： dotnet publish Gen -c Release -r win-x64 -o publish
```

产出 `publish\Gen.exe`（单文件原生可执行程序）。

## 运行

```bat
Gen.exe <solution.xml> [<solution2.xml> ...] [开关]
```

- 仓库根 `gen_use_debug.bat`：用 Debug 版，依次对 `component.confcs.client.xml`、`confcs\solution.xml`、`ZezeJava\ZezeJava\solution.zeze.xml`、`ZezeJava\ZezeJavaTest\solution.xml`、`ZezeJava\ZezexJava\{solution.client.xml,solution.xml,solution.linkd.xml}`、`python\solution.xml` 跑一遍。
- 仓库根 `gen_use_publish.bat`：同上，改用 `publish\Gen.exe`。

## 命令行参数

| 参数 | 说明 |
|---|---|
| `<solution.xml> ...` （位置参数） | 一个或多个 solution 定义文件。**只有这些"参数指定的"文件会被 `Make`（生成代码）**；通过 XML 里 `<import file=...>` 引入的 solution 只参与编译、不生成。无任何位置参数时默认找当前目录的 `solution.xml`。 |
| `-debug` | 打印调试信息（`ImportSolution '...'`、跳过重复输出的 `Skip: ...` 等）。 |
| `-DeleteOldFile <bool>` | 是否在生成结束后清理 gen 目录里本次未产出的陈旧文件，默认 `true`。保留 `.meta`、`.pyc`。传 `false` 可关闭清理。 |
| `-BeautifulVariableId` | **不生成代码**，而是把输入 XML 中 bean/module 的变量 `id` 重写成整齐的递增序号后存回原文件（开发期整理定义用）。 |

示例：

```bat
Gen.exe solution.xml                                   :: 生成当前目录 solution.xml
Gen.exe -debug ZezeJava\ZezeJava\solution.zeze.xml     :: 带调试信息生成
Gen.exe -DeleteOldFile false solution.xml              :: 生成但不清理陈旧文件
Gen.exe -BeautifulVariableId solution.xml              :: 仅整理 XML 变量 id，不生成代码
```

## solution.xml 中 `<project>` 的属性

| 属性 | 必填 | 说明 |
|---|---|---|
| `name` | 是 | 项目名。 |
| `platform` | 见下 | 目标平台，取值见下表。**普通 `<project>` 必须显式指定**（留空会被当成 `cs`，而 `cs` 分支当前未启用，会抛 `unsupport platform`）。 |
| `GenDir` | 是 | 生成代码输出目录。 |
| `SrcDir` | 是 | 手写代码目录。 |
| `CommonDir` | 否 | 公共生成目录（Bean / BeanKey / Protocol 通常放这里），留空则用 `GenDir`。 |
| `PackagePath` | 否 | 包路径 / 命名空间路径。 |
| `GenTables` | 否 | 逗号分隔，按 table 的 `gen` 属性筛选要生成的表（如 `GenTables="client"` 只生成 `gen="client"` 的表）。 |
| `IncludeAllModules` | 否 | `true` 时包含 solution 全部模块；否则只含本项目引用（依赖）到的模块。默认 `false`。 |
| `RelationalMapping` | 否 | `true` 开启关系数据库映射（SQL 表）。 |
| `MappingClass` | 否 | `true` 时为收集到的 Bean 额外生成类映射代码。 |
| `LuaUtilDir` | 否 | Lua 客户端的工具目录名，默认 `common`。 |
| `BuiltinNG` | 否 | `true` 时不生成 `Zeze.Builtin.*` 模块的代码。 |
| `SolutionName` | 否 | 生成时给非 `Zeze` 的 solution 名加该前缀（多 solution 区分用）。 |
| `hot` | 否 | `true` 标记为热更新项目。 |
| `DisableDeleteGen` | 否 | `true` 时不把 `GenDir` 加入陈旧文件清理范围。 |
| `IsUnity` / `EnableBase` / `NoRecursiveModule` / `ClientScript` / `MacroEditor` / `PresentModuleFullName` | 否 | 其他生成开关，按字面语义使用。 |

## `platform` 取值

普通 `<project>`（`Project.MakePlatform`，`Project.cs`）：

| platform | 生成器 | 说明 |
|---|---|---|
| `luaclient` | `luaClient.Maker` | Lua 客户端（Scriban 模板） |
| `cxx` | `cxx.Maker.MakeCxx()` | C++ |
| `ts` / `cxx+ts` | `ts.Maker` | TypeScript |
| `java` | `java.Maker` | Java |
| `conf+cs` | `confcs.Maker` | C# 序列化的**可独立发布**版本，不依赖 Zeze 库；Bean 代码简洁，目前用于 Unity 配置 |
| `conf+cs+net` | `cs.Maker.MakeConfCsNet` | 在 `conf+cs` 基础上增加**网络 + 日志增量同步**，同样可独立发布、不依赖 Zeze 库 |
| `python` | `python.Maker` | Python |
| ~~`cs`~~ | （分支已注释） | 普通 project 的 C# 输出走 conf 系列（`conf+cs` / `conf+cs+net`） |

`<component>`（`Component.MakePlatform`，`Component.cs`，走各语言的 `MakerComponent`）：

| platform | 生成器 |
|---|---|
| `zeze+cs` | `cs.MakerComponent` |
| `zeze+java` | `java.MakerComponent` |
| `conf+cs+net` | `confcs.MakerComponent` |

> `MakerComponent` 会**按单个 Bean/Table 分流**：`kind="rocks"` 的走 `rrcs`/`rrjava`（RocksRaft 专用 Formatter，基类 `Zeze.Raft.RocksRaft.Bean`），其余走标准 `cs`/`java`。详见 `Gen/CLAUDE.md`。

## bean / table 常用属性

```xml
<bean name="BValue" kind="rocks"> ... </bean>            <!-- kind="rocks" → RocksRaft Bean -->
<table name="tRocks" key="int" value="BValue" kind="rocks" />
<table name="tClient" key="long" value="BUser" gen="client" />   <!-- gen 配合 project GenTables 筛选 -->
```

| 属性 | 适用 | 说明 |
|---|---|---|
| `kind` | bean / table | `rocks` 表示 RocksRaft（Bean 基类变成 `Zeze.Raft.RocksRaft.Bean`，Table 走 Raft 日志复制）；bean 默认 `bean`。rocks table 的 value 必须是 rocks bean。 |
| `gen` | table | 生成目标标签（如 `client`），配合 `<project GenTables="client">` 筛选是否生成该表。 |
| `memory` | table | `true` 内存表。 |
| `autokey` | table | `true` 自增主键（key 须为 long）；`random` 随机 binary 主键。 |
| `id` | table | 显式表 id，不填则按全名 hash 生成。 |
| `RelationalMapping` | table | `true` / `false` / `project`（project 表示跟随 `<project RelationalMapping>`）。 |
| `version` | table | 关系映射 schema 版本号。 |

## 命名约束

生成的标识符在 Java/C#/C++/JS/TS/Lua **多语言保留字并集**中不能冲突，名字也不能以 `_` 开头或结尾（hot bean 的尾下划线例外）。定义命名时需顾及所有目标语言。
