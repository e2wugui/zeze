---
title: "C# / Unity 客户端接入"
description: "通过 solution.xml 自动生成 C# 代码，在 Unity 中实现与 Zeze 服务端的跨语言通信。"
category: reference
order: 60
---

# C# / Unity 客户端接入

> 本文档面向使用 **Unity 游戏引擎**的客户端开发者，说明如何基于 Zeze 的代码生成工具从 `solution.xml` 自动生成 C# 代码，实现 Bean 序列化、协议编解码与跨语言通信。

## 概述

Zeze 的 C# 客户端主要面向 Unity 游戏引擎。开发者通过 `solution.xml` 定义数据结构（Module / Bean）、协议和数据表，代码生成工具会自动生成对应的 C# 代码，客户端无需手写序列化与协议编解码逻辑，即可与 Java 服务端实现跨语言通信。

跨语言一致性由统一的二进制序列化格式保证，详见 [序列化格式](./serialize.md)。

## 项目配置

C# 客户端在 `solution.xml` 中通过 `<project>` 节点进行配置。以 `confcs/solution.xml` 为例：

```xml
<project name="confcs"
         GenDir="confcs/Gen"
         SrcDir="confcs"
         platform="conf+cs"
         IncludeAllModules="true"
         MacroEditor="UNITY_EDITOR">
```

各属性含义如下：

| 属性 | 说明 |
| --- | --- |
| `name` | 项目名称，此处为 `confcs`。 |
| `GenDir` | 生成 C# 代码的输出目录（`confcs/Gen`）。 |
| `SrcDir` | 手写 C# 源码目录（`confcs`）。 |
| `platform` | 目标平台。`conf+cs` 表示同时生成配置与 C# 客户端代码。 |
| `IncludeAllModules` | `true` 表示包含 solution 中所有模块。 |
| `MacroEditor` | 宏条件编译标识，`UNITY_EDITOR` 用于在 Unity 编辑器环境下条件编译。 |

## 数据类型表

`solution.xml` 中支持的数据类型及其在 C# 中的对应关系如下：

| XML 类型 | 说明 | C# 对应 |
| --- | --- | --- |
| `int` / `long` / `short` / `byte` | 整数类型 | `int` / `long` / `short` / `byte` |
| `bool` | 布尔 | `bool` |
| `float` / `double` | 浮点数 | `float` / `double` |
| `string` | 字符串 | `string` |
| `binary` | 二进制数据 | `Zeze.Net.Binary` |
| `list[T]` | 列表 | `List<T>`（泛型列表容器） |
| `set[T]` | 集合 | 集合容器 |
| `map[K,V]` | 映射 | 字典容器 |
| `vector2` ~ `vector4` | 游戏向量 | `Zeze.Serialize.Vector2` / `Vector3` / `Vector4` |
| `quaternion` | 四元数 | `Zeze.Serialize.Quaternion` |
| `dynamic` | 动态 Bean | 动态 Bean 基类 |
| `array` | 定长数组 | 定长数组 |

## 协议定义

在 `solution.xml` 中通过以下节点定义协议：

| 节点 | 说明 |
| --- | --- |
| `<protocol name argument handle="server,clientscript">` | 单向协议，`handle="server"` 表示服务端处理，`clientscript` 表示客户端脚本处理。 |
| `<rpc name argument result handle="server">` | 请求-响应协议，客户端发起请求、服务端返回结果。 |
| `<protocol NoProcedure="true" handle="server">` | 不生成过程调用的协议，仅由 `handle` 指定的端处理。 |

`handle` 取值含义：

- **`server`**：该协议由服务端处理。
- **`clientscript`**：该协议由客户端脚本处理。

完整协议定义语法详见 [solution.xml 参考](./solution-xml.md)。

## 代码生成

代码生成工具会在 `Gen/` 目录下生成以下 C# 类，**请勿手动编辑**：

| 生成产物 | 说明 |
| --- | --- |
| Bean 类 | 包含序列化（`Encode`）与反序列化（`Decode`）逻辑。 |
| Protocol / RPC 类 | 协议与 RPC 的编解码、发送方法。 |

> 注意：`conf+cs` **不生成 Table 类**（Table 是服务端 `java` 平台的概念）。客户端如需联网，应使用 `conf+cs+net` 平台，它会额外生成网络层代码。

每次 `solution.xml` 修改后重新运行代码生成工具即可更新 `Gen/` 目录。

## Dynamic Bean

动态 Bean 用于实现可扩展的继承体系，定义方式如下：

```xml
<!-- 定义可被继承的基类 -->
<bean name="Base" extendable="true">

<!-- 定义继承 Base 的子类 -->
<bean name="Derive" base="Base" extendable="true">

<!-- 声明一个动态类型变量，基类约束为 Base -->
<variable type="dynamic:Base">
    <value bean="Base:1"/>
    <value bean="Derive:2"/>
</variable>
```

要点：

- `extendable="true"` 表示允许该 Bean 被继承。
- `dynamic:Base` 指定动态类型的基类约束，运行时只能取已注册的子类实例。
- `<value bean="Base:1"/>` 注册具体子类及其类型编号。

## Unity 接入

### 目录结构

```
Assets/
├── Zeze/
│   ├── Gen/              # 自动生成的代码（勿手动修改）
│   └── ByteBuffer.cs     # 序列化核心
└── Scripts/
    └── GameLogic/        # 业务逻辑
```

### 基本使用

```csharp
// 构造一个生成的 Bean 实例
var value = new demo.Module1.Value();
value.int1 = 42;
value.vector3 = new Zeze.Serialize.Vector3(1.0f, 2.0f, 3.0f);

// 编码到 ByteBuffer
var bb = new Zeze.ByteBuffer();
value.Encode(bb);
```

## 接入指南

按以下步骤完成接入：

1. **编写 `solution.xml`**：定义模块、Bean、协议与数据表。
2. **运行代码生成工具**：输出 `Gen/` 目录下的 C# 代码。
3. **复制文件到 Unity**：将生成代码与 C# 运行时库复制到 Unity 的 `Assets/` 目录。
4. **编写业务逻辑**：使用生成的 Bean 与 Protocol 类进行编解码和收发。
5. **建立网络连接**：通过 WebSocket 或 TCP 与 Linkd 通信。
6. **参考序列化格式**：确保编解码字段顺序与格式一致。

## 相关文档

- [序列化格式](./serialize.md) —— 二进制编解码细节与字段顺序。
- [网络架构](./arch-net.md) —— Linkd、网络层与连接管理。
- [solution.xml 参考](./solution-xml.md) —— 配置文件完整语法。
