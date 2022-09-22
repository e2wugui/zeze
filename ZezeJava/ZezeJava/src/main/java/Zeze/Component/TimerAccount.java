package Zeze.Component;

import Zeze.Arch.LocalRemoveEventArgument;
import Zeze.Arch.Online;
import Zeze.Builtin.Timer.BArchOnlineTimer;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BOnlineTimers;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Arch.LoginArgument;
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
public class TimerAccount {
	final Online online;

	//public final static String eTimerHandleName = "Zeze.Component.TimerArchOnline.Handle";
	public final static String eOnlineTimers = "Zeze.Component.TimerArchOnline";

	TimerAccount(Online online) {
		this.online = online;

		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().getRunEmbedEvents().offer(this::onLocalRemoveEvent);
		online.getReloginEvents().getRunEmbedEvents().offer(this::onReloginEvent);
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(String account, String clientId, String timerName,
									   long delay, long period, long times, long endTime,
									   String handleName, Bean customData) throws Throwable {
		var timer = online.providerApp.zeze.getTimer();
		var bTimer = timer.tArchOlineTimer().get(timerName);
		if (null != bTimer)
			return false;
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime);
		scheduleOnline(account, clientId, timerName, simpleTimer, handleName, customData);
		return true;
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(String account, String clientId, String timerName,
									   String cron, long times, long endTime, String handleName, Bean customData) throws Throwable {
		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.tArchOlineTimer().get(timerName);
		if (null != timerId)
			return false;
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime);
		scheduleOnline(account, clientId, timerName, cronTimer, handleName, customData);
		return true;
	}

	public String scheduleOnline(String account, String clientId, long delay, long period, long times, long endTime, String name, Bean customData) throws Throwable {
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime);
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOnline(account, clientId, "@" + timer.timerIdAutoKey.nextString(), simpleTimer, name, customData);
	}

	private String scheduleOnline(String account, String clientId, String timerId, BSimpleTimer simpleTimer, String name, Bean customData) throws Throwable {
		var loginVersion = online.getLocalLoginVersion(account, clientId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BArchOnlineTimer(account, clientId, loginVersion);
		timer.tArchOlineTimer().insert(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(simpleTimer);

		var timerIds = online.getOrAddLocalBean(account, clientId, eOnlineTimers, new BOnlineTimers());
		timerIds.getTimerIds().getOrAdd(timerId).getCustomData().setBean(customData);

		Transaction.whileCommit(() -> scheduleSimple(timerId, simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), name));
		return timerId;
	}

	public String scheduleOnline(String account, String clientId, String cron, long times, long endTime, String name, Bean customData) throws Throwable {
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime);
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOnline(account, clientId, "@" + timer.timerIdAutoKey.nextString(), cronTimer, name, customData);
	}

	private String scheduleOnline(String account, String clientId, String timerId, BCronTimer cronTimer, String name, Bean customData) throws Throwable {
		var loginVersion = online.getLocalLoginVersion(account, clientId);
		if (null == loginVersion)
			throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BArchOnlineTimer(account, clientId, loginVersion);
		timer.tArchOlineTimer().insert(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(cronTimer);

		var timerIds = online.getOrAddLocalBean(account, clientId, eOnlineTimers, new BOnlineTimers());
		timerIds.getTimerIds().getOrAdd(timerId).getCustomData().setBean(customData);

		Transaction.whileCommit(() -> scheduleCron(timerId, cronTimer, name));
		return timerId;
	}

	public boolean cancel(String timerId) throws Throwable {
		var timer = online.providerApp.zeze.getTimer();
		// remove online timer
		var bTimer = timer.tArchOlineTimer().get(timerId);
		if (null == bTimer)
			return false;

		// remove online local
		var onlineTimers = online.getOrAddLocalBean(bTimer.getAccount(), bTimer.getClientId(), eOnlineTimers, new BOnlineTimers());
		onlineTimers.getTimerIds().remove(timerId);
		timer.tArchOlineTimer().remove(timerId);

		// cancel future task
		timer.cancelFuture(timerId);
		return true;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 内部实现

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(Object sender, EventDispatcher.EventArgument arg) throws Throwable {
		var local = (LocalRemoveEventArgument)arg;
		if (null != local.localData) {
			var bAny = local.localData.getDatas().get(eOnlineTimers);
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

		var loginVersion = online.getGlobalLoginVersion(user.account, user.clientId);
		var timers = online.<BOnlineTimers>getLocalBean(user.account, user.clientId, eOnlineTimers);
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
	private void scheduleCron(String timerId, BCronTimer cron, String name) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext( timerId, delay, name);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	// 再次调度 cron 定时器，真正安装到ThreadPool中。
	private void scheduleCronNext(String timerId, long delay, String name) {
		var timer = online.providerApp.zeze.getTimer();
		timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay, () -> fireCron(timerId, name)));
	}

	private void fireCron(String timerId, String name) throws Throwable {
		var timer = online.providerApp.zeze.getTimer();
		final var handle = timer.timerHandles.get(name);
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = timer.tArchOlineTimer().get(timerId);
			if (null == bTimer) {
				timer.cancelFuture(timerId);
				return 0; // done
			}
			var globalLoginVersion = online.getGlobalLoginVersion(bTimer.getAccount(), bTimer.getClientId());
			if (null == globalLoginVersion || bTimer.getLoginVersion() != globalLoginVersion) {
				// 已经不是注册定时器时候的登录了。
				timer.cancelFuture(timerId);
				return 0; // done
			}

			var cronTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BCronTimer();
			var onlineTimers = online.<BOnlineTimers>getLocalBean(bTimer.getAccount(), bTimer.getClientId(), eOnlineTimers);
			var customData = onlineTimers.getTimerIds().get(timerId).getCustomData().getBean();
			var context = new TimerContext(timerId, name, customData,
					cronTimer.getHappenTime(), cronTimer.getNextExpectedTime(), cronTimer.getExpectedTime());
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				handle.run(context);
				return Procedure.Success;
			}, "fireOnlineLocalHandle"));
			if (retNest == Procedure.Exception)
				return retNest;
			// skip other error

			if (!Timer.nextCronTimer(cronTimer, false)) {
				cancel(timerId);
				return 0; // procedure done
			}

			// continue period
			long delay = cronTimer.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(timerId, delay, name);
			return 0;
		}, "fireOnlineSimpleTimer"));
		if (ret != 0)
			cancel(timerId);
	}

	// 调度 Simple 定时器到ThreadPool中。
	private void scheduleSimple(String timerId, long delay, String name) {
		var timer = online.providerApp.zeze.getTimer();
		timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay, () -> fireSimple(timerId, name)));
	}

	// Timer发生，执行回调。
	private void fireSimple(String timerId, String name) throws Throwable {
		var timer = online.providerApp.zeze.getTimer();
		final var handle = timer.timerHandles.get(name);
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = timer.tArchOlineTimer().get(timerId);
			if (null == bTimer) {
				timer.cancelFuture(timerId);
				return 0; // done
			}
			var globalLoginVersion = online.getGlobalLoginVersion(bTimer.getAccount(), bTimer.getClientId());
			if (null == globalLoginVersion || bTimer.getLoginVersion() != globalLoginVersion) {
				// 已经不是注册定时器时候的登录了。
				timer.cancelFuture(timerId);
				return 0; // done
			}

			var simpleTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BSimpleTimer();
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				var onlineTimers = online.<BOnlineTimers>getLocalBean(
						bTimer.getAccount(), bTimer.getClientId(), eOnlineTimers);
				var customData = onlineTimers.getTimerIds().get(timerId).getCustomData().getBean();
				var context = new TimerContext(timerId, name, customData,
						simpleTimer.getHappenTimes(), simpleTimer.getNextExpectedTime(),
						simpleTimer.getExpectedTime());
				handle.run(context);
				return Procedure.Success;
			}, "fireOnlineLocalHandle"));
			if (retNest == Procedure.Exception)
				return retNest;
			// 其他错误忽略

			// 准备下一个间隔
			if (!Timer.nextSimpleTimer(simpleTimer, false)) {
				cancel(timerId);
				return 0; // procedure done
			}

			// continue period
			var delay = simpleTimer.getNextExpectedTime() - System.currentTimeMillis();
			scheduleSimple(timerId, delay, name);
			return 0; // last procedure done
		}, "fireOnlineSimpleTimer"));
		if (ret != 0)
			cancel(timerId);
	}
}
