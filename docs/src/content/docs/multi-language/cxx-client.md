---
title: C++ 客户端接入
sidebar:
  order: 1
---

Zeze C++ 客户端库位于 `cxx/` 目录，提供与 Java 服务端通信的完整能力，包括二进制序列化、网络连接管理和协议编解码。该库编译为静态库 **zezecxx.a**，可直接集成到游戏客户端项目中。

## 库结构

| 文件 | 说明 |
|------|------|
| `ByteBuffer.h/cpp` | 二进制编解码核心，支持所有 Zeze 数据类型 |
| `Net.h/cpp` | 网络层，封装 Socket、Service、Selector |
| `Protocol.h/cpp` | 协议基类，包含编码/解码和派发逻辑 |
| `Rpc.h` | RPC 模板类，支持异步调用和超时 |
| `Bean.h` | Bean 基类，包括 `EmptyBean` 和 `DynamicBean` |
| `Vector.h` | Vector2/3/4、Quaternion 等游戏向量类型 |
| `security.h/cpp` | AES 加解密 |
| `rfc2118.h/cpp` | MPPC 压缩 |
| `dh.h/cpp` | Diffie-Hellman 密钥交换 |

## ByteBuffer 编解码

**ByteBuffer** 是序列化核心类，使用 Zeze 自定义的变长整数编码格式，与 Java 端完全兼容。

```cpp
#include "ByteBuffer.h"

Zeze::ByteBuffer bb(256);
bb.WriteBool(true);
bb.WriteInt(42);
bb.WriteLong(123456789LL);
bb.WriteString("hello");
bb.WriteFloat(3.14f);

bool b = bb.ReadBool();         // true
int i = bb.ReadInt();           // 42
int64_t l = bb.ReadLong();      // 123456789
std::string s = bb.ReadString(); // "hello"
float f = bb.ReadFloat();       // 3.14f
```

`WriteLong`/`ReadLong` 使用变长编码（1-9 字节），`WriteInt4`/`ReadInt4` 是固定 4 字节编码（用于协议头）。Bean 字段使用 **Tag** 编码（高 4 位类型，低 4 位字段 ID 增量），支持向前兼容的字段跳过（`SkipUnknownField`）。

类型常量：`INTEGER`(0), `FLOAT`(1), `DOUBLE`(2), `BYTES`(3), `LIST`(4), `MAP`(5), `BEAN`(6), `DYNAMIC`(7), `VECTOR2`(8)~`VECTOR4`(12)。

## Net 网络层

**Service** 负责连接建立、协议注册和派发，**Socket** 封装平台 Socket 操作，内部使用 epoll(Linux) / kqueue(macOS) / wepoll(Windows) 实现异步 IO。

```cpp
#include "Net.h"

Zeze::Net::Startup();  // 全局初始化

Zeze::Net::Service service;
service.AddProtocolFactory(typeId, Zeze::Net::Service::ProtocolFactoryHandle(
    []() { return new MyProtocol(); },
    [](Zeze::Net::Protocol* p) -> int64_t { return 0; }
));

service.Connect("127.0.0.1", 8080, 5);  // 客户端连接
service.Listen("0.0.0.0", 8080);        // 服务器监听

Zeze::Net::Cleanup();  // 程序退出时清理
```

### 握手与安全

```cpp
service.SetHandshakeOptions(
    Zeze::Net::eEncryptTypeAesNoSecureIp,  // 加密类型
    Zeze::Net::eCompressTypeMppc,          // S2C 压缩
    Zeze::Net::eCompressTypeDisable        // C2S 压缩
);
service.SetKeepConfig(10, 25, 60);  // 心跳：检查周期, 发送超时, 接收超时（秒）
```

## Protocol 协议

自定义协议继承 `ProtocolWithArgument`：

```cpp
class MyProtocol : public Zeze::Net::ProtocolWithArgument<MyArgument> {
public:
    int ModuleId() const override { return 1; }
    int ProtocolId() const override { return 100; }
};
```

TypeId = `((int64_t)ModuleId << 32) | (unsigned int)ProtocolId`，需与 Java 端一致。

### RPC 调用

```cpp
MyRpc rpc;
rpc.Argument->setValue(42);
rpc.SendAsync(socket, [](Zeze::Net::Protocol* p) -> int64_t {
    auto response = (MyRpc*)p;
    return 0;
}, 5000);  // 超时 5 秒
```

## 编译

```bash
cd cxx/
make all    # 生成 zezecxx.a
make clean  # 清理
```

编译选项：C++11 标准，`-O2` 优化，`-pthread` 多线程。Lua 绑定通过 `ToLua.h`/`ToLuaService.h` 实现。

## 接入指南

1. 将 `cxx/` 源文件加入项目，或直接链接 `zezecxx.a`
2. 程序入口调用 `Startup()`，退出时调用 `Cleanup()`
3. 继承 `Service` 创建自己的 Service，注册协议工厂
4. 使用 `Connect()` 连接 [Linkd](../architecture/arch/) 服务器
5. 在 `OnHandshakeDone` 回调后开始业务通信
6. 参考 [序列化](../core/serialize/) 了解完整的数据类型映射
