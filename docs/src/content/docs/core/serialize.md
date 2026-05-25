---
title: "序列化协议"
sidebar:
  order: 2
---

Zeze 内置了一套紧凑的二进制序列化方案，采用自定义的 **Tag-Length-Value** 编码格式，Java 端核心实现为 `Zeze.Serialize.ByteBuffer`。该方案同时用于网络协议传输与持久化存储，确保 Java、C++、TypeScript 等多语言客户端的二进制数据完全一致。

## 核心接口

序列化主要涉及三个类型：

- **`Zeze.Serialize.Serializable`** — 可序列化接口，定义 `encode(ByteBuffer)` / `decode(IByteBuffer)` 方法
- **`Zeze.Serialize.IByteBuffer`** — 只读接口，提供所有反序列化读取方法
- **`Zeze.Serialize.ByteBuffer`** — 可读写实现，同时实现 `IByteBuffer`

## 类型编码表（4-bit Type ID）

每个字段的类型用 **4 个 bit** 表示，编码表如下：

| Type ID | 常量名 | 含义 |
|---------|--------|------|
| 0 | `INTEGER` | 有符号整数（byte / short / int / long / bool） |
| 1 | `FLOAT` | 单精度浮点数（float） |
| 2 | `DOUBLE` | 双精度浮点数（double） |
| 3 | `BYTES` | 二进制数据 / 字符串（binary / string） |
| 4 | `LIST` | 序列容器（list / set） |
| 5 | `MAP` | 关联容器（map） |
| 6 | `BEAN` | 结构化对象（→ [bean](./bean)） |
| 7 | `DYNAMIC` | 动态 Bean（dynamic） |
| 8 | `VECTOR2` | 二维浮点向量（float×2） |
| 9 | `VECTOR2INT` | 二维整数向量（int×2） |
| 10 | `VECTOR3` | 三维浮点向量（float×3） |
| 11 | `VECTOR3INT` | 三维整数向量（int×3） |
| 12 | `VECTOR4` | 四维浮点向量 / 四元数（float×4） |
| 13–15 | — | 保留，可自行扩展非标准类型 |

## 整数编码

### 有符号整数（varint）

支持 64 位补码有符号整数的全部值。正数和负数各有独立的变长编码，小值仅占 1 字节：

```
正整数:
  1字节(<  0x               40): 00xx xxxx
  2字节(<  0x             2000): 010x xxxx  +1B
  3字节(<  0x          10 0000): 0110 xxxx  +2B
  4字节(<  0x         800 0000): 0111 0xxx  +3B
  5字节(<  0x      4 0000 0000): 0111 10xx  +4B
  6字节(<  0x    200 0000 0000): 0111 110x  +5B
  7字节(<  0x 1 0000 0000 0000): 0111 1110  +6B
  8字节(<  0x80 0000 0000 0000): 0111 1111  0xxx xxxx  +6B
  9字节(             unlimited): 0111 1111  1xxx xxxx  +7B
负整数:
  1字节(>=-0x               40): 11xx xxxx
  2字节(>=-0x             2000): 101x xxxx  +1B
  3字节(>=-0x          10 0000): 1001 xxxx  +2B
  4字节(>=-0x         800 0000): 1000 1xxx  +3B
  5字节(>=-0x      4 0000 0000): 1000 01xx  +4B
  6字节(>=-0x    200 0000 0000): 1000 001x  +5B
  7字节(>=-0x 1 0000 0000 0000): 1000 0001  +6B
  8字节(>=-0x80 0000 0000 0000): 1000 0000  1xxx xxxx  +6B
  9字节(             unlimited): 1000 0000  0xxx xxxx  +7B
```

### 无符号整数（用于长度和数量）

仅用于序列化长度、元素数量、ID 增量等非负值：

```
1字节(<0x       80): 0xxx xxxx
2字节(<0x     4000): 10xx xxxx  +1B
3字节(<0x  20 0000): 110x xxxx  +2B
4字节(<0x1000 0000): 1110 xxxx  +3B
5字节(   unlimited): 1111 0000  +4B
```

### 布尔（bool）

与有符号整数兼容。序列化时 `false` 当作 0、`true` 当作 1；反序列化时 0 当作 `false`，其余当作 `true`。

## 浮点数

- **float**：按 IEEE 754 标准序列化为**小端**排列的固定 4 字节
- **double**：按 IEEE 754 标准序列化为**小端**排列的固定 8 字节

## 二进制数据与字符串

- **binary**：先写入无符号整数表示字节长度，再写入原始数据
- **string**：先将字符串按 UTF-8 编码，再按 binary 格式写入（即长度 + UTF-8 字节流）

## Tag 编码规则

Bean 的每个字段由一个 **Tag** 字节引导，Tag 编码为 `(高位)iiii tttt(低位)`：

- 低 4 位 `tttt`：字段的类型枚举（见类型编码表）
- 高 4 位 `iiii`：
  - `i = 0`：特殊含义
    - `t = 0`：**结束标签**，表示 Bean 字段序列化结束
    - `t = 1`：**结束当前层标签**，用于继承关系中切换到父类字段
    - `t = 2~15`：保留扩展
  - `i = 1~14`：距上个字段 ID 的增量（首个字段则为字段 ID 本身）
  - `i = 15`：附加一个无符号整数 `x`，用 `15 + x` 表示 ID 增量

```java
// Tag 编码示例（Java）
ByteBuffer bb = ByteBuffer.Allocate();
int lastVarId = 0;
// 写入字段 id=3, type=INTEGER(0)
lastVarId = bb.WriteTag(lastVarId, 3, ByteBuffer.INTEGER);
bb.WriteLong(42);
```

## 容器类型

### 序列容器（list / set）

单字节 `(高位)nnnn tttt(低位)`：

- `t`：元素类型枚举
- `n = 0~14`：元素数量
- `n = 15`：附加一个无符号整数 `x`，用 `15 + x` 表示元素数量

之后按指定类型连续序列化所有元素。

```java
// 写入包含 3 个 int 元素的 list
bb.WriteListType(3, ByteBuffer.INTEGER);
bb.WriteInt(10);
bb.WriteInt(20);
bb.WriteInt(30);
```

### 关联容器（map）

单字节 `(高位)kkkk vvvv(低位)`：

- `k`：键（key）的类型枚举
- `v`：值（value）的类型枚举

之后写入无符号整数表示键值对数量，再按"键值键值..."的顺序连续序列化。

```java
// 写入包含 2 个键值对的 map<int, string>
bb.WriteMapType(2, ByteBuffer.INTEGER, ByteBuffer.BYTES);
bb.WriteInt(1); bb.WriteString("hello");
bb.WriteInt(2); bb.WriteString("world");
```

## 向量类型

| 类型 | 编码方式 |
|------|----------|
| vector2 | 两个连续的 float（共 8 字节） |
| vector2int | 两个连续的有符号整数（varint） |
| vector3 | 三个连续的 float（共 12 字节） |
| vector3int | 三个连续的有符号整数（varint） |
| vector4 / quaternion | 四个连续的 float（共 16 字节） |

## Bean 编码

Bean 按"**Tag + 值**"的序列依次排列，以结束标签（单字节 `0x00`）结尾：

```
[Tag1][Value1][Tag2][Value2]...[0x00]
```

规则：

1. 字段按 **ID 从小到大** 排列，ID 范围 `[1, 0x7fffffff]`
2. 字段值等于**默认值**时可省略该字段的 Tag 和值
3. 反序列化时要求先重置 Bean 所有字段为默认值
4. 继承关系：先序列化子类字段，插入"结束当前层"标签（`0x01`），再序列化父类字段

默认值定义：数值为 0，binary/string 长度为 0，容器元素数量为 0，Bean 所有字段均为默认值，动态 Bean 为未定义（空值）。

## 动态 Bean（dynamic）

编码格式：

1. 有符号整数：动态 Bean 的 **typeId**
2. Bean 的完整序列化内容

```java
// 读取动态 Bean
DynamicBean dynBean = new DynamicBean(...);
dynBean.decode(bb);

// 动态 Bean 与普通 Bean 可互相转换：
// Bean → Dynamic（默认 typeId=0）
// Dynamic → Bean（需注意字段兼容性）
```

## 跨语言一致性保证

Zeze 的二进制编码规则对所有语言实现完全相同，保证以下一致性：

- **字节序**：浮点数使用小端序（little-endian），整数变长编码的有效位按大端排列
- **字符串编码**：统一使用 UTF-8
- **类型自动转换**：反序列化时支持安全的兼容转换
  - `INTEGER` / `FLOAT` / `DOUBLE` 之间可自动转换（注意精度截断）
  - `binary` 和 `string` 之间可互相转换（binary 转 string 时可能因非法 UTF-8 抛出异常）
  - `list` 和 `set` 之间可互相转换（list 转 set 后顺序可能变化）
  - `bean` 可转为 `dynamic`（默认 typeId=0），`dynamic` 也可转为 `bean`（需注意字段兼容）
- **未知字段跳过**：`IByteBuffer.IGNORE_INCOMPATIBLE_FIELD = false`（默认）时，遇到不兼容字段抛出异常；设为 `true` 时自动跳过，实现前向兼容

## ByteBuffer 核心 API

### 创建与初始化

```java
// 分配缓冲区
ByteBuffer bb = ByteBuffer.Allocate();       // 默认容量 16
ByteBuffer bb = ByteBuffer.Allocate(256);    // 指定初始容量

// 包装已有数据（用于读取）
ByteBuffer bb = ByteBuffer.Wrap(bytes);
ByteBuffer bb = ByteBuffer.Wrap(bytes, offset, length);
```

### 基本写入方法

| 方法签名 | 说明 |
|----------|------|
| `WriteBool(boolean b)` | 写入布尔值 |
| `WriteByte(int v)` | 写入单字节 |
| `WriteInt(int v)` | 写入变长有符号整数（int → varint） |
| `WriteLong(long v)` | 写入变长有符号整数（long → varint） |
| `WriteUInt(int v)` | 写入变长无符号整数（32-bit） |
| `WriteULong(long v)` | 写入变长无符号整数（64-bit） |
| `WriteFloat(float v)` | 写入 4 字节浮点数（小端） |
| `WriteDouble(double v)` | 写入 8 字节双精度浮点数（小端） |
| `WriteString(String str)` | 写入 UTF-8 字符串 |
| `WriteBytes(byte[] v)` | 写入二进制数据（长度 + 内容） |
| `WriteBinary(Binary v)` | 写入 Binary 对象 |
| `WriteVector2(Vector2 v)` | 写入二维浮点向量 |
| `WriteVector3(Vector3 v)` | 写入三维浮点向量 |
| `WriteVector4(Vector4 v)` | 写入四维浮点向量 |
| `WriteTag(int lastVarId, int varId, int type)` | 写入字段 Tag，返回当前 varId |

### 基本读取方法

| 方法签名 | 说明 |
|----------|------|
| `boolean ReadBool()` | 读取布尔值 |
| `byte ReadByte()` | 读取单字节 |
| `int ReadInt()` | 读取变长有符号整数（返回 int） |
| `long ReadLong()` | 读取变长有符号整数（返回 long） |
| `int ReadUInt()` | 读取变长无符号整数（32-bit） |
| `long ReadULong()` | 读取变长无符号整数（64-bit） |
| `float ReadFloat()` | 读取 4 字节浮点数 |
| `double ReadDouble()` | 读取 8 字节双精度浮点数 |
| `String ReadString()` | 读取 UTF-8 字符串 |
| `byte[] ReadBytes()` | 读取二进制数据 |
| `Binary ReadBinary()` | 读取 Binary 对象 |
| `ByteBuffer ReadByteBuffer()` | 读取子缓冲区（共享内存） |

### 带 Tag 的类型安全读取

所有带 `int tag` 参数的方法会检查 Tag 中的类型标识，支持自动兼容转换，不兼容时根据 `IGNORE_INCOMPATIBLE_FIELD` 配置决定抛异常还是跳过：

```java
// 在 Bean.decode() 中典型用法
public void decode(IByteBuffer bb) {
    int lastVarId = 0;
    int tag = bb.ReadByte();
    while (tag != 0) {
        int varId = lastVarId + bb.ReadTagSize(tag);
        lastVarId = varId;
        switch (varId) {
        case 1: name = bb.ReadString(tag); break;
        case 2: age  = bb.ReadInt(tag);    break;
        default: bb.SkipUnknownField(tag);  break;
        }
        tag = bb.ReadByte();
    }
}
```

### 容量与状态

| 方法 / 属性 | 说明 |
|-------------|------|
| `int size()` | 当前可读字节数（`WriteIndex - ReadIndex`） |
| `boolean isEmpty()` | 是否无可读数据 |
| `void Reset()` | 重置读写指针为 0 |
| `byte[] Copy()` | 复制可读数据到新数组 |
| `void Compact()` | 将剩余数据移到缓冲区开头 |

## 序列化一个完整 Bean 的示例

```java
public class UserBean implements Serializable {
    private static final int TYPE_ID = 100;

    String name;  // varId=1
    int age;      // varId=2

    @Override
    public void encode(ByteBuffer bb) {
        int lastVarId = 0;
        lastVarId = bb.WriteTag(lastVarId, 1, ByteBuffer.BYTES);
        bb.WriteString(name);
        lastVarId = bb.WriteTag(lastVarId, 2, ByteBuffer.INTEGER);
        bb.WriteInt(age);
        bb.WriteByte(0); // 结束标签
    }

    @Override
    public void decode(IByteBuffer bb) {
        name = null; age = 0; // 先重置为默认值
        int lastVarId = 0;
        int tag = bb.ReadByte();
        while (tag != 0) {
            int varId = lastVarId + bb.ReadTagSize(tag);
            lastVarId = varId;
            switch (varId) {
            case 1: name = bb.ReadString(tag); break;
            case 2: age  = bb.ReadInt(tag);    break;
            default: bb.SkipUnknownField(tag);  break;
            }
            tag = bb.ReadByte();
        }
    }

    @Override
    public int preAllocSize() { return 64; }
}
```
