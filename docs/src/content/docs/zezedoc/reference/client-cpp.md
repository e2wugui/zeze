---
title: "C++ 客户端接入"
description: "使用 zezecxx 静态库实现二进制序列化、网络连接管理与协议编解码，与 Zeze Java 服务端通信。"
category: reference
order: 61
---

# C++ 客户端接入

> 本文档面向 C++ 客户端开发者，说明如何使用 `cxx/` 目录提供的 `zezecxx` 库实现二进制序列化、网络连接管理与协议编解码，与 Zeze Java 服务端完成通信。

## 概述

`cxx/` 目录提供与 Zeze Java 服务端通信的完整能力，包含二进制序列化、网络连接管理、协议编解码等模块，编译后产出静态库 `zezecxx.a`。该库适用于原生 C++ 游戏客户端或服务，序列化格式与 Java / C# / TypeScript 客户端完全一致，保证跨语言互操作。

## 库结构

`cxx/` 目录主要源文件及其职责：

| 文件 | 说明 |
| --- | --- |
| `ByteBuffer.h` / `ByteBuffer.cpp` | 二进制编解码核心。 |
| `Net.h` / `Net.cpp` | 网络层，包含 Socket / Service / Selector（基于 epoll / kqueue / wepoll）。 |
| `Protocol.h` / `Protocol.cpp` | 协议基类，负责编解码与派发。 |
| `Rpc.h` | RPC 模板，支持异步与超时。 |
| `Bean.h` | Bean 基类（`EmptyBean`、`DynamicBean`）。 |
| `Vector.h` | `Vector2` / `Vector3` / `Vector4` / `Quaternion`。 |
| `security.h` / `security.cpp` | AES 加密。 |
| `rfc2118.h` / `rfc2118.cpp` | MPPC 压缩。 |
| `dh.h` / `dh.cpp` | DH 密钥交换。 |

## ByteBuffer 编解码

`Zeze::ByteBuffer` 提供基础读写接口。

```cpp
Zeze::ByteBuffer bb(256);

// 写入
bb.WriteBool(true);
bb.WriteInt(42);          // 1-9 字节变长
bb.WriteLong(123456789LL); // 1-9 字节变长
bb.WriteString("hello");
bb.WriteFloat(3.14f);

// 读取
bool b     = bb.ReadBool();
int i      = bb.ReadInt();
int64_t l  = bb.ReadLong();
std::string s = bb.ReadString();
float f    = bb.ReadFloat();
```

固定长度接口：

| 接口 | 说明 |
| --- | --- |
| `WriteInt4` / `ReadInt4` | 固定 4 字节，用于协议头。 |

### Bean 字段 Tag 编码

每个 Bean 字段使用 1 字节 Tag 编码：

- **高 4 位**：字段类型常量。
- **低 4 位**：字段 ID 增量（相对上一个字段的差值）。

### 类型常量

`ByteBuffer` 使用的类型常量：

| 常量 | 值 | 类型 |
| --- | --- | --- |
| `INTEGER` | 0 | 整数 |
| `FLOAT` | 1 | 单精度浮点 |
| `DOUBLE` | 2 | 双精度浮点 |
| `BYTES` | 3 | 二进制 |
| `LIST` | 4 | 列表 |
| `MAP` | 5 | 映射 |
| `BEAN` | 6 | Bean |
| `DYNAMIC` | 7 | 动态 Bean |
| `VECTOR2` | 8 | 二维向量（float） |
| `VECTOR2INT` | 9 | 二维向量（int） |
| `VECTOR3` | 10 | 三维向量（float） |
| `VECTOR3INT` | 11 | 三维向量（int） |
| `VECTOR4` | 12 | 四维向量（float，亦用于 Quaternion） |

### 前向兼容

`SkipUnknownField` 用于跳过未知字段，保证协议升级时的前向兼容性。

## 网络层（Net）

### 全局初始化与清理

```cpp
Zeze::Net::Startup();   // 全局初始化
// ... 业务逻辑 ...
Zeze::Net::Cleanup();   // 全局清理
```

### Service

`Service` 负责连接管理与协议派发：

```cpp
// 注册协议工厂（类型 ID + 工厂 + 处理器）
service.AddProtocolFactory(typeId, ProtocolFactoryHandle{factory, handler});

// 主动连接（地址、端口、超时秒）
service.Connect("127.0.0.1", 8080, 5);

// 监听（必须传 host 和 port，非无参）
service.Listen("::", 7777);   // 返回 std::string
```

### 握手与安全

连接建立后进行握手，可配置加密与压缩选项：

```cpp
// 加密类型、压缩类型
service.SetHandshakeOptions(eEncryptTypeAesNoSecureIp,
                            eCompressTypeMppc,
                            eCompressTypeDisable);
```

| 选项 | 说明 |
| --- | --- |
| `eEncryptTypeAesNoSecureIp` | AES 加密，不校验 IP 安全性。 |
| `eCompressTypeMppc` | 启用 MPPC 压缩。 |
| `eCompressTypeDisable` | 禁用压缩。 |

### 心跳配置

```cpp
// 检查周期、发送超时、接收超时（秒）
service.SetKeepConfig(10, 25, 60);
```

| 参数 | 说明 |
| --- | --- |
| 检查周期 | 心跳检测的触发间隔（10 秒）。 |
| 发送超时 | 发送心跳后等待确认的超时（25 秒）。 |
| 接收超时 | 长时间未接收数据的超时（60 秒）。 |

## 协议（Protocol）

自定义协议需继承 `ProtocolWithArgument<MyArgument>`，并实现模块号与协议号：

```cpp
class MyProtocol : public Zeze::Net::ProtocolWithArgument<MyArgument> {
public:
    static constexpr int ModuleId()   { return 1; }
    static constexpr int ProtocolId() { return 100; }

    // TypeId 与 Java 端完全一致
    static constexpr int64_t TypeId() {
        return ((int64_t)ModuleId() << 32) | (unsigned)ProtocolId();
    }
};
```

> **关键约定**：`TypeId` 由 `ModuleId` 与 `ProtocolId` 组合而成，定义方式与 Java 服务端完全一致，确保跨语言协议匹配。

## RPC

```cpp
MyRpc rpc;
rpc.Argument->setValue(42);

// 异步发送：Socket、回调、超时（毫秒）
rpc.SendAsync(socket, [](MyRpc* r) {
    // 处理返回结果
}, 5000);
```

## 编译

```bash
cd cxx/
make all      # 编译产出 zezecxx.a
make clean    # 清理
```

编译要求：**C++11**、优化级别 `-O2`、链接 `-pthread`。

### Lua 绑定

通过以下头文件提供 Lua 绑定支持：

- `ToLua.h`
- `ToLuaService.h`

## 接入指南

按以下步骤完成接入：

1. **引入库**：将 `cxx/` 源文件加入项目，或链接预编译的 `zezecxx.a`。
2. **初始化**：调用 `Startup()` / `Cleanup()` 进行全局初始化与清理。
3. **注册协议**：继承 `Service`，注册所需协议工厂与处理器。
4. **连接 Linkd**：调用 `Connect()` 建立与服务端的连接。
5. **业务通信**：在 `OnHandshakeDone` 回调后开始收发业务数据。
6. **参考序列化**：确保编解码字段顺序与格式一致。

## 相关文档

- [序列化格式](./serialize.md) —— 二进制编解码细节与字段顺序。
- [网络架构](./arch-net.md) —— Linkd、网络层与连接管理。
