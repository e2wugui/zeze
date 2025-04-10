package Zeze.Component;

import java.text.ParseException;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BOfflineRoleCustom;
import Zeze.Builtin.Timer.BTransmitCronTimer;
import Zeze.Builtin.Timer.BTransmitSimpleTimer;
import Zeze.Game.LocalRemoveEventArgument;
import Zeze.Game.LoginArgument;
import Zeze.Game.Online;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BGameOnlineTimer;
import Zeze.Builtin.Timer.BOnlineTimers;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Game.ProviderWithOnline;
import Zeze.Hot.HotHandle;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * 1. schedule，scheduleNamed 完全重新实现一套基于内存表和内存的。
 * 2. 不直接使用 Timer.schedule。但有如下关联。
 * 直接使用 Timer.timerIdAutoKey，使得返回的timerId共享一个名字空间。
 * 直接使用 Timer.timersFuture，从 ThreadPool 返回的future保存在这里。
 */
public class TimerRole {
	private static final @NotNull Logger logger = LogManager.getLogger(TimerRole.class);
	public static final String eOnlineTimers = "Zeze.Component.TimerGameOnline";
	// public static final String eTimerHandleName = "Zeze.Component.TimerGameOnline.Handle";
	public static final String eTransmitCronTimer = "Zeze.TimerRole.TransmitCronTimer";
	public static final String eTransmitSimpleTimer = "Zeze.TimerRole.TransmitSimpleTimer";

	private final @NotNull Online online;

	public TimerRole(@NotNull Online online) {
		this.online = online;

		online.getTransmitActions().put(eTransmitCronTimer, this::transmitOnlineCronTimer);
		online.getTransmitActions().put(eTransmitSimpleTimer, this::transmitOnlineSimpleTimer);
		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().add(EventDispatcher.Mode.RunEmbed, this::onLocalRemoveEvent);
		online.getLoginEvents().add(EventDispatcher.Mode.RunEmbed, this::onLoginEvent);
		online.getReloginEvents().add(EventDispatcher.Mode.RunEmbed, this::onLoginEvent);
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, long delay, long period, long times,
									   long endTime, @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) {
		return scheduleOnlineNamed(roleId, timerId, delay, period, times, endTime, handleClass, customData, "");
	}

	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, long delay, long period, long times,
									   long endTime, @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData, @NotNull String oneByOneKey) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		if (online._tRoleTimers().get(timerId) != null)
			return false;

		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		scheduleOnline(roleId, timerId, simpleTimer, handleClass, customData, false);
		return true;
	}

	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, long delay, long period, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		return scheduleOnlineNamedHot(roleId, timerId, delay, period, times, endTime, handleClass, customData, "");
	}

	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, long delay, long period, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData, @NotNull String oneByOneKey) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		if (online._tRoleTimers().get(timerId) != null)
			return false;

		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		scheduleOnlineHot(roleId, timerId, simpleTimer, handleClass, customData, false);
		return true;
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, @NotNull String cron, long times,
									   long endTime, @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) throws ParseException {
		return scheduleOnlineNamed(roleId, timerId, cron, times, endTime, handleClass, customData, "");
	}

	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, @NotNull String cron, long times,
									   long endTime, @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData, @NotNull String oneByOneKey) throws ParseException {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		if (online._tRoleTimers().get(timerId) != null)
			return false;

		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		scheduleOnline(roleId, timerId, cronTimer, handleClass, customData, false);
		return true;
	}

	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, @NotNull String cron, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) throws ParseException {
		return scheduleOnlineNamedHot(roleId, timerId, cron, times, endTime, handleClass, customData, "");
	}

	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, @NotNull String cron, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData,
										  @NotNull String oneByOneKey) throws ParseException {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		if (online._tRoleTimers().get(timerId) != null)
			return false;

		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		scheduleOnlineHot(roleId, timerId, cronTimer, handleClass, customData, false);
		return true;
	}

	public @NotNull String scheduleOnline(long roleId, long delay, long period, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) {
		return scheduleOnline(roleId, delay, period, times, endTime, handleClass, customData, "");
	}

	public @NotNull String scheduleOnline(long roleId, long delay, long period, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										  @NotNull String oneByOneKey) {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		var timerId = '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString();
		scheduleOnline(roleId, timerId, simpleTimer, handleClass, customData, false);
		return timerId;
	}

	public @NotNull String scheduleOnlineHot(long roleId, long delay, long period, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) {
		return scheduleOnlineHot(roleId, delay, period, times, endTime, handleClass, customData, "");
	}

	public @NotNull String scheduleOnlineHot(long roleId, long delay, long period, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData, @NotNull String oneByOneKey) {
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		var timerId = '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString();
		scheduleOnlineHot(roleId, timerId, simpleTimer, handleClass, customData, false);
		return timerId;
	}

	private void scheduleOnline(long roleId, @NotNull String timerId, @NotNull BSimpleTimer simpleTimer,
								@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
								boolean fromTransmit) {
		Reflect.checkDefaultConstructor(handleClass);
		var loginVersion = online.getLocalLoginVersion(roleId);
		var loginOnlineShared = online.getLoginOnlineShared(roleId);
		if (loginVersion == null || loginOnlineShared == null || loginVersion != loginOnlineShared.getLoginVersion()) {
			if (fromTransmit) {
				logger.warn("schedule simple from transmit, but not login. roleId={}, handle={}",
						roleId, handleClass.getName());
				return;
			}
			if (loginOnlineShared != null) {
				var p = new BTransmitSimpleTimer();
				p.setTimerId(timerId);
				p.setHandleClass(handleClass.getName());
				p.setSimpleTimer(simpleTimer);
				p.setLoginVersion(loginOnlineShared.getLoginVersion());
				p.setHot(false);
				if (customData != null) {
					p.setCustomClass(customData.getClass().getName());
					p.setCustomBean(new Binary(ByteBuffer.encode(customData)));
				}
				online.transmitEmbed(roleId, eTransmitSimpleTimer, List.of(roleId),
						new Binary(ByteBuffer.encode(p)), false);

				logger.info("scheduleOnline(Simple): not online but transmit {}", roleId);
				return; // 登录在其他机器上，转发过去注册OnlineTimer，不管结果了。
			}
			throw new IllegalStateException("not online " + roleId);
		}

		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion,
				online.providerApp.zeze.getTimer().timerSerialId.nextId());
		onlineTimer.getTimerObj().setBean(simpleTimer);
		online._tRoleTimers().put(timerId, onlineTimer);
		logger.debug("add online simple timer: timerId={}, roleId={}, handle={}",
				timerId, roleId, handleClass.getName());

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (customData != null) {
			Timer.register(customData);
			timerLocal.getCustomData().setBean(customData);
		}
		var handle = online.providerApp.zeze.getTimer().findTimerHandle(handleClass.getName());
		scheduleOnlineSimple(timerId, simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), handle);
	}

	private void scheduleOnlineHot(long roleId, @NotNull String timerId, @NotNull BSimpleTimer simpleTimer,
								   @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
								   boolean fromTransmit) {
		// 现在不允许在非登录状态注册timer
		Reflect.checkDefaultConstructor(handleClass);
		var loginVersion = online.getLocalLoginVersion(roleId);
		var loginOnlineShared = online.getLoginOnlineShared(roleId);
		if (loginVersion == null || loginOnlineShared == null || loginVersion != loginOnlineShared.getLoginVersion()) {
			if (fromTransmit) {
				logger.warn("schedule hot simple from transmit, but not login. roleId={}, handle={}",
						roleId, handleClass.getName());
				return;
			}
			if (loginOnlineShared != null) {
				var p = new BTransmitSimpleTimer();
				p.setTimerId(timerId);
				p.setHandleClass(handleClass.getName());
				p.setSimpleTimer(simpleTimer);
				p.setLoginVersion(loginOnlineShared.getLoginVersion());
				p.setHot(true);
				if (customData != null) {
					p.setCustomClass(customData.getClass().getName());
					p.setCustomBean(new Binary(ByteBuffer.encode(customData)));
				}
				online.transmitEmbed(roleId, eTransmitSimpleTimer, List.of(roleId), new Binary(ByteBuffer.encode(p)),
						false);
			}
			logger.info("scheduleOnlineHot(Simple): not online {}", roleId);
		} else {
			var onlineTimer = new BGameOnlineTimer(roleId, loginVersion,
					online.providerApp.zeze.getTimer().timerSerialId.nextId());
			onlineTimer.getTimerObj().setBean(simpleTimer);
			online._tRoleTimers().put(timerId, onlineTimer);
			logger.debug("add online hot simple timer: timerId={}, roleId={}, handle={}",
					timerId, roleId, handleClass.getName());

			var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
			var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
			if (customData != null) {
				Timer.register(customData);
				timerLocal.getCustomData().setBean(customData);
			}
			scheduleOnlineSimpleHot(timerId, simpleTimer.getNextExpectedTime() - System.currentTimeMillis(),
					handleClass);
		}
	}

	public @NotNull String scheduleOnline(long roleId, @NotNull String cron, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass,
										  @Nullable Bean customData) throws ParseException {
		return scheduleOnline(roleId, cron, times, endTime, handleClass, customData, "");
	}

	public @NotNull String scheduleOnline(long roleId, @NotNull String cron, long times, long endTime,
										  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										  @NotNull String oneByOneKey) throws ParseException {
		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		var timerId = '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString();
		scheduleOnline(roleId, timerId, cronTimer, handleClass, customData, false);
		return timerId;
	}

	public @NotNull String scheduleOnlineHot(long roleId, @NotNull String cron, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData) throws ParseException {
		return scheduleOnlineHot(roleId, cron, times, endTime, handleClass, customData, "");
	}

	public @NotNull String scheduleOnlineHot(long roleId, @NotNull String cron, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> handleClass,
											 @Nullable Bean customData,
											 @NotNull String oneByOneKey) throws ParseException {
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		var timerId = '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString();
		scheduleOnlineHot(roleId, timerId, cronTimer, handleClass, customData, false);
		return timerId;
	}

	private long transmitOnlineCronTimer(long sender, long target, @Nullable Binary parameter)
			throws Exception {
		if (parameter == null)
			return 0;

		var p = new BTransmitCronTimer();
		p.decode(ByteBuffer.Wrap(parameter));

		var loginOnlineShared = online.getLoginOnlineShared(target);
		if (loginOnlineShared != null && p.getLoginVersion() == loginOnlineShared.getLoginVersion()) {
			Bean custom = null;
			if (!p.getCustomClass().isEmpty()) {
				var customClass = Class.forName(p.getCustomClass());
				custom = (Bean)customClass.getDeclaredConstructor().newInstance();
				custom.decode(ByteBuffer.Wrap(p.getCustomBean()));
			}
			if (p.isHot()) {
				@SuppressWarnings("unchecked")
				var handleClass = (Class<TimerHandle>)HotHandle.findClass(online.providerApp.zeze, p.getHandleClass());
				scheduleOnlineHot(sender, p.getTimerId(), p.getCronTimer(), handleClass, custom, true);
			} else {
				@SuppressWarnings("unchecked")
				var handleClass = (Class<TimerHandle>)HotHandle.findClass(online.providerApp.zeze, p.getHandleClass());
				scheduleOnline(sender, p.getTimerId(), p.getCronTimer(), handleClass, custom, true);
			}
		}
		return 0;
	}

	private long transmitOnlineSimpleTimer(long sender, long target, @Nullable Binary parameter)
			throws Exception {
		if (parameter == null)
			return 0;

		var p = new BTransmitSimpleTimer();
		p.decode(ByteBuffer.Wrap(parameter));

		var loginOnlineShared = online.getLoginOnlineShared(target);
		if (loginOnlineShared != null && p.getLoginVersion() == loginOnlineShared.getLoginVersion()) {
			Bean custom = null;
			if (!p.getCustomClass().isEmpty()) {
				var customClass = Class.forName(p.getCustomClass());
				custom = (Bean)customClass.getDeclaredConstructor().newInstance();
				custom.decode(ByteBuffer.Wrap(p.getCustomBean()));
			}
			if (p.isHot()) {
				@SuppressWarnings("unchecked")
				var handleClass = (Class<TimerHandle>)HotHandle.findClass(online.providerApp.zeze, p.getHandleClass());
				scheduleOnlineHot(sender, p.getTimerId(), p.getSimpleTimer(), handleClass, custom, true);
			} else {
				@SuppressWarnings("unchecked")
				var handleClass = (Class<TimerHandle>)HotHandle.findClass(online.providerApp.zeze, p.getHandleClass());
				scheduleOnline(sender, p.getTimerId(), p.getSimpleTimer(), handleClass, custom, true);
			}
		}
		return 0;
	}

	private void scheduleOnline(long roleId, @NotNull String timerId, @NotNull BCronTimer cronTimer,
								@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
								boolean fromTransmit) {
		Reflect.checkDefaultConstructor(handleClass);
		var loginVersion = online.getLocalLoginVersion(roleId);
		var loginOnlineShared = online.getLoginOnlineShared(roleId);
		if (loginVersion == null || loginOnlineShared == null || loginVersion != loginOnlineShared.getLoginVersion()) {
			if (fromTransmit) {
				logger.warn("schedule cron from transmit, but not login. roleId={}, handle={}",
						roleId, handleClass.getName());
				return;
			}
			if (loginOnlineShared != null) {
				var p = new BTransmitCronTimer();
				p.setTimerId(timerId);
				p.setCronTimer(cronTimer);
				p.setHandleClass(handleClass.getName());
				p.setLoginVersion(loginOnlineShared.getLoginVersion());
				p.setHot(false);
				if (customData != null) {
					p.setCustomClass(customData.getClass().getName());
					p.setCustomBean(new Binary(ByteBuffer.encode(customData)));
				}
				online.transmitEmbed(roleId, eTransmitCronTimer, List.of(roleId), new Binary(ByteBuffer.encode(p)),
						false);
				logger.info("scheduleOnline(Cron): not online but transmit {}", roleId);
				return; // 登录在其他机器上，转发过去注册OnlineTimer，不管结果了。
			}
			throw new IllegalStateException("not online " + roleId);
		}

		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion,
				online.providerApp.zeze.getTimer().timerSerialId.nextId());
		onlineTimer.getTimerObj().setBean(cronTimer);
		online._tRoleTimers().insert(timerId, onlineTimer);
		logger.debug("add online cron timer: timerId={}, roleId={}, handle={}",
				timerId, roleId, handleClass.getName());

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (customData != null) {
			Timer.register(customData);
			timerLocal.getCustomData().setBean(customData);
		}
		var handle = online.providerApp.zeze.getTimer().findTimerHandle(handleClass.getName());
		scheduleOnlineCron(timerId, cronTimer, handle);
	}

	private void scheduleOnlineHot(long roleId, @NotNull String timerId, @NotNull BCronTimer cronTimer,
								   @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
								   boolean fromTransmit) {
		Reflect.checkDefaultConstructor(handleClass);
		var loginVersion = online.getLocalLoginVersion(roleId);
		var loginOnlineShared = online.getLoginOnlineShared(roleId);
		if (loginVersion == null || loginOnlineShared == null || loginVersion != loginOnlineShared.getLoginVersion()) {
			if (fromTransmit) {
				logger.warn("schedule hot cron from transmit, but not login. roleId={}, handle={}",
						roleId, handleClass.getName());
				return;
			}
			if (loginOnlineShared != null) {
				var p = new BTransmitCronTimer();
				p.setTimerId(timerId);
				p.setCronTimer(cronTimer);
				p.setHandleClass(handleClass.getName());
				p.setLoginVersion(loginOnlineShared.getLoginVersion());
				p.setHot(true);
				if (customData != null) {
					p.setCustomClass(customData.getClass().getName());
					p.setCustomBean(new Binary(ByteBuffer.encode(customData)));
				}
				online.transmitEmbed(roleId, eTransmitCronTimer, List.of(roleId), new Binary(ByteBuffer.encode(p)),
						false);
			}
			logger.info("scheduleOnlineHot(Cron): not online {}", roleId);
			return;
		}

		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion,
				online.providerApp.zeze.getTimer().timerSerialId.nextId());
		onlineTimer.getTimerObj().setBean(cronTimer);
		online._tRoleTimers().insert(timerId, onlineTimer);
		logger.debug("add online hot cron timer: timerId={}, roleId={}, handle={}",
				timerId, roleId, handleClass.getName());

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (customData != null) {
			Timer.register(customData);
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleOnlineCronHot(timerId, cronTimer, handleClass);
	}

	public boolean cancel(@Nullable String timerId) {
		return cancelOnline(timerId) || cancelOffline(timerId);
	}

	public boolean cancelOnline(@Nullable String timerId) {
		if (timerId == null)
			return true; // 取消不存在的timer，认为成功。

		// always cancel future task，第一步就做这个。
		Transaction.whileCommit(() -> online.providerApp.zeze.getTimer().cancelFuture(timerId));

		// remove online timer
		var bTimer = online._tRoleTimers().get(timerId); // table.remove现在不能返回旧值，只能这样写。
		if (bTimer == null)
			return false;

		// remove online local
		var roleId = bTimer.getRoleId();
		var onlineTimers = (BOnlineTimers)online.getLocalBean(roleId, eOnlineTimers);
		if (onlineTimers != null) {
			onlineTimers.getTimerIds().remove(timerId);
			if (onlineTimers.getTimerIds().isEmpty())
				online.removeLocalBean(roleId, eOnlineTimers);
		}
		// always remove from table
		online._tRoleTimers().remove(timerId);
		logger.debug("cancel online timer: timerId={}, roleId={}", timerId, roleId);
		return true;
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

	public boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, long delay, long period,
										long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return scheduleOfflineNamed(timerId, roleId, delay, period, times, endTime, missfirePolicy, handleClass,
				customData, "");
	}

	public boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, long delay, long period,
										long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
										@NotNull String oneByOneKey) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		var zeze = online.providerApp.zeze;
		var index = zeze.getTimer().tIndexs().get(timerId);
		if (index != null) {
			if (index.getServerId() != zeze.getConfig().getServerId())
				return false; // 已经被其它gs调度
			cancel(timerId); // 先取消,下面再重建
		}

		scheduleOffline(timerId, roleId, delay, period, times, endTime, missfirePolicy, handleClass, customData,
				oneByOneKey);
		return true;
	}

	private void scheduleOffline(@NotNull String timerId, long roleId, long delay, long period, long times,
								 long endTime, int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData, @NotNull String oneByOneKey) {
		Reflect.checkDefaultConstructor(handleClass);
		var logoutVersion = online.getLogoutVersion(roleId);
		if (logoutVersion == null)
			throw new IllegalStateException("not logout. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineRoleCustom(timerId, roleId, logoutVersion, handleClass.getName(),
				online.getOnlineSetName());
		if (customData != null) {
			Timer.register(customData);
			custom.getCustomData().setBean(customData);
			timer.tryRecordBeanHotModuleWhileCommit(customData);
		}
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		simpleTimer.setMissfirePolicy(missfirePolicy);
		timer.schedule(timerId, simpleTimer, OfflineHandle.class, custom);
		var config = timer.zeze.getConfig();
		var offline = online._tRoleOfflineTimers().getOrAdd(roleId);
		if (offline.getOfflineTimers().size() > config.getOfflineTimerLimit()) {
			// throw new IllegalStateException("too many offline timers. roleId=" + roleId + " size=" + offline.getOfflineTimers().size());
			logger.error("scheduleOffline(simple): too many timers. roleId={}, handle={}, size={} > {}",
					roleId, handleClass.getName(), offline.getOfflineTimers().size(), config.getOfflineTimerLimit());
		}
		if (offline.getOfflineTimers().putIfAbsent(timerId, config.getServerId()) != null)
			throw new IllegalStateException("duplicate timerId. roleId=" + roleId);
		logger.debug("add offline simple timer: timerId={}, roleId={}, handle={}",
				timerId, roleId, handleClass.getName());
	}

	public @NotNull String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
										   int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) {
		return scheduleOffline(roleId, delay, period, times, endTime, missfirePolicy, handleClass, customData, "");
	}

	public @NotNull String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
										   int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData, @NotNull String oneByOneKey) {
		var timerId = '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString();
		scheduleOffline(timerId, roleId, delay, period, times, endTime, missfirePolicy, handleClass, customData,
				oneByOneKey);
		return timerId;
	}

	public @NotNull String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
										   @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) {
		return scheduleOffline(roleId, delay, period, times, endTime, Timer.eMissfirePolicyNothing, handleClass,
				customData);
	}

	public boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, @NotNull String cron,
										long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) throws ParseException {
		return scheduleOfflineNamed(timerId, roleId, cron, times, endTime, missfirePolicy, handleClass, customData, "");
	}

	public boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, @NotNull String cron,
										long times, long endTime, int missfirePolicy,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData,
										@NotNull String oneByOneKey) throws ParseException {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		var zeze = online.providerApp.zeze;
		var index = zeze.getTimer().tIndexs().get(timerId);
		if (index != null && index.getServerId() != zeze.getConfig().getServerId())
			return false; // 已经被其它gs调度

		scheduleOffline(timerId, roleId, cron, times, endTime, missfirePolicy, handleClass, customData, oneByOneKey,
				index);
		return true;
	}

	private void scheduleOffline(@NotNull String timerId, long roleId, @NotNull String cron, long times,
								 long endTime, int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData, @NotNull String oneByOneKey,
								 @Nullable BIndex index) throws ParseException {
		Reflect.checkDefaultConstructor(handleClass);
		var logoutVersion = online.getLogoutVersion(roleId);
		if (logoutVersion == null)
			throw new IllegalStateException("not logout. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineRoleCustom(timerId, roleId, logoutVersion, handleClass.getName(),
				online.getOnlineSetName());
		if (customData != null) {
			Timer.register(customData);
			custom.getCustomData().setBean(customData);
		}
		if (index != null) {
			if (timer.cronEquals(index, timerId, cron, times, endTime, missfirePolicy, OfflineHandle.class, custom,
					oneByOneKey))
				return;
			cancel(timerId); // 先取消,下面再重建
		}
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		cronTimer.setMissfirePolicy(missfirePolicy);
		timer.schedule(timerId, cronTimer, OfflineHandle.class, custom);
		var config = timer.zeze.getConfig();
		var offline = online._tRoleOfflineTimers().getOrAdd(roleId);
		if (offline.getOfflineTimers().size() > config.getOfflineTimerLimit()) {
			// throw new IllegalStateException("too many offline timers. roleId=" + roleId + " size=" + offline.getOfflineTimers().size());
			logger.error("scheduleOffline(cron): too many timers. roleId={}, handle={}, size={} > {}",
					roleId, handleClass.getName(), offline.getOfflineTimers().size(), config.getOfflineTimerLimit());
		}
		if (offline.getOfflineTimers().putIfAbsent(timerId, config.getServerId()) != null)
			throw new IllegalStateException("duplicate timerId. roleId=" + roleId);
		logger.debug("add offline cron timer: timerId={}, roleId={}, handle={}",
				timerId, roleId, handleClass.getName());
	}

	public @NotNull String scheduleOffline(long roleId, @NotNull String cron, long times, long endTime,
										   int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) throws ParseException {
		return scheduleOffline(roleId, cron, times, endTime, missfirePolicy, handleClass, customData, "");
	}

	public @NotNull String scheduleOffline(long roleId, @NotNull String cron, long times, long endTime,
										   int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData,
										   @NotNull String oneByOneKey) throws ParseException {
		var timerId = '@' + online.providerApp.zeze.getTimer().timerIdAutoKey.nextString();
		scheduleOffline(timerId, roleId, cron, times, endTime, missfirePolicy, handleClass, customData, oneByOneKey,
				null);
		return timerId;
	}

	public @NotNull String scheduleOffline(long roleId, @NotNull String cron, long times, long endTime,
										   @NotNull Class<? extends TimerHandle> handleClass,
										   @Nullable Bean customData) throws ParseException {
		return scheduleOffline(roleId, cron, times, endTime, Timer.eMissfirePolicyNothing, handleClass, customData);
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
		if (offlineTimers != null) { //TODO: fix offlineTimers is null
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
			if (bAny != null) {
				var timers = (BOnlineTimers)bAny.getAny().getBean();
				for (var timerId : timers.getTimerIds().keySet())
					cancelOnline(timerId);
			}
		}
		return 0;
	}

	private void scheduleOnlineCron(@NotNull String timerId, @NotNull BCronTimer cron, @NotNull TimerHandle handle) {
		try {
			scheduleOnlineCronNext(timerId, cron.getNextExpectedTime() - System.currentTimeMillis(), handle);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	private void scheduleOnlineCronHot(@NotNull String timerId, @NotNull BCronTimer cron,
									   @NotNull Class<? extends TimerHandle> handleClass) {
		try {
			scheduleOnlineCronNextHot(timerId, cron.getNextExpectedTime() - System.currentTimeMillis(), handleClass);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	// 再次调度 cron 定时器，真正安装到ThreadPool中。
	private void scheduleOnlineCronNext(@NotNull String timerId, long delay, @NotNull TimerHandle handle) {
		Transaction.whileCommit(() -> online.providerApp.zeze.getTimer().timerFutures.put(timerId,
				Task.scheduleUnsafe(delay, () -> fireOnlineCron(timerId, handle, false))));
	}

	private void scheduleOnlineCronNextHot(@NotNull String timerId, long delay,
										   @NotNull Class<? extends TimerHandle> handleClass) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(() -> timer.timerFutures.put(timerId, Task.scheduleUnsafe(delay,
				() -> fireOnlineCron(timerId, timer.findTimerHandle(handleClass.getName()), true))));
	}

	private void fireOnlineCron(@NotNull String timerId, @NotNull TimerHandle handle, boolean hot) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			var bTimer = online._tRoleTimers().get(timerId);
			if (bTimer == null) {
				Transaction.whileCommit(() -> timer.cancelFuture(timerId));
				return 0;
			}
			var timerLoginVersion = bTimer.getLoginVersion();
			var roleId = bTimer.getRoleId();
			var loginVersion = online.getLoginVersion(roleId);
			if (loginVersion == null || timerLoginVersion != loginVersion) {
				// 已经不是注册定时器时候的登录了
				logger.info("cancel online cron timer mismatch version({}!={}): timerId={}, roleId={}",
						timerLoginVersion, loginVersion, timerId, roleId);
				Transaction.whileCommit(() -> timer.cancelFuture(timerId));
				return 0;
			}

			var cronTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BCronTimer();
			Bean customData = null;
			var localBean = online.<BOnlineTimers>getLocalBean(roleId, eOnlineTimers);
			if (localBean != null) {
				var onlineCustom = localBean.getTimerIds().get(timerId);
				if (onlineCustom != null) {
					customData = onlineCustom.getCustomData().getBean();
					if (customData instanceof EmptyBean)
						customData = null;
				}
			}
			var hasNext = Timer.nextCronTimer(cronTimer, false);
			var context = new TimerContext(timer, timerId, handle.getClass().getName(), customData,
					cronTimer.getHappenTime(), cronTimer.getExpectedTime(), cronTimer.getNextExpectedTime());
			context.roleId = roleId;
			var serialSaved = bTimer.getSerialId();
			var r = Task.call(online.providerApp.zeze.newProcedure(() -> {
				handle.onTimer(context);
				return Procedure.Success;
			}, "TimerRole.fireOnlineCron.inner"));

			var bTimerNew = online._tRoleTimers().get(timerId);
			if (bTimerNew == null || bTimerNew.getSerialId() != serialSaved)
				return 0; // 已经取消或覆盖成新的timer

			if (r == Procedure.Exception) {
				logger.info("cancel online cron timer for exception: timerId={}, roleId={}", timerId, roleId);
				cancelOnline(timerId); // 异常错误不忽略
				return 0;
			}
			// 其他错误忽略

			if (hasNext) { // 准备下一个间隔
				long delay = cronTimer.getNextExpectedTime() - System.currentTimeMillis();
				if (hot)
					scheduleOnlineCronNextHot(timerId, delay, handle.getClass());
				else
					scheduleOnlineCronNext(timerId, delay, handle);
			} else
				cancelOnline(timerId);
			return 0;
		}, "TimerRole.fireOnlineCron"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				logger.info("cancel online cron timer for ret={}: {}", ret, timerId);
				cancelOnline(timerId);
				return 0;
			}, "TimerRole finally cancel impossible!"));
		}
	}

	// 调度 Simple 定时器到ThreadPool中。
	private void scheduleOnlineSimple(@NotNull String timerId, long delay, @NotNull TimerHandle handle) {
		Transaction.whileCommit(() -> online.providerApp.zeze.getTimer().timerFutures.put(timerId,
				Task.scheduleUnsafe(delay, () -> fireOnlineSimple(timerId, handle, false))));
	}

	private void scheduleOnlineSimpleHot(@NotNull String timerId, long delay,
										 @NotNull Class<? extends TimerHandle> handleClass) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(() -> timer.timerFutures.put(timerId, Task.scheduleUnsafe(delay,
				() -> fireOnlineSimple(timerId, timer.findTimerHandle(handleClass.getName()), true))));
	}

	private void fireOnlineSimple(@NotNull String timerId, @NotNull TimerHandle handle, boolean hot) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			var bTimer = online._tRoleTimers().get(timerId);
			if (bTimer == null) {
				Transaction.whileCommit(() -> timer.cancelFuture(timerId));
				return 0;
			}
			var timerLoginVersion = bTimer.getLoginVersion();
			var roleId = bTimer.getRoleId();
			var loginVersion = online.getLoginVersion(roleId);
			if (loginVersion == null || timerLoginVersion != loginVersion) {
				// 已经不是注册定时器时候的登录了
				logger.info("cancel online simple timer mismatch version({}!={}): timerId={}, roleId={}",
						timerLoginVersion, loginVersion, timerId, roleId);
				Transaction.whileCommit(() -> timer.cancelFuture(timerId));
				return 0;
			}

			var simpleTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BSimpleTimer();
			var serialSaved = bTimer.getSerialId();
			var hasNext = Timer.nextSimpleTimer(simpleTimer, false);
			var r = Task.call(online.providerApp.zeze.newProcedure(() -> {
				Bean customData = null;
				var localBean = online.<BOnlineTimers>getLocalBean(roleId, eOnlineTimers);
				if (localBean != null) {
					var onlineCustom = localBean.getTimerIds().get(timerId);
					if (onlineCustom != null) {
						customData = onlineCustom.getCustomData().getBean();
						if (customData instanceof EmptyBean)
							customData = null;
					}
				}
				var context = new TimerContext(timer, timerId, handle.getClass().getName(), customData,
						simpleTimer.getHappenTimes(), simpleTimer.getExpectedTime(), simpleTimer.getNextExpectedTime());
				context.roleId = roleId;
				handle.onTimer(context);
				return Procedure.Success;
			}, "TimerRole.fireOnlineSimple.inner"));

			var bTimerNew = online._tRoleTimers().get(timerId);
			if (bTimerNew == null || bTimerNew.getSerialId() != serialSaved)
				return 0; // 已经取消或覆盖成新的timer

			if (r == Procedure.Exception) {
				logger.info("cancel online simple timer for exception: timerId={}, roleId={}", timerId, roleId);
				cancelOnline(timerId); // 异常错误不忽略
				return 0;
			}
			// 其他错误忽略

			if (hasNext) { // 准备下一个间隔
				var delay = simpleTimer.getNextExpectedTime() - System.currentTimeMillis();
				if (hot)
					scheduleOnlineSimpleHot(timerId, delay, handle.getClass());
				else
					scheduleOnlineSimple(timerId, delay, handle);
			} else
				cancelOnline(timerId);
			return 0;
		}, "TimerRole.fireOnlineSimple"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				logger.info("cancel online simple timer for ret={}: {}", ret, timerId);
				cancelOnline(timerId);
				return 0;
			}, "TimerRole finally cancel impossible!"));
		}
	}
}
