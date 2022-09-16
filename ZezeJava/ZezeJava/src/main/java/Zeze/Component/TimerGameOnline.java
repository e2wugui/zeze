package Zeze.Component;

import Zeze.Builtin.Timer.BGameOnlineCustom;
import Zeze.Builtin.Timer.BOnlineTimers;
import Zeze.Game.LocalRemoveEventArgument;
import Zeze.Game.Online;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BTimer;
import Zeze.Transaction.Bean;
import Zeze.Util.EventDispatcher;

/**
 * 基于使用 Timer.schedule。
 * 直接使用 Timer.timerIdAutoKey，使得返回的timerId共享一个名字空间。
 * 直接使用 Timer.timersFuture，从 ThreadPool 返回的future保存在这里。
 * cancel 入口从 Timer.cancel 调用。
 */
public class TimerGameOnline {
	final Online online;

	public final static String eTimerHandleName = "Zeze.Component.TimerGameOnline.Handle";
	public final static String eOnlineTimers = "Zeze.Component.TimerGameOnline";

	public TimerGameOnline(Online online) {
		this.online = online;

		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().getRunEmbedEvents().offer(this::onLocalRemoveEvent);
		var timer = online.ProviderApp.Zeze.getTimer();
		timer.addHandle(eTimerHandleName, this::fireOnlineTimer, this::cancelOnlineTimer);
	}

	public long schedule(long roleId, long delay, long period, long times, String name, Bean customData) throws Throwable {
		// 去掉下面两行，不允许在非登录状态注册timer。现在允许。
		//if (!online.isLogin(account, clientId))
		//	throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.ProviderApp.Zeze.getTimer();
		var customOnline = new BGameOnlineCustom(roleId, name);
		if (null != customData) {
			timer.register(customData.getClass());
			customOnline.getCustomData().setBean(customData);
		}
		var timerId = timer.schedule(delay, period, times, eTimerHandleName, customOnline);
		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		timerIds.getTimerIds().add(timerId);
		return timerId;
	}

	public long schedule(long roleId, String cron, String name, Bean customData) throws Throwable {
		// 去掉下面两行，不允许在非登录状态注册timer。现在允许。
		//if (!online.isLogin(account, clientId))
		//	throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.ProviderApp.Zeze.getTimer();
		var customOnline = new BGameOnlineCustom(roleId, name);
		if (null != customData) {
			timer.register(customData.getClass());
			customOnline.getCustomData().setBean(customData);
		}
		var timerId = timer.schedule(cron, eTimerHandleName, customOnline);
		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		timerIds.getTimerIds().add(timerId);
		return timerId;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 内部实现

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(Object sender, EventDispatcher.EventArgument arg) throws Throwable {
		var local = (LocalRemoveEventArgument)arg;
		var timer = online.ProviderApp.Zeze.getTimer();
		if (null != local.LocalData) {
			var bAny = local.LocalData.getDatas().get(eOnlineTimers);
			if (null != bAny) {
				var timerIds = (BOnlineTimers)bAny.getAny().getBean();
				for (var timerId : timerIds.getTimerIds())
					timer.cancel(timerId);
			}
		}
		return 0;
	}

	private void fireOnlineTimer(TimerContext context) throws Throwable {
		var timer = online.ProviderApp.Zeze.getTimer();
		var customOnline = (BGameOnlineCustom)context.customData;
		var handle = timer.timerHandles.get(customOnline.getHandleName());
		if (null == handle)
			throw new IllegalStateException("Online Handle Miss.");
		handle.run(context);
	}

	private void cancelOnlineTimer(BIndex bIndex, BTimer bTimer) {
		var customOnline = (BGameOnlineCustom)bTimer.getCustomData().getBean();
		var timerId = bTimer.getTimerId();
		var timerIds = online.<BOnlineTimers>getLocalBean(customOnline.getRoleId(), eOnlineTimers);
		if (null != timerIds) {
			timerIds.getTimerIds().remove(timerId);
			// 留着空集合也没关系。
			//if (timerIds.getTimerIds().isEmpty())
			//	online.removeLocalBean(customOnline.getRoleId(), eOnlineTimers);
		}
	}
}
