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
import Zeze.Builtin.Timer.tNodes;
import Zeze.Collections.BeanFactory;
import Zeze.Game.Online;
import Zeze.Hot.HotBeanFactory;
import Zeze.Hot.HotHandle;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotModule;
import Zeze.Serialize.Serializable;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.AnnounceServers;
import Zeze.Services.ServiceManager.BOfflineNotify;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.LongHashSet;
import Zeze.Util.OutLong;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import Zeze.Util.TransactionLevelAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.CronExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Timer extends AbstractTimer implements HotBeanFactory {
	static final Logger logger = LogManager.getLogger(Timer.class);
	private static int CountPerNode = Reflect.inDebugMode ? 1 : 10; // 调试状态下减少timer之间的影响,以免频繁redo
	private static final BeanFactory beanFactory = new BeanFactory();

	public static void setCountPerNode(int countPerNode) {
		CountPerNode = Math.max(countPerNode, 1);
	}

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

	public final Application zeze;
	private AutoKey nodeIdAutoKey;
	AutoKey timerIdAutoKey;
	AutoKey timerSerialId;

	// 在这台服务器进程内调度的所有Timer。key是timerId，value是ThreadPool.schedule的返回值。
	final ConcurrentHashMap<String, Future<?>> timerFutures = new ConcurrentHashMap<>();
	private final HotHandle<TimerHandle> hotHandle = new HotHandle<>();
	private boolean started;

	private TimerAccount timerAccount;
	private Online defaultOnline;

	private final ConcurrentHashSet<HotModule> hotModulesHaveDynamic = new ConcurrentHashSet<>();
	private boolean freshStopModuleDynamic;

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

	/**
	 * 非事务环境调用。用于启动Timer服务。
	 */
	public void start() throws Exception {
		lock();
		try {
			if (started)
				return;
			started = true;
			var hotManager = zeze.getHotManager();
			if (hotManager != null) {
				hotManager.addHotBeanFactory(this);
				beanFactory.registerWatch(this::tryRecordHotModule);
			}
			// Task.run(this::loadTimer, "Timer.loadTimer");
			loadTimer();
		} finally {
			unlock();
		}
	}

	/**
	 * 初始化在线Timer。在线Timer需要ProviderApp。
	 *
	 * @param providerApp 参数
	 */
	public void initializeOnlineTimer(@NotNull ProviderApp providerApp) {
		ProviderImplement impl;
		//noinspection ConstantValue
		if (providerApp != null && (impl = providerApp.providerImplement) != null) {
			if (impl instanceof ProviderWithOnline)
				timerAccount = new TimerAccount(((ProviderWithOnline)impl).getOnline());
			else if (impl instanceof Zeze.Game.ProviderWithOnline)
				defaultOnline = ((Zeze.Game.ProviderWithOnline)impl).getOnline();
		}
	}

	/**
	 * 停止Timer服务。
	 */
	public void stop() {
		lock();
		try {
			if (!started)
				return;
			started = false;
			var hotManager = zeze.getHotManager();
			if (hotManager != null) {
				hotManager.removeHotBeanFactory(this);
				beanFactory.unregisterWatch(this::tryRecordHotModule);
			}

			UnRegisterZezeTables(this.zeze);
		} finally {
			unlock();
		}
	}

	@NotNull TimerHandle findTimerHandle(@NotNull String handleClassName) throws ReflectiveOperationException {
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
	 * @param delay       首次触发延迟(毫秒), 不能小于0
	 * @param period      触发周期(毫秒), 只有大于0才会周期触发
	 * @param handleClass Timer处理Class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 */
	public @NotNull String schedule(long delay, long period, @NotNull Class<? extends TimerHandle> handleClass,
									@Nullable Bean customData) {
		return schedule(delay, period, -1, handleClass, customData);
	}

	/**
	 * 调度一个Timeout，即仅执行一次的Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay       首次触发延迟(毫秒), 不能小于0
	 * @param handleClass Timer处理Class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 */
	public @NotNull String schedule(long delay, @NotNull Class<? extends TimerHandle> handleClass,
									@Nullable Bean customData) {
		return schedule(delay, -1, 1, handleClass, customData);
	}

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay       首次触发延迟(毫秒), 不能小于0
	 * @param period      触发周期(毫秒), 只有大于0才会周期触发
	 * @param times       限制触发次数, -1表示不限次数
	 * @param handleClass Timer处理Class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 */
	public @NotNull String schedule(long delay, long period, long times,
									@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return schedule(delay, period, times, -1, handleClass, customData);
	}

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay       首次触发延迟(毫秒), 不能小于0
	 * @param period      触发周期(毫秒), 只有大于0才会周期触发
	 * @param times       限制触发次数, -1表示不限次数
	 * @param endTime     限制触发的最后时间(unix毫秒时间戳)
	 * @param handleClass Timer处理Class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 */
	public @NotNull String schedule(long delay, long period, long times, long endTime,
									@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return schedule(delay, period, times, endTime, eMissfirePolicyNothing, handleClass, customData);
	}

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay          首次触发延迟(毫秒), 不能小于0
	 * @param period         触发周期(毫秒), 只有大于0才会周期触发
	 * @param times          限制触发次数, -1表示不限次数
	 * @param endTime        限制触发的最后时间(unix毫秒时间戳)
	 * @param missfirePolicy 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
	 * @param handleClass    Timer处理Class
	 * @param customData     自定义数据
	 * @return 自动生成的timerId
	 */
	public @NotNull String schedule(long delay, long period, long times, long endTime, int missfirePolicy,
									@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return schedule(delay, period, times, endTime, missfirePolicy, handleClass, customData, "");
	}

	public @NotNull String schedule(long delay, long period, long times, long endTime, int missfirePolicy,
									@NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
									@NotNull String oneByOneKey) {
		var simpleTimer = new BSimpleTimer();
		initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		simpleTimer.setMissfirePolicy(missfirePolicy);
		return schedule(simpleTimer, handleClass, customData);
	}

	// 直接传递BSimpleTimer，需要自己调用它Timer.initSimpleTimer初始化。所以暂时不开放了。
	private @NotNull String schedule(@NotNull BSimpleTimer simpleTimer,
									 @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		var timeId = '@' + timerIdAutoKey.nextString();
		schedule(timeId, simpleTimer, handleClass, customData);
		return timeId;
	}

	void schedule(@NotNull String timerId, @NotNull BSimpleTimer simpleTimer,
				  @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		var serverId = zeze.getConfig().getServerId();
		var appVer = zeze.getConfig().getAppVersion();
		var root = _tNodeRoot.getOrAdd(serverId);
		if (root.getVersion() < appVer)
			root.setVersion(appVer);
		var nodeId = root.getHeadNodeId();
		if (nodeId == 0) // 如果整个双链表是空的
			nodeId = nodeIdAutoKey.nextId();
		for (; ; nodeId = nodeIdAutoKey.nextId()) {
			var node = _tNodes.getOrAdd(nodeId);
			if (node.getNextNodeId() == 0 || node.getPrevNodeId() == 0) { // 如果节点是新创建的
				var headNodeId = root.getHeadNodeId();
				var tailNodeId = root.getTailNodeId();
				if (headNodeId == 0 || tailNodeId == 0) { // 如果整个双链表是空的
					node.setPrevNodeId(nodeId);
					node.setNextNodeId(nodeId);
					root.setTailNodeId(nodeId);
				} else { // 双链表不空,插入新节点
					var head = _tNodes.get(headNodeId);
					if (head == null)
						throw new IllegalStateException("headNode is null. maybe operate before create.");
					var tail = _tNodes.get(tailNodeId);
					if (tail == null)
						throw new IllegalStateException("tailNode is null. maybe operate before create.");
					node.setPrevNodeId(tailNodeId);
					node.setNextNodeId(headNodeId);
					head.setPrevNodeId(nodeId);
					tail.setNextNodeId(nodeId);
				}
				root.setHeadNodeId(nodeId);
			}

			if (node.getTimers().size() < CountPerNode) {
				var serialId = timerSerialId.nextId();
				_tIndexs.insert(timerId, new BIndex(serverId, nodeId, serialId, appVer));

				var timer = new BTimer(timerId, handleClass.getName(), 0);
				timer.setTimerObj(simpleTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					register(customData.getClass());
					timer.getCustomData().setBean(customData);
					tryRecordBeanHotModuleWhileCommit(customData);
				}

				scheduleSimple(serialId, serverId, timerId,
						simpleTimer.getNextExpectedTime() - System.currentTimeMillis(),
						0, false, simpleTimer.getOneByOneKey());
				return;
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////////
	// Cron Timer

	/**
	 * 每月第N(monthDay)天的某个时刻(hour,minute,second)。
	 * 需要在事务内使用。
	 *
	 * @param monthDay    月内第几天
	 * @param hour        小时
	 * @param minute      分钟
	 * @param second      秒
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public @NotNull String scheduleMonth(int monthDay, int hour, int minute, int second,
										 @NotNull Class<? extends TimerHandle> handleClass,
										 @Nullable Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " " + monthDay + " * ?";
		return schedule(cron, handleClass, customData);
	}

	/**
	 * 每周第N(weekDay)天的某个时刻(hour, minute, second)。
	 * 需要在事务内使用。
	 *
	 * @param weekDay     一周的第几天
	 * @param hour        小时
	 * @param minute      分钟
	 * @param second      秒
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public @NotNull String scheduleWeek(int weekDay, int hour, int minute, int second,
										@NotNull Class<? extends TimerHandle> handleClass,
										@Nullable Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " * * " + weekDay;
		return schedule(cron, handleClass, customData);
	}

	/**
	 * 每天的某个时刻(hour, minute, second)。
	 * 需要在事务内使用。
	 *
	 * @param hour        小时
	 * @param minute      分钟
	 * @param second      秒
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public @NotNull String scheduleDay(int hour, int minute, int second,
									   @NotNull Class<? extends TimerHandle> handleClass,
									   @Nullable Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " * * ?";
		return schedule(cron, handleClass, customData);
	}

	/**
	 * 调度一个根据Cron表达式描述的Timer。
	 * 需要在事务内使用。
	 *
	 * @param cronExpression cron表达式
	 * @param handleClass    回调class
	 * @param customData     自定义数据
	 * @return 自动生成的timerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public @NotNull String schedule(@NotNull String cronExpression,
									@NotNull Class<? extends TimerHandle> handleClass,
									@Nullable Bean customData) throws ParseException {
		return schedule(cronExpression, -1, -1, handleClass, customData);
	}

	/**
	 * 调度一个根据Cron表达式描述的Timer。
	 * 需要在事务内使用。
	 *
	 * @param cronExpression cron表达式
	 * @param times          限制触发次数, -1表示不限次数
	 * @param endTime        限制触发的最后时间(unix毫秒时间戳)
	 * @param handleClass    回调class
	 * @param customData     自定义数据
	 * @return 自动生成的timerId
	 * @throws ParseException cron解析异常
	 */
	public @NotNull String schedule(@NotNull String cronExpression, long times, long endTime,
									@NotNull Class<? extends TimerHandle> handleClass,
									@Nullable Bean customData) throws ParseException {
		return schedule(cronExpression, times, endTime, eMissfirePolicyNothing, handleClass, customData);
	}

	/**
	 * 调度一个根据Cron表达式描述的Timer。
	 * 需要在事务内使用。
	 *
	 * @param cronExpression cron表达式
	 * @param times          限制触发次数, -1表示不限次数
	 * @param endTime        限制触发的最后时间(unix毫秒时间戳)
	 * @param missfirePolicy 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
	 * @param handleClass    回调class
	 * @param customData     自定义数据
	 * @return 自动生成的timerId
	 * @throws ParseException cron解析异常
	 */
	public @NotNull String schedule(@NotNull String cronExpression, long times, long endTime, int missfirePolicy,
									@NotNull Class<? extends TimerHandle> handleClass,
									@Nullable Bean customData) throws ParseException {
		return schedule(cronExpression, times, endTime,
				missfirePolicy, handleClass, customData, "");
	}

	public @NotNull String schedule(@NotNull String cronExpression, long times, long endTime, int missfirePolicy,
									@NotNull Class<? extends TimerHandle> handleClass,
									@Nullable Bean customData, @NotNull String oneByOneKey) throws ParseException {
		var cronTimer = new BCronTimer();
		initCronTimer(cronTimer, cronExpression, times, endTime, oneByOneKey);
		cronTimer.setMissfirePolicy(missfirePolicy);
		return schedule(cronTimer, handleClass, customData);
	}

	// 直接传递BCronTimer需要自动调用Timer.initCronTimer初始化。先不开放了。
	private @NotNull String schedule(@NotNull BCronTimer cronTimer,
									 @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		var timerId = '@' + timerIdAutoKey.nextString();
		schedule(timerId, cronTimer, handleClass, customData);
		return timerId;
	}

	void schedule(@NotNull String timerId, BCronTimer cronTimer, @NotNull Class<? extends TimerHandle> handleClass,
				  @Nullable Bean customData) {
		var serverId = zeze.getConfig().getServerId();
		var appVer = zeze.getConfig().getAppVersion();
		var root = _tNodeRoot.getOrAdd(serverId);
		if (root.getVersion() < appVer)
			root.setVersion(appVer);
		var nodeId = root.getHeadNodeId();
		if (nodeId == 0) // 如果整个双链表是空的
			nodeId = nodeIdAutoKey.nextId();
		for (; ; nodeId = nodeIdAutoKey.nextId()) {
			var node = _tNodes.getOrAdd(nodeId);
			if (node.getNextNodeId() == 0 || node.getPrevNodeId() == 0) { // 如果节点是新创建的
				var headNodeId = root.getHeadNodeId();
				var tailNodeId = root.getTailNodeId();
				if (headNodeId == 0 || tailNodeId == 0) { // 如果整个双链表是空的
					node.setPrevNodeId(nodeId);
					node.setNextNodeId(nodeId);
					root.setTailNodeId(nodeId);
				} else { // 双链表不空,插入新节点
					var head = _tNodes.get(headNodeId);
					if (head == null)
						throw new IllegalStateException("headNode is null. maybe operate before create.");
					var tail = _tNodes.get(tailNodeId);
					if (tail == null)
						throw new IllegalStateException("tailNode is null. maybe operate before create.");
					node.setPrevNodeId(tailNodeId);
					node.setNextNodeId(headNodeId);
					head.setPrevNodeId(nodeId);
					tail.setNextNodeId(nodeId);
				}
				root.setHeadNodeId(nodeId);
			}

			if (node.getTimers().size() < CountPerNode) {
				var serialId = timerSerialId.nextId();
				_tIndexs.insert(timerId, new BIndex(serverId, nodeId, serialId, appVer));

				var timer = new BTimer(timerId, handleClass.getName(), 0);
				timer.setTimerObj(cronTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					register(customData.getClass());
					timer.getCustomData().setBean(customData);
					tryRecordBeanHotModuleWhileCommit(customData);
				}

				scheduleCron(serialId, serverId, timerId, cronTimer, 0, false, cronTimer.getOneByOneKey());
				return;
			}
		}
	}

	/////////////////////////////////////////////////////////////////
	// Named Timer
	// 有名字的Timer，每个名字(TimerId)只能全局调度唯一一个真正的Timer。

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId     名字, 如果同名timer已存在则直接返回false
	 * @param delay       首次触发延迟(毫秒), 不能小于0
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(@NotNull String timerId, long delay,
								 @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return scheduleNamed(timerId, delay, -1, -1, -1, eMissfirePolicyNothing, handleClass, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId     名字, 如果同名timer已存在则直接返回false
	 * @param delay       首次触发延迟(毫秒), 不能小于0
	 * @param period      触发周期(毫秒), 只有大于0才会周期触发
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(@NotNull String timerId, long delay, long period,
								 @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return scheduleNamed(timerId, delay, period, -1, -1, eMissfirePolicyNothing, handleClass, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId     名字, 如果同名timer已存在则直接返回false
	 * @param delay       首次触发延迟(毫秒), 不能小于0
	 * @param period      触发周期(毫秒), 只有大于0才会周期触发
	 * @param times       限制触发次数, -1表示不限次数
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(@NotNull String timerId, long delay, long period, long times,
								 @NotNull Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return scheduleNamed(timerId, delay, period, times, -1, eMissfirePolicyNothing, handleClass, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId        名字, 如果同名timer已存在则直接返回false
	 * @param delay          首次触发延迟(毫秒), 不能小于0
	 * @param period         触发周期(毫秒), 只有大于0才会周期触发
	 * @param times          限制触发次数, -1表示不限次数
	 * @param endTime        限制触发的最后时间(unix毫秒时间戳)
	 * @param missfirePolicy 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
	 * @param handleClass    回调class
	 * @param customData     自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(@NotNull String timerId, long delay, long period, long times, long endTime,
								 int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData) {
		return scheduleNamed(timerId, delay, period, times, endTime, missfirePolicy, handleClass, customData, "");
	}

	public boolean scheduleNamed(@NotNull String timerId, long delay, long period, long times, long endTime,
								 int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData, @NotNull String oneByOneKey) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		var index = _tIndexs.get(timerId);
		if (index != null) {
			if (index.getServerId() != zeze.getConfig().getServerId())
				return false; // 已经被其它gs调度
			cancel(timerId); // 先取消,下面再重建
		}

		var simpleTimer = new BSimpleTimer();
		initSimpleTimer(simpleTimer, delay, period, times, endTime, oneByOneKey);
		simpleTimer.setMissfirePolicy(missfirePolicy);
		schedule(timerId, simpleTimer, handleClass, customData);
		return true;
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId     如果同名timer已存在则直接返回false
	 * @param cron        cron表达式
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 调度是否成功
	 * @throws ParseException cron解析异常
	 */
	public boolean scheduleNamed(@NotNull String timerId, @NotNull String cron,
								 @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData) throws ParseException {
		return scheduleNamed(timerId, cron, -1, -1, eMissfirePolicyNothing, handleClass, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId     如果同名timer已存在则直接返回false
	 * @param cron        cron表达式
	 * @param times       限制触发次数, -1表示不限次数
	 * @param endTime     限制触发的最后时间(unix毫秒时间戳)
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 调度是否成功
	 * @throws ParseException cron解析异常
	 */
	public boolean scheduleNamed(@NotNull String timerId, @NotNull String cron, long times, long endTime,
								 @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData) throws ParseException {
		return scheduleNamed(timerId, cron, times, endTime, eMissfirePolicyNothing, handleClass, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId        如果同名timer已存在则直接返回false
	 * @param cron           cron表达式
	 * @param times          限制触发次数, -1表示不限次数
	 * @param endTime        限制触发的最后时间(unix毫秒时间戳)
	 * @param missfirePolicy 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
	 * @param handleClass    回调class
	 * @param customData     自定义数据
	 * @return 调度是否成功
	 * @throws ParseException cron解析异常
	 */
	public boolean scheduleNamed(@NotNull String timerId, @NotNull String cron, long times, long endTime,
								 int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData) throws ParseException {
		return scheduleNamed(timerId, cron, times, endTime, missfirePolicy, handleClass, customData, "");
	}

	public boolean scheduleNamed(@NotNull String timerId, @NotNull String cron, long times, long endTime,
								 int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
								 @Nullable Bean customData, @NotNull String oneByOneKey) throws ParseException {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		var index = _tIndexs.get(timerId);
		if (index != null) {
			if (index.getServerId() != zeze.getConfig().getServerId())
				return false; // 已经被其它gs调度
			if (cronEquals(index, timerId, cron, times, endTime, missfirePolicy, handleClass, customData, oneByOneKey))
				return true;
			cancel(timerId); // 先取消,下面再重建
		}

		var cronTimer = new BCronTimer();
		initCronTimer(cronTimer, cron, times, endTime, oneByOneKey);
		cronTimer.setMissfirePolicy(missfirePolicy);
		schedule(timerId, cronTimer, handleClass, customData);
		return true;
	}

	public boolean cronEquals(@NotNull BIndex index, @NotNull String timerId, @NotNull String cron, long times,
							  long endTime, int missfirePolicy, @NotNull Class<? extends TimerHandle> handleClass,
							  @Nullable Bean customData, @NotNull String oneByOneKey) {
		if (timerFutures.containsKey(timerId)) { // 在调度中
			var root = _tNodeRoot.get(zeze.getConfig().getServerId());
			if (root != null) {
				var node = _tNodes.get(index.getNodeId());
				if (node != null) {
					var timer = node.getTimers().get(timerId);
					if (timer != null
							&& timer.getHandleName().equals(handleClass.getName())
							&& timer.getCustomData().getBean().equals(customData)) {
						var timerObj = timer.getTimerObj().getBean();
						if (timerObj instanceof BCronTimer) {
							var cronTimer = (BCronTimer)timerObj;
							return cronTimer.getCronExpression().equals(cron)
									&& cronTimer.getRemainTimes() == times
									&& cronTimer.getEndTime() == endTime
									&& cronTimer.getMissfirePolicy() == missfirePolicy
									&& cronTimer.getOneByOneKey().equals(oneByOneKey);
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * 取消一个具体的Timer实例。
	 * 需要在事务内调用。
	 */
	public void cancel(@Nullable String timerId) {
		if (timerId == null)
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
		if (index != null) {
			int serverId = index.getServerId();
			if (serverId != zeze.getConfig().getServerId())
				Transaction.whileCommit(() -> tryRedirectCancel(serverId, timerId));
			try {
				TimerHandle handle = null;
				var nodeId = index.getNodeId();
				var node = _tNodes.get(nodeId);
				if (node != null) {
					var timer = node.getTimers().get(timerId);
					if (timer != null)
						handle = findTimerHandle(timer.getHandleName());
				}
				cancel(serverId, timerId, nodeId, node, handle);
			} catch (Exception e) {
				Task.forceThrow(e);
			}
		} else // 定时器数据已经不存在了,尝试移除future
			Transaction.whileCommit(() -> cancelFuture(timerId));
	}

	@NotNull tAccountTimers tAccountTimers() {
		return _tAccountTimers;
	}

	@NotNull tIndexs tIndexs() {
		return _tIndexs;
	}

	@NotNull tNodes tNodes() {
		return _tNodes;
	}

	@NotNull tAccountOfflineTimers tAccountOfflineTimers() {
		return _tAccountOfflineTimers;
	}

	public TimerAccount getAccountTimer() {
		return timerAccount;
	}

	public Online getDefaultOnline() {
		return defaultOnline;
	}

	public @NotNull TimerRole getRoleTimer() {
		return defaultOnline.getTimerRole();
	}

	public @NotNull TimerRole getRoleTimer(@Nullable String onlineSetName) {
		var online = defaultOnline.getOnline(onlineSetName);
		if (online == null)
			throw new IllegalStateException("online miss " + onlineSetName);
		return online.getTimerRole();
	}

	public @Nullable BIndex getTimerIndex(@NotNull String timerId) {
		return _tIndexs.get(timerId);
	}

	public @Nullable BTimer getTimer(@NotNull String timerId) {
		var index = _tIndexs.get(timerId);
		if (index == null)
			return null;
		var node = _tNodes.get(index.getNodeId());
		return node != null ? node.getTimers().get(timerId) : null;
	}

	// ///////////////////////////////////////////////////////////
	// 内部实现
	protected void tryRedirectCancel(int serverId, @NotNull String timerId) {
		// redirect 现在仅取消future，总是尝试，不检查其他参数。
		if (zeze.getConfig().getServerId() != serverId
				&& zeze.getProviderApp().providerDirectService.providerByServerId.containsKey(serverId)) {
			redirectCancel(serverId, timerId);
		}
	}

	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	@RedirectToServer
	protected void redirectCancel(int serverId, @NotNull String timerId) {
		// redirect 现在仅取消future，总是尝试，不检查其他参数。
		cancelFuture(timerId);
	}

	void cancelFuture(@NotNull String timerId) {
		var local = timerFutures.remove(timerId);
		if (local != null)
			local.cancel(false);
	}

	private void cancel(int serverId, @NotNull String timerId, long nodeId, @Nullable BNode node,
						@Nullable TimerHandle handle) {
		// 事务成功时，总是尝试cancel future
		Transaction.whileCommit(() -> cancelFuture(timerId));
		_tIndexs.remove(timerId);
		if (node != null) {
			var timers = node.getTimers();
			var bTimer = timers.remove(timerId);
			if (timers.isEmpty()) {
				var prevNodeId = node.getPrevNodeId();
				var nextNodeId = node.getNextNodeId();
				var prev = _tNodes.get(prevNodeId);
				if (prev != null && prev.getNextNodeId() == nodeId)
					prev.setNextNodeId(nextNodeId);
				var next = _tNodes.get(nextNodeId);
				if (next != null && next.getPrevNodeId() == nodeId)
					next.setPrevNodeId(prevNodeId);
				var root = _tNodeRoot.get(serverId);
				if (root != null) {
					if (root.getHeadNodeId() == nodeId) {
						if (root.getTailNodeId() == nodeId) {
							root.setHeadNodeId(0);
							root.setTailNodeId(0);
							root.setVersion(0);
						} else
							root.setHeadNodeId(nextNodeId);
					} else if (root.getTailNodeId() == nodeId)
						root.setTailNodeId(prevNodeId);
				}
				// 把当前空的Node加入垃圾回收。
				// 由于Nodes并发访问的原因，不能马上删除。延迟一定时间就安全了。
				// 不删除的话就会在数据库留下垃圾。
				_tNodes.delayRemove(nodeId);
			}
			if (handle != null && bTimer != null) {
				Task.call(zeze.newProcedure(() -> {
					handle.onTimerCancel(bTimer);
					return 0;
				}, "Timer.fireTimerCancel"));
			}
		}
	}

	private void scheduleSimple(long timerSerialId, int serverId, @NotNull String timerId, long delay,
								long concurrentSerialNo, boolean putIfAbsent, @Nullable String oneByOneKey) {
		Transaction.whileCommit(() -> {
			if (!putIfAbsent || !timerFutures.containsKey(timerId)) {
				timerFutures.put(timerId, Task.scheduleUnsafe(delay, () -> {
					if (oneByOneKey == null || oneByOneKey.isEmpty())
						fireSimple(timerSerialId, serverId, timerId, concurrentSerialNo, false);
					else {
						zeze.getTaskOneByOneByKey().Execute(oneByOneKey,
								() -> fireSimple(timerSerialId, serverId, timerId, concurrentSerialNo, false));
					}
				}));
			}
		});
	}

	public static void initSimpleTimer(@NotNull BSimpleTimer simpleTimer, long delay, long period, long times,
									   long endTime, @NotNull String oneByOneKey) {
		if (delay < 0)
			throw new IllegalArgumentException("delay(" + delay + ") < 0");
		var now = System.currentTimeMillis();
		// simpleTimer.setDelay(delay);
		simpleTimer.setPeriod(period);
		simpleTimer.setRemainTimes(times);
		simpleTimer.setStartTime(now);
		simpleTimer.setEndTime(endTime);
		simpleTimer.setNextExpectedTime(now + delay);
		simpleTimer.setOneByOneKey(oneByOneKey);
	}

	public static boolean nextSimpleTimer(@NotNull BSimpleTimer simpleTimer, boolean missfire) {
		// check period
		var period = simpleTimer.getPeriod();
		if (period <= 0)
			return false;

		// check remain times
		var remainTimes = simpleTimer.getRemainTimes();
		if (remainTimes >= 0) {
			if (remainTimes > 0)
				simpleTimer.setRemainTimes(--remainTimes);
			if (remainTimes == 0)
				return false;
		}

		var nextExpectedTime = simpleTimer.getNextExpectedTime();
		simpleTimer.setExpectedTime(nextExpectedTime);
		simpleTimer.setHappenTimes(simpleTimer.getHappenTimes() + 1);
		long now = System.currentTimeMillis();
		simpleTimer.setHappenTime(now);

		if (missfire && simpleTimer.getMissfirePolicy() == eMissfirePolicyRunOnce) {
			// 这种策略重置时间，定时器将在新的开始时间之后按原来的间隔执行。
			// simpleTimer.setStartTime(now);
			nextExpectedTime = now + period;
		} else
			nextExpectedTime += period;
		simpleTimer.setNextExpectedTime(nextExpectedTime);

		// check endTime
		var endTime = simpleTimer.getEndTime();
		return endTime <= 0 || nextExpectedTime <= endTime;
	}

	private void fireSimple(long timerSerialId, int serverId, @NotNull String timerId, long concurrentSerialNo,
							boolean missfire) {
		if (Task.call(zeze.newProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (index == null
					|| index.getServerId() != zeze.getConfig().getServerId() // 不是拥有者，取消本地调度，应该是不大可能发生的。
					|| index.getSerialId() != timerSerialId // 新注册的，旧的future需要取消。
			) {
				Transaction.whileCommit(() -> cancelFuture(timerId));
				return 0;
			}

			var nodeId = index.getNodeId();
			var node = _tNodes.get(nodeId);
			if (node == null) {
				cancel(serverId, timerId, nodeId, null, null);
				return 0;
			}

			var timer = node.getTimers().get(timerId);
			if (timer == null)
				throw new IllegalStateException("maybe operate before timer created");
			var handle = findTimerHandle(timer.getHandleName());
			var simpleTimer = timer.getTimerObj_Zeze_Builtin_Timer_BSimpleTimer();
			if (concurrentSerialNo == timer.getConcurrentFireSerialNo()) {
				var hasNext = nextSimpleTimer(simpleTimer, missfire);
				var context = new TimerContext(this, timer, simpleTimer.getHappenTimes(),
						simpleTimer.getExpectedTime(), simpleTimer.getNextExpectedTime());

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
					cancel(serverId, timerId, nodeId, node, handle);
					return 0;
				}
				timer.setConcurrentFireSerialNo(concurrentSerialNo + 1);
				// 其他错误忽略
				if (!hasNext) {
					cancel(serverId, timerId, nodeId, node, handle);
					return 0;
				}
			}
			// else 发生了并发执行争抢，也需要再次进行本地调度。此时直接使用simpleTimer中的值，不需要再次进行计算。

			// continue period
			scheduleSimple(timerSerialId, serverId, timerId,
					simpleTimer.getNextExpectedTime() - System.currentTimeMillis(),
					concurrentSerialNo + 1, false, simpleTimer.getOneByOneKey());
			return 0;
		}, "Timer.fireSimple")) != 0) {
			Task.call(zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "Timer.cancelTimer"));
		}
	}

	private void scheduleCron(long timerSerialId, int serverId, @NotNull String timerId, @NotNull BCronTimer cron,
							  long concurrentSerialNo, boolean putIfAbsent, @Nullable String oneByOneKey) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(timerSerialId, serverId, timerId, delay, concurrentSerialNo, putIfAbsent, oneByOneKey);
		} catch (Exception ex) {
			// 这个错误是在不好处理。先只记录日志吧。
			logger.error("", ex);
		}
	}

	private void scheduleCronNext(long timerSerialId, int serverId, @NotNull String timerId, long delay,
								  long concurrentSerialNo, boolean putIfAbsent, @Nullable String oneByOneKey) {
		Transaction.whileCommit(() -> {
			if (!putIfAbsent || !timerFutures.containsKey(timerId)) {
				timerFutures.put(timerId, Task.scheduleUnsafe(delay, () -> {
					if (oneByOneKey == null || oneByOneKey.isEmpty())
						fireCron(timerSerialId, serverId, timerId, concurrentSerialNo, false);
					else {
						zeze.getTaskOneByOneByKey().Execute(oneByOneKey,
								() -> fireCron(timerSerialId, serverId, timerId, concurrentSerialNo, false));
					}
				}));
			}
		});
	}

	public static long cronNextTime(@NotNull String cron, long time) throws ParseException {
		var cronExpression = new CronExpression(cron);
		return cronExpression.getNextValidTimeAfter(new Date(time)).getTime();
	}

	public static void initCronTimer(@NotNull BCronTimer cronTimer, @NotNull String cron, long times, long endTime,
									 @NotNull String oneByOneKey) throws ParseException {
		cronTimer.setCronExpression(cron);
		cronTimer.setNextExpectedTime(cronNextTime(cron, System.currentTimeMillis()));
		cronTimer.setRemainTimes(times);
		cronTimer.setEndTime(endTime);
		cronTimer.setOneByOneKey(oneByOneKey);
	}

	public static boolean nextCronTimer(@NotNull BCronTimer cronTimer, boolean missfire) throws ParseException {
		// check remain times
		var remainTimes = cronTimer.getRemainTimes();
		if (remainTimes >= 0) {
			if (remainTimes > 0)
				cronTimer.setRemainTimes(--remainTimes);
			if (remainTimes == 0)
				return false;
		}

		var nextExpectedTime = cronTimer.getNextExpectedTime();
		cronTimer.setExpectedTime(nextExpectedTime);
		cronTimer.setHappenTimes(cronTimer.getHappenTimes() + 1);
		var now = System.currentTimeMillis();
		cronTimer.setHappenTime(now);

		long baseTime;
		if (missfire && cronTimer.getMissfirePolicy() == eMissfirePolicyRunOnce) {
			// 这种策略重置时间，定时器将在新的开始时间之后按原来的间隔执行。
			// cronTimer.setStartTime(now);
			baseTime = now;
		} else
			baseTime = nextExpectedTime;
		nextExpectedTime = cronNextTime(cronTimer.getCronExpression(), baseTime);
		cronTimer.setNextExpectedTime(nextExpectedTime);

		// check endTime
		var endTime = cronTimer.getEndTime();
		return endTime <= 0 || nextExpectedTime <= endTime;
	}

	private void fireCron(long timerSerialId, int serverId, @NotNull String timerId, long concurrentSerialNo,
						  boolean missfire) {
		if (Task.call(zeze.newProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (index == null
					|| index.getServerId() != zeze.getConfig().getServerId() // 不是拥有者，取消本地调度，应该是不大可能发生的。
					|| index.getSerialId() != timerSerialId // 新注册的，旧的future需要取消。
			) {
				Transaction.whileCommit(() -> cancelFuture(timerId));
				return 0;
			}

			var nodeId = index.getNodeId();
			var node = _tNodes.get(nodeId);
			if (node == null) {
				cancel(serverId, timerId, nodeId, null, null); // maybe concurrent cancel
				return 0;
			}

			var timer = node.getTimers().get(timerId);
			if (timer == null)
				throw new IllegalStateException("maybe operate before timer created");
			var handle = findTimerHandle(timer.getHandleName());
			var cronTimer = timer.getTimerObj_Zeze_Builtin_Timer_BCronTimer();
			if (concurrentSerialNo == timer.getConcurrentFireSerialNo()) {
				var hasNext = Timer.nextCronTimer(cronTimer, missfire);
				var context = new TimerContext(this, timer, cronTimer.getHappenTimes(),
						cronTimer.getExpectedTime(), cronTimer.getNextExpectedTime());

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
					cancel(serverId, timerId, nodeId, node, handle);
					return 0;
				}
				timer.setConcurrentFireSerialNo(concurrentSerialNo + 1);

				if (!hasNext) {
					cancel(serverId, timerId, nodeId, node, handle);
					return 0;
				}
			}
			// else 发生了并发执行争抢，也需要再次进行本地调度。此时直接使用cronTimer中的值，不需要再次进行计算。

			// continue period
			scheduleCronNext(timerSerialId, serverId, timerId,
					cronTimer.getNextExpectedTime() - System.currentTimeMillis(),
					concurrentSerialNo + 1, false, cronTimer.getOneByOneKey());
			return 0;
		}, "Timer.fireCron")) != 0) {
			Task.call(zeze.newProcedure(() -> {
				cancel(timerId);
				return 0;
			}, "Timer.cancelTimer"));
		}
	}

	private void loadTimer() throws Exception {
		var serverId = zeze.getConfig().getServerId();
		var outRoot = new BNodeRoot();
		var r = Task.call(zeze.newProcedure(() -> {
			var root = _tNodeRoot.getOrAdd(serverId);
			// 本地每次load都递增。用来处理和接管的并发。
			root.setLoadSerialNo(root.getLoadSerialNo() + 1);
			outRoot.assign(root);
			return 0;
		}, "Timer.loadTimerLocal"));
		if (r == Procedure.Success) {
			var offlineNotify = new BOfflineNotify();
			offlineNotify.serverId = serverId;
			offlineNotify.notifyId = "Zeze.Component.Timer.OfflineNotify";
			offlineNotify.notifySerialId = outRoot.getLoadSerialNo();
			zeze.getServiceManager().offlineRegister(offlineNotify,
					notify -> spliceLoadTimer(notify.serverId, notify.notifySerialId));
			loadTimer(outRoot.getHeadNodeId(), outRoot.getHeadNodeId(), serverId); // last也填头节点是因为链表是循环的
		} else
			logger.error("loadTimer failed: r={}", r);

		var agent = zeze.getServiceManager();
		if (agent instanceof Agent) { // 暂时只支持非Raft的ServiceManager
			var p = new AnnounceServers();
			p.Argument.notifyId = "Zeze.Component.Timer.NotifyOffline";
			p.Argument.serverId = serverId;
			_tNodeRoot.walk((k, v) -> {
				if (v.getHeadNodeId() != 0) {
					p.Argument.watchServerIds.add(k);
					p.Argument.watchSerialIds.add(v.getLoadSerialNo());
				}
				return true;
			});
			((Agent)agent).offlineRegister(p.Argument.notifyId,
					notify -> spliceLoadTimer(notify.serverId, notify.notifySerialId));
			agent.waitReady();
			p.SendAndWaitCheckResultCode(((Agent)agent).getClient().getSocket());
		}
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

		var first = new OutLong();
		var last = new OutLong();
		var r = Task.call(zeze.newProcedure(() -> {
			// 当接管别的服务器的定时器时，有可能那台服务器有新的CustomData，这个时候重新加载一次。
			var src = _tNodeRoot.get(serverId);
			long srcHeadNodeId, srcTailNodeId;
			if (src == null || (srcHeadNodeId = src.getHeadNodeId()) == 0 || (srcTailNodeId = src.getTailNodeId()) == 0)
				return 0;
			if (src.getVersion() > zeze.getConfig().getAppVersion()) // 不接管版本高的,让相同或高版本的接管
				return 0;
			if (src.getLoadSerialNo() != loadSerialNo) // 需要接管的机器已经活过来了
				return 0;
			var srcHead = _tNodes.get(srcHeadNodeId);
			var srcTail = _tNodes.get(srcTailNodeId);
			if (srcHead == null || srcTail == null)
				throw new IllegalStateException("maybe operate before timer created.");

			var root = _tNodeRoot.getOrAdd(zeze.getConfig().getServerId());
			var headNodeId = root.getHeadNodeId();
			var tailNodeId = root.getTailNodeId();
			var head = _tNodes.get(headNodeId);
			var tail = _tNodes.get(tailNodeId);
			if (head == null || tail == null)
				root.setTailNodeId(srcTailNodeId);
			else {
				srcHead.setPrevNodeId(tailNodeId);
				srcTail.setNextNodeId(headNodeId);
				head.setPrevNodeId(srcTailNodeId);
				tail.setNextNodeId(srcHeadNodeId);
			}
			root.setHeadNodeId(srcHeadNodeId);
			src.setHeadNodeId(0);
			src.setTailNodeId(0);
			src.setVersion(0);
			first.value = srcHeadNodeId;
			last.value = headNodeId;
			return 0;
		}, "Timer.spliceAndLoadTimerLocal"));

		if (r == 0)
			loadTimer(first.value, last.value, serverId);
		return r;
	}

	// 如果存在node，至少执行一次循环。
	private void loadTimer(long first, long last, int serverId) {
		if (first == 0 && last == 0)
			return;
		var idSet = new LongHashSet();
		var node = new OutLong(first);
		do {
			var nodeId = node.value;
			if (!idSet.add(nodeId)) // 检测并避免死循环
				break;
			// skip error. 使用node返回的值决定是否继续循环。
			var r = Task.call(zeze.newProcedure(() -> {
				loadTimer(node, last, serverId);
				return 0;
			}, "Timer.loadTimer"));
			if (r != Procedure.Success) {
				logger.error("loadTimer failed: r={}, nodeId={}", r, nodeId);
				try {
					//noinspection BusyWait
					Thread.sleep(1000); // 避免因FastErrorPeriod导致过于频繁的事务失败
				} catch (InterruptedException ignored) {
				}
			}
		} while (node.value != last);
	}

	private void loadTimer(@NotNull OutLong nodeId, long last, int serverId) throws ParseException {
		var node = _tNodes.get(nodeId.value);
		if (node == null) {
			logger.warn("loadTimer not found nodeId={}", nodeId.value);
			nodeId.value = last; // 马上结束外面的循环。last仅用在这里。
			return; // when root is empty。no node。skip error.
		}
		// BUG 修复，如果first.value直接设置，在发生redo时，当前node会被跳过。
		// 这是因为first是in&out的。另一个解决办法是，first改成只out，当前node用另一个值参数传入。
		Transaction.whileCommit(() -> nodeId.value = node.getNextNodeId()); // 设置下一个node。
		Transaction.whileRollback(() -> nodeId.value = node.getNextNodeId()); // 设置下一个node。

		var now = System.currentTimeMillis();
		var appVer = zeze.getConfig().getAppVersion();
		for (var it = node.getTimers().values().iterator(); it.hasNext(); ) {
			var timer = it.next();
			var index = _tIndexs.get(timer.getTimerName());
			if (index == null) {
				it.remove();
				continue;
			}
			if (index.getVersion() > appVer) // 无法保证高版本定时器的处理,等待各模块启动后重建定时器
				continue;

			// 优化不能用Config.getServerId整体判断，因为load中断会导致传入的serverId就是当前Config的，
			// 这回导致load中断后，部分数据没有被设置正确的serverId。
			// 需要提前到schedule之前，后面的schedule会判断这个值。
			if (index.getServerId() != serverId)
				index.setServerId(serverId);
			if (timer.getTimerObj().getBean().typeId() == BSimpleTimer.TYPEID) {
				var simpleTimer = (BSimpleTimer)timer.getTimerObj().getBean();
				if (simpleTimer.getNextExpectedTime() < now) { // missfire found
					switch (simpleTimer.getMissfirePolicy()) {
					case eMissfirePolicyRunOnce:
					case eMissfirePolicyRunOnceOldNext:
						Task.run(() -> fireSimple(index.getSerialId(), serverId, timer.getTimerName(),
								timer.getConcurrentFireSerialNo(), true), "Timer.missfireSimple");
						continue; // loop done, continue

					case eMissfirePolicyNothing:
						// 重置启动时间，调度下一个（未来）间隔的时间。没有考虑对齐。
						simpleTimer.setNextExpectedTime(now + simpleTimer.getPeriod());
						//TODO: 考虑nextExpectedTime超过endTime的情况要不要取消
						break;

					default:
						throw new UnsupportedOperationException("Unknown MissfirePolicy: "
								+ simpleTimer.getMissfirePolicy());
					}
				}
				scheduleSimple(index.getSerialId(), serverId, timer.getTimerName(),
						simpleTimer.getNextExpectedTime() - now, timer.getConcurrentFireSerialNo(),
						true, simpleTimer.getOneByOneKey());
			} else {
				var cronTimer = (BCronTimer)timer.getTimerObj().getBean();
				if (cronTimer.getNextExpectedTime() < now) {
					switch (cronTimer.getMissfirePolicy()) {
					case eMissfirePolicyRunOnce:
					case eMissfirePolicyRunOnceOldNext:
						Task.run(() -> fireCron(index.getSerialId(), serverId, timer.getTimerName(),
								timer.getConcurrentFireSerialNo(), true), "Timer.missfireCron");
						continue; // loop done, continue

					case eMissfirePolicyNothing:
						// 计算下一次（未来）发生的时间。
						cronTimer.setNextExpectedTime(cronNextTime(cronTimer.getCronExpression(), now));
						//TODO: 考虑nextExpectedTime超过endTime的情况要不要取消
						break;

					default:
						throw new UnsupportedOperationException("Unknown MissfirePolicy: "
								+ cronTimer.getMissfirePolicy());
					}
				}
				scheduleCron(index.getSerialId(), serverId, timer.getTimerName(), cronTimer,
						timer.getConcurrentFireSerialNo(), true, cronTimer.getOneByOneKey());
			}
		}
	}

	@Override
	public void clearTableCache() {
		_tNodes.__ClearTableCacheUnsafe__();
	}

	private void onHotModuleStop(@NotNull HotModule hot) {
		freshStopModuleDynamic |= hotModulesHaveDynamic.remove(hot) != null;
	}

	void tryRecordBeanHotModuleWhileCommit(@NotNull Bean customData) {
		var cl = customData.getClass().getClassLoader();
		if (HotManager.isHotModule(cl)) {
			var hotModule = (HotModule)cl;
			Transaction.whileCommit(() -> {
				hotModule.stopEvents.add(this::onHotModuleStop);
				hotModulesHaveDynamic.add(hotModule);
			});
		}
	}

	private void tryRecordHotModule(@NotNull Class<?> customClass) {
		var cl = customClass.getClassLoader();
		if (HotManager.isHotModule(cl)) {
			var hotModule = (HotModule)cl;
			hotModule.stopEvents.add(this::onHotModuleStop);
			hotModulesHaveDynamic.add(hotModule);
		}
	}

	@Override
	public @NotNull BeanFactory beanFactory() {
		return beanFactory;
	}

	@Override
	public boolean hasFreshStopModuleDynamicOnce() {
		var tmp = freshStopModuleDynamic;
		freshStopModuleDynamic = false;
		return tmp;
	}

	@Override
	public void processWithNewClasses(@NotNull List<Class<?>> newClasses) {
		for (var cls : newClasses)
			tryRecordHotModule(cls);
	}
}
