package Zeze.Component;

import java.text.ParseException;
import Zeze.Arch.LocalRemoveEventArgument;
import Zeze.Arch.LoginArgument;
import Zeze.Arch.Online;
import Zeze.Builtin.Timer.BAccountClientId;
import Zeze.Builtin.Timer.BArchOnlineTimer;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BOfflineAccountCustom;
import Zeze.Builtin.Timer.BOnlineTimers;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 1. schedule，scheduleNamed 完全重新实现一套基于内存表和内存的。
 * 2. 不直接使用 Timer.schedule。但有如下关联。
 * 直接使用 Timer.timerIdAutoKey，使得返回的timerId共享一个名字空间。
 * 直接使用 Timer.timersFuture，从 ThreadPool 返回的future保存在这里。
 */
public class TimerAccount {
	private static final Logger logger = LogManager.getLogger(TimerAccount.class);
	//public static final String eTimerHandleName = "Zeze.Component.TimerArchOnline.Handle";
	public static final String eOnlineTimers = "Zeze.Component.TimerArchOnline";

	private final @NotNull Online online;

	TimerAccount(@NotNull Online online) {
		this.online = online;

		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().add(EventDispatcher.Mode.RunEmbed, this::onLocalRemoveEvent);
		online.getLoginEvents().add(EventDispatcher.Mode.RunEmbed, this::onLoginEvent);
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									   long delay, long period, long times, long endTime,
									   @Nullable TimerHandle handle, @Nullable Bean customData) {
		return scheduleOnlineNamed(account, clientId, timerId, delay, period, times, endTime, handle, customData, "");
	}

	public boolean scheduleOnlineNamed(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									   long delay, long period, long times, long endTime,
									   @Nullable TimerHandle handle, @Nullable Bean customData,
									   @NotNull String oneByOneKey) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		var bTimer = online.providerApp.zeze.getTimer().tAccountTimers().get(timerId);
		if (bTimer != null)
			return false;
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		scheduleOnline(account, clientId, timerId, simpleTimer, handle, customData);
		return true;
	}

	public boolean scheduleOnlineNamedHot(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										  long delay, long period, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		return scheduleOnlineNamedHot(account, clientId, timerId, delay, period, times, endTime, handleClass,
				customData, "");
	}

	public boolean scheduleOnlineNamedHot(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										  long delay, long period, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										  @NotNull String oneByOneKey) {
		var bTimer = online.providerApp.zeze.getTimer().tAccountTimers().get(timerId);
		if (bTimer != null)
			return false;
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		scheduleOnlineHot(account, clientId, timerId, simpleTimer, handleClass, customData);
		return true;
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									   @NotNull String cron, long times, long endTime,
									   @Nullable TimerHandle handle, @Nullable Bean customData) throws Exception {
		return scheduleOnlineNamed(account, clientId, timerId, cron, times, endTime, handle, customData, "");
	}

	public boolean scheduleOnlineNamed(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									   @NotNull String cron, long times, long endTime,
									   @Nullable TimerHandle handle, @Nullable Bean customData,
									   @NotNull String oneByOneKey) throws Exception {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		if (online.providerApp.zeze.getTimer().tAccountTimers().get(timerId) != null)
			return false;

		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		scheduleOnline(account, clientId, timerId, cronTimer, handle, customData);
		return true;
	}

	public boolean scheduleOnlineNamedHot(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										  @NotNull String cron, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) throws Exception {
		return scheduleOnlineNamedHot(account, clientId, timerId, cron, times, endTime, handleClass, customData, "");
	}

	public boolean scheduleOnlineNamedHot(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										  @NotNull String cron, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										  @NotNull String oneByOneKey) throws Exception {
		if (online.providerApp.zeze.getTimer().tAccountTimers().get(timerId) != null)
			return false;

		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		scheduleOnlineHot(account, clientId, timerId, cronTimer, handleClass, customData);
		return true;
	}

	public @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId, long delay, long period,
										  long times, long endTime, @Nullable TimerHandle handle,
										  @Nullable Bean customData) {
		return scheduleOnline(account, clientId, delay, period, times, endTime, handle, customData, "");
	}

	public @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId, long delay, long period,
										  long times, long endTime, @Nullable TimerHandle handle,
										  @Nullable Bean customData, @NotNull String oneByOneKey) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		return scheduleOnline(account, clientId, '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString(),
				simpleTimer, handle, customData);
	}

	public @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId,
											 long delay, long period, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) {
		return scheduleOnlineHot(account, clientId, delay, period, times, endTime, handleClass, customData, "");
	}

	public @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId,
											 long delay, long period, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData, @NotNull String oneByOneKey) {
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		return scheduleOnlineHot(account, clientId,
				'@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString(),
				simpleTimer, handleClass, customData);
	}

	private @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										   @NotNull BSimpleTimer simpleTimer, @Nullable TimerHandle handle,
										   @Nullable Bean customData) {
		var loginVersion = online.getLocalLoginVersion(account, clientId);
		if (loginVersion == null)
			throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BArchOnlineTimer(account, clientId, loginVersion, timer.timerSerialId.nextId());
		timer.tAccountTimers().insert(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(simpleTimer);

		var timerIds = online.getOrAddLocalBean(account, clientId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (customData != null) {
			Timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleSimple(timerId, simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), handle);
		return timerId;
	}

	private @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId,
											  @NotNull String timerId, @NotNull BSimpleTimer simpleTimer,
											  @NotNull Class<? extends TimerHandle> handleClass,
											  @Nullable Bean customData) {
		var loginVersion = online.getLocalLoginVersion(account, clientId);
		if (loginVersion == null)
			throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BArchOnlineTimer(account, clientId, loginVersion, timer.timerSerialId.nextId());
		timer.tAccountTimers().insert(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(simpleTimer);

		var timerIds = online.getOrAddLocalBean(account, clientId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (customData != null) {
			Timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleSimpleHot(timerId, simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), handleClass);
		return timerId;
	}

	public @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId, @NotNull String cron,
										  long times, long endTime, @Nullable TimerHandle handle,
										  @Nullable Bean customData) throws Exception {
		return scheduleOnline(account, clientId, cron, times, endTime, handle, customData, "");
	}

	public @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId, @NotNull String cron,
										  long times, long endTime, @Nullable TimerHandle handle,
										  @Nullable Bean customData, @NotNull String oneByOneKey) throws Exception {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		return scheduleOnline(account, clientId, '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString(),
				cronTimer, handle, customData);
	}

	public @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId, @NotNull String cron,
											 long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) throws Exception {
		return scheduleOnlineHot(account, clientId, cron, times, endTime, handleClass, customData, "");
	}

	public @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId, @NotNull String cron,
											 long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData, @NotNull String oneByOneKey) throws Exception {
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		return scheduleOnlineHot(account, clientId,
				'@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString(),
				cronTimer, handleClass, customData);
	}

	private @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										   @NotNull BCronTimer cronTimer, @Nullable TimerHandle handle,
										   @Nullable Bean customData) {
		var loginVersion = online.getLocalLoginVersion(account, clientId);
		if (loginVersion == null)
			throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BArchOnlineTimer(account, clientId, loginVersion, timer.timerSerialId.nextId());
		timer.tAccountTimers().insert(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(cronTimer);

		var timerIds = online.getOrAddLocalBean(account, clientId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (customData != null) {
			Timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleCron(timerId, cronTimer, handle);
		return timerId;
	}

	private @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId,
											  @NotNull String timerId, @NotNull BCronTimer cronTimer,
											  @NotNull Class<? extends TimerHandle> handleClass,
											  @Nullable Bean customData) {
		var loginVersion = online.getLocalLoginVersion(account, clientId);
		if (loginVersion == null)
			throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BArchOnlineTimer(account, clientId, loginVersion, timer.timerSerialId.nextId());
		timer.tAccountTimers().insert(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(cronTimer);

		var timerIds = online.getOrAddLocalBean(account, clientId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (customData != null) {
			Timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleCronHot(timerId, cronTimer, handleClass);
		return timerId;
	}

	public boolean cancel(@NotNull String timerId) {
		var timer = online.providerApp.zeze.getTimer();
		// always cancel future task. 第一步就做这个。
		Transaction.whileCommit(() -> timer.cancelFuture(timerId));

		// remove online timer
		var bTimer = timer.tAccountTimers().get(timerId);
		if (bTimer == null)
			return false;

		// remove online local
		var onlineTimers = (BOnlineTimers)online.getLocalBean(bTimer.getAccount(), bTimer.getClientId(), eOnlineTimers);
		if (onlineTimers != null) {
			onlineTimers.getTimerIds().remove(timerId);
			if (onlineTimers.getTimerIds().isEmpty())
				online.removeLocalBean(bTimer.getAccount(), bTimer.getClientId(), eOnlineTimers);
		}
		// always remove.
		timer.tAccountTimers().remove(timerId);

		return true;
	}

	public boolean scheduleOfflineNamed(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
										long delay, long period, long times, long endTime, int missFirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		var timerIndex = online.providerApp.zeze.getTimer().tIndexs().get(timerId);
		if (timerIndex != null)
			return false;

		scheduleOffline(timerId, account, clientId, delay, period, times, endTime, missFirePolicy, handleClass,
				customData, "");
		return true;
	}

	public boolean scheduleOfflineNamed(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
										long delay, long period, long times, long endTime, int missFirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										@NotNull String oneByOneKey) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		var timerIndex = online.providerApp.zeze.getTimer().tIndexs().get(timerId);
		if (timerIndex != null)
			return false;

		scheduleOffline(timerId, account, clientId, delay, period, times, endTime, missFirePolicy, handleClass,
				customData, oneByOneKey);
		return true;
	}

	public @NotNull String scheduleOffline(@NotNull String account, @NotNull String clientId,
										   long delay, long period, long times, long endTime, int missFirePolicy,
										   @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) {
		var timerId = '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString();
		scheduleOffline(timerId, account, clientId, delay, period, times, endTime, missFirePolicy, handleClass,
				customData, "");
		return timerId;
	}

	public @NotNull String scheduleOffline(@NotNull String account, @NotNull String clientId,
										   long delay, long period, long times, long endTime, int missFirePolicy,
										   @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										   @NotNull String oneByOneKey) {
		var timerId = '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString();
		scheduleOffline(timerId, account, clientId, delay, period, times, endTime, missFirePolicy, handleClass,
				customData, oneByOneKey);
		return timerId;
	}

	private void scheduleOffline(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
								 long delay, long period, long times, long endTime, int missFirePolicy,
								 @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData, @NotNull String oneByOneKey) {
		var logoutVersion = online.getLogoutVersion(account, clientId);
		if (logoutVersion == null)
			throw new IllegalStateException("not logout. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineAccountCustom(timerId, account, clientId, logoutVersion, handleClass.getName());
		if (customData != null) {
			Timer.register(customData.getClass());
			custom.getCustomData().setBean(customData);
			timer.tryRecordBeanHotModuleWhileCommit(customData);
		}
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		simpleTimer.setMissfirePolicy(missFirePolicy);
		timer.schedule(timerId, simpleTimer, OfflineHandle.class, custom);
		var config = timer.zeze.getConfig();
		var offline = timer.tAccountOfflineTimers().getOrAdd(new BAccountClientId(account, clientId));
		if (offline.getOfflineTimers().size() > config.getOfflineTimerLimit()) {
			// throw new IllegalStateException("too many offline timers. account=" + account + " clientId=" + clientId + " size=" + offline.getOfflineTimers().size());
			logger.error("scheduleOffline(simple): too many timers. account={}, clientId={}, size={} > {}",
					account, clientId, offline.getOfflineTimers().size(), config.getOfflineTimerLimit());
		}
		if (offline.getOfflineTimers().putIfAbsent(timerId, config.getServerId()) != null)
			throw new IllegalStateException("duplicate timerId. account=" + account + " clientId=" + clientId);
	}

	public boolean scheduleOfflineNamed(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
										@NotNull String cron, long times, long endTime, int missFirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) throws ParseException {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		if (online.providerApp.zeze.getTimer().tIndexs().get(timerId) != null)
			return false;

		scheduleOffline(timerId, account, clientId, cron, times, endTime, missFirePolicy, handleClass, customData,
				"");
		return true;
	}

	public boolean scheduleOfflineNamed(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
										@NotNull String cron, long times, long endTime, int missFirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData, @NotNull String oneByOneKey) throws ParseException {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		if (online.providerApp.zeze.getTimer().tIndexs().get(timerId) != null)
			return false;

		scheduleOffline(timerId, account, clientId, cron, times, endTime, missFirePolicy, handleClass, customData,
				oneByOneKey);
		return true;
	}

	public @NotNull String scheduleOffline(@NotNull String account, @NotNull String clientId, @NotNull String cron,
										   long times, long endTime, int missFirePolicy,
										   @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) throws ParseException {
		var timerId = '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString();
		scheduleOffline(timerId, account, clientId, cron, times, endTime, missFirePolicy, handleClass, customData, "");
		return timerId;
	}

	public @NotNull String scheduleOffline(@NotNull String account, @NotNull String clientId, @NotNull String cron,
										   long times, long endTime, int missFirePolicy,
										   @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										   @NotNull String oneByOneKey) throws ParseException {
		var timerId = '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString();
		scheduleOffline(timerId, account, clientId, cron, times, endTime, missFirePolicy, handleClass, customData,
				oneByOneKey);
		return timerId;
	}

	private void scheduleOffline(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
								 @NotNull String cron, long times, long endTime, int missFirePolicy,
								 @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
								 @NotNull String oneByOneKey) throws ParseException {
		var logoutVersion = online.getLogoutVersion(account, clientId);
		if (logoutVersion == null)
			throw new IllegalStateException("not logout. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineAccountCustom(timerId, account, clientId, logoutVersion, handleClass.getName());
		if (customData != null) {
			Timer.register(customData.getClass());
			custom.getCustomData().setBean(customData);
		}
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		cronTimer.setMissfirePolicy(missFirePolicy);
		timer.schedule(timerId, cronTimer, OfflineHandle.class, custom);
		var config = timer.zeze.getConfig();
		var offline = timer.tAccountOfflineTimers().getOrAdd(new BAccountClientId(account, clientId));
		if (offline.getOfflineTimers().size() > config.getOfflineTimerLimit()) {
			// throw new IllegalStateException("too many offline timers. account=" + account + " clientId=" + clientId + " size=" + offline.getOfflineTimers().size());
			logger.error("scheduleOffline(cron): too many timers. account={}, clientId={}, size={} > {}",
					account, clientId, offline.getOfflineTimers().size(), config.getOfflineTimerLimit());
		}
		if (offline.getOfflineTimers().putIfAbsent(timerId, config.getServerId()) != null)
			throw new IllegalStateException("duplicate timerId. account=" + account + " clientId=" + clientId);
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 内部实现
	public static class OfflineHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var offlineCustom = (BOfflineAccountCustom)context.customData;
			//noinspection DataFlowIssue
			var account = offlineCustom.getAccount();
			var clientId = offlineCustom.getClientId();
			// 检查版本号，不正确的登录版本号表示过期的timer，取消掉即可。
			var loginVersion = context.timer.getAccountTimer().online.getLoginVersion(account, clientId);
			if (loginVersion != null && loginVersion == offlineCustom.getLoginVersion()) {
				context.account = account;
				context.clientId = clientId;
				context.customData = offlineCustom.getCustomData().getBean();
				@SuppressWarnings("unchecked")
				var handleClass = (Class<? extends TimerHandle>)Class.forName(offlineCustom.getHandleName());
				handleClass.getDeclaredConstructor().newInstance().onTimer(context);
			} else {
				var timerId = offlineCustom.getTimerName();
				context.timer.cancel(timerId);
				var offlineTimers = context.timer.tAccountOfflineTimers().get(new BAccountClientId(account, clientId));
				if (offlineTimers != null)
					offlineTimers.getOfflineTimers().remove(timerId);
			}
		}
	}

	private long onLoginEvent(@NotNull Object sender, @NotNull EventDispatcher.EventArgument arg) {
		var loginArg = (LoginArgument)arg;
		var loginKey = new BAccountClientId(loginArg.account, loginArg.clientId);
		var timer = online.providerApp.zeze.getTimer();
		var offlineTimers = timer.tAccountOfflineTimers().get(loginKey);
		// X: fix offlineTimers is null
		if (offlineTimers == null)
			return 0;
		for (var e : offlineTimers.getOfflineTimers().entrySet())
			timer.cancel(e.getKey());
		// 嵌入本地服务器事件事务中，
		// 删除之后，如果上面的redirectCancel失败，
		// 那么该timer触发的时候会检测到版本号不一致，
		// 然后timer最终也会被cancel掉。
		timer.tAccountOfflineTimers().remove(loginKey);
		return 0;
	}

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(@NotNull Object sender, @NotNull EventDispatcher.EventArgument arg) {
		var local = (LocalRemoveEventArgument)arg;
		if (local.local != null) {
			var bAny = local.local.getDatas().get(eOnlineTimers);
			if (bAny != null) {
				var timers = (BOnlineTimers)bAny.getAny().getBean();
				for (var timerId : timers.getTimerIds().keySet())
					cancel(timerId);
			}
		}
		return 0;
	}

	// 调度 cron 定时器
	private void scheduleCron(@NotNull String timerId, @NotNull BCronTimer cron, @Nullable TimerHandle handle) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(timerId, delay, handle);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	private void scheduleCronHot(@NotNull String timerId, @NotNull BCronTimer cron,
								 @NotNull Class<? extends TimerHandle> handleClass) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNextHot(timerId, delay, handleClass);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	// 再次调度 cron 定时器，真正安装到ThreadPool中。
	private void scheduleCronNext(@NotNull String timerId, long delay, @Nullable TimerHandle handle) {
		Transaction.whileCommit(() -> online.providerApp.zeze.getTimer().timerFutures.put(timerId,
				Task.scheduleUnsafe(delay, () -> fireCron(timerId, handle, false))));
	}

	private void scheduleCronNextHot(@NotNull String timerId, long delay,
									 @NotNull Class<? extends TimerHandle> handleClass) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(() -> timer.timerFutures.put(timerId, Task.scheduleUnsafe(delay,
				() -> fireCron(timerId, timer.findTimerHandle(handleClass.getName()), true))));
	}

	private void fireCron(@NotNull String timerId, @Nullable TimerHandle handle, boolean hot) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (handle == null) {
				cancel(timerId);
				return 0;
			}

			var bTimer = timer.tAccountTimers().get(timerId);
			if (bTimer == null) {
				Transaction.whileCommit(() -> timer.cancelFuture(timerId));
				return 0;
			}

			var loginVersion = online.getLoginVersion(bTimer.getAccount(), bTimer.getClientId());
			if (loginVersion == null || bTimer.getLoginVersion() != loginVersion) {
				// 已经不是注册定时器时候的登录了。
				Transaction.whileCommit(() -> timer.cancelFuture(timerId));
				return 0;
			}

			var localBean = online.<BOnlineTimers>getLocalBean(bTimer.getAccount(), bTimer.getClientId(), eOnlineTimers);
			if (localBean == null)
				throw new IllegalStateException("local bean not exist");
			var localTimer = localBean.getTimerIds().get(timerId);
			if (localTimer == null)
				throw new IllegalStateException("local timer not exist");
			var customData = localTimer.getCustomData().getBean();
			if (customData instanceof EmptyBean)
				customData = null;
			var cronTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BCronTimer();
			var hasNext = Timer.nextCronTimer(cronTimer, false);
			var context = new TimerContext(timer, timerId, handle.getClass().getName(), customData,
					cronTimer.getHappenTime(), cronTimer.getExpectedTime(), cronTimer.getNextExpectedTime());
			context.account = bTimer.getAccount();
			context.clientId = bTimer.getClientId();
			var serialSaved = bTimer.getSerialId();
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				handle.onTimer(context);
				return Procedure.Success;
			}, "TimerAccount.fireCron.inner"));

			var bTimerNew = timer.tAccountTimers().get(timerId);
			if (bTimerNew == null || bTimerNew.getSerialId() != serialSaved)
				return 0; // 已经取消或覆盖成新的timer

			if (retNest == Procedure.Exception) {
				cancel(timerId); // 异常错误不忽略。
				return 0;
			}
			// 其他错误忽略

			if (hasNext) { // 准备下一个间隔
				long delay = cronTimer.getNextExpectedTime() - System.currentTimeMillis();
				if (hot)
					scheduleCronNextHot(timerId, delay, handle.getClass());
				else
					scheduleCronNext(timerId, delay, handle);
			} else
				cancel(timerId);
			return 0;
		}, "TimerAccount.fireCron"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "TimerAccount finally cancel impossible!"));
		}
	}

	// 调度 Simple 定时器到ThreadPool中。
	private void scheduleSimple(@NotNull String timerId, long delay, @Nullable TimerHandle handle) {
		Transaction.whileCommit(() -> online.providerApp.zeze.getTimer().timerFutures.put(timerId,
				Task.scheduleUnsafe(delay, () -> fireSimple(timerId, handle, false))));
	}

	private void scheduleSimpleHot(@NotNull String timerId, long delay,
								   @NotNull Class<? extends TimerHandle> handleClass) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(() -> timer.timerFutures.put(timerId, Task.scheduleUnsafe(delay,
				() -> fireSimple(timerId, timer.findTimerHandle(handleClass.getName()), true))));
	}

	// Timer发生，执行回调。
	private void fireSimple(@NotNull String timerId, @Nullable TimerHandle handle, boolean hot) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (handle == null) {
				cancel(timerId);
				return 0;
			}

			var bTimer = timer.tAccountTimers().get(timerId);
			if (bTimer == null) {
				Transaction.whileCommit(() -> timer.cancelFuture(timerId));
				return 0;
			}
			var loginVersion = online.getLoginVersion(bTimer.getAccount(), bTimer.getClientId());
			if (loginVersion == null || bTimer.getLoginVersion() != loginVersion) {
				// 已经不是注册定时器时候的登录了。
				Transaction.whileCommit(() -> timer.cancelFuture(timerId));
				return 0;
			}

			var simpleTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BSimpleTimer();
			var serialSaved = bTimer.getSerialId();
			var hasNext = Timer.nextSimpleTimer(simpleTimer, false);
			var r = Task.call(online.providerApp.zeze.newProcedure(() -> {
				var localBean = online.<BOnlineTimers>getLocalBean(
						bTimer.getAccount(), bTimer.getClientId(), eOnlineTimers);
				if (localBean == null)
					throw new IllegalStateException("local bean not exist");
				var localTimer = localBean.getTimerIds().get(timerId);
				if (localTimer == null)
					throw new IllegalStateException("local timer not exist");
				var customData = localTimer.getCustomData().getBean();
				if (customData instanceof EmptyBean)
					customData = null;
				var context = new TimerContext(timer, timerId, handle.getClass().getName(), customData,
						simpleTimer.getHappenTimes(), simpleTimer.getExpectedTime(), simpleTimer.getNextExpectedTime());
				context.account = bTimer.getAccount();
				context.clientId = bTimer.getClientId();
				handle.onTimer(context);
				return Procedure.Success;
			}, "TimerAccount.fireSimple.inner"));

			var bTimerNew = timer.tAccountTimers().get(timerId);
			if (bTimerNew == null || bTimerNew.getSerialId() != serialSaved)
				return 0; // 已经取消或覆盖成新的timer

			if (r == Procedure.Exception) {
				cancel(timerId); // 异常错误不忽略。
				return 0;
			}
			// 其他错误忽略

			if (hasNext) { // 准备下一个间隔
				var delay = simpleTimer.getNextExpectedTime() - System.currentTimeMillis();
				if (hot)
					scheduleSimpleHot(timerId, delay, handle.getClass());
				else
					scheduleSimple(timerId, delay, handle);
			} else
				cancel(timerId);
			return 0;
		}, "TimerAccount.fireSimple"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "TimerAccount finally cancel impossible!"));
		}
	}
}
