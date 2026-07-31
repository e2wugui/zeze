---
title: "TypeScript 客户端接入"
description: "使用 Zeze 命名空间下的 ByteBuffer 实现与 Zeze Java 服务端通信的二进制序列化，面向浏览器或 Node.js 客户端。"
category: reference
order: 62
---

# TypeScript 客户端接入

> 本文档面向 TypeScript 客户端开发者，说明如何使用 `TypeScript/` 目录提供的序列化库实现与 Zeze Java 服务端通信的二进制序列化，适用于浏览器或 Node.js 游戏客户端。

## 概述

`TypeScript/` 目录提供与 Zeze Java 服务端通信的二进制序列化能力。**核心是 `Zeze/zeze.ts` 文件中的 `ByteBuffer` 类**（嵌套在 `Zeze` 命名空间内），其编解码格式与 Java 端完全兼容，保证跨语言互操作。该库面向运行在浏览器或 Node.js 上的客户端。

> ⚠️ **注意文件名**：库中**没有独立的 `ByteBuffer.ts` 文件**。`ByteBuffer` 类定义在 `TypeScript/Zeze/zeze.ts` 中，导出在 `Zeze` 模块下。直接 `import { ByteBuffer } from './ByteBuffer'` 会找不到模块。

库结构（关键文件）：

| 文件 | 说明 |
| --- | --- |
| `Zeze/zeze.ts` | 序列化核心：包含 `ByteBuffer`、`Bean` 等类（均在 `Zeze` 命名空间下），兼容 Java 端格式。 |
| `Zeze/app.ts` | App / 网络通信相关。 |
| `Zeze/test.ts` | 测试代码。 |
| `package.json` | npm 包配置（`typescript: ^3.9.10`）。 |
| `tsconfig.json` | TypeScript 编译配置。 |

## 编译

```bash
cd TypeScript/

# 安装 TypeScript（开发依赖）
npm install --save-dev typescript

# 编译
npm run build
# 或直接调用 tsc
node_modules\.bin\tsc.cmd
```

`tsconfig.json` 关键配置：

| 配置项 | 取值 |
| --- | --- |
| `target` | `ES2020` |
| `module` | `CommonJS` |
| `strict` | 严格模式 |

> 浏览器端需配合打包工具（如 webpack / esbuild）使用。

## 引用

`ByteBuffer` 在 `Zeze` 命名空间下，导入时连同命名空间一起引入：

```typescript
import { Zeze } from './Zeze/zeze';
//                       ^ 从 Zeze/zeze.ts 导入，而非 ByteBuffer.ts
```

## 基本使用

```typescript
const bb = new Zeze.ByteBuffer();

// 写入
bb.WriteBool(true);
bb.WriteInt(42);
bb.WriteLong(123456789n);
bb.WriteString("hello");

// 读取
const b: boolean = bb.ReadBool();
const i: number  = bb.ReadInt();
const l: bigint  = bb.ReadLong();   // 返回 bigint
const s: string  = bb.ReadString();
```

## 变长编码

`ByteBuffer` 提供多种变长与定长编码接口：

| 接口 | 说明 |
| --- | --- |
| `WriteLong` / `ReadLong` | 有符号 64 位整数，1-9 字节变长。`ReadLong` 返回 `bigint`。 |
| `WriteUInt` / `ReadUInt` | 无符号整数，1-5 字节变长。 |
| `WriteInt4` / `ReadInt4` | 固定 4 字节，用于协议头。 |

## 数据类型映射

Zeze 数据类型与 TypeScript 类型的对应关系：

| Zeze 类型 | TypeScript 类型 |
| --- | --- |
| `bool` | `boolean` |
| `byte` | `number` |
| `short` / `int` | `number` |
| `long` | `number` 或 `bigint` |
| `float` / `double` | `number` |
| `string` | `string` |
| `binary` | `Uint8Array` |

> `long` 类型在序列化时使用 `bigint`，业务代码需注意其与 `number` 之间的兼容转换。

## 接入指南

按以下步骤完成接入：

1. **复制源文件**：将 `TypeScript/` 目录下的 `.ts` 文件复制到项目中。
2. **安装依赖**：执行 `npm install --save-dev typescript`。
3. **配置编译**：配置 `tsconfig.json`（目标、模块、严格模式）。
4. **编解码**：使用 `ByteBuffer` 进行二进制编解码。
5. **网络层**：浏览器端使用 WebSocket，Node.js 端使用 `net` 模块与服务端通信。
6. **参考序列化**：确保编解码字段顺序与格式一致。

## 注意事项

- **运行测试**：执行测试前需设置 `NODE_PATH=.`，再运行 `node app.js`。
- **`long` 与 `bigint`**：`long` 类型使用 `bigint` 表示，注意与 `number` 的兼容转换。
- **跨语言一致性**：序列化格式与 Java / C++ 客户端完全一致，可放心跨语言互操作。

## 相关文档

- [序列化格式](./serialize.md) —— 二进制编解码细节与字段顺序。
- [C++ 客户端接入](./client-cpp.md) —— C++ 端实现，可对照参考。
