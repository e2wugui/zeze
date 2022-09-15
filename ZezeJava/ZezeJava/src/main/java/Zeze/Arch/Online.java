package Zeze.Arch;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.AppBase;
import Zeze.Arch.Gen.GenModule;
import Zeze.Builtin.Online.BAny;
import Zeze.Builtin.Online.BLocal;
import Zeze.Builtin.Online.BLocals;
import Zeze.Builtin.Online.BNotify;
import Zeze.Builtin.Online.BReliableNotify;
import Zeze.Builtin.Online.SReliableNotify;
import Zeze.Builtin.Online.taccount;
import Zeze.Builtin.Provider.BBroadcast;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.BSend;
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
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
import Zeze.Util.IntHashMap;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import Zeze.Util.Random;
import Zeze.Util.RedirectGenMain;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Online extends AbstractOnline {
	private static final Logger logger = LogManager.getLogger(Online.class);

	public final ProviderApp ProviderApp;
	//public final LoadReporter LoadReporter;
	private final AtomicLong _LoginTimes = new AtomicLong();

	private final EventDispatcher LoginEvents = new EventDispatcher("Online.Login");
	private final EventDispatcher ReloginEvents = new EventDispatcher("Online.Relogin");
	private final EventDispatcher LogoutEvents = new EventDispatcher("Online.Logout");
	private final EventDispatcher LocalRemoveEvents = new EventDispatcher("Online.Local.Remove");

	public interface TransmitAction {
		/**
		 * @param senderAccount 查询发起者，结果发送给他
		 * @param target        查询目标
		 * @return 按普通事务处理过程返回值处理
		 */
		long call(String senderAccount, String senderClientId, String target, Binary parameter);
	}

	private final ConcurrentHashMap<String, TransmitAction> TransmitActions = new ConcurrentHashMap<>();
	private Future<?> VerifyLocalTimer;

	public static Online create(AppBase app) {
		return GenModule.createRedirectModule(Online.class, app);
	}

	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return bean.typeId();
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		throw new UnsupportedOperationException("Online Memory Table Dynamic Only.");
	}

	@Deprecated // 仅供内部使用, 正常创建应该调用 Online.create(app)
	public Online() {
		if (Reflect.stackWalker.getCallerClass() != RedirectGenMain.class)
			throw new IllegalCallerException(Reflect.stackWalker.getCallerClass().getName());
		ProviderApp = null;
		//LoadReporter = null;
	}

	public Online(AppBase app) {
		if (app != null) {
			this.ProviderApp = app.getZeze().Redirect.ProviderApp;
			RegisterProtocols(ProviderApp.ProviderService);
			RegisterZezeTables(ProviderApp.Zeze);
		} else // for RedirectGenMain
			this.ProviderApp = null;

		//LoadReporter = new LoadReporter(this);
	}

	public void Start() {
		//LoadReporter.StartTimerTask();
		VerifyLocalTimer = Task.scheduleAtUnsafe(3 + Random.getInstance().nextInt(3), 10, this::verifyLocal); // at 3:10 - 6:10
		ProviderApp.BuiltinModules.put(this.getFullName(), this);
	}

	public void Stop() {
		//LoadReporter.Stop();
		if (VerifyLocalTimer != null)
			VerifyLocalTimer.cancel(false);
	}

	@Override
	public void UnRegister() {
		if (ProviderApp != null) {
			UnRegisterZezeTables(ProviderApp.Zeze);
			UnRegisterProtocols(ProviderApp.ProviderService);
		}
	}

	public taccount getTableAccount() {
		return _taccount;
	}

	public int getLocalCount() {
		return _tlocal.getCacheSize();
	}

	public long walkLocal(TableWalkHandle<String, BLocals> walker) {
		return _tlocal.WalkCache(walker);
	}

	public long getLoginTimes() {
		return _LoginTimes.get();
	}

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

	public ConcurrentHashMap<String, TransmitAction> getTransmitActions() {
		return TransmitActions;
	}

	public void setLocalBean(String account, String clientId, String key, Bean bean) {
		var bLocals = _tlocal.get(account);
		if (bLocals == null)
			throw new IllegalStateException("roleId not online. " + account);
		var login = bLocals.getLogins().get(clientId);
		if (login == null) {
			login = new BLocal();
			if (bLocals.getLogins().put(clientId, login) != null)
				throw new IllegalStateException("duplicate clientId:" + clientId);
		}
		var bAny = new BAny();
		bAny.getAny().setBean(bean);
		login.getDatas().put(key, bAny);
	}

	public void removeLocalBean(String account, String clientId, String key) {
		var bLocals = _tlocal.get(account);
		if (null == bLocals)
			return;
		var login = bLocals.getLogins().get(clientId);
		if (null == login)
			return;
		login.getDatas().remove(key);
	}

	@SuppressWarnings("unchecked")
	public <T extends Bean> T getLocalBean(String account, String clientId, String key) {
		var bLocals = _tlocal.get(account);
		if (bLocals == null)
			return null;
		var login = bLocals.getLogins().get(clientId);
		if (login == null)
			return null;
		var data = login.getDatas().get(key);
		if (data == null)
			return null;
		return (T)data.getAny().getBean();
	}

	private void RemoveLocalAndTrigger(String account, String clientId) throws Throwable {
		var bLocals = _tlocal.get(account);
		var localData = bLocals.getLogins().remove(clientId);
		var arg = new LocalRemoveEventArgument(account, clientId, localData != null ? localData.Copy() : null);

		if (bLocals.getLogins().isEmpty())
			_tlocal.remove(account); // remove first

		LocalRemoveEvents.triggerEmbed(this, arg);
		LocalRemoveEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
		Transaction.whileCommit(() -> LocalRemoveEvents.triggerThread(this, arg));
	}

	private void LogoutTriggerExtra(String account, String clientId) throws Throwable {
		var bOnline = _tonline.get(account);
		var onlineData = bOnline.getLogins().get(clientId);
		var arg = new LogoutEventArgument(account, clientId, onlineData != null ? onlineData.Copy() : null);

		LogoutEvents.triggerEmbed(this, arg);
		LogoutEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
		Transaction.whileCommit(() -> LogoutEvents.triggerThread(this, arg));
	}

	private void LogoutTrigger(String account, String clientId) throws Throwable {
		var bOnline = _tonline.get(account);
		var onlineData = bOnline.getLogins().remove(clientId);
		var arg = new LogoutEventArgument(account, clientId, onlineData != null ? onlineData.Copy() : null);

		if (bOnline.getLogins().isEmpty())
			_tonline.remove(account); // remove first

		LogoutEvents.triggerEmbed(this, arg);
		LogoutEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
		Transaction.whileCommit(() -> LogoutEvents.triggerThread(this, arg));
	}

	private void LoginTrigger(String account, String clientId) throws Throwable {
		var arg = new LoginArgument(account, clientId);
		LoginEvents.triggerEmbed(this, arg);
		LoginEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
		Transaction.whileCommit(() -> LoginEvents.triggerThread(this, arg));
		_LoginTimes.incrementAndGet();
	}

	private void ReloginTrigger(String account, String clientId) throws Throwable {
		var arg = new LoginArgument(account, clientId);
		ReloginEvents.triggerEmbed(this, arg);
		ReloginEvents.triggerProcedure(ProviderApp.Zeze, this, arg);
		Transaction.whileCommit(() -> ReloginEvents.triggerThread(this, arg));
		_LoginTimes.incrementAndGet();
	}

	public void OnLinkBroken(String account, String clientId, String linkName, long linkSid) throws Throwable {
		long currentLoginVersion;
		{
			var online = _tonline.get(account);
			var loginOnline = online.getLogins().get(clientId);
			if (loginOnline == null)
				return;
			// skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
			if (!loginOnline.getLinkName().equals(linkName) || loginOnline.getLinkSid() != linkSid)
				return;

			var version = _tversion.getOrAdd(account);
			var local = _tlocal.get(account);
			if (local == null)
				return; // 不在本机登录。
			var loginLocal = local.getLogins().get(clientId);
			if (loginLocal == null)
				return; // 不在本机登录。
			var loginVersion = version.getLogins().get(clientId);
			if (loginVersion == null)
				return; // 不存在登录。
			currentLoginVersion = loginLocal.getLoginVersion();
			if (loginVersion.getLoginVersion() != currentLoginVersion)
				RemoveLocalAndTrigger(account, clientId); // 本机数据已经过时，马上删除。
		}
		Transaction.whileCommit(() -> Task.schedule(ProviderApp.Zeze.getConfig().getOnlineLogoutDelay(), () -> {
			// TryRemove
			ProviderApp.Zeze.NewProcedure(() -> {
				// local online 独立判断version分别尝试删除。
				var local = _tlocal.get(account);
				if (local != null) {
					var loginLocal = local.getLogins().get(clientId);
					if (loginLocal != null && loginLocal.getLoginVersion() == currentLoginVersion)
						RemoveLocalAndTrigger(account, clientId);
				}
				// 如果玩家在延迟期间建立了新的登录，下面版本号判断会失败。
				var online = _tonline.get(account);
				var version = _tversion.getOrAdd(account);
				if (online != null) {
					var loginVersion = version.getLogins().get(clientId);
					if (loginVersion != null && loginVersion.getLoginVersion() == currentLoginVersion)
						LogoutTrigger(account, clientId);
				}
				return Procedure.Success;
			}, "Onlines.OnLinkBroken").Call();
		}));
	}

	public void addReliableNotifyMark(String account, String clientId, String listenerName) throws Throwable {
		var online = _tonline.get(account);
		if (online == null)
			throw new IllegalStateException("Not Online. AddReliableNotifyMark: " + listenerName);
		var version = _tversion.getOrAdd(account);
		version.getLogins().getOrAdd(clientId).getReliableNotifyMark().add(listenerName);
	}

	public void removeReliableNotifyMark(String account, String clientId, String listenerName) {
		// 移除尽量通过，不做任何判断。
		var login = _tversion.getOrAdd(account).getLogins().get(clientId);
		if (login != null)
			login.getReliableNotifyMark().remove(listenerName);
	}

	public void sendReliableNotifyWhileCommit(String account, String clientId, String listenerName, Protocol<?> p) {
		Transaction.whileCommit(() -> sendReliableNotify(account, clientId, listenerName, p));
	}

	public void SendReliableNotifyWhileCommit(
			String account, String clientId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendReliableNotify(account, clientId, listenerName, typeId, fullEncodedProtocol));
	}

	public void sendReliableNotifyWhileRollback(String account, String clientId, String listenerName, Protocol<?> p) {
		Transaction.whileRollback(() -> sendReliableNotify(account, clientId, listenerName, p));
	}

	public void sendReliableNotifyWhileRollback(
			String account, String clientId, String listenerName, int typeId, Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> sendReliableNotify(
				account, clientId, listenerName, typeId, fullEncodedProtocol));
	}

	public void sendReliableNotify(String account, String clientId, String listenerName, Protocol<?> p) {
		sendReliableNotify(account, clientId, listenerName, p.getTypeId(), new Binary(p.encode()));
	}

	private Zeze.Collections.Queue<BNotify> OpenQueue(String account, String clientId) {
		return ProviderApp.Zeze.getQueueModule().open(
				"Zeze.Arch.Online.ReliableNotifyQueue:" + account + ":" + clientId, BNotify.class);
	}

	/**
	 * 发送在线可靠协议，如果不在线等，仍然不会发送哦。
	 *
	 * @param fullEncodedProtocol 协议必须先编码，因为会跨事务。
	 */
	public void sendReliableNotify(String account, String clientId, String listenerName, long typeId,
								   Binary fullEncodedProtocol) {
		ProviderApp.Zeze.runTaskOneByOneByKey(listenerName, "SendReliableNotify." + listenerName, () -> {
			var online = _tonline.get(account);
			if (online == null) // 完全离线，忽略可靠消息发送：可靠消息仅仅为在线提供服务，并不提供全局可靠消息。
				return Procedure.Success;
			var version = _tversion.getOrAdd(account);
			var login = version.getLogins().get(clientId);
			if (login == null || !login.getReliableNotifyMark().contains(listenerName))
				return Procedure.Success; // 相关数据装载的时候要同步设置这个。

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

			sendEmbed(List.of(new LoginKey(account, clientId)), notify.getTypeId(), new Binary(notify.encode()));
			return Procedure.Success;
		});
	}

	public boolean isLogin(String account, String clientId) {
		var online = _tonline.get(account);
		if (null == online)
			return false;
		return online.getLogins().containsKey(clientId);
	}

	public boolean isAccountLogin(String account) {
		return getAccountLoginCount(account) > 0;
	}

	public int getAccountLoginCount(String account) {
		var online = _tonline.get(account);
		if (null == online)
			return 0;
		return online.getLogins().size();
	}

	public Collection<LoginOnLink> groupByLink(Collection<LoginKey> logins) throws Throwable {
		var groups = new HashMap<String, LoginOnLink>();
		var groupNotOnline = new LoginOnLink(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.LinkName, groupNotOnline);

		for (var alogin : logins) {
			var online = _tonline.get(alogin.Account);
			if (online == null) {
				groupNotOnline.Logins.putIfAbsent(alogin, 0L);
				continue;
			}
			var login = online.getLogins().get(alogin.ClientId);
			if (login == null) {
				groupNotOnline.Logins.putIfAbsent(alogin, 0L);
				continue;
			}
			var connector = ProviderApp.ProviderService.getLinks().get(login.getLinkName());
			if (connector == null) {
				groupNotOnline.Logins.putIfAbsent(alogin, 0L);
				continue;
			}

			if (!connector.isHandshakeDone()) {
				groupNotOnline.Logins.putIfAbsent(alogin, 0L);
				continue;
			}
			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(login.getLinkName());
			if (group == null) {
				group = new LoginOnLink();
				group.LinkName = login.getLinkName();
				group.LinkSocket = connector.getSocket();
				// 上面online存在Login的时，下面version也肯定存在相应的Login。
				group.ServerId = _tversion.getOrAdd(alogin.Account).getLogins().getOrAdd(alogin.ClientId).getServerId();
				groups.putIfAbsent(group.LinkName, group);
			}
			group.Logins.putIfAbsent(alogin, login.getLinkSid());
		}
		return groups.values();
	}

	private long triggerLinkBroken(String linkName, Collection<Long> errorSids, Map<Long, KV<String, String>> contexts)
			throws Throwable {
		for (var sid : errorSids) {
			var ctx = contexts.get(sid);
			if (ctx != null)
				OnLinkBroken(ctx.getKey(), ctx.getValue(), linkName, sid);
		}
		return 0;
	}

	public void send(AsyncSocket to, Map<Long, KV<String, String>> contexts, Send send) {
		send.Send(to, rpc -> triggerLinkBroken(ProviderService.GetLinkName(to),
				send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
	}

	private void sendEmbed(Collection<LoginKey> logins, long typeId, Binary fullEncodedProtocol) throws Throwable {
		var groups = groupByLink(logins);
		Transaction.whileCommit(() -> {
			for (var group : groups) {
				if (group.LinkSocket == null)
					continue; // skip not online

				var send = new Send(new BSend(typeId, fullEncodedProtocol));
				send.Argument.getLinkSids().addAll(group.Logins.values());
				send(group.LinkSocket, group.Contexts, send);
			}
		});
	}

	public static class LoginOnLink {
		public String LinkName = ""; // empty when not online
		public AsyncSocket LinkSocket; // null if not online
		public int ServerId = -1;
		public long ProviderSessionId;
		public final HashMap<LoginKey, Long> Logins = new HashMap<>();
		public final HashMap<Long, KV<String, String>> Contexts = new HashMap<>();
	}

	public static class LoginKey {
		public final String Account;
		public final String ClientId;

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

	public void send(String account, String clientId, long typeId, Binary fullEncodedProtocol) {
		var login = new LoginKey(account, clientId);
		ProviderApp.Zeze.runTaskOneByOneByKey(login, "Online.send", () -> {
			sendEmbed(List.of(login), typeId, fullEncodedProtocol);
			return Procedure.Success;
		});
	}

	public void send(Collection<LoginKey> logins, long typeId, Binary fullEncodedProtocol) {
		ProviderApp.Zeze.getTaskOneByOneByKey().ExecuteCyclicBarrier(logins, ProviderApp.Zeze.NewProcedure(() -> {
			sendEmbed(logins, typeId, fullEncodedProtocol);
			return Procedure.Success;
		}, "Online.send"), null, DispatchMode.Normal);
	}

	public void send(String account, String clientId, Protocol<?> p) {
		send(account, clientId, p.getTypeId(), new Binary(p.encode()));
	}

	public void send(Collection<LoginKey> logins, Protocol<?> p) {
		send(logins, p.getTypeId(), new Binary(p.encode()));
	}

	public void sendWhileCommit(String account, String clientId, Protocol<?> p) {
		Transaction.whileCommit(() -> send(account, clientId, p));
	}

	public void sendWhileCommit(Collection<LoginKey> logins, Protocol<?> p) {
		Transaction.whileCommit(() -> send(logins, p));
	}

	public void sendWhileRollback(String account, String clientId, Protocol<?> p) {
		Transaction.whileRollback(() -> send(account, clientId, p));
	}

	public void sendWhileRollback(Collection<LoginKey> logins, Protocol<?> p) {
		Transaction.whileRollback(() -> send(logins, p));
	}

	public Collection<LoginOnLink> groupAccountsByLink(Collection<String> accounts) throws Throwable {
		var groups = new HashMap<String, LoginOnLink>();
		var groupNotOnline = new LoginOnLink(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.LinkName, groupNotOnline);

		for (var account : accounts) {
			var online = _tonline.get(account);
			if (online == null) {
				groupNotOnline.Logins.putIfAbsent(new LoginKey(account, ""), 0L);
				continue;
			}
			for (var e : online.getLogins().entrySet()) {
				var login = new LoginKey(account, e.getKey());
				var connector = ProviderApp.ProviderService.getLinks().get(e.getValue().getLinkName());
				if (connector == null) {
					groupNotOnline.Logins.putIfAbsent(login, 0L);
					continue;
				}
				if (!connector.isHandshakeDone()) {
					groupNotOnline.Logins.putIfAbsent(login, 0L);
					continue;
				}
				// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
				var group = groups.get(e.getValue().getLinkName());
				if (group == null) {
					group = new LoginOnLink();
					group.LinkName = e.getValue().getLinkName();
					group.LinkSocket = connector.getSocket();
					// 上面online存在Login的时，下面version也肯定存在相应的Login。
					group.ServerId = (_tversion.getOrAdd(account)).getLogins().getOrAdd(e.getKey()).getServerId();
					groups.putIfAbsent(group.LinkName, group);
				}
				group.Logins.putIfAbsent(login, e.getValue().getLinkSid());
				group.Contexts.putIfAbsent(e.getValue().getLinkSid(), KV.Create(login.Account, login.ClientId));
			}
		}
		return groups.values();
	}

	public void sendAccountsEmbed(Collection<String> accounts, long typeId, Binary fullEncodedProtocol,
								  OnlineSend sender) throws Throwable {
		var groups = groupAccountsByLink(accounts);
		Transaction.whileCommit(() -> {
			if (sender == null) {
				for (var group : groups) {
					if (group.LinkSocket == null)
						continue; // skip not online
					var send = new Send(new BSend(typeId, fullEncodedProtocol));
					send.Argument.getLinkSids().addAll(group.Logins.values());
					send(group.LinkSocket, group.Contexts, send);
				}
			} else {
				for (var group : groups) {
					if (!sender.send(group))
						break;
				}
			}
		});
	}

	public void sendAccount(String account, long typeId, Binary fullEncodedProtocol, OnlineSend sender) {
		ProviderApp.Zeze.runTaskOneByOneByKey(account, "Online.sendAccount", () -> {
			sendAccountsEmbed(List.of(account), typeId, fullEncodedProtocol, sender);
			return Procedure.Success;
		});
	}

	public void sendAccounts(Collection<String> accounts, long typeId, Binary fullEncodedProtocol, OnlineSend sender) {
		ProviderApp.Zeze.getTaskOneByOneByKey().ExecuteCyclicBarrier(accounts, ProviderApp.Zeze.NewProcedure(() -> {
			sendAccountsEmbed(accounts, typeId, fullEncodedProtocol, sender);
			return Procedure.Success;
		}, "Online.sendAccounts"), null, DispatchMode.Normal);
	}

	/**
	 * 给账号所有的登录终端发送消息。
	 */
	public void sendAccount(String account, Protocol<?> p, OnlineSend sender) {
		sendAccount(account, p.getTypeId(), new Binary(p.encode()), sender);
	}

	/**
	 * 给账号所有的登录终端发送消息。
	 */
	public void sendAccounts(Collection<String> accounts, Protocol<?> p, OnlineSend sender) {
		sendAccounts(accounts, p.getTypeId(), new Binary(p.encode()), sender);
	}

	public void sendAccountWhileCommit(String account, Protocol<?> p, OnlineSend sender) {
		Transaction.whileCommit(() -> sendAccount(account, p, sender));
	}

	public void sendAccountsWhileCommit(Collection<String> accounts, Protocol<?> p, OnlineSend sender) {
		Transaction.whileCommit(() -> sendAccounts(accounts, p, sender));
	}

	public void sendAccountWhileRollback(String account, Protocol<?> p, OnlineSend sender) {
		Transaction.whileRollback(() -> sendAccount(account, p, sender));
	}

	public void sendAccountsWhileRollback(Collection<String> accounts, Protocol<?> p, OnlineSend sender) {
		Transaction.whileRollback(() -> sendAccounts(accounts, p, sender));
	}

	/**
	 * 转发查询请求给RoleId。
	 *
	 * @param account    查询发起者，结果发送给他。
	 * @param actionName 查询处理的实现
	 * @param target     目标角色
	 */
	public void transmit(String account, String clientId, String actionName, String target, Serializable parameter)
			throws Throwable {
		transmit(account, clientId, actionName, List.of(target), parameter);
	}

	public void processTransmit(String account, String clientId, String actionName, Collection<String> accounts,
								Binary parameter) throws Throwable {
		var handle = TransmitActions.get(actionName);
		if (handle != null) {
			for (var target : accounts) {
				ProviderApp.Zeze.NewProcedure(() -> handle.call(account, clientId, target, parameter),
						"Arch.Online.Transmit:" + actionName).Call();
			}
		}
	}

	public static class RoleOnServer {
		public int ServerId = -1; // empty when not online
		public final HashSet<String> Accounts = new HashSet<>();

		public void addAll(HashSet<String> accounts) {
			Accounts.addAll(accounts);
		}
	}

	public IntHashMap<RoleOnServer> groupByServer(Collection<String> accounts) {
		var groups = new IntHashMap<RoleOnServer>();
		var groupNotOnline = new RoleOnServer(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.ServerId, groupNotOnline);

		for (var account : accounts) {
			var online = _tonline.get(account);
			if (online == null) {
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
			if (group == null) {
				group = new RoleOnServer();
				group.ServerId = serverId;
				groups.put(group.ServerId, group);
			}
			group.Accounts.add(account);
		}
		return groups;
	}

	private static RoleOnServer merge(RoleOnServer current, RoleOnServer m) {
		if (current == null)
			return m;
		current.addAll(m.Accounts);
		return current;
	}

	private void transmitInProcedure(String account, String clientId, String actionName, Collection<String> accounts,
									 Binary parameter) throws Throwable {
		if (ProviderApp.Zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().isEmpty()) {
			// 没有启用cache-sync，马上触发本地任务。
			processTransmit(account, clientId, actionName, accounts, parameter);
			return;
		}

		var groups = groupByServer(accounts);
		RoleOnServer groupLocal = null;
		for (var it = groups.iterator(); it.moveToNext(); ) {
			var group = it.value();
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
			if (parameter != null)
				transmit.Argument.setParameter(parameter);

			var ps = ProviderApp.ProviderDirectService.ProviderByServerId.get(group.ServerId);
			if (ps == null) {
				assert groupLocal != null;
				groupLocal.addAll(group.Accounts);
				continue;
			}
			var socket = ProviderApp.ProviderDirectService.GetSocket(ps.SessionId);
			if (socket == null) {
				assert groupLocal != null;
				groupLocal.addAll(group.Accounts);
				continue;
			}
			transmit.Send(socket);
		}
		assert groupLocal != null;
		if (groupLocal.Accounts.size() > 0)
			processTransmit(account, clientId, actionName, groupLocal.Accounts, parameter);
	}

	public void transmit(String account, String clientId, String actionName, Collection<String> targets,
						 Serializable parameter) throws Throwable {
		if (!TransmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);

		var binaryParam = parameter == null ? Binary.Empty : new Binary(ByteBuffer.encode(parameter));
		// 发送协议请求在另外的事务中执行。
		ProviderApp.Zeze.NewProcedure(() -> {
			transmitInProcedure(account, clientId, actionName, targets, binaryParam);
			return Procedure.Success;
		}, "Onlines.Transmit").Call();
	}

	public void transmitWhileCommit(String account, String clientId, String actionName, String target,
									Serializable parameter) {
		if (!TransmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> {
			try {
				transmit(account, clientId, actionName, target, parameter);
			} catch (Throwable e) {
				logger.error("", e);
			}
		});
	}

	public void transmitWhileCommit(String account, String clientId, String actionName, Collection<String> targets,
									Serializable parameter) {
		if (!TransmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> {
			try {
				transmit(account, clientId, actionName, targets, parameter);
			} catch (Throwable e) {
				logger.error("", e);
			}
		});
	}

	public void transmitWhileRollback(String account, String clientId, String actionName, String target,
									  Serializable parameter) {
		if (!TransmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> {
			try {
				transmit(account, clientId, actionName, target, parameter);
			} catch (Throwable e) {
				logger.error("", e);
			}
		});
	}

	public void transmitWhileRollback(String account, String clientId, String actionName, Collection<String> targets,
									  Serializable parameter) {
		if (!TransmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> {
			try {
				transmit(account, clientId, actionName, targets, parameter);
			} catch (Throwable e) {
				logger.error("", e);
			}
		});
	}

	private void broadcast(long typeId, Binary fullEncodedProtocol, int time) {
		var broadcast = new Broadcast(new BBroadcast(typeId, fullEncodedProtocol, time));
		for (var link : ProviderApp.ProviderService.getLinks().values()) {
			if (link.getSocket() != null)
				link.getSocket().Send(broadcast);
		}
	}

	public void broadcast(Protocol<?> p, int time) {
		broadcast(p.getTypeId(), new Binary(p.encode()), time);
	}

	private void verifyLocal() {
		var account = new OutObject<String>();
		_tlocal.WalkCache((k, v) -> {
			// 先得到roleId
			account.Value = k;
			return true;
		}, () -> {
			// 锁外执行事务
			try {
				ProviderApp.Zeze.NewProcedure(() -> {
					tryRemoveLocal(account.Value);
					return 0L;
				}, "VerifyLocal:" + account).Call();
			} catch (Throwable e) {
				logger.error("", e);
			}
		});
		// 随机开始时间，避免验证操作过于集中。3:10 - 5:10
		VerifyLocalTimer = Task.scheduleAtUnsafe(3 + Random.getInstance().nextInt(3), 10, this::verifyLocal); // at 3:10 - 6:10
	}

	private void tryRemoveLocal(String account) throws Throwable {
		var version = _tversion.getOrAdd(account);
		var local = _tlocal.get(account);
		if (local == null)
			return;
		// null == online && null == local -> do nothing
		// null != online && null == local -> do nothing

		var online = _tonline.get(account);
		if (online == null) {
			// remove all
			for (var loginKey : local.getLogins().keySet())
				RemoveLocalAndTrigger(account, loginKey);
		} else {
			// 在全局数据中查找login-local，删除不存在或者版本不匹配的。
			for (var loginLocal : local.getLogins().entrySet()) {
				var loginVersion = version.getLogins().get(loginLocal.getKey());
				if (loginVersion == null || loginVersion.getLoginVersion() != loginLocal.getValue().getLoginVersion())
					RemoveLocalAndTrigger(account, loginLocal.getKey());
			}
		}
	}

	@RedirectToServer
	protected void redirectNotify(int serverId, String account) throws Throwable {
		tryRemoveLocal(account);
	}

	@Override
	protected long ProcessLoginRequest(Zeze.Builtin.Online.Login rpc) throws Throwable {
		var session = ProviderUserSession.get(rpc);

		var account = _taccount.getOrAdd(session.getAccount());
		var online = _tonline.getOrAdd(session.getAccount());
		var local = _tlocal.getOrAdd(session.getAccount());
		var version = _tversion.getOrAdd(session.getAccount());

		var loginLocal = local.getLogins().getOrAdd(rpc.Argument.getClientId());
		var loginVersion = version.getLogins().getOrAdd(rpc.Argument.getClientId());

		if (loginVersion.getLoginVersion() != 0) {
			// login exist
			LogoutTriggerExtra(session.getAccount(), rpc.Argument.getClientId());
			if (loginVersion.getLoginVersion() != loginLocal.getLoginVersion()) {
				// not local
				redirectNotify(loginVersion.getServerId(), session.getAccount());
			}
		}
		var loginVersionSerialId = account.getLastLoginVersion() + 1;
		account.setLastLoginVersion(loginVersionSerialId);
		loginVersion.setLoginVersion(loginVersionSerialId);
		loginLocal.setLoginVersion(loginVersionSerialId);

		var loginOnline = online.getLogins().getOrAdd(rpc.Argument.getClientId());
		if (!loginOnline.getLinkName().equals(session.getLinkName())
				|| loginOnline.getLinkSid() != session.getLinkSid()) {
			ProviderApp.ProviderService.kick(loginOnline.getLinkName(), loginOnline.getLinkSid(),
					BKick.ErrorDuplicateLogin,
					"duplicate login " + session.getAccount() + ":" + rpc.Argument.getClientId());
		}

		/////////////////////////////////////////////////////////////
		// 当LinkName,LinkSid没有变化的时候，保持记录是读取状态，不会申请写锁。
		// 因为Online数据可能会被很多地方缓存，写操作会造成缓存失效。
		// see Linkd.StableLinkSid
		if (!loginOnline.getLinkName().equals(session.getLinkName()))
			loginOnline.setLinkName(session.getLinkName());
		if (loginOnline.getLinkSid() != session.getLinkSid())
			loginOnline.setLinkSid(session.getLinkSid());
		/////////////////////////////////////////////////////////////

		loginVersion.setReliableNotifyConfirmIndex(0);
		loginVersion.setReliableNotifyIndex(0);
		loginVersion.getReliableNotifyMark().clear();
		OpenQueue(session.getAccount(), rpc.Argument.getClientId()).clear();

		// var linkSession = (ProviderService.LinkSession)session.getLink().getUserState();
		loginVersion.setServerId(ProviderApp.Zeze.getConfig().getServerId());

		LoginTrigger(session.getAccount(), rpc.Argument.getClientId());

		// 先提交结果再设置状态。
		// see linkd::Zezex.Provider.ModuleProvider。ProcessBroadcast
		session.sendResponseWhileCommit(rpc);
		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			setUserState.Argument.setContext(rpc.Argument.getClientId());
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		//App.Load.LoginCount.IncrementAndGet();
		return Procedure.Success;
	}

	@Override
	protected long ProcessReLoginRequest(Zeze.Builtin.Online.ReLogin rpc) throws Throwable {
		var session = ProviderUserSession.get(rpc);

		var account = _taccount.get(session.getAccount());
		if (account == null)
			return ErrorCode(ResultCodeAccountNotExist);

		var online = _tonline.get(session.getAccount());
		if (online == null)
			return ErrorCode(ResultCodeOnlineDataNotFound);

		var local = _tlocal.getOrAdd(session.getAccount());
		var version = _tversion.getOrAdd(session.getAccount());

		var loginLocal = local.getLogins().getOrAdd(rpc.Argument.getClientId());
		var loginVersion = version.getLogins().getOrAdd(rpc.Argument.getClientId());

		if (loginVersion.getLoginVersion() != 0) {
			// login exist
			// relogin 不需要补充 Logout？
			// LogoutTriggerExtra(session.getAccount(), rpc.Argument.getClientId());
			if (loginVersion.getLoginVersion() != loginLocal.getLoginVersion()) {
				// not local
				redirectNotify(loginVersion.getServerId(), session.getAccount());
			}
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
		if (!loginOnline.getLinkName().equals(session.getLinkName()))
			loginOnline.setLinkName(session.getLinkName());
		if (loginOnline.getLinkSid() != session.getLinkSid())
			loginOnline.setLinkSid(session.getLinkSid());
		/////////////////////////////////////////////////////////////

		ReloginTrigger(session.getAccount(), rpc.Argument.getClientId());

		// 先发结果，再发送同步数据（ReliableNotifySync）。
		// 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
		session.sendResponseWhileCommit(rpc);
		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			setUserState.Argument.setContext(rpc.Argument.getClientId());
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		var syncResultCode = reliableNotifySync(session.getAccount(), rpc.Argument.getClientId(),
				session, rpc.Argument.getReliableNotifyConfirmIndex(), true);

		if (syncResultCode != ResultCodeSuccess)
			return ErrorCode((short)syncResultCode);

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
			redirectNotify(loginVersion.getServerId(), session.getAccount()); // nowait
		if (local != null)
			RemoveLocalAndTrigger(session.getAccount(), clientId);
		if (online != null)
			LogoutTrigger(session.getAccount(), clientId);

		// 先设置状态，再发送Logout结果。
		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		session.sendResponseWhileCommit(rpc);
		// 在 OnLinkBroken 时处理。可以同时处理网络异常的情况。
		// App.Load.LogoutCount.IncrementAndGet();
		return Procedure.Success;
	}

	private int reliableNotifySync(String account, String clientId,
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
			var notify = new SReliableNotify(new BReliableNotify(index));
			queue.walk((node, bNofity) -> {
				notify.Argument.getNotifies().add(bNofity.getFullEncodedProtocol());
				return true;
			});
			session.sendResponseWhileCommit(notify);
		}
		return ResultCodeSuccess;
	}

	@Override
	protected long ProcessReliableNotifyConfirmRequest(Zeze.Builtin.Online.ReliableNotifyConfirm rpc) throws Throwable {
		var session = ProviderUserSession.get(rpc);

		var clientId = session.getContext();
		var online = _tonline.get(session.getAccount());
		if (online == null)
			return ErrorCode(ResultCodeOnlineDataNotFound);

		var syncResultCode = reliableNotifySync(session.getAccount(), clientId,
				session, rpc.Argument.getReliableNotifyConfirmIndex(), rpc.Argument.isSync());
		session.sendResponseWhileCommit(rpc); // 同步前提交。

		if (ResultCodeSuccess != syncResultCode)
			return ErrorCode((short)syncResultCode);

		return Procedure.Success;
	}
}
