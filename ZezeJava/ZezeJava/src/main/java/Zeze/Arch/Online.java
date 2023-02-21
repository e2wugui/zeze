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
import Zeze.Builtin.Online.BOnlines;
import Zeze.Builtin.Online.BReliableNotify;
import Zeze.Builtin.Online.BVersions;
import Zeze.Builtin.Online.SReliableNotify;
import Zeze.Builtin.Provider.BBroadcast;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.Broadcast;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Builtin.ProviderDirect.TransmitAccount;
import Zeze.Collections.BeanFactory;
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
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.EventDispatcher;
import Zeze.Util.IntHashMap;
import Zeze.Util.KV;
import Zeze.Util.LongList;
import Zeze.Util.OutObject;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TransactionLevelAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Online extends AbstractOnline {
	private static final Logger logger = LogManager.getLogger(Online.class);

	public final ProviderApp providerApp;
	public final ProviderLoad load;
	private final AtomicLong loginTimes = new AtomicLong();

	private final EventDispatcher loginEvents = new EventDispatcher("Online.Login");
	private final EventDispatcher reloginEvents = new EventDispatcher("Online.Relogin");
	private final EventDispatcher logoutEvents = new EventDispatcher("Online.Logout");
	private final EventDispatcher localRemoveEvents = new EventDispatcher("Online.Local.Remove");

	public interface TransmitAction {
		/**
		 * @param senderAccount 查询发起者，结果发送给他
		 * @param target        查询目标
		 * @return 按普通事务处理过程返回值处理
		 */
		long call(String senderAccount, String senderClientId, String target, Binary parameter);
	}

	public ProviderLoad getLoad() {
		return load;
	}

	private final ConcurrentHashMap<String, TransmitAction> transmitActions = new ConcurrentHashMap<>();
	private Future<?> verifyLocalTimer;

	public static Online create(AppBase app) {
		return GenModule.createRedirectModule(Online.class, app);
	}

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return bean.typeId();
	}

	public static final BeanFactory beanFactory = new BeanFactory();

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static void register(Class<? extends Bean> cls) {
		beanFactory.register(cls);
	}

	protected Online(AppBase app) {
		providerApp = app.getZeze().redirect.providerApp;
		RegisterProtocols(providerApp.providerService);
		RegisterZezeTables(providerApp.zeze);
		load = new ProviderLoad(this);
	}

	public void start() {
		load.start();
		verifyLocalTimer = Task.scheduleAtUnsafe(3 + Random.getInstance().nextInt(3), 10, this::verifyLocal); // at 3:10 - 6:10
		providerApp.builtinModules.put(this.getFullName(), this);
	}

	public void stop() {
		load.stop();
		if (verifyLocalTimer != null)
			verifyLocalTimer.cancel(false);
	}

	@Override
	public void UnRegister() {
		if (providerApp != null) {
			UnRegisterZezeTables(providerApp.zeze);
			UnRegisterProtocols(providerApp.providerService);
		}
	}

	public BOnlines getOnline(String account) {
		return _tonline.get(account);
	}

	public BVersions getVersion(String account) {
		return _tversion.get(account);
	}

	public int getLocalCount() {
		return _tlocal.getCacheSize();
	}

	public long walkLocal(TableWalkHandle<String, BLocals> walker) {
		return _tlocal.walkCache(walker);
	}

	public long getLoginTimes() {
		return loginTimes.get();
	}

	public EventDispatcher getLoginEvents() {
		return loginEvents;
	}

	public EventDispatcher getReloginEvents() {
		return reloginEvents;
	}

	public EventDispatcher getLogoutEvents() {
		return logoutEvents;
	}

	public EventDispatcher getLocalRemoveEvents() {
		return localRemoveEvents;
	}

	public ConcurrentHashMap<String, TransmitAction> getTransmitActions() {
		return transmitActions;
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

	@SuppressWarnings("unchecked")
	public <T extends Bean> T getOrAddLocalBean(String account, String clientId, String key, T defaultHint) {
		var bLocals = _tlocal.getOrAdd(account);
		var login = bLocals.getLogins().getOrAdd(clientId);
		var bAny = login.getDatas().getOrAdd(key);
		if (bAny.getAny().getBean().typeId() == defaultHint.typeId())
			return (T)bAny.getAny().getBean();
		bAny.getAny().setBean(defaultHint);
		return defaultHint;
	}

	private long removeLocalAndTrigger(String account, String clientId) throws Exception {
		var bLocals = _tlocal.get(account);
		var localData = bLocals.getLogins().remove(clientId);
		var arg = new LocalRemoveEventArgument(account, clientId, localData);

		if (bLocals.getLogins().isEmpty())
			_tlocal.remove(account); // remove first

		var ret = localRemoveEvents.triggerEmbed(this, arg);
		if (ret != 0)
			return ret;
		localRemoveEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> localRemoveEvents.triggerThread(this, arg));
		return 0;
	}

	private long logoutTriggerExtra(String account, String clientId) throws Exception {
		var bOnline = _tonline.get(account);
		var onlineData = bOnline.getLogins().get(clientId);
		var arg = new LogoutEventArgument(account, clientId, onlineData);

		var ret = logoutEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		logoutEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> logoutEvents.triggerThread(this, arg));
		return 0;
	}

	private long logoutTrigger(String account, String clientId) throws Exception {
		var bOnline = _tonline.get(account);
		var onlineData = bOnline.getLogins().remove(clientId);
		var arg = new LogoutEventArgument(account, clientId, onlineData);

		if (bOnline.getLogins().isEmpty())
			_tonline.remove(account); // remove first

		var ret = logoutEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		logoutEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> logoutEvents.triggerThread(this, arg));
		return 0;
	}

	private long loginTrigger(String account, String clientId) throws Exception {
		var arg = new LoginArgument(account, clientId);
		var ret = loginEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		loginEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> loginEvents.triggerThread(this, arg));
		loginTimes.incrementAndGet();
		return 0;
	}

	private long reloginTrigger(String account, String clientId) throws Exception {
		var arg = new LoginArgument(account, clientId);
		var ret = reloginEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		reloginEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> reloginEvents.triggerThread(this, arg));
		loginTimes.incrementAndGet();
		return 0;
	}

	public long linkBroken(String account, String clientId, String linkName, long linkSid) throws Exception {
		long currentLoginVersion;
		{
			var online = _tonline.get(account);
			var loginOnline = online.getLogins().get(clientId);
			if (loginOnline == null)
				return 0;
			// skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
			if (!loginOnline.getLinkName().equals(linkName) || loginOnline.getLinkSid() != linkSid)
				return 0;

			var version = _tversion.getOrAdd(account);
			var local = _tlocal.get(account);
			if (local == null)
				return 0; // 不在本机登录。
			var loginLocal = local.getLogins().get(clientId);
			if (loginLocal == null)
				return 0; // 不在本机登录。
			var loginVersion = version.getLogins().get(clientId);
			if (loginVersion == null)
				return 0; // 不存在登录。
			currentLoginVersion = loginLocal.getLoginVersion();
			if (loginVersion.getLoginVersion() != currentLoginVersion) {
				var ret = removeLocalAndTrigger(account, clientId); // 本机数据已经过时，马上删除。
				if (ret != 0)
					return ret;
			}
		}
		Transaction.whileCommit(() -> Task.schedule(providerApp.zeze.getConfig().getOnlineLogoutDelay(), () -> {
			// TryRemove
			providerApp.zeze.newProcedure(() -> {
				// local online 独立判断version分别尝试删除。
				var local = _tlocal.get(account);
				if (local != null) {
					var loginLocal = local.getLogins().get(clientId);
					if (loginLocal != null && loginLocal.getLoginVersion() == currentLoginVersion) {
						var ret = removeLocalAndTrigger(account, clientId);
						if (ret != 0)
							return ret;
					}
				}
				// 如果玩家在延迟期间建立了新的登录，下面版本号判断会失败。
				var online = _tonline.get(account);
				var version = _tversion.getOrAdd(account);
				if (online != null) {
					var loginVersion = version.getLogins().get(clientId);
					if (loginVersion != null && loginVersion.getLoginVersion() == currentLoginVersion) {
						loginVersion.setLogoutVersion(loginVersion.getLoginVersion());
						var ret = logoutTrigger(account, clientId);
						if (0 != ret)
							return ret;
					}
				}
				return Procedure.Success;
			}, "Onlines.OnLinkBroken").call();
		}));
		return 0;
	}

	public void addReliableNotifyMark(String account, String clientId, String listenerName) {
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

	private Zeze.Collections.Queue<BNotify> openQueue(String account, String clientId) {
		return providerApp.zeze.getQueueModule().open(
				"Zeze.Arch.Online.ReliableNotifyQueue:" + account + ":" + clientId, BNotify.class);
	}

	/**
	 * 发送在线可靠协议，如果不在线等，仍然不会发送哦。
	 *
	 * @param fullEncodedProtocol 协议必须先编码，因为会跨事务。
	 */
	public void sendReliableNotify(String account, String clientId, String listenerName, long typeId,
								   Binary fullEncodedProtocol) {
		providerApp.zeze.runTaskOneByOneByKey(listenerName, "SendReliableNotify." + listenerName, () -> {
			var online = _tonline.get(account);
			if (online == null) // 完全离线，忽略可靠消息发送：可靠消息仅仅为在线提供服务，并不提供全局可靠消息。
				return Procedure.Success;
			var version = _tversion.getOrAdd(account);
			var login = version.getLogins().get(clientId);
			if (login == null || !login.getReliableNotifyMark().contains(listenerName))
				return Procedure.Success; // 相关数据装载的时候要同步设置这个。

			// 先保存在再发送，然后客户端还会确认。
			// see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm 的实现。
			var queue = openQueue(account, clientId);
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

	public Long getLogoutVersion(String account, String clientId) {
		/* 不再检查offline。
		var online = _tonline.get(account);
		if (null == online)
			return null; // is not online
		var login = online.getLogins().get(clientId);
		if (null == login)
			return null; // is not login
		*/
		var version = _tversion.get(account);
		if (null == version)
			return null; // no version
		var loginVersion = version.getLogins().get(clientId);
		if (null == loginVersion)
			return null; // no login version
		return loginVersion.getLogoutVersion();
	}

	public Long getLoginVersion(String account, String clientId) {
		var version = _tversion.get(account);
		if (null == version)
			return null; // no version
		var loginVersion = version.getLogins().get(clientId);
		if (null == loginVersion)
			return null; // no login version
		return loginVersion.getLoginVersion();
	}

	public Long getLocalLoginVersion(String account, String clientId) {
		var local = _tlocal.get(account);
		if (null == local)
			return null;
		var login = local.getLogins().get(clientId);
		if (null == login)
			return null;
		return login.getLoginVersion();
	}

	public Long getGlobalLoginVersion(String account, String clientId) {
		var version = _tversion.get(account);
		if (null == version)
			return null;
		var login = version.getLogins().get(clientId);
		if (null == login)
			return null;
		return login.getLoginVersion();
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

	public Collection<LoginOnLink> groupByLink(Collection<LoginKey> logins) {
		var groups = new HashMap<String, LoginOnLink>();
		var groupNotOnline = new LoginOnLink(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.linkName, groupNotOnline);

		for (var alogin : logins) {
			var online = _tonline.get(alogin.account);
			if (online == null) {
				groupNotOnline.logins.putIfAbsent(alogin, 0L);
				continue;
			}
			var login = online.getLogins().get(alogin.clientId);
			if (login == null) {
				groupNotOnline.logins.putIfAbsent(alogin, 0L);
				continue;
			}
			var connector = providerApp.providerService.getLinks().get(login.getLinkName());
			if (connector == null) {
				groupNotOnline.logins.putIfAbsent(alogin, 0L);
				continue;
			}

			if (!connector.isHandshakeDone()) {
				groupNotOnline.logins.putIfAbsent(alogin, 0L);
				continue;
			}
			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(login.getLinkName());
			if (group == null) {
				group = new LoginOnLink();
				group.linkName = login.getLinkName();
				group.linkSocket = connector.getSocket();
				// 上面online存在Login的时，下面version也肯定存在相应的Login。
				group.serverId = _tversion.getOrAdd(alogin.account).getLogins().getOrAdd(alogin.clientId).getServerId();
				groups.putIfAbsent(group.linkName, group);
			}
			group.logins.putIfAbsent(alogin, login.getLinkSid());
		}
		return groups.values();
	}

	private long triggerLinkBroken(String linkName, LongList errorSids, Map<Long, KV<String, String>> contexts) {
		errorSids.foreach(sid -> providerApp.zeze.newProcedure(() -> {
			var ctx = contexts.get(sid);
			return ctx != null ? linkBroken(ctx.getKey(), ctx.getValue(), linkName, sid) : 0;
		}, "triggerLinkBroken").call());
		return 0;
	}

	public void send(AsyncSocket to, Map<Long, KV<String, String>> contexts, Send send) {
		send.Send(to, rpc -> triggerLinkBroken(ProviderService.getLinkName(to),
				send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
	}

	private void sendEmbed(Collection<LoginKey> logins, long typeId, Binary fullEncodedProtocol) {
		var groups = groupByLink(logins);
		Transaction.whileCommit(() -> {
			for (var group : groups) {
				if (group.linkSocket == null)
					continue; // skip not online

				var send = new Send(new Zeze.Arch.Beans.BSend(typeId, fullEncodedProtocol));
				send.Argument.getLinkSids().addAll(group.logins.values());
				send(group.linkSocket, group.contexts, send);
			}
		});
	}

	public static class LoginOnLink {
		public String linkName = ""; // empty when not online
		public AsyncSocket linkSocket; // null if not online
		public int serverId = -1;
		public long providerSessionId;
		public final HashMap<LoginKey, Long> logins = new HashMap<>();
		public final HashMap<Long, KV<String, String>> contexts = new HashMap<>();
	}

	public static class LoginKey {
		public final String account;
		public final String clientId;

		public LoginKey(String account, String clientId) {
			this.account = account;
			this.clientId = clientId;
		}

		@Override
		public int hashCode() {
			final int _prime_ = 31;
			int _h_ = 0;
			_h_ = _h_ * _prime_ + account.hashCode();
			_h_ = _h_ * _prime_ + clientId.hashCode();
			return _h_;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof LoginKey) {
				var login = (LoginKey)obj;
				return account.equals(login.account) && clientId.equals(login.clientId);
			}
			return false;
		}
	}

	public void send(String account, String clientId, long typeId, Binary fullEncodedProtocol) {
		var login = new LoginKey(account, clientId);
		providerApp.zeze.runTaskOneByOneByKey(login, "Online.send", () -> {
			sendEmbed(List.of(login), typeId, fullEncodedProtocol);
			return Procedure.Success;
		});
	}

	public void send(Collection<LoginKey> logins, long typeId, Binary fullEncodedProtocol) {
		providerApp.zeze.getTaskOneByOneByKey().executeCyclicBarrier(logins, providerApp.zeze.newProcedure(() -> {
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

	public Collection<LoginOnLink> groupAccountsByLink(Collection<String> accounts) {
		var groups = new HashMap<String, LoginOnLink>();
		var groupNotOnline = new LoginOnLink(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.linkName, groupNotOnline);

		for (var account : accounts) {
			var online = _tonline.get(account);
			if (online == null) {
				groupNotOnline.logins.putIfAbsent(new LoginKey(account, ""), 0L);
				continue;
			}
			for (var e : online.getLogins().entrySet()) {
				var login = new LoginKey(account, e.getKey());
				var connector = providerApp.providerService.getLinks().get(e.getValue().getLinkName());
				if (connector == null) {
					groupNotOnline.logins.putIfAbsent(login, 0L);
					continue;
				}
				if (!connector.isHandshakeDone()) {
					groupNotOnline.logins.putIfAbsent(login, 0L);
					continue;
				}
				// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
				var group = groups.get(e.getValue().getLinkName());
				if (group == null) {
					group = new LoginOnLink();
					group.linkName = e.getValue().getLinkName();
					group.linkSocket = connector.getSocket();
					// 上面online存在Login的时，下面version也肯定存在相应的Login。
					group.serverId = (_tversion.getOrAdd(account)).getLogins().getOrAdd(e.getKey()).getServerId();
					groups.putIfAbsent(group.linkName, group);
				}
				group.logins.putIfAbsent(login, e.getValue().getLinkSid());
				group.contexts.putIfAbsent(e.getValue().getLinkSid(), KV.create(login.account, login.clientId));
			}
		}
		return groups.values();
	}

	public void sendAccountsEmbed(Collection<String> accounts, long typeId, Binary fullEncodedProtocol,
								  OnlineSend sender) {
		var groups = groupAccountsByLink(accounts);
		Transaction.whileCommit(() -> {
			if (sender == null) {
				for (var group : groups) {
					if (group.linkSocket == null)
						continue; // skip not online
					var send = new Send(new Zeze.Arch.Beans.BSend(typeId, fullEncodedProtocol));
					send.Argument.getLinkSids().addAll(group.logins.values());
					send(group.linkSocket, group.contexts, send);
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
		providerApp.zeze.runTaskOneByOneByKey(account, "Online.sendAccount", () -> {
			sendAccountsEmbed(List.of(account), typeId, fullEncodedProtocol, sender);
			return Procedure.Success;
		});
	}

	public void sendAccounts(Collection<String> accounts, long typeId, Binary fullEncodedProtocol, OnlineSend sender) {
		providerApp.zeze.getTaskOneByOneByKey().executeCyclicBarrier(accounts, providerApp.zeze.newProcedure(() -> {
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
	public void transmit(String account, String clientId, String actionName, String target, Serializable parameter) {
		transmit(account, clientId, actionName, List.of(target), parameter);
	}

	public void processTransmit(String account, String clientId, String actionName, Collection<String> accounts,
								Binary parameter) {
		var handle = transmitActions.get(actionName);
		if (handle != null) {
			for (var target : accounts) {
				providerApp.zeze.newProcedure(() -> handle.call(account, clientId, target, parameter),
						"Arch.Online.Transmit:" + actionName).call();
			}
		}
	}

	public static class RoleOnServer {
		public int serverId = -1; // empty when not online
		public final HashSet<String> accounts = new HashSet<>();

		public void addAll(HashSet<String> accounts) {
			this.accounts.addAll(accounts);
		}
	}

	public IntHashMap<RoleOnServer> groupByServer(Collection<String> accounts) {
		var groups = new IntHashMap<RoleOnServer>();
		var groupNotOnline = new RoleOnServer(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.serverId, groupNotOnline);

		for (var account : accounts) {
			var online = _tonline.get(account);
			if (online == null) {
				groupNotOnline.accounts.add(account);
				continue;
			}
			var version = _tversion.getOrAdd(account);
			if (version.getLogins().isEmpty()) {
				// null != online 意味着这里肯定不为0，不会到达这个分支。
				// 下面要求Logins.Count必须大于0，判断一下吧。
				groupNotOnline.accounts.add(account);
				continue;
			}
			var serverId = version.getLogins().iterator().next().getValue().getServerId();
			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(serverId);
			if (group == null) {
				group = new RoleOnServer();
				group.serverId = serverId;
				groups.put(group.serverId, group);
			}
			group.accounts.add(account);
		}
		return groups;
	}

	private static RoleOnServer merge(RoleOnServer current, RoleOnServer m) {
		if (current == null)
			return m;
		current.addAll(m.accounts);
		return current;
	}

	private void transmitInProcedure(String account, String clientId, String actionName, Collection<String> accounts,
									 Binary parameter) {
		if (providerApp.zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().isEmpty()) {
			// 没有启用cache-sync，马上触发本地任务。
			processTransmit(account, clientId, actionName, accounts, parameter);
			return;
		}

		var groups = groupByServer(accounts);
		RoleOnServer groupLocal = null;
		for (var it = groups.iterator(); it.moveToNext(); ) {
			var group = it.value();
			if (group.serverId == -1 || group.serverId == providerApp.zeze.getConfig().getServerId()) {
				// loopback 就是当前gs.
				// 对于不在线的角色，直接在本机运行。
				groupLocal = merge(groupLocal, group);
				continue;
			}

			var transmit = new TransmitAccount();
			transmit.Argument.setActionName(actionName);
			transmit.Argument.setSenderAccount(account);
			transmit.Argument.setSenderClientId(clientId);
			transmit.Argument.getTargetAccounts().addAll(group.accounts);
			if (parameter != null)
				transmit.Argument.setParameter(parameter);

			var ps = providerApp.providerDirectService.providerByServerId.get(group.serverId);
			if (ps == null) {
				assert groupLocal != null;
				groupLocal.addAll(group.accounts);
				continue;
			}
			var socket = providerApp.providerDirectService.GetSocket(ps.sessionId);
			if (socket == null) {
				assert groupLocal != null;
				groupLocal.addAll(group.accounts);
				continue;
			}
			transmit.Send(socket);
		}
		assert groupLocal != null;
		if (groupLocal.accounts.size() > 0)
			processTransmit(account, clientId, actionName, groupLocal.accounts, parameter);
	}

	public void transmit(String account, String clientId, String actionName, Collection<String> targets,
						 Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);

		var binaryParam = parameter == null ? Binary.Empty : new Binary(ByteBuffer.encode(parameter));
		// 发送协议请求在另外的事务中执行。
		providerApp.zeze.newProcedure(() -> {
			transmitInProcedure(account, clientId, actionName, targets, binaryParam);
			return Procedure.Success;
		}, "Onlines.Transmit").call();
	}

	public void transmitWhileCommit(String account, String clientId, String actionName, String target,
									Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(account, clientId, actionName, target, parameter));
	}

	public void transmitWhileCommit(String account, String clientId, String actionName, Collection<String> targets,
									Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(account, clientId, actionName, targets, parameter));
	}

	public void transmitWhileRollback(String account, String clientId, String actionName, String target,
									  Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(account, clientId, actionName, target, parameter));
	}

	public void transmitWhileRollback(String account, String clientId, String actionName, Collection<String> targets,
									  Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(account, clientId, actionName, targets, parameter));
	}

	private void broadcast(long typeId, Binary fullEncodedProtocol, int time) {
		var broadcast = new Broadcast(new BBroadcast(typeId, fullEncodedProtocol, time));
		for (var link : providerApp.providerService.getLinks().values()) {
			if (link.getSocket() != null)
				link.getSocket().Send(broadcast);
		}
	}

	public void broadcast(Protocol<?> p, int time) {
		broadcast(p.getTypeId(), new Binary(p.encode()), time);
	}

	private void verifyLocal() {
		var account = new OutObject<String>();
		_tlocal.walkCache((k, v) -> {
			// 先得到roleId
			account.value = k;
			return true;
		}, () -> {
			// 锁外执行事务
			try {
				providerApp.zeze.newProcedure(() -> tryRemoveLocal(account.value), "VerifyLocal:" + account).call();
			} catch (Exception e) {
				logger.error("", e);
			}
		});
		// 随机开始时间，避免验证操作过于集中。3:10 - 5:10
		verifyLocalTimer = Task.scheduleAtUnsafe(3 + Random.getInstance().nextInt(3), 10, this::verifyLocal); // at 3:10 - 6:10
	}

	private long tryRemoveLocal(String account) throws Exception {
		var version = _tversion.getOrAdd(account);
		var local = _tlocal.get(account);
		if (local == null)
			return 0;
		// null == online && null == local -> do nothing
		// null != online && null == local -> do nothing

		var online = _tonline.get(account);
		if (online == null) {
			// remove all
			for (var loginKey : local.getLogins().keySet()) {
				var ret = removeLocalAndTrigger(account, loginKey);
				if (ret != 0)
					return ret;
			}
		} else {
			// 在全局数据中查找login-local，删除不存在或者版本不匹配的。
			for (var loginLocal : local.getLogins().entrySet()) {
				var loginVersion = version.getLogins().get(loginLocal.getKey());
				if (loginVersion == null || loginVersion.getLoginVersion() != loginLocal.getValue().getLoginVersion()) {
					var ret = removeLocalAndTrigger(account, loginLocal.getKey());
					if (ret != 0)
						return ret;
				}
			}
		}
		return 0;
	}

	@RedirectToServer
	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	protected void redirectRemoveLocal(int serverId, String account) throws Exception {
		providerApp.zeze.newProcedure(() -> tryRemoveLocal(account), "redirectNotify").call();
	}

	private void tryRedirectRemoveLocal(int serverId, String account) throws Exception {
		if (providerApp.zeze.getConfig().getServerId() != serverId
				&& providerApp.providerDirectService.providerByServerId.containsKey(serverId))
			redirectRemoveLocal(serverId, account);
	}

	@Override
	protected long ProcessLoginRequest(Zeze.Builtin.Online.Login rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);

		var online = _tonline.getOrAdd(session.getAccount());
		var local = _tlocal.getOrAdd(session.getAccount());
		var version = _tversion.getOrAdd(session.getAccount());

		var loginLocal = local.getLogins().getOrAdd(rpc.Argument.getClientId());
		var loginVersion = version.getLogins().getOrAdd(rpc.Argument.getClientId());

		if (loginVersion.getLoginVersion() != loginVersion.getLogoutVersion()) {
			// login exist
			loginVersion.setLogoutVersion(loginVersion.getLoginVersion());
			var ret = logoutTriggerExtra(session.getAccount(), rpc.Argument.getClientId());
			if (0 != ret)
				return ret;
			if (loginVersion.getLoginVersion() != loginLocal.getLoginVersion()) {
				tryRedirectRemoveLocal(loginVersion.getServerId(), session.getAccount());
			}
		}
		var loginVersionSerialId = version.getLastLoginVersion() + 1;
		version.setLastLoginVersion(loginVersionSerialId);
		loginVersion.setLoginVersion(loginVersionSerialId);
		loginLocal.setLoginVersion(loginVersionSerialId);

		var loginOnline = online.getLogins().getOrAdd(rpc.Argument.getClientId());
		if (!loginOnline.getLinkName().equals(session.getLinkName())
				|| loginOnline.getLinkSid() != session.getLinkSid()) {
			providerApp.providerService.kick(loginOnline.getLinkName(), loginOnline.getLinkSid(),
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
		openQueue(session.getAccount(), rpc.Argument.getClientId()).clear();

		// var linkSession = (ProviderService.LinkSession)session.getLink().getUserState();
		loginVersion.setServerId(providerApp.zeze.getConfig().getServerId());

		var ret = loginTrigger(session.getAccount(), rpc.Argument.getClientId());
		if (0 != ret)
			return ret;

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
	protected long ProcessReLoginRequest(Zeze.Builtin.Online.ReLogin rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);

		var online = _tonline.get(session.getAccount());
		if (online == null)
			return errorCode(ResultCodeOnlineDataNotFound);

		var local = _tlocal.getOrAdd(session.getAccount());
		var version = _tversion.getOrAdd(session.getAccount());

		var loginLocal = local.getLogins().getOrAdd(rpc.Argument.getClientId());
		var loginVersion = version.getLogins().getOrAdd(rpc.Argument.getClientId());

		if (loginVersion.getLoginVersion() != 0) {
			// login exist
			// relogin 不需要补充 Logout？
			// var ret = LogoutTriggerExtra(session.getAccount(), rpc.Argument.getClientId());
			// if (0 != ret)
			//	return ret;
			if (loginVersion.getLoginVersion() != loginLocal.getLoginVersion()) {
				tryRedirectRemoveLocal(loginVersion.getServerId(), session.getAccount());
			}
		}
		var loginVersionSerialId = version.getLastLoginVersion() + 1;
		version.setLastLoginVersion(loginVersionSerialId);
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

		var ret = reloginTrigger(session.getAccount(), rpc.Argument.getClientId());
		if (0 != ret)
			return ret;

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
			return errorCode((short)syncResultCode);

		//App.Load.LoginCount.IncrementAndGet();
		return Procedure.Success;
	}

	@Override
	protected long ProcessLogoutRequest(Zeze.Builtin.Online.Logout rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);

		if (!session.isLogin())
			return errorCode(ResultCodeNotLogin);

		var local = _tlocal.get(session.getAccount());
		var online = _tonline.get(session.getAccount());
		var version = _tversion.getOrAdd(session.getAccount());

		var clientId = session.getContext();
		var loginVersion = version.getLogins().getOrAdd(clientId);
		// 登录在其他机器上。
		if (local == null && online != null) {
			tryRedirectRemoveLocal(loginVersion.getServerId(), session.getAccount()); // nowait
		}
		if (local != null) {
			var ret = removeLocalAndTrigger(session.getAccount(), clientId);
			if (ret != 0)
				return ret;
		}
		if (online != null) {
			loginVersion.setLogoutVersion(loginVersion.getLoginVersion());
			var ret = logoutTrigger(session.getAccount(), clientId);
			if (0 != ret)
				return ret;
		}
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
								   ProviderUserSession session, long index, boolean sync) {
		var online = _tversion.getOrAdd(account);
		var queue = openQueue(account, clientId);
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
			queue.walk((node, bNotify) -> {
				notify.Argument.getNotifies().add(bNotify.getFullEncodedProtocol());
				return true;
			});
			session.sendResponseWhileCommit(notify);
		}
		return ResultCodeSuccess;
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	protected long ProcessReliableNotifyConfirmRequest(Zeze.Builtin.Online.ReliableNotifyConfirm rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);

		var clientId = session.getContext();
		var online = _tonline.get(session.getAccount());
		if (online == null)
			return errorCode(ResultCodeOnlineDataNotFound);

		var syncResultCode = reliableNotifySync(session.getAccount(), clientId,
				session, rpc.Argument.getReliableNotifyConfirmIndex(), rpc.Argument.isSync());
		session.sendResponseWhileCommit(rpc); // 同步前提交。

		if (ResultCodeSuccess != syncResultCode)
			return errorCode((short)syncResultCode);

		return Procedure.Success;
	}
}
