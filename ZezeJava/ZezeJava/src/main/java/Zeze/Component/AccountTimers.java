package Zeze.Component;

import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 账号定时器门面，把TimerAccount的三类定时器组织成统一入口。
 * 通过 {@link Timer#accounts(String, String)} 获取。
 *
 * <pre>{@code
 * timer.accounts(account, clientId).online().schedule(TimerSpec.ofDelay(1000), MyHandle.class, custom);
 * }</pre>
 */
public final class AccountTimers {
	private final @NotNull TimerAccount timerAccount;
	private final @NotNull String account;
	private final @NotNull String clientId;
	private final @NotNull TimerScope online = new OnlineScope();
	private final @NotNull TimerScope onlineHot = new OnlineHotScope();
	private final @NotNull TimerScope offline = new OfflineScope();

	AccountTimers(@NotNull TimerAccount timerAccount, @NotNull String account, @NotNull String clientId) {
		this.timerAccount = timerAccount;
		this.account = account;
		this.clientId = clientId;
	}

	public @NotNull String account() {
		return account;
	}

	public @NotNull String clientId() {
		return clientId;
	}

	/**
	 * 在线定时器(cold handle)：仅当账号登录在本服务器时触发，handle在调度时解析。
	 * 不允许热更新模块调用。
	 */
	public @NotNull TimerScope online() {
		return online;
	}

	/**
	 * 在线定时器(hot handle)：仅当账号登录在本服务器时触发，handle在触发时按名字解析。
	 * 支持热更新模块调用。
	 */
	public @NotNull TimerScope onlineHot() {
		return onlineHot;
	}

	/**
	 * 离线定时器：账号不在线时也能触发。
	 */
	public @NotNull TimerScope offline() {
		return offline;
	}

	/**
	 * 取消定时器(online和offline都会尝试)，取消不存在的timer认为成功。
	 */
	public boolean cancel(@Nullable String timerId) {
		return timerAccount.cancel(timerId, account, clientId);
	}

	private final class OnlineScope implements TimerScope {
		@Override
		public @NotNull String schedule(@NotNull TimerSpec spec, @NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			timerAccount.zeze().verifyCallerCold(
					StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
			return timerAccount.scheduleOnlineImpl(account, clientId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull TimerSpec spec,
									 @NotNull Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			timerAccount.zeze().verifyCallerCold(
					StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
			return timerAccount.scheduleOnlineNamedImpl(account, clientId, timerId, spec, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerAccount.cancelOnline(timerId, account, clientId);
		}
	}

	private final class OnlineHotScope implements TimerScope {
		@Override
		public @NotNull String schedule(@NotNull TimerSpec spec, @NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			return timerAccount.scheduleOnlineHot(account, clientId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull TimerSpec spec,
									 @NotNull Class<? extends TimerHandle> handleClass,
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
		public @NotNull String schedule(@NotNull TimerSpec spec, @NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			return timerAccount.scheduleOffline(account, clientId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull TimerSpec spec,
									 @NotNull Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			return timerAccount.scheduleOfflineNamed(timerId, account, clientId, spec, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerAccount.cancelOffline(timerId, account, clientId);
		}
	}
}
