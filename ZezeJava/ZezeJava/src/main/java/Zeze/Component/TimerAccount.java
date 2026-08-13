package Zeze.Component;

import java.text.ParseException;
import java.util.List;
import Zeze.Arch.LocalRemoveEventArgument;
import Zeze.Arch.LoginArgument;
import Zeze.Arch.Online;
import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Builtin.Timer.BAccountClientId;
import Zeze.Builtin.Timer.BArchOnlineTimer;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BOfflineAccountCustom;
import Zeze.Builtin.Timer.BOnlineTimers;
import Zeze.Builtin.Timer.BTransmitCancelAccountTimer;
import Zeze.Builtin.Timer.BTransmitCronTimer;
import Zeze.Builtin.Timer.BTransmitSimpleTimer;
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
public class TimerAccount extends TimerOnlineBase<BAccountClientId> {
	private static final @NotNull Logger logger = LogManager.getLogger(TimerAccount.class);
	//public static final String eTimerHandleName = "Zeze.Component.TimerArchOnline.Handle";
	public static final String eOnlineTimers = "Zeze.Component.TimerArchOnline";
	public static final String eTransmitCronTimer = "Zeze.TimerAccount.TransmitCronTimer";
	public static final String eTransmitSimpleTimer = "Zeze.TimerAccount.TransmitSimpleTimer";
	public static final String eTransmitCancelAccountTimer = "Zeze.TimerAccount.TransmitCancelAccountTimer";

	private final @NotNull Online online;

	TimerAccount(@NotNull Online online) {
		this.online = online;

		online.getTransmitActions().put(eTransmitCronTimer, this::transmitOnlineCronTimer);
		online.getTransmitActions().put(eTransmitSimpleTimer, this::transmitOnlineSimpleTimer);
		online.getTransmitActions().put(eTransmitCancelAccountTimer, this::transmitCancelAccountTimer);
		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().add(EventDispatcher.Mode.RunEmbed, this::onLocalRemoveEvent);
		online.getLoginEvents().add(EventDispatcher.Mode.RunEmbed, this::onLoginEvent);
	}

	// ///////////////////////////////////////////////////////////////
	// TimerOnlineBase 钩子实现
	private static final class ArchOnlineTimer extends OnlineTimer<BAccountClientId> {
		private final @NotNull BArchOnlineTimer bTimer;

		ArchOnlineTimer(@NotNull BArchOnlineTimer bTimer) {
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
		@NotNull BAccountClientId identity() {
			return new BAccountClientId(bTimer.getAccount(), bTimer.getClientId());
		}
	}

	@Override
	@NotNull Timer timer() {
		return online.providerApp.zeze.getTimer();
	}

	@Override
	@NotNull String name() {
		return "TimerAccount";
	}

	@Override
	@Nullable Long getLocalLoginVersion(@NotNull BAccountClientId id) {
		return online.getLocalLoginVersion(id.getAccount(), id.getClientId());
	}

	@Override
	@Nullable Long getSharedLoginVersion(@NotNull BAccountClientId id) {
		return online.getLoginVersion(id.getAccount(), id.getClientId());
	}

	@Override
	@Nullable Long getLoginVersion(@NotNull BAccountClientId id) {
		return online.getLoginVersion(id.getAccount(), id.getClientId());
	}

	@Override
	@NotNull OnlineTimer<BAccountClientId> newOnlineTimer(@NotNull BAccountClientId id, long loginVersion,
														  long serialId, @NotNull Bean timerObj) {
		var onlineTimer = new BArchOnlineTimer(id.getAccount(), id.getClientId(), loginVersion, serialId);
		onlineTimer.getTimerObj().setBean(timerObj);
		return new ArchOnlineTimer(onlineTimer);
	}

	@Override
	@Nullable OnlineTimer<BAccountClientId> getOnlineTimer(@NotNull String timerId) {
		var bTimer = timer().tAccountTimers().get(timerId);
		return bTimer != null ? new ArchOnlineTimer(bTimer) : null;
	}

	@Override
	void insertOnlineTimer(@NotNull String timerId, @NotNull OnlineTimer<BAccountClientId> onlineTimer) {
		timer().tAccountTimers().insert(timerId, ((ArchOnlineTimer)onlineTimer).bTimer);
	}

	@Override
	void removeOnlineTimer(@NotNull String timerId) {
		timer().tAccountTimers().remove(timerId);
	}

	@Override
	@NotNull BOnlineTimers getOrAddLocalTimers(@NotNull BAccountClientId id) {
		return online.getOrAddLocalBean(id.getAccount(), id.getClientId(), eOnlineTimers, new BOnlineTimers());
	}

	@Override
	@Nullable BOnlineTimers getLocalTimers(@NotNull BAccountClientId id) {
		return online.getLocalBean(id.getAccount(), id.getClientId(), eOnlineTimers);
	}

	@Override
	void removeLocalTimers(@NotNull BAccountClientId id) {
		online.removeLocalBean(id.getAccount(), id.getClientId(), eOnlineTimers);
	}

	@Override
	void transmitSimple(@NotNull BAccountClientId target, @NotNull BTransmitSimpleTimer p) {
		online.transmit(target.getAccount(), target.getClientId(), eTransmitSimpleTimer,
				List.of(new BLoginKey(target.getAccount(), target.getClientId())), p);
	}

	@Override
	void transmitCron(@NotNull BAccountClientId target, @NotNull BTransmitCronTimer p) {
		online.transmit(target.getAccount(), target.getClientId(), eTransmitCronTimer,
				List.of(new BLoginKey(target.getAccount(), target.getClientId())), p);
	}

	@Override
	void transmitCancel(@NotNull BAccountClientId target, @NotNull String timerId, long loginVersion) {
		var p = new BTransmitCancelAccountTimer();
		p.setTimerId(timerId);
		p.setAccount(target.getAccount());
		p.setClientId(target.getClientId());
		p.setLoginVersion(loginVersion);
		online.transmit(target.getAccount(), target.getClientId(), eTransmitCancelAccountTimer,
				List.of(new BLoginKey(target.getAccount(), target.getClientId())), p);
	}

	@Override
	@NotNull String identityString(@NotNull BAccountClientId id) {
		return "account=" + id.getAccount() + " clientId=" + id.getClientId();
	}

	@Override
	void fillContext(@NotNull BAccountClientId id, @NotNull TimerContext context) {
		context.account = id.getAccount();
		context.clientId = id.getClientId();
	}

	// ///////////////////////////////////////////////////////////////
	// Online Named Timer
	// 本进程内的有名字定时器，名字仅在本进程内唯一。

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(String, String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									   long delay, long period, long times, long endTime,
									   @NotNull Class<? extends TimerHandle> handle, @Nullable Bean customData) {
		return scheduleOnlineNamed(account, clientId, timerId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(String, String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									   long delay, long period, long times, long endTime,
									   @NotNull Class<? extends TimerHandle> handle, @Nullable Bean customData,
									   @NotNull String oneByOneKey) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnlineNamed(account, clientId, timerId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime)
						.oneByOneKey(oneByOneKey), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(String, String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										  long delay, long period, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		return scheduleOnlineNamedHot(account, clientId, timerId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime), handleClass,
				customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(String, String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										  long delay, long period, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										  @NotNull String oneByOneKey) {
		return scheduleOnlineNamedHot(account, clientId, timerId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime)
						.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(String, String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									   @NotNull String cron, long times, long endTime,
									   @NotNull Class<? extends TimerHandle> handle,
									   @Nullable Bean customData) throws ParseException {
		return scheduleOnlineNamed(account, clientId, timerId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(String, String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									   @NotNull String cron, long times, long endTime,
									   @NotNull Class<? extends TimerHandle> handle, @Nullable Bean customData,
									   @NotNull String oneByOneKey) throws ParseException {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnlineNamed(account, clientId, timerId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime).oneByOneKey(oneByOneKey), handle,
				customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(String, String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										  @NotNull String cron, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) throws ParseException {
		return scheduleOnlineNamedHot(account, clientId, timerId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(String, String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										  @NotNull String cron, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										  @NotNull String oneByOneKey) throws ParseException {
		return scheduleOnlineNamedHot(account, clientId, timerId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime).oneByOneKey(oneByOneKey), handleClass,
				customData);
	}

	// ///////////////////////////////////////////////////////////////
	// Online Timer

	/**
	 * @deprecated 使用 {@link #scheduleOnline(String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId, long delay, long period,
										  long times, long endTime, @NotNull Class<? extends TimerHandle> handle,
										  @Nullable Bean customData) {
		return scheduleOnline(account, clientId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnline(String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId, long delay, long period,
										  long times, long endTime, @NotNull Class<? extends TimerHandle> handle,
										  @Nullable Bean customData, @NotNull String oneByOneKey) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnline(account, clientId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime)
						.oneByOneKey(oneByOneKey), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId,
											 long delay, long period, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) {
		return scheduleOnlineHot(account, clientId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime), handleClass,
				customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId,
											 long delay, long period, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData, @NotNull String oneByOneKey) {
		return scheduleOnlineHot(account, clientId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime)
						.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnline(String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId, @NotNull String cron,
										  long times, long endTime, @NotNull Class<? extends TimerHandle> handle,
										  @Nullable Bean customData) throws ParseException {
		return scheduleOnline(account, clientId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnline(String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId, @NotNull String cron,
										  long times, long endTime, @NotNull Class<? extends TimerHandle> handle,
										  @Nullable Bean customData,
										  @NotNull String oneByOneKey) throws ParseException {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnline(account, clientId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime).oneByOneKey(oneByOneKey), handle,
				customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId, @NotNull String cron,
											 long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) throws ParseException {
		return scheduleOnlineHot(account, clientId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId, @NotNull String cron,
											 long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData,
											 @NotNull String oneByOneKey) throws ParseException {
		return scheduleOnlineHot(account, clientId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime).oneByOneKey(oneByOneKey), handleClass,
				customData);
	}

	// ///////////////////////////////////////////////////////////////
	// Builder 入口
	// 推荐使用Builder描述调度参数，避免超长的参数列表。

	public @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId,
										  @NotNull BSimpleTimerBuilder builder,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnlineImpl(account, clientId, builder, handleClass, customData);
	}

	@NotNull String scheduleOnlineImpl(@NotNull String account, @NotNull String clientId,
									   @NotNull BSimpleTimerBuilder builder,
									   @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) {
		var timerId = newAutoTimerId();
		scheduleOnline(false, new BAccountClientId(account, clientId), timerId, builder.build(), handleClass,
				customData, false);
		return timerId;
	}

	public @NotNull String scheduleOnline(@NotNull String account, @NotNull String clientId,
										  @NotNull BCronTimerBuilder builder,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) throws ParseException {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnlineImpl(account, clientId, builder, handleClass, customData);
	}

	@NotNull String scheduleOnlineImpl(@NotNull String account, @NotNull String clientId,
									   @NotNull BCronTimerBuilder builder,
									   @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) throws ParseException {
		var timerId = newAutoTimerId();
		scheduleOnline(false, new BAccountClientId(account, clientId), timerId, builder.build(), handleClass,
				customData, false);
		return timerId;
	}

	public @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId,
											 @NotNull BSimpleTimerBuilder builder,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) {
		var timerId = newAutoTimerId();
		scheduleOnline(true, new BAccountClientId(account, clientId), timerId, builder.build(), handleClass,
				customData, false);
		return timerId;
	}

	public @NotNull String scheduleOnlineHot(@NotNull String account, @NotNull String clientId,
											 @NotNull BCronTimerBuilder builder,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) throws ParseException {
		var timerId = newAutoTimerId();
		scheduleOnline(true, new BAccountClientId(account, clientId), timerId, builder.build(), handleClass,
				customData, false);
		return timerId;
	}

	public boolean scheduleOnlineNamed(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									   @NotNull BSimpleTimerBuilder builder,
									   @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnlineNamedImpl(account, clientId, timerId, builder, handleClass, customData);
	}

	boolean scheduleOnlineNamedImpl(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									@NotNull BSimpleTimerBuilder builder,
									@NotNull Class<? extends TimerHandle> handleClass,
									@Nullable Bean customData) {
		if (!checkNamedTimerId(timerId))
			return false;
		scheduleOnline(false, new BAccountClientId(account, clientId), timerId, builder.build(), handleClass,
				customData, false);
		return true;
	}

	public boolean scheduleOnlineNamed(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									   @NotNull BCronTimerBuilder builder,
									   @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) throws ParseException {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		return scheduleOnlineNamedImpl(account, clientId, timerId, builder, handleClass, customData);
	}

	boolean scheduleOnlineNamedImpl(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
									@NotNull BCronTimerBuilder builder,
									@NotNull Class<? extends TimerHandle> handleClass,
									@Nullable Bean customData) throws ParseException {
		if (!checkNamedTimerId(timerId))
			return false;
		scheduleOnline(false, new BAccountClientId(account, clientId), timerId, builder.build(), handleClass,
				customData, false);
		return true;
	}

	public boolean scheduleOnlineNamedHot(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										  @NotNull BSimpleTimerBuilder builder,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		if (!checkNamedTimerId(timerId))
			return false;
		scheduleOnline(true, new BAccountClientId(account, clientId), timerId, builder.build(), handleClass,
				customData, false);
		return true;
	}

	public boolean scheduleOnlineNamedHot(@NotNull String account, @NotNull String clientId, @NotNull String timerId,
										  @NotNull BCronTimerBuilder builder,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) throws ParseException {
		if (!checkNamedTimerId(timerId))
			return false;
		scheduleOnline(true, new BAccountClientId(account, clientId), timerId, builder.build(), handleClass,
				customData, false);
		return true;
	}

	// ///////////////////////////////////////////////////////////////
	// 取消
	public boolean cancel(@Nullable String timerId, @NotNull String account, @NotNull String clientId) {
		if (timerId == null)
			return true;
		return cancelOnline(timerId, account, clientId) || cancelOffline(timerId, account, clientId);
	}

	public boolean cancelOnline(@Nullable String timerId, @NotNull String account, @NotNull String clientId) {
		return cancelOnline(timerId, new BAccountClientId(account, clientId), false);
	}

	public boolean cancelOffline(@Nullable String timerId, @NotNull String account, @NotNull String clientId) {
		if (timerId == null)
			return true;
		var timer = online.providerApp.zeze.getTimer();
		timer.cancel(timerId);
		var bTimers = timer.tAccountOfflineTimers().get(new BAccountClientId(account, clientId));
		if (bTimers != null)
			bTimers.getOfflineTimers().remove(timerId);
		return true;
	}

	// ///////////////////////////////////////////////////////////////
	// Offline Timer

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
										long delay, long period, long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return scheduleOfflineNamed(timerId, account, clientId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime)
						.missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
										long delay, long period, long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										@NotNull String oneByOneKey) {
		return scheduleOfflineNamed(timerId, account, clientId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime)
						.missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	private void scheduleOffline(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
								 @NotNull BSimpleTimerBuilder builder,
								 @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData) {
		Reflect.checkDefaultConstructor(handleClass);
		var logoutVersion = online.getLogoutVersion(account, clientId);
		if (logoutVersion == null) {
			throw new IllegalStateException("not logout. account=" + account + " clientId=" + clientId
					+ " timerId=" + timerId);
		}

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineAccountCustom(timerId, account, clientId, logoutVersion, handleClass.getName());
		if (customData != null) {
			Timer.register(customData);
			custom.getCustomData().setBean(customData);
			timer.tryRecordBeanHotModuleWhileCommit(customData);
		}
		timer.schedule(timerId, builder.build(), OfflineHandle.class, custom);
		var config = timer.zeze.getConfig();
		var offline = timer.tAccountOfflineTimers().getOrAdd(new BAccountClientId(account, clientId));
		if (offline.getOfflineTimers().size() > config.getOfflineTimerLimit()) {
			// throw new IllegalStateException("too many offline timers. account=" + account + " clientId=" + clientId + " size=" + offline.getOfflineTimers().size());
			logger.error("scheduleOffline(simple): too many timers. account={}, clientId={}, timerId={}, handle={}, size={} > {}",
					account, clientId, timerId, handleClass.getName(),
					offline.getOfflineTimers().size(), config.getOfflineTimerLimit());
		}
		if (offline.getOfflineTimers().putIfAbsent(timerId, config.getServerId()) != null) {
			throw new IllegalStateException("duplicate timerId. account=" + account + " clientId=" + clientId
					+ " timerId=" + timerId);
		}
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOffline(@NotNull String account, @NotNull String clientId,
										   long delay, long period, long times, long endTime, int missfirePolicy,
										   @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) {
		return scheduleOffline(account, clientId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime)
						.missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(String, String, BSimpleTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOffline(@NotNull String account, @NotNull String clientId,
										   long delay, long period, long times, long endTime, int missfirePolicy,
										   @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										   @NotNull String oneByOneKey) {
		return scheduleOffline(account, clientId,
				BSimpleTimerBuilder.ofDelay(delay).period(period).times(times).endTime(endTime)
						.missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
										@NotNull String cron, long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) throws ParseException {
		return scheduleOfflineNamed(timerId, account, clientId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime).missfirePolicy(missfirePolicy),
				handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
										@NotNull String cron, long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										@NotNull String oneByOneKey) throws ParseException {
		return scheduleOfflineNamed(timerId, account, clientId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime).missfirePolicy(missfirePolicy)
						.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	private void scheduleOffline(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
								 @NotNull BCronTimerBuilder builder,
								 @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
								 @Nullable BIndex index) throws ParseException {
		Reflect.checkDefaultConstructor(handleClass);
		var logoutVersion = online.getLogoutVersion(account, clientId);
		if (logoutVersion == null) {
			throw new IllegalStateException("not logout. account=" + account + " clientId=" + clientId
					+ " timerId=" + timerId);
		}

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineAccountCustom(timerId, account, clientId, logoutVersion, handleClass.getName());
		if (customData != null) {
			Timer.register(customData);
			custom.getCustomData().setBean(customData);
			timer.tryRecordBeanHotModuleWhileCommit(customData);
		}
		if (index != null) {
			if (timer.cronEquals(index, timerId, builder, OfflineHandle.class, custom))
				return;
			cancel(timerId, account, clientId); // 先取消,下面再重建
		}
		timer.schedule(timerId, builder.build(), OfflineHandle.class, custom);
		var config = timer.zeze.getConfig();
		var offline = timer.tAccountOfflineTimers().getOrAdd(new BAccountClientId(account, clientId));
		if (offline.getOfflineTimers().size() > config.getOfflineTimerLimit()) {
			// throw new IllegalStateException("too many offline timers. account=" + account + " clientId=" + clientId + " size=" + offline.getOfflineTimers().size());
			logger.error("scheduleOffline(cron): too many timers. account={}, clientId={}, timerId={}, handle={}, size={} > {}",
					account, clientId, timerId, handleClass.getName(),
					offline.getOfflineTimers().size(), config.getOfflineTimerLimit());
		}
		if (offline.getOfflineTimers().putIfAbsent(timerId, config.getServerId()) != null) {
			throw new IllegalStateException("duplicate timerId. account=" + account + " clientId=" + clientId
					+ " timerId=" + timerId);
		}
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOffline(@NotNull String account, @NotNull String clientId, @NotNull String cron,
										   long times, long endTime, int missfirePolicy,
										   @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) throws ParseException {
		return scheduleOffline(account, clientId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime).missfirePolicy(missfirePolicy),
				handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(String, String, BCronTimerBuilder, Class, Bean)} 替代
	 */
	@Deprecated
	public @NotNull String scheduleOffline(@NotNull String account, @NotNull String clientId, @NotNull String cron,
										   long times, long endTime, int missfirePolicy,
										   @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										   @NotNull String oneByOneKey) throws ParseException {
		return scheduleOffline(account, clientId,
				BCronTimerBuilder.ofCron(cron).times(times).endTime(endTime).missfirePolicy(missfirePolicy)
						.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	// ///////////////////////////////////////////////////////////////
	// Offline Builder 入口

	public @NotNull String scheduleOffline(@NotNull String account, @NotNull String clientId,
										   @NotNull BSimpleTimerBuilder builder,
										   @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) {
		var timerId = newAutoTimerId();
		scheduleOffline(timerId, account, clientId, builder, handleClass, customData);
		return timerId;
	}

	public @NotNull String scheduleOffline(@NotNull String account, @NotNull String clientId,
										   @NotNull BCronTimerBuilder builder,
										   @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) throws ParseException {
		var timerId = newAutoTimerId();
		scheduleOffline(timerId, account, clientId, builder, handleClass, customData, null);
		return timerId;
	}

	public boolean scheduleOfflineNamed(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
										@NotNull BSimpleTimerBuilder builder,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		var zeze = online.providerApp.zeze;
		var index = zeze.getTimer().tIndexs().get(timerId);
		if (index != null) {
			if (index.getServerId() != zeze.getConfig().getServerId())
				return false; // 已经被其它gs调度
			cancel(timerId, account, clientId); // 先取消,下面再重建
		}
		scheduleOffline(timerId, account, clientId, builder, handleClass, customData);
		return true;
	}

	public boolean scheduleOfflineNamed(@NotNull String timerId, @NotNull String account, @NotNull String clientId,
										@NotNull BCronTimerBuilder builder,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) throws ParseException {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		var zeze = online.providerApp.zeze;
		var index = zeze.getTimer().tIndexs().get(timerId);
		if (index != null && index.getServerId() != zeze.getConfig().getServerId())
			return false; // 已经被其它gs调度
		scheduleOffline(timerId, account, clientId, builder, handleClass, customData, index);
		return true;
	}

	/// ///////////////////////////////////////////////////////////////////////////////////////
	// 内部实现
	public static class OfflineHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var offlineCustom = (BOfflineAccountCustom)context.customData;
			//noinspection DataFlowIssue
			var account = offlineCustom.getAccount();
			var clientId = offlineCustom.getClientId();
			// 检查版本号，不正确的登录版本号表示过期的timer，取消掉即可。
			var timer = context.timer;
			var loginVersion = context.timer.getAccountTimer().online.getLogoutVersion(account, clientId);
			if (loginVersion != null && loginVersion == offlineCustom.getLoginVersion()) {
				context.account = account;
				context.clientId = clientId;
				context.customData = offlineCustom.getCustomData().getBean();
				if (context.customData instanceof EmptyBean)
					context.customData = null;
				timer.findTimerHandle(offlineCustom.getHandleName()).onTimer(context);
			} else {
				var timerId = offlineCustom.getTimerName();
				context.timer.cancel(timerId);
				var offlineTimers = context.timer.tAccountOfflineTimers().get(new BAccountClientId(account, clientId));
				if (offlineTimers != null)
					offlineTimers.getOfflineTimers().remove(timerId);
			}
		}
	}

	private long onLoginEvent(@NotNull Object sender, @NotNull EventDispatcher.EventArgument arg) {
		var loginArg = (LoginArgument)arg;
		var loginKey = new BAccountClientId(loginArg.account, loginArg.clientId);
		var timer = online.providerApp.zeze.getTimer();
		var offlineTimers = timer.tAccountOfflineTimers().get(loginKey);
		// X: fix offlineTimers is null
		if (offlineTimers == null)
			return 0;
		for (var e : offlineTimers.getOfflineTimers().entrySet())
			timer.cancel(e.getKey());
		// 嵌入本地服务器事件事务中，
		// 删除之后，如果上面的redirectCancel失败，
		// 那么该timer触发的时候会检测到版本号不一致，
		// 然后timer最终也会被cancel掉。
		timer.tAccountOfflineTimers().remove(loginKey);
		return 0;
	}

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(@NotNull Object sender, @NotNull EventDispatcher.EventArgument arg) {
		var bAny = ((LocalRemoveEventArgument)arg).local.getDatas().get(eOnlineTimers);
		if (bAny != null)
			onLocalRemove((BOnlineTimers)bAny.getAny().getBean());
		return 0;
	}

	private long transmitOnlineCronTimer(@NotNull String senderAccount, @NotNull String senderClientId,
										 @NotNull String targetAccount, @NotNull String targetClientId,
										 @Nullable Binary parameter) throws ReflectiveOperationException {
		return onTransmitCronTimer(new BAccountClientId(targetAccount, targetClientId), parameter);
	}

	private long transmitOnlineSimpleTimer(@NotNull String senderAccount, @NotNull String senderClientId,
										   @NotNull String targetAccount, @NotNull String targetClientId,
										   @Nullable Binary parameter) throws ReflectiveOperationException {
		return onTransmitSimpleTimer(new BAccountClientId(targetAccount, targetClientId), parameter);
	}

	private long transmitCancelAccountTimer(@NotNull String senderAccount, @NotNull String senderClientId,
											@NotNull String targetAccount, @NotNull String targetClientId,
											@Nullable Binary parameter) {
		if (parameter == null)
			return 0;
		var p = new BTransmitCancelAccountTimer();
		p.decode(ByteBuffer.Wrap(parameter));
		onTransmitCancel(p.getTimerId(), new BAccountClientId(p.getAccount(), p.getClientId()), p.getLoginVersion());
		return 0;
	}
}
