package Zeze.Component;

import java.util.Calendar;
import Zeze.Builtin.Timer.BTimerId;
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
 *    直接使用 Timer.timerIdAutoKey，使得返回的timerId共享一个名字空间。
 *    直接使用 Timer.timersFuture，从 ThreadPool 返回的future保存在这里。
 * 3. cancel 用户入口从 Timer.calcel 调用。
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
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(long roleId, String timerName,
									   long delay, long period, long times,
									   String handleName, Bean customData) throws Throwable {
		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.tOnlineNamed().get(timerName);
		if (null != timerId)
			return false;
		timerId = new BTimerId(scheduleOnline(roleId, delay, period, times, handleName, customData));
		timer.tOnlineNamed().insert(timerName, timerId);
		timer.tGameOlineTimer().get(timerId.getTimerId()).setNamedName(timerName);
		return true;
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(long roleId, String timerName,
									   String cron, long times, String handleName, Bean customData) throws Throwable {
		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.tOnlineNamed().get(timerName);
		if (null != timerId)
			return false;
		timerId = new BTimerId(scheduleOnline(roleId, cron, times, handleName, customData));
		timer.tOnlineNamed().insert(timerName, timerId);
		timer.tGameOlineTimer().get(timerId.getTimerId()).setNamedName(timerName);
		return true;
	}

	public void cancelNamed(String timerName) throws Throwable {
		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.tOnlineNamed().get(timerName);
		cancel(timerId.getTimerId());
		timer.tOnlineNamed().remove(timerName);
	}

	public long scheduleOnline(long roleId, long delay, long period, long times, String name, Bean customData) throws Throwable {
		// 去掉下面两行，不允许在非登录状态注册timer。现在允许。
		var loginVersion = online.getLocalLoginVersion(roleId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.timerIdAutoKey.nextId();

		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion, "");
		timer.tGameOlineTimer().put(timerId, onlineTimer);
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times);
		onlineTimer.getTimerObj().setBean(simpleTimer);

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		timerIds.getTimerIds().getOrAdd(timerId).getCustomData().setBean(customData);

		Transaction.whileCommit(() -> scheduleSimple(timerId, delay, name));
		return timerId;
	}

	public long scheduleOnline(long roleId, String cron, long times, String name, Bean customData) throws Throwable {
		var loginVersion = online.getLocalLoginVersion(roleId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.timerIdAutoKey.nextId();

		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion, "");
		var cronTimer = new BCronTimer();
		cronTimer.setRemainTimes(times);
		onlineTimer.getTimerObj().setBean(cronTimer);
		timer.tGameOlineTimer().put(timerId, onlineTimer);

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		timerIds.getTimerIds().getOrAdd(timerId).getCustomData().setBean(customData);

		Transaction.whileCommit(() -> scheduleCronLocal(timerId, cron, name));
		return timerId;
	}

	public boolean cancel(long timerId) throws Throwable {
		var timer = online.providerApp.zeze.getTimer();

		// remove online timer
		var bTimer = timer.tGameOlineTimer().get(timerId);
		if (null == bTimer)
			return false;

		// remove online local
		var onlineTimers = online.getOrAddLocalBean(bTimer.getRoleId(), eOnlineTimers, new BOnlineTimers());
		onlineTimers.getTimerIds().remove(timerId);
		timer.tArchOlineTimer().remove(timerId);
		if (!bTimer.getNamedName().isEmpty())
			timer.tOnlineNamed().remove(bTimer.getNamedName());

		// cancel future task
		var future = online.providerApp.zeze.getTimer().timersFuture.remove(timerId);
		if (null == future)
			return false;
		future.cancel(true);
		return true;
	}

	public long scheduleOffline(long roleId) {
		return 0;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 内部实现

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(Object sender, EventDispatcher.EventArgument arg) throws Throwable {
		var local = (LocalRemoveEventArgument)arg;
		var timer = online.providerApp.zeze.getTimer();
		if (null != local.localData) {
			var bAny = local.localData.getDatas().get(eOnlineTimers);
			if (null != bAny) {
				var timers = (BOnlineTimers)bAny.getAny().getBean();
				for (var timerId : timers.getTimerIds().keySet())
					cancel(timerId);
				for (var name : timers.getNamedNames())
					timer.cancelNamed(name);
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
		// XXX
		// 这里有个问题，如果在线定时器很多，这个嵌到relogin-procedure中的事务需要更新很多记录。
		// 如果启动新的事务执行更新，又会破坏原子性。
		// 先整体在一个事务内更新，这样更安全。
		// 由于Online Timer是本进程的，用户也不会修改，所以整体更新目前看来还可接受。
		for (var tid : timers.getTimerIds().keySet()) {
			timer.tArchOlineTimer().get(tid).setLoginVersion(loginVersion);
		}
		return 0;
	}

	// 调度 cron 定时器
	private void scheduleCronLocal(long timerId, String cronExpression, String name) {
		try {
			long delay = Timer.getNextValidTimeAfter(cronExpression, Calendar.getInstance()).getTimeInMillis() - System.currentTimeMillis();
			scheduleCronNext( timerId, delay, name);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	// 再次调度 cron 定时器，真正安装到ThreadPool中。
	private void scheduleCronNext(long timerId, long delay, String name) {
		var timer = online.providerApp.zeze.getTimer();
		timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay, () -> fireCron(timerId, name)));
	}

	private void fireCron(long timerId, String name) throws Throwable {
		var timer = online.providerApp.zeze.getTimer();
		final var handle = timer.timerHandles.get(name);
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = timer.tGameOlineTimer().get(timerId);
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
			Timer.nextCronTimer(cronTimer);
			var onlineTimers = online.<BOnlineTimers>getLocalBean(bTimer.getRoleId(), eOnlineTimers);
			var customData = onlineTimers.getTimerIds().get(timerId).getCustomData().getBean();
			var context = new TimerContext(
					timerId, name, customData,
					cronTimer.getHappenTimeMills(),
					cronTimer.getNextExpectedTimeMills(),
					cronTimer.getExpectedTimeMills());
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				handle.run(context);
				return Procedure.Success;
			}, "fireOnlineLocalHandle"));
			if (retNest == Procedure.Exception)
				return retNest;
			// skip other error

			if (cronTimer.getRemainTimes() > 0) {
				cronTimer.setRemainTimes(cronTimer.getRemainTimes() - 1);
				if (cronTimer.getRemainTimes() == 0) {
					cancel(timerId);
					return 0; // procedure done
				}
			}

			long delay = cronTimer.getNextExpectedTimeMills() - System.currentTimeMillis();
			scheduleCronNext(timerId, delay, name);
			return 0;
		}, "fireOnlineSimpleTimer"));
		if (ret != 0)
			cancel(timerId);
	}

	// 调度 Simple 定时器到ThreadPool中。
	private void scheduleSimple(long timerId, long delay, String name) {
		var timer = online.providerApp.zeze.getTimer();
		timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay, () -> fireOnlineSimpleTimer(timerId, name)));
	}

	// Timer发生，执行回调。
	private void fireOnlineSimpleTimer(long timerId, String name) throws Throwable {
		var timer = online.providerApp.zeze.getTimer();
		final var handle = timer.timerHandles.get(name);
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = timer.tGameOlineTimer().get(timerId);
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
			Timer.nextSimpleTimer(simpleTimer);
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				var onlineTimers = online.<BOnlineTimers>getLocalBean(bTimer.getRoleId(), eOnlineTimers);
				var customData = onlineTimers.getTimerIds().get(timerId).getCustomData().getBean();
				var context = new TimerContext(
						timerId, name, customData,
						simpleTimer.getHappenTimes(),
						simpleTimer.getNextExpectedTimeMills(),
						simpleTimer.getExpectedTimeMills());
				handle.run(context);
				return Procedure.Success;
			}, "fireOnlineLocalHandle"));
			if (retNest == Procedure.Exception)
				return retNest;

			// 不管任何结果都递减次数。
			if (simpleTimer.getRemainTimes() > 0) {
				simpleTimer.setRemainTimes(simpleTimer.getRemainTimes() - 1);
				if (simpleTimer.getRemainTimes() == 0) {
					cancel(timerId);
				}
			}

			if (simpleTimer.getPeriod() > 0) {
				var delay = simpleTimer.getNextExpectedTimeMills() - System.currentTimeMillis();
				scheduleSimple(timerId, delay, name);
			}
			return 0;
		}, "fireOnlineSimpleTimer"));
		if (ret != 0)
			cancel(timerId);
	}
}
