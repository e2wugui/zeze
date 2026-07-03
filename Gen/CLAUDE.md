# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 本文件聚焦 `Gen/`（Zeze 的 XML→多语言代码生成器，C# / .NET 10）。仓库总览见根目录 `../CLAUDE.md`，Claude Code 会同时加载两份。下方用中文说明（遵循用户偏好）。

## 这是什么

`Gen/` 是命令行代码生成器 `Gen.exe`：读取若干 `solution*.xml` 定义，编译成内存对象模型，再为多种目标平台生成协议 / Bean / Table / Module 代码。产物散落在仓库各处（`ZezeJava/**/Gen/`、`projects/**/gen/`、`TypeScript/`、`python/`、`cxx/` 等）。**不要手改 `Gen/`、`gen/` 目录下的文件**——重新生成会覆盖。

## 构建与运行

- **Debug 构建**：在 `Gen/` 下 `dotnet build` → `Gen/bin/Debug/net10.0/Gen.exe`。（`Gen/README.md` 写的 net8.0 已过时，实际是 **net10.0**。）
- **Release / 原生单文件**：仓库根 `PublishGen.bat` → `dotnet publish Gen -c Release -r win-x64 -o publish`，`PublishAot=true` 产出原生单文件 `publish/Gen.exe` 并删 pdb。
- **批量生成**：仓库根 `gen_use_debug.bat`（用 Debug 版）或 `gen_use_publish.bat`（用 publish 版）。脚本把 `Gen.exe` 加进 PATH，依次对多个 solution 跑一遍：`component.confcs.client.xml`、`confcs/solution.xml`、`ZezeJava/ZezeJava/solution.zeze.xml`、`ZezeJava/ZezeJavaTest/solution.xml`、`ZezeJava/ZezexJava/{solution.client.xml,solution.xml,solution.linkd.xml}`、`python/solution.xml`。
- **手动跑单个**：`Gen.exe <path/to/solution.xml>`。开关：`-debug`（打印 ImportSolution/Skip）、`-DeleteOldFile false`（禁用清理陈旧文件）、`-BeautifulVariableId`（重写 XML 变量 id 为整齐序号，仅整理用）。无参数时默认找当前目录 `solution.xml`。
- 依赖：`Scriban`（模板）、`Mvp.Xml`（XInclude）。模板 `templates/*.scriban-txt` 作为 EmbeddedResource 嵌入，**改模板后必须重新 build** 才生效。

## 架构（核心，跨多文件才能看清）

代码在 `Gen/Gen/`，命名空间 `Zeze.Gen`。`Gen/Zeze/`（Net/Transaction/Util）是少量运行时辅助（`Ranges`、`AtomicLong`、`FileSystem` 等），不参与生成逻辑本身。

**三阶段流水线**（`Program.Main`，`Gen/Gen/Program.cs`）：
1. **构造**（`ImportSolution`）：用 `Mvp.Xml.XInclude.XIncludingReader` 加载 XML（支持 `<import file=...>` / XInclude），递归构建 `Solution` 模型；按绝对路径去重，避免循环 import。
2. **Compile**（`Solution.Compile` → `Project.Compile` → `ModuleSpace.Compile`）：这一步才解析跨引用，通过 `Program.NamedObjects`——**大小写不敏感**的全名注册表（Windows 文件系统大小写不敏感，故名字也按不敏感判重）。`GenDerives()` 在此后建立 Bean 继承链（`Bean.Derives`）。
3. **Make**（`Solution.Make` → 每个 `Project.Make` → `Project.MakePlatform`）：实际写出代码。

**模型层级**：`ModuleSpace`（基类，持有 Beans/BeanKeys/Protocols/Modules 字典）← `Solution`、`Module`。`Solution` 含多个 `Project`；`Project` 含 `Service`、引用若干 `Module`；`Bean`/`BeanKey`/`Protocol`/`Table`/`Rpc` 是被生成的实体；`Component` 是可组合的模块集。

**平台分发**（`Project.MakePlatform`，`Gen/Gen/Project.cs:331`）——按 solution.xml 的 `<project platform="...">` 分派：

| platform | 生成器 | 说明 |
|---|---|---|
| `cs`（platform 留空时默认） | `cs.Maker` | 注意：纯 `cs` 分支在 `MakePlatform` 里被注释；普通 project 的 C# 输出走 conf 系列 |
| `luaclient` | `luaClient.Maker` | Lua 客户端，Scriban 模板 |
| `cxx` | `cxx.Maker.MakeCxx()` | C++ |
| `ts` / `cxx+ts` | `ts.Maker` | TypeScript |
| `java` | `java.Maker` | Java |
| `conf+cs` | `confcs.Maker` | C# 序列化的可独立发布版本，不依赖 Zeze 库；Bean 简洁，用于 Unity 配置 |
| `conf+cs+net` | `cs.Maker.MakeConfCsNet(depends)` | 在 conf+cs 上加网络 + 日志增量同步，同样可独立发布 |
| `python` | `python.Maker` | Python |

每个语言目录（`cs/`、`java/`、`luaclient/`、`ts/`、`cxx/`、`python/`、`confcs/`、`javadata/`、`rrcs/`、`rrjava/`）内是一个 `Maker` + 若干按关注点拆分的 Formatter（`BeanFormatter`、`ModuleFormatter`、`ProtocolFormatter`、`RpcFormatter`、`ServiceFormatter`、`TypeName`、`TypeTagName` …）。cs/java/cxx/python/ts 用 `StreamWriter` 直接拼字符串输出；**luaclient 用 Scriban 模板**。

**输出管理**（`Program`）：
- `OpenStreamWriter` / `OpenWriterNoPath` 统一登记所有输出；同一文件多次写默认跳过（除非 `overwrite`）。
- `StreamWriterOverwriteWhenChange`：**仅当内容变化才落盘**，避免无谓改动触发下游重新编译。
- `AddGenDir` 登记 gen 目录；`FlushOutputs()` 在每个 platform 阶段之间冲刷。
- `DeleteOldFileInGenDirs()`：Make 结束后清理 gen 目录里本次没产出的陈旧文件（保留 `.meta`、`.pyc`），可用 `-DeleteOldFile false` 关闭。

**Lua 客户端特例**（`luaclient/`，活跃改动区）：
- `ScriptModelBuilder` 两遍构建 Scriban 模型：先建壳再填引用，**引用语义共享**（同一对象在模板里是同一引用，不是拷贝）。
- `FileChunkGen`（标记 `--- [[ AUTO GENERATE START/END ]] ---`）：把"生成段"合并进**用户手写文件**——`message.lua`/`module.lua` 的注册段每次重写；`Module<Name>.lua` 先 `LoadFile`，已存在则只刷新 chunk、保留手写的 `OnMsg_<Protocol>` 实现，不存在才整体生成骨架。

**命名约束**（`Program.CheckReserveName` 等）：名字不能以 `_` 开头；不能以 `_` 结尾（hot bean 例外）；须匹配 `^\w_+$`；且不能命中 `Program.reservedNames`——这是 Java/C#/C++/JS/TS/Lua **多语言保留字并集**，因为同一份 XML 会生成到多语言。改这个集合要顾及所有目标语言。

## 改动流程

1. 改生成器源码（`Gen/Gen/**/*.cs`）或模板（`templates/*.scriban-txt`）后，先 `dotnet build` 重建 `Gen.exe`。
2. 用 `gen_use_debug.bat`（或对应 publish 脚本）重新生成各处 `Gen/` 产物。
3. 生成产物通常随生成器改动一起提交。
- `solution.xml` 的 `<project>` 必填 `GenDir` 和 `SrcDir`，否则报 `Config Need: GenDir & SrcDir`。
- 普通 project 的 C# 输出用 `conf+cs` / `conf+cs+net`（均为可独立发布的 conf 系列）；纯 `cs` platform 分支已被注释。
