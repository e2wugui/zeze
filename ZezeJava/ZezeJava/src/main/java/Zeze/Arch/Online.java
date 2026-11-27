package Zeze.Arch;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.AppBase;
import Zeze.Application;
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
import Zeze.Builtin.Online.Login;
import Zeze.Builtin.Online.Logout;
import Zeze.Builtin.Online.ReLogin;
import Zeze.Builtin.Online.ReliableNotifyConfirm;
import Zeze.Builtin.Online.SReliableNotify;
import Zeze.Builtin.Provider.BBroadcast;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.Provider.Broadcast;
import Zeze.Builtin.Provider.CheckLinkSession;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Builtin.ProviderDirect.TransmitAccount;
import Zeze.Collections.BeanFactory;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
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
import Zeze.Util.EventDispatcher;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongList;
import Zeze.Util.DaemonTimer;
import Zeze.Util.TaskSpec;
import Zeze.Util.TransactionLevelAnnotation;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Arch 版在线模块：维护账号/端点两级在线状态、本地登录数据、可靠通知与 transmit 分发，并按 link 分组向客户端发送协议。
 */
public class Online extends AbstractOnline {
	protected static final Logger logger = LogManager.getLogger(Online.class);

	public final ProviderApp providerApp;
	private final AtomicLong loginTimes = new AtomicLong();

	private final EventDispatcher loginEvents;
	private final EventDispatcher reloginEvents;
	private final EventDispatcher logoutEvents;
	private final EventDispatcher localRemoveEvents;

	@FunctionalInterface
	public interface TransmitAction {
		/**
		 * @param senderAccount  查询发起者，结果发送给他
		 * @param targetAccount  查询目标
		 * @param targetClientId 查询目标
		 * @return 按普通事务处理过程返回值处理
		 */
		long call(String senderAccount, String senderClientId,
		          String targetAccount, String targetClientId,
		          @Nullable Binary parameter) throws Exception;
	}

	private final ConcurrentHashMap<String, TransmitAction> transmitActions = new ConcurrentHashMap<>();

	public static Online create(AppBase app) {
		return GenModule.createRedirectModule(Online.class, app);
	}

	public static long getSpecialTypeIdFromBean(Serializable bean) {
		return bean.typeId();
	}

	public static final BeanFactory beanFactory = new BeanFactory();

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static void register(Class<? extends Serializable> cls) {
		beanFactory.register(cls);
	}

	private volatile long localActiveTimeout = 600 * 1000; // 活跃时间超时。
	private static final long LOCAL_CHECK_PERIOD = 600 * 1000; // 检查间隔
	private final DaemonTimer verifyLocalDaemon = new DaemonTimer("Arch.Online.verifyLocal", LOCAL_CHECK_PERIOD, this::verifyLocal);
	private final AtomicBoolean logoutDelayConfigWarned = new AtomicBoolean();

	protected Online(AppBase app) {
		var zeze = Objects.requireNonNull(app.getZeze());

		loginEvents = new EventDispatcher("Online.Login");
		reloginEvents = new EventDispatcher("Online.Relogin");
		logoutEvents = new EventDispatcher("Online.Logout");
		localRemoveEvents = new EventDispatcher("Online.Local.Remove");

		//noinspection DataFlowIssue
		providerApp = zeze.redirect.providerApp;
		RegisterProtocols(providerApp.providerService);
		RegisterZezeTables(providerApp.zeze);
	}

	public void start() {
		var logoutDelay = providerApp.zeze.getConfig().getOnlineLogoutDelay();
		if (logoutDelay >= localActiveTimeout && logoutDelayConfigWarned.compareAndSet(false, true))
			logger.warn("OnlineLogoutDelay({}ms) >= localActiveTimeout({}ms): "
					+ "verifyLocal可能在重连宽限窗口内提前登出，请调整两者配比。", logoutDelay, localActiveTimeout);
		verifyLocalDaemon.start(); // 幂等：stop后重新start即重启周期检查
		providerApp.builtinModules.put(this.getFullName(), this);
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
			verifyLocalDaemon.setPeriodMs(period); // 下一次续约生效
		} finally {
			unlock();
		}
	}

	public void stop() {
		verifyLocalDaemon.stop(); // 锁外有界等待在飞一轮（预算=timeoutMs+5s）
	}

	@Override
	public void UnRegister() {
		UnRegisterZezeTables(providerApp.zeze);
		UnRegisterProtocols(providerApp.providerService);
	}

	public boolean isOnline(String account) {
		var online = getOnline(account);
		if (null == online)
			return false;
		for (var login : online.getLogins().values()) {
			if (login.getLink().getState() == eLogined)
				return true;
		}
		return false;
	}

	public boolean isOnline(String account, String clientId) {
		var online = getOnline(account);
		if (null == online)
			return false;
		var login = online.getLogins().get(clientId);
		if (null == login)
			return false;
		return login.getLink().getState() == eLogined;
	}

	public @Nullable BOnlines getOnline(String account) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			return _tonline.get(account);
		return _tonline.selectDirty(account);
	}

	public BOnlines getOrAddOnline(String account) {
		return _tonline.getOrAdd(account);
	}

	public @Nullable BOnline getLogin(String account, String clientId) {
		var onlines = getOnline(account);
		if (onlines == null)
			return null;
		var login = onlines.getLogins().get(clientId);
		return login != null && login.getLink().getState() == eLogined ? login : null;
	}

	public int getState(String account, String clientId) {
		var onlines = getOnline(account);
		if (onlines == null)
			return eOffline;
		var login = onlines.getLogins().get(clientId);
		return login != null ? login.getLink().getState() : eOffline;
	}

	public int getLocalCount() {
		return _tlocal.getCacheSize();
	}

	public long walkLocal(TableWalkHandle<String, BLocals> walker) throws Exception {
		return _tlocal.walkMemory(walker);
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

	private BLocal getLoginLocal(String account, String clientId) {
		var bLocals = _tlocal.get(account);
		if (bLocals == null)
			throw new IllegalStateException("account not online. " + account);
		var bLoginLocal = bLocals.getLogins().get(clientId);
		if (bLoginLocal == null || bLoginLocal.getLoginVersion()
				!= getOrAddOnline(account).getLogins().getOrAdd(clientId).getLoginVersion())
			throw new IllegalStateException("account.client not online. " + account + "." + clientId);
		return bLoginLocal;
	}

	// 非抛异常且不建行的本地登录查询，版本号落后于 online 时返回 null。
	private @Nullable BLocal tryGetLoginLocal(String account, String clientId) {
		var bLocals = _tlocal.get(account);
		if (bLocals == null)
			return null;
		var bLoginLocal = bLocals.getLogins().get(clientId);
		if (bLoginLocal == null)
			return null;
		var online = getOnline(account);
		if (online == null)
			return null;
		var loginOnline = online.getLogins().get(clientId);
		return loginOnline != null && bLoginLocal.getLoginVersion() == loginOnline.getLoginVersion() ? bLoginLocal : null;
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

	public void setLocalBean(String account, String clientId, String key, Bean bean) {
		setLocalBean(getLoginLocal(account, clientId), key, bean);
	}

	private void setLocalBean(BLocal login, String key, Bean bean) {
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
	public <T extends Bean> @Nullable T getLocalBean(String account, String clientId, String key) {
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
	public <T extends Bean> T getOrAddLocalBean(String account, String clientId,
	                                                     String key, T defaultHint) {
		var login = getLoginLocal(account, clientId);
		var bAny = login.getDatas().getOrAdd(key);
		if (bAny.getAny().getBean().typeId() == defaultHint.typeId())
			return (T)bAny.getAny().getBean();
		bAny.getAny().setBean(defaultHint);
		return defaultHint;
	}

	private long removeLocalAndTrigger(String account, String clientId) throws Exception {
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
			Transaction.whileCommit(() -> localRemoveEvents.triggerThread(providerApp.zeze, this, arg, account));
		}
		return 0;
	}

	private long logoutTrigger(String account, String clientId, boolean finalLogout) throws Exception {
		var bOnline = getOrAddOnline(account);
		var bLink = bOnline.getLogins().getOrAdd(clientId).getLink();
		bOnline.getLogins().getOrAdd(clientId).setLink(new BLink(bLink.getLinkName(), bLink.getLinkSid(), eOffline));

		var loginVersion = bOnline.getLogins().getOrAdd(clientId);

		// 提交后通知旧服，让清理事务读到已提交的登出状态。
		var oldServerId = loginVersion.getServerId();
		Transaction.whileCommit(() -> tryRedirectRemoveLocal(oldServerId, account));

		// 总是删除
		var removeResult = removeLocalAndTrigger(account, clientId);
		if (removeResult != 0)
			return removeResult;

		var arg = new LogoutEventArgument(account, clientId);
		var ret = logoutEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		logoutEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> logoutEvents.triggerThread(providerApp.zeze, this, arg, account));
		// 仅最终登出清理 ReliableNotify 队列根行；重登补登出需保留队列供后续 reliableNotifySync 使用。
		if (finalLogout) {
			openQueue(account, clientId).remove();
			var login = getOrAddOnline(account).getLogins().getOrAdd(clientId);
			login.setReliableNotifyConfirmIndex(0);
			login.setReliableNotifyIndex(0);
		}
		return 0;
	}

	private long loginTrigger(String account, String clientId) throws Exception {
		var arg = new LoginArgument(account, clientId);
		var ret = loginEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		loginEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> loginEvents.triggerThread(providerApp.zeze, this, arg, account));
		// 挂 whileCommit 计数，避免事务 redo 重复累加。
		Transaction.whileCommit(loginTimes::incrementAndGet);
		return 0;
	}

	private long reloginTrigger(String account, String clientId) throws Exception {
		var arg = new LoginArgument(account, clientId);
		var ret = reloginEvents.triggerEmbed(this, arg);
		if (0 != ret)
			return ret;
		reloginEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> reloginEvents.triggerThread(providerApp.zeze, this, arg, account));
		// 同 loginTrigger，挂 whileCommit 防 redo 重复计数。
		Transaction.whileCommit(loginTimes::incrementAndGet);
		return 0;
	}

	private long tryLogout(BDelayLogoutCustom custom) throws Exception {
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
			var ret = logoutTrigger(account, clientId, true);
			if (0 != ret)
				return ret;
		}
		return Procedure.Success;
	}

	/**
	 * 通过 projectName 查找当前 JVM 内的 Online 实例。
	 * 用于 Timer 持久化数据（如 BDelayLogoutCustom）在进程重启后恢复 Online 上下文，
	 * 替代 static Online instance 的方式。
	 */
	public static @Nullable Online findOnline(String projectName) {
		var app = Application.getAppInstance(projectName);
		if (app == null) {
			logger.warn("findOnline: Application not found. projectName={}", projectName);
			return null;
		}
		var pApp = app.getProviderApp();
		if (pApp == null)
			return null;
		if (!(pApp.providerImplement instanceof ProviderWithOnline pwo)) {
			logger.warn("findOnline: providerImplement is not ProviderWithOnline. projectName={}", projectName);
			return null;
		}
		return pwo.getOnline();
	}

	public static class DelayLogout implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) throws Exception {
			assert context.customData != null;
			logout((BDelayLogoutCustom)context.customData);
		}

		public static void logout(BDelayLogoutCustom custom) throws Exception {
			var online = Online.findOnline(custom.getProjectName());
			if (online == null)
				return; // 应用未启动或已停止：等下一次 tick 或忽略
			var ret = online.tryLogout(custom);
			if (ret == 0)
				return;
			Online.logger.error("tryLogout fail. ret={}, projectName={}", ret, custom.getProjectName());
			// 抛出异常使 Timer 嵌套过程连同半程写入整体回滚，残留状态由 verifyLocal 收敛。
			throw new RuntimeException("DelayLogout tryLogout fail, rollback half-written state. ret=" + ret
					+ ", account=" + custom.getAccount() + ", clientId=" + custom.getClientId()
					+ ", loginVersion=" + custom.getLoginVersion());
		}
	}

	public long onSendError(String account, String clientId,
	                        String linkName, long linkSid) {
		// 仅将当前 link 的 eLogined 状态幂等标记为 eLinkBroken（不建行、不触发登出）；
		// 登出由 linkBroken 调度的 DelayLogout 与 verifyLocal 收敛。
		var online = getOnline(account);
		if (online == null)
			return 0;
		var loginOnline = online.getLogins().get(clientId);
		if (loginOnline == null)
			return 0;
		var link = loginOnline.getLink();
		if (link.getState() != eLogined)
			return 0;
		// 仅处理当前 link 的报告，忽略旧连接迟到事件。
		if (!link.getLinkName().equals(linkName) || link.getLinkSid() != linkSid)
			return 0;
		loginOnline.setLink(new BLink(link.getLinkName(), link.getLinkSid(), eLinkBroken));
		return 0;
	}

	public long linkBroken(String account, String clientId,
	                       String linkName, long linkSid) throws Exception {
		// 不自动建行，忽略已登出或未知条目。
		var online = getOnline(account);
		if (online == null)
			return 0;
		var loginOnline = online.getLogins().get(clientId);
		if (loginOnline == null || loginOnline.getLink().getState() == eOffline)
			return 0;
		// skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
		var link = loginOnline.getLink();
		if (!link.getLinkName().equals(linkName) || link.getLinkSid() != linkSid)
			return 0;

		var local = _tlocal.get(account);
		var loginLocal = local != null ? local.getLogins().get(clientId) : null;
		if (local == null || loginLocal == null) {
			// 崩溃重启后 _tlocal 丢失但 _tonline 仍归属本机时，仍需推进 eLinkBroken 并调度延迟登出。
			if (loginOnline.getServerId() != providerApp.zeze.getConfig().getServerId())
				return 0; // 不在本机登录。
			loginOnline.setLink(new BLink(link.getLinkName(), link.getLinkSid(), eLinkBroken));
			var zezeGhost = providerApp.zeze;
			zezeGhost.getTimer().schedule(TimerSpec.ofDelay(zezeGhost.getConfig().getOnlineLogoutDelay()).times(1),
					DelayLogout.class, new BDelayLogoutCustom(account, clientId, loginOnline.getLoginVersion(),
							zezeGhost.getProjectName()));
			return 0;
		}

		loginOnline.setLink(new BLink(link.getLinkName(), link.getLinkSid(), eLinkBroken));

		if (loginOnline.getLoginVersion() != loginLocal.getLoginVersion()) {
			var ret = removeLocalAndTrigger(account, clientId); // 本机数据已经过时，马上删除。
			if (ret != 0)
				return ret;
		}

		// shorter use
		var zeze = providerApp.zeze;
		var delay = zeze.getConfig().getOnlineLogoutDelay();
		zeze.getTimer().schedule(TimerSpec.ofDelay(delay).times(1), DelayLogout.class,
				new BDelayLogoutCustom(account, clientId, loginOnline.getLoginVersion(), zeze.getProjectName()));
		return 0;
	}

	public void addReliableNotifyMark(String account, String clientId, String listenerName) {
		var login = getLogin(account, clientId);
		if (login == null)
			throw new IllegalStateException("Not Online. AddReliableNotifyMark: " + listenerName);
		login.getReliableNotifyMark().add(listenerName);
	}

	public void removeReliableNotifyMark(String account, String clientId, String listenerName) {
		// 移除尽量通过，不做任何判断。
		var online = getOnline(account);
		if (online != null) {
			var login = online.getLogins().get(clientId);
			if (login != null)
				login.getReliableNotifyMark().remove(listenerName);
		}
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofReliableNotify(online, account, clientId, listenerName).send(p)} 替代。
	 */
	@Deprecated
	public void sendReliableNotify(String account, String clientId,
	                               String listenerName, Protocol<?> p) {
		OnlineSpec.ofReliableNotify(this, account, clientId, listenerName).send(p);
	}

	private Zeze.Collections.Queue<BNotify> openQueue(String account, String clientId) {
		if (usesEncodedReliableQueue(account, clientId) && account.indexOf('@') < 0 && clientId.indexOf('@') < 0) {
			// 旧colon身份可能与其他身份共享队列，消息没有身份标记，不能自动归属/搬运。
			var legacy = providerApp.zeze.getQueueModule().open(legacyReliableQueueName(account, clientId), BNotify.class);
			if (!legacy.isEmpty())
				throw new IllegalStateException("drain the legacy reliable queue before migrating account/clientId");
		}
		return providerApp.zeze.getQueueModule().open(
				reliableNotifyQueueName(account, clientId), BNotify.class);
	}

	private static boolean usesEncodedReliableQueue(String account, String clientId) {
		return account.indexOf(':') >= 0 || clientId.indexOf(':') >= 0
				|| account.indexOf('@') >= 0 || clientId.indexOf('@') >= 0;
	}

	private static String legacyReliableQueueName(String account, String clientId) {
		return "Zeze.Arch.Online.ReliableNotifyQueue:" + account + ':' + clientId;
	}

	private static String encodeReliableQueueIdentity(String identity) {
		// 原始UTF-16 code unit编码，避免UTF-8替换孤立surrogate时使两个Java身份再次碰撞。
		var bytes = new byte[Math.multiplyExact(identity.length(), 2)];
		for (int i = 0; i < identity.length(); i++) {
			var c = identity.charAt(i);
			bytes[i * 2] = (byte)(c >>> 8);
			bytes[i * 2 + 1] = (byte)c;
		}
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String reliableNotifyQueueName(String account, String clientId) {
		if (!usesEncodedReliableQueue(account, clientId))
			return legacyReliableQueueName(account, clientId);
		// 新名字不含':'或'@'，与必含':'的全部旧键不相交；Base64url也不含分隔符'/'。
		return "Zeze.Arch.Online.ReliableNotifyQueue2/" + encodeReliableQueueIdentity(account)
				+ '/' + encodeReliableQueueIdentity(clientId);
	}

	/**
	 * 事务内迁移含 ':' 或 '@' 身份的可靠通知索引：须在停旧写入、结束会话并排空队列后调用。
	 */
	public void migrateLegacyReliableNotifyQueue(String account, String clientId) {
		if (!usesEncodedReliableQueue(account, clientId))
			throw new IllegalStateException("this identity retains the legacy reliable queue");
		var online = getOnline(account);
		var login = online != null ? online.getLogins().get(clientId) : null;
		if (login != null && login.getLink().getState() != eOffline)
			throw new IllegalStateException("end the account/clientId session before migration");
		if (!openQueue(account, clientId).isEmpty())
			throw new IllegalStateException("drain reliable notifications before migration");
		if (login != null) {
			login.setReliableNotifyConfirmIndex(0);
			login.setReliableNotifyIndex(0);
		}
	}

	/**
	 * 发送在线可靠协议，如果不在线等，仍然不会发送哦。
	 * 如果在事务中，则事务提交时才排队发送。如果不在事务中，马上排队发送。
	 *
	 * @param fullEncodedProtocol 协议必须先编码，因为会跨事务。
	 * @deprecated 使用 {@code OnlineSpec.ofReliableNotify(online, account, clientId, listenerName).send(typeId, fullEncodedProtocol)} 替代。
	 */
	@Deprecated
	public void sendReliableNotify(String account, String clientId, String listenerName,
	                               long typeId, Binary fullEncodedProtocol) {
		OnlineSpec.ofReliableNotify(this, account, clientId, listenerName).send(typeId, fullEncodedProtocol);
	}

	public void sendReliableNotifyDirect(String account, String clientId,
	                                     String listenerName,
	                                     long typeId, Binary fullEncodedProtocol) {
		TaskSpec.ofProcedure(providerApp.zeze.newProcedure(() -> {
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
			return Procedure.Success;
		}, "Online.sendReliableNotify." + listenerName)).executeOneByOne(listenerName);
	}

	public @Nullable Long getLogoutVersion(String account, String clientId) {
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

	public @Nullable Long getLoginVersion(String account, String clientId) {
		var login = getLogin(account, clientId);
		return login != null ? login.getLoginVersion() : null;
	}

	public @Nullable Long getLocalLoginVersion(String account, String clientId) {
		var local = _tlocal.get(account);
		if (null == local)
			return null;
		var login = local.getLogins().get(clientId);
		if (null == login)
			return null;
		return login.getLoginVersion();
	}

	public boolean isLogin(String account, String clientId) {
		return getLogin(account, clientId) != null;
	}

	public boolean isAccountLogin(String account) {
		return getAccountLoginCount(account) > 0;
	}

	public int getAccountLoginCount(String account) {
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

	private long triggerLinkBroken(String linkName, LongList errorSids,
	                               Map<Long, BLoginKey> contexts) {
		errorSids.foreach(sid -> TaskSpec.ofProcedure(providerApp.zeze.newProcedure(() -> {
			var ctx = contexts.get(sid);
			if (ctx != null) {
				return onSendError(ctx.getAccount(), ctx.getClientId(), linkName, sid);
			}
			return 0;
		}, "Online.triggerLinkBroken")).run());
		return 0;
	}

	public boolean send(AsyncSocket to, Map<Long, BLoginKey> contexts, Send send) {
		return 0 == send.sendCallbackBrokenTimeout(Objects.requireNonNull(to.getConnector()),
			rpc -> triggerLinkBroken(
				ProviderService.getLinkName(to),
				send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(),
				contexts));
	}

	public boolean send(String linkName, long linkSid, Protocol<?> p) {
		return send(null, linkName, linkSid, p);
	}

	public boolean send(@Nullable BLoginKey loginKey, String linkName, long linkSid, Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		var connector = providerApp.providerService.getLinks().get(linkName);
		if (connector == null) {
			logger.warn("send({}): link connector not found. name={}", p.getTypeId(), linkName);
			return false;
		}
		var link = connector.getSocket();
		if (link == null) {
			logger.warn("send({}): link socket not found. name={}", p.getTypeId(), linkName);
			return false;
		}
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(p.getTypeId()))
			AsyncSocket.log("Send", loginKey != null ? loginKey.getAccount() + ',' + loginKey.getClientId() : linkName, p);
		var pdata = new Binary(p.encode());
		ZezeCounter.instance.addSendSize(p.getTypeId(), pdata.size()); // 内层协议在包装点归因
		var send = new Send(new BSend(p.getTypeId(), pdata));
		send.Argument.getLinkSids().add(linkSid);
		return send(link, loginKey != null ? Map.of(linkSid, loginKey) : Map.of(), send);
	}

	public static final class LinkRoles {
		final String linkName;
		final @Nullable AsyncSocket linkSocket;
		final Send send;
		final ArrayList<BLoginKey> accounts = new ArrayList<>();

		public LinkRoles(String linkName, @Nullable AsyncSocket linkSocket,
		                 long typeId, Binary fullEncodedProtocol) {
			this.linkName = linkName;
			this.linkSocket = linkSocket;
			ZezeCounter.instance.addSendSize(typeId, fullEncodedProtocol.size()); // 内层协议在包装点归因
			send = new Send(new BSend(typeId, fullEncodedProtocol));
		}
	}

	private static @Nullable AsyncSocket getLinkSocket(ConcurrentHashMap<String, Connector> links, String linkName,
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
		errorSids.foreach(linkSid -> TaskSpec.ofProcedure(providerApp.zeze.newProcedure(() -> {
			int idx = group.send.Argument.getLinkSids().indexOf(linkSid);
			if (idx >= 0) {
				var loginKey = group.accounts.get(idx);
				return onSendError(loginKey.getAccount(), loginKey.getClientId(), group.linkName, linkSid);
			}
			return 0;
		}, "Online.triggerLinkBroken2")).run());
	}

	private static long getTypeId(Binary fullEncodedProtocol) {
		int moduleId = ByteBuffer.ToInt(fullEncodedProtocol.bytesUnsafe(), fullEncodedProtocol.getOffset());
		int protocolId = ByteBuffer.ToInt(fullEncodedProtocol.bytesUnsafe(), fullEncodedProtocol.getOffset() + 4);
		return Protocol.makeTypeId(moduleId, protocolId);
	}

	// 可在事务外执行
	public int sendDirect(Set<BLoginKey> loginKeys, long typeId, Binary fullEncodedProtocol,
	                      boolean quietWhenAbsent) {
		if (loginKeys.isEmpty())
			return 0;
		var groups = new HashMap<String, LinkRoles>();
		var links = providerApp.providerService.getLinks();
		for (var loginKey : loginKeys) {
			var account = loginKey.getAccount();
			var clientId = loginKey.getClientId();
			var online = _tonline.selectDirty(account);
			if (online == null) {
				if (!quietWhenAbsent) {
					logger.info("sendDirects({}): not found account={} in _tonline",
							getTypeId(fullEncodedProtocol), account);
				}
				continue;
			}
			var login = online.getLogins().get(clientId);
			if (login == null) {
				if (!quietWhenAbsent) {
					logger.debug("sendDirects({}): not found login for clientId={} account={}",
							getTypeId(fullEncodedProtocol), clientId, account);
				}
				continue;
			}
			var link = login.getLink();
			var state = link.getState();
			if (state != eLogined) {
				if (!quietWhenAbsent) {
					logger.debug("sendDirects({}): state={} != eLogined for clientId={} account={}",
							getTypeId(fullEncodedProtocol), state, clientId, account);
				}
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

			if (0 == group.send.sendCallbackBrokenTimeout(
				Objects.requireNonNull(group.linkSocket.getConnector()),
				rpc -> {
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
	public boolean sendDirect(String account, String clientId, long typeId,
	                          Binary fullEncodedProtocol, boolean quietWhenAbsent) {
		var online = _tonline.selectDirty(account);
		if (online == null) {
			if (!quietWhenAbsent) {
				logger.info("sendDirect({}): not found account={} in _tonline",
						getTypeId(fullEncodedProtocol), account);
			}
			return false;
		}
		var login = online.getLogins().get(clientId);
		if (login == null) {
			if (!quietWhenAbsent) {
				logger.debug("sendDirect({}): not found login for clientId={} account={}",
						getTypeId(fullEncodedProtocol), clientId, account);
			}
			return false;
		}

		var link = login.getLink();
		var state = link.getState();
		if (state != eLogined) {
			if (!quietWhenAbsent) {
				logger.debug("sendDirect({}): state={} != eLogined for clientId={} account={}",
						getTypeId(fullEncodedProtocol), state, clientId, account);
			}
			return false;
		}
		var linkName = link.getLinkName();
		var connector = providerApp.providerService.getLinks().get(linkName);
		if (connector == null) {
			logger.warn("sendDirect({}): not found connector for linkName={} account={} clientId={}",
					getTypeId(fullEncodedProtocol), linkName, account, clientId);
			TaskSpec.ofProcedure(providerApp.zeze.newProcedure(() -> {
				var rc = onSendError(account, clientId, linkName, link.getLinkSid());
				if (rc != 0)
					logger.error("triggerLinkBroken1 failed: linkName={}, linkSid={}, account={}, clientId={}, rc={}",
							linkName, link.getLinkSid(), account, clientId, rc);
				return rc;
			}, "Online.triggerLinkBroken1")).run();
			return false;
		}
		if (!connector.isHandshakeDone()) {
			logger.warn("sendDirect({}): not isHandshakeDone for linkName={} account={} clientId={}",
					getTypeId(fullEncodedProtocol), linkName, account, clientId);
			TaskSpec.ofProcedure(providerApp.zeze.newProcedure(() -> {
				var rc = onSendError(account, clientId, linkName, link.getLinkSid());
				if (rc != 0)
					logger.error("triggerLinkBroken1 failed: linkName={}, linkSid={}, account={}, clientId={}, rc={}",
							linkName, link.getLinkSid(), account, clientId, rc);
				return rc;
			}, "Online.triggerLinkBroken1")).run();
			return false;
		}
		// 后面保存connector.socket并使用，如果之后连接被关闭，以后发送协议失败。
		var linkSocket = connector.getSocket();
		if (linkSocket == null) {
			logger.warn("sendDirect({}): closed connector for linkName={} account={} clientId={}",
					getTypeId(fullEncodedProtocol), linkName, account, clientId);
			TaskSpec.ofProcedure(providerApp.zeze.newProcedure(() -> {
				var rc = onSendError(account, clientId, linkName, link.getLinkSid());
				if (rc != 0)
					logger.error("triggerLinkBroken1 failed: linkName={}, linkSid={}, account={}, clientId={}, rc={}",
							linkName, link.getLinkSid(), account, clientId, rc);
				return rc;
			}, "Online.triggerLinkBroken1")).run();
			return false;
		}
		ZezeCounter.instance.addSendSize(typeId, fullEncodedProtocol.size()); // 内层协议在包装点归因
		var send = new Send(new BSend(typeId, fullEncodedProtocol));
		send.Argument.getLinkSids().add(link.getLinkSid());
		return 0 == send.sendCallbackBrokenTimeout(
			connector,
			rpc -> {
			if (send.isTimeout() || !send.Result.getErrorLinkSids().isEmpty()) {
				var linkSid = send.Argument.getLinkSids().get(0);
				TaskSpec.ofProcedure(providerApp.zeze.newProcedure(() -> {
					var rc = onSendError(account, clientId, linkName, linkSid);
					if (rc != 0)
						logger.error("triggerLinkBroken1 failed: linkName={}, linkSid={}, account={}, clientId={}, rc={}",
								linkName, linkSid, account, clientId, rc);
					return rc;
				}, "Online.triggerLinkBroken1")).run();
			}
			return Procedure.Success;
		});
	}

	public static class LoginOnLink {
		public String linkName = ""; // empty when not online
		public @Nullable AsyncSocket linkSocket; // null if not online
		public long providerSessionId;
		public final HashMap<BLoginKey, Long> logins = new HashMap<>();
		public final HashMap<Long, BLoginKey> contexts = new HashMap<>();
	}

	/** @deprecated 使用 {@code OnlineSpec.ofLogins(online, loginKeys).quietWhenAbsent(quietWhenAbsent).sendNow(typeId, fullEncodedProtocol)} 替代（立即语义与发送计数保持一致）。 */
	@Deprecated
	public int send(Collection<BLoginKey> loginKeys, long typeId, Binary fullEncodedProtocol,
	                boolean quietWhenAbsent) {
		return OnlineSpec.ofLogins(this, loginKeys).quietWhenAbsent(quietWhenAbsent).sendNow(typeId, fullEncodedProtocol);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofLogin(online, account, clientId).send(p)} 替代。
	 */
	@Deprecated
	public void send(String account, String clientId, Protocol<?> p) {
		OnlineSpec.ofLogin(this, account, clientId).send(p);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofLogin(online, account, clientId).sendResponse(r)} 替代。
	 */
	@Deprecated
	public void sendResponse(String account, String clientId, Rpc<?, ?> r) {
		OnlineSpec.ofLogin(this, account, clientId).sendResponse(r);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofLogins(online, logins).send(p)} 替代。
	 */
	@Deprecated
	public void send(Collection<BLoginKey> logins, Protocol<?> p) {
		OnlineSpec.ofLogins(this, logins).send(p);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofLogin(online, account, clientId).send(p)} 替代。事务内自动在提交时发送。
	 */
	@Deprecated
	public void sendWhileCommit(String account, String clientId, Protocol<?> p) {
		OnlineSpec.ofLogin(this, account, clientId).send(p);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofLogins(online, logins).send(p)} 替代。事务内自动在提交时发送。
	 */
	@Deprecated
	public void sendWhileCommit(Collection<BLoginKey> logins, Protocol<?> p) {
		OnlineSpec.ofLogins(this, logins).send(p);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofLogin(online, account, clientId).sendResponse(r)} 替代。事务内自动在提交时发送。
	 */
	@Deprecated
	public void sendResponseWhileCommit(String account, String clientId, Rpc<?, ?> r) {
		OnlineSpec.ofLogin(this, account, clientId).sendResponse(r);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofLogin(online, account, clientId).sendWhileRollback(p)} 替代。
	 */
	@Deprecated
	public void sendWhileRollback(String account, String clientId, Protocol<?> p) {
		OnlineSpec.ofLogin(this, account, clientId).sendWhileRollback(p);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofLogins(online, logins).sendWhileRollback(p)} 替代。
	 */
	@Deprecated
	public void sendWhileRollback(Collection<BLoginKey> logins, Protocol<?> p) {
		OnlineSpec.ofLogins(this, logins).sendWhileRollback(p);
	}

	public int sendAccountDirect(String account, long typeId, Binary fullEncodedProtocol,
	                             boolean quietWhenAbsent) {
		var groups = new HashMap<String, LinkRoles>();
		var links = providerApp.providerService.getLinks();
		var online = _tonline.selectDirty(account);
		if (online == null) {
			if (!quietWhenAbsent) {
				logger.info("sendAccountDirect({}): not found account={} in _tonline",
						getTypeId(fullEncodedProtocol), account);
			}
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
			group.accounts.add(new BLoginKey(account, e.getKey()));
		}
		int sendCount = 0;
		for (var group : groups.values()) {
			if (null == group.linkSocket) {
				processErrorSids(group.send.Argument.getLinkSids(), group);
				continue; // link miss process done.
			}
			if (0 == group.send.sendCallbackBrokenTimeout(
				Objects.requireNonNull(group.linkSocket.getConnector()), rpc -> {
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
	public int sendAccountsDirect(Collection<String> accounts, long typeId, Binary fullEncodedProtocol,
	                              boolean quietWhenAbsent) {
		if (accounts.isEmpty())
			return 0;
		var groups = new HashMap<String, LinkRoles>();
		var links = providerApp.providerService.getLinks();
		for (var account : accounts) {
			var online = _tonline.selectDirty(account);
			if (online == null) {
				if (!quietWhenAbsent) {
					logger.info("sendAccountsDirect({}): not found account={} in _tonline",
							getTypeId(fullEncodedProtocol), account);
				}
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
				group.accounts.add(new BLoginKey(account, e.getKey()));
			}
		}
		int sendCount = 0;
		for (var group : groups.values()) {
			if (null == group.linkSocket) {
				processErrorSids(group.send.Argument.getLinkSids(), group);
				continue; // link miss process done.
			}

			if (0 == group.send.sendCallbackBrokenTimeout(
				Objects.requireNonNull(group.linkSocket.getConnector()), rpc -> {
				var send = group.send;
				var errorSids = send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids();
				processErrorSids(errorSids, group);
				return Procedure.Success;
			}))
				sendCount++;
		}
		return sendCount;
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofAccount(online, account).send(typeId, fullEncodedProtocol)} 替代。
	 */
	@Deprecated
	public void sendAccount(String account, long typeId, Binary fullEncodedProtocol) { // OnlineSend sender
		OnlineSpec.ofAccount(this, account).send(typeId, fullEncodedProtocol);
	}

	/** @deprecated 使用 {@code OnlineSpec.ofAccounts(online, accounts).sendNow(typeId, fullEncodedProtocol)} 替代（立即语义与发送计数保持一致）。 */
	@Deprecated
	public int sendAccounts(Collection<String> accounts, long typeId, Binary fullEncodedProtocol) { // OnlineSend sender
		return OnlineSpec.ofAccounts(this, accounts).sendNow(typeId, fullEncodedProtocol);
	}

	/**
	 * 给账号所有的登录终端发送消息。
	 *
	 * @deprecated 使用 {@code OnlineSpec.ofAccount(online, account).send(p)} 替代。
	 */
	@Deprecated
	public void sendAccount(String account, Protocol<?> p/*, OnlineSend sender*/) {
		OnlineSpec.ofAccount(this, account).send(p);
	}

	/**
	 * 给账号所有的登录终端发送消息。
	 *
	 * @deprecated 使用 {@code OnlineSpec.ofAccounts(online, accounts).send(p)} 替代。
	 */
	@Deprecated
	public void sendAccounts(Collection<String> accounts, Protocol<?> p/*, OnlineSend sender*/) {
		OnlineSpec.ofAccounts(this, accounts).send(p);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofAccount(online, account).send(p)} 替代。事务内自动在提交时发送。
	 */
	@Deprecated
	public void sendAccountWhileCommit(String account, Protocol<?> p/*, OnlineSend sender*/) {
		OnlineSpec.ofAccount(this, account).send(p);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofAccounts(online, accounts).send(p)} 替代。事务内自动在提交时发送。
	 */
	@Deprecated
	public void sendAccountsWhileCommit(Collection<String> accounts, Protocol<?> p/*, OnlineSend sender*/) {
		OnlineSpec.ofAccounts(this, accounts).send(p);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofAccount(online, account).sendWhileRollback(p)} 替代。
	 */
	@Deprecated
	public void sendAccountWhileRollback(String account, Protocol<?> p/*, OnlineSend sender*/) {
		OnlineSpec.ofAccount(this, account).sendWhileRollback(p);
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofAccounts(online, accounts).sendWhileRollback(p)} 替代。
	 */
	@Deprecated
	public void sendAccountsWhileRollback(Collection<String> accounts, Protocol<?> p/*, OnlineSend sender*/) {
		OnlineSpec.ofAccounts(this, accounts).sendWhileRollback(p);
	}

	/**
	 * 转发查询请求给RoleId。
	 *
	 * @param account    查询发起者，结果发送给他。
	 * @param actionName 查询处理的实现
	 * @param target     目标角色
	 * @deprecated 使用 {@code OnlineSpec.ofTransmit(online, account, clientId, actionName, target, targetClientId).parameter(parameter).transmit()} 替代。
	 */
	@Deprecated
	public void transmit(String account, String clientId, String actionName,
	                     String target, String targetClientId, @Nullable Serializable parameter) {
		OnlineSpec.ofTransmit(this, account, clientId, actionName, target, targetClientId).parameter(parameter).transmit();
	}

	private void processTransmit(String account, String clientId, String actionName,
	                             Collection<BLoginKey> accounts, @Nullable Binary parameter) {
		var handle = transmitActions.get(actionName);
		if (handle != null) {
			for (var target : accounts) {
				var rc = providerApp.zeze.newProcedure(() ->
								handle.call(account, clientId, target.getAccount(), target.getClientId(), parameter),
						"Online.processTransmit:" + actionName).call();
				if (rc != 0)
					logger.error("processTransmit fail: action={}, target={}:{}, rc={}",
							actionName, target.getAccount(), target.getClientId(), rc);
			}
		} else {
			var sample = new ArrayList<String>();
			var size = 0;
			for (var target : accounts) {
				size++;
				if (sample.size() < 8)
					sample.add(target.getAccount() + ":" + target.getClientId());
			}
			logger.error("processTransmit unknown action: action={}, sender={}:{}, targets.size={}, sample={}",
					actionName, account, clientId, size, sample);
		}
	}

	public static class RoleOnServer {
		public final int serverId; // empty when not online
		public final HashSet<BLoginKey> accounts = new HashSet<>();

		RoleOnServer() {
			this(-1);
		}

		RoleOnServer(int serverId) {
			this.serverId = serverId;
		}

		void addAll(HashSet<BLoginKey> accounts) {
			this.accounts.addAll(accounts);
		}
	}

	public IntHashMap<RoleOnServer> groupByServer(Collection<BLoginKey> accounts) {
		var groups = new IntHashMap<RoleOnServer>();
		var groupNotOnline = new RoleOnServer(); // LinkName is Empty And Socket is null.
		groups.put(groupNotOnline.serverId, groupNotOnline);

		for (var account : accounts) {
			var online = getOnline(account.getAccount());
			if (online == null || online.getLogins().isEmpty()) {
				// null != online 意味着这里肯定不为0，理论上不会到达这个分支。
				groupNotOnline.accounts.add(account);
				continue;
			}
			var login = online.getLogins().get(account.getClientId());
			if (login == null || login.getLink().getState() != eLogined) {
				groupNotOnline.accounts.add(account);
				continue;
			}
			var serverId = login.getServerId();
			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(serverId);
			if (group == null) {
				group = new RoleOnServer(serverId);
				groups.put(serverId, group);
			}
			group.accounts.add(account);
		}
		return groups;
	}

	private static RoleOnServer merge(@Nullable RoleOnServer current, RoleOnServer m) {
		if (current == null)
			return m;
		current.addAll(m.accounts);
		return current;
	}

	private void transmitInProcedure(String account, String clientId, String actionName,
	                                 Collection<BLoginKey> accounts, @Nullable Binary parameter) {
		if (!providerApp.zeze.getConfig().hasGlobal()) {
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
			transmit.Argument.getTargets().addAll(group.accounts);
			if (parameter != null)
				transmit.Argument.setParameter(parameter);

			//noinspection DataFlowIssue
			var ps = providerApp.providerDirectService.providerByServerId.get(group.serverId);
			if (ps == null) {
				if (groupLocal == null)
					groupLocal = new RoleOnServer();
				groupLocal.addAll(group.accounts);
				continue;
			}
			var socket = providerApp.providerDirectService.GetSocket(ps.sessionId);
			if (socket == null) {
				if (groupLocal == null)
					groupLocal = new RoleOnServer();
				groupLocal.addAll(group.accounts);
				continue;
			}
			// 延迟到提交后发送，避免事务 redo 向远端重复投递。
			Transaction.whileCommit(() -> transmit.Send(socket));
		}
		assert groupLocal != null;
		if (!groupLocal.accounts.isEmpty())
			processTransmit(account, clientId, actionName, groupLocal.accounts, parameter);
	}

	public void transmit(String account, String clientId, String actionName,
	                     Collection<BLoginKey> targets, @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);

		var binaryParam = parameter == null ? Binary.Empty : new Binary(ByteBuffer.encode(parameter));
		// 发送协议请求在另外的事务中执行。
		TaskSpec.ofProcedure(providerApp.zeze.newProcedure(() -> {
			transmitInProcedure(account, clientId, actionName, targets, binaryParam);
			return Procedure.Success;
		}, "Online.transmit")).run();
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofTransmit(online, account, clientId, actionName, targetAccount, targetClientId).parameter(parameter).transmit()} 替代。事务内自动在提交时发送。
	 */
	@Deprecated
	public void transmitWhileCommit(String account, String clientId, String actionName,
	                                String targetAccount, String targetClientId, @Nullable Serializable parameter) {
		OnlineSpec.ofTransmit(this, account, clientId, actionName, targetAccount, targetClientId).parameter(parameter).transmit();
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofTransmit(online, account, clientId, actionName, targets).parameter(parameter).transmit()} 替代。事务内自动在提交时发送。
	 */
	@Deprecated
	public void transmitWhileCommit(String account, String clientId, String actionName,
	                                Collection<BLoginKey> targets, @Nullable Serializable parameter) {
		OnlineSpec.ofTransmit(this, account, clientId, actionName, targets).parameter(parameter).transmit();
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofTransmit(online, account, clientId, actionName, targetAccount, targetClientId).parameter(parameter).transmitWhileRollback()} 替代。
	 */
	@Deprecated
	public void transmitWhileRollback(String account, String clientId, String actionName,
	                                  String targetAccount, String targetClientId, @Nullable Serializable parameter) {
		OnlineSpec.ofTransmit(this, account, clientId, actionName, targetAccount, targetClientId).parameter(parameter).transmitWhileRollback();
	}

	/**
	 * @deprecated 使用 {@code OnlineSpec.ofTransmit(online, account, clientId, actionName, targets).parameter(parameter).transmitWhileRollback()} 替代。
	 */
	@Deprecated
	public void transmitWhileRollback(String account, String clientId, String actionName,
	                                  Collection<BLoginKey> targets, @Nullable Serializable parameter) {
		OnlineSpec.ofTransmit(this, account, clientId, actionName, targets).parameter(parameter).transmitWhileRollback();
	}

	private int broadcast(long typeId, Binary fullEncodedProtocol, int time, boolean onlySameVersion) {
		ZezeCounter.instance.addSendSize(typeId, fullEncodedProtocol.size()); // 内层协议在包装点归因
		var broadcast = new Broadcast(new BBroadcast.Data(typeId, fullEncodedProtocol, time, onlySameVersion));
		var pdata = broadcast.encode();
		int sendCount = 0;
		for (var link : providerApp.providerService.getLinks().values()) {
			// 单次读取 socket 后判空，避免与 Connector.stop 并发导致 NPE。
			var so = link.getSocket();
			if (so != null && so.Send(pdata)) {
				ZezeCounter.instance.addSendSize(Broadcast.TypeId_, pdata.size()); // wrapper归因
				sendCount++;
			}
		}
		return sendCount;
	}

	public int broadcast(Protocol<?> p) {
		return broadcast(p, 60 * 1000, false);
	}

	public int broadcast(Protocol<?> p, boolean onlySameVersion) {
		return broadcast(p, 60 * 1000, onlySameVersion);
	}

	public int broadcast(Protocol<?> p, int time) {
		return broadcast(p, time, false);
	}

	public int broadcast(Protocol<?> p, int time, boolean onlySameVersion) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Broc", providerApp.providerService.getLinks().size(), p);
		return broadcast(typeId, new Binary(p.encode()), time, onlySameVersion);
	}

	class VerifyBatch {
		private final ArrayList<String> accounts = new ArrayList<>();

		void add(String account) {
			var aTime = localActiveTimes.get(account);
			if (aTime != null && System.currentTimeMillis() - aTime > localActiveTimeout)
				accounts.add(account);
		}

		void tryPerform() {
			if (accounts.size() > 10)
				perform();
		}

		void perform() {
			if (!accounts.isEmpty()) {
				try {
					// 传播 tryRemoveLocal 失败码使整批回滚，随后仍发 CheckLinkSession 探测驱动清理。
					var rc = providerApp.zeze.newProcedure(() -> {
						for (var account : accounts) {
							var rc2 = tryRemoveLocal(account);
							if (rc2 != 0) {
								logger.error("tryRemoveLocal fail. account={}, rc={}", account, rc2);
								return rc2;
							}
						}
						return 0L;
					}, "Online.verifyLocal").call();
					if (rc != 0)
						logger.error("verifyLocal batch failed: rc={}, accounts={}", rc, accounts);
					sendAccountsDirect(accounts, CheckLinkSession.TypeId_, new Binary(new CheckLinkSession().encode()), true);
				} catch (Exception e) {
					logger.error("", e);
				}
				accounts.clear();
			}
		}
	}

	private void verifyLocal() throws Exception {
		var batch = new VerifyBatch();
		// 锁外执行事务
		_tlocal.walkMemory((k, v) -> {
			batch.add(k);
			batch.tryPerform();
			return true;
		});
		batch.perform();
	}

	private long tryRemoveLocal(String account) throws Exception {
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
			} else if (login.getLink().getState() == eLinkBroken) {
				// eLinkBroken 残留（如 DelayLogout 失败回滚后）在此重走 tryLogout 收敛。
				var ret = tryLogout(new BDelayLogoutCustom(account, clientId, login.getLoginVersion(),
						providerApp.zeze.getProjectName()));
				if (ret != 0)
					return ret;
			}
		}
		return 0;
	}

	@RedirectToServer
	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	protected void redirectRemoveLocal(int serverId, String account) {
		var rc = providerApp.zeze.newProcedure(() -> tryRemoveLocal(account), "Online.redirectRemoveLocal").call();
		if (rc != 0)
			logger.error("redirectRemoveLocal failed: serverId={}, account={}, rc={}", serverId, account, rc);
	}

	private void tryRedirectRemoveLocal(int serverId, String account) {
		//noinspection DataFlowIssue
		if (providerApp.zeze.getConfig().getServerId() != serverId
				&& providerApp.providerDirectService.providerByServerId.containsKey(serverId))
			redirectRemoveLocal(serverId, account);
	}

	private boolean isPreparedLogin(String account, String clientId, long version) {
		var online = getOnline(account);
		if (online == null || !online.getAccount().equals(account))
			return false;
		var login = online.getLogins().get(clientId);
		return login != null && login.getLoginVersion() == version && login.getLogoutVersion() == version
				&& login.getLink().getState() == eOffline;
	}

	// 首次观察的会话代数跨事务重试保持不变；续办版本只在补登出提交后发布。
	private static final class LoginAttempt {
		private @Nullable Long observedVersion;
		private @Nullable Long committedLogoutVersion;

		private boolean observe(long version) {
			var observed = observedVersion;
			if (observed != null)
				return observed == version;
			observedVersion = version;
			return true;
		}
	}

	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	@Override
	protected long ProcessLoginRequest(Login rpc) {
		return processLogin(rpc, rpc.Argument.getClientId(), false, 0);
	}

	private long processLogin(Protocol<?> rpc, String clientId, boolean relogin, long confirmIndex) {
		if (Transaction.getCurrent() != null)
			throw new IllegalStateException("Login/ReLogin must run outside a transaction");
		if (clientId.isEmpty())
			return Procedure.LogicError;
		var attempt = new LoginAttempt();
		var actionName = relogin ? "ProcessReLoginRequest" : "ProcessLoginRequest";
		long result = TaskSpec.ofProcedure(providerApp.zeze.newProcedure(
				() -> processLoginInTransaction(rpc, clientId, relogin, confirmIndex, attempt, null), actionName)).call();
		var logoutVersion = attempt.committedLogoutVersion;
		if (result != 0 || logoutVersion == null)
			return result;
		return TaskSpec.ofProcedure(providerApp.zeze.newProcedure(
				() -> processLoginInTransaction(rpc, clientId, relogin, confirmIndex, attempt, logoutVersion),
				actionName + "AfterLogout")).call();
	}

	private long processLoginInTransaction(Protocol<?> rpc, String clientId, boolean relogin, long confirmIndex,
			LoginAttempt attempt, @Nullable Long expectedLogoutVersion) throws Exception {
		var session = ProviderUserSession.get(rpc);
		var account = session.getAccount();
		if (expectedLogoutVersion != null
				&& !isPreparedLogin(account, clientId, expectedLogoutVersion))
			return errorCode(ResultCodeLoginSuperseded);

		var online = getOrAddOnline(account);
		var loginOnline = online.getLogins().getOrAdd(clientId);

		var onlineAccount = online.getAccount();
		var isBound = !onlineAccount.isEmpty();
		if (!isBound)
			online.setAccount(account); // 这里依赖登录事件做进一步的角色是否属于账号的验证,验证失败会回滚
		else if (!onlineAccount.equals(account))
			return Procedure.LogicError;

		if (!attempt.observe(loginOnline.getLoginVersion()))
			return errorCode(ResultCodeLoginSuperseded);

		if (expectedLogoutVersion == null && isBound
				&& loginOnline.getLoginVersion() != loginOnline.getLogoutVersion()) {
			var logoutVersion = loginOnline.getLoginVersion();
			loginOnline.setLogoutVersion(loginOnline.getLoginVersion());

			var link = loginOnline.getLink();
			if (!link.getLinkName().equals(session.getLinkName()) || link.getLinkSid() != session.getLinkSid()) {
				var oldLinkName = link.getLinkName();
				var oldLinkSid = link.getLinkSid();
				var description = "duplicate login " + account + ":" + clientId;
				Transaction.whileCommit(() -> providerApp.providerService.kick(
						oldLinkName, oldLinkSid, BKick.ErrorDuplicateLogin, description));
			}
			var ret = logoutTrigger(account, clientId, false);
			if (0 != ret)
				return ret;
			// 重试或回滚不能留下待续办标记。
			Transaction.whileCommit(() -> attempt.committedLogoutVersion = logoutVersion);
			return Procedure.Success;
		}

		// 开始登录流程，先准备 link-state 与本服 local 记录（跨服补登出不得提前创建 _tlocal，避免误触发本服 localRemoveEvents）。
		var local = _tlocal.getOrAdd(account);
		var loginLocal = local.getLogins().getOrAdd(clientId);
		Transaction.whileCommit(() -> putLocalActiveTime(account));

		var loginVersionSerialId = online.getLastLoginVersion() + 1;
		online.setLastLoginVersion(loginVersionSerialId);
		loginOnline.setLoginVersion(loginVersionSerialId);
		loginLocal.setLoginVersion(loginVersionSerialId);

		loginOnline.setLink(new BLink(session.getLinkName(), session.getLinkSid(), eLogined));

		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			setUserState.Argument.getUserState().setContext(clientId);
			//noinspection DataFlowIssue
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		if (!relogin) {
			loginOnline.setReliableNotifyConfirmIndex(0);
			loginOnline.setReliableNotifyIndex(0);
			loginOnline.getReliableNotifyMark().clear();
			openQueue(account, clientId).clear();
		}

		loginOnline.setServerId(providerApp.zeze.getConfig().getServerId());

		session.respond(rpc);
		if (!relogin)
			return loginTrigger(account, clientId);
		var ret = reloginTrigger(account, clientId);
		if (0 != ret)
			return ret;

		var syncResultCode = reliableNotifySync(account, clientId,
				session, confirmIndex, true);

		if (syncResultCode != ResultCodeSuccess)
			return errorCode((short)syncResultCode);

		return Procedure.Success;
	}

	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	@Override
	protected long ProcessReLoginRequest(ReLogin rpc) {
		return processLogin(rpc, rpc.Argument.getClientId(), true, rpc.Argument.getReliableNotifyConfirmIndex());
	}

	@Override
	protected long ProcessLogoutRequest(Logout rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);

		if (!session.isLogin())
			return errorCode(ResultCodeNotLogin);

		var account = session.getAccount();
		var clientId = session.getContext();
		var login = getLogin(account, clientId);
		if (login != null) {
			if (!isCurrentLink(login, session))
				return errorCode(ResultCodeNotLogin);
			login.setLogoutVersion(login.getLoginVersion());
			var ret = logoutTrigger(account, clientId, true);
			if (0 != ret)
				return ret;
			// online 被删除
		}
		// 先设置状态，再发送Logout结果。
		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			//noinspection DataFlowIssue
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});
		session.respond(rpc);
		// 在 OnLinkBroken 时处理。可以同时处理网络异常的情况。
		return Procedure.Success;
	}

	private static boolean isCurrentLink(BOnline login, ProviderUserSession session) {
		var link = login.getLink();
		return link.getState() == eLogined && link.getLinkSid() == session.getLinkSid()
				&& link.getLinkName().equals(session.getLinkName());
	}

	private int reliableNotifySync(String account, String clientId,
	                               ProviderUserSession session, long index, boolean sync) throws Exception {
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
			session.respond(notify);
		}
		return ResultCodeSuccess;
	}

	@Override
	protected long ProcessReliableNotifyConfirmRequest(ReliableNotifyConfirm rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);

		// 拒绝未登录会话，避免向 _tonline 注入空 clientId 条目。
		if (!session.isLogin())
			return errorCode(ResultCodeNotLogin);

		var clientId = session.getContext();
		var online = getOnline(session.getAccount());
		if (online == null)
			return errorCode(ResultCodeOnlineDataNotFound);
		var login = online.getLogins().get(clientId);
		if (login == null || !isCurrentLink(login, session))
			return errorCode(ResultCodeNotLogin);

		session.respond(rpc); // 同步前提交。
		var syncResultCode = reliableNotifySync(session.getAccount(), clientId,
				session, rpc.Argument.getReliableNotifyConfirmIndex(), rpc.Argument.isSync());

		if (ResultCodeSuccess != syncResultCode)
			return errorCode((short)syncResultCode);

		return Procedure.Success;
	}

	/**
	 * 动态绑定模块。返回 true 仅表示在线记录存在且已登记在事务提交后发送 Bind，不代表 linkd 已送达。
	 */
	public boolean bindDynamic(String account, String clientId, int... moduleIds) {
		var login = getLogin(account, clientId);
		if (null == login)
			return false;

		var link = login.getLink();

		Transaction.whileCommit(() -> {
			var connector = providerApp.providerService.getLinks().get(link.getLinkName());
			var socket = connector == null ? null : connector.getSocket();
			if (socket == null) {
				logger.warn("bindDynamic: link miss. linkName={}, account={}, clientId={}, moduleIds={}",
						link.getLinkName(), account, clientId, java.util.Arrays.toString(moduleIds));
				return;
			}
			var bind = new Zeze.Builtin.Provider.Bind();
			bind.Argument.getLinkSids().add(link.getLinkSid());
			for (var moduleId : moduleIds) {
				bind.Argument.getModules().put(moduleId, new BModule.Data(
						BModule.ChoiceTypeDefault, true));
			}
			// 仅对同步发送失败（如连接关闭）记告警。
			if (bind.SendForWait(socket).isCompletedExceptionally())
				logger.warn("bindDynamic: send fail. linkName={}, account={}, clientId={}, moduleIds={}",
						link.getLinkName(), account, clientId, java.util.Arrays.toString(moduleIds));
		});
		return true;
	}
}
