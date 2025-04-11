package Zeze.Game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import Zeze.AppBase;
import Zeze.Arch.Beans.BSend;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderImplement;
import Zeze.Arch.ProviderService;
import Zeze.Arch.ProviderUserSession;
import Zeze.Arch.RedirectToServer;
import Zeze.Builtin.Game.Online.BAny;
import Zeze.Builtin.Game.Online.BDelayLogoutCustom;
import Zeze.Builtin.Game.Online.BLink;
import Zeze.Builtin.Game.Online.BLocal;
import Zeze.Builtin.Game.Online.BNotify;
import Zeze.Builtin.Game.Online.BOnline;
import Zeze.Builtin.Game.Online.BOnlineShared;
import Zeze.Builtin.Game.Online.BReliableNotify;
import Zeze.Builtin.Game.Online.Login;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.Game.Online.ReLogin;
import Zeze.Builtin.Game.Online.ReliableNotifyConfirm;
import Zeze.Builtin.Game.Online.SReliableNotify;
import Zeze.Builtin.Game.Online.tOnline;
import Zeze.Builtin.Game.Online.tOnlineShared;
import Zeze.Builtin.Game.Online.tRoleOfflineTimers;
import Zeze.Builtin.Game.Online.tRoleTimers;
import Zeze.Builtin.Game.Online.tlocal;
import Zeze.Builtin.Provider.BBroadcast;
import Zeze.Builtin.Provider.BKick;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.Provider.Broadcast;
import Zeze.Builtin.Provider.CheckLinkSession;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.Provider.SetUserState;
import Zeze.Builtin.ProviderDirect.Transmit;
import Zeze.Collections.BeanFactory;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerRole;
import Zeze.Hot.HotBeanFactory;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Hot.HotUpgrade;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.EventDispatcher;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.LongList;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TransactionLevelAnnotation;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Online extends AbstractOnline implements HotUpgrade, HotBeanFactory {
	protected static final @NotNull Logger logger = LogManager.getLogger(Online.class);
	protected static final BeanFactory beanFactory = new BeanFactory();
	protected static @Nullable Online defaultInstance; // 默认Online实例,stop后会置null

	public final @NotNull ProviderApp providerApp;
	private final AtomicLong loginTimes = new AtomicLong();
	private final @NotNull TimerRole timerRole;

	private final @NotNull EventDispatcher loginEvents;
	private final @NotNull EventDispatcher reloginEvents;
	private final @NotNull EventDispatcher logoutEvents;
	private final @NotNull EventDispatcher localRemoveEvents;
	private final @NotNull EventDispatcher linkBrokenEvents;

	// 缓存拥有Local数据的HotModule，用来优化。
	private final ConcurrentHashSet<HotModule> hotModulesHaveLocal = new ConcurrentHashSet<>();
	private boolean freshStopModuleLocal;
	private final ConcurrentHashSet<HotModule> hotModulesHaveDynamic = new ConcurrentHashSet<>();
	private boolean freshStopModuleDynamic;
	private volatile long localActiveTimeout = 600 * 1000; // 活跃时间超时。
	private volatile long localCheckPeriod = 600 * 1000; // 检查间隔
	private final AtomicInteger verifyLocalCount = new AtomicInteger();

	private final ConcurrentHashMap<String, TransmitAction> transmitActions = new ConcurrentHashMap<>();
	private @Nullable Future<?> verifyLocalTimer;

	public @NotNull ProviderApp getProviderApp() {
		return providerApp;
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

	private void onHotModuleStop(HotModule hot) {
		freshStopModuleLocal |= hotModulesHaveLocal.remove(hot) != null;
		freshStopModuleDynamic |= hotModulesHaveDynamic.remove(hot) != null;
	}

	@Override
	public boolean hasFreshStopModuleLocalOnce() {
		var tmp = freshStopModuleLocal;
		freshStopModuleLocal = false;
		return tmp;
	}

	@Override
	public boolean hasFreshStopModuleDynamicOnce() {
		var tmp = freshStopModuleDynamic;
		freshStopModuleDynamic = false;
		return tmp;
	}

	@Override
	public void clearTableCache() {
		_tOnline.__ClearTableCacheUnsafe__();
		_tOnlineShared.__ClearTableCacheUnsafe__();
	}

	private void tryRecordHotModule(Class<?> customClass) {
		var cl = customClass.getClassLoader();
		if (HotManager.isHotModule(cl)) {
			var hotModule = (HotModule)cl;
			hotModule.stopEvents.add(this::onHotModuleStop);
			hotModulesHaveDynamic.add(hotModule);
		}
	}

	@Override
	public void processWithNewClasses(@NotNull List<Class<?>> newClasses) {
		for (var cls : newClasses)
			tryRecordHotModule(cls);
	}

	@Override
	public BeanFactory beanFactory() {
		return beanFactory;
	}

	static class Retreat {
		final long roleId;
		final @NotNull String key;
		final @NotNull Bean bean;

		Retreat(long roleId, @NotNull String key, @NotNull Bean bean) {
			this.roleId = roleId;
			this.key = key;
			this.bean = bean;
		}
	}

	@Override
	public void upgrade(@NotNull Function<Bean, Bean> retreatFunc) throws Exception {
		// 如果需要，重建_tlocal内存表的用户设置的bean。
		var retreats = new ArrayList<Retreat>();
		_tlocal.walk((roleId, locals) -> {
			for (var data : locals.getDatas()) {
				var retreatBean = retreatFunc.apply(data.getValue().getAny().getBean());
				if (retreatBean != null) {
					beanFactory.register(retreatBean); // 覆盖beanFactory.
					retreats.add(new Retreat(roleId, data.getKey(), retreatBean));
					if (retreats.size() > 50) {
						saveRetreats(retreats);
						retreats.clear();
					}
				}
			}
			return true;
		});
		if (!retreats.isEmpty())
			saveRetreats(retreats);
	}

	private void saveRetreats(@NotNull ArrayList<Retreat> retreats) {
		// 【注意，这里不使用 Task.call or run，因为这个在热更流程中调用，避免去使用hotGuard。】
		// 确认事务可以在更新流程中可以使用。
		// 也许更优化的方法是为这个更新实现一个不是事务的版本。
		providerApp.zeze.newProcedure(() -> {
			for (var r : retreats)
				setLocalBean(r.roleId, r.key, r.bean);
			return 0;
		}, "saveRetreats").call();
	}

	@FunctionalInterface
	public interface TransmitAction {
		/**
		 * @param sender 查询发起者，结果发送给他
		 * @param target 查询目标角色
		 * @return 按普通事务处理过程返回值处理
		 */
		long call(long sender, long target, @Nullable Binary parameter) throws Exception;
	}

	protected static @NotNull Online create(@NotNull AppBase app) {
		return GenModule.createRedirectModule(Online.class, app);
	}

	public static void register(@NotNull Class<? extends Serializable> cls) {
		beanFactory.register(cls);
	}

	public static long getSpecialTypeIdFromBean(@NotNull Serializable bean) {
		return bean.typeId();
	}

	public static @NotNull Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public @NotNull TimerRole getTimerRole() {
		return timerRole;
	}

	public String getOnlineSetName() {
		return multiInstanceName;
	}

	public @NotNull tRoleTimers _tRoleTimers() {
		return _tRoleTimers;
	}

	public @NotNull tRoleOfflineTimers _tRoleOfflineTimers() {
		return _tRoleOfflineTimers;
	}

	// load redirect 使用这个构造函数。
	protected Online(@NotNull AppBase app) {
		super("", app.getZeze().getConfig());
		var zeze = app.getZeze();
		loginEvents = new EventDispatcher(zeze, "Online.Login");
		reloginEvents = new EventDispatcher(zeze, "Online.Relogin");
		logoutEvents = new EventDispatcher(zeze, "Online.Logout");
		localRemoveEvents = new EventDispatcher(zeze, "Online.Local.Remove");
		linkBrokenEvents = new EventDispatcher(zeze, "Online.LinkBroken");

		providerApp = zeze.redirect.providerApp;
		defaultInstance = this;
		RegisterProtocols(providerApp.providerService);
		RegisterZezeTables(providerApp.zeze);
		timerRole = new TimerRole(this);
	}

	// 创建新的OnlineSet使用这个构造函数。独立OnlineSet不装载redirect子类。
	private Online(@NotNull AppBase app, @NotNull String name) {
		super(name, app.getZeze().getConfig());
		var zeze = app.getZeze();
		loginEvents = new EventDispatcher(zeze, "Online.Login");
		reloginEvents = new EventDispatcher(zeze, "Online.Relogin");
		logoutEvents = new EventDispatcher(zeze, "Online.Logout");
		localRemoveEvents = new EventDispatcher(zeze, "Online.Local.Remove");
		linkBrokenEvents = new EventDispatcher(zeze, "Online.LinkBroken");

		providerApp = app.getZeze().redirect.providerApp;
		RegisterZezeTables(providerApp.zeze);
		timerRole = new TimerRole(this);
	}

	/**
	 * 创建新的在线集合，必须在App.Start流程中，紧接着默认Online的创建，马上创建其他的在线集合。
	 * 必须在Application.start()之前调用，以保证这里创建的Table能被open。
	 *
	 * @param name 在线集合名字
	 * @return 返回新建的在线集合实例。返回值可以保存下来。
	 */
	protected @NotNull Online createOnlineSet(@NotNull AppBase app, @NotNull String name) throws Exception {
		if (name.isEmpty())
			throw new IllegalArgumentException("empty name");
		if (this != defaultInstance)
			throw new IllegalStateException("must be called by default online");
		var online = new Online(app, name);
		online.Initialize(app);
		return online;
	}

	public @NotNull ProviderWithOnline getProviderWithOnline() {
		return (ProviderWithOnline)providerApp.providerImplement;
	}

	public @Nullable Online getOnline(@Nullable String name) {
		return getProviderWithOnline().getOnline(name);
	}

	// 优先获取上下文中的Online
	public @NotNull Online getOnlineByContext() {
		var dispatch = ProviderImplement.localDispatch();
		var online = dispatch != null ? getProviderWithOnline().getOnline(dispatch.Argument.getOnlineSetName()) : null;
		return online != null ? online : this;
	}

	public void start() {
		// default online 负责启动所有的online set。
		if (defaultInstance == this) {
			providerApp.builtinModules.put(this.getFullName(), this);
			getProviderWithOnline().foreachOnline(online -> {
				if (online != this)
					online.start();
			});
		}
		startLocalCheck();
		var hotManager = providerApp.zeze.getHotManager();
		if (hotManager != null) {
			hotManager.addHotUpgrade(this);
			hotManager.addHotBeanFactory(this);
			beanFactory.registerWatch(this::tryRecordHotModule);
		}
	}

	private boolean processOffline(Long roleId, @NotNull BLocal local, boolean serverStart) {
		providerApp.zeze.newProcedure(() -> procedureOffline(roleId, local, serverStart), "procedureOffline").call();
		return true; // continue walk
	}

	private long procedureOffline(Long roleId, @NotNull BLocal local, boolean serverStart) throws Exception {
		var onlineShared = getOrAddOnlineShared(roleId);
		var account = onlineShared.getAccount();

		// 本机数据已经过时，马上删除。
		if (local.getLoginVersion() != onlineShared.getLoginVersion()) {
			var ret = removeLocalAndTrigger(roleId, serverStart);
			if (ret != 0) {
				logger.info("processOffline({}): account={}, roleId={}, removeLocalAndTrigger={}",
						multiInstanceName, account, roleId, ret);
				return ret;
			}
		}

		// skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
		var link = onlineShared.getLink();
		// local 里面的Link的State没有意义。
		var linkName = local.getLink().getLinkName();
		var linkSid = local.getLink().getLinkSid();
		if (!link.getLinkName().equals(linkName) || link.getLinkSid() != linkSid) {
			logger.info("processOffline({}): account={}, roleId={}, linkName={}, linkSid={} != linkName={}, linkSid={}",
					multiInstanceName, account, roleId, linkName, linkSid, link.getLinkName(), link.getLinkSid());
			return 0;
		}

		onlineShared.setLink(new BLink(link.getLinkName(), link.getLinkSid(), eLinkBroken));
		// local 在这个流程里面不需要setLink，如果是这个登录的，那么扇面已经删除（removeLocalAndTrigger），否则不需要修改。

		var ret = linkBrokenTrigger(account, roleId);
		// for shorter use
		logger.info("processOffline({}): account={}, roleId={}, linkName={}, linkSid={}, triggerEmbed={}",
				multiInstanceName, account, roleId, linkName, linkSid, ret);

		// see tryLogout
		// 如果玩家在延迟期间建立了新的登录，下面版本号判断会失败。
		if (onlineShared.getLink().getState() != eOffline && assignLogoutVersion(onlineShared)) {
			var ret2 = logoutTrigger(roleId, LogoutReason.LOGOUT);
			logger.info("processOffline: roleId={}, state={}, logoutTrigger={}",
					roleId, onlineShared.getLink().getState(), ret2);
			return ret2;
		}
		logger.info("processOffline: roleId={}, state={}, version.login/logoutVersion={}",
				roleId, onlineShared.getLink().getState(), onlineShared.getLoginVersion());
		return 0;
	}

	/**
	 * 启动过程自动调用。
	 */
	public void startAfter() {
		// default online 负责所有的online set。
		if (defaultInstance == this) {
			getProviderWithOnline().foreachOnline(online -> {
				try {
					online._tlocal.walk((roleId, local) -> processOffline(roleId, local, true));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	/**
	 * 应用在开始停机前主动调用。
	 * 此时应用还是完整的环境。
	 */
	public void stopBefore() {
		// default online 负责所有的online set。
		if (defaultInstance == this) {
			providerApp.providerService.setDisableChoiceFromLinks(true);
			getProviderWithOnline().foreachOnline(online -> {
				try {
					online._tlocal.walk((roleId, local) -> processOffline(roleId, local, false));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	public void stop() {
		var hotManager = providerApp.zeze.getHotManager();
		if (hotManager != null) {
			hotManager.removeHotUpgrade(this);
			hotManager.removeHotBeanFactory(this);
			beanFactory.unregisterWatch(this::tryRecordHotModule);
		}

		// default online 负责停止所有的online set。
		if (defaultInstance == this) {
			getProviderWithOnline().foreachOnline(online -> {
				if (online != this)
					online.stop();
			});
			defaultInstance = null;
		}
		if (verifyLocalTimer != null)
			verifyLocalTimer.cancel(false);
	}

	@Override
	public void UnRegister() {
		UnRegisterProtocols(providerApp.providerService);
		UnRegisterZezeTables(providerApp.zeze);
	}

	public @NotNull tlocal getTLocal() {
		return _tlocal;
	}

	public @NotNull tOnline getTOnline() {
		return _tOnline;
	}

	public @NotNull tOnlineShared getTOnlineShared() {
		return _tOnlineShared;
	}

	@SuppressWarnings("unchecked")
	public <T extends Bean> @Nullable T getUserData(long roleId) {
		var online = getOnline(roleId);
		return online != null ? (T)online.getUserData().getBean() : null;
	}

	@SuppressWarnings("unchecked")
	public <T extends Bean> @Nullable T getUserDataShared(long roleId) {
		var onlineShared = getOnlineShared(roleId);
		return onlineShared != null ? (T)onlineShared.getUserData().getBean() : null;
	}

	public @Nullable BOnline getOnline(long roleId) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			return _tOnline.get(roleId);
		return _tOnline.selectDirty(roleId);
	}

	public boolean isOnline(long roleId) {
		var onlineShared = getOnlineShared(roleId);
		return null != onlineShared && onlineShared.getLink().getState() == eLogined;
	}

	public @Nullable BOnlineShared getOnlineShared(long roleId) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			return _tOnlineShared.get(roleId);
		return _tOnlineShared.selectDirty(roleId);
	}

	public @NotNull BOnline getOrAddOnline(long roleId) {
		return _tOnline.getOrAdd(roleId);
	}

	public @NotNull BOnlineShared getOrAddOnlineShared(long roleId) {
		return _tOnlineShared.getOrAdd(roleId);
	}

	public @Nullable BOnline getLoginOnline(long roleId) {
		var onlineShared = getOnlineShared(roleId);
		return onlineShared != null && onlineShared.getLink().getState() == eLogined ? getOnline(roleId) : null;
	}

	public @Nullable BOnlineShared getLoginOnlineShared(long roleId) {
		var onlineShared = getOnlineShared(roleId);
		return onlineShared != null && onlineShared.getLink().getState() == eLogined ? onlineShared : null;
	}

	public int getState(long roleId) {
		var onlineShared = getOnlineShared(roleId);
		return onlineShared != null ? onlineShared.getLink().getState() : eOffline;
	}

	public <T extends Bean> void setUserData(long roleId, @NotNull T data) {
		var cl = data.getClass().getClassLoader();
		if (HotManager.isHotModule(cl)) {
			var hotModule = (HotModule)cl;
			Transaction.whileCommit(() -> {
				hotModule.stopEvents.add(this::onHotModuleStop);
				hotModulesHaveDynamic.add(hotModule);
			});
		}
		getOrAddOnline(roleId).getUserData().setBean(data);
	}

	public int getLocalCount() {
		return verifyLocalCount.get();
	}

	public int getLocalDatabaseSize() {
		return (int)_tlocal.getDatabaseSize();
	}

	public int getLocalDatabaseSizeApproximation() {
		return (int)_tlocal.getDatabaseSizeApproximation();
	}

	public long walkLocal(@NotNull TableWalkHandle<Long, BLocal> walker) throws Exception {
		return _tlocal.walk(walker);
	}

	public long getLoginTimes() {
		return loginTimes.get();
	}

	// 登录事件
	public @NotNull EventDispatcher getLoginEvents() {
		return loginEvents;
	}

	public @NotNull EventDispatcher getLinkBrokenEvents() {
		return linkBrokenEvents;
	}

	// 断线重连事件
	public @NotNull EventDispatcher getReloginEvents() {
		return reloginEvents;
	}

	// 登出事件
	public @NotNull EventDispatcher getLogoutEvents() {
		return logoutEvents;
	}

	// 角色从本机删除事件
	public @NotNull EventDispatcher getLocalRemoveEvents() {
		return localRemoveEvents;
	}

	public @NotNull ConcurrentHashMap<String, TransmitAction> getTransmitActions() {
		return transmitActions;
	}

	private BLocal getLoginLocal(long roleId) {
		var bLocal = _tlocal.get(roleId);
		if (bLocal == null || bLocal.getLoginVersion() != getOrAddOnlineShared(roleId).getLoginVersion())
			throw new IllegalStateException("roleId not online or invalid version. " + roleId);
		return bLocal;
	}

	private final ConcurrentHashMap<Long, Long> localActiveTimes = new ConcurrentHashMap<>();

	private void putLocalActiveTime(long roleId) {
		localActiveTimes.put(roleId, System.currentTimeMillis());
	}

	private void removeLocalActiveTime(long roleId) {
		localActiveTimes.remove(roleId);
	}

	public void setLocalActiveTimeIfPresent(long roleId) {
		localActiveTimes.computeIfPresent(roleId, (k, v) -> System.currentTimeMillis());
	}

	public <T extends Bean> void setLocalBean(long roleId, @NotNull String key, @NotNull T bean) {
		var bLocal = getLoginLocal(roleId);
		beanFactory.register(bean);
		var bAny = new BAny();
		bAny.getAny().setBean(bean);
		bLocal.getDatas().put(key, bAny);
		if (HotManager.isHotModule(bean.getClass().getClassLoader())) {
			var hotModule = (HotModule)bean.getClass().getClassLoader();
			Transaction.whileCommit(() -> {
				hotModule.stopEvents.add(this::onHotModuleStop);
				hotModulesHaveLocal.add(hotModule);
			});
		}
	}

	public void removeLocalBean(long roleId, @NotNull String key) {
		var bLocal = _tlocal.get(roleId);
		if (bLocal != null)
			bLocal.getDatas().remove(key);
	}

	@SuppressWarnings("unchecked")
	public <T extends Bean> @Nullable T getLocalBean(long roleId, @NotNull String key) {
		var bLocal = _tlocal.get(roleId);
		if (bLocal == null)
			return null;
		var data = bLocal.getDatas().get(key);
		if (data == null)
			return null;
		return (T)data.getAny().getBean();
	}

	@SuppressWarnings("unchecked")
	public <T extends Bean> @NotNull T getOrAddLocalBean(long roleId, @NotNull String key, @NotNull T defaultHint) {
		var bLocal = getLoginLocal(roleId);
		var data = bLocal.getDatas().getOrAdd(key);
		if (data.getAny().getBean().typeId() == defaultHint.typeId())
			return (T)data.getAny().getBean();
		data.getAny().setBean(defaultHint);
		if (HotManager.isHotModule(defaultHint.getClass().getClassLoader())) {
			var hotModule = (HotModule)defaultHint.getClass().getClassLoader();
			Transaction.whileCommit(() -> {
				hotModule.stopEvents.add(this::onHotModuleStop);
				hotModulesHaveLocal.add(hotModule);
			});
		}
		return defaultHint;
	}

	private long removeLocalAndTrigger(long roleId) throws Exception {
		return removeLocalAndTrigger(roleId, false);
	}

	private long removeLocalAndTrigger(long roleId, boolean notVerifyCount) throws Exception {
		var arg = new LocalRemoveEventArgument(roleId, _tlocal.get(roleId));
		// local 没有数据不触发事件？
		if (arg.local != null) {
			_tlocal.remove(roleId); // remove first
			if (!notVerifyCount)
				Transaction.whileCommit(verifyLocalCount::decrementAndGet);
			Transaction.whileCommit(() -> removeLocalActiveTime(roleId));

			var ret = localRemoveEvents.triggerEmbed(this, arg);
			if (ret != 0)
				return ret;
			localRemoveEvents.triggerProcedure(providerApp.zeze, this, arg);
			Transaction.whileCommit(() -> localRemoveEvents.triggerThread(this, arg));
		}
		return 0;
	}

	/**
	 * 返回登出版本号，
	 *
	 * @param roleId roleId
	 * @return null if not offline
	 */
	public @Nullable Long getLogoutVersion(long roleId) {
		var onlineShared = getOnlineShared(roleId);
		if (onlineShared == null)
			return 0L;
		if (onlineShared.getLink().getState() != eOffline)
			return null;
		return onlineShared.getLogoutVersion();
	}

	/**
	 * 返回登录版本号。
	 *
	 * @param roleId roleId
	 * @return null if not login
	 */
	public @Nullable Long getLoginVersion(long roleId) {
		var onlineShared = getLoginOnlineShared(roleId);
		return onlineShared != null ? onlineShared.getLoginVersion() : null;
	}

	/**
	 * 返回本进程局部登录数据里面的版本号。
	 *
	 * @param roleId roleId
	 * @return null if not found in local
	 */
	public @Nullable Long getLocalLoginVersion(long roleId) {
		var local = _tlocal.get(roleId);
		return local != null ? local.getLoginVersion() : null;
	}

	public boolean isLogin(long roleId) {
		return getLoginOnline(roleId) != null;
	}

	private static boolean assignLogoutVersion(@NotNull BOnlineShared online) {
		if (online.getLoginVersion() == online.getLogoutVersion())
			return false;
		online.setLogoutVersion(online.getLoginVersion());
		return true;
	}

	private long logoutTrigger(long roleId, @NotNull LogoutReason logoutReason) throws Exception {
		var onlineShared = getOrAddOnlineShared(roleId);
		var link = onlineShared.getLink();
		onlineShared.setLink(new BLink(link.getLinkName(), link.getLinkSid(), eOffline));
		// local 在这个流程里面不需要setLink，如果是这个登录的，那么扇面已经删除（removeLocalAndTrigger），否则不需要修改。

		// 总是尝试通知上一次登录的服务器，里面会忽略本机。
		tryRedirectRemoveLocal(multiInstanceName, getOrAddOnline(roleId).getServerId(), roleId);
		// 总是删除
		removeLocalAndTrigger(roleId);

		var arg = new LogoutEventArgument(this, roleId, logoutReason);
		var ret = logoutEvents.triggerEmbed(this, arg);
		if (ret != 0)
			return ret;
		logoutEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> logoutEvents.triggerThread(this, arg));
		return 0;
	}

	private long linkBrokenTrigger(@SuppressWarnings("unused") @NotNull String account, long roleId) throws Exception {
		var arg = new LinkBrokenArgument(roleId);
		// 由于account可能没有，这里就不传递这个参数了。
		var ret = linkBrokenEvents.triggerEmbed(this, arg);
		if (ret != 0)
			return ret;
		linkBrokenEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> linkBrokenEvents.triggerThread(this, arg));
		return 0;
	}

	private long loginTrigger(@NotNull String account, long roleId) throws Exception {
		loginTimes.incrementAndGet();
		var arg = new LoginArgument(this, account, roleId);
		var ret = loginEvents.triggerEmbed(this, arg);
		if (ret != 0)
			return ret;
		loginEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> loginEvents.triggerThread(this, arg));
		return 0;
	}

	private long reloginTrigger(@NotNull String account, long roleId) throws Exception {
		loginTimes.incrementAndGet();
		var arg = new LoginArgument(this, account, roleId);
		var ret = reloginEvents.triggerEmbed(this, arg);
		if (ret != 0)
			return ret;
		reloginEvents.triggerProcedure(providerApp.zeze, this, arg);
		Transaction.whileCommit(() -> reloginEvents.triggerThread(this, arg));
		return 0;
	}

	private long tryLogout(@NotNull BDelayLogoutCustom custom) throws Exception {
		var roleId = custom.getRoleId();
		var currentLoginVersion = custom.getLoginVersion();
		// local online 独立判断version分别尝试删除。
		var local = _tlocal.get(roleId);
		if (local != null && local.getLoginVersion() == currentLoginVersion) {
			var ret = removeLocalAndTrigger(roleId);
			logger.info("tryLogout: roleId={}, currentLoginVersion={}, removeLocalAndTrigger={}",
					roleId, currentLoginVersion, ret);
			if (ret != 0)
				return ret;
		}
		// 如果玩家在延迟期间建立了新的登录，下面版本号判断会失败。
		var onlineShared = getOnlineShared(roleId);
		var logoutVersion = onlineShared != null ? onlineShared.getLogoutVersion() : -1;
		if (onlineShared != null && onlineShared.getLink().getState() != eOffline
				&& onlineShared.getLoginVersion() == currentLoginVersion
				&& assignLogoutVersion(onlineShared)) {
			var ret = logoutTrigger(roleId, LogoutReason.LOGOUT);
			logger.info("tryLogout: roleId={}, state={}, currentLoginVersion={}, logoutVersion={}, logoutTrigger={}",
					roleId, onlineShared.getLink().getState(), currentLoginVersion, logoutVersion, ret);
			if (ret != 0)
				return ret;
		} else {
			logger.info("tryLogout: roleId={}, state={}, version.login/logoutVersion={}/{}, currentLoginVersion={}",
					roleId, onlineShared != null ? onlineShared.getLink().getState() : -1,
					onlineShared != null ? onlineShared.getLoginVersion() : -1, logoutVersion, currentLoginVersion);
		}
		return Procedure.Success;
	}

	public static class DelayLogout implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			logout((BDelayLogoutCustom)context.customData);
		}

		public static void logout(BDelayLogoutCustom custom) throws Exception {
			if (defaultInstance != null) {
				// 这里虽然调用instanceDefaultOnline，但里面执行会根据context里面OnlineSetName访问的不同的Online数据
				// 也许这里改成 getOnlineSet(name).tryLogout，tryLogout 就直接访问自身数据比较好。
				// 能工作，先这样了。
				var onlineSet = defaultInstance.getOnline(custom.getOnlineSetName());
				if (onlineSet != null) {
					var ret = onlineSet.tryLogout(custom);
					logger.log(ret == 0 ? Level.INFO : Level.ERROR,
							"DelayLogout({}): roleId={}, loginVersion={}, tryLogout={}",
							custom.getOnlineSetName(), custom.getRoleId(), custom.getLoginVersion(), ret);
				} else {
					logger.log(Level.ERROR, "DelayLogout({}): roleId={}, loginVersion={}, not found OnlineSetName",
							custom.getOnlineSetName(), custom.getRoleId(), custom.getLoginVersion());
				}
			}
		}
	}

	public long sendError(@NotNull String account, long roleId,
						  @NotNull String linkName, long linkSid) throws Exception {
		var onlineShared = getOrAddOnlineShared(roleId);

		var local = _tlocal.get(roleId);
		if (local != null && local.getLoginVersion() != onlineShared.getLoginVersion()) { // 本机数据已经过时，马上删除。
			var ret = removeLocalAndTrigger(roleId);
			if (ret != 0) {
				logger.info("sendError({}): account={}, roleId={}, linkName={}, linkSid={}, removeLocalAndTrigger={}",
						multiInstanceName, account, roleId, linkName, linkSid, ret);
				return ret;
			}
		}

		// skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
		var link = onlineShared.getLink();
		if (!link.getLinkName().equals(linkName) || link.getLinkSid() != linkSid) {
			logger.info("sendError({}): account={}, roleId={}, linkName={}, linkSid={} != linkName={}, linkSid={}",
					multiInstanceName, account, roleId, linkName, linkSid, link.getLinkName(), link.getLinkSid());
			return 0;
		}

		onlineShared.setLink(new BLink(link.getLinkName(), link.getLinkSid(), eLinkBroken));
		// local 在这个流程里面不需要setLink，如果是这个登录的，那么扇面已经删除（removeLocalAndTrigger），否则不需要修改。

		var ret = linkBrokenTrigger(account, roleId);
		// for shorter use
		logger.info("sendError({}): account={}, roleId={}, linkName={}, linkSid={}, triggerEmbed={}",
				multiInstanceName, account, roleId, linkName, linkSid, ret);

		// see tryLogout
		// 如果玩家在延迟期间建立了新的登录，下面版本号判断会失败。
		if (onlineShared.getLink().getState() != eOffline && assignLogoutVersion(onlineShared)) {
			var ret2 = logoutTrigger(roleId, LogoutReason.LOGOUT);
			logger.info("sendError: roleId={}, state={}, logoutTrigger={}",
					roleId, onlineShared.getLink().getState(), ret2);
			return ret2;
		}
		logger.info("sendError: roleId={}, state={}, version.login/logoutVersion={}",
				roleId, onlineShared.getLink().getState(), onlineShared.getLoginVersion());
		return 0;
	}

	public long linkBroken(@NotNull String account, long roleId,
						   @NotNull String linkName, long linkSid) throws Exception {
		var onlineShared = getOrAddOnlineShared(roleId);

		var local = _tlocal.get(roleId);
		if (local == null) {
			logger.info("linkBroken({}): account={}, roleId={}, linkName={}, linkSid={}, roleId not found in tlocal",
					multiInstanceName, account, roleId, linkName, linkSid);
			return 0; // 不在本机登录。
		}

		// 本机数据已经过时，马上删除。
		if (local.getLoginVersion() != onlineShared.getLoginVersion()) {
			var ret = removeLocalAndTrigger(roleId);
			if (ret != 0) {
				logger.info("linkBroken({}): account={}, roleId={}, linkName={}, linkSid={}, removeLocalAndTrigger={}",
						multiInstanceName, account, roleId, linkName, linkSid, ret);
				return ret;
			}
		}

		// skip not owner: 仅仅检查LinkSid是不充分的。后面继续检查LoginVersion。
		var link = onlineShared.getLink();
		if (!link.getLinkName().equals(linkName) || link.getLinkSid() != linkSid) {
			logger.info("linkBroken({}): account={}, roleId={}, linkName={}, linkSid={} != linkName={}, linkSid={}",
					multiInstanceName, account, roleId, linkName, linkSid, link.getLinkName(), link.getLinkSid());
			return 0;
		}

		onlineShared.setLink(new BLink(link.getLinkName(), link.getLinkSid(), eLinkBroken));
		// local 在这个流程里面不需要setLink，如果是这个登录的，那么扇面已经删除（removeLocalAndTrigger），否则不需要修改。

		var ret = linkBrokenTrigger(account, roleId);
		// for shorter use
		var zeze = providerApp.zeze;
		var delay = zeze.getConfig().getOnlineLogoutDelay();
		logger.info("linkBroken({}): account={}, roleId={}, linkName={}, linkSid={}, triggerEmbed={}, delay={}",
				multiInstanceName, account, roleId, linkName, linkSid, ret, delay);
		zeze.getTimer().schedule(delay, DelayLogout.class, new BDelayLogoutCustom(roleId, onlineShared.getLoginVersion(),
				multiInstanceName));
		return 0;
	}

	// 优先在上下文中的Online上发送
	public void send(long roleId, @NotNull Protocol<?> p) {
		getOnlineByContext().sendOnline(roleId, p);
	}

	// 在指定Online上发送
	public void sendOnline(long roleId, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", roleId, multiInstanceName, p);
		sendDirect(roleId, typeId, new Binary(p.encode()), false);
	}

	// 优先在上下文中的Online上发送
	public <A extends Serializable, R extends Serializable> void sendRpc(
			long roleId, @NotNull Rpc<A, R> rpc, ProtocolHandle<Rpc<A, R>> responseHandle) {
		getOnlineByContext().sendOnlineRpc(roleId, rpc, responseHandle);
	}

	// 优先在上下文中的Online上发送
	public <A extends Serializable, R extends Serializable> void sendRpc(
			long roleId, @NotNull Rpc<A, R> rpc, ProtocolHandle<Rpc<A, R>> responseHandle, int timeoutMs) {
		getOnlineByContext().sendOnlineRpc(roleId, rpc, responseHandle, timeoutMs);
	}

	// 在指定Online上发送
	public <A extends Serializable, R extends Serializable> void sendOnlineRpc(
			long roleId, @NotNull Rpc<A, R> rpc, ProtocolHandle<Rpc<A, R>> responseHandle) {
		sendOnlineRpc(roleId, rpc, responseHandle, 5000);
	}

	// 在指定Online上发送
	public <A extends Serializable, R extends Serializable> boolean sendOnlineRpc(
			long roleId, @NotNull Rpc<A, R> rpc, ProtocolHandle<Rpc<A, R>> responseHandle, int timeoutMs) {
		var service = providerApp.providerService;
		// try remove. 只维护一个上下文。多次发送相同rpc会如此，这个应该最好报错。沿用老的逻辑吧。see Rpc.Send
		service.removeRpcContext(rpc.getSessionId(), rpc);
		rpc.setResponseHandle(responseHandle);
		var sessionId = service.addRpcContext(rpc);
		rpc.setSessionId(sessionId);
		rpc.setTimeout(timeoutMs);
		rpc.setIsTimeout(false);
		rpc.setRequest(true);
		rpc.schedule(service, sessionId, timeoutMs);
		return sendDirect(roleId, rpc.getTypeId(), new Binary(rpc.encode()), false);
	}

	// 在指定Online上同步发送
	public <A extends Serializable, R extends Serializable> TaskCompletionSource<R> sendOnlineRpcForWait(
			long roleId, @NotNull Rpc<A, R> rpc) {
		return sendOnlineRpcForWait(roleId, rpc, 5000);
	}

	// 在指定Online上同步发送
	public <A extends Serializable, R extends Serializable> TaskCompletionSource<R> sendOnlineRpcForWait(
			long roleId, @NotNull Rpc<A, R> rpc, int timeoutMs) {
		var future = new TaskCompletionSource<R>();
		rpc.setFuture(future);
		if (!sendOnlineRpc(roleId, rpc, null, timeoutMs))
			future.setException(new IllegalStateException("sendOnlineRpc fail."));
		return future;
	}

	// 尝试给所有Onlines发送,可在任意Online上执行
	public void sendAllOnlines(long roleId, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", roleId, "*", p);
		var data = new Binary(p.encode());
		getProviderWithOnline().foreachOnline(online -> online.sendDirect(roleId, typeId, data, true));
	}

	// 优先在上下文中的Online上发送
	public void send(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		getOnlineByContext().sendOnline(roleIds, p);
	}

	// 在指定Online上发送
	public void sendOnline(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");

		if (roleIds.isEmpty())
			return;
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var sb = new StringBuilder();
			for (var roleId : roleIds)
				sb.append(roleId).append(',');
			int n = sb.length();
			if (n > 0)
				sb.setLength(n - 1);
			if (!multiInstanceName.isEmpty())
				sb.append('@').append(multiInstanceName);
			var idsStr = sb.toString();
			AsyncSocket.log("Send", idsStr, p);
		}
		sendOnline(roleIds, typeId, new Binary(p.encode()));
	}

	// 尝试给所有Onlines发送,可在任意Online上执行
	public void sendAllOnlines(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");

		if (roleIds.isEmpty())
			return;
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId)) {
			var sb = new StringBuilder();
			for (var roleId : roleIds)
				sb.append(roleId).append(',');
			int n = sb.length();
			if (n > 0)
				sb.setLength(n - 1);
			var idsStr = sb.append("@*").toString();
			AsyncSocket.log("Send", idsStr, p);
		}
		sendAllOnlines(roleIds, typeId, new Binary(p.encode()));
	}

	// 优先在上下文中的Online上发送
	public void sendWhileCommit(long roleId, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileCommit(() -> send(roleId, p));
	}

	// 在指定Online上发送
	public void sendWhileCommitOnline(long roleId, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileCommit(() -> sendOnline(roleId, p));
	}

	// 尝试给所有Onlines发送,可在任意Online上执行
	public void sendWhileCommitAllOnlines(long roleId, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileCommit(() -> sendAllOnlines(roleId, p));
	}

	// 优先在上下文中的Online上发送
	public void sendWhileCommit(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileCommit(() -> send(roleIds, p));
	}

	// 在指定Online上发送
	public void sendWhileCommitOnline(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileCommit(() -> sendOnline(roleIds, p));
	}

	// 尝试给所有Onlines发送,可在任意Online上执行
	public void sendWhileCommitAllOnlines(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileCommit(() -> sendAllOnlines(roleIds, p));
	}

	// 优先在上下文中的Online上发送
	public void sendWhileRollback(long roleId, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileRollback(() -> send(roleId, p));
	}

	// 在指定Online上发送
	public void sendWhileRollbackOnline(long roleId, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileRollback(() -> sendOnline(roleId, p));
	}

	// 尝试给所有Onlines发送,可在任意Online上执行
	public void sendWhileRollbackAllOnlines(long roleId, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileRollback(() -> sendAllOnlines(roleId, p));
	}

	// 优先在上下文中的Online上发送
	public void sendWhileRollback(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileRollback(() -> send(roleIds, p));
	}

	// 在指定Online上发送
	public void sendWhileRollbackOnline(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileRollback(() -> sendOnline(roleIds, p));
	}

	// 尝试给所有Onlines发送,可在任意Online上执行
	public void sendWhileRollbackAllOnlines(@NotNull Collection<Long> roleIds, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileRollback(() -> sendAllOnlines(roleIds, p));
	}

//	public void send(long roleId, long typeId, Binary fullEncodedProtocol) {
//		// 发送协议请求在另外的事务中执行。
//		providerApp.zeze.getTaskOneByOneByKey().Execute(roleId,
//				() -> Task.call(providerApp.zeze.newProcedure(() -> {
//					sendEmbed(List.of(roleId), typeId, fullEncodedProtocol);
//					return Procedure.Success;
//				}, "Online.send")), DispatchMode.Normal);
//	}

	// 优先在上下文中的Online上发送
	public int send(@NotNull Collection<Long> roleIds, long typeId, @NotNull Binary fullEncodedProtocol,
					boolean trySend) {
		return getOnlineByContext().sendOnline(roleIds, typeId, fullEncodedProtocol, trySend);
	}

	// 在指定Online上发送
	public int sendOnline(@NotNull Collection<Long> roleIds, long typeId, @NotNull Binary fullEncodedProtocol,
						  boolean trySend) {
		int roleCount = roleIds.size();
		if (roleCount == 1) {
			var it = roleIds.iterator();
			if (it.hasNext()) // 不确定roleIds是否稳定,所以还是判断一下保险
				return sendDirect(it.next(), typeId, fullEncodedProtocol, trySend) ? 1 : 0;
		} else if (roleCount > 1) {
			return sendDirect(roleIds, typeId, fullEncodedProtocol, trySend);
//			providerApp.zeze.getTaskOneByOneByKey().executeCyclicBarrier(roleIds, providerApp.zeze.newProcedure(() -> {
//				sendEmbed(roleIds, typeId, fullEncodedProtocol);
//				return Procedure.Success;
//			}, "Online.send"), null, DispatchMode.Normal);
		}
		return 0;
	}

	// 优先在上下文中的Online上发送
	public int send(@NotNull Collection<Long> roleIds, long typeId, @NotNull Binary fullEncodedProtocol) {
		return getOnlineByContext().sendOnline(roleIds, typeId, fullEncodedProtocol);
	}

	// 在指定Online上发送
	public int sendOnline(@NotNull Collection<Long> roleIds, long typeId, @NotNull Binary fullEncodedProtocol) {
		return sendOnline(roleIds, typeId, fullEncodedProtocol, false);
	}

	// 尝试给所有Onlines发送,可在任意Online上执行
	public void sendAllOnlines(@NotNull Collection<Long> roleIds, long typeId, @NotNull Binary fullEncodedProtocol) {
		getProviderWithOnline().foreachOnline(online -> online.sendOnline(roleIds, typeId, fullEncodedProtocol, true));
	}

//	public void sendNoBarrier(Iterable<Long> roleIds, long typeId, Binary fullEncodedProtocol) {
//		// 发送协议请求在另外的事务中执行。
//		Task.run(providerApp.zeze.newProcedure(() -> {
//			sendEmbed(roleIds, typeId, fullEncodedProtocol);
//			return Procedure.Success;
//		}, "Online.send"), null, null, DispatchMode.Normal);
//	}

	private long triggerLinkBroken(@NotNull String linkName, @NotNull LongList errorSids,
								   @NotNull Map<Long, Long> context) {
		errorSids.foreach(linkSid -> providerApp.zeze.newProcedure(() -> {
			var roleId = context.get(linkSid);
			// 补发的linkBroken没有account上下文。
			return roleId != null ? sendError("", roleId, linkName, linkSid) : 0;
		}, "Online.triggerLinkBroken").call());
		return 0;
	}

	/**
	 * 直接通过Link链接发送Send协议。
	 */
	public boolean send(@NotNull AsyncSocket link, @NotNull Map<Long, Long> contexts, @NotNull Send send) {
		return send.Send(link, rpc -> triggerLinkBroken(ProviderService.getLinkName(link),
				send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
	}

	// 直接通过 linkName, linkSid 发送协议。
	public boolean send(@NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
		return send(null, linkName, linkSid, p);
	}

	public boolean send(@Nullable Long roleId, @NotNull String linkName, long linkSid, @NotNull Protocol<?> p) {
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
			AsyncSocket.log("Send", roleId != null ? roleId : 0, p);
		var send = new Send(new BSend(p.getTypeId(), new Binary(p.encode())));
		send.Argument.getLinkSids().add(linkSid);
		return send(link, roleId != null ? Map.of(linkSid, roleId) : Map.of(), send);
	}

	//	public void send(Collection<Long> keys, AsyncSocket to, Map<Long, Long> contexts, Send send) {
//		if (keys.size() > 1) {
//			send.Send(to, rpc -> triggerLinkBroken(ProviderService.getLinkName(to),
//					send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
//		} else {
//			//noinspection CodeBlock2Expr
//			providerApp.zeze.getTaskOneByOneByKey().executeCyclicBarrier(keys, "sendOneByOne", () -> {
//				send.Send(to, rpc -> triggerLinkBroken(ProviderService.getLinkName(to),
//						send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids(), contexts));
//			}, null, DispatchMode.Normal);
//		}
//	}

//	public void sendEmbed(Iterable<Long> roleIds, long typeId, Binary fullEncodedProtocol) {
//		var groups = groupByLink(roleIds);
//		Transaction.whileCommit(() -> {
//			for (var group : groups) {
//				if (group.linkSocket == null)
//					continue; // skip not online
//
//				var send = new Send(new Zeze.Arch.Beans.BSend(typeId, fullEncodedProtocol));
//				send.Argument.getLinkSids().addAll(group.roles.values());
//				send(group.linkSocket, group.contexts, send);
//			}
//		});
//	}

	public static final class LinkRoles {
		final @NotNull String linkName;
		final @Nullable AsyncSocket linkSocket;
		final @NotNull Send send;
		final LongList roleIds = new LongList();

		public LinkRoles(@NotNull String linkName, @Nullable AsyncSocket linkSocket,
						 long typeId, @NotNull Binary fullEncodedProtocol) {
			this.linkName = linkName;
			this.linkSocket = linkSocket;
			send = new Send(new BSend(typeId, fullEncodedProtocol));
		}
	}

	private static @Nullable AsyncSocket getLinkSocket(ConcurrentHashMap<String, Connector> links,
													   String linkName, long roleId) {
		var connector = links.get(linkName);
		if (connector == null) {
			logger.warn("getLinkSocket: not found connector for linkName={} roleId={}", linkName, roleId);
			return null;
		}
		if (!connector.isHandshakeDone()) {
			logger.warn("getLinkSocket: not isHandshakeDone for linkName={} roleId={}", linkName, roleId);
			return null;
		}
		var linkSocket = connector.getSocket();
		if (linkSocket == null) {
			logger.warn("getLinkSocket: closed connector for linkName={} roleId={}", linkName, roleId);
			return null;
		}
		return linkSocket;
	}

	private void processErrorSids(LongList errorSids, LinkRoles group) {
		errorSids.foreach(linkSid -> Task.run(providerApp.zeze.newProcedure(() -> {
			int idx = group.send.Argument.getLinkSids().indexOf(linkSid);
			// 补发的linkBroken没有account上下文
			return idx >= 0 ? sendError("", group.roleIds.get(idx), group.linkName, linkSid) : 0;
		}, "Online.triggerLinkBroken2")));
	}

	private static long getTypeId(@NotNull Binary fullEncodedProtocol) {
		int moduleId = ByteBuffer.ToInt(fullEncodedProtocol.bytesUnsafe(), 0);
		int protocolId = ByteBuffer.ToInt(fullEncodedProtocol.bytesUnsafe(), 4);
		return Protocol.makeTypeId(moduleId, protocolId);
	}

	// 可在事务外执行
	public int sendDirect(@NotNull Iterable<Long> roleIds, long typeId, @NotNull Binary fullEncodedProtocol,
						  boolean trySend) {
		var roleIdSet = new LongHashSet();
		for (var roleId : roleIds)
			roleIdSet.add(roleId); // 去重
		if (roleIdSet.isEmpty())
			return 0;
		var groups = new HashMap<String, LinkRoles>();
		var links = providerApp.providerService.getLinks();
		for (var it = roleIdSet.iterator(); it.moveToNext(); ) {
			var roleId = it.value();
			var onlineShared = _tOnlineShared.selectDirty(roleId);
			if (onlineShared == null) {
				if (!trySend) {
					logger.info("sendDirects({}): not found roleId={} in _tonline",
							getTypeId(fullEncodedProtocol), roleId);
				}
				continue;
			}
			var link = onlineShared.getLink();
			var state = link.getState();
			if (state != eLogined) {
				if (!trySend) {
					logger.info("sendDirects({}): state={} != eLogined for roleId={}",
							getTypeId(fullEncodedProtocol), state, roleId);
				}
				continue;
			}
			var linkName = link.getLinkName();
			// 后面保存connector.socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(linkName);
			if (group == null) {
				var linkSocket = getLinkSocket(links, linkName, roleId); // maybe null
				groups.put(linkName, group = new LinkRoles(linkName, linkSocket, typeId, fullEncodedProtocol));
			}
			group.send.Argument.getLinkSids().add(link.getLinkSid());
			group.roleIds.add(roleId);
		}

		int sendCount = 0;
		for (var group : groups.values()) {
			if (group.linkSocket == null) {
				processErrorSids(group.send.Argument.getLinkSids(), group);
				continue; // link miss process done
			}

			group.roleIds.foreach(this::setLocalActiveTimeIfPresent);
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
	public boolean sendDirect(long roleId, long typeId, @NotNull Binary fullEncodedProtocol, boolean trySend) {
		var onlineShared = _tOnlineShared.selectDirty(roleId);
		if (onlineShared == null) {
			if (!trySend) {
				logger.info("sendDirect({}): not found roleId={} in _tonline",
						getTypeId(fullEncodedProtocol), roleId);
			}
			return false;
		}
		var link = onlineShared.getLink();
		var state = link.getState();
		if (state != eLogined) {
			if (!trySend) {
				logger.info("sendDirect({}): state={} != eLogined for roleId={}",
						getTypeId(fullEncodedProtocol), state, roleId);
			}
			return false;
		}
		var linkName = link.getLinkName();
		var connector = providerApp.providerService.getLinks().get(linkName);
		if (connector == null) {
			logger.warn("sendDirect({}): not found connector for linkName={} roleId={}",
					getTypeId(fullEncodedProtocol), linkName, roleId);
			// link miss
			Task.run(providerApp.zeze.newProcedure(() -> sendError("", roleId, linkName, link.getLinkSid()),
					"Online.triggerLinkBroken0_a"));
			return false;
		}
		if (!connector.isHandshakeDone()) {
			logger.warn("sendDirect({}): not isHandshakeDone for linkName={} roleId={}",
					getTypeId(fullEncodedProtocol), linkName, roleId);
			// link miss
			Task.run(providerApp.zeze.newProcedure(() -> sendError("", roleId, linkName, link.getLinkSid()),
					"Online.triggerLinkBroken0_b"));
			return false;
		}
		// 后面保存connector.socket并使用，如果之后连接被关闭，以后发送协议失败。
		var linkSocket = connector.getSocket();
		if (linkSocket == null) {
			logger.warn("sendDirect({}): closed connector for linkName={} roleId={}",
					getTypeId(fullEncodedProtocol), linkName, roleId);
			// link miss
			Task.run(providerApp.zeze.newProcedure(() -> sendError("", roleId, linkName, link.getLinkSid()),
					"Online.triggerLinkBroken0_c"));
			return false;
		}
		var send = new Send(new BSend(typeId, fullEncodedProtocol));
		send.Argument.getLinkSids().add(link.getLinkSid());
		setLocalActiveTimeIfPresent(roleId);
		return send.Send(linkSocket, rpc -> {
			if (send.isTimeout() || !send.Result.getErrorLinkSids().isEmpty()) {
				var linkSid = send.Argument.getLinkSids().get(0);
				// 补发的linkBroken没有account上下文
				providerApp.zeze.newProcedure(() -> sendError("", roleId, linkName, linkSid),
						"Online.triggerLinkBroken1").call();
			}
			return Procedure.Success;
		});
	}

//	public static final class RoleOnLink {
//		String linkName = "";
//		AsyncSocket linkSocket;
//		// long providerSessionId;
//		final HashMap<Long, Long> roles = new HashMap<>(); // roleid -> linksid
//		final HashMap<Long, Long> contexts = new HashMap<>(); // linksid -> roleid
//	}
//
//	public Collection<RoleOnLink> groupByLink(Iterable<Long> roleIds) {
//		var groups = new HashMap<String, RoleOnLink>();
//		var groupNotOnline = new RoleOnLink(); // LinkName is Empty And Socket is null.
//		groups.put(groupNotOnline.linkName, groupNotOnline);
//
//		for (var roleId : roleIds) {
//			var online = _tonline.get(roleId);
//			if (online == null) {
//				groupNotOnline.roles.putIfAbsent(roleId, null);
//				logger.info("groupByLink: not found roleId={} in _tonline", roleId);
//				continue;
//			}
//
//			var linkName = online.getLink().getLinkName();
//			var connector = providerApp.providerService.getLinks().get(linkName);
//			if (connector == null) {
//				logger.warn("groupByLink: not found connector for linkName={} roleId={}", linkName, roleId);
//				groupNotOnline.roles.putIfAbsent(roleId, null);
//				continue;
//			}
//			if (!connector.isHandshakeDone()) {
//				logger.warn("groupByLink: not isHandshakeDone for linkName={} roleId={}", linkName, roleId);
//				groupNotOnline.roles.putIfAbsent(roleId, null);
//				continue;
//			}
//			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
//			var group = groups.get(linkName);
//			if (group == null) {
//				group = new RoleOnLink();
//				group.linkName = linkName;
//				group.linkSocket = connector.getSocket();
//				groups.put(group.linkName, group);
//			}
//			var linkSid = online.getLink().getLinkSid();
//			group.roles.putIfAbsent(roleId, linkSid);
//			group.contexts.putIfAbsent(linkSid, roleId);
//		}
//		return groups.values();
//	}

	public void addReliableNotifyMark(long roleId, @NotNull String listenerName) {
		var online = getLoginOnline(roleId);
		if (online == null)
			throw new IllegalStateException("Not Online. AddReliableNotifyMark: " + listenerName);
		online.getReliableNotifyMark().add(listenerName);
	}

	public void removeReliableNotifyMark(long roleId, @NotNull String listenerName) {
		// 移除尽量通过，不做任何判断。
		var online = getOnline(roleId);
		if (online != null)
			online.getReliableNotifyMark().remove(listenerName);
	}

	public void sendReliableNotifyWhileCommit(long roleId, @NotNull String listenerName, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileCommit(() -> sendReliableNotify(roleId, listenerName, p));
	}

	public void sendReliableNotifyWhileCommit(long roleId, @NotNull String listenerName, int typeId,
											  @NotNull Binary fullEncodedProtocol) {
		Transaction.whileCommit(() -> sendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol));
	}

	public void sendReliableNotifyWhileRollback(long roleId, @NotNull String listenerName, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		Transaction.whileRollback(() -> sendReliableNotify(roleId, listenerName, p));
	}

	public void sendReliableNotifyWhileRollback(long roleId, @NotNull String listenerName, int typeId,
												@NotNull Binary fullEncodedProtocol) {
		Transaction.whileRollback(() -> sendReliableNotify(roleId, listenerName, typeId, fullEncodedProtocol));
	}

	public void sendReliableNotify(long roleId, @NotNull String listenerName, @NotNull Protocol<?> p) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Send", roleId + ":" + listenerName, p);
		sendReliableNotify(roleId, listenerName, typeId, new Binary(p.encode()));
	}

	private @NotNull Zeze.Collections.Queue<BNotify> openQueue(long roleId) {
		return providerApp.zeze.getQueueModule().open("Zeze.Game.Online.ReliableNotifyQueue:" + roleId, BNotify.class);
	}

	/**
	 * 发送在线可靠协议，如果不在线等，仍然不会发送哦。
	 *
	 * @param fullEncodedProtocol 协议必须先编码，因为会跨事务。
	 */
	public void sendReliableNotify(long roleId, @NotNull String listenerName, long typeId,
								   @NotNull Binary fullEncodedProtocol) {
		providerApp.zeze.runTaskOneByOneByKey(listenerName, "Online.sendReliableNotify." + listenerName, () -> {
			var online = getLoginOnline(roleId);
			if (online == null)
				return Procedure.Success;
			if (!online.getReliableNotifyMark().contains(listenerName))
				return Procedure.Success; // 相关数据装载的时候要同步设置这个。

			// 先保存在再发送，然后客户端还会确认。
			// see Game.Login.Module: CLogin CReLogin CReliableNotifyConfirm 的实现。
			var queue = openQueue(roleId);
			var bNotify = new BNotify();
			bNotify.setFullEncodedProtocol(fullEncodedProtocol);
			queue.add(bNotify);

			// 不直接发送协议，是因为客户端需要识别ReliableNotify并进行处理（计数）。
			var notify = new SReliableNotify(new BReliableNotify(online.getReliableNotifyIndex()));
			online.setReliableNotifyIndex(online.getReliableNotifyIndex() + 1); // after set notify.Argument
			notify.Argument.getNotifies().add(fullEncodedProtocol);

			Transaction.whileCommit(() -> {
				if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
					AsyncSocket.log("Send", roleId + ":" + listenerName, notify);
				sendDirect(roleId, notify.getTypeId(), new Binary(notify.encode()), false);
			});
//			sendEmbed(List.of(roleId), notify.getTypeId(), new Binary(notify.encode()));
			return Procedure.Success;
		});
	}

	/**
	 * 转发查询请求给RoleId。
	 *
	 * @param sender     查询发起者，结果发送给他。
	 * @param actionName 查询处理的实现
	 * @param roleId     目标角色
	 */
	public void transmit(long sender, @NotNull String actionName, long roleId, @Nullable Serializable parameter) {
		transmit(sender, actionName, List.of(roleId), parameter);
	}

	public void transmit(long sender, @NotNull String actionName, long roleId) {
		transmit(sender, actionName, roleId, null);
	}

	public void processTransmit(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds,
								@Nullable Binary parameter) {
		var handle = transmitActions.get(actionName);
		if (handle != null) {
			for (var target : roleIds) {
				Task.call(providerApp.zeze.newProcedure(() -> handle.call(sender, target, parameter),
						"Online.transmit: " + actionName));
			}
		}
	}

	public static final class RoleOnServer {
		int serverId = -1; // providerId
		final HashSet<Long> roles = new HashSet<>();

		@Override
		public String toString() {
			return roles.toString();
		}
	}

	public @NotNull IntHashMap<RoleOnServer> groupByServerId(@NotNull Iterable<Long> roleIds) {
		var groups = new IntHashMap<RoleOnServer>();
		var groupNotOnline = new RoleOnServer(); // LinkName is Empty And Socket is null.
		groups.put(-1, groupNotOnline);

		for (var roleId : roleIds) {
			var online = getLoginOnline(roleId);
			if (online == null) {
				groupNotOnline.roles.add(roleId);
				continue;
			}

			// 后面保存connector.Socket并使用，如果之后连接被关闭，以后发送协议失败。
			var group = groups.get(online.getServerId());
			if (group == null) {
				group = new RoleOnServer();
				group.serverId = online.getServerId();
				groups.put(group.serverId, group);
			}
			group.roles.add(roleId);
		}
		return groups;
	}

	private static @NotNull RoleOnServer merge(@Nullable RoleOnServer current, @NotNull RoleOnServer m) {
		if (current == null)
			return m;
		current.roles.addAll(m.roles);
		return current;
	}

	public void transmitEmbed(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds,
							  @Nullable Binary parameter, boolean processNotOnline) {
		if (providerApp.zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().isEmpty()) {
			// 没有启用cache-sync，马上触发本地任务。
			processTransmit(sender, actionName, roleIds, parameter);
			return;
		}

		var groups = groupByServerId(roleIds);
		RoleOnServer groupLocal = null;
		for (var it = groups.iterator(); it.moveToNext(); ) {
			var group = it.value();
			if (group.serverId == -1) {
				// not online
				if (processNotOnline)
					groupLocal = merge(groupLocal, group);
				else
					logger.info("transmit not online roles={}", group);
				continue;
			}
			if (group.serverId == providerApp.zeze.getConfig().getServerId()) {
				// loopback 就是当前gs.
				groupLocal = merge(groupLocal, group);
				continue;
			}
			var transmit = new Transmit();
			transmit.Argument.setActionName(actionName);
			transmit.Argument.setSender(sender);
			transmit.Argument.getRoles().addAll(group.roles);
			transmit.Argument.setOnlineSetName(multiInstanceName);
			if (parameter != null)
				transmit.Argument.setParameter(parameter);
			var ps = providerApp.providerDirectService.providerByServerId.get(group.serverId);
			if (ps == null) {
				assert groupLocal != null;
				groupLocal.roles.addAll(group.roles);
				continue;
			}
			var socket = providerApp.providerDirectService.GetSocket(ps.getSessionId());
			if (socket == null) {
				assert groupLocal != null;
				groupLocal.roles.addAll(group.roles);
				continue;
			}
			Transaction.whileCommit(() -> transmit.Send(socket));
		}
		if (groupLocal != null && !groupLocal.roles.isEmpty())
			processTransmit(sender, actionName, groupLocal.roles, parameter);
	}

	public void transmit(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds) {
		transmit(sender, actionName, roleIds, null);
	}

	public void transmit(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds,
						 @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		ByteBuffer bb;
		if (parameter != null) {
			int preSize = parameter.preAllocSize();
			bb = ByteBuffer.Allocate(Math.min(preSize, 65536));
			parameter.encode(bb);
			if (bb.WriteIndex > preSize)
				parameter.preAllocSize(bb.WriteIndex);
		} else
			bb = null;
		// 发送协议请求在另外的事务中执行。
		Task.run(providerApp.zeze.newProcedure(() -> {
			transmitEmbed(sender, actionName, roleIds, bb != null ? new Binary(bb) : null, true);
			return Procedure.Success;
		}, "Online.transmit"), null, null, DispatchMode.Normal);
	}

	public void transmitWhileCommit(long sender, @NotNull String actionName, long roleId) {
		transmitWhileCommit(sender, actionName, roleId, null);
	}

	public void transmitWhileCommit(long sender, @NotNull String actionName, long roleId,
									@Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(sender, actionName, roleId, parameter));
	}

	public void transmitWhileCommit(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds) {
		transmitWhileCommit(sender, actionName, roleIds, null);
	}

	public void transmitWhileCommit(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds,
									@Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileCommit(() -> transmit(sender, actionName, roleIds, parameter));
	}

	public void transmitWhileRollback(long sender, @NotNull String actionName, long roleId) {
		transmitWhileRollback(sender, actionName, roleId, null);
	}

	public void transmitWhileRollback(long sender, @NotNull String actionName, long roleId,
									  @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileRollback(() -> transmit(sender, actionName, roleId, parameter));
	}

	public void transmitWhileRollback(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds) {
		transmitWhileRollback(sender, actionName, roleIds, null);
	}

	public void transmitWhileRollback(long sender, @NotNull String actionName, @NotNull Iterable<Long> roleIds,
									  @Nullable Serializable parameter) {
		if (!transmitActions.containsKey(actionName))
			throw new UnsupportedOperationException("Unknown Action Name: " + actionName);
		Transaction.whileRollback(() -> transmit(sender, actionName, roleIds, parameter));
	}

	private int broadcast(long typeId, @NotNull Binary fullEncodedProtocol, int time, boolean onlySameVersion) {
//		TaskCompletionSource<Long> future = null;
		var broadcast = new Broadcast(new BBroadcast.Data(typeId, fullEncodedProtocol, time, onlySameVersion));
		var pdata = broadcast.encode();
		int sendCount = 0;
		for (var link : providerApp.providerService.getLinks().values()) {
			if (link.getSocket() != null && link.getSocket().Send(pdata))
				sendCount++;
		}
//		if (future != null)
//			future.await();
		return sendCount;
	}

	public int broadcast(@NotNull Protocol<?> p) {
		return broadcast(p, 60 * 1000, false);
	}

	public int broadcast(@NotNull Protocol<?> p, boolean onlySameVersion) {
		return broadcast(p, 60 * 1000, onlySameVersion);
	}

	public int broadcast(@NotNull Protocol<?> p, int time) {
		return broadcast(p, time, false);
	}

	public int broadcast(@NotNull Protocol<?> p, int time, boolean onlySameVersion) {
		if (p instanceof Rpc && p.isRequest())
			throw new IllegalArgumentException(p.getClass().getName() + " is rpc. please use sendRpc/sendOnlineRpc");
		var typeId = p.getTypeId();
		if (AsyncSocket.ENABLE_PROTOCOL_LOG && AsyncSocket.canLogProtocol(typeId))
			AsyncSocket.log("Broc", providerApp.providerService.getLinks().size(), p);
		return broadcast(typeId, new Binary(p.encode()), time, onlySameVersion);
	}

	class VerifyBatch {
		private final ArrayList<Long> roleIds = new ArrayList<>();
		private int walkCount;
		private int removeCount;

		int getRemainCount() {
			return walkCount - removeCount;
		}

		void add(long roleId) {
			walkCount++;
			var aTime = localActiveTimes.get(roleId);
			if (aTime != null && System.currentTimeMillis() - aTime > localActiveTimeout) {
				roleIds.add(roleId);
				removeCount++;
			}
		}

		void tryPerform() {
			if (roleIds.size() > 20)
				perform();
		}

		void perform() {
			if (!roleIds.isEmpty()) {
				try {
					providerApp.zeze.newProcedure(() -> {
						for (var roleId : roleIds)
							tryRemoveLocal(roleId, true);
						return 0L;
					}, "Online.verifyLocal:" + roleIds).call();
					sendDirect(roleIds, CheckLinkSession.TypeId_, new Binary(new CheckLinkSession().encode()), true);
				} catch (Exception e) {
					logger.error("", e);
				}
				roleIds.clear();
			}
		}
	}

	private void verifyLocal() throws Exception {
		try {
			var batch = new VerifyBatch();
			// 锁外执行事务
			_tlocal.walk((k, v) -> {
				batch.add(k);
				batch.tryPerform();
				return true;
			});
			batch.perform();
			verifyLocalCount.set(batch.getRemainCount());
		} finally {
			startLocalCheck();
		}
	}

	private long tryRemoveLocal(long roleId, boolean notVerifyCount) throws Exception {
		var local = _tlocal.get(roleId);
		if (local == null)
			return 0;

		var onlineShared = getOnlineShared(roleId);
		if (onlineShared == null || onlineShared.getLink().getState() == eOffline
				|| onlineShared.getLoginVersion() != local.getLoginVersion())
			return removeLocalAndTrigger(roleId, notVerifyCount);
		return 0;
	}

	@RedirectToServer
	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	protected void redirectRemoveLocal(int serverId, long roleId, @Nullable String instanceName) {
		if (defaultInstance != null) {
			// 能收到redirect的肯定是defaultOnline，这里为了保险期间和代码更清楚，直接使用defaultInstance。
			var onlineSet = defaultInstance.getOnline(instanceName);
			if (onlineSet != null)
				providerApp.zeze.newProcedure(() -> onlineSet.tryRemoveLocal(roleId, false),
						"Online.redirectRemoveLocal").call();
		}
	}

	private void tryRedirectRemoveLocal(String instanceName, int serverId, long roleId) {
		if (providerApp.zeze.getConfig().getServerId() != serverId
				&& providerApp.providerDirectService.providerByServerId.containsKey(serverId)) {
			if (this == defaultInstance)
				redirectRemoveLocal(serverId, roleId, instanceName);
			else if (defaultInstance != null)
				defaultInstance.redirectRemoveLocal(serverId, roleId, instanceName);
		}
	}

	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	@Override
	protected long ProcessLoginRequest(@NotNull Login rpc) {
		if (!AsyncSocket.ENABLE_PROTOCOL_LOG)
			logger.info("Login[{}]: {}", rpc.getSender().getSessionId(), AsyncSocket.toStr(rpc.Argument));
		var onlineSet = getOnline(rpc.Argument.getOnlineSetName());
		if (onlineSet == null) {
			var session = ProviderUserSession.get(rpc);
			providerApp.providerService.kick(session.getLinkName(), session.getLinkSid(),
					BKick.ErrorOnlineSetName, "unknown OnlineSetName: '" + rpc.Argument.getOnlineSetName() + '\'');
			return 0;
		}
		return onlineSet.ProcessLoginRequestOnlineSet(rpc);
	}

	private long ProcessLoginRequestOnlineSet(@NotNull Login rpc) {
		var done = new OutObject<>(false);
		while (!done.value) {
			var r = Task.call(providerApp.zeze.newProcedure(() -> ProcessLoginRequest(rpc, done),
					"ProcessLoginRequest"));
			if (r != 0)
				return r;
		}
		return 0;
	}

	private long ProcessLoginRequest(@NotNull Login rpc, @NotNull OutObject<Boolean> done) throws Exception {
		done.value = true; // 默认设置成处理完成，包括错误的时候。下面分支需要的时候重新设置成false。

		var session = ProviderUserSession.get(rpc);
		var dispatch = ProviderImplement.localDispatch();
		if (dispatch != null)
			dispatch.Argument.setOnlineSetName(multiInstanceName); // 这里需要设置OnlineSetName,因为link此时还无法设置
		var roleId = rpc.Argument.getRoleId();
		var onlineShared = getOrAddOnlineShared(roleId);
		var isAdd = new OutObject<>(false);
		var local = _tlocal.getOrAdd(roleId, isAdd);
		Transaction.whileCommit(() -> putLocalActiveTime(roleId));
		if (isAdd.value)
			Transaction.whileCommit(verifyLocalCount::incrementAndGet);
		var link = onlineShared.getLink();

		var onlineAccount = onlineShared.getAccount();
		var isBound = !onlineAccount.isEmpty();
		if (!isBound)
			onlineShared.setAccount(session.getAccount()); // 这里依赖loginTrigger做进一步的角色是否属于账号的验证,验证失败会回滚
		else if (!onlineAccount.equals(session.getAccount()))
			return Procedure.LogicError;

		// login exist
		if (isBound && assignLogoutVersion(onlineShared)) {
			if (!link.getLinkName().equals(session.getLinkName()) || link.getLinkSid() != session.getLinkSid()) {
				providerApp.providerService.kick(link.getLinkName(), link.getLinkSid(),
						BKick.ErrorDuplicateLogin, "duplicate role login");
			}
			var ret = logoutTrigger(roleId, LogoutReason.LOGIN);
			if (ret != 0)
				return ret;
			// 发生了补Logout事件，重做Login。
			done.value = false;
			return 0; // Logout事件补了以后这个局部事务是成功完成的。
		}

		// 开始登录流程，先准备 link-state。
		var loginVersion = onlineShared.getLoginVersion() + 1;
		local.setLoginVersion(loginVersion);
		onlineShared.setLoginVersion(loginVersion);
		var bLink = new BLink(session.getLinkName(), session.getLinkSid(), eLogined);
		onlineShared.setLink(bLink);
		local.setLink(bLink);

		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			//setUserState.Argument.getUserState().setLoginVersion(loginVersion);
			setUserState.Argument.getUserState().setOnlineSetName(multiInstanceName);
			setUserState.Argument.getUserState().setContext(String.valueOf(roleId));
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		var online = getOrAddOnline(roleId);
		online.getReliableNotifyMark().clear();
		openQueue(roleId).clear();
		online.setReliableNotifyConfirmIndex(0);
		online.setReliableNotifyIndex(0);

		// var linkSession = (ProviderService.LinkSession)session.getLink().getUserState();
		online.setServerId(providerApp.zeze.getConfig().getServerId());

		// Login的结果先提交进事务，然后再触发loginTrigger，这样loginTrigger中发送的协议排在后面。
		session.sendResponseWhileCommit(rpc);
		return loginTrigger(session.getAccount(), roleId);
	}

	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	@Override
	protected long ProcessReLoginRequest(@NotNull ReLogin rpc) {
		if (!AsyncSocket.ENABLE_PROTOCOL_LOG)
			logger.info("ReLogin[{}]: {}", rpc.getSender().getSessionId(), AsyncSocket.toStr(rpc.Argument));
		var onlineSet = getOnline(rpc.Argument.getOnlineSetName());
		if (onlineSet == null) {
			var session = ProviderUserSession.get(rpc);
			providerApp.providerService.kick(session.getLinkName(), session.getLinkSid(),
					BKick.ErrorOnlineSetName, "unknown OnlineSetName: '" + rpc.Argument.getOnlineSetName() + '\'');
			return 0;
		}
		return onlineSet.ProcessReLoginRequestOnlineSet(rpc);
	}

	protected long ProcessReLoginRequestOnlineSet(@NotNull ReLogin rpc) {
		var done = new OutObject<>(false);
		while (!done.value) {
			var r = Task.call(providerApp.zeze.newProcedure(() -> ProcessReLoginRequest(rpc, done),
					"ProcessReLoginRequest"));
			if (r != 0)
				return r;
		}
		return 0;
	}

	private long ProcessReLoginRequest(@NotNull ReLogin rpc, @NotNull OutObject<Boolean> done) throws Exception {
		done.value = true; // 默认设置成处理完成，包括错误的时候。下面分支需要的时候重新设置成false。

		var session = ProviderUserSession.get(rpc);
		var dispatch = ProviderImplement.localDispatch();
		if (dispatch != null)
			dispatch.Argument.setOnlineSetName(multiInstanceName); // 这里需要设置OnlineSetName,因为link此时还无法设置
		var roleId = rpc.Argument.getRoleId();
		var onlineShared = getOrAddOnlineShared(roleId);
		var isAdd = new OutObject<>(false);
		var local = _tlocal.getOrAdd(roleId, isAdd);
		Transaction.whileCommit(() -> putLocalActiveTime(roleId));
		if (isAdd.value)
			Transaction.whileCommit(verifyLocalCount::incrementAndGet);
		var link = onlineShared.getLink();

		var onlineAccount = onlineShared.getAccount();
		var isBound = !onlineAccount.isEmpty();
		if (!isBound)
			onlineShared.setAccount(session.getAccount()); // 这里依赖reloginTrigger做进一步的角色是否属于账号的验证,验证失败会回滚
		else if (!onlineAccount.equals(session.getAccount()))
			return Procedure.LogicError;

		// login exist
		if (isBound && assignLogoutVersion(onlineShared)) {
			if (!link.getLinkName().equals(session.getLinkName()) || link.getLinkSid() != session.getLinkSid()) {
				providerApp.providerService.kick(link.getLinkName(), link.getLinkSid(),
						BKick.ErrorDuplicateLogin, "duplicate role login");
			}
			var ret = logoutTrigger(roleId, LogoutReason.RE_LOGIN);
			if (ret != 0)
				return ret;
			// 发生了补Logout事件，重做ReLogin。
			done.value = false;
			return 0; // Logout事件补了以后这个局部事务是成功完成的。
		}

		// 开始登录流程，先准备 link-state。
		var loginVersion = onlineShared.getLoginVersion() + 1;
		local.setLoginVersion(loginVersion);
		onlineShared.setLoginVersion(loginVersion);
		var bLink = new BLink(session.getLinkName(), session.getLinkSid(), eLogined);
		onlineShared.setLink(bLink);
		local.setLink(bLink);

		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(session.getLinkSid());
			//setUserState.Argument.getUserState().setLoginVersion(loginVersion);
			setUserState.Argument.getUserState().setOnlineSetName(multiInstanceName);
			setUserState.Argument.getUserState().setContext(String.valueOf(roleId));
			rpc.getSender().Send(setUserState); // 直接使用link连接。
		});

		getOrAddOnline(roleId).setServerId(providerApp.zeze.getConfig().getServerId());

		// 先发结果，再发送同步数据（ReliableNotifySync）。
		// 都使用 WhileCommit，如果成功，按提交的顺序发送，失败全部不会发送。
		session.sendResponseWhileCommit(rpc);

		var ret = reloginTrigger(session.getAccount(), roleId);
		if (ret != 0)
			return ret;

		var syncResultCode = reliableNotifySync(roleId, session, rpc.Argument.getReliableNotifyConfirmIndex());
		if (syncResultCode != ResultCodeSuccess)
			return errorCode(syncResultCode);

		return Procedure.Success;
	}

	@Override
	protected long ProcessLogoutRequest(@NotNull Logout rpc) throws Exception {
		if (!AsyncSocket.ENABLE_PROTOCOL_LOG)
			logger.info("Logout[{}]: {}", rpc.getSender().getSessionId(), AsyncSocket.toStr(rpc.Argument));
		var session = ProviderUserSession.get(rpc);
		var onlineSet = getOnline(session.getOnlineSetName());
		return onlineSet != null ? onlineSet.ProcessLogoutRequestOnlineSet(rpc) : 0; // skip logout error
	}

	protected long ProcessLogoutRequestOnlineSet(@NotNull Logout rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);
		if (session.getRoleId() == null)
			return errorCode(ResultCodeNotLogin);

		localLogout(session.getRoleId(), rpc.getSender(), session.getLinkSid());
		session.sendResponseWhileCommit(rpc);
		// 在 OnLinkBroken 时处理。可以同时处理网络异常的情况。
		// App.Load.LogoutCount.IncrementAndGet();
		return Procedure.Success;
	}

	public long localLogout(long roleId) throws Exception {
		var onlineShared = _tOnlineShared.get(roleId);
		if (onlineShared == null)
			return Procedure.LogicError;
		var linkSid = onlineShared.getLink().getLinkSid();
		var link = providerApp.providerService.getLinks().get(onlineShared.getLink().getLinkName());
		return localLogout(roleId, link.getSocket(), linkSid);
	}

	private long localLogout(long roleId, @NotNull AsyncSocket link, long linkSid) throws Exception {
		//var local = _tlocal.get(session.getRoleId());
		var onlineShared = getLoginOnlineShared(roleId);
		if (onlineShared != null) {
			if (assignLogoutVersion(onlineShared)) {
				var ret = logoutTrigger(roleId, LogoutReason.LOGOUT);
				if (ret != 0)
					return ret;
				// 到这里online被删除了。
			}
		}
		// 先设置状态，再发送Logout结果。
		Transaction.whileCommit(() -> {
			var setUserState = new SetUserState();
			setUserState.Argument.setLinkSid(linkSid);
			link.Send(setUserState); // 直接使用link连接。
		});
		return 0;
	}

	private int reliableNotifySync(long roleId, @NotNull ProviderUserSession session,
								   long reliableNotifyConfirmCount) throws Exception {
		return reliableNotifySync(roleId, session, reliableNotifyConfirmCount, true);
	}

	private int reliableNotifySync(long roleId, @NotNull ProviderUserSession session,
								   long index, boolean sync) throws Exception {
		var online = getOrAddOnline(roleId);
		var queue = openQueue(roleId);
		if (index < online.getReliableNotifyConfirmIndex()
				|| index > online.getReliableNotifyIndex()
				|| index - online.getReliableNotifyConfirmIndex() > queue.size()) {
			return ResultCodeReliableNotifyConfirmIndexOutOfRange;
		}

		int confirmCount = (int)(index - online.getReliableNotifyConfirmIndex());
		for (int i = 0; i < confirmCount; i++)
			queue.poll();
		if (sync) {
			var notify = new SReliableNotify(new BReliableNotify(index));
			queue.walk((nodeId, bNotify) -> {
				notify.Argument.getNotifies().add(bNotify.getFullEncodedProtocol());
				return true;
			});
			session.sendResponseWhileCommit(notify);
		}
		//online.getReliableNotifyQueue().RemoveRange(0, confirmCount);
		online.setReliableNotifyConfirmIndex(index);
		return ResultCodeSuccess;
	}

	@Override
	protected long ProcessReliableNotifyConfirmRequest(@NotNull ReliableNotifyConfirm rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);
		var onlineSet = getOnline(session.getOnlineSetName());
		if (onlineSet == null) {
			logger.warn("ProcessReliableNotifyConfirmRequest online set not found {}", session.getOnlineSetName());
			return 0;
		}
		return onlineSet.ProcessReliableNotifyConfirmRequestOnlineSet(rpc);
	}

	protected long ProcessReliableNotifyConfirmRequestOnlineSet(@NotNull ReliableNotifyConfirm rpc) throws Exception {
		var session = ProviderUserSession.get(rpc);
		var roleId = session.getRoleId();
		if (roleId == null)
			return errorCode(ResultCodeNotLogin);
		var online = getLoginOnline(roleId);
		if (online != null)
			return errorCode(ResultCodeNotLogin);

		session.sendResponseWhileCommit(rpc); // 同步前提交。

		var syncResultCode = reliableNotifySync(session.getRoleId(), session,
				rpc.Argument.getReliableNotifyConfirmIndex(), rpc.Argument.isSync());
		if (syncResultCode != ResultCodeSuccess)
			return errorCode(syncResultCode);

		return Procedure.Success;
	}

	public boolean bindDynamic(long roleId, int @NotNull ... moduleIds) {
		var bean = getLoginOnlineShared(roleId);
		if (null == bean)
			return false;
		var link = bean.getLink();

		Transaction.whileCommit(() -> {
			var bind = new Zeze.Builtin.Provider.Bind();
			var connector = providerApp.providerService.getLinks().get(link.getLinkName());
			var socket = connector.getSocket();
			bind.Argument.getLinkSids().add(link.getLinkSid());
			for (var moduleId : moduleIds) {
				bind.Argument.getModules().put(moduleId, new BModule.Data(
						BModule.ChoiceTypeDefault, BModule.ConfigTypeDynamic));
			}
			bind.SendForWait(socket);
		});
		return true;
	}
}
