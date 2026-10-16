package Zeze.Component;

import Zeze.Application;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/**
 * 账号定时器门面，把TimerAccount的三类定时器组织成统一入口。
 * 通过 {@link Timer#accounts(String, String)} 获取。
 *
 * <pre>{@code
 * timer.accounts(account, clientId).online().schedule(TimerSpec.ofDelay(1000), MyHandle.class, custom);
 * }</pre>
 */
public final class AccountTimers {
	private final TimerAccount timerAccount;
	private final String account;
	private final String clientId;

	AccountTimers(TimerAccount timerAccount, String account, String clientId) {
		this.timerAccount = timerAccount;
		this.account = account;
		this.clientId = clientId;
	}

	public String account() {
		return account;
	}

	public String clientId() {
		return clientId;
	}

	/**
	 * 在线定时器(cold handle)：仅当账号登录在本服务器时触发，handle在调度时解析。
	 * 不允许热更新模块调用。
	 */
	public TimerScope online() {
		return new OnlineScope();
	}

	/**
	 * 在线定时器(hot handle)：仅当账号登录在本服务器时触发，handle在触发时按名字解析。
	 * 支持热更新模块调用。
	 */
	public TimerScope onlineHot() {
		return new OnlineHotScope();
	}

	/**
	 * 离线定时器：账号不在线时也能触发。
	 */
	public TimerScope offline() {
		return new OfflineScope();
	}

	/**
	 * 取消定时器(online和offline都会尝试)，取消不存在的timer认为成功。
	 */
	public boolean cancel(@Nullable String timerId) {
		return timerAccount.cancel(timerId, account, clientId);
	}

	private final class OnlineScope implements TimerScope {
		@Override
		public String schedule(TimerSpec spec, Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			var zeze = timerAccount.zeze();
			if (zeze.isHotVerifyEnabled())
				zeze.verifyCallerCold(Application.CALLER_WALKER.getCallerClass());
			return timerAccount.scheduleOnlineImpl(account, clientId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(String timerId, TimerSpec spec,
									 Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			var zeze = timerAccount.zeze();
			if (zeze.isHotVerifyEnabled())
				zeze.verifyCallerCold(Application.CALLER_WALKER.getCallerClass());
			return timerAccount.scheduleOnlineNamedImpl(account, clientId, timerId, spec, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerAccount.cancelOnline(timerId, account, clientId);
		}
	}

	private final class OnlineHotScope implements TimerScope {
		@Override
		public String schedule(TimerSpec spec, Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			return timerAccount.scheduleOnlineHot(account, clientId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(String timerId, TimerSpec spec,
									 Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			return timerAccount.scheduleOnlineNamedHot(account, clientId, timerId, spec, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerAccount.cancelOnline(timerId, account, clientId);
		}
	}

	private final class OfflineScope implements TimerScope {
		@Override
		public String schedule(TimerSpec spec, Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			return timerAccount.scheduleOffline(account, clientId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(String timerId, TimerSpec spec,
									 Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			return timerAccount.scheduleOfflineNamed(timerId, account, clientId, spec, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerAccount.cancelOffline(timerId, account, clientId);
		}
	}
}
