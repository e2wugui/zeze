package Zeze.Component;

import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/**
 * 账号定时器门面，把TimerAccount的两类定时器组织成统一入口。
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
	 * 在线定时器：仅当账号登录在本服务器时触发。
	 */
	public TimerScope online() {
		return new OnlineScope();
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
			return timerAccount.scheduleOnline(account, clientId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(String timerId, TimerSpec spec,
									 Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			return timerAccount.scheduleOnlineNamed(account, clientId, timerId, spec, handleClass, customData);
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
