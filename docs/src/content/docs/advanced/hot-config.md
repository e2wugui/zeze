---
title: "热更模块配置指南"
sidebar:
  order: 1.5
---

本篇讲**如何把一个模块配成热更模块**：solution.xml 声明、源码目录冷热分离、Gen 生成行为、构建与打包、运行时装载的完整配置链。热更新的运行时机制（ClassLoader 层级、升级流程、状态迁移）见[热更新](./hot-reload.md)。

参照实现：`ZezeJava/ZezexJava`（8 个热模块的完整示例工程）。

## 1. solution.xml 声明

热更模块需要**三处**配置，缺一不可：

```xml
<!-- ① 模块级：声明这个模块是热更模块 -->
<module name="Login" id="1" hot="true">
    ...
</module>

<!-- ② + ③ 工程级：工程标记为热更工程，并指定热模块用户码目录 -->
<project name="server" hot="true"
         GenDir="server/Gen" SrcDir="server/src"
         HotSrcDir="server/src-hot"
         platform="java" GenTables="">
```

| 配置 | 层级 | 含义 |
|------|------|------|
| `hot="true"` | module | 模块标记为热更模块 |
| `hot="true"` | project | 工程支持热更模块 |
| `HotSrcDir` | project | 热更模块用户码（模块主类）的输出目录 |

**双 hot 语义**：只有 `project.hot && module.hot` 同时成立，模块才按热更处理（生成表格注册用 `replaceTable`、主类归 `HotSrcDir`）。单独的模块级 hot 不生效——这允许同一个 solution 里有热更工程和非热更工程引用同一模块。

**强制校验**：工程拥有热更模块（双 hot）但未配置 `HotSrcDir` 时，Gen 直接报错退出，不会悄悄回退到 `SrcDir`：

```
Exception: project:server has hot module Game.Login, HotSrcDir must be configured.
```

回退会在冷源码树重建模块主类骨架、破坏冷热分离，因此按配置错误处理。

## 2. 目录布局：冷热分离

热更模块启用后，源码分三个目录：

| 目录 | 内容 | 编译产物去向 |
|------|------|--------------|
| `src/` | **冷代码**：非热更模块、App、公共代码 | 冷 classpath（server.jar） |
| `src-hot/` | **热代码**：热更模块的实现 + 模块主类 + 接口 | 仅热模块 jar |
| `Gen/` | Gen 生成物（全量再生，gitignore） | 由构建分拣，见第 5 节 |

以 `Game.Equip` 为例：

```
src-hot/Game/Equip/
├── ModuleEquip.java    # 模块主类（用户码）
├── Equip.java          # 业务实现（用户码）
├── IEquip.java         # 跨装载器接口（手写）
└── IModuleEquip.java   # 跨装载器接口（手写）

src/Game/               # 不含任何热模块文件
├── App.java
├── Map/、Rank/         # 冷模块
└── ...
```

**原则：`src/` 下不留任何热模块文件。** 冷 classpath 上出现热模块类会被运行时的冷抢载守卫拦截（见第 8 节）。

### Gen 的生成行为

- **模块主类 `Module<X>.java`**：双 hot 模块生成并维护在 `HotSrcDir` 下。文件不存在时生成含 `NotImplement` 处理器的初始骨架；已存在时做 chunk 级增量维护（增删协议时更新 `GEN MODULE` chunk、补插新协议的空处理器），不覆盖用户代码。
- **兼容迁移**：`HotSrcDir` 下没有而 `SrcDir` 下有旧文件时，Gen 保持原地维护并输出警告（不会两处各生成一份）。把文件移过去后警告消失。
- **`AbstractModule`、Bean、协议等生成码**：仍全量输出到 `GenDir`，由构建按包分拣（见第 5 节）。

## 3. 接口设计规范

热模块对外暴露的能力通过**手写接口**（`I*.java`）声明，接口与实现同在 `src-hot/`：

```java
// src-hot/Game/Fight/IModuleFight.java
package Game.Fight;

import Zeze.Hot.HotService;

public interface IModuleFight extends HotService {
    void StartCalculateFighter(long roleId);
    boolean isAreYouFightDone();
}
```

规范要点：

1. **接口不是 Gen 生成的**，由用户手工编写和维护，继承平台基类 `Zeze.Hot.HotService`。
2. **冷代码不能引用这些接口**。接口类只存在于 `interface.jar`，由 HotManager 的类加载器装载；冷代码（测试、其他冷模块）在编译期和字节码引用它都会因冷 classpath 缺类而失败。跨装载器调用用反射：

   ```java
   // 拿热模块服务：getModuleContext 的 Class 参数只是上下文索引的 key，用平台基类即可
   var service = zeze.getHotManager()
           .getModuleContext("Game.Fight", Zeze.Hot.HotService.class).getService();
   var isDone = service.getClass().getMethod("isAreYouFightDone");
   if ((boolean)isDone.invoke(service)) { ... }
   ```

3. **接口之间可以跨包引用**（如 `IEquip` 引 `Game.Item.IItem`）：所有模块的 interface.jar 条目由 HotManager 合并进**同一个类加载器**索引，跨包解析拿到的是同一身份，不存在分裂。
4. **接口签名保持稳定**：接口身份不随热更替换，升级时不应修改既有方法的签名——新增方法可以，改删要保证旧调用方的兼容。

## 4. 配置文件（服务端）

`server.xml` 的 zeze 节点指定热更工作目录：

```xml
<zeze name="..." HotWorkingDir="hot" HotDistributeDir="hot/distributes" ...>
```

- **HotWorkingDir**：热更根目录。启动时从这里装载 `modules/*.jar` 与 `interfaces/*.interface.jar`。
- **HotDistributeDir**：发布文件的接收目录，`Zeze.Hot.Distribute` 上传的新版本先落这里。

## 5. 构建配置

冷热分离用 Gradle 源集实现：`main` 只含 `src`，新增 `hot` 源集含 `src-hot`，Gen 生成物按包分拣到两个源集。核心配置（摘自 ZezexJava）：

```groovy
def hotModuleNames = ['Buf', 'Equip', 'Fight', 'Item', 'Login', 'LongSet', 'Skill', 'Timer']
def hotPackagePatterns = hotModuleNames.collect { "Game/${it}/**" }

// Gen 全量再生成无条件输出热模块包，按包分拣到 build 下两个镜像目录再挂进源集
def genMainDir = layout.buildDirectory.dir('generated/sources/gen-main')
def genHotDir = layout.buildDirectory.dir('generated/sources/gen-hot')
def syncGenMain = tasks.register('syncGenMain', Sync) {
    from 'Gen'
    into genMainDir
    exclude hotPackagePatterns   // 热模块的生成码不进冷源集
}
def syncGenHot = tasks.register('syncGenHot', Sync) {
    from 'Gen'
    into genHotDir
    include hotPackagePatterns
}

sourceSets {
    main {
        java {
            srcDirs = ['src']
            srcDir syncGenMain
        }
    }
    hot {
        java {
            srcDirs = ['src-hot']
            srcDir syncGenHot
        }
        compileClasspath += sourceSets.main.output           // 热代码可以引用冷代码
        compileClasspath += sourceSets.main.compileClasspath
    }
}
```

要点：

- **不能把 `src/Game/<X>` 注册成独立 srcDir 根来绕过排除**——IDEA 会把每个 srcDir 标成源码根并检查包路径与文件路径的一致性，分拣镜像目录保留完整包路径，IDE 和 CLI 都不报错。
- `Gen/` 是每次全量再生的 gitignore 产物，只能过滤不能搬目录。
- 热代码单向依赖冷代码（`hot` 的 classpath 含 `main.output`），反向依赖不存在。

## 6. 打包发布

### Distribute 打包

`Zeze.Hot.Distribute` 把编译产物按模块拆分成冷包和热包：

```bash
java -cp <zeze平台类>;<应用全部类> Zeze.Hot.Distribute \
    -privateBean \
    -app Game.App \
    -workingDir hot \
    -classes <classes目录> \
    -providerModuleBinds provider.module.binds.xml \
    -config server.xml
```

| 参数 | 含义 |
|------|------|
| `-classes` | 输入的 classes 根目录（见下方 staging 说明） |
| `-workingDir` | 输出根目录（即 `HotWorkingDir`） |
| `-app` | 应用入口类，Gen 在其中生成了静态方法 `distributeHot(Distribute)`，声明本工程的热模块全名集合 |
| `-privateBean` | Bean 类打进模块 jar 而非 interface.jar（接口 jar 只留接口与契约类） |
| `-providerModuleBinds` / `-config` | 打包模块配置（module.config）所需 |
| `-atomicAll` | 全局两阶段原子发布（多服务器） |

打包前 `HotWorkingDir` 必须已存在（可先 `mkdir hot/distributes`），否则跳过 module.config 打包环节。

### staging：冷热合并后再打包

Distribute 以"目录里存在 `Module<DirName>.class`"识别模块目录。冷热分离构建后，`main` 输出里没有热模块主类、`hot` 输出里没有留冷的生成码——单独喂哪个都不完整，需要先把两边合并成一个完整布局：

```bat
rd /s /q build\classes\java\hotstage
mkdir build\classes\java\hotstage
xcopy /e /i /y build\classes\java\main build\classes\java\hotstage
xcopy /e /i /y build\classes\java\hot  build\classes\java\hotstage
java ... Zeze.Hot.Distribute -classes build/classes/java/hotstage ...
```

### 产物布局

```
hot/
├── server.jar                        # 冷包：除热模块外的全部类（不含热模块目录）
├── modules/
│   ├── Game.Login.jar                # 热模块实现（Module、AbstractModule、Bean...）
│   └── Game.Equip.jar
├── interfaces/
│   ├── Game.Login.interface.jar      # 跨装载器接口
│   └── Game.Equip.interface.jar
├── __hot_schemas__Game.jar           # schema 热更
└── distributes/                      # 发布接收目录
```

服务进程的启动 classpath 只放**平台类 + `server.jar`**，热模块类只能经 HotManager 从 `HotWorkingDir` 装载——这是冷热分离的运行时前提。

## 7. 运行时装载

启动流程（App 生成代码驱动，无需手工干预）：

1. `HotManager` 初始化：扫描 `HotWorkingDir/interfaces/*.interface.jar`，把所有接口条目合并进**单一类加载器**索引（`putJar`）。
2. `loadExistModules`：为 `modules/*.jar` 逐个创建 `HotModule` 子类加载器，装载 `Module<X>` 实现类。
3. `App.startModules()`：冷模块直接启动，热模块经 `HotManager.startModule("Game.Xxx")` 启动。
4. 热模块的表格注册使用 `replaceTable`（替换而非新增），升级重装时数据保留。

在线升级流程（TryDistribute 两阶段、回滚、状态迁移）见[热更新](./hot-reload.md)。

## 8. 冷抢载守卫

`HotModule` 构造时装载模块主类后立即校验类加载器归属：

```java
this.moduleClass = loadClass(moduleClassName);
// 双亲委派下，冷classpath上的同名类会抢先命中；冷类不受热更控制——之后换jar
// 也不会换掉这个身份，安装对该模块静默失效。
if (moduleClass.getClassLoader() != this)
    throw new IllegalStateException("hot module shadowed by cold classpath: "
            + moduleClassName + " loaded by " + moduleClass.getClassLoader());
```

触发条件：**冷 classpath 上存在热模块同名类**。典型原因：

- 模块实现或骨架文件残留在 `src/`（冷源集）里；
- 测试/IDE 运行时把 hot 源集输出也挂进了 classpath；
- Distribute 后手工把模块 jar 加进了启动 classpath。

排查方向：确认 `src/` 下无热模块文件、启动 classpath 只含平台与 `server.jar`。守卫 fail-fast 发生在安装可回滚区，走既有 recoverModules 恢复。

## 9. 检查清单

启用热更模块的完整配置链：

- [ ] solution.xml：模块 `hot="true"`，工程 `hot="true"` + `HotSrcDir`
- [ ] `src/` 无热模块文件；实现、主类、接口全在 `src-hot/`
- [ ] 接口手写、继承 `HotService`、签名稳定；冷代码零引用（跨装载器调用用反射）
- [ ] Gradle：`main`/`hot` 双源集 + Gen 分拣（热模块包 exclude 出冷侧）
- [ ] `server.xml`：`HotWorkingDir` / `HotDistributeDir`
- [ ] Distribute：staging 合并后打包，产物 = server.jar（无热模块）+ modules + interfaces
- [ ] 启动 classpath：平台类 + server.jar，不含任何热模块类
