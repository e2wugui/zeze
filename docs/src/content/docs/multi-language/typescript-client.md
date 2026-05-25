---
title: TypeScript 客户端接入
sidebar:
  order: 2
---

Zeze TypeScript 客户端库位于 `TypeScript/` 目录，提供与 Java 服务端通信的二进制序列化能力。该库主要面向浏览器或 Node.js 环境的游戏客户端。

## 库结构

| 文件 | 说明 |
|------|------|
| `ByteBuffer.ts` | 二进制编解码核心，兼容 Java 端序列化格式 |
| `package.json` | 项目配置，定义依赖和构建脚本 |
| `tsconfig.json` | TypeScript 编译配置 |

## ByteBuffer 编解码

**ByteBuffer** 是 TypeScript 端的序列化核心类，与 Java 的 `Zeze.Serialize.ByteBuffer` 和 C++ 的 `ByteBuffer` 使用完全相同的二进制格式。

### 编译与使用

```bash
cd TypeScript/

# 安装依赖
npm install --save-dev typescript

# 编译
npm run build
# 或直接使用 tsc
node_modules\.bin\tsc.cmd
```

编译配置（`tsconfig.json`）使用 ES2020 目标和 CommonJS 模块格式，开启严格模式。如需在浏览器中使用，可配合 webpack 或 esbuild 等打包工具。

### 在代码中引用

```typescript
import { ByteBuffer } from './ByteBuffer';
```

### 基本使用

```typescript
const bb = new ByteBuffer();

// 写入
bb.WriteBool(true);
bb.WriteInt(42);
bb.WriteLong(123456789n);
bb.WriteString("hello");

// 读取
const b = bb.ReadBool();       // true
const i = bb.ReadInt();        // 42
const l = bb.ReadLong();       // 123456789n
const s = bb.ReadString();     // "hello"
```

### 变长编码

ByteBuffer 使用 Zeze 自定义的变长整数编码：

- **WriteLong / ReadLong**：有符号 64 位整数，使用 1-9 字节变长编码
- **WriteUInt / ReadUInt**：无符号整数，使用 1-5 字节变长编码
- **WriteInt4 / ReadInt4**：固定 4 字节编码，用于协议头

### 数据类型映射

| Zeze 类型 | TypeScript 类型 | 读方法 | 写方法 |
|-----------|----------------|--------|--------|
| bool | boolean | ReadBool() | WriteBool() |
| byte | number | ReadByte() | WriteByte() |
| short/int/long | number / bigint | ReadShort/Int/Long() | WriteShort/Int/Long() |
| float | number | ReadFloat() | WriteFloat() |
| double | number | ReadDouble() | WriteDouble() |
| string | string | ReadString() | WriteString() |
| binary | Uint8Array | ReadBytes() | WriteBytes() |

## 接入指南

1. 将 `TypeScript/` 目录下的 `.ts` 文件复制到项目中
2. 安装 TypeScript 依赖：`npm install --save-dev typescript`
3. 根据目标环境配置 `tsconfig.json`（浏览器或 Node.js）
4. 使用 ByteBuffer 编解码与服务器通信的二进制数据
5. 网络层可使用 WebSocket（浏览器）或 `net` 模块（Node.js）
6. 参考 [序列化](../core/serialize/) 了解完整的编码规范

## 注意事项

- 运行测试需设置 `NODE_PATH=.` 环境变量，然后执行 `node app.js`
- `long` 类型在 TypeScript 中使用 `bigint`，注意与 `number` 的类型兼容性
- 序列化格式与 Java/C++ 完全一致，可跨语言互操作
