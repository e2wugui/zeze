# 13 processErrorSids indexOf 归因：同链双登录第二个漏标

- **严重级**：P2（有界自愈：verifyLocal 默认 600s / CheckLinkSession 应答 / 后续单发失败补偿）
- **类别**：网关断链在线清理归因错误
- **子系统**：Arch / Game
- **位置**：`ZezeJava/ZezeJava/src/main/java/Zeze/Arch/Online.java:939-948`；`Game/Online.java:1340-1346`（同型）
- **状态**：⚔️ 对抗验证 **CONFIRMED**（2026-10-20 红队机制链全环实锤：linkSid 确 per-connection（ProviderUserSession:79-81←Dispatch.Argument.linkSid=连接 sessionId）但登录模型**不强制一连接一登录**——Game 换角（Online.java:2046-2075 只查同 roleId 重复）后 linkd 侧 userState 单值（LinkdUserSession:30）令 LinkBroken 主通告只能清最后一个 roleId（ProviderWithOnline:54-61），**旧 roleId 断链清理唯一入口恰是本缺陷的 processErrorSids**——缺陷从边角升格为该场景主路径失效。分组平行 add（Arch:998-999/Game:1390-1391）+linkd 逐条上报无去重（LinkdProvider:556-572）+LongList.indexOf 恒返首匹配（:370-382）全环在案。**两处表述修正**：Arch 触发面=同连接双登录的异常客户端行为（常态多端点=多连接=不同 linkSid=不触发）；"第一个登录被重复标记"实为幂等早退空转（onSendError 三态）。自愈通道 CheckLinkSession 探测本身也走 processErrorSids 缺陷路径，收敛需第一个登录先走完 eLinkBroken→DelayLogout 移出分组，比原判更长（仍 600s 级有界））

## 缺陷

批量 sendDirect 整链失败后的 errorSids 回执处理用 `indexOf(linkSid)` 定位账号——同一 linkd
连接（同 linkName+linkSid）承载两个登录（Arch 多端点模型 `getAccountLoginCount>1` 是常态；
Game 换角未登出窗口两 roleId 共享 linkSid）时，分组 `LinkSids` 含两个相同 sid，linkd 侧
对每个失败条目各上报一次，`indexOf` 两次都命中下标 0——第一个登录被重复标记，第二个登录的
`onSendError` 永不执行，其在线状态停留 eLogined。期间 `isOnline`/`getAccountLoginCount`
误报、`sendDirect` 对其继续走已死链。

## 代码证据

```java
// Arch/Online.java:939-948（Game/Online.java:1340-1346 同型）
private void processErrorSids(LongList errorSids, LinkRoles group) {
    errorSids.foreach(linkSid -> TaskSpec.ofProcedure(providerApp.zeze.newProcedure(() -> {
        int idx = group.send.Argument.getLinkSids().indexOf(linkSid);  // ← 恒命中下标0
        if (idx >= 0) {
            var loginKey = group.accounts.get(idx);
            return onSendError(loginKey.getAccount(), loginKey.getClientId(), group.linkName, linkSid);
        }
        return 0;
    }, ...)).run());
}
```

## 触发场景

同链双登录 + 整链失败（`isTimeout()` 时 errorSids=全部 linkSids）：第二个登录的断链清理
丢失，等 verifyLocal（600s）或后续补偿路径收敛。

## 修法方向

按索引遍历对齐 errorSids 与 accounts/roleIds（errorSids 的每个条目对应 LinkSids 的一次
出现），或 indexOf 改为带起点游标的顺序扫描；Arch 与 Game 两处同修。
