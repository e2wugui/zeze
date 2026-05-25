---
title: C# 客户端接入
sidebar:
  order: 3
---

Zeze C# 客户端主要面向 Unity 游戏引擎，通过 **solution.xml** 定义数据结构和协议，自动生成 C# 代码，实现与 Java 服务端的跨语言通信。

## solution.xml 定义

`confcs/solution.xml` 描述模块（Module）、Bean 结构、协议和数据表。

### 项目配置

```xml
<project name="confcs" GenDir="confcs/Gen" SrcDir="confcs"
         platform="conf+cs" IncludeAllModules="true" MacroEditor="UNITY_EDITOR">
</project>
```

- **GenDir**：生成的 C# 代码输出目录（`confcs/Gen`）
- **platform**：`conf+cs` 表示配置 + C# 客户端
- **IncludeAllModules**：包含 solution 中所有模块定义
- **MacroEditor**：Unity 编辑器宏，用于条件编译

### 数据类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `int`, `long`, `short`, `byte` | 整数类型 | `type="int"` |
| `bool` | 布尔 | `type="bool"` |
| `float`, `double` | 浮点 | `type="float"` |
| `string`, `binary` | 字符串/二进制 | `type="string"` |
| `list[T]`, `set[T]` | 列表/集合 | `type="list[int]"` |
| `map[K,V]` | 映射 | `type="map[long,Value]"` |
| `vector2`~`vector4`, `quaternion` | 游戏向量 | `type="vector3"` |
| `dynamic` | 动态 Bean | `type="dynamic"` |
| `array` | 定长数组 | `type="array" value="vector3"` |

### 协议定义

```xml
<protocol name="Protocol1" argument="Value" handle="server,clientscript"/>
<rpc name="Rpc1" argument="Value" result="Value" handle="server"/>
<protocol name="ProtocolNoProcedure" NoProcedure="true" handle="server"/>
```

**handle** 指定处理端：`server` 服务端处理，`clientscript` 客户端脚本处理。

## 代码生成

Zeze 代码生成工具根据 solution.xml 在 `confcs/Gen/` 下生成：Bean 类（序列化/反序列化）、Protocol/RPC 类（编解码和发送）、Table 类（客户端本地数据表）。

> **重要**：`Gen/` 目录下的文件是自动生成的，不要手动编辑。

### Dynamic Bean

```xml
<bean name="Base" extendable="true">
    <variable id="1" name="baseInt" type="int"/>
</bean>
<bean name="Derive" base="Base" extendable="true">
    <variable id="1" name="deriveInt" type="int"/>
</bean>
<bean name="Dynamic">
    <variable id="1" name="dyn" type="dynamic:Base">
        <value bean="Base:1"/>
        <value bean="Derive:2"/>
    </variable>
</bean>
```

`extendable="true"` 允许继承，`dynamic:Base` 指定动态类型的基类约束。

## Unity 接入

```
Assets/
  ├─ Zeze/Gen/          # 自动生成的代码
  ├─ Zeze/ByteBuffer.cs # 序列化核心
  └─ Scripts/GameLogic/ # 业务逻辑
```

```csharp
var value = new demo.Module1.Value();
value.int1 = 42;
value.vector3 = new Zeze.Vector3(1.0f, 2.0f, 3.0f);

var bb = new Zeze.ByteBuffer();
value.Encode(bb);   // 序列化
```

## 接入指南

1. 编写 `solution.xml` 定义数据模型和协议
2. 运行 Zeze 代码生成工具，输出到 `Gen/` 目录
3. 将生成代码和 C# 运行时库复制到 Unity `Assets/` 目录
4. 使用生成的 Bean 和 Protocol 类进行数据编解码
5. 通过 WebSocket 或 TCP 与 [Linkd](../architecture/arch/) 通信
6. 参考 [序列化](../core/serialize/) 了解完整的编码规范
