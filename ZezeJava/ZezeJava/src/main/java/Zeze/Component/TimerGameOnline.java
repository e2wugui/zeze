package Zeze.Component;

import java.text.ParseException;
import Zeze.Builtin.Timer.BGameOnlineCustom;
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

	public final static String TimerHandleName = "Zeze.Component.TimerGameOnline.Handle";
	public final static String LocalDataCustomPrefix = "Zeze.Component.TimerGameOnline.Custom.";

	public TimerGameOnline(Online online) {
		this.online = online;

		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().getRunEmbedEvents().offer(this::onLocalRemoveEvent);
		var timer = online.ProviderApp.Zeze.getTimer();
		timer.addHandle(TimerHandleName, this::fireOnlineTimer, this::cancelOnlineTimer);
	}

	public long schedule(long roleId, long delay, long period, long times, String name, Bean customData) {
		// 去掉下面两行，不允许在非登录状态注册timer。现在允许。
		//if (!online.isLogin(account, clientId))
		//	throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.ProviderApp.Zeze.getTimer();
		var customOnline = new BGameOnlineCustom(roleId, name);
		if (null != customData) {
			timer.register(customData.getClass());
			customOnline.getCustomData().setBean(customData);
		}
		var timerId = timer.schedule(delay, period, times, TimerHandleName, customOnline);
		online.setLocalBean(roleId, LocalDataCustomPrefix + timerId, customData);
		return timerId;
	}

	public long schedule(long roleId, String cron, String name, Bean customData) throws ParseException {
		// 去掉下面两行，不允许在非登录状态注册timer。现在允许。
		//if (!online.isLogin(account, clientId))
		//	throw new IllegalStateException("not login. account=" + account + " clientId=" + clientId);

		var timer = online.ProviderApp.Zeze.getTimer();
		var customOnline = new BGameOnlineCustom(roleId, name);
		if (null != customData) {
			timer.register(customData.getClass());
			customOnline.getCustomData().setBean(customData);
		}
		var timerId = timer.schedule(cron, TimerHandleName, customOnline);
		online.setLocalBean(roleId, LocalDataCustomPrefix + timerId, customData);
		return timerId;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 内部实现

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(Object sender, EventDispatcher.EventArgument arg) throws Throwable {
		var local = (LocalRemoveEventArgument)arg;
		var timer = online.ProviderApp.Zeze.getTimer();
		if (null != local.LocalData) {
			for (var e : local.LocalData.getDatas().entrySet()) {
				if (e.getKey().startsWith(LocalDataCustomPrefix)) {
					// is timer data
					var timerId = Long.parseLong(e.getKey().substring(LocalDataCustomPrefix.length()));
					timer.cancel(timerId);
				}
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
		context.customData = customOnline.getCustomData().getBean();
		handle.run(context);
	}

	private void cancelOnlineTimer(BIndex bIndex, BTimer bTimer) {
		var customOnline = (BGameOnlineCustom)bTimer.getCustomData().getBean();
		var timerId = bTimer.getTimerId();
		online.removeLocalBean(customOnline.getRoleId(), LocalDataCustomPrefix + timerId);
	}
}
