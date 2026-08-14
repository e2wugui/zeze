package Zeze.Component;

import Zeze.Application;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 角色定时器门面，把TimerRole的三类定时器组织成统一入口。
 * 通过 {@link Timer#roles(long)} 或 {@link Timer#roles(String, long)} 获取。
 *
 * <pre>{@code
 * timer.roles(roleId).online().schedule(TimerSpec.ofDelay(1000), MyHandle.class, custom);
 * }</pre>
 */
public final class RoleTimers {
	private final @NotNull TimerRole timerRole;
	private final long roleId;

	RoleTimers(@NotNull TimerRole timerRole, long roleId) {
		this.timerRole = timerRole;
		this.roleId = roleId;
	}

	public long roleId() {
		return roleId;
	}

	/**
	 * 在线定时器(cold handle)：仅当角色登录在本服务器时触发，handle在调度时解析。
	 * 不允许热更新模块调用。
	 */
	public @NotNull TimerScope online() {
		return new OnlineScope();
	}

	/**
	 * 在线定时器(hot handle)：仅当角色登录在本服务器时触发，handle在触发时按名字解析。
	 * 支持热更新模块调用。
	 */
	public @NotNull TimerScope onlineHot() {
		return new OnlineHotScope();
	}

	/**
	 * 离线定时器：角色不在线时也能触发。
	 */
	public @NotNull TimerScope offline() {
		return new OfflineScope();
	}

	/**
	 * 取消定时器(online和offline都会尝试)，取消不存在的timer认为成功。
	 */
	public boolean cancel(@Nullable String timerId) {
		return timerRole.cancel(timerId, roleId);
	}

	private final class OnlineScope implements TimerScope {
		@Override
		public @NotNull String schedule(@NotNull TimerSpec spec, @NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			var zeze = timerRole.zeze();
			if (zeze.isHotVerifyEnabled())
				zeze.verifyCallerCold(Application.CALLER_WALKER.getCallerClass());
			return timerRole.scheduleOnlineImpl(roleId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull TimerSpec spec,
									 @NotNull Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			var zeze = timerRole.zeze();
			if (zeze.isHotVerifyEnabled())
				zeze.verifyCallerCold(Application.CALLER_WALKER.getCallerClass());
			return timerRole.scheduleOnlineNamedImpl(roleId, timerId, spec, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerRole.cancelOnline(timerId, roleId);
		}
	}

	private final class OnlineHotScope implements TimerScope {
		@Override
		public @NotNull String schedule(@NotNull TimerSpec spec, @NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			return timerRole.scheduleOnlineHot(roleId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull TimerSpec spec,
									 @NotNull Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			return timerRole.scheduleOnlineNamedHot(roleId, timerId, spec, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerRole.cancelOnline(timerId, roleId);
		}
	}

	private final class OfflineScope implements TimerScope {
		@Override
		public @NotNull String schedule(@NotNull TimerSpec spec, @NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			return timerRole.scheduleOffline(roleId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull TimerSpec spec,
									 @NotNull Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			return timerRole.scheduleOfflineNamed(timerId, roleId, spec, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerRole.cancelOffline(timerId, roleId);
		}
	}
}
