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
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
import Zeze.Util.Task;

/**
 * 1. schedule，scheduleNamed 完全重新实现一套基于内存表和内存的。
 * 2. 不直接使用 Timer.schedule。但有如下关联。
 * 直接使用 Timer.timerIdAutoKey，使得返回的timerId共享一个名字空间。
 * 直接使用 Timer.timersFuture，从 ThreadPool 返回的future保存在这里。
 */
public class TimerRole {
	final Online online;

	//public final static String eTimerHandleName = "Zeze.Component.TimerGameOnline.Handle";
	public final static String eOnlineTimers = "Zeze.Component.TimerGameOnline";

	TimerRole(Online online) {
		this.online = online;

		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().getRunEmbedEvents().offer(this::onLocalRemoveEvent);
		online.getReloginEvents().getRunEmbedEvents().offer(this::onReloginEvent);
		online.getLoginEvents().getRunEmbedEvents().offer(this::onLoginEvent);
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(long roleId, String timerName,
									   long delay, long period, long times, long endTime,
									   TimerHandle handleName, Bean customData) {
		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.tRoleTimers().get(timerName);
		if (null != timerId)
			return false;
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime);
		scheduleOnline(roleId, timerName, simpleTimer, handleName, customData);
		return true;
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(long roleId, String timerName,
									   String cron, long times, long endTime,
									   TimerHandle handleName, Bean customData) throws Exception {
		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.tRoleTimers().get(timerName);
		if (null != timerId)
			return false;
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime);
		scheduleOnline(roleId, timerName, cronTimer, handleName, customData);
		return true;
	}

	public String scheduleOnline(long roleId, long delay, long period, long times, long endTime,
								 TimerHandle name, Bean customData) {
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime);
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOnline(roleId, "@" + timer.timerIdAutoKey.nextString(), simpleTimer, name, customData);
	}

	private String scheduleOnline(long roleId, String timerId, BSimpleTimer simpleTimer,
								  TimerHandle name, Bean customData) {
		// 去掉下面两行，不允许在非登录状态注册timer。现在允许。
		var loginVersion = online.getLocalLoginVersion(roleId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion);
		timer.tRoleTimers().put(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(simpleTimer);

		if (null != customData) {
			timer.register(customData.getClass());
			var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
			timerIds.getTimerIds().getOrAdd(timerId).getCustomData().setBean(customData);
		}
		scheduleSimple(timerId, simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), name);
		return timerId;
	}

	public String scheduleOnline(long roleId, String cron, long times, long endTime,
								 TimerHandle name, Bean customData) throws Exception {
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime);
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOnline(roleId, "@" + timer.timerIdAutoKey.nextString(), cronTimer, name, customData);
	}

	private String scheduleOnline(long roleId, String timerId, BCronTimer cronTimer,
								  TimerHandle name, Bean customData) {
		var loginVersion = online.getLocalLoginVersion(roleId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion);
		onlineTimer.getTimerObj().setBean(cronTimer);
		timer.tRoleTimers().insert(timerId, onlineTimer);

		if (null != customData) {
			timer.register(customData.getClass());
			var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
			timerIds.getTimerIds().getOrAdd(timerId).getCustomData().setBean(customData);
		}
		scheduleCron(timerId, cronTimer, name);
		return timerId;
	}

	public boolean cancel(String timerId) {
		var timer = online.providerApp.zeze.getTimer();

		// remove online timer
		var bTimer = timer.tRoleTimers().get(timerId);
		if (null == bTimer)
			return false;

		// remove online local
		var onlineTimers = online.getOrAddLocalBean(bTimer.getRoleId(), eOnlineTimers, new BOnlineTimers());
		onlineTimers.getTimerIds().remove(timerId);
		timer.tRoleTimers().remove(timerId);

		// cancel future task
		timer.cancelFuture(timerId);
		return true;
	}

	public String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
								  Class<? extends TimerHandle> handleClassName, Bean customData) {
		var logoutVersion = online.getLogoutVersion(roleId);
		if (null == logoutVersion)
			throw new IllegalStateException("not logout. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineRoleCustom("", roleId, logoutVersion, handleClassName.getName());
		var timerName = timer.schedule(delay, period, times, endTime,
				Timer.eMissfirePolicyNothing, OfflineHandle.class, custom);
		custom.setTimerName(timerName); // 没办法，循环依赖了，只能在这里设置。
		if (null != customData) {
			timer.register(customData.getClass());
			custom.getCustomData().setBean(customData);
		}
		var offline = timer.tRoleOfflineTimers().getOrAdd(roleId);
		if (offline.getOfflineTimers().size() > timer.zeze.getConfig().getOfflineTimerLimit())
			throw new IllegalStateException("too many offline timers. roleId=" + roleId + " size=" + offline.getOfflineTimers().size());

		if (null != offline.getOfflineTimers().putIfAbsent(timerName, timer.zeze.getConfig().getServerId()))
			throw new IllegalStateException("duplicate timerName. roleId=" + roleId);
		return timerName;
	}

	public String scheduleOffline(long roleId, String cron, long times, long endTime,
								  Class<? extends TimerHandle> handleClassName, Bean customData) throws ParseException {
		var logoutVersion = online.getLogoutVersion(roleId);
		if (null == logoutVersion)
			throw new IllegalStateException("not logout. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineRoleCustom("", roleId, logoutVersion, handleClassName.getName());
		var timerName = timer.schedule(cron, times, endTime,
				Timer.eMissfirePolicyNothing, OfflineHandle.class, custom);
		custom.setTimerName(timerName); // 没办法，循环依赖了，只能在这里设置。
		if (null != customData) {
			timer.register(customData.getClass());
			custom.getCustomData().setBean(customData);
		}
		var offline = timer.tRoleOfflineTimers().getOrAdd(roleId);
		if (offline.getOfflineTimers().size() > timer.zeze.getConfig().getOfflineTimerLimit())
			throw new IllegalStateException("too many offline timers. roleId=" + roleId + " size=" + offline.getOfflineTimers().size());

		if (null != offline.getOfflineTimers().putIfAbsent(timerName, timer.zeze.getConfig().getServerId()))
			throw new IllegalStateException("duplicate timerName. roleId=" + roleId);
		return timerName;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 内部实现
	public static class OfflineHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) throws Exception {
			var offlineCustom = (BOfflineRoleCustom)context.customData;
			var loginVersion = context.timer.getRoleTimer().online.getLoginVersion(offlineCustom.getRoleId());
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
				var offlineTimers = context.timer.tRoleOfflineTimers().get(offlineCustom.getRoleId());
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
		var offlineTimers = timer.tRoleOfflineTimers().get(loginArg.roleId);
		// X: fix offlineTimers is null
		if (null == offlineTimers)
			return 0;
		for (var e : offlineTimers.getOfflineTimers().entrySet()) {
			timer.cancelTryRedirect(e.getValue(), e.getKey());
		}
		// 嵌入本地服务器事件事务中，
		// 删除之后，如果上面的redirectCancel失败，
		// 那么该timer触发的时候会检测到版本号不一致，
		// 然后timer最终也会被cancel掉。
		timer.tRoleOfflineTimers().remove(loginArg.roleId);
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

	// relogin 时需要更新已经注册的定时器的版本号。
	private long onReloginEvent(Object sender, EventDispatcher.EventArgument arg) {
		var user = (LoginArgument)arg;
		var timer = online.providerApp.zeze.getTimer();

		var loginVersion = online.getGlobalLoginVersion(user.roleId);
		var timers = online.<BOnlineTimers>getLocalBean(user.roleId, eOnlineTimers);
		if (null != timers) {
			// XXX
			// 这里有个问题，如果在线定时器很多，这个嵌到relogin-procedure中的事务需要更新很多记录。
			// 如果启动新的事务执行更新，又会破坏原子性。
			// 先整体在一个事务内更新，这样更安全。
			// 由于Online Timer是本进程的，用户也不会修改，所以整体更新目前看来还可接受。
			for (var tid : timers.getTimerIds().keySet()) {
				timer.tRoleTimers().get(tid).setLoginVersion(loginVersion);
			}
		}
		return 0;
	}

	// 调度 cron 定时器
	private void scheduleCron(String timerId, BCronTimer cron, TimerHandle name) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(timerId, delay, name);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	// 再次调度 cron 定时器，真正安装到ThreadPool中。
	private void scheduleCronNext(String timerId, long delay, TimerHandle name) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireCron(timerId, name))));
	}

	private void fireCron(String timerId, TimerHandle handle) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = timer.tRoleTimers().get(timerId);
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
			{
				var onlineTimers = online.<BOnlineTimers>getLocalBean(bTimer.getRoleId(), eOnlineTimers);
				if (null != onlineTimers) {
					var cTimer = onlineTimers.getTimerIds().get(timerId);
					if (null != cTimer)
						customData = cTimer.getCustomData().getBean();
				}
			}
			var context = new TimerContext(timer, timerId, handle.getClass().getName(), customData,
					cronTimer.getHappenTime(), cronTimer.getNextExpectedTime(),
					cronTimer.getExpectedTime());
			context.roleId = bTimer.getRoleId();
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				handle.onTimer(context);
				return Procedure.Success;
			}, "fireOnlineLocalHandle"));
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
		}, "fireOnlineSimpleTimer"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "TimerRole finally cancel impossible!"));
		}
	}

	// 调度 Simple 定时器到ThreadPool中。
	private void scheduleSimple(String timerId, long delay, TimerHandle name) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireOnlineSimpleTimer(timerId, name))));
	}

	// Timer发生，执行回调。
	private void fireOnlineSimpleTimer(String timerId, TimerHandle handle) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = timer.tRoleTimers().get(timerId);
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
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				Bean customData = null;
				{
					var onlineTimers = online.<BOnlineTimers>getLocalBean(bTimer.getRoleId(), eOnlineTimers);
					if (null != onlineTimers) {
						var cTimer = onlineTimers.getTimerIds().get(timerId);
						if (null != cTimer)
							customData = cTimer.getCustomData().getBean();
					}
				}
				var context = new TimerContext(timer, timerId, handle.getClass().getName(), customData,
						simpleTimer.getHappenTimes(), simpleTimer.getNextExpectedTime(),
						simpleTimer.getExpectedTime());
				context.roleId = bTimer.getRoleId();
				handle.onTimer(context);
				return Procedure.Success;
			}, "fireOnlineLocalHandle"));
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
		}, "fireOnlineSimpleTimer"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "TimerRole finally cancel impossible!"));
		}
	}
}
