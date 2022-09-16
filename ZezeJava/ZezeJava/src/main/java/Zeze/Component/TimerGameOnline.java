package Zeze.Component;

import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Game.LocalRemoveEventArgument;
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
 * 1. scheduleNamed 基于 Timer.scheduleNamed。
 * 2. schedule 完全重新实现一套基于内存表和内存的。
 * 3. 不直接使用 Timer.schedule。但有如下关联。
 *    直接使用 Timer.timerIdAutoKey，使得返回的timerId共享一个名字空间。
 *    直接使用 Timer.timersFuture，从 ThreadPool 返回的future保存在这里。
 * 4. cancel 用户入口从 Timer.calcel 调用。
 */
public class TimerGameOnline {
	final Online online;

	//public final static String eTimerHandleName = "Zeze.Component.TimerGameOnline.Handle";
	public final static String eOnlineTimers = "Zeze.Component.TimerGameOnline";
	private final ConcurrentHashMap<Long, BGameOnlineTimer> timers = new ConcurrentHashMap<>();

	public TimerGameOnline(Online online) {
		this.online = online;

		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().getRunEmbedEvents().offer(this::onLocalRemoveEvent);
	}

	// todo
	public long scheduleNamed() {
		return -1;
	}

	public long schedule(long roleId, long delay, long period, long times, String name, Bean customData) throws Throwable {
		// 去掉下面两行，不允许在非登录状态注册timer。现在允许。
		if (!online.isLogin(roleId))
			throw new IllegalStateException("not login. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.timerIdAutoKey.nextId();

		var onlineTimer = new BGameOnlineTimer(roleId);
		if (null != timers.putIfAbsent(timerId, onlineTimer))
			throw new IllegalStateException("duplicate timerId @" + roleId + " handle=" + name);
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times);
		onlineTimer.getTimerObj().setBean(simpleTimer);

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		timerIds.getTimerIds().getOrAdd(timerId).getCustomData().setBean(customData);

		Transaction.whileCommit(() -> scheduleSimpleLocal(timerId, delay, period, name));
		return timerId;
	}

	public long schedule(long roleId, String cron, String name, Bean customData) throws Throwable {
		// 去掉下面两行，不允许在非登录状态注册timer。现在允许。
		if (!online.isLogin(roleId))
			throw new IllegalStateException("not login. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var timerId = timer.timerIdAutoKey.nextId();

		var onlineTimer = new BGameOnlineTimer(roleId);
		var cronTimer = new BCronTimer();
		onlineTimer.getTimerObj().setBean(cronTimer);
		if (null != timers.putIfAbsent(timerId, onlineTimer))
			throw new IllegalStateException("duplicate timerId @" + roleId + " handle=" + name);

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		timerIds.getTimerIds().getOrAdd(timerId).getCustomData().setBean(customData);

		Transaction.whileCommit(() -> scheduleCronLocal(timerId, cron, name));
		return timerId;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 内部实现

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(Object sender, EventDispatcher.EventArgument arg) throws Throwable {
		var local = (LocalRemoveEventArgument)arg;
		var timer = online.providerApp.zeze.getTimer();
		if (null != local.LocalData) {
			var bAny = local.LocalData.getDatas().get(eOnlineTimers);
			if (null != bAny) {
				var timers = (BOnlineTimers)bAny.getAny().getBean();
				for (var timerId : timers.getTimerIds().keySet())
					cancel(timerId);
				for (var name : timers.getNamedNames().keySet())
					timer.cancelNamed(name);
			}
		}
		return 0;
	}

	// 不公开，统一通过Timer.cancel调用。
	boolean cancel(long timerId) throws Throwable {
		// remove online timer
		var bTimer = timers.remove(timerId);
		if (null == bTimer)
			return false;

		// remove online local
		var onlineTimers = online.getOrAddLocalBean(bTimer.getRoleId(), eOnlineTimers, new BOnlineTimers());
		onlineTimers.getTimerIds().remove(timerId);

		// cancel future task
		var future = online.ProviderApp.Zeze.getTimer().timersFuture.remove(timerId);
		if (null == future)
			return false;
		future.cancel(true);
		return true;
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

	private void fireCronLocal(long timerId, String name) throws Throwable {
		var timer = online.ProviderApp.Zeze.getTimer();
		final var handle = timer.timerHandles.get(name);
		var ret = Task.call(online.ProviderApp.Zeze.newProcedure(() -> {
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
			var onlineTimers = online.<BOnlineTimers>getLocalBean(bTimer.getRoleId(), eOnlineTimers);
			var customData = onlineTimers.getTimerIds().get(timerId).getCustomData().getBean();
			var context = new TimerContext(
					timerId, name, customData,
					cronTimer.getHappenTimeMills(),
					cronTimer.getNextExpectedTimeMills(),
					cronTimer.getExpectedTimeMills());
			var retNest = Task.call(online.ProviderApp.Zeze.newProcedure(() -> {
				handle.run(context);
				return Procedure.Success;
			}, "fireOnlineLocalHandle"));
			if (retNest == Procedure.Exception)
				return retNest;
			// skip other error

			long delay = context.nextExpectedTimeMills - System.currentTimeMillis();
			scheduleCronNext(timerId, delay, name);
			return 0;
		}, "fireOnlineSimpleTimer"));
		if (ret != 0)
			cancel(timerId);
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
	private void fireOnlineSimpleTimer(long timerId, String name) throws Throwable {
		var timer = online.ProviderApp.Zeze.getTimer();
		final var handle = timer.timerHandles.get(name);
		var ret = Task.call(online.ProviderApp.Zeze.newProcedure(() -> {
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
			var retNest = Task.call(online.ProviderApp.Zeze.newProcedure(() -> {
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
			return 0;
		}, "fireOnlineSimpleTimer"));
		if (ret != 0)
			cancel(timerId);
	}
}
