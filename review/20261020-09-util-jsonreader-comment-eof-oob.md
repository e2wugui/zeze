# 09 JsonReader.skipComment 首读越界（EOF 守卫遗漏）

- **严重级**：P2（仅畸形输入可触发、无数据损坏；但外部输入入口得到无语义 AIOOBE 而非可分类解析错误）
- **类别**：Json 解析边界/异常路径
- **子系统**：Util
- **位置**：`ZezeJava/ZezeJava/src/main/java/Zeze/Util/JsonReader.java:309`（skipComment 首读）；波及 `:222`（end 的 `buf[++pos]`）、`:257`（skipNext 无界检查）、`:298-300`（skipVar 注释分支）
- **状态**：已复核存活（Util 分片初审，主审读码定谳）

## 缺陷

`skipComment()` 的第一次读取没有 EOF 守卫：`/` 恰为缓冲区最后一个字节时 `++pos == buf.length`，
`buf[buf.length]` 抛 AIOOBE。同类问题的 EOF 硬化在库内到处都是——`next()`（:241-246）的 NUL
哨兵注释明确记载修复动机（"空 body 请求打进 /api/search 即 500"）、skipComment 块注释扫描
内部（:311-313）也有 `pos >= buf.length` 守卫——唯独首读漏防，同一硬化遗漏边。
`Json.parse` 各静态入口只捕 `ReflectiveOperationException`，AIOOBE 原样穿透到 DbWeb 等
外部输入入口。

## 代码证据

```java
// JsonReader.java:307-315
private void skipComment() {
    int b;
    if ((b = Objects.requireNonNull(buf)[++pos]) == '*') {   // ← 首读无守卫：'/'为末字节即 AIOOBE
        for (pos++; ; ) {
            if (pos >= Objects.requireNonNull(buf).length)
                return; // 未终结的块注释到EOF：按耗尽处理 ← 内部扫描有守卫，首读没有
```

## 触发场景

`Json.parse("[1 /", Object.class)` → `parseArray0` → `skipVar(']')` → :298 `'/'` 分支 →
`pos--` → `skipComment()` → `buf[4]`（len=4）AIOOBE。`end(int)` 的 :222 `buf[++pos]` 在尾部
孤立 `/` 时同样越界。

## 修法方向

skipComment 首读前判 `pos >= buf.length` 按耗尽返回（对齐 :311 口径）；end(:222) 与
skipNext(:257) 的 `++pos` 读同批加界检。回归用例：`"[1 /"`、`"[1] /"`、`"/"`。
