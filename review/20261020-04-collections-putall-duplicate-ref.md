# 04 批内重复引用击穿 putAll 挂接原子性预检

- **严重级**：P2（需调用方将同一 bean 实例放入同一批的两个键——本身是误用，但代码显式承诺干净拒绝而未兑现）
- **类别**：别名污染/批量原子性缺口
- **子系统**：Transaction/Collections
- **位置**：`Transaction/Collections/PMap2.java:97-113`；同型 `PList2.java:162-167`、`PList2.java:218-228`、`PSortedMap2.java:120-129`
- **状态**：已复核存活（集合族分片初审，主审读码定谳）

## 缺陷

批量挂接的预检只检测**既有**受管状态（`e.getValue().isManaged()`），检测不到**批内重复引用**：
同一未受管 bean 在批内出现两次时预检双绿，挂接循环在第一个键挂接成功后，第二个键的
`initRootInfoWithRedo` 内 `if (isManaged()) throw`（Bean.java:88）中途抛出——第一次挂接已生效
且 rootInfo 是普通字段写、**不受事务回滚保护**（PMap2.java:94-96 注释自述双循环设计目标恰是
"后段项的 HasManagedException 不得留下已挂接的前段项"，被批内重复击穿）。

## 代码证据

```java
// PMap2.java:97-113
for (var e : m.entrySet()) {              // 预检：只查既有 isManaged
    ...
    if (e.getValue().isManaged())         // 批内重复引用时两个出现点都尚未受管，双绿
        throw new HasManagedException();
}
...
for (var e : m.entrySet()) {              // 挂接：第一个键挂上后 bean 变受管
    v.initRootInfoWithRedo(rootInfo, this); // 第二个键在此抛，第一个挂接已生效且不回滚
    v.mapKey(k);
}
```

## 触发场景

`map.putAll(Map.of(k1, bean, k2, bean))` 或 `list.addAll(List.of(bean, bean))`（bean 为新建
未受管实例）——调用方 catch 后复用该 bean 永久抛 HasManagedException，mapKey 残留为首个键。

## 修法方向

预检循环用 IdentityHashSet 对本批 bean 值去重（重复即抛），兑现注释承诺；四个同型点
（PMap2/PList2×2/PSortedMap2）一并修。
