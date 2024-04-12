package Zeze.Arch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import Zeze.AppBase;
import Zeze.Arch.Beans.BSend;
import Zeze.Arch.Gen.GenModule;
import Zeze.Builtin.Online.BAny;
import Zeze.Builtin.Online.BDelayLogoutCustom;
import Zeze.Builtin.Online.BLink;
import Zeze.Builtin.Online.BLocal;
import Zeze.Builtin.Online.BLocals;
import Zeze.Builtin.Online.BNotify;
import Zeze.Builtin.Online.BOnline;
import Zeze.Builtin.Online.BOnlines;
import Zeze.Builtin.Online.BReliableNotify;
import Zeze.Builtin.Online.SReliableNotify;
import Zeze.Builtin.Provider.BBroadcast;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.Broadcast;
import Zeze.Builtin.Provider.CheckLinkSession;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Builtin.ProviderDirect.TransmitAccount;
import Zeze.Collections.BeanFactory;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Hot.HotUpgrade;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.EventDispatcher;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongList;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TransactionLevelAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Online extends AbstractOnline implements HotUpgrade {
	protected static final Logger logger = LogManager.getLogger(Online.class);

	public final ProviderApp providerApp;
	private final AtomicLong loginTimes = new AtomicLong();

	private final EventDispatcher loginEvents;
	private final EventDispatcher reloginEvents;
	private final EventDispatcher logoutEvents;
	private final EventDispatcher localRemoveEvents;

	// 缓存拥有Local数据的HotModule，用来优化。
	private final ConcurrentHashSet<HotModule> hotModulesHaveLocal = new ConcurrentHashSet<>();
	private boolean freshStopModule = false;

	private void onHotModuleStop(HotModule hot) {
		freshStopModule |= hotModulesHaveLocal.remove(hot) != null;
	}

	@Override
	public boolean hasFreshStopModuleLocalOnce() {
		var tmp = freshStopModule;
		freshStopModule = false;
		return tmp;
	}

	static class Retreat {
		final String account;
		final String clientId;
		final String key;
		final Bean bean;

		public Retreat(String account, String clientId, String key, Bean bean) {
			this.account = account;
			this.clientId = clientId;
			this.key = key;
			this.bean = bean;
		}
	}

	@Override
	public void upgrade(Function<Bean, Bean> retreatFunc) {
		// 如果需要，重建_tlocal内存表的用户设置的bean。
		var retreats = new ArrayList<Retreat>();
		_tlocal.walkMemory((account, locals) -> {
			for (var login : locals.getLogins()) {
				var clientId = login.getKey();
				for (var e : login.getValue().getDatas()) {
					var retreatBean = retreatFunc.apply(e.getValue().getAny().getBean());
					if (retreatBean != null) {
						beanFactory.register(retreatBean); // 覆盖beanFactory.
						retreats.add(new Retreat(account, clientId, e.getKey(), retreatBean));
						if (retreats.size() > 50) {
							saveRetreats(retreats);
							retreats.clear();
						}
					}
				}
			}
			return true;
		});
		if (!retreats.isEmpty())
			saveRetreats(retreats);
	}

	private void saveRetreats(ArrayList<Retreat> retreats) {
		// 【注意，这里不使用 Task.call or run，因为这个在热更流程中调用，避免去使用hotGuard。】
		// 确认事务可以在更新流程中可以使用。
		// 也许更优化的方法是为这个更新实现一个不是事务的版本。
		providerApp.zeze.newProcedure(() -> {
			for (var r : retreats)
				setLocalBean(r.account, r.clientId, r.key, r.bean);
			return 0;
		}, "saveRetreats").call();
	}

	public interface TransmitAction {
		/**
		 * @param senderAccount 查询发起者，结果发送给他
		 * @param target        查询目标
		 * @return 按普通事务处理过程返回值处理
		 */
		long call(@NotNull String senderAccount, @NotNull String senderClientId, @NotNull String target, Binary parameter);
	}

	private final ConcurrentHashMap<String, TransmitAction> transmitActions = new ConcurrentHashMap<>();
	private Future<?> verifyLocalTimer;

	public static @NotNull Online create(@NotNull AppBase app) {
		return GenModule.createRedirectModule(Online.class, app);
	}

	public static long getSpecialTypeIdFromBean(@NotNull Serializable bean) {
		return bean.typeId();
	}

	public static final BeanFactory beanFactory = new BeanFactory();

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static void register(@NotNull Class<? extends Serializable> cls) {
		beanFactory.register(cls);
	}

	private volatile long localActiveTimeout = 600 * 1000; // 活跃时间超时。
	private volatile long localCheckPeriod = 600 * 1000; // 检查间隔

	protected Online(@NotNull AppBase app) {
		var zeze = app.getZeze();

		loginEvents = new EventDispatcher(zeze, "Online.Login");
		reloginEvents = new EventDispatcher(zeze, "Online.Relogin");
		logoutEvents = new EventDispatcher(zeze, "Online.Logout");
		localRemoveEvents = new EventDispatcher(zeze, "Online.Local.Remove");

		providerApp = zeze.redirect.providerApp;
		RegisterProtocols(providerApp.providerService);
		RegisterZezeTables(providerApp.zeze);
	}

	public void start() {
		startLocalCheck();
		providerApp.builtinModules.put(this.getFullName(), this);
		var hotManager = providerApp.zeze.getHotManager();
		if (null != hotManager)
			hotManager.addHotUpgrade(this);
	}

	public void setLocalActiveTimeout(long timeout) {
		lock();
		try {
			localActiveTimeout = timeout;
		} finally {
			unlock();
		}
	}

	public void setLocalCheckPeriod(long period) {
		lock();
		try {
			if (period <= 1)
				throw new IllegalArgumentException();
			localCheckPeriod = period;
		} finally {
			unlock();
		}
	}

	private void startLocalCheck() {
		if (verifyLocalTimer != null)
			verifyLocalTimer.cancel(false);
		verifyLocalTimer = Task.scheduleUnsafe(localCheckPeriod, this::verifyLocal);
	}

	public void stop() {
		var hotManager = providerApp.zeze.getHotManager();
		if (null != hotManager)
			hotManager.removeHotUpgrade(this);
		instance = null;
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

	public @Nullable BOnlines getOnline(@NotNull String account) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			return _tonline.get(account);
		return _tonline.selectDirty(account);
	}

	public @NotNull BOnlines getOrAddOnline(@NotNull String account) {
		return _tonline.getOrAdd(account);
	}

	public @Nullable BOnline getLogin(@NotNull String account, @NotNull String clientId) {
		var onlines = getOnline(account);
		if (onlines == null)
			return null;
		var login = onlines.getLogins().get(clientId);
		return login != null && login.getLink().getState() == eLogined ? login : null;
	}

	public int getState(@NotNull String account, @NotNull String clientId) {
		var onlines = getOnline(account);
		if (onlines == null)
			return eOffline;
		var login = onlines.getLogins().get(clientId);
		return login != null ? login.getLink().getState() : eOffline;
	}

	public int getLocalCount() {
		return _tlocal.getCacheSize();
	}

	public long walkLocal(@NotNull TableWalkHandle<String, BLocals> walker) {
		return _tlocal.walkMemory(walker);
	}

	public long getLoginTimes() {
		return loginTimes.get();
	}

	public @NotNull EventDispatcher getLoginEvents() {
		return loginEvents;
	}

	public @NotNull EventDispatcher getReloginEvents() {
		return reloginEvents;
	}

	public @NotNull EventDispatcher getLogoutEvents() {
		return logoutEvents;
	}

	public @NotNull EventDispatcher getLocalRemoveEvents() {
		return localRemoveEvents;
	}

	public @NotNull ConcurrentHashMap<String, TransmitAction> getTransmitActions() {
		return transmitActions;
	}

	private BLocal getLoginLocal(@NotNull String account, @NotNull String clientId) {
		var bLocals = _tlocal.get(account);
		if (bLocals == null)
			throw new IllegalStateException("account not online. " + account);
		var bLoginLocal = bLocals.getLogins().get(clientId);
		if (bLoginLocal == null || bLoginLocal.getLoginVersion()
				!= getOrAddOnline(account).getLogins().getOrAdd(clientId).getLoginVersion())
			throw new IllegalStateException("account.client not online. " + account + "." + clientId);
		return bLoginLocal;
	}

	private final ConcurrentHashMap<String, Long> localActiveTimes = new ConcurrentHashMap<>();

	private void putLocalActiveTime(String account) {
		localActiveTimes.put(account, System.currentTimeMillis());
	}

	private void removeLocalActiveTime(String account) {
		localActiveTimes.remove(account);
	}

	public void setLocalActiveTimeIfPresent(String account) {
		localActiveTimes.computeIfPresent(account, (k, v) -> System.currentTimeMillis());
	}

	public void setLocalBean(@NotNull String account, @NotNull String clientId, @NotNull String key, @NotNull Bean bean) {
		var login = getLoginLocal(account, clientId);
		var bAny = new BAny();
		bAny.getAny().setBean(bean);
		login.getDatas().put(key, bAny);
		if (HotManager.isHotModule(bean.getClass().getClassLoader())) {
			var hotModule = (HotModule)bean.getClass().getClassLoader();
			Transaction.whileCommit(() -> {
				hotModule.stopEvents.add(this::onHotModuleStop);
				hotModulesHaveLocal.add(hotModule);
			});
		}
	}

	public void removeLocalBean(@NotNull String account, @NotNull String clientId, @NotNull String key) {
		var bLocals = _tlocal.get(account);
		if (null == bLocals)
			return;
		var login = bLocals.getLogins().get(clientId);
		if (null == login)
			return;
		login.getDatas().remove(key);
	}

	@SuppressWarnings("unchecked")
	public <T extends Bean> @Nullable T getLocalBean(@NotNull String account, @NotNull String clientId, @NotNull String key) {
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
	public <T extends Bean> T getOrAddLocalBean(@NotNull String account, @NotNull String clientId, String key, @NotNull T defaultHint) {
		var login = getLoginLocal(account, clientId);
		var bAny = login.getDatas().getOrAdd(key);
		if (bAny.getAny().getBean().typeId() == defaultHint.typeId())
			return (T)bAny.getAny().getBean();
		bAny.getAny().setBean(defaultHint);
		if (HotManager.isHotModule(defaultHint.getClass().getClassLoader())) {
			var hotModule = (HotModule)defaultHint.getClass().getClassLoader();
			Transaction.whileCommit(() -> {
				hotModule.stopEvents.add(this::onHotModuleStop);
				hotModulesHaveLocal.add(hotModule);
			});
		}
		return defaultHint;
	}

	private long removeLocalAndTrigger(@NotNull String account, @NotNull String clientId) throws Exception {
		var bLocals = _tlocal.get(account);
		if (bLocals == null)
			return 0;
		var localData = bLocals.getLogins().remove(clientId);
		if (bLocals.getLogins().isEmpty()) {
			_tlocal.remove(account); // remove first
			Transaction.whileCommit(() -> removeLocalActiveTime(account));
		}
		if (localData != null) {
			var arg = new LocalRemoveEventArgument(account, clientId, localData);

			var ret = localRemoveEvents.triggerEmbed(this, arg);
			if (ret != 0)
				return ret;
			localRemoveEvents.triggerProcedure(providerApp.zeze, this, arg);
			Transaction.whileCommit(() -> localRemoveEvents.triggerThread(this, arg));
		}
		return 0;
	}

	private long logoutTrigger(@NotNull String account, @NotNull String clientId) throws Exception {
		var bOnline = getOrAddOnline(account);
		var bLink = bOnline.getLogins().getOrAdd(clientId).getLink();
		bOnline.getLogins().getOrAdd(clientId).setLink(new BLink(bLink.getLinkName(), bLink.getLinkSid(), eOffline));
		var arg = new LogoutEventArgument(account, clientId);

		var loginVersion = bOnline.getLogins().getOrAdd(clientId);

		// 总是尝试通知上一次登录的服务器，里面会忽略本机。
		tryRedirectRemoveLocal(loginVersion.getServerId(), account);

		// 总是删除
		removeLocalAndTrigger(account, clientId);

		var ret = logoutEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		logoutEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> logoutEvents.triggerThread(this, arg));
		return 0;
	}

	private long loginTrigger(@NotNull String account, @NotNull String clientId) throws Exception {
		var arg = new LoginArgument(account, clientId);
		var ret = loginEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		loginEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> loginEvents.triggerThread(this, arg));
		loginTimes.incrementAndGet();
		return 0;
	}

	private long reloginTrigger(@NotNull String account, @NotNull String clientId) throws Exception {
		var arg = new LoginArgument(account, clientId);
		var ret = reloginEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		reloginEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> reloginEvents.triggerThread(this, arg));
		loginTimes.incrementAndGet();
		return 0;
	}

	private long tryLogout(@NotNull BDelayLogoutCustom custom) throws Exception {
		var account = custom.getAccount();
		var clientId = custom.getClientId();
		var currentLoginVersion = custom.getLoginVersion();

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
		var online = getOnline(account);
		var login = online != null ? online.getLogins().get(clientId) : null;
		if (login != null && login.getLink().getState() != eOffline
				&& login.getLoginVersion() == currentLoginVersion) {
			login.setLogoutVersion(login.getLoginVersion());
			var ret = logoutTrigger(account, clientId);
			if (0 != ret)
				return ret;
		}
		return Procedure.Success;
	}

	static Online instance;

	public static class DelayLogout implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			logout((BDelayLogoutCustom)context.customData);
		}

		public static void logout(BDelayLogoutCustom custom) throws Exception {
			if (null != instance) {
				var ret = instance.tryLogout(custom);
				if (ret != 0)
					Online.logger.error("tryLogout fail. {}", ret);
			}
		}

		@Override
		public void onTimerCancel() throws Exception {
		}
	}

	public long sendError(@NotNull String account, @NotNull String clientId,
						  @NotNull String linkName, long linkSid) throws Exception {
		// todo 这个版本的处理没有经过考验，需要参考Game.Online。

		var online = getOrAddOnline(account);
		var loginOnline = online.getLogins().getOrAdd(clientId);
		// skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
		var link = loginOnline.getLink();
		if (!link.getLinkName().equals(linkName) || link.getLinkSid() != linkSid)
			return 0;

		var local = _tlocal.get(account);
		if (local != null) {
			var loginLocal = local.getLogins().get(clientId);
			if (loginLocal != null) {
				loginOnline.setLink(new BLink(link.getLinkName(), link.getLinkSid(), eLinkBroken));
				if (loginOnline.getLoginVersion() != loginLocal.getLoginVersion()) {
					var ret = removeLocalAndTrigger(account, clientId); // 本机数据已经过时，马上删除。
					if (ret != 0)
						logger.info("sendError removeLocalAndTrigger ret{}", ret);
				}
			}
		}

		// shorter use
		DelayLogout.logout(new BDelayLogoutCustom(account, clientId, loginOnline.getLoginVersion()));
		return 0;
	}

	public long linkBroken(@NotNull String account, @NotNull String clientId,
						   @NotNull String linkName, long linkSid) throws Exception {
		var online = getOrAddOnline(account);
		var loginOnline = online.getLogins().getOrAdd(clientId);
		// skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
		var link = loginOnline.getLink();
		if (!link.getLinkName().equals(linkName) || link.getLinkSid() != linkSid)
			return 0;

		var local = _tlocal.get(account);
		if (local == null)
			return 0; // 不在本机登录。
		var loginLocal = local.getLogins().get(clientId);
		if (loginLocal == null)
			return 0; // 不在本机登录。

		loginOnline.setLink(new BLink(link.getLinkName(), link.getLinkSid(), eLinkBroken));

		if (loginOnline.getLoginVersion() != loginLocal.getLoginVersion()) {
			var ret = removeLocalAndTrigger(account, clientId); // 本机数据已经过时，马上删除。
			if (ret != 0)
				return ret;
		}

		// shorter use
		var zeze = providerApp.zeze;
		var delay = zeze.getConfig().getOnlineLogoutDelay();
		zeze.getTimer().schedule(delay, DelayLogout.class, new BDelayLogoutCustom(account, clientId, loginOnline.getLoginVersion()));
		return 0;
	}

	public void addReliableNotifyMark(@NotNull String account, @NotNull String clientId, @NotNull String listenerName) {
		var login = getLogin(account, clientId);
		if (login == null)
			throw new IllegalStateException("Not Online. AddReliableNotifyMark: " + listenerName);
		login.getReliableNotifyMark().add(listenerName);
	}

	public void removeReliableNotifyMark(@NotNull String account, @NotNull String clientId, @NotNull String listenerName) {
		// 移除尽量通过，不做任何判断。
		var online = getOnline(account);
		if (online != null) {
			var login = online.getLogins().get(clientId);
			if (login != null)
				login.getReliableNotifyMark().remove(listenerName);
		}
	}

	public void sendReliableNotifyWhileCommit(@NotNull String account, @NotNull String clientId,
											  @NotNull String listenerName, @NotNull Protocol<?> p) {
		Transaction.whileCommit(() -> sendReliableNotify(account, clientId, listenerName, p));
	}

	public void SendReliableNotifyWhileCommit(@NotNull String account, @NotNull String clientId,
											  @NotNull String listenerName, int typeId,
											  @NotNull Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendReliableNotify(account, clientId, listenerName, typeId, fullEncodedProtocol));
	}

	public void sendReliableNotifyWhileRollback(@NotNull String account, @NotNull String clientId,
												@NotNull String listenerName, @NotNull Protocol<?> p) {
		Transaction.whileRollback(() -> sendReliableNotify(account, clientId, listenerName, p));
	}

	public void sendReliableNotifyWhileRollback(@NotNull String account, @NotNull String clientId,
												@NotNull String listenerName, int typeId,
												@NotNull Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> sendReliableNotify(
				account, clientId, listenerName, typeId, fullEncodedProtocol));
	}

	public void sendReliableNotify(@NotNull String account, @NotNull String clientId,
								   @NotNull String listenerName, @NotNull Protocol<?> p) {
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", account + ',' + clientId + ':' + listenerName, p);
		sendReliableNotify(account, clientId, listenerName, typeId, new Binary(p.encode()));
	}

	private @NotNull Zeze.Collections.Queue<BNotify> openQueue(@NotNull String account, @NotNull String clientId) {
		return providerApp.zeze.getQueueModule().open(
				"Zeze.Arch.Online.ReliableNotifyQueue:" + account + ":" + clientId, BNotify.class);
	}

	/**
	 * 发送在线可靠协议，如果不在线等，仍然不会发送哦。
	 *
	 * @param fullEncodedProtocol 协议必须先编码，因为会跨事务。
	 */
	public void sendReliableNotify(@NotNull String account, @NotNull String clientId, @NotNull String listenerName,
								   long typeId, @NotNull Binary fullEncodedProtocol) {
		providerApp.zeze.runTaskOneByOneByKey(listenerName, "Online.sendReliableNotify." + listenerName, () -> {
			var login = getLogin(account, clientId);
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

			Transaction.whileCommit(() -> {
				if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
					AsyncSocket.log("Send", account + ',' + clientId + ':' + listenerName, notify);
				sendDirect(account, clientId, notify.getTypeId(), new Binary(notify.encode()), false);
			});
//			sendEmbed(List.of(new LoginKey(account, clientId)), notify.getTypeId(), new Binary(notify.encode()));
			return Procedure.Success;
		});
	}

	public @Nullable Long getLogoutVersion(@NotNull String account, @NotNull String clientId) {
		/* 不再检查offline。
		var online = _tonline.get(account);
		if (null == online)
			return null; // is not online
		var login = online.getLogins().get(clientId);
		if (null == login)
			return null; // is not login
		*/
		var online = getOnline(account);
		if (online == null)
			return 0L;
		var login = online.getLogins().get(clientId);
		if (login == null)
			return 0L;
		if (login.getLink().getState() != eOffline)
			return null;
		return login.getLogoutVersion();
	}

	public @Nullable Long getLoginVersion(@NotNull String account, @NotNull String clientId) {
		var login = getLogin(account, clientId);
		return login != null ? login.getLoginVersion() : null;
	}

	public @Nullable Long getLocalLoginVersion(@NotNull String account, @NotNull String clientId) {
		var local = _tlocal.get(account);
		if (null == local)
			return null;
		var login = local.getLogins().get(clientId);
		if (null == login)
			return null;
		return login.getLoginVersion();
	}

	public boolean isLogin(@NotNull String account, @NotNull String clientId) {
		return getLogin(account, clientId) != null;
	}

	public boolean isAccountLogin(@NotNull String account) {
		return getAccountLoginCount(account) > 0;
	}

	public int getAccountLoginCount(@NotNull String account) {
		int sum = 0;
		var online = getOnline(account);
		if (online != null) {
			for (var login : online.getLogins().values()) {
				if (login.getLink().getState() == eLogined)
					sum++;
			}
		}
		return sum;
	}

//	public Collection<LoginOnLink> groupByLink(Iterable<LoginKey> loginKeys) {
//		var groups = new HashMap<String, LoginOnLink>();
//		var groupNotOnline = new LoginOnLink(); // LinkName is Empty And Socket is null.
//		groups.put(groupNotOnline.linkName, groupNotOnline);
//
//		for (var loginKey : loginKeys) {
//			var online = _tonline.get(loginKey.account);
//			if (online == null) {
//				groupNotOnline.logins.putIfAbsent(loginKey, 0L);
//				logger.info("groupByLink: not found account={} in _tonline", loginKey.account);
//				continue;
//			}
//			var login = online.getLogins().get(loginKey.clientId);
//			if (login == null) {
//				logger.info("groupByLink: not found login for clientId={} account={}", loginKey.clientId, loginKey.account);
//				groupNotOnline.logins.putIfAbsent(loginKey, 0L);
//				continue;
//			}
//			var connector = providerApp.providerService.getLinks().get(login.getLinkName());
//			if (connector == null) {
//				logger.warn("groupByLink: not found connector for linkName={} account={}",
//						login.getLinkName(), loginKey.account);
//				groupNotOnline.logins.putIfAbsent(loginKey, 0L);
//				continue;
//			}
//
//			if (!connector.isHandshakeDone()) {
//				logger.warn("groupByLink: not isHandshakeDone for linkName={} account={}",
//						login.getLinkName(), loginKey.account);
//				groupNotOnline.logins.putIfAbsent(loginKey, 0L);
//				continue;
//			}
//			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
//			var group = groups.get(login.getLinkName());
//			if (group == null) {
//				group = new LoginOnLink();
//				group.linkName = login.getLinkName();
//				group.linkSocket = connector.getSocket();
//				groups.putIfAbsent(group.linkName, group);
//			}
//			group.logins.putIfAbsent(loginKey, login.getLinkSid());
//		}
//		return groups.values();
//	}

	private long triggerLinkBroken(@NotNull String linkName, @NotNull LongList errorSids,
								   @NotNull Map<Long, LoginKey> contexts) {
		errorSids.foreach(sid -> providerApp.zeze.newProcedure(() -> {
			var ctx = contexts.get(sid);
			if (ctx != null) {
				return sendError(ctx.account, ctx.clientId, linkName, sid);
			}
			return 0;
		}, "Online.triggerLinkBroken").call());
		return 0;
	}

	public boolean send(@NotNull AsyncSocket to, @NotNull Map<Long, LoginKey> contexts, @NotNull Send send) {
		return send.Send(to, rpc -> triggerLinkBroken(ProviderService.getLinkName(to),
				send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
	}

	public boolean send(@NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
		return send(null, linkName, linkSid, p);
	}

	public boolean send(@Nullable LoginKey loginKey, @NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
		var connector = providerApp.providerService.getLinks().get(linkName);
		if (null == connector) {
			logger.warn("link connector not found. name={}", linkName);
			return false;
		}
		var link = connector.getSocket();
		if (null == link) {
			logger.warn("link socket not found. name={}", linkName);
			return false;
		}
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(p.getTypeId()))
			AsyncSocket.log("Send", loginKey != null ? loginKey.account + ',' + loginKey.clientId : linkName, p);
		var send = new Send(new BSend(p.getTypeId(), new Binary(p.encode())));
		send.Argument.getLinkSids().add(linkSid);
		return send(link, loginKey != null ? Map.of(linkSid, loginKey) : Map.of(), send);
	}

//	public boolean send(Collection<LoginKey> keys, AsyncSocket to, Map<Long, LoginKey> contexts, Send send) {
//		if (keys.size() > 1) {
//			return send.Send(to, rpc -> triggerLinkBroken(ProviderService.getLinkName(to),
//					send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
//		}
//		//noinspection CodeBlock2Expr
//		providerApp.zeze.getTaskOneByOneByKey().executeCyclicBarrier(keys, "sendOneByOne", () -> {
//			send.Send(to, rpc -> triggerLinkBroken(ProviderService.getLinkName(to),
//					send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
//		}, null, DispatchMode.Normal);
//		return true;
//	}

//	private void sendEmbed(Collection<LoginKey> logins, long typeId, Binary fullEncodedProtocol) {
//		var groups = groupByLink(logins);
//		Transaction.whileCommit(() -> {
//			for (var group : groups) {
//				if (group.linkSocket == null)
//					continue; // skip not online
//
//				var send = new Send(new Zeze.Arch.Beans.BSend(typeId, fullEncodedProtocol));
//				send.Argument.getLinkSids().addAll(group.logins.values());
//				send(group.linkSocket, group.contexts, send);
//			}
//		});
//	}

	public static final class LinkRoles {
		final @NotNull String linkName;
		final AsyncSocket linkSocket;
		final @NotNull Send send;
		final ArrayList<LoginKey> accounts = new ArrayList<>();

		public LinkRoles(@NotNull String linkName, AsyncSocket linkSocket,
						 long typeId, @NotNull Binary fullEncodedProtocol) {
			this.linkName = linkName;
			this.linkSocket = linkSocket;
			send = new Send(new BSend(typeId, fullEncodedProtocol));
		}
	}

	private static AsyncSocket getLinkSocket(ConcurrentHashMap<String, Connector> links, String linkName,
											 String account, String clientId) {
		var connector = links.get(linkName);
		if (connector == null) {
			logger.warn("sendDirect: not found connector for linkName={} clientId={} account={}",
					linkName, account, clientId);
			return null;
		}
		if (!connector.isHandshakeDone()) {
			logger.warn("sendDirect: not isHandshakeDone for linkName={} clientId={} account={}",
					linkName, account, clientId);
			return null;
		}
		var linkSocket = connector.getSocket();
		if (linkSocket == null) {
			logger.warn("sendDirect: closed connector for linkName={} clientId={} account={}",
					linkName, account, clientId);
			return null;
		}
		return linkSocket;
	}

	private void processErrorSids(LongList errorSids, LinkRoles group) {
		errorSids.foreach(linkSid -> Task.run(providerApp.zeze.newProcedure(() -> {
			int idx = group.send.Argument.getLinkSids().indexOf(linkSid);
			var loginKey = group.accounts.get(idx);
			return idx >= 0 ? sendError(loginKey.account, loginKey.clientId, group.linkName, linkSid) : 0;
		}, "Online.triggerLinkBroken2")));
	}

	// 可在事务外执行
	public int sendDirect(@NotNull Set<LoginKey> loginKeys, long typeId, @NotNull Binary fullEncodedProtocol,
						  boolean trySend) {
		if (loginKeys.isEmpty())
			return 0;
		var groups = new HashMap<String, LinkRoles>();
		var links = providerApp.providerService.getLinks();
		for (var loginKey : loginKeys) {
			var account = loginKey.account;
			var clientId = loginKey.clientId;
			var online = _tonline.selectDirty(account);
			if (online == null) {
				if (!trySend)
					logger.info("sendDirect: not found account={} in _tonline", account);
				continue;
			}
			var login = online.getLogins().get(clientId);
			if (login == null) {
				if (!trySend)
					logger.info("sendDirect: not found login for clientId={} account={}", clientId, account);
				continue;
			}
			var link = login.getLink();
			var state = link.getState();
			if (state != eLogined) {
				if (!trySend)
					logger.info("sendDirect: state={} != eLogined for clientId={} account={}", state, clientId, account);
				continue;
			}
			var linkName = link.getLinkName();
			// 后面保存connector.socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(linkName);
			if (group == null) {
				var linkSocket = getLinkSocket(links, linkName, account, clientId); // maybe null
				groups.put(linkName, group = new LinkRoles(linkName, linkSocket, typeId, fullEncodedProtocol));
			}
			group.send.Argument.getLinkSids().add(link.getLinkSid());
			group.accounts.add(loginKey);
		}
		int sendCount = 0;
		for (var group : groups.values()) {
			if (null == group.linkSocket) {
				processErrorSids(group.send.Argument.getLinkSids(), group);
				continue; // link miss process done
			}

			if (group.send.Send(group.linkSocket, rpc -> {
				var send = group.send;
				var errorSids = send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids();
				processErrorSids(errorSids, group);
				return Procedure.Success;
			}))
				sendCount++;
		}
		return sendCount;
	}

	// 可在事务外执行
	public boolean sendDirect(@NotNull String account, @NotNull String clientId, long typeId,
							  @NotNull Binary fullEncodedProtocol, boolean trySend) {
		var online = _tonline.selectDirty(account);
		if (online == null) {
			if (!trySend)
				logger.info("sendDirect: not found account={} in _tonline", account);
			return false;
		}
		var login = online.getLogins().get(clientId);
		if (login == null) {
			if (!trySend)
				logger.info("sendDirect: not found login for clientId={} account={}", clientId, account);
			return false;
		}

		var link = login.getLink();
		var state = link.getState();
		if (state != eLogined) {
			if (!trySend)
				logger.info("sendDirect: state={} != eLogined for clientId={} account={}", state, clientId, account);
			return false;
		}
		var linkName = link.getLinkName();
		var connector = providerApp.providerService.getLinks().get(linkName);
		if (connector == null) {
			logger.warn("sendDirect: not found connector for linkName={} account={} clientId={}",
					linkName, account, clientId);
			Task.run(providerApp.zeze.newProcedure(() -> sendError(account, clientId, linkName, link.getLinkSid()),
					"Online.triggerLinkBroken1"));
			return false;
		}
		if (!connector.isHandshakeDone()) {
			logger.warn("sendDirect: not isHandshakeDone for linkName={} account={} clientId={}",
					linkName, account, clientId);
			Task.run(providerApp.zeze.newProcedure(() -> sendError(account, clientId, linkName, link.getLinkSid()),
					"Online.triggerLinkBroken1"));
			return false;
		}
		// 后面保存connector.socket并使用，如果之后连接被关闭，以后发送协议失败。
		var linkSocket = connector.getSocket();
		if (linkSocket == null) {
			logger.warn("sendDirect: closed connector for linkName={} account={} clientId={}",
					linkName, account, clientId);
			Task.run(providerApp.zeze.newProcedure(() -> sendError(account, clientId, linkName, link.getLinkSid()),
					"Online.triggerLinkBroken1"));
			return false;
		}
		var send = new Send(new BSend(typeId, fullEncodedProtocol));
		send.Argument.getLinkSids().add(link.getLinkSid());
		return send.Send(linkSocket, rpc -> {
			if (send.isTimeout() || !send.Result.getErrorLinkSids().isEmpty()) {
				var linkSid = send.Argument.getLinkSids().get(0);
				providerApp.zeze.newProcedure(() -> sendError(account, clientId, linkName, linkSid),
						"Online.triggerLinkBroken1").call();
			}
			return Procedure.Success;
		});
	}

	public static class LoginOnLink {
		public String linkName = ""; // empty when not online
		public AsyncSocket linkSocket; // null if not online
		public long providerSessionId;
		public final HashMap<LoginKey, Long> logins = new HashMap<>();
		public final HashMap<Long, LoginKey> contexts = new HashMap<>();
	}

	public static class LoginKey {
		public final @NotNull String account;
		public final @NotNull String clientId;

		public LoginKey(@NotNull String account, @NotNull String clientId) {
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

//	public void send(String account, String clientId, long typeId, Binary fullEncodedProtocol) {
//		var login = new LoginKey(account, clientId);
//		providerApp.zeze.runTaskOneByOneByKey(login, "Online.send", () -> {
//			sendEmbed(List.of(login), typeId, fullEncodedProtocol);
//			return Procedure.Success;
//		});
//	}

	public int send(@NotNull Collection<LoginKey> loginKeys, long typeId, @NotNull Binary fullEncodedProtocol,
					boolean trySend) {
		int loginCount = loginKeys.size();
		if (loginCount == 1) {
			var it = loginKeys.iterator();
			if (it.hasNext()) { // 不确定loginKeys是否稳定,所以还是判断一下保险
				var loginKey = it.next();
				return sendDirect(loginKey.account, loginKey.clientId, typeId, fullEncodedProtocol, trySend) ? 1 : 0;
			}
		} else if (loginCount > 1) {
			return sendDirect(loginKeys instanceof Set ? (Set<LoginKey>)loginKeys : new HashSet<>(loginKeys),
					typeId, fullEncodedProtocol, trySend);
//			var p = providerApp.zeze.newProcedure(() -> {
//				sendEmbed(loginKeys, typeId, fullEncodedProtocol);
//				return Procedure.Success;
//			}, "Online.send");
//			if (loginKeys.size() > 1)
//				Task.executeUnsafe(p);
//			else
//				providerApp.zeze.getTaskOneByOneByKey().executeCyclicBarrier(loginKeys, p, null, DispatchMode.Normal);
		}
		return 0;
	}

	public void send(@NotNull String account, @NotNull String clientId, @NotNull Protocol<?> p) {
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", account + ',' + clientId, p);
		sendDirect(account, clientId, typeId, new Binary(p.encode()), false);
	}

	public void sendResponse(@NotNull String account, @NotNull String clientId, @NotNull Rpc<?, ?> r) {
		r.setRequest(false);
		send(account, clientId, r);
	}

	public void send(@NotNull Collection<LoginKey> logins, @NotNull Protocol<?> p) {
		if (logins.size() <= 0)
			return;
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var sb = new StringBuilder();
			for (var login : logins)
				sb.append(login.account).append(',').append(login.clientId).append(';');
			int n = sb.length();
			if (n > 0)
				sb.setLength(n - 1);
			var idsStr = sb.toString();
			AsyncSocket.log("Send", idsStr, p);
		}
		send(logins, typeId, new Binary(p.encode()), false);
	}

	public void sendWhileCommit(@NotNull String account, @NotNull String clientId, @NotNull Protocol<?> p) {
		Transaction.whileCommit(() -> send(account, clientId, p));
	}

	public void sendWhileCommit(@NotNull Collection<LoginKey> logins, @NotNull Protocol<?> p) {
		Transaction.whileCommit(() -> send(logins, p));
	}

	public void sendResponseWhileCommit(@NotNull String account, @NotNull String clientId, @NotNull Rpc<?, ?> r) {
		Transaction.whileCommit(() -> {
			r.setRequest(false);
			send(account, clientId, r);
		});
	}

	public void sendWhileRollback(@NotNull String account, @NotNull String clientId, @NotNull Protocol<?> p) {
		Transaction.whileRollback(() -> send(account, clientId, p));
	}

	public void sendWhileRollback(@NotNull Collection<LoginKey> logins, @NotNull Protocol<?> p) {
		Transaction.whileRollback(() -> send(logins, p));
	}

//	public Collection<LoginOnLink> groupAccountsByLink(Collection<String> accounts) {
//		var groups = new HashMap<String, LoginOnLink>();
//		var groupNotOnline = new LoginOnLink(); // LinkName is Empty And Socket is null.
//		groups.put(groupNotOnline.linkName, groupNotOnline);
//
//		for (var account : accounts) {
//			var online = _tonline.get(account);
//			if (online == null) {
//				groupNotOnline.logins.putIfAbsent(new LoginKey(account, ""), 0L);
//				continue;
//			}
//			for (var e : online.getLogins().entrySet()) {
//				var login = new LoginKey(account, e.getKey());
//				var connector = providerApp.providerService.getLinks().get(e.getValue().getLinkName());
//				if (connector == null) {
//					groupNotOnline.logins.putIfAbsent(login, 0L);
//					continue;
//				}
//				if (!connector.isHandshakeDone()) {
//					groupNotOnline.logins.putIfAbsent(login, 0L);
//					continue;
//				}
//				// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
//				var group = groups.get(e.getValue().getLinkName());
//				if (group == null) {
//					group = new LoginOnLink();
//					group.linkName = e.getValue().getLinkName();
//					group.linkSocket = connector.getSocket();
//					groups.putIfAbsent(group.linkName, group);
//				}
//				group.logins.putIfAbsent(login, e.getValue().getLinkSid());
//				group.contexts.putIfAbsent(e.getValue().getLinkSid(), login);
//			}
//		}
//		return groups.values();
//	}

//	public void sendAccountsEmbed(Collection<String> accounts, long typeId, Binary fullEncodedProtocol,
//								  OnlineSend sender) {
//		var groups = groupAccountsByLink(accounts);
//		Transaction.whileCommit(() -> {
//			if (sender == null) {
//				for (var group : groups) {
//					if (group.linkSocket == null)
//						continue; // skip not online
//					var send = new Send(new Zeze.Arch.Beans.BSend(typeId, fullEncodedProtocol));
//					send.Argument.getLinkSids().addAll(group.logins.values());
//					send(group.linkSocket, group.contexts, send);
//				}
//			} else {
//				for (var group : groups) {
//					if (!sender.send(group))
//						break;
//				}
//			}
//		});
//	}

	// 可在事务外执行
	public int sendAccountDirect(@NotNull String account, long typeId, @NotNull Binary fullEncodedProtocol,
								 boolean trySend) {
		var groups = new HashMap<String, LinkRoles>();
		var links = providerApp.providerService.getLinks();
		var online = _tonline.selectDirty(account);
		if (online == null) {
			if (!trySend)
				logger.info("sendAccountDirect: not found account={} in _tonline", account);
			return 0;
		}
		for (var e : online.getLogins()) {
			var login = e.getValue();
			var link = login.getLink();
			if (link.getState() != eLogined)
				continue;
			var linkName = link.getLinkName();
			// 后面保存connector.socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(linkName);
			if (group == null) {
				var linkSocket = getLinkSocket(links, linkName, account, e.getKey()); // maybe null
				groups.put(linkName, group = new LinkRoles(linkName, linkSocket, typeId, fullEncodedProtocol));
			}
			group.send.Argument.getLinkSids().add(link.getLinkSid());
			group.accounts.add(new LoginKey(account, e.getKey()));
		}
		int sendCount = 0;
		for (var group : groups.values()) {
			if (null == group.linkSocket) {
				processErrorSids(group.send.Argument.getLinkSids(), group);
				continue; // link miss process done.
			}
			if (group.send.Send(group.linkSocket, rpc -> {
				var send = group.send;
				var errorSids = send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids();
				processErrorSids(errorSids, group);
				return Procedure.Success;
			}))
				sendCount++;
		}
		return sendCount;
	}

	// 可在事务外执行
	public int sendAccountsDirect(@NotNull Collection<String> accounts, long typeId, @NotNull Binary fullEncodedProtocol,
								  boolean trySend) {
		if (accounts.isEmpty())
			return 0;
		var groups = new HashMap<String, LinkRoles>();
		var links = providerApp.providerService.getLinks();
		for (var account : accounts) {
			var online = _tonline.selectDirty(account);
			if (online == null) {
				if (!trySend)
					logger.info("sendAccountsDirect: not found account={} in _tonline", account);
				continue;
			}
			for (var e : online.getLogins()) {
				var login = e.getValue();

				var link = login.getLink();
				if (link.getState() != eLogined)
					continue;
				var linkName = link.getLinkName();
				// 后面保存connector.socket并使用，如果之后连接被关闭，以后发送协议失败。
				var group = groups.get(linkName);
				if (group == null) {
					var linkSocket = getLinkSocket(links, linkName, account, e.getKey()); // maybe null
					groups.put(linkName, group = new LinkRoles(linkName, linkSocket, typeId, fullEncodedProtocol));
				}
				group.send.Argument.getLinkSids().add(link.getLinkSid());
				group.accounts.add(new LoginKey(account, e.getKey()));
			}
		}
		int sendCount = 0;
		for (var group : groups.values()) {
			if (null == group.linkSocket) {
				processErrorSids(group.send.Argument.getLinkSids(), group);
				continue; // link miss process done.
			}

			if (group.send.Send(group.linkSocket, rpc -> {
				var send = group.send;
				var errorSids = send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids();
				processErrorSids(errorSids, group);
				return Procedure.Success;
			}))
				sendCount++;
		}
		return sendCount;
	}

	public void sendAccount(@NotNull String account, long typeId, @NotNull Binary fullEncodedProtocol) { // OnlineSend sender
		sendAccountDirect(account, typeId, fullEncodedProtocol, false);
//		providerApp.zeze.runTaskOneByOneByKey(account, "Online.sendAccount", () -> {
//			sendAccountsEmbed(List.of(account), typeId, fullEncodedProtocol, sender);
//			return Procedure.Success;
//		});
	}

	public int sendAccounts(@NotNull Collection<String> accounts, long typeId, @NotNull Binary fullEncodedProtocol) { // OnlineSend sender
		int accountCount = accounts.size();
		if (accountCount == 1) {
			var it = accounts.iterator();
			if (it.hasNext()) // 不确定accounts是否稳定,所以还是判断一下保险
				return sendAccountDirect(it.next(), typeId, fullEncodedProtocol, false);
		} else if (accountCount > 1) {
			return sendAccountsDirect(accounts instanceof Set ? accounts : new HashSet<>(accounts),
					typeId, fullEncodedProtocol, false);
//			var p = providerApp.zeze.newProcedure(() -> {
//				sendAccountsEmbed(accounts, typeId, fullEncodedProtocol, sender);
//				return Procedure.Success;
//			}, "Online.sendAccounts");
//			if (accounts.size() > 1)
//				Task.executeUnsafe(p);
//			else
//				providerApp.zeze.getTaskOneByOneByKey().executeCyclicBarrier(accounts, p, null, DispatchMode.Normal);
		}
		return 0;
	}

	/**
	 * 给账号所有的登录终端发送消息。
	 */
	public void sendAccount(@NotNull String account, @NotNull Protocol<?> p/*, OnlineSend sender*/) {
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", account, p);
		sendAccountDirect(account, typeId, new Binary(p.encode()), false);
	}

	/**
	 * 给账号所有的登录终端发送消息。
	 */
	public void sendAccounts(@NotNull Collection<String> accounts, @NotNull Protocol<?> p/*, OnlineSend sender*/) {
		if (accounts.size() <= 0)
			return;
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var sb = new StringBuilder();
			for (var account : accounts)
				sb.append(account).append(',');
			int n = sb.length();
			if (n > 0)
				sb.setLength(n - 1);
			var idsStr = sb.toString();
			AsyncSocket.log("Send", idsStr, p);
		}
		sendAccounts(accounts, typeId, new Binary(p.encode())/*, sender*/);
	}

	public void sendAccountWhileCommit(@NotNull String account, @NotNull Protocol<?> p/*, OnlineSend sender*/) {
		Transaction.whileCommit(() -> sendAccount(account, p/*, sender*/));
	}

	public void sendAccountsWhileCommit(@NotNull Collection<String> accounts, @NotNull Protocol<?> p/*, OnlineSend sender*/) {
		Transaction.whileCommit(() -> sendAccounts(accounts, p/*, sender*/));
	}

	public void sendAccountWhileRollback(@NotNull String account, @NotNull Protocol<?> p/*, OnlineSend sender*/) {
		Transaction.whileRollback(() -> sendAccount(account, p/*, sender*/));
	}

	public void sendAccountsWhileRollback(@NotNull Collection<String> accounts, @NotNull Protocol<?> p/*, OnlineSend sender*/) {
		Transaction.whileRollback(() -> sendAccounts(accounts, p/*, sender*/));
	}

	/**
	 * 转发查询请求给RoleId。
	 *
	 * @param account    查询发起者，结果发送给他。
	 * @param actionName 查询处理的实现
	 * @param target     目标角色
	 */
	public void transmit(@NotNull String account, @NotNull String clientId, @NotNull String actionName,
						 @NotNull String target, @Nullable Serializable parameter) {
		transmit(account, clientId, actionName, List.of(target), parameter);
	}

	public void processTransmit(@NotNull String account, @NotNull String clientId, @NotNull String actionName,
								@NotNull Collection<String> accounts, @Nullable Binary parameter) {
		var handle = transmitActions.get(actionName);
		if (handle != null) {
			for (var target : accounts) {
				providerApp.zeze.newProcedure(() -> handle.call(account, clientId, target, parameter),
						"Online.processTransmit:" + actionName).call();
			}
		}
	}

	public static class RoleOnServer {
		public int serverId = -1; // empty when not online
		public final HashSet<String> accounts = new HashSet<>();

		public void addAll(@NotNull HashSet<String> accounts) {
			this.accounts.addAll(accounts);
		}
	}

	public @NotNull IntHashMap<RoleOnServer> groupByServer(@NotNull Collection<String> accounts) {
		var groups = new IntHashMap<RoleOnServer>();
		var groupNotOnline = new RoleOnServer(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.serverId, groupNotOnline);

		for (var account : accounts) {
			var online = getOnline(account);
			if (online == null || online.getLogins().isEmpty()) {
				// null != online 意味着这里肯定不为0，不会到达这个分支。
				// 下面要求Logins.Count必须大于0，判断一下吧。
				groupNotOnline.accounts.add(account);
				continue;
			}
			// 这里随便找第一个，什么逻辑？
			var serverId = online.getLogins().iterator().next().getValue().getServerId();
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

	private static @NotNull RoleOnServer merge(@Nullable RoleOnServer current, @NotNull RoleOnServer m) {
		if (current == null)
			return m;
		current.addAll(m.accounts);
		return current;
	}

	private void transmitInProcedure(@NotNull String account, @NotNull String clientId, @NotNull String actionName,
									 @NotNull Collection<String> accounts, @Nullable Binary parameter) {
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
		if (!groupLocal.accounts.isEmpty())
			processTransmit(account, clientId, actionName, groupLocal.accounts, parameter);
	}

	public void transmit(@NotNull String account, @NotNull String clientId, @NotNull String actionName,
						 @NotNull Collection<String> targets, @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);

		var binaryParam = parameter == null ? Binary.Empty : new Binary(ByteBuffer.encode(parameter));
		// 发送协议请求在另外的事务中执行。
		Task.run(providerApp.zeze.newProcedure(() -> {
			transmitInProcedure(account, clientId, actionName, targets, binaryParam);
			return Procedure.Success;
		}, "Online.transmit"));
	}

	public void transmitWhileCommit(@NotNull String account, @NotNull String clientId, @NotNull String actionName,
									@NotNull String target, @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(account, clientId, actionName, target, parameter));
	}

	public void transmitWhileCommit(@NotNull String account, @NotNull String clientId, @NotNull String actionName,
									@NotNull Collection<String> targets, @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(account, clientId, actionName, targets, parameter));
	}

	public void transmitWhileRollback(@NotNull String account, @NotNull String clientId, @NotNull String actionName,
									  @NotNull String target, @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileRollback(() -> transmit(account, clientId, actionName, target, parameter));
	}

	public void transmitWhileRollback(@NotNull String account, @NotNull String clientId, @NotNull String actionName,
									  @NotNull Collection<String> targets, @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileRollback(() -> transmit(account, clientId, actionName, targets, parameter));
	}

	private int broadcast(long typeId, @NotNull Binary fullEncodedProtocol, int time) {
		var broadcast = new Broadcast(new BBroadcast.Data(typeId, fullEncodedProtocol, time));
		var pdata = broadcast.encode();
		int sendCount = 0;
		for (var link : providerApp.providerService.getLinks().values()) {
			if (link.getSocket() != null && link.getSocket().Send(pdata))
				sendCount++;
		}
		return sendCount;
	}

	public int broadcast(@NotNull Protocol<?> p, int time) {
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Broc", providerApp.providerService.getLinks().size(), p);
		return broadcast(typeId, new Binary(p.encode()), time);
	}

	class VerifyBatch {
		final ArrayList<String> accounts = new ArrayList<>();

		public boolean add(String account) {
			var aTime = localActiveTimes.get(account);
			if (null != aTime && System.currentTimeMillis() - aTime > localActiveTimeout)
				accounts.add(account);
			return true;
		}

		public void tryPerform() {
			if (accounts.size() > 20) {
				perform();
			}
		}

		public void perform() {
			if (!accounts.isEmpty()) {
				try {
					providerApp.zeze.newProcedure(() -> {
						for (var account : accounts)
							tryRemoveLocal(account);
						return 0;
					}, "Online.verifyLocal:" + accounts).call();
					sendAccountsDirect(accounts, CheckLinkSession.TypeId_, new Binary(new CheckLinkSession().encode()), true);
				} catch (Exception e) {
					logger.error("", e);
				}
				accounts.clear();
			}
		}
	}

	private void verifyLocal() {
		var batch = new VerifyBatch();
		// 锁外执行事务
		_tlocal.walkMemory((k, v) -> {
			batch.add(k);
			batch.tryPerform();
			return true;
		});
		batch.perform();
		startLocalCheck();
	}

	private long tryRemoveLocal(@NotNull String account) throws Exception {
		var local = _tlocal.get(account);
		if (local == null)
			return 0;
		var online = getOnline(account);
		// 在全局数据中查找login-local，删除不存在或者版本不匹配的。
		for (var e : local.getLogins().entrySet()) {
			var clientId = e.getKey();
			var login = online != null ? online.getLogins().get(clientId) : null;
			if (login == null || login.getLink().getState() == eOffline
					|| login.getLoginVersion() != e.getValue().getLoginVersion()) {
				var ret = removeLocalAndTrigger(account, clientId);
				if (ret != 0)
					return ret;
			}
		}
		return 0;
	}

	@RedirectToServer
	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	protected void redirectRemoveLocal(int serverId, @NotNull String account) {
		providerApp.zeze.newProcedure(() -> tryRemoveLocal(account), "Online.redirectRemoveLocal").call();
	}

	private void tryRedirectRemoveLocal(int serverId, @NotNull String account) throws Exception {
		if (providerApp.zeze.getConfig().getServerId() != serverId
				&& providerApp.providerDirectService.providerByServerId.containsKey(serverId))
			redirectRemoveLocal(serverId, account);
	}

	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	@Override
	protected long ProcessLoginRequest(Zeze.Builtin.Online.Login rpc) throws Exception {
		var done = new OutObject<>(false);
		while (!done.value) {
			var r = Task.call(providerApp.zeze.newProcedure(() -> ProcessLoginRequest(rpc, done), "ProcessLoginRequest"));
			if (r != 0)
				return r;
		}
		return 0;
	}

	private long ProcessLoginRequest(Zeze.Builtin.Online.Login rpc, @NotNull OutObject<Boolean> done) throws Exception {
		done.value = true; // 默认设置成处理完成，包括错误的时候。下面分支需要的时候重新设置成false。

		var session = ProviderUserSession.get(rpc);
		var account = session.getAccount();

		var online = getOrAddOnline(account);
		var local = _tlocal.getOrAdd(account);
		var loginLocal = local.getLogins().getOrAdd(rpc.Argument.getClientId());
		Transaction.whileCommit(() -> putLocalActiveTime(account));
		var loginOnline = online.getLogins().getOrAdd(rpc.Argument.getClientId());

		var onlineAccount = online.getAccount();
		var isBound = !onlineAccount.isEmpty();
		if (!isBound)
			online.setAccount(account); // 这里依赖loginTrigger做进一步的角色是否属于账号的验证,验证失败会回滚
		else if (!onlineAccount.equals(account))
			return Procedure.LogicError;

		if (isBound && loginOnline.getLoginVersion() != loginOnline.getLogoutVersion()) {
			// login exist
			loginOnline.setLogoutVersion(loginOnline.getLoginVersion());

			var link = loginOnline.getLink();
			if (!link.getLinkName().equals(session.getLinkName()) || link.getLinkSid() != session.getLinkSid()) {
				providerApp.providerService.kick(link.getLinkName(), link.getLinkSid(),
						BKick.ErrorDuplicateLogin,
						"duplicate login " + account + ":" + rpc.Argument.getClientId());
			}
			var ret = logoutTrigger(account, rpc.Argument.getClientId());
			if (0 != ret)
				return ret;
			// 发生了补Logout事件，重做Login。
			done.value = false;
			return 0; // Logout事件补了以后这个局部事务是成功完成的。
		}

		// 开始登录流程，先准备 link-state。
		var loginVersionSerialId = online.getLastLoginVersion() + 1;
		online.setLastLoginVersion(loginVersionSerialId);
		loginOnline.setLoginVersion(loginVersionSerialId);
		loginLocal.setLoginVersion(loginVersionSerialId);

		loginOnline.setLink(new BLink(session.getLinkName(), session.getLinkSid(), eLogined));

		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			//setUserState.Argument.getUserState().setLoginVersion(loginVersionSerialId);
			setUserState.Argument.getUserState().setContext(rpc.Argument.getClientId());
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		loginOnline.setReliableNotifyConfirmIndex(0);
		loginOnline.setReliableNotifyIndex(0);
		loginOnline.getReliableNotifyMark().clear();
		openQueue(account, rpc.Argument.getClientId()).clear();

		// var linkSession = (ProviderService.LinkSession)session.getLink().getUserState();
		loginOnline.setServerId(providerApp.zeze.getConfig().getServerId());

		session.sendResponseWhileCommit(rpc);
		return loginTrigger(account, rpc.Argument.getClientId());
	}

	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	@Override
	protected long ProcessReLoginRequest(Zeze.Builtin.Online.ReLogin rpc) throws Exception {
		var done = new OutObject<>(false);
		while (!done.value) {
			var r = Task.call(providerApp.zeze.newProcedure(() -> ProcessReLoginRequest(rpc, done), "ProcessReLoginRequest"));
			if (r != 0)
				return r;
		}
		return 0;
	}

	private long ProcessReLoginRequest(Zeze.Builtin.Online.ReLogin rpc, @NotNull OutObject<Boolean> done) throws Exception {
		done.value = true; // 默认设置成处理完成，包括错误的时候。下面分支需要的时候重新设置成false。

		var session = ProviderUserSession.get(rpc);
		var account = session.getAccount();

		var online = getOrAddOnline(account);
		var local = _tlocal.getOrAdd(account);
		var loginLocal = local.getLogins().getOrAdd(rpc.Argument.getClientId());
		Transaction.whileCommit(() -> putLocalActiveTime(account));
		var loginOnline = online.getLogins().getOrAdd(rpc.Argument.getClientId());

		var onlineAccount = online.getAccount();
		var isBound = !onlineAccount.isEmpty();
		if (!isBound)
			online.setAccount(account); // 这里依赖reloginTrigger做进一步的角色是否属于账号的验证,验证失败会回滚
		else if (!onlineAccount.equals(account))
			return Procedure.LogicError;

		if (isBound && loginOnline.getLoginVersion() != loginOnline.getLogoutVersion()) {
			// login exist
			loginOnline.setLogoutVersion(loginOnline.getLoginVersion());
			var link = loginOnline.getLink();
			if (!link.getLinkName().equals(session.getLinkName()) || link.getLinkSid() != session.getLinkSid()) {
				providerApp.providerService.kick(link.getLinkName(), link.getLinkSid(),
						BKick.ErrorDuplicateLogin,
						"duplicate login " + account + ":" + rpc.Argument.getClientId());
			}
			var ret = logoutTrigger(account, rpc.Argument.getClientId());
			if (0 != ret)
				return ret;
			// 发生了补Logout事件，重做ReLogin。
			done.value = false;
			return 0; // Logout事件补了以后这个局部事务是成功完成的。
		}

		// 开始登录流程，先准备 link-state。
		var loginVersionSerialId = online.getLastLoginVersion() + 1;
		online.setLastLoginVersion(loginVersionSerialId);
		loginOnline.setLoginVersion(loginVersionSerialId);
		loginLocal.setLoginVersion(loginVersionSerialId);

		loginOnline.setLink(new BLink(session.getLinkName(), session.getLinkSid(), eLogined));

		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			//setUserState.Argument.getUserState().setLoginVersion(loginVersionSerialId);
			setUserState.Argument.getUserState().setContext(rpc.Argument.getClientId());
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		loginOnline.setServerId(providerApp.zeze.getConfig().getServerId());

		/////////////////////////////////////////////////////////////
		// 先发结果，再发送同步数据（ReliableNotifySync）。
		// 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
		session.sendResponseWhileCommit(rpc);
		var ret = reloginTrigger(account, rpc.Argument.getClientId());
		if (0 != ret)
			return ret;

		var syncResultCode = reliableNotifySync(account, rpc.Argument.getClientId(),
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

		//var local = _tlocal.get(session.getAccount());
		var account = session.getAccount();
		var clientId = session.getContext();
		var login = getLogin(account, clientId);
		if (login != null) {
			login.setLogoutVersion(login.getLoginVersion());
			var ret = logoutTrigger(account, clientId);
			if (0 != ret)
				return ret;
			// online 被删除
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

	private int reliableNotifySync(@NotNull String account, @NotNull String clientId,
								   @NotNull ProviderUserSession session, long index, boolean sync) {
		var online = getOrAddOnline(account);
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
		var online = getOnline(session.getAccount());
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
