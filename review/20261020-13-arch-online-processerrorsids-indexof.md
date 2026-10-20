# 13 processErrorSids indexOf 归因：同链双登录第二个漏标

- **严重级**：P2（有界自愈：verifyLocal 默认 600s / CheckLinkSession 应答 / 后续单发失败补偿）
- **类别**：网关断链在线清理归因错误
- **子系统**：Arch / Game
- **位置**：`ZezeJava/ZezeJava/src/main/java/Zeze/Arch/Online.java:939-948`；`Game/Online.java:1340-1346`（同型）
- **状态**：📋 **owner 裁定关闭·不修（2026-10-20）**——**两 roleId 不可能共享 linkSid**：正常协议下换角=先 Logout 再 Login，一个连接任一时刻只有一个活跃 roleId，分组 LinkSids 不会含重复 sid；出现重复即客户端已违约出错，不在服务端正确性契约内。主审复核的机制事实保留备查：服务端确无同连接双登录的强制拦截（ProcessSetUserState:627-637 对旧 context 无清理覆盖、ProcessLoginRequest:2063-2075 只对同 roleId kick）——即触发该场景需客户端在同连接上 Login 新角色而不 Logout 旧角色的错误行为，此时 processErrorSids 的 indexOf 归因退化属错误场景下的尽力而为，不构成缺陷

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
