package Zeze.Component;

import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/**
 * 角色定时器门面，把TimerRole的两类定时器组织成统一入口。
 * 通过 {@link Timer#roles(long)} 或 {@link Timer#roles(String, long)} 获取。
 *
 * <pre>{@code
 * timer.roles(roleId).online().schedule(TimerSpec.ofDelay(1000), MyHandle.class, custom);
 * }</pre>
 */
public final class RoleTimers {
	private final TimerRole timerRole;
	private final long roleId;

	RoleTimers(TimerRole timerRole, long roleId) {
		this.timerRole = timerRole;
		this.roleId = roleId;
	}

	public long roleId() {
		return roleId;
	}

	/**
	 * 在线定时器：仅当角色登录在本服务器时触发。
	 */
	public TimerScope online() {
		return new OnlineScope();
	}

	/**
	 * 离线定时器：角色不在线时也能触发。
	 */
	public TimerScope offline() {
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
		public String schedule(TimerSpec spec, Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			return timerRole.scheduleOnline(roleId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(String timerId, TimerSpec spec,
									 Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			return timerRole.scheduleOnlineNamed(roleId, timerId, spec, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerRole.cancelOnline(timerId, roleId);
		}
	}

	private final class OfflineScope implements TimerScope {
		@Override
		public String schedule(TimerSpec spec, Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
			return timerRole.scheduleOffline(roleId, spec, handleClass, customData);
		}

		@Override
		public boolean scheduleNamed(String timerId, TimerSpec spec,
									 Class<? extends TimerHandle> handleClass,
									 @Nullable Bean customData) {
			return timerRole.scheduleOfflineNamed(timerId, roleId, spec, handleClass, customData);
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			return timerRole.cancelOffline(timerId, roleId);
		}
	}
}
