# 08 GenericBean.decode 递归深度无上限

- **严重级**：P2（main 树唯一消费方为本机 history 数据，非直接远端攻击面）
- **类别**：解码递归深度无上限（与 SkipUnknownField 防护不对称）
- **子系统**：Serialize
- **位置**：`ZezeJava/ZezeJava/src/main/java/Zeze/Serialize/GenericBean.java:52-66`（decode）、`:102-103`（BEAN/DYNAMIC 递归）；对照 `IByteBuffer.java:704-721`（SkipUnknownField 的 MAX_SKIP_UNKNOWN_FIELD_DEPTH=100 先例）
- **状态**：已复核存活（Net/Serialize 分片初审，主审读码定谳）

## 缺陷

`GenericBean.decode/decodeField` 对 BEAN/DYNAMIC（及 LIST/MAP 内嵌）的同型递归完全没有等价
于 `SkipUnknownField` 的深度防护。构造每层 2 字节（BEAN tag + 内层结束符 0x00）的深嵌套流，
几十 KB 即可堆出数万层递归 → `StackOverflowError`——穿透 `catch(Exception)`、无定位信息的
崩溃形态。修复范式已在库内（SkipUnknownField 的 ThreadLocal 深度计数，注释明确记载是为
恶意深嵌套流防 SOE 穿透而加）。

## 代码证据

```java
// GenericBean.java:102-103 —— 每层 2 字节即可递归一层，无深度计数
case ByteBuffer.BEAN -> new GenericBean().decode(bb);
case ByteBuffer.DYNAMIC -> new GenericDynamicBean().decode(bb);

// IByteBuffer.java SkipUnknownField（对照）：MAX_SKIP_UNKNOWN_FIELD_DEPTH = 100 硬上限
```

## 触发场景

corrupted/被篡改的 history 数据经 `History/Verify.java:107`
（`new GenericBean().decode(bb).buildString(sb)`）校验时得到不可捕获定位的 SOE 而非干净
解析错误。

## 修法方向

对齐 SkipUnknownField：decode/decodeField 用 ThreadLocal 深度计数，超上限抛带语义的
IllegalStateException（GenericDynamicBean 同步）。
