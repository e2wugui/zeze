package Zeze.Arch;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.Online.*;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.BLinkBroken;
import Zeze.Builtin.Provider.Broadcast;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Builtin.ProviderDirect.TransmitAccount;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
import Zeze.Util.Func4;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Online extends AbstractOnline {
    public static long GetSpecialTypeIdFromBean(Bean bean) {
        return bean.getTypeId();
    }

    public static Bean CreateBeanFromSpecialTypeId(long typeId) {
        throw new UnsupportedOperationException("Online Memory Table Dynamic Only.");
    }

    public final ProviderApp ProviderApp;
    //public LoadReporter LoadReporter { get; }
    public taccount getTableAccount() {
        return _taccount;
    }

    public Online(ProviderApp app) {
        this.ProviderApp = app;

        RegisterProtocols(ProviderApp.ProviderService);
        RegisterZezeTables(ProviderApp.Zeze);

        //LoadReporter = new(this);
    }

    @Override
    public void UnRegister() {
        UnRegisterZezeTables(ProviderApp.Zeze);
        UnRegisterProtocols(ProviderApp.ProviderService);
    }

    public void Start() {
        //LoadReporter.StartTimerTask();
        Task.scheduleAt(3 + Zeze.Util.Random.getInstance().nextInt(3), 10, this::VerifyLocal); // at 3:10 - 6:10
        ProviderApp.BuiltinModules.put(this.getFullName(), this);
    }

    public int getLocalCount() {
        return _tlocal.getCacheSize();
    }

    public long walkLocal(TableWalkHandle<String, BLocals> walker) {
        return _tlocal.WalkCache(walker);
    }

    public void setLocalBean(String account, String clientId, String key, Bean bean) {
        var bLocals = _tlocal.get(account);
        if (null == bLocals)
            throw new RuntimeException("roleid not online. " + account);
        var login = bLocals.getLogins().get(clientId);
        if (null == login) {
            login = new BLocal();
            if (null != bLocals.getLogins().put(clientId, login))
                throw new RuntimeException("duplicate clientId:" + clientId);
        }
        var bAny = new BAny();
        bAny.getAny().setBean(bean);
        login.getDatas().put(key, bAny);
    }

    @SuppressWarnings("unchecked")
    public <T extends Bean> T getLocalBean(String account, String clientId, String key) {
        var bLocals = _tlocal.get(account);
        if (null == bLocals)
            return null;
        var login = bLocals.getLogins().get(clientId);
        if (null == login)
            return null;
        var data = login.getDatas().get(key);
        if (null == data)
            return null;
        return (T)data.getAny().getBean();
    }

    private final EventDispatcher LoginEvents = new EventDispatcher("Online.Login");
    private final EventDispatcher ReloginEvents = new EventDispatcher("Online.Relogin");
    private final EventDispatcher LogoutEvents = new EventDispatcher("Online.Logout");
    private final EventDispatcher LocalRemoveEvents = new EventDispatcher("Online.Local.Remove");

    public EventDispatcher getLoginEvents() {
        return LoginEvents;
    }

    public EventDispatcher getReloginEvents() {
        return ReloginEvents;
    }

    public EventDispatcher getLogoutEvents() {
        return LogoutEvents;
    }

    public EventDispatcher getLocalRemoveEvents() {
        return LocalRemoveEvents;
    }

    private final AtomicLong _LoginTimes = new AtomicLong();

    public long getLoginTimes() {
        return _LoginTimes.get();
    }

    private void RemoveLocalAndTrigger(String account, String clientId) throws Throwable {
        var bLocals = _tlocal.get(account);
        var localData = bLocals.getLogins().remove(clientId);
        var arg = new LocalRemoveEventArgument();
        arg.Account = account;
        arg.ClientId = clientId;
        if (null != localData)
            arg.LocalData = localData.Copy();

        if (bLocals.getLogins().isEmpty())
            _tlocal.remove(account); // remove first

        LocalRemoveEvents.triggerEmbed(this, arg);
         LocalRemoveEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
        Transaction.getCurrent().RunWhileCommit(() -> LocalRemoveEvents.triggerThread(this, arg));
    }

    private void RemoveOnlineAndTrigger(String account, String clientId) throws Throwable {
        var bOnlines = _tonline.get(account);
        var onlineData = bOnlines.getLogins().remove(clientId);

        var arg = new LogoutEventArgument();
        arg.Account = account;
        arg.ClientId = clientId;
        if (null != onlineData)
            arg.OnlineData = onlineData.Copy();

        if (bOnlines.getLogins().isEmpty())
            _tonline.remove(account); // remove first

        LogoutEvents.triggerEmbed(this, arg);
         LogoutEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
        Transaction.getCurrent().RunWhileCommit(() -> LogoutEvents.triggerThread(this, arg));
    }

    private void LoginTrigger(String account, String clientId) throws Throwable {
        var arg = new LoginArgument();
        arg.Account = account;
        arg.ClientId = clientId;

        LoginEvents.triggerEmbed(this, arg);
        LoginEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
        Transaction.getCurrent().RunWhileCommit(() -> LoginEvents.triggerThread(this, arg));
        _LoginTimes.incrementAndGet();
    }

    private void ReloginTrigger(String account, String clientId) throws Throwable {
        var arg = new LoginArgument();
        arg.Account = account;
        arg.ClientId = clientId;

        ReloginEvents.triggerEmbed(this, arg);
        ReloginEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
        Transaction.getCurrent().RunWhileCommit(() -> ReloginEvents.triggerThread(this, arg));
        _LoginTimes.incrementAndGet();
    }

    public void OnLinkBroken(String account, String clientId, BLinkBroken arg) throws Throwable {
        long currentLoginVersion;
        {
            var online = _tonline.get(account);
            var loginOnline = online.getLogins().get(clientId);
            if (null == loginOnline)
                return;
            // skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
            if (loginOnline.getLinkSid() != arg.getLinkSid())
                return;

            var version = _tversion.getOrAdd(account);
            var local = _tlocal.get(account);
            if (null == local)
                return; // 不在本机登录。
            var loginLocal = local.getLogins().get(clientId);
            if (null == loginLocal)
                return; // 不在本机登录。
            var loginVersion = version.getLogins().get(clientId);
            if (null == loginVersion)
                return; // 不存在登录。
            currentLoginVersion = loginLocal.getLoginVersion();
            if (loginVersion.getLoginVersion() != currentLoginVersion)
                RemoveLocalAndTrigger(account, clientId); // 本机数据已经过时，马上删除。
        }
        final var finalCurrentLoginVersion = currentLoginVersion;
        Transaction.getCurrent().RunWhileCommit(() -> Task.schedule(10 * 60 * 1000, () -> {
            // TryRemove
            ProviderApp.Zeze.NewProcedure(() ->
            {
                // local online 独立判断version分别尝试删除。
                var local = _tlocal.get(account);
                if (null != local) {
                    var loginLocal = local.getLogins().get(clientId);
                    if (null != loginLocal && loginLocal.getLoginVersion() == finalCurrentLoginVersion)
                        RemoveLocalAndTrigger(account, clientId);
                }
                // 如果玩家在延迟期间建立了新的登录，下面版本号判断会失败。
                var online = _tonline.get(account);
                var version = _tversion.getOrAdd(account);
                if (null != online) {
                    var loginVersion = version.getLogins().get(clientId);
                    if (null != loginVersion && loginVersion.getLoginVersion() == finalCurrentLoginVersion)
                        RemoveOnlineAndTrigger(account, clientId);
                }
                return Procedure.Success;
            }, "Onlines.OnLinkBroken").Call();
        }));
    }

    public void addReliableNotifyMark(String account, String clientId, String listenerName) throws Throwable {
        var online = _tonline.get(account);
        if (null == online)
            throw new RuntimeException("Not Online. AddReliableNotifyMark: " + listenerName);
        var version = _tversion.getOrAdd(account);
        version.getLogins().getOrAdd(clientId).getReliableNotifyMark().add(listenerName);
    }

    public void removeReliableNotifyMark(String account, String clientId, String listenerName) {
        // 移除尽量通过，不做任何判断。
        var login = _tversion.getOrAdd(account).getLogins().get(clientId);
        if (null != login)
            login.getReliableNotifyMark().remove(listenerName);
    }

    public void sendReliableNotifyWhileCommit(String account, String clientId, String listenerName, Protocol<?> p) {
        Transaction.getCurrent().RunWhileCommit(
                () -> sendReliableNotify(account, clientId, listenerName, p)
                );
    }

    public void SendReliableNotifyWhileCommit(
            String account, String clientId, String listenerName, int typeId, Binary fullEncodedProtocol) {
        Transaction.getCurrent().RunWhileCommit(
                () -> sendReliableNotify(account, clientId, listenerName, typeId, fullEncodedProtocol)
                );
    }

    public void sendReliableNotifyWhileRollback(String account, String clientId, String listenerName, Protocol<?> p) {
        Transaction.getCurrent().RunWhileRollback(
                () -> sendReliableNotify(account, clientId, listenerName, p)
                );
    }

    public void sendReliableNotifyWhileRollback(
            String account, String clientId, String listenerName, int typeId, Binary fullEncodedProtocol)
    {
        Transaction.getCurrent().RunWhileRollback(
                () -> sendReliableNotify(account, clientId, listenerName, typeId, fullEncodedProtocol)
                );
    }

    public void sendReliableNotify(String account, String clientId, String listenerName, Protocol<?> p) {
        sendReliableNotify(account, clientId, listenerName, p.getTypeId(), new Binary(p.Encode()));
    }

    private Zeze.Collections.Queue<BNotify> OpenQueue(String account, String clientId) {
        return ProviderApp.Zeze.getQueueModule().open("Zeze.Arch.Online.ReliableNotifyQueue:" + account + ":" + clientId, BNotify.class);
    }

    /// <summary>
    /// 发送在线可靠协议，如果不在线等，仍然不会发送哦。
    /// </summary>
    /// <param name="roleId"></param>
    /// <param name="listenerName"></param>
    /// <param name="fullEncodedProtocol">协议必须先编码，因为会跨事务。</param>
    public void sendReliableNotify(
            String account, String clientId, String listenerName, long typeId, Binary fullEncodedProtocol) {
        ProviderApp.Zeze.getTaskOneByOneByKey().Execute(
                listenerName,
                ProviderApp.Zeze.NewProcedure(() ->
                        {
                            var online = _tonline.get(account);
                            if (null == online) {
                                // 完全离线，忽略可靠消息发送：可靠消息仅仅为在线提供服务，并不提供全局可靠消息。
                                return Procedure.Success;
                            }
                            var version = _tversion.getOrAdd(account);
                            var login = version.getLogins().get(clientId);
                            if (null == login || false == login.getReliableNotifyMark().contains(listenerName)) {
                                return Procedure.Success; // 相关数据装载的时候要同步设置这个。
                            }

                            // 先保存在再发送，然后客户端还会确认。
                            // see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm 的实现。
                            var queue = OpenQueue(account, clientId);
                            var bNotify = new BNotify();
                            bNotify.setFullEncodedProtocol(fullEncodedProtocol);
                            queue.add(bNotify);

                            var notify = new SReliableNotify(); // 不直接发送协议，是因为客户端需要识别ReliableNotify并进行处理（计数）。
                            notify.Argument.setReliableNotifyIndex(login.getReliableNotifyIndex());
                            login.setReliableNotifyIndex(login.getReliableNotifyIndex() + 1); // after set notify.Argument
                            notify.Argument.getNotifies().add(fullEncodedProtocol);

                            SendInProcedure(List.of(new LoginKey(account, clientId))
                                    , notify.getTypeId(), new Binary(notify.Encode()));
                            return Procedure.Success;
                        },
                        "SendReliableNotify." + listenerName
                ));
    }

    public Collection<RoleOnLink> GroupByLink(Collection<LoginKey> logins) throws Throwable {
        var groups = new HashMap<String, RoleOnLink>();
        var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
        groups.put(groupNotOnline.LinkName, groupNotOnline);

        for (var alogin : logins) {
            var online = _tonline.get(alogin.Account);
            if (null == online) {
                groupNotOnline.Logins.putIfAbsent(alogin, 0L);
                continue;
            }
            var login = online.getLogins().get(alogin.ClientId);
            if (null == login) {
                groupNotOnline.Logins.putIfAbsent(alogin, 0L);
                continue;
            }
            var connector = ProviderApp.ProviderService.getLinks().get(login.getLinkName());
            if (null == connector) {
                groupNotOnline.Logins.putIfAbsent(alogin, 0L);
                continue;
            }

            if (false == connector.isHandshakeDone()) {
                groupNotOnline.Logins.putIfAbsent(alogin, 0L);
                continue;
            }
            // 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
            var group = groups.get(login.getLinkName());
            if (null == group) {
                group = new RoleOnLink();
                group.LinkName = login.getLinkName();
                group.LinkSocket = connector.getSocket();
                // 上面online存在Login的时，下面version也肯定存在相应的Login。
                group.ServerId = (_tversion.getOrAdd(alogin.Account)).getLogins().getOrAdd(alogin.ClientId).getServerId();
                groups.putIfAbsent(group.LinkName, group);
            }
            group.Logins.putIfAbsent(alogin, login.getLinkSid());
        }
        return groups.values();
    }

    private void SendInProcedure(Collection<LoginKey> logins, long typeId, Binary fullEncodedProtocol) throws Throwable {
        var groups = GroupByLink(logins);
        Transaction.getCurrent().RunWhileCommit(() -> {
            for (var group : groups) {
                if (group.LinkSocket == null)
                    continue; // skip not online

                var send = new Send();
                send.Argument.setProtocolType(typeId);
                send.Argument.setProtocolWholeData(fullEncodedProtocol);
                send.Argument.getLinkSids().addAll(group.Logins.values());
                group.LinkSocket.Send(send);
            }
        });
    }

    public static class RoleOnLink {
        public String LinkName = ""; // empty when not online
        public AsyncSocket LinkSocket; // null if not online
        public int ServerId = -1;
        public long ProviderSessionId;
        public HashMap<LoginKey, Long> Logins = new HashMap<>();
    }

    public static class LoginKey {
        public String Account;
        public String ClientId;

        public LoginKey(String account, String clientId) {
            Account = account;
            ClientId = clientId;
        }

        @Override
        public int hashCode() {
            final int _prime_ = 31;
            int _h_ = 0;
            _h_ = _h_ * _prime_ + Account.hashCode();
            _h_ = _h_ * _prime_ + ClientId.hashCode();
            return _h_;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (obj instanceof LoginKey) {
                var login = (LoginKey)obj;
                return Account.equals(login.Account) && ClientId.equals(login.ClientId);
            }
            return false;
        }
    }

    private void Send(String account, String clientId, long typeId, Binary fullEncodedProtocol) {
        var login = new LoginKey(account, clientId);
        ProviderApp.Zeze.getTaskOneByOneByKey().Execute(login,
                ProviderApp.Zeze.NewProcedure(() -> {
                    SendInProcedure(List.of(login), typeId, fullEncodedProtocol);
                    return Procedure.Success;
                }, "Onlines.Send"));
    }

    private void Send(Collection<LoginKey> logins, long typeId, Binary fullEncodedProtocol) {
        ProviderApp.Zeze.getTaskOneByOneByKey().ExecuteCyclicBarrier(logins,
                ProviderApp.Zeze.NewProcedure(() -> {
                    SendInProcedure(logins, typeId, fullEncodedProtocol);
                    return Procedure.Success;
                }, "Onlines.Send"), null);
    }

    public void send(String account, String clientId, Protocol<?> p) {
        Send(account, clientId, p.getTypeId(), new Binary(p.Encode()));
    }

    public void send(Collection<LoginKey> logins, Protocol<?> p) {
        Send(logins, p.getTypeId(), new Binary(p.Encode()));
    }

    public void sendWhileCommit(String account, String clientId, Protocol<?> p) {
        Transaction.getCurrent().RunWhileCommit(() -> send(account, clientId, p));
    }

    public void SendWhileCommit(Collection<LoginKey> logins, Protocol<?> p) {
        Transaction.getCurrent().RunWhileCommit(() -> send(logins, p));
    }

    public void sendWhileRollback(String account, String clientId, Protocol<?> p) {
        Transaction.getCurrent().RunWhileRollback(() -> send(account, clientId, p));
    }

    public void sendWhileRollback(Collection<LoginKey> logins, Protocol<?> p) {
        Transaction.getCurrent().RunWhileRollback(() -> send(logins, p));
    }

    /// <summary>
    /// Func<senderAccount, senderClientId, target, result>
    /// sender: 查询发起者，结果发送给他。
    /// target: 查询目标。
    /// result: 返回值，int，按普通事务处理过程返回值处理。
    /// </summary>
    public ConcurrentHashMap<String, Func4<String, String, String, Binary, Long>> TransmitActions = new ConcurrentHashMap<>();

    /// <summary>
    /// 转发查询请求给RoleId。
    /// </summary>
    /// <param name="sender">查询发起者，结果发送给他。</param>
    /// <param name="actionName">查询处理的实现</param>
    /// <param name="roleId">目标角色</param>
    public void transmit(String account, String clientId, String actionName, String target, Serializable parameter) throws Throwable {
        transmit(account, clientId, actionName, List.of(target), parameter);
    }

    public void processTransmit(String account, String clientId, String actionName, Collection<String> accounts, Binary parameter) throws Throwable {
        var handle = TransmitActions.get(actionName);
        if (null != handle) {
            for (var target : accounts) {
                ProviderApp.Zeze.NewProcedure(() -> handle.call(account, clientId, target, parameter), "Arch.Online.Transmit:" + actionName).Call();
            }
        }
    }

    public static class RoleOnServer {
        public int ServerId = -1; // empty when not online
        public HashSet<String> Accounts = new HashSet<>();

        public void addAll(HashSet<String> accounts) {
            Accounts.addAll(accounts);
        }
    }

    public Collection<RoleOnServer> GroupByServer(Collection<String> accounts) {
        var groups = new HashMap<Integer, RoleOnServer>();
        var groupNotOnline = new RoleOnServer(); // LinkName is Empty And Socket is null.
        groups.put(groupNotOnline.ServerId, groupNotOnline);

        for (var account : accounts) {
            var online = _tonline.get(account);
            if (null == online) {
                groupNotOnline.Accounts.add(account);
                continue;
            }
            var version = _tversion.getOrAdd(account);
            if (version.getLogins().isEmpty()) {
                // null != online 意味着这里肯定不为0，不会到达这个分支。
                // 下面要求Logins.Count必须大于0，判断一下吧。
                groupNotOnline.Accounts.add(account);
                continue;
            }
            var serverId = version.getLogins().iterator().next().getValue().getServerId();
            // 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
            var group = groups.get(serverId);
            if (null == group) {
                group = new RoleOnServer();
                group.ServerId = serverId;
                groups.put(group.ServerId, group);
            }
            group.Accounts.add(account);
        }
        return groups.values();
    }

    private RoleOnServer merge(RoleOnServer current, RoleOnServer m) {
        if (null == current)
            return m;
        current.addAll(m.Accounts);
        return current;
    }

    private void transmitInProcedure(String account, String clientId, String actionName, Collection<String> accounts, Binary parameter) throws Throwable {
        if (ProviderApp.Zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().isEmpty()) {
            // 没有启用cache-sync，马上触发本地任务。
            processTransmit(account, clientId, actionName, accounts, parameter);
            return;
        }

        var groups = GroupByServer(accounts);
        RoleOnServer groupLocal = null;
        for (var group : groups) {
            if (group.ServerId == -1 || group.ServerId == ProviderApp.Zeze.getConfig().getServerId()) {
                // loopback 就是当前gs.
                // 对于不在线的角色，直接在本机运行。
                groupLocal = merge(groupLocal, group);
                continue;
            }

            var transmit = new TransmitAccount();
            transmit.Argument.setActionName(actionName);
            transmit.Argument.setSenderAccount(account);
            transmit.Argument.setSenderClientId(clientId);
            transmit.Argument.getTargetAccounts().addAll(group.Accounts);
            if (null != parameter) {
                transmit.Argument.setParameter(parameter);
            }

            var ps = ProviderApp.ProviderDirectService.ProviderByServerId.get(group.ServerId);
            if (null == ps) {
                groupLocal.addAll(group.Accounts);
                continue;
            }
            var socket = ProviderApp.ProviderDirectService.GetSocket(ps.SessionId);
            if (null == socket) {
                groupLocal.addAll(group.Accounts);
                continue;
            }
            transmit.Send(socket);
        }
        if (groupLocal.Accounts.size() > 0)
            processTransmit(account, clientId, actionName, groupLocal.Accounts, parameter);
    }

    public void transmit(String account, String clientId, String actionName, Collection<String> targets, Serializable parameter) throws Throwable {
        if (false == TransmitActions.containsKey(actionName))
            throw new RuntimeException("Unkown Action Name: " + actionName);

        var binaryParam = parameter == null ? Binary.Empty : new Binary(ByteBuffer.Encode(parameter));
        // 发送协议请求在另外的事务中执行。
        ProviderApp.Zeze.NewProcedure(() -> {
            transmitInProcedure(account, clientId, actionName, targets, binaryParam);
            return Procedure.Success;
        }, "Onlines.Transmit").Call();
    }

    public void transmitWhileCommit(String account, String clientId, String actionName, String target, Serializable parameter) {
        if (false == TransmitActions.containsKey(actionName))
            throw new RuntimeException("Unkown Action Name: " + actionName);
        Transaction.getCurrent().RunWhileCommit(() -> {
            try {
                transmit(account, clientId, actionName, target, parameter);
            } catch (Throwable e) {
                logger.error(e);
            }
        });
    }

    public void transmitWhileCommit(String account, String clientId, String actionName, Collection<String> targets, Serializable parameter) {
        if (false == TransmitActions.containsKey(actionName))
            throw new RuntimeException("Unkown Action Name: " + actionName);
        Transaction.getCurrent().RunWhileCommit(() -> {
            try {
                transmit(account, clientId, actionName, targets, parameter);
            } catch (Throwable e) {
                logger.error(e);
            }
        });
    }

    public void transmitWhileRollback(String account, String clientId, String actionName, String target, Serializable parameter) {
        if (false == TransmitActions.containsKey(actionName))
            throw new RuntimeException("Unkown Action Name: " + actionName);
        Transaction.getCurrent().RunWhileRollback(() -> {
            try {
                transmit(account, clientId, actionName, target, parameter);
            } catch (Throwable e) {
                logger.error(e);
            }
        });
    }

    public void transmitWhileRollback(String account, String clientId, String actionName, Collection<String> targets, Serializable parameter) {
        if (false == TransmitActions.containsKey(actionName))
            throw new RuntimeException("Unkown Action Name: " + actionName);
        Transaction.getCurrent().RunWhileRollback(() -> {
            try {
                transmit(account, clientId, actionName, targets, parameter);
            } catch (Throwable e) {
                logger.error(e);
            }
        });
    }

    private void Broadcast(long typeId, Binary fullEncodedProtocol, int time) {
        var broadcast = new Broadcast();
        broadcast.Argument.setProtocolType(typeId);
        broadcast.Argument.setProtocolWholeData(fullEncodedProtocol);
        broadcast.Argument.setTime(time);

        for (var link : ProviderApp.ProviderService.getLinks().values())
        {
            if (null != link.getSocket())
                link.getSocket().Send(broadcast);
        }
    }

    public void Broadcast(Protocol<?> p, int time) {
        Broadcast(p.getTypeId(), new Binary(p.Encode()), time);
    }

    private void VerifyLocal() {
        var account = new OutObject<String>();
        _tlocal.WalkCache((k, v) -> {
                    // 先得到roleId
                    account.Value = k;
                    return true;
                },
                () ->
                {
                    // 锁外执行事务
                    try {
                        ProviderApp.Zeze.NewProcedure(() ->
                        {
                            TryRemoveLocal(account.Value);
                            return 0L;
                        }, "VerifyLocal:" + account).Call();
                    } catch (Throwable e) {
                        logger.error(e);
                    }
                });
        // 随机开始时间，避免验证操作过于集中。3:10 - 5:10
        Task.scheduleAt(3 + Zeze.Util.Random.getInstance().nextInt(3), 10, this::VerifyLocal); // at 3:10 - 6:10
    }

    private static final Logger logger = LogManager.getLogger(Online.class);

    private void TryRemoveLocal(String account) throws Throwable {
        var online = _tonline.get(account);
        var local = _tlocal.get(account);
        var version = _tversion.getOrAdd(account);
        if (null == local)
            return;
        // null == online && null == local -> do nothing
        // null != online && null == local -> do nothing

        if (null == online) {
            // remove all
            for (var loginLocal : local.getLogins().entrySet())
                RemoveLocalAndTrigger(account, loginLocal.getKey());
        } else {
            // 在全局数据中查找login-local，删除不存在或者版本不匹配的。
            for (var loginLocal : local.getLogins().entrySet()) {
                var loginVersion = version.getLogins().get(loginLocal.getKey());
                if (null == loginVersion || loginVersion.getLoginVersion() != loginLocal.getValue().getLoginVersion()) {
                    RemoveLocalAndTrigger(account, loginLocal.getKey());
                }
            }
        }

    }

    @RedirectToServer
    protected void RedirectNotify(int serverId, String account) throws Throwable {
        TryRemoveLocal(account);
    }

    @Override
    protected long ProcessLoginRequest(Login rpc) throws Throwable {
        var session = ProviderUserSession.get(rpc);

        var account = _taccount.getOrAdd(session.getAccount());
        var online = _tonline.getOrAdd(session.getAccount());
        var local = _tlocal.getOrAdd(session.getAccount());
        var version = _tversion.getOrAdd(session.getAccount());

        var loginLocal = local.getLogins().getOrAdd(rpc.Argument.getClientId());
        var loginVersion = version.getLogins().getOrAdd(rpc.Argument.getClientId());

        // login exist && not local
        if (loginVersion.getLoginVersion() != 0 && loginVersion.getLoginVersion() != loginLocal.getLoginVersion()) {
            // nowait
            RedirectNotify(loginVersion.getServerId(), session.getAccount());
        }
        var loginVersionSerialId = account.getLastLoginVersion() + 1;
        account.setLastLoginVersion(loginVersionSerialId);
        loginVersion.setLoginVersion(loginVersionSerialId);
        loginLocal.setLoginVersion(loginVersionSerialId);

        var loginOnline = online.getLogins().getOrAdd(rpc.Argument.getClientId());
        if (!loginOnline.getLinkName().equals(session.getLinkName()) || loginOnline.getLinkSid() == session.getLinkSid()) {
            ProviderApp.ProviderService.kick(loginOnline.getLinkName(), loginOnline.getLinkSid(),
                    BKick.ErrorDuplicateLogin, "duplicate login " + session.getAccount() + ":" + rpc.Argument.getClientId());
        }

        /////////////////////////////////////////////////////////////
        // 当LinkName,LinkSid没有变化的时候，保持记录是读取状态，不会申请写锁。
        // 因为Online数据可能会被很多地方缓存，写操作会造成缓存失效。
        // see Linkd.StableLinkSid
        if (false == loginOnline.getLinkName().equals(session.getLinkName()))
            loginOnline.setLinkName(session.getLinkName());
        if (loginOnline.getLinkSid() != session.getLinkSid())
            loginOnline.setLinkSid(session.getLinkSid());
        /////////////////////////////////////////////////////////////

        loginVersion.setReliableNotifyConfirmIndex(0);
        loginVersion.setReliableNotifyIndex(0);
        loginVersion.getReliableNotifyMark().clear();
        OpenQueue(session.getAccount(), rpc.Argument.getClientId()).clear();

        var linkSession = (ProviderService.LinkSession)session.getLink().getUserState();
        loginVersion.setServerId(ProviderApp.Zeze.getConfig().getServerId());

        LoginTrigger(session.getAccount(), rpc.Argument.getClientId());

        // 先提交结果再设置状态。
        // see linkd::Zezex.Provider.ModuleProvider。ProcessBroadcast
        session.SendResponseWhileCommit(rpc);
        Transaction.getCurrent().RunWhileCommit(() -> {
            var setUserState = new SetUserState();
            setUserState.Argument.setLinkSid(session.getLinkSid());
            setUserState.Argument.setContext(rpc.Argument.getClientId());
            rpc.getSender().Send(setUserState); // 直接使用link连接。
        });
        //App.Load.LoginCount.IncrementAndGet();
        return Procedure.Success;
    }

    @Override
    protected long ProcessLogoutRequest(Zeze.Builtin.Online.Logout rpc) throws Throwable {
        var session = ProviderUserSession.get(rpc);

        if (!session.isLogin())
            return ErrorCode(ResultCodeNotLogin);

        var local = _tlocal.get(session.getAccount());
        var online = _tonline.get(session.getAccount());
        var version = _tversion.getOrAdd(session.getAccount());

        var clientId = session.getContext();
        var loginVersion = version.getLogins().getOrAdd(clientId);
        // 登录在其他机器上。
        if (local == null && online != null)
            RedirectNotify(loginVersion.getServerId(), session.getAccount()); // nowait
        if (null != local)
            RemoveLocalAndTrigger(session.getAccount(), clientId);
        if (null != online)
            RemoveOnlineAndTrigger(session.getAccount(), clientId);

        // 先设置状态，再发送Logout结果。
        Transaction.getCurrent().RunWhileCommit(() -> {
            var setUserState = new SetUserState();
            setUserState.Argument.setLinkSid(session.getLinkSid());
            rpc.getSender().Send(setUserState); // 直接使用link连接。
        });
        session.SendResponseWhileCommit(rpc);
        // 在 OnLinkBroken 时处理。可以同时处理网络异常的情况。
        // App.Load.LogoutCount.IncrementAndGet();
        return Procedure.Success;
    }

    private int ReliableNotifySync(String account, String clientId,
                                   ProviderUserSession session, long index, boolean sync) throws Throwable {
        var online = _tversion.getOrAdd(account);
        var queue = OpenQueue(account, clientId);
        var loginOnline = online.getLogins().getOrAdd(clientId);
        if (index < loginOnline.getReliableNotifyConfirmIndex()
                || index > loginOnline.getReliableNotifyIndex()
                || index - loginOnline.getReliableNotifyConfirmIndex() > queue.size()) {
            return ResultCodeReliableNotifyConfirmIndexOutOfRange;
        }

        int confirmCount = (int)(index - loginOnline.getReliableNotifyConfirmIndex());
        for (int i = 0; i < confirmCount; i++)
            queue.poll();
        loginOnline.setReliableNotifyConfirmIndex(index);

        if (sync) {
            var notify = new SReliableNotify();
            notify.Argument.setReliableNotifyIndex(index);
            queue.walk((node, bNofity) -> {
                notify.Argument.getNotifies().add(bNofity.getFullEncodedProtocol());
                return true;
            });
            session.SendResponseWhileCommit(notify);
        }
        return ResultCodeSuccess;
    }

    @Override
    protected long ProcessReliableNotifyConfirmRequest(Zeze.Builtin.Online.ReliableNotifyConfirm rpc) throws Throwable {
        var session = ProviderUserSession.get(rpc);

        var clientId = session.getContext();
        var online = _tonline.get(session.getAccount());
        if (null == online)
            return ErrorCode(ResultCodeOnlineDataNotFound);

        var syncResultCode = ReliableNotifySync(session.getAccount(), clientId,
                session, rpc.Argument.getReliableNotifyConfirmIndex(), rpc.Argument.isSync());
        session.SendResponseWhileCommit(rpc); // 同步前提交。

        if (ResultCodeSuccess != syncResultCode)
            return ErrorCode((short)syncResultCode);

        return Procedure.Success;
    }

    @Override
    protected long ProcessReLoginRequest(Zeze.Builtin.Online.ReLogin rpc) throws Throwable {
        var session = ProviderUserSession.get(rpc);

        var account = _taccount.get(session.getAccount());
        if (null == account)
            return ErrorCode(ResultCodeAccountNotExist);

        var online = _tonline.get(session.getAccount());
        if (null == online)
            return ErrorCode(ResultCodeOnlineDataNotFound);

        var local = _tlocal.getOrAdd(session.getAccount());
        var version = _tversion.getOrAdd(session.getAccount());

        var loginLocal = local.getLogins().getOrAdd(rpc.Argument.getClientId());
        var loginVersion = version.getLogins().getOrAdd(rpc.Argument.getClientId());
        // login exist && not local
        if (loginVersion.getLoginVersion() != 0 && loginVersion.getLoginVersion() != loginLocal.getLoginVersion()) {
            // nowait
            RedirectNotify(loginVersion.getServerId(), session.getAccount());
        }
        var loginVersionSerialId = account.getLastLoginVersion() + 1;
        account.setLastLoginVersion(loginVersionSerialId);
        loginVersion.setLoginVersion(loginVersionSerialId);
        loginLocal.setLoginVersion(loginVersionSerialId);

        /////////////////////////////////////////////////////////////
        // 当LinkName,LinkSid没有变化的时候，保持记录是读取状态，不会申请写锁。
        // 因为Online数据可能会被很多地方缓存，写操作会造成缓存失效。
        // see Linkd.StableLinkSid
        var loginOnline = online.getLogins().getOrAdd(rpc.Argument.getClientId());
        if (false == loginOnline.getLinkName().equals(session.getLinkName()))
            loginOnline.setLinkName(session.getLinkName());
        if (loginOnline.getLinkSid() != session.getLinkSid())
            loginOnline.setLinkSid(session.getLinkSid());
        /////////////////////////////////////////////////////////////

        ReloginTrigger(session.getAccount(), rpc.Argument.getClientId());

        // 先发结果，再发送同步数据（ReliableNotifySync）。
        // 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
        session.SendResponseWhileCommit(rpc);
        Transaction.getCurrent().RunWhileCommit(() -> {
            var setUserState = new SetUserState();
            setUserState.Argument.setLinkSid(session.getLinkSid());
            setUserState.Argument.setContext(rpc.Argument.getClientId());
            rpc.getSender().Send(setUserState); // 直接使用link连接。
        });

        var syncResultCode = ReliableNotifySync(session.getAccount(), rpc.Argument.getClientId(),
                session, rpc.Argument.getReliableNotifyConfirmIndex(), true);

        if (syncResultCode != ResultCodeSuccess)
            return ErrorCode((short)syncResultCode);

        //App.Load.LoginCount.IncrementAndGet();
        return Procedure.Success;
    }
}
