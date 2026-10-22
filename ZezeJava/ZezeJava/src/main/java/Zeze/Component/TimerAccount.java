package Zeze.Component;

import java.util.Objects;

import java.util.List;
import Zeze.Application;
import Zeze.Arch.LocalRemoveEventArgument;
import Zeze.Arch.LoginArgument;
import Zeze.Arch.Online;
import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Builtin.Timer.BAccountClientId;
import Zeze.Builtin.Timer.BArchOnlineTimer;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BOfflineAccountCustom;
import Zeze.Builtin.Timer.BOnlineTimers;
import Zeze.Builtin.Timer.BTimer;
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
import org.jspecify.annotations.Nullable;

/**
 * 1. schedule，scheduleNamed 完全重新实现一套基于内存表和内存的。
 * 2. 不直接使用 Timer.schedule。但有如下关联。
 * 直接使用 Timer.timerIdAutoKey，使得返回的timerId共享一个名字空间。
 * 直接使用 Timer.timersFuture，从 ThreadPool 返回的future保存在这里。
 * <p>
 * online定时器的调度、触发、取消、转发逻辑在 {@link TimerOnlineBase} 中实现。
 */
public class TimerAccount extends TimerOnlineBase<BAccountClientId> {
	private static final Logger logger = LogManager.getLogger(TimerAccount.class);
	public static final String eOnlineTimers = "Zeze.Component.TimerArchOnline";
	public static final String eTransmitCronTimer = "Zeze.TimerAccount.TransmitCronTimer";
	public static final String eTransmitSimpleTimer = "Zeze.TimerAccount.TransmitSimpleTimer";
	public static final String eTransmitCancelAccountTimer = "Zeze.TimerAccount.TransmitCancelAccountTimer";

	private final Online online;

	TimerAccount(Online online) {
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
		private final BArchOnlineTimer bTimer;

		ArchOnlineTimer(BArchOnlineTimer bTimer) {
			this.bTimer = bTimer;
		}

		@Override
		Bean getTimerObj() {
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
		BAccountClientId identity() {
			return new BAccountClientId(bTimer.getAccount(), bTimer.getClientId());
		}
	}

	@Override
	Timer timer() {
		return Objects.requireNonNull(online.providerApp.zeze.getTimer());
	}

	@Override
	String name() {
		return "TimerAccount";
	}

	@Override
	@Nullable Long getLocalLoginVersion(BAccountClientId id) {
		return online.getLocalLoginVersion(id.getAccount(), id.getClientId());
	}

	@Override
	@Nullable Long getSharedLoginVersion(BAccountClientId id) {
		return online.getLoginVersion(id.getAccount(), id.getClientId());
	}

	@Override
	@Nullable Long getLoginVersion(BAccountClientId id) {
		return online.getLoginVersion(id.getAccount(), id.getClientId());
	}

	@Override
	OnlineTimer<BAccountClientId> newOnlineTimer(BAccountClientId id, long loginVersion,
														  long serialId, Bean timerObj) {
		var onlineTimer = new BArchOnlineTimer(id.getAccount(), id.getClientId(), loginVersion, serialId);
		onlineTimer.getTimerObj().setBean(timerObj);
		return new ArchOnlineTimer(onlineTimer);
	}

	@Override
	@Nullable OnlineTimer<BAccountClientId> getOnlineTimer(String timerId) {
		var bTimer = timer().tAccountTimers().get(timerId);
		return bTimer != null ? new ArchOnlineTimer(bTimer) : null;
	}

	@Override
	void insertOnlineTimer(String timerId, OnlineTimer<BAccountClientId> onlineTimer) {
		timer().tAccountTimers().insert(timerId, ((ArchOnlineTimer)onlineTimer).bTimer);
	}

	@Override
	void removeOnlineTimer(String timerId) {
		timer().tAccountTimers().remove(timerId);
	}

	@Override
	BOnlineTimers getOrAddLocalTimers(BAccountClientId id) {
		return online.getOrAddLocalBean(id.getAccount(), id.getClientId(), eOnlineTimers, new BOnlineTimers());
	}

	@Override
	@Nullable BOnlineTimers getLocalTimers(BAccountClientId id) {
		return online.getLocalBean(id.getAccount(), id.getClientId(), eOnlineTimers);
	}

	@Override
	void removeLocalTimers(BAccountClientId id) {
		online.removeLocalBean(id.getAccount(), id.getClientId(), eOnlineTimers);
	}

	@Override
	void transmitSimple(BAccountClientId target, BTransmitSimpleTimer p) {
		online.transmit(target.getAccount(), target.getClientId(), eTransmitSimpleTimer,
				List.of(new BLoginKey(target.getAccount(), target.getClientId())), p);
	}

	@Override
	void transmitCron(BAccountClientId target, BTransmitCronTimer p) {
		online.transmit(target.getAccount(), target.getClientId(), eTransmitCronTimer,
				List.of(new BLoginKey(target.getAccount(), target.getClientId())), p);
	}

	@Override
	void transmitCancel(BAccountClientId target, String timerId, long loginVersion) {
		var p = new BTransmitCancelAccountTimer();
		p.setTimerId(timerId);
		p.setAccount(target.getAccount());
		p.setClientId(target.getClientId());
		p.setLoginVersion(loginVersion);
		online.transmit(target.getAccount(), target.getClientId(), eTransmitCancelAccountTimer,
				List.of(new BLoginKey(target.getAccount(), target.getClientId())), p);
	}

	@Override
	String identityString(BAccountClientId id) {
		return "account=" + id.getAccount() + " clientId=" + id.getClientId();
	}

	@Override
	void fillContext(BAccountClientId id, TimerContext context) {
		context.account = id.getAccount();
		context.clientId = id.getClientId();
	}

	// ///////////////////////////////////////////////////////////////
	// Online Named Timer
	// 本进程内的有名字定时器，名字仅在本进程内唯一。

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(String account, String clientId, String timerId,
									   long delay, long period, long times, long endTime,
									   Class<? extends TimerHandle> handle, @Nullable Bean customData) {
		return scheduleOnlineNamed(account, clientId, timerId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(String account, String clientId, String timerId,
									   long delay, long period, long times, long endTime,
									   Class<? extends TimerHandle> handle, @Nullable Bean customData,
									   String oneByOneKey) {
		var zeze = online.providerApp.zeze;
		if (zeze.isHotVerifyEnabled())
			zeze.verifyCallerCold(Application.CALLER_WALKER.getCallerClass());
		return scheduleOnlineNamed(account, clientId, timerId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime)
						.oneByOneKey(oneByOneKey), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(String account, String clientId, String timerId,
										  long delay, long period, long times, long endTime,
										  Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		return scheduleOnlineNamedHot(account, clientId, timerId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime), handleClass,
				customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(String account, String clientId, String timerId,
										  long delay, long period, long times, long endTime,
										  Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										  String oneByOneKey) {
		return scheduleOnlineNamedHot(account, clientId, timerId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime)
						.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(String account, String clientId, String timerId,
									   String cron, long times, long endTime,
									   Class<? extends TimerHandle> handle,
									   @Nullable Bean customData) {
		return scheduleOnlineNamed(account, clientId, timerId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamed(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamed(String account, String clientId, String timerId,
									   String cron, long times, long endTime,
									   Class<? extends TimerHandle> handle, @Nullable Bean customData,
									   String oneByOneKey) {
		var zeze = online.providerApp.zeze;
		if (zeze.isHotVerifyEnabled())
			zeze.verifyCallerCold(Application.CALLER_WALKER.getCallerClass());
		return scheduleOnlineNamed(account, clientId, timerId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime).oneByOneKey(oneByOneKey), handle,
				customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(String account, String clientId, String timerId,
										  String cron, long times, long endTime,
										  Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		return scheduleOnlineNamedHot(account, clientId, timerId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineNamedHot(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOnlineNamedHot(String account, String clientId, String timerId,
										  String cron, long times, long endTime,
										  Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										  String oneByOneKey) {
		return scheduleOnlineNamedHot(account, clientId, timerId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime).oneByOneKey(oneByOneKey), handleClass,
				customData);
	}

	// ///////////////////////////////////////////////////////////////
	// Online Timer

	/**
	 * @deprecated 使用 {@link #scheduleOnline(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOnline(String account, String clientId, long delay, long period,
										  long times, long endTime, Class<? extends TimerHandle> handle,
										  @Nullable Bean customData) {
		return scheduleOnline(account, clientId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnline(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOnline(String account, String clientId, long delay, long period,
										  long times, long endTime, Class<? extends TimerHandle> handle,
										  @Nullable Bean customData, String oneByOneKey) {
		var zeze = online.providerApp.zeze;
		if (zeze.isHotVerifyEnabled())
			zeze.verifyCallerCold(Application.CALLER_WALKER.getCallerClass());
		return scheduleOnline(account, clientId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime)
						.oneByOneKey(oneByOneKey), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOnlineHot(String account, String clientId,
											 long delay, long period, long times, long endTime,
											 Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) {
		return scheduleOnlineHot(account, clientId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime), handleClass,
				customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOnlineHot(String account, String clientId,
											 long delay, long period, long times, long endTime,
											 Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData, String oneByOneKey) {
		return scheduleOnlineHot(account, clientId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime)
						.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnline(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOnline(String account, String clientId, String cron,
										  long times, long endTime, Class<? extends TimerHandle> handle,
										  @Nullable Bean customData) {
		return scheduleOnline(account, clientId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime), handle, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnline(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOnline(String account, String clientId, String cron,
										  long times, long endTime, Class<? extends TimerHandle> handle,
										  @Nullable Bean customData,
										  String oneByOneKey) {
		var zeze = online.providerApp.zeze;
		if (zeze.isHotVerifyEnabled())
			zeze.verifyCallerCold(Application.CALLER_WALKER.getCallerClass());
		return scheduleOnline(account, clientId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime).oneByOneKey(oneByOneKey), handle,
				customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOnlineHot(String account, String clientId, String cron,
											 long times, long endTime,
											 Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) {
		return scheduleOnlineHot(account, clientId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOnlineHot(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOnlineHot(String account, String clientId, String cron,
											 long times, long endTime,
											 Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData,
											 String oneByOneKey) {
		return scheduleOnlineHot(account, clientId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime).oneByOneKey(oneByOneKey), handleClass,
				customData);
	}

	// ///////////////////////////////////////////////////////////////
	// Spec 入口
	// 推荐使用TimerSpec描述调度参数，避免超长的参数列表。

	public String scheduleOnline(String account, String clientId,
										  TimerSpec spec,
										  Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		var zeze = online.providerApp.zeze;
		if (zeze.isHotVerifyEnabled())
			zeze.verifyCallerCold(Application.CALLER_WALKER.getCallerClass());
		return scheduleOnlineImpl(account, clientId, spec, handleClass, customData);
	}

	String scheduleOnlineImpl(String account, String clientId,
									   TimerSpec spec,
									   Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) {
		var timerId = newAutoTimerId();
		var id = new BAccountClientId(account, clientId);
		switch (spec) {
		case SimpleTimerSpec s -> scheduleOnline(false, id, timerId, s.build(), handleClass, customData, false);
		case CronTimerSpec c -> scheduleOnline(false, id, timerId, c.build(), handleClass, customData, false);
		}
		return timerId;
	}

	public String scheduleOnlineHot(String account, String clientId,
											 TimerSpec spec,
											 Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) {
		var timerId = newAutoTimerId();
		var id = new BAccountClientId(account, clientId);
		switch (spec) {
		case SimpleTimerSpec s -> scheduleOnline(true, id, timerId, s.build(), handleClass, customData, false);
		case CronTimerSpec c -> scheduleOnline(true, id, timerId, c.build(), handleClass, customData, false);
		}
		return timerId;
	}

	public boolean scheduleOnlineNamed(String account, String clientId,
									   String timerId, TimerSpec spec,
									   Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) {
		var zeze = online.providerApp.zeze;
		if (zeze.isHotVerifyEnabled())
			zeze.verifyCallerCold(Application.CALLER_WALKER.getCallerClass());
		return scheduleOnlineNamedImpl(account, clientId, timerId, spec, handleClass, customData);
	}

	boolean scheduleOnlineNamedImpl(String account, String clientId,
									String timerId, TimerSpec spec,
									Class<? extends TimerHandle> handleClass,
									@Nullable Bean customData) {
		if (isNamedTimerIdOccupied(timerId))
			return false;
		var id = new BAccountClientId(account, clientId);
		switch (spec) {
		case SimpleTimerSpec s -> scheduleOnline(false, id, timerId, s.build(), handleClass, customData, false);
		case CronTimerSpec c -> scheduleOnline(false, id, timerId, c.build(), handleClass, customData, false);
		}
		return true;
	}

	public boolean scheduleOnlineNamedHot(String account, String clientId,
										  String timerId, TimerSpec spec,
										  Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		if (isNamedTimerIdOccupied(timerId))
			return false;
		var id = new BAccountClientId(account, clientId);
		switch (spec) {
		case SimpleTimerSpec s -> scheduleOnline(true, id, timerId, s.build(), handleClass, customData, false);
		case CronTimerSpec c -> scheduleOnline(true, id, timerId, c.build(), handleClass, customData, false);
		}
		return true;
	}

	// ///////////////////////////////////////////////////////////////
	// 取消
	public boolean cancel(@Nullable String timerId, String account, String clientId) {
		if (timerId == null)
			return true;
		return cancelOnline(timerId, account, clientId) || cancelOffline(timerId, account, clientId);
	}

	public boolean cancelOnline(@Nullable String timerId, String account, String clientId) {
		return cancelOnline(timerId, new BAccountClientId(account, clientId), false);
	}

	public boolean cancelOffline(@Nullable String timerId, String account, String clientId) {
		if (timerId == null)
			return true;
		var timer = online.providerApp.zeze.getTimer();
		// 归属校验（对齐TimerRole.cancelOffline）：无校验直接timer.cancel时，
		// 传入任意timerId（SafeBatch看门狗、他人offline timer、
		// 全局命名timer）都会被越权取消，且恒返回true掩盖"是否真的取消"。
		//noinspection DataFlowIssue
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
		if (!(customData instanceof BOfflineAccountCustom custom))
			return false; // 不是本账号的offline timer，归属不符拒绝
		if (!custom.getAccount().equals(account) || !custom.getClientId().equals(clientId))
			return false;
		timer.cancel(timerId);
		var key = new BAccountClientId(account, clientId);
		var bTimers = timer.tAccountOfflineTimers().get(key);
		if (bTimers != null) {
			bTimers.getOfflineTimers().remove(timerId);
			if (bTimers.getOfflineTimers().isEmpty())
				timer.tAccountOfflineTimers().remove(key);
		}
		return true;
	}

	// ///////////////////////////////////////////////////////////////
	// Offline Timer

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(String timerId, String account, String clientId,
										long delay, long period, long times, long endTime, int missfirePolicy,
										Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return scheduleOfflineNamed(timerId, account, clientId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime)
						.missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(String timerId, String account, String clientId,
										long delay, long period, long times, long endTime, int missfirePolicy,
										Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										String oneByOneKey) {
		return scheduleOfflineNamed(timerId, account, clientId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime)
						.missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	private void scheduleOffline(String timerId, String account, String clientId,
								 SimpleTimerSpec builder,
								 Class<? extends TimerHandle> handleClass,
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
			//noinspection DataFlowIssue
			timer.tryRecordBeanHotModuleWhileCommit(customData);
		}
		Objects.requireNonNull(timer).schedule(timerId, builder.build(), OfflineHandle.class, custom);
		var config = timer.getZeze().getConfig();
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
	 * @deprecated 使用 {@link #scheduleOffline(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOffline(String account, String clientId,
										   long delay, long period, long times, long endTime, int missfirePolicy,
										   Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) {
		return scheduleOffline(account, clientId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime)
						.missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOffline(String account, String clientId,
										   long delay, long period, long times, long endTime, int missfirePolicy,
										   Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										   String oneByOneKey) {
		return scheduleOffline(account, clientId,
				TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime)
						.missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(String timerId, String account, String clientId,
										String cron, long times, long endTime, int missfirePolicy,
										Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
		return scheduleOfflineNamed(timerId, account, clientId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime).missfirePolicy(missfirePolicy),
				handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOfflineNamed(String, String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public boolean scheduleOfflineNamed(String timerId, String account, String clientId,
										String cron, long times, long endTime, int missfirePolicy,
										Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										String oneByOneKey) {
		return scheduleOfflineNamed(timerId, account, clientId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime).missfirePolicy(missfirePolicy)
						.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	private void scheduleOffline(String timerId, String account, String clientId,
								 CronTimerSpec builder,
								 Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
								 @Nullable BIndex index) {
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
			//noinspection DataFlowIssue
			timer.tryRecordBeanHotModuleWhileCommit(customData);
		}
		if (index != null) {
			//noinspection DataFlowIssue
			if (timer.cronEquals(index, timerId, builder, OfflineHandle.class, custom))
				return;
			cancel(timerId, account, clientId); // 先取消,下面再重建
		}
		Objects.requireNonNull(timer).schedule(timerId, builder.build(), OfflineHandle.class, custom);
		var config = timer.getZeze().getConfig();
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
	 * @deprecated 使用 {@link #scheduleOffline(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOffline(String account, String clientId, String cron,
										   long times, long endTime, int missfirePolicy,
										   Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) {
		return scheduleOffline(account, clientId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime).missfirePolicy(missfirePolicy),
				handleClass, customData);
	}

	/**
	 * @deprecated 使用 {@link #scheduleOffline(String, String, TimerSpec, Class, Bean)} 替代
	 */
	@Deprecated
	public String scheduleOffline(String account, String clientId, String cron,
										   long times, long endTime, int missfirePolicy,
										   Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										   String oneByOneKey) {
		return scheduleOffline(account, clientId,
				TimerSpec.ofCron(cron).times(times).endTime(endTime).missfirePolicy(missfirePolicy)
						.oneByOneKey(oneByOneKey), handleClass, customData);
	}

	// ///////////////////////////////////////////////////////////////
	// Offline Spec 入口

	public String scheduleOffline(String account, String clientId,
										   TimerSpec spec,
										   Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) {
		var timerId = newAutoTimerId();
		switch (spec) {
		case SimpleTimerSpec s -> scheduleOffline(timerId, account, clientId, s, handleClass, customData);
		case CronTimerSpec c -> scheduleOffline(timerId, account, clientId, c, handleClass, customData, null);
		}
		return timerId;
	}

	// 查timerId当前登记的账号offline timer归属，查表路径仿cancelOffline：
	// 非本族timer返回null；本族返回customData，供调用方做族+归属判定。
	private static @Nullable BOfflineAccountCustom getAccountOfflineCustom(Timer timer, String timerId) {
		var index = timer.tIndexs().get(timerId);
		if (index == null)
			return null;
		var node = timer.tNodes().get(index.getNodeId());
		var bTimer = node != null ? node.getTimers().get(timerId) : null;
		if (bTimer == null)
			return null;
		return bTimer.getCustomData().getBean() instanceof BOfflineAccountCustom custom ? custom : null;
	}

	public boolean scheduleOfflineNamed(String timerId, String account, String clientId,
										TimerSpec spec,
										Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		var zeze = online.providerApp.zeze;
		var timer = zeze.getTimer();
		//noinspection DataFlowIssue
		if (timer.isOnlineTimerIdOccupied(timerId))
			return false; // 同名被在线族定时器占用，不得并存（否则共用timerFutures相互覆盖）
		var index = timer.tIndexs().get(timerId);
		if (index != null && index.getServerId() != zeze.getConfig().getServerId())
			return false; // 已经被其它gs调度
		// 族+归属判定——撞本server的命名timer时，非本族（全局/在线timer）cancel
		// 恒false；本族异主（他账号遗留的同名offline timer）cancel因归属不符也恒false。
		// 两者都会让随后scheduleOffline的_tIndexs.insert撞已存在键抛IAE中断调用方整个事务。
		// 对齐签名契约直接返回false；仅本族同主放行，走下面cancel+重建。
		if (index != null) {
			var custom = getAccountOfflineCustom(timer, timerId);
			if (custom == null || !custom.getAccount().equals(account) || !custom.getClientId().equals(clientId))
				return false;
		}
		switch (spec) {
		case SimpleTimerSpec s -> {
			if (index != null)
				cancel(timerId, account, clientId); // 先取消,下面再重建
			scheduleOffline(timerId, account, clientId, s, handleClass, customData);
		}
		case CronTimerSpec c -> scheduleOffline(timerId, account, clientId, c, handleClass, customData, index);
		}
		return true;
	}

	/// ///////////////////////////////////////////////////////////////////////////////////////
	// 内部实现
	public static class OfflineHandle implements TimerHandle {
		// timer终止的每条路径（打完/回调异常/显式cancel）都经Timer.cancel的
		// onTimerCancel钩子同步清簿记，与onLoginEvent一致，杜绝"index已删簿记残留"
		// 使同名重调度putIfAbsent撞残留抛IllegalStateException。
		@Override
		public void onTimerCancel(BTimer bTimer) {
			if (!(bTimer.getCustomData().getBean() instanceof BOfflineAccountCustom custom))
				return;
			try {
				var zeze = Timer.currentProcedureApp();
				if (zeze == null)
					return;
				var timer = zeze.getTimer();
				var key = new BAccountClientId(custom.getAccount(), custom.getClientId());
				//noinspection DataFlowIssue
				var offlineTimers = timer.tAccountOfflineTimers().get(key);
				if (offlineTimers != null) {
					offlineTimers.getOfflineTimers().remove(custom.getTimerName());
					if (offlineTimers.getOfflineTimers().isEmpty())
						timer.tAccountOfflineTimers().remove(key);
					logger.debug("OfflineHandle.onTimerCancel: timerId={}, account={}, clientId={}",
							custom.getTimerName(), custom.getAccount(), custom.getClientId());
				}
			} catch (Exception e) {
				logger.error("OfflineHandle.onTimerCancel: timerId={}, account={}, clientId={}",
						custom.getTimerName(), custom.getAccount(), custom.getClientId(), e);
			}
		}

		@Override
		public void onTimer(TimerContext context) throws Exception {
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
				var key = new BAccountClientId(account, clientId);
				var offlineTimers = context.timer.tAccountOfflineTimers().get(key);
				if (offlineTimers != null) {
					offlineTimers.getOfflineTimers().remove(timerId);
					if (offlineTimers.getOfflineTimers().isEmpty())
						context.timer.tAccountOfflineTimers().remove(key);
				}
			}
		}
	}

	private long onLoginEvent(Object sender, EventDispatcher.EventArgument arg) {
		var loginArg = (LoginArgument)arg;
		var loginKey = new BAccountClientId(loginArg.account, loginArg.clientId);
		var timer = online.providerApp.zeze.getTimer();
		//noinspection DataFlowIssue
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
	private long onLocalRemoveEvent(Object sender, EventDispatcher.EventArgument arg) {
		var bAny = ((LocalRemoveEventArgument)arg).local.getDatas().get(eOnlineTimers);
		if (bAny != null)
			onLocalRemove((BOnlineTimers)bAny.getAny().getBean());
		return 0;
	}

	private long transmitOnlineCronTimer(String senderAccount, String senderClientId,
										 String targetAccount, String targetClientId,
										 @Nullable Binary parameter) throws ReflectiveOperationException {
		return onTransmitCronTimer(new BAccountClientId(targetAccount, targetClientId), parameter);
	}

	private long transmitOnlineSimpleTimer(String senderAccount, String senderClientId,
										   String targetAccount, String targetClientId,
										   @Nullable Binary parameter) throws ReflectiveOperationException {
		return onTransmitSimpleTimer(new BAccountClientId(targetAccount, targetClientId), parameter);
	}

	private long transmitCancelAccountTimer(String senderAccount, String senderClientId,
											String targetAccount, String targetClientId,
											@Nullable Binary parameter) {
		if (parameter == null)
			return 0;
		var p = new BTransmitCancelAccountTimer();
		p.decode(ByteBuffer.Wrap(parameter));
		onTransmitCancel(p.getTimerId(), new BAccountClientId(p.getAccount(), p.getClientId()), p.getLoginVersion());
		return 0;
	}
}
