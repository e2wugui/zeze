# 06 LogMap1.remove(K,V) 空值 NPE 分叉

- **严重级**：P2（低影响：PMap 值恒非 null 是既定不变量；属同输入两分支行为分叉）
- **类别**：托管/非托管分支契约分叉
- **子系统**：Transaction/Collections
- **位置**：`Transaction/Collections/LogMap1.java:94-103`（镜像 `LogSortedMap1.java:100`）；入口 `PMap1.java:89` / `PMap2.java:141-153` / PSortedMap1/2 的 remove(Entry)
- **状态**：⚔️ 对抗验证 DOWNGRADED 理论备忘 → ✅ **已按防御对齐修复（2026-10-20）**：LogMap1/LogSortedMap1 的 remove(K,V) 改 `old != null && old.equals(value)`（以容器值为 receiver，逐位对齐非托管分支形态），回归测试 TestLogMapRemoveEntryNullValue（null 值 false / 不存在 key false / 正常删除不受影响 × Map1+SortedMap1）。验证：javac 零告警 + 直跑全过。红队三重隔离结论保留备查：remove(Entry) 主树零调用、null 值实际不可达、无契约基准

## 缺陷

`remove(Entry)` 的托管路径 `if (value.equals(old))` 在 `value==null` 时 NPE，而同一操作的
非托管路径 `if (exist != null && exist.equals(v))` 对 null 值安全返回 false——同输入两分支
行为不同。托管分支 NPE 炸事务，非托管分支安静返回"没删"。

## 代码证据

```java
// LogMap1.java:94-103（托管路径）—— value==null 直接 NPE
public final boolean remove(K key, V value) {
    V old = getValue().get(key);
    if (value.equals(old)) {          // ← value null 则 NPE
        ...
    }
    return false;
}

// PMap2.java:149-152（非托管路径）—— 同输入安全 false
V exist = map.get(k);
if (exist != null && exist.equals(v)) { // v==null → equals(null)=false → 安全
```

## 触发场景

`map.remove(entry)`，entry 的值来自含 null 值的外来 Map（如 java.util.HashMap 迭代产物；
注意 `Map.entry()` 本身禁 null，须从允许 null 值的容器来）。

## 修法方向

LogMap1/LogSortedMap1 的 remove(K,V) 改 `if (old != null && old.equals(value))` 形态
（以 map 内值为 receiver），或前置 `value == null → return false` 对齐非托管分支。
