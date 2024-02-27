package Zeze.Component;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderImplement;
import Zeze.Arch.ProviderWithOnline;
import Zeze.Arch.RedirectToServer;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BNode;
import Zeze.Builtin.Timer.BNodeRoot;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Builtin.Timer.BTimer;
import Zeze.Builtin.Timer.tAccountOfflineTimers;
import Zeze.Builtin.Timer.tAccountTimers;
import Zeze.Builtin.Timer.tIndexs;
import Zeze.Collections.BeanFactory;
import Zeze.Game.Online;
import Zeze.Hot.HotBeanFactory;
import Zeze.Hot.HotHandle;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Serialize.Serializable;
import Zeze.Services.ServiceManager.BOfflineNotify;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.LongHashSet;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.CronExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Timer extends AbstractTimer implements HotBeanFactory {
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(@NotNull Serializable bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static @NotNull Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static long getOnlineSpecialTypeIdFromBean(@NotNull Serializable bean) {
		return bean.typeId();
	}

	public static @NotNull Bean createOnlineBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static final int CountPerNode = 50;

	static final Logger logger = LogManager.getLogger(Timer.class);
	public final Application zeze;
	private AutoKey nodeIdAutoKey;
	AutoKey timerIdAutoKey;
	AutoKey timerSerialId;

	// 在这台服务器进程内调度的所有Timer。key是timerId，value是ThreadPool.schedule的返回值。
	final ConcurrentHashMap<String, Future<?>> timersFuture = new ConcurrentHashMap<>();
	private final HotHandle<TimerHandle> hotHandle = new HotHandle<>();

	public static @NotNull Timer create(@NotNull AppBase app) {
		return GenModule.createRedirectModule(Timer.class, app);
	}

	protected Timer(@NotNull AppBase app) {
		zeze = app.getZeze();
		if (zeze != null) // 只生成Redirect代码时zeze可能为null
			RegisterZezeTables(zeze);
	}

	static void register(@NotNull Class<? extends Serializable> cls) {
		beanFactory.register(cls);
	}

	public void loadCustomClassAnd() {
		nodeIdAutoKey = zeze.getAutoKey("Zeze.Component.Timer.NodeId");
		timerIdAutoKey = zeze.getAutoKey("Zeze.Component.Timer.TimerId");
		timerSerialId = zeze.getAutoKey("Zeze.Component.Timer.SerialId");
	}

	private boolean started = false;

	/**
	 * 非事务环境调用。用于启动Timer服务。
	 */
	public synchronized void start() {
		if (started)
			return;

		started = true;
		var hotManager = zeze.getHotManager();
		if (null != hotManager) {
			hotManager.addHotBeanFactory(this);
			beanFactory.registerWatch(this::tryRecordHotModule);
		}
		// Task.run(this::loadTimer, "Timer.loadTimer");
		loadTimer();
	}

	/**
	 * 初始化在线Timer。在线Timer需要ProviderApp。
	 *
	 * @param providerApp 参数
	 */
	public void initializeOnlineTimer(@NotNull ProviderApp providerApp) {
		ProviderImplement impl;
		//noinspection ConstantValue
		if (null != providerApp && null != (impl = providerApp.providerImplement)) {
			if (impl instanceof ProviderWithOnline)
				timerAccount = new TimerAccount(((ProviderWithOnline)impl).getOnline());
			else if (impl instanceof Zeze.Game.ProviderWithOnline)
				defaultOnline = ((Zeze.Game.ProviderWithOnline)impl).getOnline();
		}
	}

	/**
	 * 停止Timer服务。
	 */
	public synchronized void stop() {
		if (!started)
			return;
		started = false;
		var hotManager = zeze.getHotManager();
		if (null != hotManager) {
			hotManager.removeHotBeanFactory(this);
			beanFactory.unregisterWatch(this::tryRecordHotModule);
		}

		UnRegisterZezeTables(this.zeze);
	}

	TimerHandle findTimerHandle(String handleClassName) throws Exception {
		return hotHandle.findHandle(zeze, handleClassName);
	}

	/////////////////////////////////////////////////////////////////////////
	// Simple Timer
	// 调度一个Timer实例。
	// name为静态注册到这个模块的处理名字。
	// 相同的name可以调度多个timer实例。
	// @return 返回 TimerId。

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay      延迟
	 * @param period     间隔
	 * @param handle     Timer处理Class
	 * @param customData 自定义数据
	 * @return TimerId
	 */
	public @NotNull String schedule(long delay, long period, @NotNull Class<? extends TimerHandle> handle,
									@Nullable Bean customData) {
		return schedule(delay, period, -1, handle, customData);
	}

	/**
	 * 调度一个Timeout，即仅执行一次的Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay      延迟
	 * @param handle     Timer处理Class
	 * @param customData 自定义数据
	 * @return TimerId
	 */
	public @NotNull String schedule(long delay, @NotNull Class<? extends TimerHandle> handle,
									@Nullable Bean customData) {
		return schedule(delay, -1, 1, handle, customData);
	}

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay      延迟
	 * @param period     间隔
	 * @param times      最大触发次数
	 * @param handle     Timer处理Class
	 * @param customData 自定义数据
	 * @return TimerId
	 */
	public @NotNull String schedule(long delay, long period, long times, @NotNull Class<? extends TimerHandle> handle,
									@Nullable Bean customData) {
		return schedule(delay, period, times, -1, handle, customData);
	}

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay      延迟
	 * @param period     间隔
	 * @param times      最大触发次数
	 * @param endTime    结束时间
	 * @param handle     Timer处理Class
	 * @param customData 自定义数据
	 * @return TimerId
	 */
	public @NotNull String schedule(long delay, long period, long times, long endTime,
									@NotNull Class<? extends TimerHandle> handle, @Nullable Bean customData) {
		return schedule(delay, period, times, endTime, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay          延迟
	 * @param period         间隔
	 * @param times          最大触发次数
	 * @param endTime        结束时间
	 * @param missFirePolicy 错失触发策略
	 * @param handle         Timer处理Class
	 * @param customData     自定义数据
	 * @return TimerId
	 */
	public @NotNull String schedule(long delay, long period, long times, long endTime, int missFirePolicy,
									@NotNull Class<? extends TimerHandle> handle, @Nullable Bean customData) {
		return schedule(delay, period, times, endTime,
				missFirePolicy, handle, customData, "");
	}

	public @NotNull String schedule(long delay, long period, long times, long endTime, int missFirePolicy,
									@NotNull Class<? extends TimerHandle> handle, @Nullable Bean customData,
									String oneByOneKey) {
		var simpleTimer = new BSimpleTimer();
		initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		simpleTimer.setMissfirePolicy(missFirePolicy);
		return schedule(simpleTimer, handle, customData);
	}

	// 直接传递BSimpleTimer，需要自己调用它Timer.initSimpleTimer初始化。所以暂时不开放了。
	private @NotNull String schedule(@NotNull BSimpleTimer simpleTimer, @NotNull Class<? extends TimerHandle> handle,
									 @Nullable Bean customData) {
		// auto name
		return schedule("@" + timerIdAutoKey.nextString(), simpleTimer, handle, customData);
	}

	@NotNull String schedule(@NotNull String timerId, @NotNull BSimpleTimer simpleTimer,
							 @NotNull Class<? extends TimerHandle> handle, @Nullable Bean customData) {
		var serverId = zeze.getConfig().getServerId();
		var root = _tNodeRoot.getOrAdd(serverId);
		var nodeId = root.getHeadNodeId();
		if (nodeId == 0) { // no node
			nodeId = nodeIdAutoKey.nextId();
		}
		while (true) {
			var node = _tNodes.getOrAdd(nodeId);
			// 如果节点是新创建的，这里根据node的变量来判断。
			if (node.getNextNodeId() == 0 || node.getPrevNodeId() == 0) {

				if (root.getHeadNodeId() == 0 || root.getTailNodeId() == 0) {
					// root is empty
					node.setPrevNodeId(nodeId);
					node.setNextNodeId(nodeId);
					root.setHeadNodeId(nodeId);
					root.setTailNodeId(nodeId);
				} else {
					// link to root head
					var head = _tNodes.get(root.getHeadNodeId());
					if (null == head)
						throw new IllegalStateException("headNode is null. maybe operate before create.");
					head.setPrevNodeId(nodeId);
					node.setNextNodeId(root.getHeadNodeId());
					node.setPrevNodeId(root.getTailNodeId());
					root.setHeadNodeId(nodeId);
					var tail = _tNodes.get(root.getTailNodeId());
					if (null == tail)
						throw new IllegalStateException("tailNode is null. maybe operate before create.");
					tail.setNextNodeId(root.getHeadNodeId());
				}
			}

			if (node.getTimers().size() < CountPerNode) {
				var index = new BIndex();
				index.setServerId(serverId);
				index.setNodeId(nodeId);
				index.setSerialId(timerSerialId.nextId());
				_tIndexs.tryAdd(timerId, index);

				var timer = new BTimer();
				timer.setTimerName(timerId);
				timer.setHandleName(handle.getName());
				timer.setTimerObj(simpleTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					register(customData.getClass());
					timer.getCustomData().setBean(customData);
					tryRecordBeanHotModuleWhileCommit(customData);
				}

				scheduleSimple(index.getSerialId(), serverId, timerId,
						simpleTimer.getExpectedTime() - System.currentTimeMillis(),
						timer.getConcurrentFireSerialNo(),
						false, simpleTimer.getOneByOneKey());
				return timerId;
			}
			nodeId = nodeIdAutoKey.nextId();
		}
	}

	//////////////////////////////////////////////////////////////////////////////////
	// Cron Timer

	/**
	 * 每月第N(monthDay)天的某个时刻(hour,minute,second)。
	 * 需要在事务内使用。
	 *
	 * @param monthDay   月内第几天
	 * @param hour       小时
	 * @param minute     分钟
	 * @param second     秒
	 * @param handle     回调class
	 * @param customData 自定义数据
	 * @return TimerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public @NotNull String scheduleMonth(int monthDay, int hour, int minute, int second,
										 @NotNull Class<? extends TimerHandle> handle,
										 @Nullable Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " " + monthDay + " * ?";
		return schedule(cron, handle, customData);
	}

	/**
	 * 每周第N(weekDay)天的某个时刻(hour, minute, second)。
	 * 需要在事务内使用。
	 *
	 * @param weekDay    一周的第几天
	 * @param hour       小时
	 * @param minute     分钟
	 * @param second     秒
	 * @param handle     回调class
	 * @param customData 自定义数据
	 * @return TimerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public @NotNull String scheduleWeek(int weekDay, int hour, int minute, int second,
										@NotNull Class<? extends TimerHandle> handle,
										@Nullable Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " * * " + weekDay;
		return schedule(cron, handle, customData);
	}

	/**
	 * 每天的某个时刻(hour, minute, second)。
	 * 需要在事务内使用。
	 *
	 * @param hour       小时
	 * @param minute     分钟
	 * @param second     秒
	 * @param handle     回调class
	 * @param customData 自定义数据
	 * @return TimerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public @NotNull String scheduleDay(int hour, int minute, int second,
									   @NotNull Class<? extends TimerHandle> handle,
									   @Nullable Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " * * ?";
		return schedule(cron, handle, customData);
	}

	/**
	 * 调度一个根据Cron表达式描述的Timer。
	 * 需要在事务内使用。
	 *
	 * @param cronExpression cron 表达式
	 * @param handle         回调class
	 * @param customData     自定义数据
	 * @return TimerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public @NotNull String schedule(@NotNull String cronExpression,
									@NotNull Class<? extends TimerHandle> handle,
									@Nullable Bean customData) throws ParseException {
		return schedule(cronExpression, -1, -1, handle, customData);
	}

	/**
	 * 调度一个根据Cron表达式描述的Timer。
	 * 需要在事务内使用。
	 *
	 * @param cronExpression cron 表达式
	 * @param times          次数限制
	 * @param endTime        结束时间限制
	 * @param handle         回调class
	 * @param customData     自定义数据
	 * @return TimerId
	 * @throws ParseException cron解析异常
	 */
	public @NotNull String schedule(@NotNull String cronExpression, long times, long endTime,
									@NotNull Class<? extends TimerHandle> handle,
									@Nullable Bean customData) throws ParseException {
		return schedule(cronExpression, times, endTime, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个根据Cron表达式描述的Timer。
	 * 需要在事务内使用。
	 *
	 * @param cronExpression cron 表达式
	 * @param times          次数限制
	 * @param endTime        结束时间限制
	 * @param missFirePolicy 触发丢失处理策略
	 * @param handle         回调class
	 * @param customData     自定义数据
	 * @return TimerId
	 * @throws ParseException cron解析异常
	 */
	public @NotNull String schedule(@NotNull String cronExpression, long times, long endTime, int missFirePolicy,
									@NotNull Class<? extends TimerHandle> handle,
									@Nullable Bean customData) throws ParseException {
		return schedule(cronExpression, times, endTime,
				missFirePolicy, handle, customData, "");
	}

	public @NotNull String schedule(@NotNull String cronExpression, long times, long endTime, int missFirePolicy,
									@NotNull Class<? extends TimerHandle> handle,
									@Nullable Bean customData, String oneByOneKey) throws ParseException {
		var cronTimer = new BCronTimer();
		initCronTimer(cronTimer, cronExpression, times, endTime, oneByOneKey);
		cronTimer.setMissfirePolicy(missFirePolicy);
		return schedule(cronTimer, handle, customData);
	}

	// 直接传递BCronTimer需要自动调用Timer.initCronTimer初始化。先不开放了。
	private @NotNull String schedule(@NotNull BCronTimer cronTimer,
									 @NotNull Class<? extends TimerHandle> name, @Nullable Bean customData) {
		return schedule("@" + timerIdAutoKey.nextString(), cronTimer, name, customData);
	}

	@NotNull String schedule(@NotNull String timerId, BCronTimer cronTimer,
							 @NotNull Class<? extends TimerHandle> name, @Nullable Bean customData) {
		var serverId = zeze.getConfig().getServerId();
		var root = _tNodeRoot.getOrAdd(serverId);
		var nodeId = root.getHeadNodeId();
		if (nodeId == 0) { // no node
			nodeId = nodeIdAutoKey.nextId();
		}
		while (true) {
			var node = _tNodes.getOrAdd(nodeId);
			// 如果节点是新创建的，这里根据node的变量来判断。
			if (node.getNextNodeId() == 0 || node.getPrevNodeId() == 0) {

				if (root.getHeadNodeId() == 0 || root.getTailNodeId() == 0) {
					// root is empty
					node.setPrevNodeId(nodeId);
					node.setNextNodeId(nodeId);
					root.setHeadNodeId(nodeId);
					root.setTailNodeId(nodeId);
				} else {
					// link to root head
					var head = _tNodes.get(root.getHeadNodeId());
					if (null == head)
						throw new IllegalStateException("headNode is null. maybe operate before create.");
					head.setPrevNodeId(nodeId);
					node.setNextNodeId(root.getHeadNodeId());
					node.setPrevNodeId(root.getTailNodeId());
					root.setHeadNodeId(nodeId);
					var tail = _tNodes.get(root.getTailNodeId());
					if (null == tail)
						throw new IllegalStateException("tailNode is null. maybe operate before create.");
					tail.setNextNodeId(root.getHeadNodeId());
				}
			}

			if (node.getTimers().size() < CountPerNode) {
				var index = new BIndex();
				index.setServerId(serverId);
				index.setNodeId(nodeId);
				index.setSerialId(timerSerialId.nextId());
				_tIndexs.insert(timerId, index);

				var timer = new BTimer();
				timer.setTimerName(timerId);
				timer.setHandleName(name.getName());
				timer.setTimerObj(cronTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					register(customData.getClass());
					timer.getCustomData().setBean(customData);
					tryRecordBeanHotModuleWhileCommit(customData);
				}

				scheduleCron(index.getSerialId(), serverId,
						timerId, cronTimer,
						timer.getConcurrentFireSerialNo(),
						false, cronTimer.getOneByOneKey());
				return timerId;
			}
			nodeId = nodeIdAutoKey.nextId();
		}
	}

	/////////////////////////////////////////////////////////////////
	// Named Timer
	// 有名字的Timer，每个名字只能全局调度唯一一个真正的Timer。
	// 对这种Timer，不暴露TimerId，只能通过名字访问。
	// 当真正的Timer被迁移到不同的Server时，名字到TimerId的映射不需要改变。

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId    名字
	 * @param delay      延迟
	 * @param handle     回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(@NotNull String timerId, long delay,
								 @NotNull Class<? extends TimerHandle> handle, @Nullable Bean customData) {
		return scheduleNamed(timerId, delay, -1, -1, -1, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId    名字
	 * @param delay      延迟
	 * @param period     间隔
	 * @param handle     回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(@NotNull String timerId, long delay, long period,
								 @NotNull Class<? extends TimerHandle> handle, @Nullable Bean customData) {
		return scheduleNamed(timerId, delay, period, -1, -1, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId    名字
	 * @param delay      延迟
	 * @param period     间隔
	 * @param times      次数
	 * @param handle     回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(@NotNull String timerId, long delay, long period, long times,
								 @NotNull Class<? extends TimerHandle> handle, @Nullable Bean customData) {
		return scheduleNamed(timerId, delay, period, times, -1, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId        名字
	 * @param delay          延迟
	 * @param period         间隔
	 * @param times          次数
	 * @param endTime        结束时间
	 * @param missFirePolicy 触发丢失策略
	 * @param handle         回调class
	 * @param customData     自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(@NotNull String timerId, long delay, long period, long times, long endTime,
								 int missFirePolicy, @NotNull Class<? extends TimerHandle> handle,
								 @Nullable Bean customData) {
		return scheduleNamed(timerId, delay, period, times, endTime,
				missFirePolicy, handle, customData, "");
	}

	public boolean scheduleNamed(@NotNull String timerId, long delay, long period, long times, long endTime,
								 int missFirePolicy, @NotNull Class<? extends TimerHandle> handle,
								 @Nullable Bean customData, String oneByOneKey) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timer name. startsWith '@' is reserved.");

		var index = _tIndexs.get(timerId);
		if (null != index)
			return false;

		var simpleTimer = new BSimpleTimer();
		initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		simpleTimer.setMissfirePolicy(missFirePolicy);
		schedule(timerId, simpleTimer, handle, customData);
		return true;
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerName  名字，即TimerId
	 * @param cron       cron 表达式
	 * @param handle     回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 * @throws ParseException cron解析异常
	 */
	public boolean scheduleNamed(@NotNull String timerName, @NotNull String cron,
								 @NotNull Class<? extends TimerHandle> handle,
								 @Nullable Bean customData) throws ParseException {
		return scheduleNamed(timerName, cron, -1, -1, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerName  名字，即TimerId
	 * @param cron       cron 表达式
	 * @param times      次数
	 * @param endTime    结束时间
	 * @param handle     回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 * @throws ParseException cron解析异常
	 */
	public boolean scheduleNamed(@NotNull String timerName, @NotNull String cron, long times, long endTime,
								 @NotNull Class<? extends TimerHandle> handle,
								 @Nullable Bean customData) throws ParseException {
		return scheduleNamed(timerName, cron, times, endTime, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerName      名字，即TimerId
	 * @param cron           cron 表达式
	 * @param times          次数
	 * @param endTime        结束时间
	 * @param missFirePolicy 触发丢失策略
	 * @param handle         回调class
	 * @param customData     自定义数据
	 * @return 调度是否成功
	 * @throws ParseException cron解析异常
	 */
	public boolean scheduleNamed(@NotNull String timerName, @NotNull String cron, long times, long endTime,
								 int missFirePolicy, @NotNull Class<? extends TimerHandle> handle,
								 @Nullable Bean customData) throws ParseException {
		return scheduleNamed(timerName, cron, times, endTime,
				missFirePolicy, handle, customData, "");
	}

	public boolean scheduleNamed(@NotNull String timerName, @NotNull String cron, long times, long endTime,
								 int missFirePolicy, @NotNull Class<? extends TimerHandle> handle,
								 @Nullable Bean customData, String oneByOneKey) throws ParseException {
		var timerId = _tIndexs.get(timerName);
		if (null != timerId)
			return false;
		var cronTimer = new BCronTimer();
		initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		cronTimer.setMissfirePolicy(missFirePolicy);
		schedule(timerName, cronTimer, handle, customData);
		return true;
	}

	/**
	 * 取消一个具体的Timer实例。
	 * 需要在事务内调用。
	 *
	 * @param timerId timerId
	 */
	public void cancel(String timerId) {
		if (null == timerId)
			return; // 忽略没有初始化的timerId。

		/*
		try {
			// XXX 统一通过这里取消定时器，可能会浪费一次内存表查询。
			// 还是让账户相关Timer自己取消吧。
			if (null != timerRole && timerRole.cancel(timerId))
				return; // done

			if (null != timerAccount && timerAccount.cancel(timerId))
				return; // done

		} catch (Exception ex) {
			logger.error("ignore cancel error.", ex);
			return; // done;
		}
		*/
		var index = _tIndexs.get(timerId);
		if (null != index) {
			cancel(index.getServerId(), timerId, index, _tNodes.get(index.getNodeId()));
			// cancel future
			if (index.getServerId() != zeze.getConfig().getServerId())
				Transaction.whileCommit(() -> tryRedirectCancel(index.getServerId(), timerId));
		} else {
			// 定时器数据已经不存在了，尝试移除future。
			cancelFuture(timerId);
		}
	}

	///////////////////////////////////////////////////////////////////////////////
	// Online Timer
	@NotNull tAccountTimers tAccountTimers() {
		return _tAccountTimers;
	}

	@NotNull tIndexs tIndexs() {
		return _tIndexs;
	}

	@NotNull tAccountOfflineTimers tAccountOfflineTimers() {
		return _tAccountOfflineTimers;
	}

	private TimerAccount timerAccount;
	private Online defaultOnline;

	public TimerAccount getAccountTimer() {
		return timerAccount;
	}

	public Online getDefaultOnline() {
		return defaultOnline;
	}

	public TimerRole getRoleTimer() {
		return defaultOnline.getTimerRole();
	}

	public TimerRole getRoleTimer(String onlineSetName) {
		var online = defaultOnline.getOnline(onlineSetName);
		if (null == online)
			throw new IllegalStateException("online miss " + onlineSetName);
		return online.getTimerRole();
	}

	/////////////////////////////////////////////////////////////
	// 内部实现
	protected void tryRedirectCancel(int serverId, @NotNull String timerId) {
		// redirect 现在仅取消future，总是尝试，不检查其他参数。
		if (zeze.getConfig().getServerId() != serverId
				&& zeze.getProviderApp().providerDirectService.providerByServerId.containsKey(serverId)) {
			redirectCancel(serverId, timerId);
		}
	}

	@RedirectToServer
	protected void redirectCancel(int serverId, @NotNull String timerId) {
		// redirect 现在仅取消future，总是尝试，不检查其他参数。
		cancelFuture(timerId);
	}

	void cancelFuture(@NotNull String timerName) {
		var local = timersFuture.remove(timerName);
		if (null != local)
			local.cancel(false);
	}

	private void cancel(int serverId, @NotNull String timerName, @Nullable BIndex index, @Nullable BNode node) {
		// 事务成功时，总是尝试cancel future
		Transaction.whileCommit(() -> cancelFuture(timerName));

		if (null == index)
			return;

		// remove node
		if (null != node) {
			var timers = node.getTimers();
			timers.remove(timerName);

			if (timers.isEmpty()) {
				var prev = _tNodes.get(node.getPrevNodeId());
				var next = _tNodes.get(node.getNextNodeId());
				var root = _tNodeRoot.get(serverId);
				if (null == root || null == prev || null == next)
					throw new IllegalStateException("maybe operate before timer created.");
				if (root.getHeadNodeId() == root.getTailNodeId()) {
					// only one node and will be removed.
					root.setHeadNodeId(0L);
					root.setTailNodeId(0L);
				} else {
					if (root.getHeadNodeId() == index.getNodeId())
						root.setHeadNodeId(node.getNextNodeId());
					if (root.getTailNodeId() == index.getNodeId())
						root.setTailNodeId(node.getPrevNodeId());
				}
				prev.setNextNodeId(node.getNextNodeId());
				next.setPrevNodeId(node.getPrevNodeId());

				// 把当前空的Node加入垃圾回收。
				// 由于Nodes并发访问的原因，不能马上删除。延迟一定时间就安全了。
				// 不删除的话就会在数据库留下垃圾。
				_tNodes.delayRemove(index.getNodeId());
			}
		}
		_tIndexs.remove(timerName);
	}

	private void scheduleSimple(long timerSerialId, int serverId,
								@NotNull String timerId, long delay,
								long concurrentSerialNo,
								boolean putIfAbsent,
								String oneByOneKey) {
		Transaction.whileCommit(
				() -> {
					if (!putIfAbsent || !timersFuture.containsKey(timerId)) {
						timersFuture.put(timerId, Task.scheduleUnsafe(delay, () -> {
							if (null == oneByOneKey || oneByOneKey.isEmpty()) {
								fireSimple(timerSerialId, serverId, timerId, concurrentSerialNo, false);
							} else {
								zeze.getTaskOneByOneByKey().Execute(oneByOneKey,
										() -> fireSimple(timerSerialId, serverId, timerId, concurrentSerialNo, false));
							}
						}));
					}
				});
	}

	public static void initSimpleTimer(@NotNull BSimpleTimer simpleTimer,
									   long delay, long period, long times, long endTime,
									   String oneByOneKey) {
		if (delay < 0)
			throw new IllegalArgumentException("delay(" + delay + ") < 0");
		var now = System.currentTimeMillis();
		//timer.setDelay(delay);
		simpleTimer.setPeriod(period);
		//times == -1, this means Infinite number of times ----lwj
		simpleTimer.setRemainTimes(times);
		simpleTimer.setEndTime(endTime);
		long expectedTime = now + delay;
		simpleTimer.setExpectedTime(expectedTime);
		simpleTimer.setNextExpectedTime(expectedTime);
		simpleTimer.setStartTime(expectedTime);
		simpleTimer.setOneByOneKey(oneByOneKey);
	}

	public static boolean nextSimpleTimer(@NotNull BSimpleTimer simpleTimer, boolean missFire) {
		// check period
		if (simpleTimer.getPeriod() <= 0)
			return false;

		// check remain times
		if (simpleTimer.getRemainTimes() > 0) {
			simpleTimer.setRemainTimes(simpleTimer.getRemainTimes() - 1);
			if (simpleTimer.getRemainTimes() == 0)
				return false;
		}

		simpleTimer.setHappenTimes(simpleTimer.getHappenTimes() + 1);
		long now = System.currentTimeMillis();
		simpleTimer.setHappenTime(now);

		// 下面这段代码可以写的更简洁，但这样写，思路更清楚。
		if (missFire) {
			if (simpleTimer.getMissfirePolicy() == eMissfirePolicyRunOnce) {
				// 这种策略重置时间，定时器将在新的开始时间之后按原来的间隔执行。
				// simpleTimer.setStartTime(now);
				simpleTimer.setExpectedTime(now);
				simpleTimer.setNextExpectedTime(now + simpleTimer.getPeriod());
			} else {
				simpleTimer.setExpectedTime(simpleTimer.getNextExpectedTime());
				simpleTimer.setNextExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());
			}
		} else {
			simpleTimer.setExpectedTime(simpleTimer.getNextExpectedTime());
			simpleTimer.setNextExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());
		}

		// check endTime
		return simpleTimer.getEndTime() <= 0 || simpleTimer.getNextExpectedTime() <= simpleTimer.getEndTime();
	}

	private long fireSimple(long timerSerialId, int serverId, @NotNull String timerId, long concurrentSerialNo, boolean missFire) {
		if (0 != Task.call(zeze.newProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (null == index
					|| index.getServerId() != zeze.getConfig().getServerId() // 不是拥有者，取消本地调度，应该是不大可能发生的。
					|| index.getSerialId() != timerSerialId // 新注册的，旧的future需要取消。
			) {
				cancelFuture(timerId);
				return 0;
			}

			var node = _tNodes.get(index.getNodeId());
			if (null == node) {
				cancel(serverId, timerId, index, null);
				return 0; // procedure done
			}

			var timer = node.getTimers().get(timerId);
			if (null == timer)
				throw new IllegalStateException("maybe operate before timer created.");
			final var handle = findTimerHandle(timer.getHandleName());
			var simpleTimer = timer.getTimerObj_Zeze_Builtin_Timer_BSimpleTimer();
			if (concurrentSerialNo == timer.getConcurrentFireSerialNo()) {

				final var context = new TimerContext(this, timer, simpleTimer.getHappenTime(),
						simpleTimer.getNextExpectedTime(), simpleTimer.getExpectedTime());

				// 当调度发生了错误或者由于异步时序没有原子保证，导致同时（或某个瞬间）在多个Server进程调度时，
				// 这个系列号保证触发用户回调只会发生一次。这个并发问题不取消定时器，继续尝试调度（去争抢执行权）。
				// 定时器的调度生命期由其他地方保证最终一致。如果保证发生了错误，将一致并发争抢执行权。
				var serialSaved = index.getSerialId();
				var ret = Task.call(zeze.newProcedure(() -> {
					handle.onTimer(context);
					return 0;
				}, "Timer.fireSimpleUser"));

				var indexNew = _tIndexs.get(timerId);
				if (indexNew == null || indexNew.getSerialId() != serialSaved)
					return 0; // canceled or new timer.

				if (ret == Procedure.Exception) {
					// 用户处理不允许异常，其他错误记录忽略，日志已经记录。
					cancel(serverId, timerId, index, node);
					return 0; // procedure done
				}
				timer.setConcurrentFireSerialNo(concurrentSerialNo + 1);
				// 其他错误忽略

				// 准备下一个间隔
				if (!nextSimpleTimer(simpleTimer, missFire)) {
					cancel(serverId, timerId, index, node);
					return 0;
				}
			}
			// else 发生了并发执行争抢，也需要再次进行本地调度。此时直接使用simpleTimer中的值，不需要再次进行计算。

			// continue period
			long delay = simpleTimer.getNextExpectedTime() - System.currentTimeMillis();
			scheduleSimple(timerSerialId, serverId, timerId,
					delay, concurrentSerialNo + 1, false,
					simpleTimer.getOneByOneKey());
			return 0L;
		}, "Timer.fireSimple"))) {
			Task.call(zeze.newProcedure(() -> {
				cancel(timerId);
				return 0L;
			}, "Timer.cancelTimer"));
		}
		return 0L;
	}

	private void scheduleCron(long timerSerialId, int serverId,
							  @NotNull String timerName, @NotNull BCronTimer cron,
							  long concurrentSerialNo,
							  boolean putIfAbsent, String oneByOneKey) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(timerSerialId, serverId, timerName, delay, concurrentSerialNo, putIfAbsent, oneByOneKey);
		} catch (Exception ex) {
			// 这个错误是在不好处理。先只记录日志吧。
			logger.error("", ex);
		}
	}

	private void scheduleCronNext(long timerSerialId, int serverId,
								  @NotNull String timerName, long delay,
								  long concurrentSerialNo,
								  boolean putIfAbsent, String oneByOneKey) {
		Transaction.whileCommit(
				() -> {
					if (!putIfAbsent || !timersFuture.containsKey(timerName)) {
						timersFuture.put(timerName, Task.scheduleUnsafe(delay, () -> {
							if (null == oneByOneKey || oneByOneKey.isEmpty()) {
								fireCron(timerSerialId, serverId, timerName, concurrentSerialNo, false);
							} else {
								zeze.getTaskOneByOneByKey().Execute(oneByOneKey,
										() -> fireCron(timerSerialId, serverId, timerName, concurrentSerialNo, false));
							}
						}));
					}
				});
	}

	public static long cronNextTime(@NotNull String cron, long time) throws ParseException {
		var cronExpression = new CronExpression(cron);
		return cronExpression.getNextValidTimeAfter(new Date(time)).getTime();
	}

	public static void initCronTimer(@NotNull BCronTimer cronTimer,
									 @NotNull String cron, long times, long endTime,
									 String oneByOneKey)
			throws ParseException {
		cronTimer.setCronExpression(cron);
		long expectedTime = cronNextTime(cron, System.currentTimeMillis());
		cronTimer.setNextExpectedTime(expectedTime);
		cronTimer.setRemainTimes(times);
		cronTimer.setEndTime(endTime);
		cronTimer.setOneByOneKey(oneByOneKey);
	}

	public static boolean nextCronTimer(@NotNull BCronTimer cronTimer, boolean missFire) throws ParseException {
		// check remain times
		if (cronTimer.getRemainTimes() > 0) {
			cronTimer.setRemainTimes(cronTimer.getRemainTimes() - 1);
			if (cronTimer.getRemainTimes() == 0)
				return false;
		}

		var now = System.currentTimeMillis();
		cronTimer.setHappenTime(now);

		// 下面这段代码可以写的更简洁，但这样写，思路更清楚。
		if (missFire) {
			if (cronTimer.getMissfirePolicy() == eMissfirePolicyRunOnce) {
				// 这种策略重置时间，定时器将在新的开始时间之后按原来的间隔执行。
				// cronTimer.setStartTime(now);
				cronTimer.setExpectedTime(now);
				cronTimer.setNextExpectedTime(cronNextTime(cronTimer.getCronExpression(), now));
			} else {
				cronTimer.setExpectedTime(cronTimer.getNextExpectedTime());
				cronTimer.setNextExpectedTime(cronNextTime(cronTimer.getCronExpression(), cronTimer.getExpectedTime()));
			}
		} else {
			cronTimer.setExpectedTime(cronTimer.getNextExpectedTime());
			cronTimer.setNextExpectedTime(cronNextTime(cronTimer.getCronExpression(), cronTimer.getExpectedTime()));
		}

		// check endTime
		return cronTimer.getEndTime() <= 0 || cronTimer.getNextExpectedTime() <= cronTimer.getEndTime();
	}

	private void fireCron(long timerSerialId, int serverId, @NotNull String timerId, long concurrentSerialNo, boolean missFire) {
		if (0 != Task.call(zeze.newProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (null == index
					|| index.getServerId() != zeze.getConfig().getServerId() // 不是拥有者，取消本地调度，应该是不大可能发生的。
					|| index.getSerialId() != timerSerialId // 新注册的，旧的future需要取消。
			) {
				cancelFuture(timerId);
				return 0;
			}

			var node = _tNodes.get(index.getNodeId());
			if (null == node) {
				// maybe concurrent cancel
				cancel(serverId, timerId, index, null);
				return 0; // procedure done
			}
			var timer = node.getTimers().get(timerId);
			if (null == timer)
				throw new IllegalStateException("maybe operate before timer created.");
			final var handle = findTimerHandle(timer.getHandleName());
			var cronTimer = timer.getTimerObj_Zeze_Builtin_Timer_BCronTimer();
			if (concurrentSerialNo == timer.getConcurrentFireSerialNo()) {
				final var context = new TimerContext(this, timer, cronTimer.getHappenTime(),
						cronTimer.getNextExpectedTime(), cronTimer.getExpectedTime());

				// 当调度发生了错误或者由于异步时序没有原子保证，导致同时（或某个瞬间）在多个Server进程调度时，
				// 这个系列号保证触发用户回调只会发生一次。这个并发问题不取消定时器，继续尝试调度（去争抢执行权）。
				// 定时器的调度生命期由其他地方保证最终一致。如果保证发生了错误，将一致并发争抢执行权。
				var serialSaved = index.getSerialId();
				var ret = Task.call(zeze.newProcedure(() -> {
					handle.onTimer(context);
					return 0;
				}, "Timer.fireCronUser"));

				var indexNew = _tIndexs.get(timerId);
				if (indexNew == null || indexNew.getSerialId() != serialSaved)
					return 0; // canceled or new timer.

				if (ret == Procedure.Exception) {
					// 用户处理不允许异常，其他错误记录忽略，日志已经记录。
					cancel(serverId, timerId, index, node);
					return 0; // procedure done
				}
				timer.setConcurrentFireSerialNo(concurrentSerialNo + 1);

				if (!Timer.nextCronTimer(cronTimer, missFire)) {
					cancel(serverId, timerId, index, node);
					return 0; // procedure done
				}
			}
			// else 发生了并发执行争抢，也需要再次进行本地调度。此时直接使用cronTimer中的值，不需要再次进行计算。

			// continue period
			long delay = cronTimer.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(timerSerialId, serverId, timerId,
					delay, concurrentSerialNo + 1,
					false, cronTimer.getOneByOneKey());
			return 0L; // procedure done
		}, "Timer.fireCron"))) {
			Task.call(zeze.newProcedure(() -> {
				cancel(timerId);
				return 0L;
			}, "Timer.cancelTimer"));
		}
	}

	private void loadTimer() {
		var serverId = zeze.getConfig().getServerId();
		final var out = new OutObject<BNodeRoot>();
		var r = Task.call(zeze.newProcedure(() -> {
			var root = _tNodeRoot.getOrAdd(serverId);
			// 本地每次load都递增。用来处理和接管的并发。
			root.setLoadSerialNo(root.getLoadSerialNo() + 1);
			out.value = root.copy();
			return 0L;
		}, "Timer.loadTimerLocal"));
		if (r == Procedure.Success) {
			var root = out.value;
			var offlineNotify = new BOfflineNotify();
			offlineNotify.serverId = zeze.getConfig().getServerId();
			offlineNotify.notifySerialId = root.getLoadSerialNo();
			offlineNotify.notifyId = "Zeze.Component.Timer.OfflineNotify";
			zeze.getServiceManager().offlineRegister(offlineNotify,
					(notify) -> spliceLoadTimer(notify.serverId, notify.notifySerialId));
			loadTimer(root.getHeadNodeId(), root.getHeadNodeId(), serverId);
		} else
			logger.error("loadTimer failed: r={}", r);
	}

	/**
	 * 收到接管通知的服务器调用这个函数进行接管处理。
	 *
	 * @param serverId 需要接管的服务器Id
	 * @return 事务执行结果. 0表示成功
	 */
	private long spliceLoadTimer(int serverId, long loadSerialNo) {
		if (serverId == zeze.getConfig().getServerId())
			return 0; // skip self

		final var first = new OutLong();
		final var last = new OutLong();

		var result = Task.call(zeze.newProcedure(() -> {
			// 当接管别的服务器的定时器时，有可能那台服务器有新的CustomData，这个时候重新加载一次。
			var src = _tNodeRoot.get(serverId);
			if (null == src || src.getHeadNodeId() == 0 || src.getTailNodeId() == 0)
				return 0L; // nothing need to do.

			if (src.getLoadSerialNo() != loadSerialNo)
				return 0L; // 需要接管的机器已经活过来了。

			// prepare splice
			var root = _tNodeRoot.getOrAdd(zeze.getConfig().getServerId());
			var srcHead = _tNodes.get(src.getHeadNodeId());
			var srcTail = _tNodes.get(src.getTailNodeId());
			var head = _tNodes.get(root.getHeadNodeId());
			var tail = _tNodes.get(root.getTailNodeId());

			if (null == srcHead || null == srcTail)
				throw new IllegalStateException("maybe operate before timer created.");

			if (head == null || tail == null) {
				root.setHeadNodeId(src.getHeadNodeId());
				root.setTailNodeId(src.getTailNodeId());
			}

			// 先保存存储过程退出以后需要装载的timer范围。
			first.value = src.getHeadNodeId();
			last.value = root.getHeadNodeId();
			// splice
			srcTail.setNextNodeId(root.getHeadNodeId());
			root.setHeadNodeId(src.getHeadNodeId());
			if (head != null)
				head.setPrevNodeId(src.getTailNodeId());
			if (tail != null)
				tail.setNextNodeId(src.getHeadNodeId());
			srcHead.setPrevNodeId(root.getTailNodeId());
			// clear src
			src.setHeadNodeId(0L);
			src.setTailNodeId(0L);
			return 0L;
		}, "Timer.spliceAndLoadTimerLocal"));

		if (0L == result) {
			return loadTimer(first.value, last.value, serverId);
		}
		return result;
	}

	// 如果存在node，至少执行一次循环。
	private long loadTimer(long first, long last, int serverId) {
		if (first == 0 && last == 0)
			return 0;
		var idSet = new LongHashSet();
		var node = new OutLong(first);
		do {
			var nodeId = node.value;
			if (!idSet.add(nodeId)) // 检测并避免死循环
				break;
			// skip error. 使用node返回的值决定是否继续循环。
			var r = Task.call(zeze.newProcedure(() -> loadTimer(node, last, serverId), "Timer.loadTimer"));
			if (r != Procedure.Success) {
				logger.error("loadTimer failed: r={}, nodeId={}", r, nodeId);
				try {
					//noinspection BusyWait
					Thread.sleep(1000); // 避免因FastErrorPeriod导致过于频繁的事务失败
				} catch (InterruptedException ignored) {
				}
			}
		} while (node.value != last);
		return 0L;
	}

	private long loadTimer(@NotNull OutLong first, long last, int serverId) throws ParseException {
		var node = _tNodes.get(first.value);
		if (null == node) {
			logger.warn("loadTimer not found nodeId={}", first.value);
			first.value = last; // 马上结束外面的循环。last仅用在这里。
			return 0; // when root is empty。no node。skip error.
		}
		// BUG 修复，如果first.value直接设置，在发生redo时，当前node会被跳过。
		// 这是因为first是in&out的。另一个解决办法是，first改成只out，当前node用另一个值参数传入。
		Transaction.whileCommit(() -> {
			first.value = node.getNextNodeId(); // 设置下一个node。
		});
		Transaction.whileRollback(() -> {
			first.value = node.getNextNodeId(); // 设置下一个node。
		});

		var now = System.currentTimeMillis();
		for (var timer : node.getTimers().values()) {
			var index = _tIndexs.get(timer.getTimerName());
			if (index == null)
				continue;

			// 优化不能用Config.getServerId整体判断，因为load中断会导致传入的serverId就是当前Config的，
			// 这回导致load中断后，部分数据没有被设置正确的serverId。
			// 需要提前到schedule之前，后面的schedule会判断这个值。
			if (serverId != index.getServerId()) {
				index.setServerId(serverId);
			}
			if (timer.getTimerObj().getBean().typeId() == BSimpleTimer.TYPEID) {
				var simpleTimer = (BSimpleTimer)timer.getTimerObj().getBean();
				if (simpleTimer.getNextExpectedTime() < now) { // missFire found
					switch (simpleTimer.getMissfirePolicy()) {
					case eMissfirePolicyRunOnce:
					case eMissfirePolicyRunOnceOldNext:
						Task.run(() -> fireSimple(
								index.getSerialId(),
								serverId, timer.getTimerName(),
								timer.getConcurrentFireSerialNo(), true), "Timer.missFireSimple");
						continue; // loop done, continue

					case eMissfirePolicyNothing:
						// 重置启动时间，调度下一个（未来）间隔的时间。没有考虑对齐。
						simpleTimer.setNextExpectedTime(System.currentTimeMillis() + simpleTimer.getPeriod());
						break;

					default:
						throw new UnsupportedOperationException("Unknown MissFirePolicy: " + simpleTimer.getMissfirePolicy());
					}
				}
				scheduleSimple(
						index.getSerialId(),
						serverId, timer.getTimerName(),
						simpleTimer.getNextExpectedTime() - System.currentTimeMillis(),
						timer.getConcurrentFireSerialNo(),
						true, simpleTimer.getOneByOneKey());
			} else {
				var cronTimer = (BCronTimer)timer.getTimerObj().getBean();
				if (cronTimer.getNextExpectedTime() < now) {
					switch (cronTimer.getMissfirePolicy()) {
					case eMissfirePolicyRunOnce:
					case eMissfirePolicyRunOnceOldNext:
						Task.run(() -> fireCron(
								index.getSerialId(),
								serverId, timer.getTimerName(),
								timer.getConcurrentFireSerialNo(), true), "Timer.missFireCron");
						continue; // loop done, continue

					case eMissfirePolicyNothing:
						// 计算下一次（未来）发生的时间。
						cronTimer.setNextExpectedTime(cronNextTime(cronTimer.getCronExpression(), System.currentTimeMillis()));
						break;

					default:
						throw new UnsupportedOperationException("Unknown MissFirePolicy: " + cronTimer.getMissfirePolicy());
					}
				}
				scheduleCron(
						index.getSerialId(),
						serverId, timer.getTimerName(), cronTimer,
						timer.getConcurrentFireSerialNo(),
						true, cronTimer.getOneByOneKey());
			}
		}
		return 0L;
	}

	@Override
	public void clearTableCache() {
		_tNodes.__ClearTableCacheUnsafe__();
	}

	private final ConcurrentHashSet<HotModule> hotModulesHaveDynamic = new ConcurrentHashSet<>();
	private boolean freshStopModuleDynamic = false;

	private void onHotModuleStop(HotModule hot) {
		freshStopModuleDynamic |= hotModulesHaveDynamic.remove(hot) != null;
	}

	void tryRecordBeanHotModuleWhileCommit(Bean customData) {
		var cl = customData.getClass().getClassLoader();
		if (HotManager.isHotModule(cl)) {
			var hotModule = (HotModule)cl;
			Transaction.whileCommit(() -> {
				hotModule.stopEvents.add(this::onHotModuleStop);
				hotModulesHaveDynamic.add(hotModule);
			});
		}
	}

	private void tryRecordHotModule(Class<?> customClass) {
		var cl = customClass.getClassLoader();
		if (HotManager.isHotModule(cl)) {
			var hotModule = (HotModule)cl;
			hotModule.stopEvents.add(this::onHotModuleStop);
			hotModulesHaveDynamic.add(hotModule);
		}
	}

	@Override
	public BeanFactory beanFactory() {
		return beanFactory;
	}

	@Override
	public boolean hasFreshStopModuleDynamicOnce() {
		var tmp = freshStopModuleDynamic;
		freshStopModuleDynamic = false;
		return tmp;
	}

	@Override
	public void processWithNewClasses(List<Class<?>> newClasses) {
		for (var cls : newClasses) {
			tryRecordHotModule(cls);
		}
	}
}
