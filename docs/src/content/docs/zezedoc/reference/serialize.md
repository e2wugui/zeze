---
title: "序列化协议"
description: "Zeze 紧凑二进制 TLV 编码、类型 ID、Tag 编码与 ByteBuffer API 速查"
category: reference
order: 5
---

本文是 Zeze **序列化协议**的完整参考——覆盖紧凑二进制 TLV 编码、4-bit 类型 ID、整数变长编码、Tag 编码、容器编码、向量编码、跨语言一致性规则和 `ByteBuffer` API，供写代码时随查随用。数据模型见 [Bean 数据模型](./bean.md)，概念讲解见 [定义数据](../manual/03-defining-data.md)。

## 概述

Zeze 使用**紧凑二进制 TLV 编码**（Tag-Length-Value 思路），Java 核心类是 `Zeze.Serialize.ByteBuffer`。它同时用于：

| 用途 | 说明 |
|------|------|
| 网络传输 | Protocol / Rpc 的参数与结果 |
| 持久化 | Table 数据落库 |

**保证 Java / C++ / TypeScript 多语言二进制一致**——同一份数据在任何语言下编码结果完全相同，可互相解析。

### 核心接口

| 接口 / 类 | 说明 |
|-----------|------|
| `Serializable` | 核心接口，实现 `encode` / `decode` |
| `IByteBuffer` | 只读接口，提供各类读取方法 |
| `ByteBuffer` | 可读写实现，实现 `IByteBuffer` |

---

## 4-bit 类型 ID

每个值的高 4 位编码其类型（0-15）：

| Type ID | 类型 | 说明 |
|---------|------|------|
| 0 | INTEGER | 有符号整数（`byte`/`short`/`int`/`long`/`bool`） |
| 1 | FLOAT | 32 位浮点 |
| 2 | DOUBLE | 64 位浮点 |
| 3 | BYTES | `binary` / `string` |
| 4 | LIST | `list` / `set` |
| 5 | MAP | `map` |
| 6 | BEAN | 普通 Bean |
| 7 | DYNAMIC | 动态 Bean |
| 8 | VECTOR2 | 两 float |
| 9 | VECTOR2INT | 两 int |
| 10 | VECTOR3 | 三 float |
| 11 | VECTOR3INT | 三 int |
| 12 | VECTOR4 / 四元数 | 四 float |
| 13-15 | 保留 | 扩展用 |

---

## 整数编码

### 有符号 varint（支持 64 位补码）

有符号整数采用变长编码，正负数各自独立变长，**小值只需 1 字节**：

| 字节数 | 取值范围 |
|--------|----------|
| 1 | 小值 |
| 2 | |
| ... | |
| 9 | 最大范围 |

> bool 与有符号整数兼容：`false = 0`、`true = 1`。

### 无符号整数（仅用于长度 / 数量）

| 字节数 | 取值范围 |
|--------|----------|
| 1-5 | 用于编码长度、集合元素数量等 |

---

## 浮点编码

| 类型 | 字节数 | 编码 |
|------|--------|------|
| `float` | 4 | IEEE 754，**小端** |
| `double` | 8 | IEEE 754，**小端** |

---

## binary 与 string

| 类型 | 编码方式 |
|------|----------|
| `binary` | 先写**无符号整数长度**，再写原始字节 |
| `string` | 先转 UTF-8，再按 `binary` 编码（长度 + UTF-8 字节） |

---

## Tag 编码

Tag 编码字段 id 与类型，格式为 `iiii tttt`（高位 i，低位 t）：

| 组成 | 位 | 说明 |
|------|----|------|
| t（低 4 位） | 0-3 | 类型枚举（4-bit Type ID） |
| i（高 4 位） | 4-7 | 距上个字段 id 的增量 |

### i 的取值规则

| i 值 | 含义 |
|------|------|
| 0 | 特殊标签：t=0 结束标签（`0x00`）、t=1 结束当前层/继承切换、t=2-15 保留 |
| 1-14 | 距上个字段 id 的增量（首个字段为 id 本身） |
| 15 | 附加无符号整数 x，实际增量为 `15 + x` |

---

## 容器编码

### 序列（list / set）

格式 `nnnn tttt`：

| n 值 | 含义 |
|------|------|
| 0-14 | 元素数量为 n |
| 15 | 附加无符号整数 x，数量为 `15 + x` |

低 4 位 t 为元素类型，随后依次写各元素。

### 关联（map）

格式 `kkkk vvvv`（高 4 位 k 键类型，低 4 位 v 值类型），随后写无符号数量，再依次写键值对。

---

## 向量编码

| 类型 | 编码 |
|------|------|
| `vector2` | 两 float（8 字节） |
| `vector2int` | 两有符号 varint |
| `vector3` | 三 float |
| `vector3int` | 三有符号 varint |
| `vector4` | 四 float |

---

## Bean 编码

Bean 编码为 **Tag + 值**序列，以 `0x00`（结束标签）结尾：

| 规则 | 说明 |
|------|------|
| 字段顺序 | 按 id 从小到大 |
| id 范围 | `[1, 0x7fffffff]` |
| 默认值省略 | 字段等于默认值时可省略不写 |
| 反序列化 | 先重置为默认值，再读取存在的字段 |
| 继承 | 先子类，插入 `0x01`（结束当前层/继承切换），再父类 |

### 默认值定义

| 类型 | 默认值 |
|------|--------|
| 数值 | `0` |
| `binary` / `string` | 长度 0 |
| 容器 | 0 个元素 |
| Bean | 全字段默认值 |
| dynamic | 未定义（EmptyBean） |

---

## dynamic 编码

| 顺序 | 内容 |
|------|------|
| 1 | 有符号整数 typeId |
| 2 | Bean 完整内容 |

---

## 跨语言一致性

| 维度 | 规则 |
|------|------|
| 浮点 | 小端 |
| 整数（变长） | 大端组 |
| 字符串 | UTF-8 |

### 类型自动转换

反序列化时兼容类型间自动转换：

| 转换 | 说明 |
|------|------|
| INTEGER / FLOAT / DOUBLE 之间 | 数值类型互换 |
| binary ↔ string | |
| list ↔ set | |
| bean ↔ dynamic | |

### 未知字段处理

| 设置 | 行为 |
|------|------|
| `IByteBuffer.IGNORE_INCOMPATIBLE_FIELD = false`（默认） | 遇到未知字段**抛异常** |
| `IByteBuffer.IGNORE_INCOMPATIBLE_FIELD = true` | 跳过未知字段（前向兼容） |

---

## ByteBuffer API

### 创建

| 方法 | 说明 |
|------|------|
| `ByteBuffer.Allocate()` | 分配默认容量 |
| `ByteBuffer.Allocate(256)` | 分配指定容量 |
| `ByteBuffer.Wrap(bytes)` | 包装已有字节数组 |
| `ByteBuffer.Wrap(bytes, offset, length)` | 包装指定区间 |

### 写入

| 方法 | 说明 |
|------|------|
| `WriteBool(v)` | bool |
| `WriteByte(v)` | byte |
| `WriteInt(v)` | 有符号 varint |
| `WriteLong(v)` | 有符号 varint |
| `WriteUInt(v)` | 无符号（长度/数量） |
| `WriteULong(v)` | 无符号 |
| `WriteFloat(v)` | 小端 4 字节 |
| `WriteDouble(v)` | 小端 8 字节 |
| `WriteString(s)` | UTF-8（长度 + 字节） |
| `WriteBytes(bytes)` | 长度 + 内容 |
| `WriteBinary(bin)` | binary |
| `WriteVector2(v)` / `WriteVector3(v)` / `WriteVector4(v)` | float 向量 |
| `WriteVector2Int(v)` / `WriteVector3Int(v)` | int 向量 |
| `WriteQuaternion(v)` | 四元数（内部转调 `WriteVector4`） |
| `WriteTag(lastVarId, varId, type)` | 写 Tag，**返回当前 varId** |

### 读取

| 方法 | 说明 |
|------|------|
| `ReadBool()` | |
| `ReadByte()` | |
| `ReadInt()` | 有符号 varint |
| `ReadLong()` | 有符号 varint |
| `ReadUInt()` | 无符号 |
| `ReadULong()` | 无符号 |
| `ReadFloat()` | 小端 |
| `ReadDouble()` | 小端 |
| `ReadString()` | UTF-8 |
| `ReadBytes()` | 长度 + 内容 |
| `ReadBinary()` | binary |
| `ReadByteBuffer()` | 读取子 ByteBuffer |

带 Tag 的类型安全读取会检查类型标识，并支持兼容转换。

### 容量与状态

| 方法 | 说明 |
|------|------|
| `size()` | 已写入字节数 |
| `isEmpty()` | 是否为空 |
| `Reset()` | 重置读写位置 |
| `Copy()` | 复制一份 |
| `Compact()` | 压缩（整理） |

---

## 完整 Bean 序列化示例

下面演示一个 Bean 的 encode / decode：

```java
// 假设生成的 Bean（XML 声明）：
// <bean name="Score">
//     <variable id="1" name="level" type="int"/>
//     <variable id="2" name="name"  type="string"/>
//     <variable id="3" name="coins" type="long"/>
// </bean>

Score score = new Score();
score.setLevel(10);
score.setName("alice");
score.setCoins(5000L);

// encode：自动序列化为紧凑二进制
ByteBuffer buf = ByteBuffer.Allocate();
score.encode(buf);
byte[] bytes = buf.CopyBytes();   // 得到字节数组，可网络发送或落库

// decode：从字节数组还原
ByteBuffer in = ByteBuffer.Wrap(bytes);
Score restored = new Score();
restored.decode(in);

System.out.println(restored.getLevel());  // 10
System.out.println(restored.getName());   // alice
System.out.println(restored.getCoins());  // 5000
```

字段按 id 从小到大编码，等于默认值的字段被省略；反序列化时先重置默认值再读取，因此旧数据缺字段也能正确恢复（版本兼容，见 [Bean 数据模型](./bean.md)）。

## 相关文档

- [Bean 数据模型](./bean.md) — variable id、版本兼容、类型自动转换
- [定义数据](../manual/03-defining-data.md) — 序列化「免费」的概念讲解
- [C++ 客户端](./client-cpp.md) — 多语言二进制一致
- [TypeScript 客户端](./client-typescript.md) — 多语言二进制一致
