package Zeze.Component;

import java.text.ParseException;
import Zeze.Builtin.Timer.BOfflineRoleCustom;
import Zeze.Game.LocalRemoveEventArgument;
import Zeze.Game.LoginArgument;
import Zeze.Game.Online;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BGameOnlineTimer;
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
public class TimerRole {
	final @NotNull Online online;

	//public static final String eTimerHandleName = "Zeze.Component.TimerGameOnline.Handle";
	public static final String eOnlineTimers = "Zeze.Component.TimerGameOnline";

	public TimerRole(@NotNull Online online) {
		this.online = online;

		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().getRunEmbedEvents().offer(this::onLocalRemoveEvent);
		online.getLoginEvents().getRunEmbedEvents().offer(this::onLoginEvent);
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerName,
									   long delay, long period, long times, long endTime,
									   @Nullable TimerHandle handleName, @Nullable Bean customData) {
		var timerId = online._tRoleTimers().get(timerName);
		if (null != timerId)
			return false;
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime);
		scheduleOnline(roleId, timerName, simpleTimer, handleName, customData);
		return true;
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerName,
									   @NotNull String cron, long times, long endTime,
									   @Nullable TimerHandle handleName, @Nullable Bean customData) throws Exception {
		var timerId = online._tRoleTimers().get(timerName);
		if (null != timerId)
			return false;
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime);
		scheduleOnline(roleId, timerName, cronTimer, handleName, customData);
		return true;
	}

	public @NotNull String scheduleOnline(long roleId, long delay, long period, long times, long endTime,
										  @Nullable TimerHandle name, @Nullable Bean customData) {
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime);
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOnline(roleId, "@" + timer.timerIdAutoKey.nextString(), simpleTimer, name, customData);
	}

	private @NotNull String scheduleOnline(long roleId, @NotNull String timerId, @NotNull BSimpleTimer simpleTimer,
										   @Nullable TimerHandle name, @Nullable Bean customData) {
		// 去掉下面两行，不允许在非登录状态注册timer。现在允许。
		var loginVersion = online.getLocalLoginVersion(roleId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion, timer.timerSerialId.nextId());
		online._tRoleTimers().put(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(simpleTimer);

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (null != customData) {
			timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleSimple(timerId, simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), name);
		return timerId;
	}

	public @NotNull String scheduleOnline(long roleId, @NotNull String cron, long times, long endTime,
										  @Nullable TimerHandle name, @Nullable Bean customData) throws Exception {
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime);
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOnline(roleId, "@" + timer.timerIdAutoKey.nextString(), cronTimer, name, customData);
	}

	private @NotNull String scheduleOnline(long roleId, @NotNull String timerId, @NotNull BCronTimer cronTimer,
										   @Nullable TimerHandle name, @Nullable Bean customData) {
		var loginVersion = online.getLocalLoginVersion(roleId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion, timer.timerSerialId.nextId());
		onlineTimer.getTimerObj().setBean(cronTimer);
		online._tRoleTimers().insert(timerId, onlineTimer);

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (null != customData) {
			timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleCron(timerId, cronTimer, name);
		return timerId;
	}

	public boolean cancel(@NotNull String timerId) {
		var timer = online.providerApp.zeze.getTimer();

		// remove online timer
		var bTimer = online._tRoleTimers().get(timerId);
		if (null == bTimer)
			return false;

		// remove online local
		var onlineTimers = online.getOrAddLocalBean(bTimer.getRoleId(), eOnlineTimers, new BOnlineTimers());
		onlineTimers.getTimerIds().remove(timerId);
		online._tRoleTimers().remove(timerId);

		// cancel future task
		timer.cancelFuture(timerId);
		return true;
	}

	public @NotNull boolean scheduleOfflineNamed(@NotNull String timerName, long roleId, long delay, long period,
												 long times, long endTime, int missFirePolicy,
												 @NotNull Class<? extends TimerHandle> handleClassName,
												 @Nullable Bean customData) {
		var timer = online.providerApp.zeze.getTimer();
		var timerIndex = timer.tIndexs().get(timerName);
		if (null != timerIndex)
			return false;
		scheduleOffline(timerName, roleId, delay, period, times, endTime, missFirePolicy, handleClassName, customData);
		return true;
	}

	private @NotNull String scheduleOffline(@NotNull String timerId, long roleId, long delay, long period,
											long times, long endTime, int missFirePolicy,
											@NotNull Class<? extends TimerHandle> handleClassName,
											@Nullable Bean customData) {

		var logoutVersion = online.getLogoutVersion(roleId);
		if (null == logoutVersion)
			throw new IllegalStateException("not logout. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineRoleCustom("", roleId, logoutVersion, handleClassName.getName(), online.getOnlineSetName());

		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime);
		simpleTimer.setMissfirePolicy(missFirePolicy);
		var timerName = timer.schedule(timerId, simpleTimer, OfflineHandle.class, custom);

		custom.setTimerName(timerName); // 没办法，循环依赖了，只能在这里设置。
		if (null != customData) {
			timer.register(customData.getClass());
			custom.getCustomData().setBean(customData);
		}
		var offline = online._tRoleOfflineTimers().getOrAdd(roleId);
		if (offline.getOfflineTimers().size() > timer.zeze.getConfig().getOfflineTimerLimit())
			throw new IllegalStateException("too many offline timers. roleId=" + roleId + " size=" + offline.getOfflineTimers().size());

		if (null != offline.getOfflineTimers().putIfAbsent(timerName, timer.zeze.getConfig().getServerId()))
			throw new IllegalStateException("duplicate timerName. roleId=" + roleId);
		return timerName;
	}

	public @NotNull String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
										   int missFirePolicy, @NotNull Class<? extends TimerHandle> handleClassName,
										   @Nullable Bean customData) {
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOffline("@" + timer.timerIdAutoKey.nextString(),
				roleId, delay, period, times, endTime, missFirePolicy,
				handleClassName, customData);
	}

	public @NotNull String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
										   @NotNull Class<? extends TimerHandle> handleClassName,
										   @Nullable Bean customData) {
		return scheduleOffline(roleId, delay, period, times, endTime, Timer.eMissfirePolicyNothing, handleClassName, customData);
	}

	public @NotNull boolean scheduleOfflineNamed(@NotNull String timerName, long roleId, @NotNull String cron,
												 long times, long endTime, int missFirePolicy,
												 @NotNull Class<? extends TimerHandle> handleClassName,
												 @Nullable Bean customData) throws ParseException {
		var timer = online.providerApp.zeze.getTimer();
		var timerIndex = timer.tIndexs().get(timerName);
		if (null != timerIndex)
			return false;

		scheduleOffline(timerName, roleId, cron, times, endTime, missFirePolicy, handleClassName, customData);
		return true;
	}

	private @NotNull String scheduleOffline(@NotNull String timerId, long roleId, @NotNull String cron, long times,
											long endTime, int missFirePolicy,
											@NotNull Class<? extends TimerHandle> handleClassName,
											@Nullable Bean customData) throws ParseException {
		var logoutVersion = online.getLogoutVersion(roleId);
		if (null == logoutVersion)
			throw new IllegalStateException("not logout. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineRoleCustom("", roleId, logoutVersion, handleClassName.getName(), online.getOnlineSetName());
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime);
		cronTimer.setMissfirePolicy(missFirePolicy);
		var timerName = timer.schedule(timerId, cronTimer, OfflineHandle.class, custom);
		custom.setTimerName(timerName); // 没办法，循环依赖了，只能在这里设置。
		if (null != customData) {
			timer.register(customData.getClass());
			custom.getCustomData().setBean(customData);
		}
		var offline = online._tRoleOfflineTimers().getOrAdd(roleId);
		if (offline.getOfflineTimers().size() > timer.zeze.getConfig().getOfflineTimerLimit())
			throw new IllegalStateException("too many offline timers. roleId=" + roleId + " size=" + offline.getOfflineTimers().size());

		if (null != offline.getOfflineTimers().putIfAbsent(timerName, timer.zeze.getConfig().getServerId()))
			throw new IllegalStateException("duplicate timerName. roleId=" + roleId);
		return timerName;
	}

	public @NotNull String scheduleOffline(long roleId, @NotNull String cron, long times, long endTime,
										   int missFirePolicy, @NotNull Class<? extends TimerHandle> handleClassName,
										   @Nullable Bean customData) throws ParseException {
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOffline("@" + timer.timerIdAutoKey.nextString(),
				roleId, cron, times, endTime, missFirePolicy, handleClassName, customData);
	}

	public @NotNull String scheduleOffline(long roleId, @NotNull String cron, long times, long endTime,
										   @NotNull Class<? extends TimerHandle> handleClassName,
										   @Nullable Bean customData) throws ParseException {
		return scheduleOffline(roleId, cron, times, endTime, Timer.eMissfirePolicyNothing, handleClassName, customData);
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 内部实现
	public static class OfflineHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var offlineCustom = (BOfflineRoleCustom)context.customData;
			var loginVersion = context.timer.getRoleTimer(offlineCustom.getOnlineSetName()).online.getLoginVersion(offlineCustom.getRoleId());
			// 检查版本号，不正确的登录版本号表示过期的timer，取消掉即可。
			if (null != loginVersion && loginVersion == offlineCustom.getLoginVersion()) {
				@SuppressWarnings("unchecked")
				var handleClass = (Class<? extends TimerHandle>)Class.forName(offlineCustom.getHandleName());
				final var handle = handleClass.getDeclaredConstructor().newInstance();
				context.roleId = offlineCustom.getRoleId();
				context.customData = offlineCustom.getCustomData().getBean();
				handle.onTimer(context);
			} else {
				context.timer.cancel(offlineCustom.getTimerName());
				var online = context.timer.getDefaultOnline().getOnlineSet(offlineCustom.getOnlineSetName());
				if (online != null) {
					var offlineTimers = online._tRoleOfflineTimers().get(offlineCustom.getRoleId());
					if (offlineTimers != null)
						offlineTimers.getOfflineTimers().remove(offlineCustom.getTimerName());
				}
			}
		}

		@Override
		public void onTimerCancel() {
		}
	}

	private long onLoginEvent(@NotNull Object sender, @NotNull EventDispatcher.EventArgument arg) {
		var timer = online.providerApp.zeze.getTimer();
		var loginArg = (LoginArgument)arg;
		var offlineTimers = online._tRoleOfflineTimers().get(loginArg.roleId);
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
		online._tRoleOfflineTimers().remove(loginArg.roleId);
		return 0;
	}

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(@NotNull Object sender, @NotNull EventDispatcher.EventArgument arg) {
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
	private void scheduleCron(@NotNull String timerId, @NotNull BCronTimer cron, @Nullable TimerHandle name) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(timerId, delay, name);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	// 再次调度 cron 定时器，真正安装到ThreadPool中。
	private void scheduleCronNext(@NotNull String timerId, long delay, @Nullable TimerHandle name) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireCron(timerId, name))));
	}

	private void fireCron(@NotNull String timerId, @Nullable TimerHandle handle) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = online._tRoleTimers().get(timerId);
			if (null == bTimer) {
				timer.cancelFuture(timerId);
				return 0; // done
			}
			var globalLoginVersion = online.getGlobalLoginVersion(bTimer.getRoleId());
			if (null == globalLoginVersion || bTimer.getLoginVersion() != globalLoginVersion) {
				// 已经不是注册定时器时候的登录了。
				timer.cancelFuture(timerId);
				return 0; // done
			}

			var cronTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BCronTimer();
			Bean customData = null;
			var localBean = online.<BOnlineTimers>getLocalBean(bTimer.getRoleId(), eOnlineTimers);
			if (localBean != null) {
				var onlineCustom = localBean.getTimerIds().get(timerId);
				if (onlineCustom != null) {
					customData = onlineCustom.getCustomData().getBean();
					if (customData.typeId() == EmptyBean.TYPEID)
						customData = null;
				}
			}
			var context = new TimerContext(timer, timerId, handle.getClass().getName(), customData,
					cronTimer.getHappenTime(), cronTimer.getNextExpectedTime(),
					cronTimer.getExpectedTime());
			context.roleId = bTimer.getRoleId();
			var serialSaved = bTimer.getSerialId();
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				handle.onTimer(context);
				return Procedure.Success;
			}, "TimerRole.fireOnlineLocalHandle"));

			var bTimerNew = online._tRoleTimers().get(timerId);
			if (bTimerNew == null || bTimerNew.getSerialId() != serialSaved)
				return 0; // canceled or new timer

			if (retNest == Procedure.Exception) {
				cancel(timerId); // 异常错误不忽略。
				return 0;
			}
			// skip other error

			if (!Timer.nextCronTimer(cronTimer, false)) {
				cancel(timerId);
				return 0; // procedure done
			}

			// continue period
			long delay = cronTimer.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(timerId, delay, handle);
			return 0;
		}, "TimerRole.fireOnlineSimpleTimer"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "TimerRole finally cancel impossible!"));
		}
	}

	// 调度 Simple 定时器到ThreadPool中。
	private void scheduleSimple(@NotNull String timerId, long delay, @Nullable TimerHandle name) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireSimple(timerId, name))));
	}

	// Timer发生，执行回调。
	private void fireSimple(@NotNull String timerId, @Nullable TimerHandle handle) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = online._tRoleTimers().get(timerId);
			if (null == bTimer) {
				timer.cancelFuture(timerId);
				return 0; // done
			}
			var globalLoginVersion = online.getGlobalLoginVersion(bTimer.getRoleId());
			if (null == globalLoginVersion || bTimer.getLoginVersion() != globalLoginVersion) {
				// 已经不是注册定时器时候的登录了。
				timer.cancelFuture(timerId);
				return 0; // done
			}

			var simpleTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BSimpleTimer();
			var serialSaved = bTimer.getSerialId();
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				Bean customData = null;
				var localBean = online.<BOnlineTimers>getLocalBean(bTimer.getRoleId(), eOnlineTimers);
				if (localBean != null) {
					var onlineCustom = localBean.getTimerIds().get(timerId);
					if (onlineCustom != null) {
						customData = onlineCustom.getCustomData().getBean();
						if (customData.typeId() == EmptyBean.TYPEID)
							customData = null;
					}
				}
				var context = new TimerContext(timer, timerId, handle.getClass().getName(), customData,
						simpleTimer.getHappenTimes(), simpleTimer.getNextExpectedTime(),
						simpleTimer.getExpectedTime());
				context.roleId = bTimer.getRoleId();
				handle.onTimer(context);
				return Procedure.Success;
			}, "TimerRole.fireOnlineLocalHandle"));

			var bTimerNew = online._tRoleTimers().get(timerId);
			if (bTimerNew == null || bTimerNew.getSerialId() != serialSaved)
				return 0; // canceled or new timer

			if (retNest == Procedure.Exception) {
				cancel(timerId); // 异常错误不忽略。
				return 0;
			}
			// 其他错误忽略

			// 准备下一个间隔
			if (!Timer.nextSimpleTimer(simpleTimer, false)) {
				cancel(timerId);
				return 0;
			}

			// continue period
			var delay = simpleTimer.getNextExpectedTime() - System.currentTimeMillis();
			scheduleSimple(timerId, delay, handle);
			return 0;
		}, "TimerRole.fireOnlineSimpleTimer"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "TimerRole finally cancel impossible!"));
		}
	}
}
