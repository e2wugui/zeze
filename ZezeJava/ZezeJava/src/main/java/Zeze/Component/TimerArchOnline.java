package Zeze.Component;

import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Arch.LocalRemoveEventArgument;
import Zeze.Arch.Online;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BOnlineTimer;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
import Zeze.Util.Task;

/**
 * 完全重新实现一套基于内存表和内存的。
 * 不直接使用 Timer.schedule。
 * 但有如下关联。
 * 直接使用 Timer.timerIdAutoKey，使得返回的timerId共享一个名字空间。
 * 直接使用 Timer.timersFuture，从 ThreadPool 返回的future保存在这里。
 * cancel 用户入口从 Timer.calcel 调用。
 */
public class TimerArchOnline {
	final Online online;
	private final ConcurrentHashMap<Long, BOnlineTimer> timers = new ConcurrentHashMap<>();

	public final static String TimerHandleName = "Zeze.Component.TimerArchOnline.Handle";
	public final static String LocalDataCustomPrefix = "Zeze.Component.TimerArchOnline.Custom.";

	public TimerArchOnline(Online online) {
		this.online = online;

		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().getRunEmbedEvents().offer(this::onLocalRemoveEvent);
	}

	public long schedule(String account, String clientId, long delay, long period, long times, String name, Bean customData) {
		// 去掉下面两行，不允许在非登录状态注册timer。现在允许。
		//if (!online.isLogin(account, clientId))
		//	throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.ProviderApp.Zeze.getTimer();
		var timerId = timer.timerIdAutoKey.nextId();

		var onlineTimer = new BOnlineTimer(account, clientId);
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times);
		onlineTimer.getTimerObj().setBean(simpleTimer);
		if (null != timers.putIfAbsent(timerId, onlineTimer))
			throw new IllegalStateException("duplicate timerId @" + account + ":" + clientId + " handle=" + name);

		online.setLocalBean(account, clientId, LocalDataCustomPrefix + timerId, customData);

		Transaction.whileCommit(() -> scheduleSimpleLocal(timerId, delay, period, name));
		return timerId;
	}

	public long schedule(String account, String clientId, String cron, String name, Bean customData) {
		// 去掉下面两行，不允许在非登录状态注册timer。现在允许。
		//if (!online.isLogin(account, clientId))
		//	throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.ProviderApp.Zeze.getTimer();
		var timerId = timer.timerIdAutoKey.nextId();

		var onlineTimer = new BOnlineTimer(account, clientId);
		var cronTimer = new BCronTimer();
		onlineTimer.getTimerObj().setBean(cronTimer);
		if (null != timers.putIfAbsent(timerId, onlineTimer))
			throw new IllegalStateException("duplicate timerId @" + account + ":" + clientId + " handle=" + name);

		online.setLocalBean(account, clientId, LocalDataCustomPrefix + timerId, customData);

		Transaction.whileCommit(() -> scheduleCronLocal(timerId, cron, name));
		return timerId;
	}

	// 不公开，统一通过Timer.cancel调用。
	boolean cancel(long timerId) {
		// remove online timer
		var bTimer = timers.remove(timerId);
		if (null == bTimer)
			return false;

		// remove online local
		online.removeLocalBean(bTimer.getAccount(), bTimer.getClientId(), LocalDataCustomPrefix + timerId);

		// cancel future task
		var future = online.ProviderApp.Zeze.getTimer().timersFuture.remove(timerId);
		if (null == future)
			return false;
		future.cancel(true);
		return true;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 内部实现

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(Object sender, EventDispatcher.EventArgument arg) {
		var local = (LocalRemoveEventArgument)arg;
		var timer = online.ProviderApp.Zeze.getTimer();
		if (null != local.LocalData) {
			for (var e : local.LocalData.getDatas().entrySet()) {
				if (e.getKey().startsWith(LocalDataCustomPrefix)) {
					// is timer data
					var timerId = Long.valueOf(e.getKey().substring(LocalDataCustomPrefix.length()));
					timers.remove(timerId);
					var future = timer.timersFuture.remove(timerId);
					if (null != future)
						future.cancel(true);
				}
			}
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
		var timer = online.ProviderApp.Zeze.getTimer();
		timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay, () -> fireCronLocal(timerId, name)));
	}

	private void fireCronLocal(long timerId, String name) {
		var timer = online.ProviderApp.Zeze.getTimer();
		final var handle = timer.timerHandles.get(name);
		Task.Call(online.ProviderApp.Zeze.NewProcedure(() -> {
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = timers.get(timerId);
			if (null == bTimer) {
				// try cancel future
				var future = timer.timersFuture.remove(timerId);
				if (null != future)
					future.cancel(true);
				return 0; // done
			}

			var cronTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BCronTimer();
			Timer.nextCronTimer(cronTimer);
			var customData = online.getLocalBean(bTimer.getAccount(), bTimer.getClientId(), LocalDataCustomPrefix + timerId);
			var context = new TimerContext(
					timerId, name, customData,
					cronTimer.getHappenTimeMills(),
					cronTimer.getNextExpectedTimeMills(),
					cronTimer.getExpectedTimeMills());
			Task.Call(online.ProviderApp.Zeze.NewProcedure(() -> {
				handle.run(context);
				return Procedure.Success;
			}, "fireOnlineLocalHandle"));

			long delay = context.nextExpectedTimeMills - System.currentTimeMillis();
			scheduleCronNext(timerId, delay, name);
			return 0;
		}, "fireOnlineSimpleTimer"));
	}

	// 调度 Simple 定时器到ThreadPool中。
	private void scheduleSimpleLocal(long timerId, long delay, long period, String name) {
		var timer = online.ProviderApp.Zeze.getTimer();
		if (period > 0)
			timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay, period,
					() -> fireOnlineSimpleTimer(timerId, name)));
		else
			timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
					() -> fireOnlineSimpleTimer(timerId, name)));
	}

	// Timer发生，执行回调。
	private void fireOnlineSimpleTimer(long timerId, String name) {
		var timer = online.ProviderApp.Zeze.getTimer();
		final var handle = timer.timerHandles.get(name);
		Task.Call(online.ProviderApp.Zeze.NewProcedure(() -> {
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}

			var bTimer = timers.get(timerId);
			if (null == bTimer) {
				// try cancel future
				var future = timer.timersFuture.remove(timerId);
				if (null != future)
					future.cancel(true);
				return 0; // done
			}

			var simpleTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BSimpleTimer();
			Timer.nextSimpleTimer(simpleTimer);
			Task.Call(online.ProviderApp.Zeze.NewProcedure(() -> {
				var customData = online.getLocalBean(bTimer.getAccount(), bTimer.getClientId(), LocalDataCustomPrefix + timerId);
				var context = new TimerContext(
						timerId, name, customData,
						simpleTimer.getHappenTimes(),
						simpleTimer.getNextExpectedTimeMills(),
						simpleTimer.getExpectedTimeMills());
				handle.run(context);
				return Procedure.Success;
			}, "fireOnlineLocalHandle"));

			// 不管任何结果都递减次数。
			if (simpleTimer.getRemainTimes() > 0) {
				simpleTimer.setRemainTimes(simpleTimer.getRemainTimes() - 1);
				if (simpleTimer.getRemainTimes() == 0) {
					cancel(timerId);
				}
			}
			return 0;
		}, "fireOnlineSimpleTimer"));
	}
}
