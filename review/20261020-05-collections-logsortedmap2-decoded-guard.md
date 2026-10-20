# 05 LogSortedMap2.mergeChangedToReplaced 漏 decoded 守卫

- **严重级**：P2（需调用方在 follower 解码日志上误调——LogMap2 同场景已修并有专门测试，排序镜像漏接）
- **类别**：镜像保护缺失
- **子系统**：Transaction/Collections
- **位置**：`Transaction/Collections/LogSortedMap2.java:67-74`；对照 `LogMap2.java:63-67`（有守卫+TestLogMap2MergeDecodedRejects 钉住）
- **状态**：⚔️ 对抗验证 CONFIRMED → ✅ **已修复（2026-10-20）**：LogSortedMap2.mergeChangedToReplaced 补 decoded 守卫（对齐 LogMap2 文案），镜像回归测试 TestLogSortedMap2MergeDecodedRejects（decoded 拒绝 + leader 幂等两用例）。验证：javac 零告警 + 直跑 4 用例全过（含 #06 的 2 个）。红队标注保留：当前触发不可构造（监听器只收 leader 态日志，生产调用全部强转 LogMap2），修复为契约对齐性质

## 缺陷

`mergeChangedToReplaced` 在 LogMap2 有解码态守卫（解码态的 `changed` 为空、`changedWithKey`
才是完整回放日志且其中 LogBean 以 null self 构建，对它 merge 语义未定义），排序家族同函数
漏接——对解码态调用会在 `Objects.requireNonNull(e.getValue().getThis())` 处抛裸 NPE 而非
带语义的拒绝。

## 代码证据

```java
// LogMap2.java:63-67（有守卫）
public void mergeChangedToReplaced() {
    // leader-only：解码态的changed为空、changedWithKey才是完整回放日志，merge会把
    // 解码重建的bean塞进replaced——语义未定义（增量丢失/错挂）且无报错，显式拒绝。
    if (decoded)
        throw new IllegalStateException("mergeChangedToReplaced on decoded log (leader-only)");

// LogSortedMap2.java:67-74（同函数，无守卫）
public void mergeChangedToReplaced() {
    if (!merged) {
        merged = true;
        buildChangedWithKey();
        for (var e : changedWithKey.entrySet())
            getReplaced().put(e.getKey(), (V)Objects.requireNonNull(e.getValue().getThis())); // ← 裸 NPE
    }
}
```

## 触发场景

调用方按 ZezexJava `ModuleEquip.java:349` / `ModuleBuf.java:148` 的
`notemap2.mergeChangedToReplaced()` 监听器模式，在 PSortedMap2 的变更监听里对 follower 解码
日志调用（LogMap2 同场景已被守卫拒绝，排序镜像漏接）。

## 修法方向

LogSortedMap2 补同款 `if (decoded) throw`，并镜像 TestLogMap2MergeDecodedRejects 加排序版
回归测试。
