package Zeze.Component;

import java.text.ParseException;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 角色定时器门面，把TimerRole的三类定时器组织成统一入口。
 * 通过 {@link Timer#roles(long)} 或 {@link Timer#roles(String, long)} 获取。
 *
 * <pre>{@code
 * timer.roles(roleId).online().schedule(BSimpleTimerBuilder.ofDelay(1000), MyHandle.class, custom);
 * }</pre>
 */
public final class RoleTimers {
	private final @NotNull TimerRole timerRole;
	private final long roleId;
	private final @NotNull TimerScope online = new OnlineScope();
	private final @NotNull TimerScope onlineHot = new OnlineHotScope();
	private final @NotNull TimerScope offline = new OfflineScope();

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
		return online;
	}

	/**
	 * 在线定时器(hot handle)：仅当角色登录在本服务器时触发，handle在触发时按名字解析。
	 * 支持热更新模块调用。
	 */
	public @NotNull TimerScope onlineHot() {
		return onlineHot;
	}

	/**
	 * 离线定时器：角色不在线时也能触发。
	 */
	public @NotNull TimerScope offline() {
		return offline;
	}

	/**
	 * 取消定时器(online和offline都会尝试)，取消不存在的timer认为成功。
	 */
	public boolean cancel(@Nullable String timerId) {
		return timerRole.cancel(timerId, roleId);
	}

	private final class OnlineScope implements TimerScope {
		@Override
		public @NotNull String schedule(@NotNull BSimpleTimerBuilder builder,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			timerRole.zeze().verifyCallerCold(
					StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
			return timerRole.scheduleOnlineImpl(roleId, builder, handleClass, customData);
		}

		@Override
		public @NotNull String schedule(@NotNull BCronTimerBuilder builder,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) throws ParseException {
			timerRole.zeze().verifyCallerCold(
					StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
			return timerRole.scheduleOnlineImpl(roleId, builder, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull BSimpleTimerBuilder builder,
									 @NotNull Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			timerRole.zeze().verifyCallerCold(
					StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
			return timerRole.scheduleOnlineNamedImpl(roleId, timerId, builder, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull BCronTimerBuilder builder,
									 @NotNull Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) throws ParseException {
			timerRole.zeze().verifyCallerCold(
					StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
			return timerRole.scheduleOnlineNamedImpl(roleId, timerId, builder, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerRole.cancelOnline(timerId, roleId);
		}
	}

	private final class OnlineHotScope implements TimerScope {
		@Override
		public @NotNull String schedule(@NotNull BSimpleTimerBuilder builder,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			return timerRole.scheduleOnlineHot(roleId, builder, handleClass, customData);
		}

		@Override
		public @NotNull String schedule(@NotNull BCronTimerBuilder builder,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) throws ParseException {
			return timerRole.scheduleOnlineHot(roleId, builder, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull BSimpleTimerBuilder builder,
									 @NotNull Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			return timerRole.scheduleOnlineNamedHot(roleId, timerId, builder, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull BCronTimerBuilder builder,
									 @NotNull Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) throws ParseException {
			return timerRole.scheduleOnlineNamedHot(roleId, timerId, builder, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerRole.cancelOnline(timerId, roleId);
		}
	}

	private final class OfflineScope implements TimerScope {
		@Override
		public @NotNull String schedule(@NotNull BSimpleTimerBuilder builder,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			return timerRole.scheduleOffline(roleId, builder, handleClass, customData);
		}

		@Override
		public @NotNull String schedule(@NotNull BCronTimerBuilder builder,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) throws ParseException {
			return timerRole.scheduleOffline(roleId, builder, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull BSimpleTimerBuilder builder,
									 @NotNull Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			return timerRole.scheduleOfflineNamed(timerId, roleId, builder, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(@NotNull String timerId, @NotNull BCronTimerBuilder builder,
									 @NotNull Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) throws ParseException {
			return timerRole.scheduleOfflineNamed(timerId, roleId, builder, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerRole.cancelOffline(timerId, roleId);
		}
	}
}
