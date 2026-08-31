package Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BOnlineTimers;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Builtin.Timer.BTransmitCronTimer;
import Zeze.Builtin.Timer.BTransmitSimpleTimer;
import Zeze.Hot.HotHandle;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Reflect;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TimerRole/TimerAccount的公共基类，实现online定时器的调度、触发、取消、转发逻辑。
 * 子类通过钩子方法提供身份、在线表、本地存储、转发等差异点。
 *
 * @param <I> 在线身份类型(Long=roleId, BAccountClientId=account+clientId)
 */
abstract class TimerOnlineBase<I> {
	private static final @NotNull Logger logger = LogManager.getLogger(TimerOnlineBase.class);

	/**
	 * 在线定时器记录的适配器，屏蔽BGameOnlineTimer/BArchOnlineTimer的差异。
	 */
	abstract static class OnlineTimer<I> {
		abstract @NotNull Bean getTimerObj();

		abstract long getSerialId();

		abstract long getLoginVersion();

		abstract @NotNull I identity();
	}

	// ///////////////////////////////////////////////////////////////
	// 子类钩子
	abstract @NotNull Timer timer();

	abstract @NotNull String name(); // 日志和存储过程命名用

	abstract @Nullable Long getLocalLoginVersion(@NotNull I id);

	abstract @Nullable Long getSharedLoginVersion(@NotNull I id);

	abstract @Nullable Long getLoginVersion(@NotNull I id); // 触发时校验用

	abstract @NotNull OnlineTimer<I> newOnlineTimer(@NotNull I id, long loginVersion, long serialId,
													@NotNull Bean timerObj);

	abstract @Nullable OnlineTimer<I> getOnlineTimer(@NotNull String timerId);

	abstract void insertOnlineTimer(@NotNull String timerId, @NotNull OnlineTimer<I> onlineTimer);

	abstract void removeOnlineTimer(@NotNull String timerId);

	abstract @NotNull BOnlineTimers getOrAddLocalTimers(@NotNull I id);

	abstract @Nullable BOnlineTimers getLocalTimers(@NotNull I id);

	abstract void removeLocalTimers(@NotNull I id);

	abstract void transmitSimple(@NotNull I target, @NotNull BTransmitSimpleTimer p);

	abstract void transmitCron(@NotNull I target, @NotNull BTransmitCronTimer p);

	abstract void transmitCancel(@NotNull I target, @NotNull String timerId, long loginVersion);

	abstract @NotNull String identityString(@NotNull I id); // 日志用

	abstract void fillContext(@NotNull I id, @NotNull TimerContext context);

	final @NotNull Application zeze() {
		return timer().zeze;
	}

	// 检查命名timerId格式合法（非法抛异常）且是否已被占用，被各scheduleOnlineNamed入口使用。
	final boolean isNamedTimerIdOccupied(@NotNull String timerId) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		return getOnlineTimer(timerId) != null;
	}

	final @NotNull String newAutoTimerId() {
		return '@' + timer().timerIdAutoKey.nextString();
	}

	// ///////////////////////////////////////////////////////////////
	// 调度（cold/hot共用）
	// hot=false时在调度时解析handle(要求cold调用者，由入口方法verifyCallerCold保证)，
	// hot=true时在触发时按名字解析handle(支持热更新模块)。
	final void scheduleOnline(boolean hot, @NotNull I id, @NotNull String timerId, @NotNull BSimpleTimer simpleTimer,
							  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
							  boolean fromTransmit) {
		Reflect.checkDefaultConstructor(handleClass);
		var localVersion = getLocalLoginVersion(id);
		var sharedVersion = getSharedLoginVersion(id);
		if (localVersion == null || sharedVersion == null || localVersion.longValue() != sharedVersion.longValue()) {
			if (fromTransmit) {
				logger.warn("schedule {}simple from transmit, but not login. {} timerId={} handle={}",
						hot ? "hot " : "", identityString(id), timerId, handleClass.getName());
				return;
			}
			if (sharedVersion != null) {
				var p = new BTransmitSimpleTimer();
				p.setTimerId(timerId);
				p.setHandleClass(handleClass.getName());
				p.setSimpleTimer(simpleTimer);
				p.setLoginVersion(sharedVersion);
				p.setHot(hot);
				if (customData != null) {
					p.setCustomClass(customData.getClass().getName());
					p.setCustomBean(new Binary(ByteBuffer.encode(customData)));
				}
				transmitSimple(id, p);
				logger.info("scheduleOnline{}(Simple): not online but transmit {} {}",
						hot ? "Hot" : "", identityString(id), timerId);
				return; // 登录在其他机器上，转发过去注册OnlineTimer，不管结果了。
			}
			throw new IllegalStateException("not online " + identityString(id) + ", " + timerId);
		}

		insertOnlineTimer(timerId, newOnlineTimer(id, localVersion, timer().timerSerialId.nextId(), simpleTimer));
		logger.debug("add online {}simple timer: timerId={}, {} handle={}",
				hot ? "hot " : "", timerId, identityString(id), handleClass.getName());

		var timerLocal = getOrAddLocalTimers(id).getTimerIds().getOrAdd(timerId);
		if (customData != null) {
			Timer.register(customData);
			timerLocal.getCustomData().setBean(customData);
			timer().tryRecordBeanHotModuleWhileCommit(customData);
		}
		var delay = Math.max(simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), 1);
		if (hot)
			scheduleOnlineSimpleHot(timerId, delay, handleClass);
		else
			scheduleOnlineSimple(timerId, delay, timer().findTimerHandle(handleClass.getName()));
	}

	final void scheduleOnline(boolean hot, @NotNull I id, @NotNull String timerId, @NotNull BCronTimer cronTimer,
							  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
							  boolean fromTransmit) {
		Reflect.checkDefaultConstructor(handleClass);
		var localVersion = getLocalLoginVersion(id);
		var sharedVersion = getSharedLoginVersion(id);
		if (localVersion == null || sharedVersion == null || localVersion.longValue() != sharedVersion.longValue()) {
			if (fromTransmit) {
				logger.warn("schedule {}cron from transmit, but not login. {} timerId={} handle={}",
						hot ? "hot " : "", identityString(id), timerId, handleClass.getName());
				return;
			}
			if (sharedVersion != null) {
				var p = new BTransmitCronTimer();
				p.setTimerId(timerId);
				p.setCronTimer(cronTimer);
				p.setHandleClass(handleClass.getName());
				p.setLoginVersion(sharedVersion);
				p.setHot(hot);
				if (customData != null) {
					p.setCustomClass(customData.getClass().getName());
					p.setCustomBean(new Binary(ByteBuffer.encode(customData)));
				}
				transmitCron(id, p);
				logger.info("scheduleOnline{}(Cron): not online but transmit {} {}",
						hot ? "Hot" : "", identityString(id), timerId);
				return; // 登录在其他机器上，转发过去注册OnlineTimer，不管结果了。
			}
			throw new IllegalStateException("not online " + identityString(id) + ", " + timerId);
		}

		insertOnlineTimer(timerId, newOnlineTimer(id, localVersion, timer().timerSerialId.nextId(), cronTimer));
		logger.debug("add online {}cron timer: timerId={}, {} handle={}",
				hot ? "hot " : "", timerId, identityString(id), handleClass.getName());

		var timerLocal = getOrAddLocalTimers(id).getTimerIds().getOrAdd(timerId);
		if (customData != null) {
			Timer.register(customData);
			timerLocal.getCustomData().setBean(customData);
			timer().tryRecordBeanHotModuleWhileCommit(customData);
		}
		if (hot)
			scheduleOnlineCronHot(timerId, cronTimer, handleClass);
		else
			scheduleOnlineCron(timerId, cronTimer, timer().findTimerHandle(handleClass.getName()));
	}

	// ///////////////////////////////////////////////////////////////
	// 取消
	final boolean cancelOnline(@Nullable String timerId, @NotNull I id, boolean fromTransmit) {
		if (timerId == null)
			return true; // 取消不存在的timer，认为成功。

		var localVersion = getLocalLoginVersion(id);
		var sharedVersion = getSharedLoginVersion(id);
		if (sharedVersion != null && (localVersion == null || localVersion.longValue() != sharedVersion.longValue())) {
			// 判断包括了localVersion是null的情况。
			if (fromTransmit) {
				logger.warn("cancelOnline from transmit, but not login at local server. {} timerId={}",
						identityString(id), timerId);
				return true;
			}
			transmitCancel(id, timerId, sharedVersion);
			logger.info("cancelOnline: transmit {} {}", identityString(id), timerId);
			return true; // 登录在其他机器上，转发过去取消OnlineTimer，不管结果了。
		}
		return cancelOnlineLocal(timerId);
	}

	final boolean cancelOnlineLocal(@Nullable String timerId) {
		if (timerId == null)
			return true;
		// always cancel future task，第一步就做这个。
		Transaction.whileCommit(() -> timer().cancelFuture(timerId));

		// remove online timer
		var bTimer = getOnlineTimer(timerId); // table.remove现在不能返回旧值，只能这样写。
		if (bTimer == null)
			return false;

		// remove online local
		var id = bTimer.identity();
		var onlineTimers = getLocalTimers(id);
		if (onlineTimers != null) {
			onlineTimers.getTimerIds().remove(timerId);
			if (onlineTimers.getTimerIds().isEmpty())
				removeLocalTimers(id);
		}
		// always remove from table
		removeOnlineTimer(timerId);
		logger.debug("cancel online timer: timerId={}, {}", timerId, identityString(id));
		return true;
	}

	// ///////////////////////////////////////////////////////////////
	// transmit接收，target是定时器归属者(不是发送者)。
	final long onTransmitSimpleTimer(@NotNull I target, @Nullable Binary parameter)
			throws ReflectiveOperationException {
		if (parameter == null)
			return 0;

		var p = new BTransmitSimpleTimer();
		p.decode(ByteBuffer.Wrap(parameter));

		var sharedVersion = getSharedLoginVersion(target);
		if (sharedVersion == null || p.getLoginVersion() != sharedVersion) {
			logger.warn("transmit simple timer dropped: not login or version mismatch. {} timerId={} handle={}",
					identityString(target), p.getTimerId(), p.getHandleClass());
			return 0;
		}
		var custom = decodeCustom(p.getCustomClass(), p.getCustomBean());
		@SuppressWarnings("unchecked")
		var handleClass = (Class<TimerHandle>)HotHandle.findClass(zeze(), p.getHandleClass());
		scheduleOnline(p.isHot(), target, p.getTimerId(), p.getSimpleTimer(), handleClass, custom, true);
		return 0;
	}

	final long onTransmitCronTimer(@NotNull I target, @Nullable Binary parameter)
			throws ReflectiveOperationException {
		if (parameter == null)
			return 0;

		var p = new BTransmitCronTimer();
		p.decode(ByteBuffer.Wrap(parameter));

		var sharedVersion = getSharedLoginVersion(target);
		if (sharedVersion == null || p.getLoginVersion() != sharedVersion) {
			logger.warn("transmit cron timer dropped: not login or version mismatch. {} timerId={} handle={}",
					identityString(target), p.getTimerId(), p.getHandleClass());
			return 0;
		}
		var custom = decodeCustom(p.getCustomClass(), p.getCustomBean());
		@SuppressWarnings("unchecked")
		var handleClass = (Class<TimerHandle>)HotHandle.findClass(zeze(), p.getHandleClass());
		scheduleOnline(p.isHot(), target, p.getTimerId(), p.getCronTimer(), handleClass, custom, true);
		return 0;
	}

	final void onTransmitCancel(@NotNull String timerId, @NotNull I id, long loginVersion) {
		var sharedVersion = getSharedLoginVersion(id);
		if (sharedVersion != null && loginVersion == sharedVersion)
			cancelOnline(timerId, id, true);
		else
			logger.debug("transmit cancel timer dropped: not login or version mismatch. {} timerId={}",
					identityString(id), timerId);
	}

	private @Nullable Bean decodeCustom(@NotNull String customClass, @NotNull Binary customBean)
			throws ReflectiveOperationException {
		if (customClass.isEmpty())
			return null;
		// 必须与handleClass一样走HotHandle.findClass：热更模块的custom bean在hot类加载器里，
		// Class.forName（框架类加载器）找不到类，跨服转发的定时器会因此丢失。
		var custom = (Bean)HotHandle.findClass(zeze(), customClass)
				.getConstructor((Class<?>[])null).newInstance((Object[])null);
		custom.decode(ByteBuffer.Wrap(customBean));
		return custom;
	}

	// Online.Local删除时，取消这个用户所有的在线定时器。
	final void onLocalRemove(@NotNull BOnlineTimers timers) {
		for (var timerId : timers.getTimerIds().keySet())
			cancelOnlineLocal(timerId);
	}

	// ///////////////////////////////////////////////////////////////
	// 安装到ThreadPool与触发
	private void scheduleOnlineSimple(@NotNull String timerId, long delay, @Nullable TimerHandle handle) {
		Transaction.whileCommit(() -> {
			var exist = timer().timerFutures.put(timerId,
					TaskSpec.ofAction(() -> fireOnlineSimple(timerId, handle, false)).scheduleNow(delay));
			if (null != exist)
				exist.cancel(false);
		});
	}

	private void scheduleOnlineSimpleHot(@NotNull String timerId, long delay,
										 @NotNull Class<? extends TimerHandle> handleClass) {
		var timer = timer();
		Transaction.whileCommit(() -> {
			var exist = timer.timerFutures.put(timerId, TaskSpec
					.ofAction(() -> fireOnlineSimple(timerId, timer.findTimerHandle(handleClass.getName()), true))
					.scheduleNow(delay));
			if (null != exist)
				exist.cancel(false);
		});
	}

	private void scheduleOnlineCron(@NotNull String timerId, @NotNull BCronTimer cron, @Nullable TimerHandle handle) {
		try {
			scheduleOnlineCronNext(timerId,
					Math.max(cron.getNextExpectedTime() - System.currentTimeMillis(), 1), handle);
		} catch (Exception ex) {
			logger.error("scheduleOnlineCron exception:", ex);
		}
	}

	private void scheduleOnlineCronHot(@NotNull String timerId, @NotNull BCronTimer cron,
									   @NotNull Class<? extends TimerHandle> handleClass) {
		try {
			scheduleOnlineCronNextHot(timerId,
					Math.max(cron.getNextExpectedTime() - System.currentTimeMillis(), 1), handleClass);
		} catch (Exception ex) {
			logger.error("scheduleOnlineCronHot exception:", ex);
		}
	}

	// 再次调度 cron 定时器，真正安装到ThreadPool中。
	private void scheduleOnlineCronNext(@NotNull String timerId, long delay, @Nullable TimerHandle handle) {
		Transaction.whileCommit(() -> {
			var exist = timer().timerFutures.put(timerId,
					TaskSpec.ofAction(() -> fireOnlineCron(timerId, handle, false)).scheduleNow(delay));
			if (null != exist)
				exist.cancel(false);
		});
	}

	private void scheduleOnlineCronNextHot(@NotNull String timerId, long delay,
										   @NotNull Class<? extends TimerHandle> handleClass) {
		var timer = timer();
		Transaction.whileCommit(() -> {
			var exist = timer.timerFutures.put(timerId, TaskSpec
					.ofAction(() -> fireOnlineCron(timerId, timer.findTimerHandle(handleClass.getName()), true))
					.scheduleNow(delay));
			if (null != exist)
				exist.cancel(false);
		});
	}

	private void fireOnlineCron(@NotNull String timerId, @Nullable TimerHandle handle, boolean hot) {
		fireOnline(timerId, handle, hot, "Cron", new FireKind<>() {
			private boolean hasNextFlag;

			@Override
			public long execute(@NotNull OnlineTimer<I> bTimer, @NotNull I id, @NotNull TimerHandle handle)
					throws Exception {
				var cronTimer = (BCronTimer)bTimer.getTimerObj();
				Bean customData = null;
				var localBean = getLocalTimers(id);
				if (localBean != null) {
					var onlineCustom = localBean.getTimerIds().get(timerId);
					if (onlineCustom != null) {
						customData = onlineCustom.getCustomData().getBean();
						if (customData instanceof EmptyBean)
							customData = null;
					}
				}
				hasNextFlag = CronTimerSpec.nextCronTimer(cronTimer, false);
				var context = new TimerContext(timer(), timerId, handle.getClass().getName(), customData,
						cronTimer.getHappenTimes(), cronTimer.getExpectedTime(), cronTimer.getNextExpectedTime());
				fillContext(id, context);
				return TaskSpec.ofProcedure(zeze().newProcedure(() -> {
					handle.onTimer(context);
					return Procedure.Success;
				}, name() + ".fireOnlineCron.inner")).call();
			}

			@Override
			public boolean hasNext(@NotNull OnlineTimer<I> bTimer, @NotNull I id, @NotNull TimerHandle handle) {
				return hasNextFlag;
			}

			@Override
			public void scheduleNext(@NotNull OnlineTimer<I> bTimer, @NotNull I id, @NotNull TimerHandle handle,
									 boolean hot) {
				var cronTimer = (BCronTimer)bTimer.getTimerObj();
				var delay = Math.max(cronTimer.getNextExpectedTime() - System.currentTimeMillis(), 1);
				if (hot)
					scheduleOnlineCronNextHot(timerId, delay, handle.getClass());
				else
					scheduleOnlineCronNext(timerId, delay, handle);
			}
		});
	}

	private void fireOnlineSimple(@NotNull String timerId, @Nullable TimerHandle handle, boolean hot) {
		fireOnline(timerId, handle, hot, "Simple", new FireKind<>() {
			@Override
			public long execute(@NotNull OnlineTimer<I> bTimer, @NotNull I id, @NotNull TimerHandle handle) {
				var simpleTimer = (BSimpleTimer)bTimer.getTimerObj();
				SimpleTimerSpec.beforeCallSimpleTimer(simpleTimer, false);
				return TaskSpec.ofProcedure(zeze().newProcedure(() -> {
					Bean customData = null;
					var localBean = getLocalTimers(id);
					if (localBean != null) {
						var onlineCustom = localBean.getTimerIds().get(timerId);
						if (onlineCustom != null) {
							customData = onlineCustom.getCustomData().getBean();
							if (customData instanceof EmptyBean)
								customData = null;
						}
					}
					var context = new TimerContext(timer(), timerId, handle.getClass().getName(), customData,
							simpleTimer.getHappenTimes(), simpleTimer.getExpectedTime(),
							simpleTimer.getNextExpectedTime());
					fillContext(id, context);
					handle.onTimer(context);
					simpleTimer.setNextExpectedTime(context.nextExpectedTimeMills);
					return Procedure.Success;
				}, name() + ".fireOnlineSimple.inner")).call();
			}

			@Override
			public boolean hasNext(@NotNull OnlineTimer<I> bTimer, @NotNull I id, @NotNull TimerHandle handle) {
				return ((BSimpleTimer)bTimer.getTimerObj()).getNextExpectedTime() != 0;
			}

			@Override
			public void scheduleNext(@NotNull OnlineTimer<I> bTimer, @NotNull I id, @NotNull TimerHandle handle,
									 boolean hot) {
				var simpleTimer = (BSimpleTimer)bTimer.getTimerObj();
				var delay = Math.max(simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), 1);
				if (hot)
					scheduleOnlineSimpleHot(timerId, delay, handle.getClass());
				else
					scheduleOnlineSimple(timerId, delay, handle);
			}
		});
	}

	private interface FireKind<T> {
		long execute(@NotNull OnlineTimer<T> bTimer, @NotNull T id, @NotNull TimerHandle handle) throws Exception;

		boolean hasNext(@NotNull OnlineTimer<T> bTimer, @NotNull T id, @NotNull TimerHandle handle);

		void scheduleNext(@NotNull OnlineTimer<T> bTimer, @NotNull T id, @NotNull TimerHandle handle, boolean hot);
	}

	private void fireOnline(@NotNull String timerId, @Nullable TimerHandle handle, boolean hot,
							@NotNull String kind, @NotNull FireKind<I> fireKind) {
		var timer = timer();
		var procSuffix = handle != null ? "." + handle.getClass().getName() : "";
		var ret = TaskSpec.ofProcedure(zeze().newProcedure(() -> {
			if (handle == null) {
				cancelOnlineLocal(timerId);
				return 0;
			}
			var bTimer = getOnlineTimer(timerId);
			if (bTimer == null) {
				Transaction.whileCommit(() -> timer.cancelFuture(timerId));
				return 0;
			}
			var timerLoginVersion = bTimer.getLoginVersion();
			var id = bTimer.identity();
			var loginVersion = getLoginVersion(id);
			if (loginVersion == null || timerLoginVersion != loginVersion) {
				// 已经不是注册定时器时候的登录了
				logger.info("cancel online {} timer mismatch version({}!={}): timerId={}, {}",
						kind.toLowerCase(), timerLoginVersion, loginVersion, timerId, identityString(id));
				Transaction.whileCommit(() -> timer.cancelFuture(timerId));
				return 0;
			}

			var serialSaved = bTimer.getSerialId();
			var r = fireKind.execute(bTimer, id, handle);

			var bTimerNew = getOnlineTimer(timerId);
			if (bTimerNew == null || bTimerNew.getSerialId() != serialSaved)
				return 0; // 已经取消或覆盖成新的timer

			if (r == Procedure.Exception) {
				logger.info("cancel online {} timer for exception: timerId={}, {}",
						kind.toLowerCase(), timerId, identityString(id));
				cancelOnlineLocal(timerId); // 异常错误不忽略
				return 0;
			}
			// 其他错误忽略

			if (fireKind.hasNext(bTimer, id, handle))
				fireKind.scheduleNext(bTimer, id, handle, hot);
			else
				cancelOnlineLocal(timerId);
			return 0;
		}, name() + ".fireOnline" + kind + procSuffix)).call();
		// 上面的存储过程几乎处理了所有错误，正常情况下总是返回0（成功），下面这个作为最终保护。
		if (ret != 0) {
			TaskSpec.ofProcedure(zeze().newProcedure(() -> {
				logger.info("cancel online {} timer for ret={}: {}", kind.toLowerCase(), ret, timerId);
				cancelOnlineLocal(timerId);
				return 0;
			}, name() + " finally cancel impossible!")).call();
		}
	}
}
