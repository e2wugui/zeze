package Zeze.Component;

import java.text.ParseException;
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
import Zeze.Hot.HotHandle;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
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
	private static final Logger logger = LogManager.getLogger(TimerRole.class);
	public static final String eOnlineTimers = "Zeze.Component.TimerGameOnline";
	// public static final String eTimerHandleName = "Zeze.Component.TimerGameOnline.Handle";
	public static final String eTransmitCronTimer = "Zeze.TimerRole.TransmitCronTimer";
	public static final String eTransmitSimpleTimer = "Zeze.TimerRole.TransmitSimpleTimer";

	final @NotNull Online online;

	public TimerRole(@NotNull Online online) {
		this.online = online;

		online.getTransmitActions().put(eTransmitCronTimer, this::transmitCronTimerHandle);
		online.getTransmitActions().put(eTransmitSimpleTimer, this::transmitSimpleTimerHandle);
		// online timer 生命期和 Online.Local 一致。
		online.getLocalRemoveEvents().add(EventDispatcher.Mode.RunEmbed, this::onLocalRemoveEvent);
		online.getLoginEvents().add(EventDispatcher.Mode.RunEmbed, this::onLoginEvent);
		online.getReloginEvents().add(EventDispatcher.Mode.RunEmbed, this::onLoginEvent);
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, long delay, long period, long times,
									   long endTime, @NotNull TimerHandle handleName, @Nullable Bean customData) {
		return scheduleOnlineNamed(roleId, timerId, delay, period, times, endTime,
				handleName, customData, "");
	}

	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, long delay, long period, long times,
									   long endTime, @NotNull TimerHandle handleName, @Nullable Bean customData,
									   String oneByOneKey) {

		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		if (online._tRoleTimers().get(timerId) != null)
			return false;
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		scheduleOnline(roleId, timerId, simpleTimer, handleName, customData, false);
		return true;
	}

	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, long delay, long period, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleName,
										  @Nullable Bean customData) {
		return scheduleOnlineNamedHot(roleId, timerId, delay, period, times, endTime,
				handleName, customData, "");
	}

	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, long delay, long period, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleName,
										  @Nullable Bean customData, String oneByOneKey) {
		if (online._tRoleTimers().get(timerId) != null)
			return false;
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		scheduleOnlineHot(roleId, timerId, simpleTimer, handleName, customData, false);
		return true;
	}

	// 本进程内的有名字定时器，名字仅在本进程内唯一。
	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, @NotNull String cron, long times,
									   long endTime, @NotNull TimerHandle handleName,
									   @Nullable Bean customData) throws Exception {
		return scheduleOnlineNamed(roleId, timerId,
				cron, times, endTime,
				handleName, customData, "");
	}

	public boolean scheduleOnlineNamed(long roleId, @NotNull String timerId, @NotNull String cron, long times,
									   long endTime, @NotNull TimerHandle handleName,
									   @Nullable Bean customData, String oneByOneKey) throws Exception {

		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		if (online._tRoleTimers().get(timerId) != null)
			return false;
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		scheduleOnline(roleId, timerId, cronTimer, handleName, customData, false);
		return true;
	}

	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, @NotNull String cron, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleName,
										  @Nullable Bean customData) throws Exception {
		return scheduleOnlineNamedHot(roleId, timerId,
				cron, times, endTime, handleName, customData, "");
	}

	public boolean scheduleOnlineNamedHot(long roleId, @NotNull String timerId, @NotNull String cron, long times,
										  long endTime, @NotNull Class<? extends TimerHandle> handleName,
										  @Nullable Bean customData, String oneByOneKey) throws Exception {
		if (online._tRoleTimers().get(timerId) != null)
			return false;
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		scheduleOnlineHot(roleId, timerId, cronTimer, handleName, customData, false);
		return true;
	}

	public @NotNull String scheduleOnline(long roleId, long delay, long period, long times, long endTime,
										  @NotNull TimerHandle name, @Nullable Bean customData) {
		return scheduleOnline(roleId, delay, period, times, endTime,
				name, customData, "");
	}

	public @NotNull String scheduleOnline(long roleId, long delay, long period, long times, long endTime,
										  @NotNull TimerHandle name, @Nullable Bean customData,
										  String oneByOneKey) {

		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		var timer = online.providerApp.zeze.getTimer();
		var timerId = '@' + timer.timerIdAutoKey.nextString();
		scheduleOnline(roleId, timerId, simpleTimer, name, customData, false);
		return timerId;
	}

	public @NotNull String scheduleOnlineHot(long roleId, long delay, long period, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> name,
											 @Nullable Bean customData) {
		return scheduleOnlineHot(roleId, delay, period, times, endTime,
				name, customData, "");
	}

	public @NotNull String scheduleOnlineHot(long roleId, long delay, long period, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> name,
											 @Nullable Bean customData, String oneByOneKey) {
		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		var timer = online.providerApp.zeze.getTimer();
		var timerId = '@' + timer.timerIdAutoKey.nextString();
		scheduleOnlineHot(roleId, timerId, simpleTimer, name, customData, false);
		return timerId;
	}

	private void scheduleOnline(long roleId, @NotNull String timerId, @NotNull BSimpleTimer simpleTimer,
								@NotNull TimerHandle name, @Nullable Bean customData, boolean fromTransmit) {
		// 去掉下面两行，允许在非登录状态注册timer。现在不允许。
		var loginVersion = online.getLocalLoginVersion(roleId);
		if (null == loginVersion) {
			if (fromTransmit) {
				logger.warn("schedule simple from transmit, but not login. roleId={} {}",
						roleId, name.getClass().getName());
				return;
			}
			var loginOnline = online.getLoginOnline(roleId);
			if (null != loginOnline) {
				// 参数 timerId, cronTimer, timerHandleClassName, customData
				var p = new BTransmitSimpleTimer();
				p.setTimerId(timerId);
				p.setSimpleTimer(simpleTimer);
				p.setHandleClass(name.getClass().getName());
				p.setLoginVersion(loginOnline.getLoginVersion());
				p.setHot(false);
				if (null != customData) {
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

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion, timer.timerSerialId.nextId());
		online._tRoleTimers().put(timerId, onlineTimer);
		onlineTimer.getTimerObj().setBean(simpleTimer);

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (null != customData) {
			Timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleSimple(timerId, simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), name);
	}

	private void scheduleOnlineHot(long roleId, @NotNull String timerId, @NotNull BSimpleTimer simpleTimer,
								   @NotNull Class<? extends TimerHandle> name, @Nullable Bean customData,
								   boolean fromTransmit) {
		// 去掉下面两行，允许在非登录状态注册timer。现在不允许。
		var loginVersion = online.getLocalLoginVersion(roleId);
		if (null == loginVersion) {
			if (fromTransmit) {
				logger.warn("schedule hot simple from transmit, but not login. roleId={} {}", roleId, name.getName());
				return;
			}
			var loginOnline = online.getLoginOnline(roleId);
			if (null != loginOnline) {
				// 参数 timerId, cronTimer, timerHandleClassName, customData
				var p = new BTransmitSimpleTimer();
				p.setTimerId(timerId);
				p.setSimpleTimer(simpleTimer);
				p.setHandleClass(name.getName());
				p.setLoginVersion(loginOnline.getLoginVersion());
				p.setHot(true);
				if (null != customData) {
					p.setCustomClass(customData.getClass().getName());
					p.setCustomBean(new Binary(ByteBuffer.encode(customData)));
				}
				online.transmitEmbed(roleId, eTransmitSimpleTimer, List.of(roleId), new Binary(ByteBuffer.encode(p)), false);
			}
			logger.info("scheduleOnlineHot(Simple): not online {}", roleId);
		} else {
			var timer = online.providerApp.zeze.getTimer();
			var onlineTimer = new BGameOnlineTimer(roleId, loginVersion, timer.timerSerialId.nextId());
			online._tRoleTimers().put(timerId, onlineTimer);
			onlineTimer.getTimerObj().setBean(simpleTimer);

			var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
			var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
			if (null != customData) {
				Timer.register(customData.getClass());
				timerLocal.getCustomData().setBean(customData);
			}
			scheduleSimpleHot(timerId, simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), name);
		}
	}

	public @NotNull String scheduleOnline(long roleId, @NotNull String cron, long times, long endTime,
										  @NotNull TimerHandle name, @Nullable Bean customData) throws Exception {
		return scheduleOnline(roleId, cron, times, endTime, name, customData, "");
	}

	public @NotNull String scheduleOnline(long roleId, @NotNull String cron, long times, long endTime,
										  @NotNull TimerHandle name,
										  @Nullable Bean customData, String oneByOneKey) throws Exception {

		online.providerApp.zeze.verifyCallerCold(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());

		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		var timer = online.providerApp.zeze.getTimer();
		var timerId = "@" + timer.timerIdAutoKey.nextString();
		scheduleOnline(roleId, timerId, cronTimer, name, customData, false);
		return timerId;
	}

	public @NotNull String scheduleOnlineHot(long roleId, @NotNull String cron, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> name,
											 @Nullable Bean customData) throws Exception {
		return scheduleOnlineHot(roleId, cron, times, endTime, name, customData, "");
	}

	public @NotNull String scheduleOnlineHot(long roleId, @NotNull String cron, long times, long endTime,
											 @NotNull Class<? extends TimerHandle> name,
											 @Nullable Bean customData, String oneByOneKey) throws Exception {
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		var timer = online.providerApp.zeze.getTimer();
		var timerId = "@" + timer.timerIdAutoKey.nextString();
		scheduleOnlineHot(roleId, timerId, cronTimer, name, customData, false);
		return timerId;
	}

	private long transmitCronTimerHandle(long sender, long target, @Nullable Binary parameter) throws Exception {
		if (null == parameter)
			return 0;

		var p = new BTransmitCronTimer();
		p.decode(ByteBuffer.Wrap(parameter));

		var loginOnline = online.getLoginOnline(target);
		if (null != loginOnline && p.getLoginVersion() == loginOnline.getLoginVersion()) {
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
				var handleClass = Class.forName(p.getHandleClass());
				var handle = handleClass.getDeclaredConstructor().newInstance();
				scheduleOnline(sender, p.getTimerId(), p.getCronTimer(), (TimerHandle)handle, custom, true);
			}
		}
		return 0;
	}

	private long transmitSimpleTimerHandle(long sender, long target, @Nullable Binary parameter) throws Exception {
		if (null == parameter)
			return 0;

		var p = new BTransmitSimpleTimer();
		p.decode(ByteBuffer.Wrap(parameter));

		var loginOnline = online.getLoginOnline(target);
		if (null != loginOnline && p.getLoginVersion() == loginOnline.getLoginVersion()) {
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
				var handleClass = Class.forName(p.getHandleClass());
				var handle = handleClass.getDeclaredConstructor().newInstance();
				scheduleOnline(sender, p.getTimerId(), p.getSimpleTimer(), (TimerHandle)handle, custom, true);
			}
		}
		return 0;
	}

	private void scheduleOnline(long roleId, @NotNull String timerId, @NotNull BCronTimer cronTimer,
								@NotNull TimerHandle name, @Nullable Bean customData, boolean fromTransmit) {
		var loginVersion = online.getLocalLoginVersion(roleId);
		if (null == loginVersion) {
			if (fromTransmit) {
				logger.warn("schedule cron from transmit, but not login. roleId={} {}",
						roleId, name.getClass().getName());
				return;
			}
			var loginOnline = online.getLoginOnline(roleId);
			if (null != loginOnline) {
				// 参数 timerId, cronTimer, timerHandleClassName, customData
				var p = new BTransmitCronTimer();
				p.setTimerId(timerId);
				p.setCronTimer(cronTimer);
				p.setHandleClass(name.getClass().getName());
				p.setLoginVersion(loginOnline.getLoginVersion());
				p.setHot(false);
				if (null != customData) {
					p.setCustomClass(customData.getClass().getName());
					p.setCustomBean(new Binary(ByteBuffer.encode(customData)));
				}
				online.transmitEmbed(roleId, eTransmitCronTimer, List.of(roleId), new Binary(ByteBuffer.encode(p)), false);
				logger.info("scheduleOnline(Cron): not online but transmit {}", roleId);
				return; // 登录在其他机器上，转发过去注册OnlineTimer，不管结果了。
			}
			throw new IllegalStateException("not online " + roleId);
		}

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion, timer.timerSerialId.nextId());
		onlineTimer.getTimerObj().setBean(cronTimer);
		online._tRoleTimers().insert(timerId, onlineTimer);

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (null != customData) {
			Timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleCron(timerId, cronTimer, name);
	}

	private void scheduleOnlineHot(long roleId, @NotNull String timerId, @NotNull BCronTimer cronTimer,
								   @NotNull Class<? extends TimerHandle> name, @Nullable Bean customData,
								   boolean fromTransmit) {
		var loginVersion = online.getLocalLoginVersion(roleId);
		if (null == loginVersion) {
			if (fromTransmit) {
				logger.warn("schedule hot cron from transmit, but not login. roleId={} {}", roleId, name.getName());
				return;
			}
			var loginOnline = online.getLoginOnline(roleId);
			if (null != loginOnline) {
				// 参数 timerId, cronTimer, timerHandleClassName, customData
				var p = new BTransmitCronTimer();
				p.setTimerId(timerId);
				p.setCronTimer(cronTimer);
				p.setHandleClass(name.getName());
				p.setLoginVersion(loginOnline.getLoginVersion());
				p.setHot(true);
				if (null != customData) {
					p.setCustomClass(customData.getClass().getName());
					p.setCustomBean(new Binary(ByteBuffer.encode(customData)));
				}
				online.transmitEmbed(roleId, eTransmitCronTimer, List.of(roleId), new Binary(ByteBuffer.encode(p)), false);
			}
			logger.info("scheduleOnlineHot(Cron): not online {}", roleId);
			return;
		}

		var timer = online.providerApp.zeze.getTimer();
		var onlineTimer = new BGameOnlineTimer(roleId, loginVersion, timer.timerSerialId.nextId());
		onlineTimer.getTimerObj().setBean(cronTimer);
		online._tRoleTimers().insert(timerId, onlineTimer);

		var timerIds = online.getOrAddLocalBean(roleId, eOnlineTimers, new BOnlineTimers());
		var timerLocal = timerIds.getTimerIds().getOrAdd(timerId);
		if (null != customData) {
			Timer.register(customData.getClass());
			timerLocal.getCustomData().setBean(customData);
		}
		scheduleCronHot(timerId, cronTimer, name);
	}

	public boolean cancel(String timerId) {
		if (null == timerId)
			return true; // 取消不存在的timer，认为成功。

		var timer = online.providerApp.zeze.getTimer();

		// always cancel future task，第一步就做这个。
		timer.cancelFuture(timerId);

		// remove online timer
		var bTimer = online._tRoleTimers().get(timerId); // table.remove现在不能返回旧值，只能这样写。
		if (null == bTimer)
			return false;

		// remove online local
		var onlineTimers = (BOnlineTimers)online.getLocalBean(bTimer.getRoleId(), eOnlineTimers);
		if (null != onlineTimers) {
			onlineTimers.getTimerIds().remove(timerId);
			if (onlineTimers.getTimerIds().isEmpty())
				online.removeLocalBean(bTimer.getRoleId(), eOnlineTimers);
		}
		// always remove from table.
		online._tRoleTimers().remove(timerId);
		return true;
	}

	public @NotNull boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, long delay, long period,
												 long times, long endTime, int missFirePolicy,
												 @NotNull Class<? extends TimerHandle> handleClassName,
												 @Nullable Bean customData) {
		return scheduleOfflineNamed(timerId, roleId, delay, period, times, endTime,
				missFirePolicy, handleClassName, customData, "");
	}

	public @NotNull boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, long delay, long period,
												 long times, long endTime, int missFirePolicy,
												 @NotNull Class<? extends TimerHandle> handleClassName,
												 @Nullable Bean customData, String oneByOneKey) {
		var timer = online.providerApp.zeze.getTimer();
		var timerIndex = timer.tIndexs().get(timerId);
		if (null != timerIndex)
			return false;
		scheduleOffline(timerId, roleId, delay, period, times, endTime, missFirePolicy,
				handleClassName, customData, oneByOneKey);
		return true;
	}

	private @NotNull String scheduleOffline(@NotNull String timerId, long roleId, long delay, long period,
											long times, long endTime, int missFirePolicy,
											@NotNull Class<? extends TimerHandle> handleClassName,
											@Nullable Bean customData, String oneByOneKey) {

		var logoutVersion = online.getLogoutVersion(roleId);
		if (null == logoutVersion)
			throw new IllegalStateException("not logout. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineRoleCustom("", roleId, logoutVersion, handleClassName.getName(), online.getOnlineSetName());

		var simpleTimer = new BSimpleTimer();
		Timer.initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		simpleTimer.setMissfirePolicy(missFirePolicy);
		var timerName = timer.schedule(timerId, simpleTimer, OfflineHandle.class, custom);

		custom.setTimerName(timerName); // 没办法，循环依赖了，只能在这里设置。
		if (null != customData) {
			Timer.register(customData.getClass());
			custom.getCustomData().setBean(customData);
			timer.tryRecordBeanHotModuleWhileCommit(customData);
		}
		var offline = online._tRoleOfflineTimers().getOrAdd(roleId);
		if (offline.getOfflineTimers().size() > timer.zeze.getConfig().getOfflineTimerLimit()) {
			// throw new IllegalStateException("too many offline timers. roleId=" + roleId + " size=" + offline.getOfflineTimers().size());
			logger.error("too many offline timers. roleId={}, size={}", roleId, offline.getOfflineTimers().size());
		}

		if (null != offline.getOfflineTimers().putIfAbsent(timerName, timer.zeze.getConfig().getServerId()))
			throw new IllegalStateException("duplicate timerName. roleId=" + roleId);
		return timerName;
	}

	public @NotNull String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
										   int missFirePolicy, @NotNull Class<? extends TimerHandle> handleClassName,
										   @Nullable Bean customData) {
		return scheduleOffline(roleId, delay, period, times, endTime,
				missFirePolicy, handleClassName, customData, "");
	}

	public @NotNull String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
										   int missFirePolicy, @NotNull Class<? extends TimerHandle> handleClassName,
										   @Nullable Bean customData, String oneByOneKey) {
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOffline("@" + timer.timerIdAutoKey.nextString(),
				roleId, delay, period, times, endTime, missFirePolicy,
				handleClassName, customData, oneByOneKey);
	}

	public @NotNull String scheduleOffline(long roleId, long delay, long period, long times, long endTime,
										   @NotNull Class<? extends TimerHandle> handleClassName,
										   @Nullable Bean customData) {
		return scheduleOffline(roleId, delay, period, times, endTime, Timer.eMissfirePolicyNothing, handleClassName, customData);
	}

	public @NotNull boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, @NotNull String cron,
												 long times, long endTime, int missFirePolicy,
												 @NotNull Class<? extends TimerHandle> handleClassName,
												 @Nullable Bean customData) throws ParseException {
		return scheduleOfflineNamed(timerId, roleId, cron, times, endTime,
				missFirePolicy, handleClassName, customData, "");
	}

	public @NotNull boolean scheduleOfflineNamed(@NotNull String timerId, long roleId, @NotNull String cron,
												 long times, long endTime, int missFirePolicy,
												 @NotNull Class<? extends TimerHandle> handleClassName,
												 @Nullable Bean customData, String oneByOneKey) throws ParseException {
		var timer = online.providerApp.zeze.getTimer();
		var timerIndex = timer.tIndexs().get(timerId);
		if (null != timerIndex)
			return false;

		scheduleOffline(timerId, roleId, cron, times, endTime, missFirePolicy, handleClassName, customData, oneByOneKey);
		return true;
	}

	private @NotNull String scheduleOffline(@NotNull String timerId, long roleId, @NotNull String cron, long times,
											long endTime, int missFirePolicy,
											@NotNull Class<? extends TimerHandle> handleClassName,
											@Nullable Bean customData, String oneByOneKey) throws ParseException {
		var logoutVersion = online.getLogoutVersion(roleId);
		if (null == logoutVersion)
			throw new IllegalStateException("not logout. roleId=" + roleId);

		var timer = online.providerApp.zeze.getTimer();
		var custom = new BOfflineRoleCustom("", roleId, logoutVersion, handleClassName.getName(), online.getOnlineSetName());
		var cronTimer = new BCronTimer();
		Timer.initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		cronTimer.setMissfirePolicy(missFirePolicy);
		var timerName = timer.schedule(timerId, cronTimer, OfflineHandle.class, custom);
		custom.setTimerName(timerName); // 没办法，循环依赖了，只能在这里设置。
		if (null != customData) {
			Timer.register(customData.getClass());
			custom.getCustomData().setBean(customData);
		}
		var offline = online._tRoleOfflineTimers().getOrAdd(roleId);
		if (offline.getOfflineTimers().size() > timer.zeze.getConfig().getOfflineTimerLimit())
			throw new IllegalStateException("too many offline timers. roleId=" + roleId + " size=" + offline.getOfflineTimers().size());

		if (null != offline.getOfflineTimers().putIfAbsent(timerName, timer.zeze.getConfig().getServerId()))
			throw new IllegalStateException("duplicate timerName. roleId=" + roleId);
		return timerName;
	}

	public @NotNull String scheduleOffline(long roleId, @NotNull String cron, long times, long endTime,
										   int missFirePolicy, @NotNull Class<? extends TimerHandle> handleClassName,
										   @Nullable Bean customData) throws ParseException {
		return scheduleOffline(roleId, cron, times, endTime,
				missFirePolicy, handleClassName, customData, "");
	}

	public @NotNull String scheduleOffline(long roleId, @NotNull String cron, long times, long endTime,
										   int missFirePolicy, @NotNull Class<? extends TimerHandle> handleClassName,
										   @Nullable Bean customData, String oneByOneKey) throws ParseException {
		var timer = online.providerApp.zeze.getTimer();
		return scheduleOffline("@" + timer.timerIdAutoKey.nextString(),
				roleId, cron, times, endTime, missFirePolicy, handleClassName, customData, oneByOneKey);
	}

	public @NotNull String scheduleOffline(long roleId, @NotNull String cron, long times, long endTime,
										   @NotNull Class<? extends TimerHandle> handleClassName,
										   @Nullable Bean customData) throws ParseException {
		return scheduleOffline(roleId, cron, times, endTime, Timer.eMissfirePolicyNothing, handleClassName, customData);
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 内部实现
	public static class OfflineHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var offlineCustom = (BOfflineRoleCustom)context.customData;
			var loginVersion = context.timer.getRoleTimer(offlineCustom.getOnlineSetName()).online.getLogoutVersion(offlineCustom.getRoleId());
			// 检查版本号，不正确的登录版本号表示过期的timer，取消掉即可。
			if (null != loginVersion && loginVersion == offlineCustom.getLoginVersion()) {
				@SuppressWarnings("unchecked")
				var handleClass = (Class<? extends TimerHandle>)Class.forName(offlineCustom.getHandleName());
				final var handle = handleClass.getDeclaredConstructor().newInstance();
				context.roleId = offlineCustom.getRoleId();
				var userCustom = offlineCustom.getCustomData().getBean();
				context.customData = userCustom instanceof EmptyBean ? null : userCustom;
				handle.onTimer(context);
			} else {
				context.timer.cancel(offlineCustom.getTimerName());
				var online = context.timer.getDefaultOnline().getOnline(offlineCustom.getOnlineSetName());
				if (online != null) {
					var offlineTimers = online._tRoleOfflineTimers().get(offlineCustom.getRoleId());
					if (offlineTimers != null)
						offlineTimers.getOfflineTimers().remove(offlineCustom.getTimerName());
				}
			}
		}

		@Override
		public void onTimerCancel() {
		}
	}

	private long onLoginEvent(@NotNull Object sender, @NotNull EventDispatcher.EventArgument arg) {
		var timer = online.providerApp.zeze.getTimer();
		var loginArg = (LoginArgument)arg;
		var offlineTimers = online._tRoleOfflineTimers().get(loginArg.roleId);
		// X: fix offlineTimers is null
		if (null == offlineTimers)
			return 0;
		for (var e : offlineTimers.getOfflineTimers().entrySet()) {
			timer.cancel(e.getKey());
		}
		// 嵌入本地服务器事件事务中，
		// 删除之后，如果上面的redirectCancel失败，
		// 那么该timer触发的时候会检测到版本号不一致，
		// 然后timer最终也会被cancel掉。
		online._tRoleOfflineTimers().remove(loginArg.roleId);
		return 0;
	}

	// Online.Local 删除事件，取消这个用户所有的在线定时器。
	private long onLocalRemoveEvent(@NotNull Object sender, @NotNull EventDispatcher.EventArgument arg) {
		var local = (LocalRemoveEventArgument)arg;
		if (null != local.local) {
			var bAny = local.local.getDatas().get(eOnlineTimers);
			if (null != bAny) {
				var timers = (BOnlineTimers)bAny.getAny().getBean();
				for (var timerId : timers.getTimerIds().keySet())
					cancel(timerId);
			}
		}
		return 0;
	}

	// 调度 cron 定时器
	private void scheduleCron(@NotNull String timerId, @NotNull BCronTimer cron, @NotNull TimerHandle name) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(timerId, delay, name);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	private void scheduleCronHot(@NotNull String timerId, @NotNull BCronTimer cron, @NotNull Class<? extends TimerHandle> name) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNextHot(timerId, delay, name);
		} catch (Exception ex) {
			Timer.logger.error("", ex);
		}
	}

	// 再次调度 cron 定时器，真正安装到ThreadPool中。
	private void scheduleCronNext(@NotNull String timerId, long delay, @NotNull TimerHandle name) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireCron(timerId, name, false))));
	}

	private void scheduleCronNextHot(@NotNull String timerId, long delay, @NotNull Class<? extends TimerHandle> name) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireCron(timerId, timer.findTimerHandle(name.getName()), true))));
	}

	private void fireCron(@NotNull String timerId, @NotNull TimerHandle handle, boolean hot) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			/*
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}
			*/

			var bTimer = online._tRoleTimers().get(timerId);
			if (null == bTimer) {
				timer.cancelFuture(timerId);
				return 0; // done
			}
			var loginVersion = online.getLoginVersion(bTimer.getRoleId());
			if (null == loginVersion || bTimer.getLoginVersion() != loginVersion) {
				// 已经不是注册定时器时候的登录了。
				timer.cancelFuture(timerId);
				return 0; // done
			}

			var cronTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BCronTimer();
			Bean customData = null;
			var localBean = online.<BOnlineTimers>getLocalBean(bTimer.getRoleId(), eOnlineTimers);
			if (localBean != null) {
				var onlineCustom = localBean.getTimerIds().get(timerId);
				if (onlineCustom != null) {
					customData = onlineCustom.getCustomData().getBean();
					if (customData instanceof EmptyBean)
						customData = null;
				}
			}
			var context = new TimerContext(timer, timerId, handle.getClass().getName(), customData,
					cronTimer.getHappenTime(), cronTimer.getNextExpectedTime(),
					cronTimer.getExpectedTime());
			context.roleId = bTimer.getRoleId();
			var serialSaved = bTimer.getSerialId();
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				handle.onTimer(context);
				return Procedure.Success;
			}, "TimerRole.fireOnlineLocalHandle"));

			var bTimerNew = online._tRoleTimers().get(timerId);
			if (bTimerNew == null || bTimerNew.getSerialId() != serialSaved)
				return 0; // canceled or new timer

			if (retNest == Procedure.Exception) {
				cancel(timerId); // 异常错误不忽略。
				return 0;
			}
			// skip other error

			if (!Timer.nextCronTimer(cronTimer, false)) {
				cancel(timerId);
				return 0; // procedure done
			}

			// continue period
			long delay = cronTimer.getNextExpectedTime() - System.currentTimeMillis();
			if (hot)
				scheduleCronNextHot(timerId, delay, handle.getClass());
			else
				scheduleCronNext(timerId, delay, handle);
			return 0;
		}, "TimerRole.fireOnlineSimpleTimer"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "TimerRole finally cancel impossible!"));
		}
	}

	// 调度 Simple 定时器到ThreadPool中。
	private void scheduleSimple(@NotNull String timerId, long delay, @NotNull TimerHandle name) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireSimple(timerId, name, false))));
	}

	private void scheduleSimpleHot(@NotNull String timerId, long delay, @NotNull Class<? extends TimerHandle> name) {
		var timer = online.providerApp.zeze.getTimer();
		Transaction.whileCommit(
				() -> timer.timersFuture.put(timerId, Task.scheduleUnsafe(delay,
						() -> fireSimple(timerId, timer.findTimerHandle(name.getName()), true))));
	}

	// Timer发生，执行回调。
	private void fireSimple(@NotNull String timerId, @NotNull TimerHandle handle, boolean hot) {
		var timer = online.providerApp.zeze.getTimer();
		var ret = Task.call(online.providerApp.zeze.newProcedure(() -> {
			/*
			if (null == handle) {
				cancel(timerId);
				return 0; // done
			}
			*/

			var bTimer = online._tRoleTimers().get(timerId);
			if (null == bTimer) {
				timer.cancelFuture(timerId);
				return 0; // done
			}
			var loginVersion = online.getLoginVersion(bTimer.getRoleId());
			if (null == loginVersion || bTimer.getLoginVersion() != loginVersion) {
				// 已经不是注册定时器时候的登录了。
				timer.cancelFuture(timerId);
				return 0; // done
			}

			var simpleTimer = bTimer.getTimerObj_Zeze_Builtin_Timer_BSimpleTimer();
			var serialSaved = bTimer.getSerialId();
			var retNest = Task.call(online.providerApp.zeze.newProcedure(() -> {
				Bean customData = null;
				var localBean = online.<BOnlineTimers>getLocalBean(bTimer.getRoleId(), eOnlineTimers);
				if (localBean != null) {
					var onlineCustom = localBean.getTimerIds().get(timerId);
					if (onlineCustom != null) {
						customData = onlineCustom.getCustomData().getBean();
						if (customData instanceof EmptyBean)
							customData = null;
					}
				}
				var context = new TimerContext(timer, timerId, handle.getClass().getName(), customData,
						simpleTimer.getHappenTimes(), simpleTimer.getNextExpectedTime(),
						simpleTimer.getExpectedTime());
				context.roleId = bTimer.getRoleId();
				handle.onTimer(context);
				return Procedure.Success;
			}, "TimerRole.fireOnlineLocalHandle"));

			var bTimerNew = online._tRoleTimers().get(timerId);
			if (bTimerNew == null || bTimerNew.getSerialId() != serialSaved)
				return 0; // canceled or new timer

			if (retNest == Procedure.Exception) {
				cancel(timerId); // 异常错误不忽略。
				return 0;
			}
			// 其他错误忽略

			// 准备下一个间隔
			if (!Timer.nextSimpleTimer(simpleTimer, false)) {
				cancel(timerId);
				return 0;
			}

			// continue period
			var delay = simpleTimer.getNextExpectedTime() - System.currentTimeMillis();
			if (hot)
				scheduleSimpleHot(timerId, delay, handle.getClass());
			else
				scheduleSimple(timerId, delay, handle);
			return 0;
		}, "TimerRole.fireOnlineSimpleTimer"));
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			Task.call(online.providerApp.zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "TimerRole finally cancel impossible!"));
		}
	}
}
