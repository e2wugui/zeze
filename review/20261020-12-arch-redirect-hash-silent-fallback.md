# 12 @RedirectHash conc≤1 目标不可达窗口静默改投任意 provider

- **严重级**：P2（窗口=滚动重启/直连重建期；依赖服务器本地态的方法在错误服务器读到空数据且无任何错误）
- **类别**：Redirect 路由正确性/哈希亲和契约
- **子系统**：Arch
- **位置**：`ZezeJava/ZezeJava/src/main/java/Zeze/Arch/RedirectBase.java:112-126`；对照同文件 choiceServer 的 orOtherServer（注解显式 opt-in，默认抛 RedirectException）、conc>1 路径（ProviderDistribute.choiceDataIndex miss→ProviderNotExist）
- **状态**：已复核存活（Arch 分片初审，主审读码定谳）

## 缺陷

`dataConcurrentLevel<=1` 的 `@RedirectHash`（不带 ConcurrentLevelSource 的注解，生成器回落
`"0"`，见 `Arch/Gen/MethodOverride.java:245-248`）在环选中的归属服务器 socket 不可达时
（直连重建窗口：滚动重启时 OnSocketClose 已清 localState 但 SM 未摘除、环仍含该节点；或新
节点入环但 direct mesh 未建好），**静默改投环上任意一个活 provider**——与 hash 无关、无注释、
无开关。三处口径唯此静默：choiceServer 的 orOtherServer 是注解 opt-in（默认抛
RedirectException）；conc>1 路径同场景返回 miss=ProviderNotExist。

影响：(a) 依赖服务器本地状态（内存态/_tlocal 型数据）的方法在错误服务器上读到空数据且无错；
(b) 默认 oneByOne 键为 `Long.hashCode(_t_.getSessionId())`（目标 socket），改投即换串行队列，
同 hash 键的串行假设在窗口内失效。

## 代码证据

```java
// RedirectBase.java:112-126 —— conc<=1：环上任意活的 provider，与 hash 无关
if (dataConcurrentLevel <= 1) {
    servers.lock();
    try {
        for (int i = 0, n = servers.getLocalStates().size(); i < n; i++) {
            var e = servers.getNextStateEntry();
            if (e == null) break;
            socket = Objects.requireNonNull(service).GetSocket(((ProviderModuleState)e.getValue()).sessionId);
            if (socket != null && !socket.isClosed())
                return socket;
        }
    } finally { servers.unlock(); }
}
// conc>1 落到这里：显式抛 SERVER_NOT_FOUND
```

## 触发场景

不带 ConcurrentLevelSource 的 `@RedirectHash` 方法，其 hash 归属服务器处于直连重建窗口，
调用被静默发往任意存活 provider 执行。

## 修法方向

需产品语义裁定：(a) 改为与 conc>1 一致的显式失败（抛 RedirectException，调用方可重试等
重建完成）；或 (b) 注解 opt-in 保留回退（对齐 orOtherServer 先例）。当前形态（无注释无开关
静默改投）与同族两处设计口径相悖，最少应加注释钉死意图。
