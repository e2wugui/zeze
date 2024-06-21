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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 1. schedule，scheduleNamed 完全重新实现一套基于内存表和内存的。
 * 2. 不直接使用 Timer.schedule。但有如下关联。
 * 直接使用 Timer.timerIdAutoKey，使得返回的timerId共享一个名字空间。
 * 直接使用 Timer.timersFuture，从 ThreadPool 返回的future保存在这里。
 */
public class TimerAccount {
	final Online online;

	//public static final String eTimerHandleName = "Zeze.Component.TimerArchOnline.Handle";
	public static final String eOnlineTimers = "Zeze.Component.TimerArchOnline";

	TimerAccount(Online online) {
		this.online = online;

		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().add(EventDispatcher.Mode.RunEmbed, this::onLocalRemoveEvent);
		online.getLoginEvents().add(EventDispatcher.Mode.RunEmbed, this::onLoginEvent);
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(String account, String clientId, String timerName,
									   long delay, long period, long times, long endTime,
									   TimerHandle handle, Bean customData) {
		return scheduleOnlineNamed(account, clientId, timerName,
				delay, period, times, endTime,
				handle, customData, "");
	}

	public boolean scheduleOnlineNamed(String account, String clientId, String timerName,
									   long delay, long period, long times, long endTime,
									   TimerHandle handle, Bean customData, String oneByOneKey) {

		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		var timer = online.providerApp.zeze.getTimer();
		var bTimer = timer.tAccountTimers().get(timerName);
		if (null != bTimer)
			return false;
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		scheduleOnline(account, clientId, timerName, simpleTimer, handle, customData);
		return true;
	}

	public boolean scheduleOnlineNamedHot(String account, String clientId, String timerName,
										  long delay, long period, long times, long endTime,
										  Class<? extends TimerHandle> handle, Bean customData) {
		return scheduleOnlineNamedHot(account, clientId, timerName,
				delay, period, times, endTime,
				handle, customData, "");
	}

	public boolean scheduleOnlineNamedHot(String account, String clientId, String timerName,
										  long delay, long period, long times, long endTime,
										  Class<? extends TimerHandle> handle, Bean customData,
										  String oneByOneKey) {
		var timer = online.providerApp.zeze.getTimer();
		var bTimer = timer.tAccountTimers().get(timerName);
		if (null != bTimer)
			return false;
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		scheduleOnlineHot(account, clientId, timerName, simpleTimer, handle, customData);
		return true;
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(String account, String clientId, String timerName,
									   String cron, long times, long endTime,
									   TimerHandle handleName, Bean customData) throws Exception {
		return scheduleOnlineNamed(account, clientId, timerName,
				cron, times, endTime, handleName, customData, "");
	}

	public boolean scheduleOnlineNamed(String account, String clientId, String timerName,
									   String cron, long times, long endTime,
									   TimerHandle handleName, Bean customData,
									   String oneByOneKey) throws Exception {

		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.tAccountTimers().get(timerName);
		if (null != timerId)
			return false;
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		scheduleOnline(account, clientId, timerName, cronTimer, handleName, customData);
		return true;
	}

	public boolean scheduleOnlineNamedHot(String account, String clientId, String timerName,
										  String cron, long times, long endTime,
										  Class<? extends TimerHandle> handleName, Bean customData) throws Exception {
		return scheduleOnlineNamedHot(account, clientId, timerName,
				cron, times, endTime,
				handleName, customData, "");
	}

	public boolean scheduleOnlineNamedHot(String account, String clientId, String timerName,
										  String cron, long times, long endTime,
										  Class<? extends TimerHandle> handleName, Bean customData,
										  String oneByOneKey) throws Exception {
		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.tAccountTimers().get(timerName);
		if (null != timerId)
			return false;
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		scheduleOnlineHot(account, clientId, timerName, cronTimer, handleName, customData);
		return true;
	}

	public String scheduleOnline(String account, String clientId, long delay, long period, long times, long endTime,
								 TimerHandle name, Bean customData) {
		return scheduleOnline(account, clientId,
				delay, period, times, endTime, name, customData, "");
	}

	public String scheduleOnline(String account, String clientId, long delay, long period, long times, long endTime,
								 TimerHandle name, Bean customData, String oneByOneKey) {

		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOnline(account, clientId, "@" + timer.timerIdAutoKey.nextString(), simpleTimer, name, customData);
	}

	public String scheduleOnlineHot(String account, String clientId, long delay, long period, long times, long endTime,
									Class<? extends TimerHandle> name, Bean customData) {
		return scheduleOnlineHot(account, clientId,
				delay, period, times, endTime,
				name, customData, "");
	}

	public String scheduleOnlineHot(String account, String clientId, long delay, long period, long times, long endTime,
									Class<? extends TimerHandle> name, Bean customData, String oneByOneKey) {
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOnlineHot(account, clientId, "@" + timer.timerIdAutoKey.nextString(), simpleTimer, name, customData);
	}

	private String scheduleOnline(String account, String clientId, String timerId, BSimpleTimer simpleTimer,
								  TimerHandle name, Bean customData) {
		var loginVersion = online.getLocalLoginVersion(account, clientId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BArchOnlineTimer(account, clientId, loginVersion, timer.timerSerialId.nextId());
		timer.tAccountTimers().insert(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(simpleTimer);

		var timerIds = online.getOrAddLocalBean(account, clientId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (null != customData) {
			Timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleSimple(timerId, simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), name);
		return timerId;
	}

	private String scheduleOnlineHot(String account, String clientId, String timerId, BSimpleTimer simpleTimer,
									 Class<? extends TimerHandle> name, Bean customData) {
		var loginVersion = online.getLocalLoginVersion(account, clientId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BArchOnlineTimer(account, clientId, loginVersion, timer.timerSerialId.nextId());
		timer.tAccountTimers().insert(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(simpleTimer);

		var timerIds = online.getOrAddLocalBean(account, clientId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (null != customData) {
			Timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleSimpleHot(timerId, simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), name);
		return timerId;
	}

	public String scheduleOnline(String account, String clientId, String cron, long times, long endTime,
								 TimerHandle name, Bean customData) throws Exception {
		return scheduleOnline(account, clientId,
				cron, times, endTime, name, customData, "");
	}

	public String scheduleOnline(String account, String clientId, String cron, long times, long endTime,
								 TimerHandle name, Bean customData, String oneByOneKey) throws Exception {

		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOnline(account, clientId, "@" + timer.timerIdAutoKey.nextString(), cronTimer, name, customData);
	}

	public String scheduleOnlineHot(String account, String clientId, String cron, long times, long endTime,
									Class<? extends TimerHandle> name, Bean customData) throws Exception {
		return scheduleOnlineHot(account, clientId,
				cron, times, endTime, name, customData, "");
	}

	public String scheduleOnlineHot(String account, String clientId, String cron, long times, long endTime,
									Class<? extends TimerHandle> name, Bean customData, String oneByOneKey) throws Exception {
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOnlineHot(account, clientId, "@" + timer.timerIdAutoKey.nextString(), cronTimer, name, customData);
	}

	private String scheduleOnline(String account, String clientId, String timerId, BCronTimer cronTimer,
								  TimerHandle name, Bean customData) {
		var loginVersion = online.getLocalLoginVersion(account, clientId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BArchOnlineTimer(account, clientId, loginVersion, timer.timerSerialId.nextId());
		timer.tAccountTimers().insert(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(cronTimer);

		var timerIds = online.getOrAddLocalBean(account, clientId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (null != customData) {
			Timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleCron(timerId, cronTimer, name);
		return timerId;
	}

	private String scheduleOnlineHot(String account, String clientId, String timerId, BCronTimer cronTimer,
									 Class<? extends TimerHandle> name, Bean customData) {
		var loginVersion = online.getLocalLoginVersion(account, clientId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BArchOnlineTimer(account, clientId, loginVersion, timer.timerSerialId.nextId());
		timer.tAccountTimers().insert(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(cronTimer);

		var timerIds = online.getOrAddLocalBean(account, clientId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (null != customData) {
			Timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleCronHot(timerId, cronTimer, name);
		return timerId;
	}

	public boolean cancel(String timerId) {
		var timer = online.providerApp.zeze.getTimer();
		// always cancel future task. 第一步就做这个。
		timer.cancelFuture(timerId);

		// remove online timer
		var bTimer = timer.tAccountTimers().get(timerId);
		if (null == bTimer)
			return false;

		// remove online local
		var onlineTimers = (BOnlineTimers)online.getLocalBean(bTimer.getAccount(), bTimer.getClientId(), eOnlineTimers);
		if (null != onlineTimers) {
			onlineTimers.getTimerIds().remove(timerId);
			if (onlineTimers.getTimerIds().isEmpty())
				online.removeLocalBean(bTimer.getAccount(), bTimer.getClientId(), eOnlineTimers);
		}
		// always remove.
		timer.tAccountTimers().remove(timerId);

		return true;
	}

	public boolean scheduleOfflineNamed(String timerName, String account, String clientId,
										long delay, long period, long times, long endTime, int missFirePolicy,
										Class<? extends TimerHandle> handleClassName, Bean customData) {
		var timer = online.providerApp.zeze.getTimer();
		var timerIndex = timer.tIndexs().get(timerName);
		if (null != timerIndex)
			return false;

		scheduleOffline(timerName, account, clientId, delay, period, times, endTime, missFirePolicy,
				handleClassName, customData, "");
		return true;
	}

	public boolean scheduleOfflineNamed(String timerName, String account, String clientId,
										long delay, long period, long times, long endTime, int missFirePolicy,
										Class<? extends TimerHandle> handleClassName, Bean customData,
										String oneByOneKey) {
		var timer = online.providerApp.zeze.getTimer();
		var timerIndex = timer.tIndexs().get(timerName);
		if (null != timerIndex)
			return false;

		scheduleOffline(timerName, account, clientId, delay, period, times, endTime, missFirePolicy,
				handleClassName, customData, oneByOneKey);
		return true;
	}

	public String scheduleOffline(String account, String clientId,
								  long delay, long period, long times, long endTime, int missFirePolicy,
								  Class<? extends TimerHandle> handleClassName, Bean customData) {
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOffline("@" + timer.timerIdAutoKey.nextString(),
				account, clientId, delay, period, times, endTime, missFirePolicy,
				handleClassName, customData, "");
	}

	public String scheduleOffline(String account, String clientId,
								  long delay, long period, long times, long endTime, int missFirePolicy,
								  Class<? extends TimerHandle> handleClassName, Bean customData,
								  String oneByOneKey) {
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOffline("@" + timer.timerIdAutoKey.nextString(),
				account, clientId, delay, period, times, endTime, missFirePolicy,
				handleClassName, customData, oneByOneKey);
	}

	private String scheduleOffline(String timerId, String account, String clientId,
								   long delay, long period, long times, long endTime, int missFirePolicy,
								   Class<? extends TimerHandle> handleClassName, Bean customData, String oneByOneKey) {
		var logoutVersion = online.getLogoutVersion(account, clientId);
		if (null == logoutVersion)
			throw new IllegalStateException("not logout. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineAccountCustom("", account, clientId, logoutVersion, handleClassName.getName());
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		simpleTimer.setMissfirePolicy(missFirePolicy);
		var timerName = timer.schedule(timerId, simpleTimer, OfflineHandle.class, custom);
		custom.setTimerName(timerName); // 没办法，循环依赖了，只能在这里设置。
		if (null != customData) {
			Timer.register(customData.getClass());
			custom.getCustomData().setBean(customData);
			timer.tryRecordBeanHotModuleWhileCommit(customData);
		}
		var offline = timer.tAccountOfflineTimers().getOrAdd(new BAccountClientId(account, clientId));
		if (offline.getOfflineTimers().size() > timer.zeze.getConfig().getOfflineTimerLimit())
			throw new IllegalStateException("too many offline timers. account="
					+ account + " clientId=" + clientId + " size=" + offline.getOfflineTimers().size());

		if (null != offline.getOfflineTimers().putIfAbsent(timerName, timer.zeze.getConfig().getServerId()))
			throw new IllegalStateException("duplicate timerName. account=" + account + " clientId=" + clientId);
		return timerName;
	}

	public boolean scheduleOfflineNamed(String timerName, String account, String clientId, String cron,
										long times, long endTime, int missFirePolicy,
										Class<? extends TimerHandle> handleClassName, Bean customData) throws ParseException {
		var timer = online.providerApp.zeze.getTimer();
		var timerIndex = timer.tIndexs().get(timerName);
		if (null != timerIndex)
			return false;
		scheduleOffline(timerName, account, clientId, cron, times, endTime, missFirePolicy,
				handleClassName, customData, "");
		return true;
	}

	public boolean scheduleOfflineNamed(String timerName, String account, String clientId, String cron,
										long times, long endTime, int missFirePolicy,
										Class<? extends TimerHandle> handleClassName, Bean customData,
										String oneByOneKey) throws ParseException {
		var timer = online.providerApp.zeze.getTimer();
		var timerIndex = timer.tIndexs().get(timerName);
		if (null != timerIndex)
			return false;
		scheduleOffline(timerName, account, clientId, cron, times, endTime, missFirePolicy,
				handleClassName, customData, oneByOneKey);
		return true;
	}

	public String scheduleOffline(String account, String clientId, String cron, long times, long endTime,
								  int missFirePolicy,
								  Class<? extends TimerHandle> handleClassName, Bean customData) throws ParseException {
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOffline("@" + timer.timerIdAutoKey.nextString(),
				account, clientId, cron, times, endTime, missFirePolicy, handleClassName, customData, "");
	}

	public String scheduleOffline(String account, String clientId, String cron, long times, long endTime,
								  int missFirePolicy,
								  Class<? extends TimerHandle> handleClassName, Bean customData,
								  String oneByOneKey) throws ParseException {
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOffline("@" + timer.timerIdAutoKey.nextString(),
				account, clientId, cron, times, endTime, missFirePolicy, handleClassName, customData, oneByOneKey);
	}

	private String scheduleOffline(String timerId, String account, String clientId, String cron, long times, long endTime,
								   int missFirePolicy,
								   Class<? extends TimerHandle> handleClassName, Bean customData,
								   String oneByOneKey) throws ParseException {
		var logoutVersion = online.getLogoutVersion(account, clientId);
		if (null == logoutVersion)
			throw new IllegalStateException("not logout. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineAccountCustom("", account, clientId, logoutVersion, handleClassName.getName());
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		cronTimer.setMissfirePolicy(missFirePolicy);
		var timerName = timer.schedule(timerId, cronTimer, OfflineHandle.class, custom);
		custom.setTimerName(timerName); // 没办法，循环依赖了，只能在这里设置。
		if (null != customData) {
			Timer.register(customData.getClass());
			custom.getCustomData().setBean(customData);
		}
		var offline = timer.tAccountOfflineTimers().getOrAdd(new BAccountClientId(account, clientId));
		if (offline.getOfflineTimers().size() > timer.zeze.getConfig().getOfflineTimerLimit())
			throw new IllegalStateException("too many offline timers. account="
					+ account + " clientId=" + clientId + " size=" + offline.getOfflineTimers().size());

		if (null != offline.getOfflineTimers().putIfAbsent(timerName, timer.zeze.getConfig().getServerId()))
			throw new IllegalStateException("duplicate timerName. account=" + account + " clientId=" + clientId);
		return timerName;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 内部实现
	public static class OfflineHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) throws Exception {
			var offlineCustom = (BOfflineAccountCustom)context.customData;
			//noinspection DataFlowIssue
			var loginVersion = context.timer.getAccountTimer().online
					.getLoginVersion(offlineCustom.getAccount(), offlineCustom.getClientId());
			// 检查版本号，不正确的登录版本号表示过期的timer，取消掉即可。
			if (null != loginVersion && loginVersion == offlineCustom.getLoginVersion()) {
				@SuppressWarnings("unchecked")
				var handleClass = (Class<? extends TimerHandle>)Class.forName(offlineCustom.getHandleName());
				final var handle = handleClass.getDeclaredConstructor().newInstance();
				context.account = offlineCustom.getAccount();
				context.clientId = offlineCustom.getClientId();
				context.customData = offlineCustom.getCustomData().getBean();
				handle.onTimer(context);
			} else {
				context.timer.cancel(offlineCustom.getTimerName());
				var offlineTimers = context.timer.tAccountOfflineTimers()
						.get(new BAccountClientId(offlineCustom.getAccount(), offlineCustom.getClientId()));
				if (null == offlineTimers)
					throw new IllegalStateException("maybe operate before timer created.");
				offlineTimers.getOfflineTimers().remove(offlineCustom.getTimerName());
			}
		}

		@Override
		public void onTimerCancel() {
		}
	}

	private long onLoginEvent(Object sender, EventDispatcher.EventArgument arg) {
		var timer = online.providerApp.zeze.getTimer();
		var loginArg = (LoginArgument)arg;
		var loginKey = new BAccountClientId(loginArg.account, loginArg.clientId);
		var offlineTimers = timer.tAccountOfflineTimers().get(loginKey);
		// X: fix offlineTimers is null
		if (null == offlineTimers)
			return 0;
		for (var e : offlineTimers.getOfflineTimers().entrySet()) {
			timer.cancel(e.getKey());
		}
		// 嵌入本地服务器事件事务中，
		// 删除之后，如果上面的redirectCancel失败，
		// 那么该timer触发的时候会检测到版本号不一致，
		// 然后timer最终也会被cancel掉。
		timer.tAccountOfflineTimers().remove(loginKey);
		return 0;
	}

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(Object sender, EventDispatcher.EventArgument arg) {
		var local = (LocalRemoveEventArgument)arg;
		if (null != local.local) {
			var bAny = local.local.getDatas().get(eOnlineTimers);
			if (null != bAny) {
				var timers = (BOnlineTimers)bAny.getAny().getBean();
				for (var timerId : timers.getTimerIds().keySet())
					cancel(timerId);
			}
		}
		return 0;
	}

	// 调度 cron 定时器
	private void scheduleCron(String timerId, BCronTimer cron, TimerHandle handle) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(timerId, delay, handle);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	private void scheduleCronHot(String timerId, BCronTimer cron, Class<? extends TimerHandle> handle) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNextHot(timerId, delay, handle);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	// 再次调度 cron 定时器，真正安装到ThreadPool中。
	private void scheduleCronNext(String timerId, long delay, TimerHandle handle) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireCron(timerId, handle, false))));
	}

	private void scheduleCronNextHot(String timerId, long delay, Class<? extends TimerHandle> handle) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireCron(timerId, timer.findTimerHandle(handle.getName()), true))));
	}

	private void fireCron(@NotNull String timerId, @Nullable TimerHandle handle, boolean hot) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (handle == null) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = timer.tAccountTimers().get(timerId);
			if (bTimer == null) {
				timer.cancelFuture(timerId);
				return 0; // done
			}

			var loginVersion = online.getLoginVersion(bTimer.getAccount(), bTimer.getClientId());
			if (loginVersion == null || bTimer.getLoginVersion() != loginVersion) {
				// 已经不是注册定时器时候的登录了。
				timer.cancelFuture(timerId);
				return 0; // done
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
			}, "TimerAccount.fireOnlineLocalHandle"));

			var bTimerNew = timer.tAccountTimers().get(timerId);
			if (bTimerNew == null || bTimerNew.getSerialId() != serialSaved)
				return 0; // canceled or new timer

			if (retNest == Procedure.Exception) {
				cancel(timerId); // 异常错误不忽略。
				return 0;
			}
			// skip other error
			if (!hasNext) {
				cancel(timerId);
				return 0; // procedure done
			}

			// continue period
			long delay = cronTimer.getNextExpectedTime() - System.currentTimeMillis();
			if (hot)
				scheduleCronNextHot(timerId, delay, handle.getClass());
			else
				scheduleCronNext(timerId, delay, handle);
			return 0;
		}, "TimerAccount.fireOnlineSimpleTimer"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "TimerAccount finally cancel impossible!"));
		}
	}

	// 调度 Simple 定时器到ThreadPool中。
	private void scheduleSimple(String timerId, long delay, TimerHandle handle) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireSimple(timerId, handle, false))));
	}

	private void scheduleSimpleHot(String timerId, long delay, Class<? extends TimerHandle> handle) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireSimple(timerId, timer.findTimerHandle(handle.getName()), true))));
	}

	// Timer发生，执行回调。
	private void fireSimple(@NotNull String timerId, @Nullable TimerHandle handle, boolean hot) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (handle == null) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = timer.tAccountTimers().get(timerId);
			if (bTimer == null) {
				timer.cancelFuture(timerId);
				return 0; // done
			}
			var loginVersion = online.getLoginVersion(bTimer.getAccount(), bTimer.getClientId());
			if (loginVersion == null || bTimer.getLoginVersion() != loginVersion) {
				// 已经不是注册定时器时候的登录了。
				timer.cancelFuture(timerId);
				return 0; // done
			}

			var simpleTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BSimpleTimer();
			var serialSaved = bTimer.getSerialId();
			var hasNext = Timer.nextSimpleTimer(simpleTimer, false);
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				var localBean = online.<BOnlineTimers>getLocalBean(bTimer.getAccount(), bTimer.getClientId(), eOnlineTimers);
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
			}, "TimerAccount.fireOnlineLocalHandle"));

			var bTimerNew = timer.tAccountTimers().get(timerId);
			if (bTimerNew == null || bTimerNew.getSerialId() != serialSaved)
				return 0; // canceled or new timer

			if (retNest == Procedure.Exception) {
				cancel(timerId); // 异常错误不忽略。
				return 0;
			}
			// 其他错误忽略

			// 准备下一个间隔
			if (!hasNext) {
				cancel(timerId);
				return 0; // procedure done
			}

			// continue period
			var delay = simpleTimer.getNextExpectedTime() - System.currentTimeMillis();
			if (hot)
				scheduleSimpleHot(timerId, delay, handle.getClass());
			else
				scheduleSimple(timerId, delay, handle);
			return 0; // last procedure done
		}, "TimerAccount.fireOnlineSimpleTimer"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "TimerAccount finally cancel impossible!"));
		}
	}
}
