package Zeze.Component;

import java.text.ParseException;
import java.util.List;
import Zeze.Builtin.Timer.BGameOnlineTimer;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BOfflineRoleCustom;
import Zeze.Builtin.Timer.BOnlineTimers;
import Zeze.Builtin.Timer.BTransmitCancelRoleTimer;
import Zeze.Builtin.Timer.BTransmitCronTimer;
import Zeze.Builtin.Timer.BTransmitSimpleTimer;
import Zeze.Game.LocalRemoveEventArgument;
import Zeze.Game.LoginArgument;
import Zeze.Game.Online;
import Zeze.Game.ProviderWithOnline;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.EventDispatcher;
import Zeze.Util.Reflect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 1. schedule，scheduleNamed 完全重新实现一套基于内存表和内存的。
 * 2. 不直接使用 Timer.schedule。但有如下关联。
 * 直接使用 Timer.timerIdAutoKey，使得返回的timerId共享一个名字空间。
 * 直接使用 Timer.timersFuture，从 ThreadPool 返回的future保存在这里。
 * <p>
 * online定时器的调度、触发、取消、转发逻辑在 {@link TimerOnlineBase} 中实现。
 */
public class TimerRole extends TimerOnlineBase<Long> {
	private static final @NotNull Logger logger = LogManager.getLogger(TimerRole.class);
	public static final String eOnlineTimers = "Zeze.Component.TimerGameOnline";
	// public static final String eTimerHandleName = "Zeze.Component.TimerGameOnline.Handle";
	public static final String eTransmitCronTimer = "Zeze.TimerRole.TransmitCronTimer";
	public static final String eTransmitSimpleTimer = "Zeze.TimerRole.TransmitSimpleTimer";
	public static final String eTransmitCancelRoleTimer = "Zeze.TimerRole.TransmitCancelRoleTimer";

	private final @NotNull Online online;

	public TimerRole(@NotNull Online online) {
		this.online = online;

		online.getTransmitActions().put(eTransmitCronTimer, this::transmitOnlineCronTimer);
		online.getTransmitActions().put(eTransmitSimpleTimer, this::transmitOnlineSimpleTimer);
		online.getTransmitActions().put(eTransmitCancelRoleTimer, this::transmitCancelRoleTimer);
		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().add(EventDispatcher.Mode.RunEmbed, this::onLocalRemoveEvent);
		online.getLoginEvents().add(EventDispatcher.Mode.RunEmbed, this::onLoginEvent);
		online.getReloginEvents().add(EventDispatcher.Mode.RunEmbed, this::onLoginEvent);
	}

	// ///////////////////////////////////////////////////////////////
	// TimerOnlineBase 钩子实现
	private static final class GameOnlineTimer extends OnlineTimer<Long> {
		private final @NotNull BGameOnlineTimer bTimer;

		GameOnlineTimer(@NotNull BGameOnlineTimer bTimer) {
			this.bTimer = bTimer;
		}

		@Override
		@NotNull Bean getTimerObj() {
			return bTimer.getTimerObj().getBean();
		}

		@Override
		long getSerialId() {
			return bTimer.getSerialId();
		}

		@Override
		long getLoginVersion() {
			return bTimer.getLoginVersion();
		}

		@Override
		@NotNull Long identity() {
			return bTimer.getRoleId();
		}
	}

	@Override
	@NotNull Timer timer() {
		return online.providerApp.zeze.getTimer();
	}

	@Override
	@NotNull String name() {
		return "TimerRole";
	}

	@Override
	@Nullable Long getLocalLoginVersion(@NotNull Long roleId) {
		return online.getLocalLoginVersion(roleId);
	}

	@Override
	@Nullable Long getSharedLoginVersion(@NotNull Long roleId) {
		var shared = online.getLoginOnlineShared(roleId);
		return shared != null ? Long.valueOf(shared.getLoginVersion()) : null;
	}

	@Override
	@Nullable Long getLoginVersion(@NotNull Long roleId) {
		return online.getLoginVersion(roleId);
	}

	@Override
	@NotNull OnlineTimer<Long> newOnlineTimer(@NotNull Long roleId, long loginVersion, long serialId,
											  @NotNull Bean timerObj) {
		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion, serialId);
		onlineTimer.getTimerObj().setBean(timerObj);
		return new GameOnlineTimer(onlineTimer);
	}

	@Override
	@Nullable OnlineTimer<Long> getOnlineTimer(@NotNull String timerId) {
		var bTimer = online._tRoleTimers().get(timerId);
		return bTimer != null ? new GameOnlineTimer(bTimer) : null;
	}

	@Override
	void insertOnlineTimer(@NotNull String timerId, @NotNull OnlineTimer<Long> onlineTimer) {
		online._tRoleTimers().insert(timerId, ((GameOnlineTimer)onlineTimer).bTimer);
	}

	@Override
	void removeOnlineTimer(@NotNull String timerId) {
		online._tRoleTimers().remove(timerId);
	}

	@Override
	@NotNull BOnlineTimers getOrAddLocalTimers(@NotNull Long roleId) {
		return online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
	}

	@Override
	@Nullable BOnlineTimers getLocalTimers(@NotNull Long roleId) {
		return online.getLocalBean(roleId, eOnlineTimers);
	}

	@Override
	void removeLocalTimers(@NotNull Long roleId) {
		online.removeLocalBean(roleId, eOnlineTimers);
	}

	@Override
	void transmitSimple(@NotNull Long target, @NotNull BTransmitSimpleTimer p) {
		online.transmitEmbed(target, eTransmitSimpleTimer, List.of(target),
				new Binary(ByteBuffer.encode(p)), false);
	}

	@Override
	void transmitCron(@NotNull Long target, @NotNull BTransmitCronTimer p) {
		online.transmitEmbed(target, eTransmitCronTimer, List.of(target),
				new Binary(ByteBuffer.encode(p)), false);
	}

	@Override
	void transmitCancel(@NotNull Long target, @NotNull String timerId, long loginVersion) {
		var p = new BTransmitCancelRoleTimer();
		p.setTimerId(timerId);
		p.setRoleId(target);
		p.setLoginVersion(loginVersion);
		online.transmitEmbed(target, eTransmitCancelRoleTimer, List.of(target),
				new Binary(ByteBuffer.encode(p)), false);
	}

	@Override
	@NotNull String identityString(@NotNull Long roleId) {
		return "roleId=" + roleId;
	}

	@Override
	void fillContext(@NotNull Long roleId, @NotNull TimerContext context) {
		context.roleId = roleId;
	}

	// ///////////////////////////////////////////////////////////////
	// Online Named Timer
	// 本进程内的有名字定时器，名字仅在本进程内唯一。

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(long, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, long delay, long period, long times,
									   long endTime, @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) {
		return scheduleOnlineNamed(roleId, timerId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(long, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, long delay, long period, long times,
									   long endTime, @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData, @NotNull String oneByOneKey) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnlineNamed(roleId, timerId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(long, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, long delay, long period, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		return scheduleOnlineNamedHot(roleId, timerId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(long, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, long delay, long period, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData, @NotNull String oneByOneKey) {
		return scheduleOnlineNamedHot(roleId, timerId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(long, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, @NotNull String cron, long times,
									   long endTime, @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) throws ParseException {
		return scheduleOnlineNamed(roleId, timerId, TimerSpec.ofCron(cron).times(times).endTime(endTime),
				handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(long, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, @NotNull String cron, long times,
									   long endTime, @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData, @NotNull String oneByOneKey) throws ParseException {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnlineNamed(roleId, timerId, TimerSpec.ofCron(cron).times(times).endTime(endTime)
				.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(long, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, @NotNull String cron, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) throws ParseException {
		return scheduleOnlineNamedHot(roleId, timerId, TimerSpec.ofCron(cron).times(times)
				.endTime(endTime), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(long, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, @NotNull String cron, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData,
										  @NotNull String oneByOneKey) throws ParseException {
		return scheduleOnlineNamedHot(roleId, timerId, TimerSpec.ofCron(cron).times(times).endTime(endTime)
				.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	// ///////////////////////////////////////////////////////////////
	// Online Timer

	/**
	 * @deprecated 使用 {@link #scheduleOnline(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnline(long roleId, long delay, long period, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		return scheduleOnline(roleId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnline(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnline(long roleId, long delay, long period, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										  @NotNull String oneByOneKey) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnline(roleId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnlineHot(long roleId, long delay, long period, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) {
		return scheduleOnlineHot(roleId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnlineHot(long roleId, long delay, long period, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData, @NotNull String oneByOneKey) {
		return scheduleOnlineHot(roleId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnline(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnline(long roleId, @NotNull String cron, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) throws ParseException {
		return scheduleOnline(roleId, TimerSpec.ofCron(cron).times(times).endTime(endTime),
				handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnline(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnline(long roleId, @NotNull String cron, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										  @NotNull String oneByOneKey) throws ParseException {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnline(roleId, TimerSpec.ofCron(cron).times(times).endTime(endTime)
				.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnlineHot(long roleId, @NotNull String cron, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) throws ParseException {
		return scheduleOnlineHot(roleId, TimerSpec.ofCron(cron).times(times).endTime(endTime),
				handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnlineHot(long roleId, @NotNull String cron, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData,
											 @NotNull String oneByOneKey) throws ParseException {
		return scheduleOnlineHot(roleId, TimerSpec.ofCron(cron).times(times).endTime(endTime)
				.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	// ///////////////////////////////////////////////////////////////
	// Spec 入口
	// 推荐使用TimerSpec描述调度参数，避免超长的参数列表。

	public @NotNull String scheduleOnline(long roleId, @NotNull TimerSpec spec,
										  @NotNull Class<? extends TimerHandle> handleClass) {
		return scheduleOnline(roleId, spec, handleClass, null);
	}

	public @NotNull String scheduleOnline(long roleId, @NotNull TimerSpec spec,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnlineImpl(roleId, spec, handleClass, customData);
	}

	@NotNull String scheduleOnlineImpl(long roleId, @NotNull TimerSpec spec,
									   @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) {
		var timerId = newAutoTimerId();
		switch (spec) {
		case SimpleTimerSpec s -> scheduleOnline(false, roleId, timerId, s.build(), handleClass, customData, false);
		case CronTimerSpec c -> scheduleOnline(false, roleId, timerId, c.build(), handleClass, customData, false);
		}
		return timerId;
	}

	public @NotNull String scheduleOnlineHot(long roleId, @NotNull TimerSpec spec,
											 @NotNull Class<? extends TimerHandle> handleClass) {
		return scheduleOnlineHot(roleId, spec, handleClass, null);
	}

	public @NotNull String scheduleOnlineHot(long roleId, @NotNull TimerSpec spec,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) {
		var timerId = newAutoTimerId();
		switch (spec) {
		case SimpleTimerSpec s -> scheduleOnline(true, roleId, timerId, s.build(), handleClass, customData, false);
		case CronTimerSpec c -> scheduleOnline(true, roleId, timerId, c.build(), handleClass, customData, false);
		}
		return timerId;
	}

	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, @NotNull TimerSpec spec,
									   @NotNull Class<? extends TimerHandle> handleClass) {
		return scheduleOnlineNamed(roleId, timerId, spec, handleClass, null);
	}

	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, @NotNull TimerSpec spec,
									   @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnlineNamedImpl(roleId, timerId, spec, handleClass, customData);
	}

	boolean scheduleOnlineNamedImpl(long roleId, @NotNull String timerId, @NotNull TimerSpec spec,
									@NotNull Class<? extends TimerHandle> handleClass,
									@Nullable Bean customData) {
		if (!checkNamedTimerId(timerId))
			return false;
		switch (spec) {
		case SimpleTimerSpec s -> scheduleOnline(false, roleId, timerId, s.build(), handleClass, customData, false);
		case CronTimerSpec c -> scheduleOnline(false, roleId, timerId, c.build(), handleClass, customData, false);
		}
		return true;
	}

	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, @NotNull TimerSpec spec,
										  @NotNull Class<? extends TimerHandle> handleClass) {
		return scheduleOnlineNamedHot(roleId, timerId, spec, handleClass, null);
	}

	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, @NotNull TimerSpec spec,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		if (!checkNamedTimerId(timerId))
			return false;
		switch (spec) {
		case SimpleTimerSpec s -> scheduleOnline(true, roleId, timerId, s.build(), handleClass, customData, false);
		case CronTimerSpec c -> scheduleOnline(true, roleId, timerId, c.build(), handleClass, customData, false);
		}
		return true;
	}

	// ///////////////////////////////////////////////////////////////
	// 取消
	public boolean cancel(@Nullable String timerId, long roleId) {
		return cancelOnline(timerId, roleId) || cancelOffline(timerId); // offline 使用旧的参数调用。
	}

	public boolean cancelOnline(@Nullable String timerId, long roleId) {
		return cancelOnline(timerId, roleId, false);
	}

	public boolean cancelOffline(@Nullable String timerId) {
		if (timerId == null)
			return true; // 取消不存在的timer，认为成功。

		var timer = online.providerApp.zeze.getTimer();
		var index = timer.tIndexs().get(timerId);
		if (index == null)
			return false;
		var node = timer.tNodes().get(index.getNodeId());
		if (node == null)
			return false;
		var bTimer = node.getTimers().get(timerId);
		if (bTimer == null)
			return false;
		var customData = bTimer.getCustomData().getBean();
		if (!(customData instanceof BOfflineRoleCustom))
			return false;
		return cancelOffline(timerId, ((BOfflineRoleCustom)customData).getRoleId());
	}

	public boolean cancelOffline(@Nullable String timerId, long roleId) {
		if (timerId == null)
			return true; // 取消不存在的timer，认为成功。

		online.providerApp.zeze.getTimer().cancel(timerId);
		var bTimers = online._tRoleOfflineTimers().get(roleId);
		var r = bTimers != null && bTimers.getOfflineTimers().remove(timerId) != null;
		if (r)
			logger.debug("cancel offline timer: timerId={}, roleId={}", timerId, roleId);
		return r;
	}

	// ///////////////////////////////////////////////////////////////
	// Offline Timer

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, long delay, long period,
										long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return scheduleOfflineNamed(timerId, roleId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime).missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, long delay, long period,
										long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										@NotNull String oneByOneKey) {
		return scheduleOfflineNamed(timerId, roleId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime).missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	private void scheduleOffline(@NotNull String timerId, long roleId, @NotNull SimpleTimerSpec builder,
								 @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData) {
		Reflect.checkDefaultConstructor(handleClass);
		var logoutVersion = online.getLogoutVersion(roleId);
		if (logoutVersion == null)
			throw new IllegalStateException("not logout. roleId=" + roleId + ", timerId=" + timerId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineRoleCustom(timerId, roleId, logoutVersion, handleClass.getName(),
				online.getOnlineSetName());
		if (customData != null) {
			Timer.register(customData);
			custom.getCustomData().setBean(customData);
			timer.tryRecordBeanHotModuleWhileCommit(customData);
		}
		timer.schedule(timerId, builder.build(), OfflineHandle.class, custom);
		var config = timer.zeze.getConfig();
		var offline = online._tRoleOfflineTimers().getOrAdd(roleId);
		if (offline.getOfflineTimers().size() > config.getOfflineTimerLimit()) {
			// throw new IllegalStateException("too many offline timers. roleId=" + roleId + " size=" + offline.getOfflineTimers().size());
			logger.error("scheduleOffline(simple): too many timers. roleId={}, timerId={}, handle={}, size={} > {}",
					roleId, timerId, handleClass.getName(), offline.getOfflineTimers().size(),
					config.getOfflineTimerLimit());
		}
		if (offline.getOfflineTimers().putIfAbsent(timerId, config.getServerId()) != null)
			throw new IllegalStateException("duplicate timerId. roleId=" + roleId + ", timerId=" + timerId);
		logger.debug("add offline simple timer: timerId={}, roleId={}, handle={}",
				timerId, roleId, handleClass.getName());
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
										   int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) {
		return scheduleOffline(roleId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime).missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
										   int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData, @NotNull String oneByOneKey) {
		return scheduleOffline(roleId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime).missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
										   @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) {
		return scheduleOffline(roleId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, @NotNull String cron,
										long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) throws ParseException {
		return scheduleOfflineNamed(timerId, roleId, TimerSpec.ofCron(cron).times(times).endTime(endTime)
				.missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, @NotNull String cron,
										long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData,
										@NotNull String oneByOneKey) throws ParseException {
		return scheduleOfflineNamed(timerId, roleId, TimerSpec.ofCron(cron).times(times).endTime(endTime)
				.missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	private void scheduleOffline(@NotNull String timerId, long roleId, @NotNull CronTimerSpec builder,
								 @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData,
								 @Nullable BIndex index) {
		Reflect.checkDefaultConstructor(handleClass);
		var logoutVersion = online.getLogoutVersion(roleId);
		if (logoutVersion == null)
			throw new IllegalStateException("not logout. roleId=" + roleId + ", timerId=" + timerId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineRoleCustom(timerId, roleId, logoutVersion, handleClass.getName(),
				online.getOnlineSetName());
		if (customData != null) {
			Timer.register(customData);
			custom.getCustomData().setBean(customData);
			timer.tryRecordBeanHotModuleWhileCommit(customData);
		}
		if (index != null) {
			if (timer.cronEquals(index, timerId, builder, OfflineHandle.class, custom))
				return;
			cancel(timerId, roleId); // 先取消,下面再重建
		}
		timer.schedule(timerId, builder.build(), OfflineHandle.class, custom);
		var config = timer.zeze.getConfig();
		var offline = online._tRoleOfflineTimers().getOrAdd(roleId);
		if (offline.getOfflineTimers().size() > config.getOfflineTimerLimit()) {
			// throw new IllegalStateException("too many offline timers. roleId=" + roleId + " size=" + offline.getOfflineTimers().size());
			logger.error("scheduleOffline(cron): too many timers. roleId={}, timerId={}, handle={}, size={} > {}",
					roleId, timerId, handleClass.getName(), offline.getOfflineTimers().size(),
					config.getOfflineTimerLimit());
		}
		if (offline.getOfflineTimers().putIfAbsent(timerId, config.getServerId()) != null)
			throw new IllegalStateException("duplicate timerId. roleId=" + roleId + ", timerId=" + timerId);
		logger.debug("add offline cron timer: timerId={}, roleId={}, handle={}",
				timerId, roleId, handleClass.getName());
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOffline(long roleId, @NotNull String cron, long times, long endTime,
										   int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) throws ParseException {
		return scheduleOffline(roleId, TimerSpec.ofCron(cron).times(times).endTime(endTime)
				.missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOffline(long roleId, @NotNull String cron, long times, long endTime,
										   int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData,
										   @NotNull String oneByOneKey) throws ParseException {
		return scheduleOffline(roleId, TimerSpec.ofCron(cron).times(times).endTime(endTime)
				.missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(long, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOffline(long roleId, @NotNull String cron, long times, long endTime,
										   @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) throws ParseException {
		return scheduleOffline(roleId, TimerSpec.ofCron(cron).times(times).endTime(endTime),
				handleClass, customData);
	}

	// ///////////////////////////////////////////////////////////////
	// Offline Spec 入口

	public @NotNull String scheduleOffline(long roleId, @NotNull TimerSpec spec,
										   @NotNull Class<? extends TimerHandle> handleClass) {
		return scheduleOffline(roleId, spec, handleClass, null);
	}

	public @NotNull String scheduleOffline(long roleId, @NotNull TimerSpec spec,
										   @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) {
		var timerId = newAutoTimerId();
		switch (spec) {
		case SimpleTimerSpec s -> scheduleOffline(timerId, roleId, s, handleClass, customData);
		case CronTimerSpec c -> scheduleOffline(timerId, roleId, c, handleClass, customData, null);
		}
		return timerId;
	}

	public boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, @NotNull TimerSpec spec,
										@NotNull Class<? extends TimerHandle> handleClass) {
		return scheduleOfflineNamed(timerId, roleId, spec, handleClass, null);
	}

	public boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, @NotNull TimerSpec spec,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		var zeze = online.providerApp.zeze;
		var index = zeze.getTimer().tIndexs().get(timerId);
		if (index != null && index.getServerId() != zeze.getConfig().getServerId())
			return false; // 已经被其它gs调度
		switch (spec) {
		case SimpleTimerSpec s -> {
			if (index != null)
				cancel(timerId, roleId); // 先取消,下面再重建
			scheduleOffline(timerId, roleId, s, handleClass, customData);
		}
		case CronTimerSpec c -> scheduleOffline(timerId, roleId, c, handleClass, customData, index);
		}
		return true;
	}

	/// ///////////////////////////////////////////////////////////////////////////////////////
	// 内部实现
	public static class OfflineHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var offlineCustom = (BOfflineRoleCustom)context.customData;
			//noinspection DataFlowIssue
			var roleId = offlineCustom.getRoleId();
			var onlineSetName = offlineCustom.getOnlineSetName();
			// 检查版本号，不正确的登录版本号表示过期的timer，取消掉即可。
			var timer = context.timer;
			var loginVersion = timer.getRoleTimer(onlineSetName).online.getLogoutVersion(roleId);
			if (loginVersion != null && loginVersion == offlineCustom.getLoginVersion()) {
				context.roleId = roleId;
				var userCustom = offlineCustom.getCustomData().getBean();
				context.customData = userCustom instanceof EmptyBean ? null : userCustom;
				timer.findTimerHandle(offlineCustom.getHandleName()).onTimer(context);
			} else {
				var timerId = offlineCustom.getTimerName();
				timer.cancel(timerId);
				var providerImpl = timer.zeze.getProviderApp().providerImplement;
				var online = providerImpl instanceof ProviderWithOnline
						? ((ProviderWithOnline)providerImpl).getOnline(onlineSetName)
						: timer.getDefaultOnline();
				if (online != null) {
					var offlineTimers = online._tRoleOfflineTimers().get(roleId);
					if (offlineTimers != null) {
						offlineTimers.getOfflineTimers().remove(timerId);
						logger.debug("OfflineHandle: cancel offline timer: timerId={}, roleId={}, handle={}",
								timerId, roleId, offlineCustom.getHandleName());
					}
				}
			}
		}
	}

	private long onLoginEvent(@NotNull Object sender, @NotNull EventDispatcher.EventArgument arg) {
		var roleId = ((LoginArgument)arg).roleId;
		var offlineTimers = online._tRoleOfflineTimers().get(roleId);
		if (offlineTimers != null) {
			var timer = online.providerApp.zeze.getTimer();
			for (var timerId : offlineTimers.getOfflineTimers().keySet())
				timer.cancel(timerId);
			// 嵌入本地服务器事件事务中，
			// 删除之后，如果上面的redirectCancel失败，
			// 那么该timer触发的时候会检测到版本号不一致，
			// 然后timer最终也会被cancel掉。
			online._tRoleOfflineTimers().remove(roleId);
			logger.debug("onLoginEvent: cancel all offline timer: roleId={}, size={}",
					roleId, offlineTimers.getOfflineTimers().size());
		}
		return 0;
	}

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(@NotNull Object sender, @NotNull EventDispatcher.EventArgument arg) {
		var local = ((LocalRemoveEventArgument)arg).local;
		if (local != null) {
			var bAny = local.getDatas().get(eOnlineTimers);
			if (bAny != null)
				onLocalRemove((BOnlineTimers)bAny.getAny().getBean());
		}
		return 0;
	}

	private long transmitOnlineCronTimer(long sender, long target, @Nullable Binary parameter)
			throws ReflectiveOperationException {
		return onTransmitCronTimer(target, parameter);
	}

	private long transmitOnlineSimpleTimer(long sender, long target, @Nullable Binary parameter)
			throws ReflectiveOperationException {
		return onTransmitSimpleTimer(target, parameter);
	}

	private long transmitCancelRoleTimer(long sender, long target, @Nullable Binary parameter) {
		if (parameter == null)
			return 0;
		var p = new BTransmitCancelRoleTimer();
		p.decode(ByteBuffer.Wrap(parameter));
		onTransmitCancel(p.getTimerId(), p.getRoleId(), p.getLoginVersion());
		return 0;
	}
}
