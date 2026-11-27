package Zeze.Component;

import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderImplement;
import Zeze.Arch.RedirectToServer;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BNode;
import Zeze.Builtin.Timer.BNodeRoot;
import Zeze.Builtin.Timer.BOfflineAccountCustom;
import Zeze.Builtin.Timer.BOfflineRoleCustom;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Builtin.Timer.BTimer;
import Zeze.Builtin.Timer.tAccountOfflineTimers;
import Zeze.Builtin.Timer.tAccountTimers;
import Zeze.Builtin.Timer.tIndexs;
import Zeze.Builtin.Timer.tNodes;
import Zeze.Collections.BeanFactory;
import Zeze.Game.Online;
import Zeze.Serialize.RawBean;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.OutLong;
import Zeze.Util.Reflect;
import Zeze.Util.ReflectHandleCache;
import Zeze.Util.TaskSpec;
import Zeze.Util.TransactionLevelAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 持久化定时器组件：simple/cron 定时器的调度与触发、命名/账号/角色各族入口与接管（Takeover）。
 */
public class Timer extends AbstractTimer implements TimerScope {
	static final Logger logger = LogManager.getLogger(Timer.class);
	private static int CountPerNode = Reflect.inDebugMode ? 1 : 3; // 调试状态下减少timer之间的影响,以免频繁redo
	private static final BeanFactory beanFactory = new BeanFactory();
	private static final LongConcurrentHashMap<AtomicLong> exceptCounter = new LongConcurrentHashMap<>();

	public static void setCountPerNode(int countPerNode) {
		CountPerNode = Math.max(countPerNode, 1);
	}

	public static long getSpecialTypeIdFromBean(Serializable bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		try {
			return beanFactory.createBeanFromSpecialTypeId(typeId);
		} catch (Exception e) {
			//noinspection DataFlowIssue
			if (exceptCounter.computeIfAbsent(typeId, __ -> new AtomicLong()).getAndIncrement() < 10)
				logger.error("createBeanFromSpecialTypeId({}) exception:", typeId, e);
			return new RawBean(typeId);
		}
	}

	public static long getOnlineSpecialTypeIdFromBean(Serializable bean) {
		return bean.typeId();
	}

	public static Bean createOnlineBeanFromSpecialTypeId(long typeId) {
		try {
			return beanFactory.createBeanFromSpecialTypeId(typeId);
		} catch (Exception e) {
			//noinspection DataFlowIssue
			if (exceptCounter.computeIfAbsent(typeId, __ -> new AtomicLong()).getAndIncrement() < 10)
				logger.error("createOnlineBeanFromSpecialTypeId({}) exception:", typeId, e);
			return new RawBean(typeId);
		}
	}

	public final @Nullable Application zeze;
	private @Nullable AutoKey nodeIdAutoKey;
	@Nullable AutoKey timerIdAutoKey;
	@Nullable AutoKey timerSerialId;

	// 在这台服务器进程内调度的所有Timer。key是timerId，value是ThreadPool.schedule的返回值。
	final ConcurrentHashMap<String, Future<?>> timerFutures = new ConcurrentHashMap<>();
	private final ReflectHandleCache<TimerHandle> timerHandles = new ReflectHandleCache<>(TimerHandle.class);
	// 触发入口跨线程读取；安装与停止摘取句柄在 timerFutures 监视器下互斥。
	private volatile boolean started;

	// 供 TimerOnlineBase 读取停机闸门状态
	final boolean isStarted() {
		return started;
	}

	private @Nullable TimerAccount timerAccount;
	private @Nullable Online defaultOnline;

	public Application getZeze() {
		return Objects.requireNonNull(zeze, "timer application is not initialized");
	}

	public static Timer create(AppBase app) {
		return GenModule.createRedirectModule(Timer.class, app);
	}

	protected Timer(AppBase app) {
		zeze = app.getZeze();
		if (zeze != null) { // 只生成Redirect代码时zeze可能为null
			RegisterZezeTables(zeze);
			// 必须在构造期（Takeover.claim 前）注册作用域，否则 startModules 阶段写定时器会因未 stamp 被 fence 拦截
			getZeze().getTakeover().addScope(timerTakeoverScope);
		}
	}

	static void register(Serializable s) {
		beanFactory.register(s);
	}

	public void loadCustomClassAnd() {
		nodeIdAutoKey = getZeze().getAutoKey("Zeze.Component.Timer.NodeId");
		timerIdAutoKey = getZeze().getAutoKey("Zeze.Component.Timer.TimerId");
		timerSerialId = getZeze().getAutoKey("Zeze.Component.Timer.SerialId");
	}

	/**
	 * 非事务环境调用。用于启动Timer服务。
	 */
	public void start() throws Exception {
		lock();
		try {
			if (started)
				return;
			// 必须先置位再 loadTimer，避免装载提交时的 whileCommit 安装被停机闸门拒绝
			started = true;
			try {
				loadTimer();
			} catch (Exception e) {
				try {
					stop(); // 复用正常停止路径撤销此前节点已安装的future。
				} catch (Exception cleanup) {
					e.addSuppressed(cleanup);
				}
				throw e;
			}
		} finally {
			unlock();
		}
	}

	/**
	 * 初始化在线Timer。在线Timer需要ProviderApp。
	 *
	 * @param providerApp 参数
	 */
	public void initializeOnlineTimer(ProviderApp providerApp) {
		ProviderImplement impl;
		//noinspection ConstantValue
		if (providerApp != null && (impl = providerApp.providerImplement) != null) {
			if (impl instanceof Zeze.Arch.ProviderWithOnline p)
				timerAccount = new TimerAccount(Objects.requireNonNull(p.getOnline()));
			else if (impl instanceof Zeze.Game.ProviderWithOnline p)
				defaultOnline = p.getOnline();
		}
	}

	/**
	 * 停止Timer服务。
	 */
	public void stop() throws Exception {
		lock();
		try {
			if (!started)
				return;

			List<Future<?>> futures;
			synchronized (timerFutures) {
				started = false;
				futures = List.copyOf(timerFutures.values());
				timerFutures.clear();
			}

			// 停止当前正在调度的timer。
			for (var future : futures)
				future.cancel(false);
			for (var future : futures) {
				try {
					future.get(1, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break; // 停止线程被中断，不再空等剩余任务
				} catch (Exception ignored) {
					// ignored
				}
			}

			// 不在此 UnRegisterZezeTables：表在构造时注册，stop时注销两者不匹配。
		} finally {
			unlock();
		}
	}

	TimerHandle findTimerHandle(String handleClassName) {
		return timerHandles.findHandle(handleClassName);
	}

	private @Nullable TimerHandle findTimerHandleOrNull(String handleClassName) {
		try {
			return findTimerHandle(handleClassName);
		} catch (Throwable e) {
			logger.error("findTimerHandle({}) exception:", handleClassName, e);
			return null;
		}
	}

	/**
	 * TimerHandle.onTimerCancel 没有上下文参数，无状态handle到这里反查所属App。
	 * 钩子总是在 {@code Timer.cancel} 的 fireTimerCancel 过程内同步执行，
	 * 取当前过程栈顶（即该过程）的owner即发起cancel的Timer所属App。
	 */
	static @Nullable Application currentProcedureApp() {
		var t = Transaction.getCurrent();
		if (t == null || !t.isRunning())
			return null;
		var stack = t.getProcedureStack();
		return stack.isEmpty() ? null : stack.getLast().getZeze();
	}

	static void checkRunningTransaction(String methodName) {
		var t = Transaction.getCurrent();
		if (t == null || !t.isRunning())
			throw new IllegalStateException("Timer." + methodName + " must be called within a running transaction.");
	}

	/**
	 * 事务内，得到可以直接修改的custom bean引用。
	 *
	 * @param timerId timerId
	 * @return custom bean
	 */
	public @Nullable Bean getTimerCustomBean(String timerId) {
		var bIndex = _tIndexs.get(timerId);
		if (null == bIndex)
			return null;
		if (bIndex.getServerId() != getZeze().getConfig().getServerId())
			return null; // 只能本server修改

		var bNode = _tNodes.get(bIndex.getNodeId());
		if (null == bNode)
			return null;

		var bTimer = bNode.getTimers().get(timerId);
		if (bTimer == null)
			return null;

		var bean = bTimer.getCustomData().getBean();
		if (bean instanceof EmptyBean)
			return null; // no custom

		return bean;
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
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofDelay(delay).period(period), handleClass, customData)}
	 */
	@Deprecated
	public String schedule(long delay, long period, Class<? extends TimerHandle> handleClass,
	                                @Nullable Bean customData) {
		return schedule(TimerSpec.ofDelay(delay).period(period), handleClass, customData);
	}

	/**
	 * 调度一个Timeout，即仅执行一次的Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay       首次触发延迟(毫秒), 不能小于0
	 * @param handleClass Timer处理Class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofDelay(delay).times(1), handleClass, customData)}
	 */
	@Deprecated
	public String schedule(long delay, Class<? extends TimerHandle> handleClass,
	                                @Nullable Bean customData) {
		return schedule(TimerSpec.ofDelay(delay).times(1), handleClass, customData);
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
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofDelay(delay).period(period).times(times), handleClass, customData)}
	 */
	@Deprecated
	public String schedule(long delay, long period, long times,
	                                Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return schedule(TimerSpec.ofDelay(delay).period(period).times(times), handleClass, customData);
	}

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay       首次触发延迟(毫秒), 不能小于0
	 * @param period      触发周期(毫秒), 只有大于0才会周期触发
	 * @param times       限制触发次数, -1表示不限次数
	 * @param endTime     限制触发的最后时间(unix毫秒时间戳), 只有大于0会限制
	 * @param handleClass Timer处理Class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime), handleClass, customData)}
	 */
	@Deprecated
	public String schedule(long delay, long period, long times, long endTime,
	                                Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return schedule(TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime),
				handleClass, customData);
	}

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 *
	 * @param delay          首次触发延迟(毫秒), 不能小于0
	 * @param period         触发周期(毫秒), 只有大于0才会周期触发
	 * @param times          限制触发次数, -1表示不限次数
	 * @param endTime        限制触发的最后时间(unix毫秒时间戳), 只有大于0会限制
	 * @param missfirePolicy 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
	 * @param handleClass    Timer处理Class
	 * @param customData     自定义数据
	 * @return 自动生成的timerId
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime).missfirePolicy(missfirePolicy), handleClass, customData)}
	 */
	@Deprecated
	public String schedule(long delay, long period, long times, long endTime, int missfirePolicy,
	                                Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return schedule(TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime)
				.missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime).missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData)}
	 */
	@Deprecated
	public String schedule(long delay, long period, long times, long endTime, int missfirePolicy,
	                                Class<? extends TimerHandle> handleClass, @Nullable Bean customData,
	                                String oneByOneKey) {
		return schedule(TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime)
				.missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	// 直接传递BSimpleTimer（由SimpleTimerSpec.build()生成）。暂时不开放了。
	private String schedule(BSimpleTimer simpleTimer,
	                                 Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		//noinspection DataFlowIssue
		var timeId = '@' + timerIdAutoKey.nextString();
		schedule(timeId, simpleTimer, handleClass, customData);
		return timeId;
	}

	void schedule(String timerId, BSimpleTimer simpleTimer,
	              Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		Reflect.checkDefaultConstructor(handleClass);
		var serverId = getZeze().getConfig().getServerId();
		var appVer = getZeze().getConfig().getAppVersion();
		var root = _tNodeRoot.getOrAdd(serverId);
		checkTimerFence(serverId, root); // 插链前fence：被接管则致命退出，不再写链
		if (root.getVersion() < appVer)
			root.setVersion(appVer);
		var nodeId = root.getHeadNodeId();
		if (nodeId == 0) // 如果整个双链表是空的
			//noinspection DataFlowIssue
			nodeId = nodeIdAutoKey.nextId();
		for (; ; //noinspection DataFlowIssue
			 nodeId = nodeIdAutoKey.nextId()) {
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
				//noinspection DataFlowIssue
				var serialId = timerSerialId.nextId();
				_tIndexs.insert(timerId, new BIndex(serverId, nodeId, serialId, appVer));

				var timer = new BTimer(timerId, handleClass.getName(), 0);
				timer.setTimerObj(simpleTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					register(customData);
					timer.getCustomData().setBean(customData);
				}

				scheduleSimple(serialId, serverId, timerId,
						Math.max(simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), 1),
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
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofMonth(monthDay,hour,minute,second), handleClass, customData)}
	 */
	@Deprecated
	public String scheduleMonth(int monthDay, int hour, int minute, int second,
	                                     Class<? extends TimerHandle> handleClass,
	                                     @Nullable Bean customData) {
		return schedule(TimerSpec.ofMonth(monthDay, hour, minute, second), handleClass, customData);
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
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofWeek(weekDay,hour,minute,second), handleClass, customData)}
	 */
	@Deprecated
	public String scheduleWeek(int weekDay, int hour, int minute, int second,
	                                    Class<? extends TimerHandle> handleClass,
	                                    @Nullable Bean customData) {
		return schedule(TimerSpec.ofWeek(weekDay, hour, minute, second), handleClass, customData);
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
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofDay(hour,minute,second), handleClass, customData)}
	 */
	@Deprecated
	public String scheduleDay(int hour, int minute, int second,
	                                   Class<? extends TimerHandle> handleClass,
	                                   @Nullable Bean customData) {
		return schedule(TimerSpec.ofDay(hour, minute, second), handleClass, customData);
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
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofCron(cronExpression), handleClass, customData)}
	 */
	@Deprecated
	public String schedule(String cronExpression,
	                                Class<? extends TimerHandle> handleClass,
	                                @Nullable Bean customData) throws ParseException {
		return schedule(TimerSpec.ofCron(cronExpression), handleClass, customData);
	}

	/**
	 * 调度一个根据Cron表达式描述的Timer。
	 * 需要在事务内使用。
	 *
	 * @param cronExpression cron表达式
	 * @param times          限制触发次数, -1表示不限次数
	 * @param endTime        限制触发的最后时间(unix毫秒时间戳), 只有大于0会限制
	 * @param handleClass    回调class
	 * @param customData     自定义数据
	 * @return 自动生成的timerId
	 * @throws ParseException cron解析异常
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofCron(cronExpression).times(times).endTime(endTime), handleClass, customData)}
	 */
	@Deprecated
	public String schedule(String cronExpression, long times, long endTime,
	                                Class<? extends TimerHandle> handleClass,
	                                @Nullable Bean customData) throws ParseException {
		return schedule(TimerSpec.ofCron(cronExpression).times(times).endTime(endTime),
				handleClass, customData);
	}

	/**
	 * 调度一个根据Cron表达式描述的Timer。
	 * 需要在事务内使用。
	 *
	 * @param cronExpression cron表达式
	 * @param times          限制触发次数, -1表示不限次数
	 * @param endTime        限制触发的最后时间(unix毫秒时间戳), 只有大于0会限制
	 * @param missfirePolicy 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
	 * @param handleClass    回调class
	 * @param customData     自定义数据
	 * @return 自动生成的timerId
	 * @throws ParseException cron解析异常
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofCron(cronExpression).times(times).endTime(endTime).missfirePolicy(missfirePolicy), handleClass, customData)}
	 */
	@Deprecated
	public String schedule(String cronExpression, long times, long endTime, int missfirePolicy,
	                                Class<? extends TimerHandle> handleClass,
	                                @Nullable Bean customData) throws ParseException {
		return schedule(TimerSpec.ofCron(cronExpression).times(times).endTime(endTime)
				.missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * @deprecated 请使用 {@code schedule(TimerSpec.ofCron(cronExpression).times(times).endTime(endTime).missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData)}
	 */
	@Deprecated
	public String schedule(String cronExpression, long times, long endTime, int missfirePolicy,
	                                Class<? extends TimerHandle> handleClass,
	                                @Nullable Bean customData, String oneByOneKey) throws ParseException {
		return schedule(TimerSpec.ofCron(cronExpression).times(times).endTime(endTime)
				.missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	// 直接传递BCronTimer（由CronTimerSpec.build()生成）。暂时不开放了。
	private String schedule(BCronTimer cronTimer,
	                                 Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		//noinspection DataFlowIssue
		var timerId = '@' + timerIdAutoKey.nextString();
		schedule(timerId, cronTimer, handleClass, customData);
		return timerId;
	}

	void schedule(String timerId, BCronTimer cronTimer, Class<? extends TimerHandle> handleClass,
	              @Nullable Bean customData) {
		Reflect.checkDefaultConstructor(handleClass);
		var serverId = getZeze().getConfig().getServerId();
		var appVer = getZeze().getConfig().getAppVersion();
		var root = _tNodeRoot.getOrAdd(serverId);
		checkTimerFence(serverId, root); // 插链前fence：被接管则致命退出，不再写链
		if (root.getVersion() < appVer)
			root.setVersion(appVer);
		var nodeId = root.getHeadNodeId();
		if (nodeId == 0) // 如果整个双链表是空的
			//noinspection DataFlowIssue
			nodeId = nodeIdAutoKey.nextId();
		for (; ; //noinspection DataFlowIssue
			 nodeId = nodeIdAutoKey.nextId()) {
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
				//noinspection DataFlowIssue
				var serialId = timerSerialId.nextId();
				_tIndexs.insert(timerId, new BIndex(serverId, nodeId, serialId, appVer));

				var timer = new BTimer(timerId, handleClass.getName(), 0);
				timer.setTimerObj(cronTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					register(customData);
					timer.getCustomData().setBean(customData);
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
	 * @deprecated 请使用 {@code scheduleNamed(timerId, TimerSpec.ofDelay(delay), handleClass, customData)}
	 */
	@Deprecated
	public boolean scheduleNamed(String timerId, long delay,
	                             Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return scheduleNamed(timerId, TimerSpec.ofDelay(delay), handleClass, customData);
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
	 * @deprecated 请使用 {@code scheduleNamed(timerId, TimerSpec.ofDelay(delay).period(period), handleClass, customData)}
	 */
	@Deprecated
	public boolean scheduleNamed(String timerId, long delay, long period,
	                             Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return scheduleNamed(timerId, TimerSpec.ofDelay(delay).period(period), handleClass, customData);
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
	 * @deprecated 请使用 {@code scheduleNamed(timerId, TimerSpec.ofDelay(delay).period(period).times(times), handleClass, customData)}
	 */
	@Deprecated
	public boolean scheduleNamed(String timerId, long delay, long period, long times,
	                             Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		return scheduleNamed(timerId, TimerSpec.ofDelay(delay).period(period).times(times),
				handleClass, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId        名字, 如果同名timer已存在则直接返回false
	 * @param delay          首次触发延迟(毫秒), 不能小于0
	 * @param period         触发周期(毫秒), 只有大于0才会周期触发
	 * @param times          限制触发次数, -1表示不限次数
	 * @param endTime        限制触发的最后时间(unix毫秒时间戳), 只有大于0会限制
	 * @param missfirePolicy 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
	 * @param handleClass    回调class
	 * @param customData     自定义数据
	 * @return 调度是否成功
	 * @deprecated 请使用 {@code scheduleNamed(timerId, TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime).missfirePolicy(missfirePolicy), handleClass, customData)}
	 */
	@Deprecated
	public boolean scheduleNamed(String timerId, long delay, long period, long times, long endTime,
	                             int missfirePolicy, Class<? extends TimerHandle> handleClass,
	                             @Nullable Bean customData) {
		return scheduleNamed(timerId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime).missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @deprecated 请使用 {@code scheduleNamed(timerId, TimerSpec.ofDelay(delay).period(period).times(times).endTime(endTime).missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData)}
	 */
	@Deprecated
	public boolean scheduleNamed(String timerId, long delay, long period, long times, long endTime,
	                             int missfirePolicy, Class<? extends TimerHandle> handleClass,
	                             @Nullable Bean customData, String oneByOneKey) {
		return scheduleNamed(timerId, TimerSpec.ofDelay(delay).period(period).times(times)
				.endTime(endTime).missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
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
	 * @deprecated 请使用 {@code scheduleNamed(timerId, TimerSpec.ofCron(cron), handleClass, customData)}
	 */
	@Deprecated
	public boolean scheduleNamed(String timerId, String cron,
	                             Class<? extends TimerHandle> handleClass,
	                             @Nullable Bean customData) {
		return scheduleNamed(timerId, TimerSpec.ofCron(cron), handleClass, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId     如果同名timer已存在则直接返回false
	 * @param cron        cron表达式
	 * @param times       限制触发次数, -1表示不限次数
	 * @param endTime     限制触发的最后时间(unix毫秒时间戳), 只有大于0会限制
	 * @param handleClass 回调class
	 * @param customData  自定义数据
	 * @return 调度是否成功
	 * @deprecated 请使用 {@code scheduleNamed(timerId, TimerSpec.ofCron(cron).times(times).endTime(endTime), handleClass, customData)}
	 */
	@Deprecated
	public boolean scheduleNamed(String timerId, String cron, long times, long endTime,
	                             Class<? extends TimerHandle> handleClass,
	                             @Nullable Bean customData) {
		return scheduleNamed(timerId, TimerSpec.ofCron(cron).times(times).endTime(endTime),
				handleClass, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId        如果同名timer已存在则直接返回false
	 * @param cron           cron表达式
	 * @param times          限制触发次数, -1表示不限次数
	 * @param endTime        限制触发的最后时间(unix毫秒时间戳), 只有大于0会限制
	 * @param missfirePolicy 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
	 * @param handleClass    回调class
	 * @param customData     自定义数据
	 * @return 调度是否成功
	 * @deprecated 请使用 {@code scheduleNamed(timerId, TimerSpec.ofCron(cron).times(times).endTime(endTime).missfirePolicy(missfirePolicy), handleClass, customData)}
	 */
	@Deprecated
	public boolean scheduleNamed(String timerId, String cron, long times, long endTime,
	                             int missfirePolicy, Class<? extends TimerHandle> handleClass,
	                             @Nullable Bean customData) {
		return scheduleNamed(timerId, TimerSpec.ofCron(cron).times(times).endTime(endTime)
				.missfirePolicy(missfirePolicy), handleClass, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @deprecated 请使用 {@code scheduleNamed(timerId, TimerSpec.ofCron(cron).times(times).endTime(endTime).missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData)}
	 */
	@Deprecated
	public boolean scheduleNamed(String timerId, String cron, long times, long endTime,
	                             int missfirePolicy, Class<? extends TimerHandle> handleClass,
	                             @Nullable Bean customData, String oneByOneKey) {
		return scheduleNamed(timerId, TimerSpec.ofCron(cron).times(times).endTime(endTime)
				.missfirePolicy(missfirePolicy).oneByOneKey(oneByOneKey), handleClass, customData);
	}

	/////////////////////////////////////////////////////////////////
	// Spec 入口
	// 推荐使用TimerSpec描述调度参数，避免超长的参数列表。

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 *
	 * @param spec        Timer调度参数
	 * @param handleClass Timer处理Class
	 * @param customData  自定义数据
	 * @return 自动生成的timerId
	 */
	@Override
	public String schedule(TimerSpec spec, Class<? extends TimerHandle> handleClass,
									@Nullable Bean customData) {
		checkRunningTransaction("schedule");
		return switch (spec) {
			case SimpleTimerSpec s -> schedule(s.build(), handleClass, customData);
			case CronTimerSpec c -> schedule(c.build(), handleClass, customData);
		};
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 *
	 * @param timerId     名字, 如果同名timer已被其它server调度则直接返回false
	 * @param spec        Timer调度参数
	 * @param handleClass Timer处理Class
	 * @param customData  自定义数据
	 * @return 调度是否成功
	 */
	@Override
	public boolean scheduleNamed(String timerId, TimerSpec spec,
								 Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		checkRunningTransaction("scheduleNamed");
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timerId '" + timerId + "', must not begin with '@'");
		if (isOnlineTimerIdOccupied(timerId))
			return false; // 同名已被在线族占用
		var index = _tIndexs.get(timerId);
		if (index != null) {
			if (index.getServerId() != getZeze().getConfig().getServerId())
				return false; // 已经被其它gs调度
			// 仅允许本服同族（全局族）重建；撞离线族返回 false，避免残留离线簿记误杀全局定时器
			if (isOfflineFamilyTimer(index, timerId))
				return false;
			if (spec instanceof CronTimerSpec c && cronEquals(index, timerId, c, handleClass, customData))
				return true;
			cancel(timerId); // 先取消,下面再重建
		}
		switch (spec) {
		case SimpleTimerSpec s -> schedule(timerId, s.build(), handleClass, customData);
		case CronTimerSpec c -> schedule(timerId, c.build(), handleClass, customData);
		}
		return true;
	}

	// 判断 index 对应定时器是否属于账号或角色 offline 族
	private boolean isOfflineFamilyTimer(BIndex index, String timerId) {
		var node = _tNodes.get(index.getNodeId());
		var bTimer = node != null ? node.getTimers().get(timerId) : null;
		if (bTimer == null)
			return false;
		var customData = bTimer.getCustomData().getBean();
		return customData instanceof BOfflineAccountCustom || customData instanceof BOfflineRoleCustom;
	}

	public boolean cronEquals(BIndex index, String timerId, CronTimerSpec spec,
	                          Class<? extends TimerHandle> handleClass, @Nullable Bean customData) {
		if (timerFutures.containsKey(timerId)) { // 在调度中
			var root = _tNodeRoot.get(getZeze().getConfig().getServerId());
			if (root != null) {
				var node = _tNodes.get(index.getNodeId());
				if (node != null) {
					var timer = node.getTimers().get(timerId);
					if (timer != null
							&& timer.getHandleName().equals(handleClass.getName())
							&& isSameCustomData(timer.getCustomData().getBean(), customData)) {
						var timerObj = timer.getTimerObj().getBean();
						if (timerObj instanceof BCronTimer cronTimer) {
							return spec.matches(cronTimer);
						}
					}
				}
			}
		}
		return false;
	}

	private static boolean isSameCustomData(Bean savedCustomData, @Nullable Bean customData) {
		return Objects.equals(savedCustomData instanceof EmptyBean ? null : savedCustomData, customData);
	}

	/**
	 * 取消一个具体的Timer实例。
	 * 需要在事务内调用。
	 *
	 * @return 是否真的取消了已存在的timer
	 */
	@Override
	public boolean cancel(@Nullable String timerId) {
		if (timerId == null) {
			logger.warn("Timer cancel(null).", new Exception());
			return false; // 忽略没有初始化的timerId。
		}
		checkRunningTransaction("cancel");
		var index = _tIndexs.get(timerId);
		if (index != null) {
			int serverId = index.getServerId();
			if (serverId != getZeze().getConfig().getServerId())
				Transaction.whileCommit(() -> tryRedirectCancel(serverId, timerId));
			TimerHandle handle = null;
			var nodeId = index.getNodeId();
			var node = _tNodes.get(nodeId);
			if (node != null) {
				var timer = node.getTimers().get(timerId);
				if (timer != null)
					handle = findTimerHandleOrNull(timer.getHandleName());
			}
			cancel(serverId, timerId, nodeId, node, handle);
			return true;
		}
		// 查无索引不得取消 future，避免误杀共用 timerFutures 的在线族同名定时器
		return false;
	}

	tAccountTimers tAccountTimers() {
		return _tAccountTimers;
	}

	tIndexs tIndexs() {
		return _tIndexs;
	}

	/**
	 * 命名 timerId 是否被在线族占用（逐 onlineSet 查角色在线表及账号在线表）。
	 */
	public boolean isOnlineTimerIdOccupied(String timerId) {
		if (_tAccountTimers.get(timerId) != null)
			return true;
		if (defaultOnline != null) {
			var occupied = new boolean[1];
			defaultOnline.getProviderWithOnline().foreachOnline(
					online -> occupied[0] |= online.getTimerRole().isOnlineTimerOccupied(timerId));
			return occupied[0];
		}
		return false;
	}

	/**
	 * 命名 timerId 全局唯一查重（检查 _tIndexs 及全部在线族存储表）。
	 */
	public boolean isNamedTimerIdOccupied(String timerId) {
		return _tIndexs.get(timerId) != null || isOnlineTimerIdOccupied(timerId);
	}

	tNodes tNodes() {
		return _tNodes;
	}

	tAccountOfflineTimers tAccountOfflineTimers() {
		return _tAccountOfflineTimers;
	}

	public TimerAccount getAccountTimer() {
		if (timerAccount == null)
			throw new IllegalStateException("account timer not initialized. please call initializeOnlineTimer first.");
		return timerAccount;
	}

	public @Nullable Online getDefaultOnline() {
		return defaultOnline;
	}

	public TimerRole getRoleTimer() {
		if (defaultOnline == null)
			throw new IllegalStateException("role timer not initialized. please call initializeOnlineTimer first.");
		return defaultOnline.getTimerRole();
	}

	public TimerRole getRoleTimer(@Nullable String onlineSetName) {
		if (defaultOnline == null)
			throw new IllegalStateException("role timer not initialized. please call initializeOnlineTimer first.");
		var online = defaultOnline.getOnline(onlineSetName);
		if (online == null)
			throw new IllegalStateException("online miss " + onlineSetName);
		return online.getTimerRole();
	}

	/**
	 * 获取指定角色的定时器门面，默认onlineSet。
	 * 通过返回对象的 online()/offline() 选择定时器类型。
	 */
	public RoleTimers roles(long roleId) {
		return new RoleTimers(getRoleTimer(), roleId);
	}

	/**
	 * 获取指定角色的定时器门面，指定onlineSet。
	 * 通过返回对象的 online()/offline() 选择定时器类型。
	 */
	public RoleTimers roles(String onlineSetName, long roleId) {
		return new RoleTimers(getRoleTimer(onlineSetName), roleId);
	}

	/**
	 * 获取指定账号的定时器门面。
	 * 通过返回对象的 online()/offline() 选择定时器类型。
	 */
	public AccountTimers accounts(String account, String clientId) {
		return new AccountTimers(getAccountTimer(), account, clientId);
	}

	public @Nullable BIndex getTimerIndex(String timerId) {
		return _tIndexs.get(timerId);
	}

	public @Nullable BTimer getTimer(String timerId) {
		var index = _tIndexs.get(timerId);
		if (index == null)
			return null;
		var node = _tNodes.get(index.getNodeId());
		return node != null ? node.getTimers().get(timerId) : null;
	}

	// ///////////////////////////////////////////////////////////
	// 内部实现
	protected void tryRedirectCancel(int serverId, String timerId) {
		// redirect 现在仅取消future，总是尝试，不检查其他参数。
		var providerApp = getZeze().getProviderApp();
		if (providerApp == null || providerApp.providerDirectService == null)
			return;
		if (getZeze().getConfig().getServerId() != serverId
				&& providerApp.providerDirectService.providerByServerId.containsKey(serverId)) {
			redirectCancel(serverId, timerId);
		}
	}

	@TransactionLevelAnnotation(Level = TransactionLevel.None)
	@RedirectToServer
	protected void redirectCancel(int serverId, String timerId) {
		// redirect 现在仅取消future，总是尝试，不检查其他参数。
		cancelFuture(timerId);
	}

	void installFuture(String timerId, long delay, boolean putIfAbsent, TaskSpec<Long> task) {
		Future<?> previous;
		synchronized (timerFutures) {
			if (!started || (putIfAbsent && timerFutures.containsKey(timerId)))
				return;
			previous = timerFutures.put(timerId, task.scheduleNow(delay));
		}
		if (previous != null)
			previous.cancel(false);
	}

	void cancelFuture(String timerId) {
		var local = timerFutures.remove(timerId);
		if (local != null)
			local.cancel(false);
	}

	// 仅对本进程 root 执行写路径 fence；stamp==0（新行或被接管后墓碑）在 on 模式下认领为 myEpoch
	private void checkTimerFence(int serverId, BNodeRoot root) {
		var takeover = getZeze().getTakeoverOrNull();
		if (takeover != null && serverId == getZeze().getConfig().getServerId()) {
			takeover.requireScoped(timerTakeoverScope);
			var stamp = root.getLoadSerialNo();
			if (stamp == 0 && Takeover.ModeOn.equals(takeover.getMode())) {
				stamp = takeover.getMyEpoch();
				root.setLoadSerialNo(stamp);
			}
			takeover.checkFence(timerTakeoverScope, stamp);
		}
	}

	private void unlinkEmptyNode(int serverId, long nodeId, BNode node) {
		var prevNodeId = node.getPrevNodeId();
		var nextNodeId = node.getNextNodeId();
		var prev = _tNodes.get(prevNodeId);
		if (prev != null && prev.getNextNodeId() == nodeId)
			prev.setNextNodeId(nextNodeId);
		var next = _tNodes.get(nextNodeId);
		if (next != null && next.getPrevNodeId() == nodeId)
			next.setPrevNodeId(prevNodeId);
		// 对 index.serverId 与本进程 root 幂等修正边界（接管装载窗口内 index.serverId 可能仍指向死者）
		var indexRoot = _tNodeRoot.get(serverId);
		if (indexRoot != null)
			checkTimerFence(serverId, indexRoot);
		fixRootBoundary(indexRoot, nodeId, prevNodeId, nextNodeId);
		var myRoot = _tNodeRoot.get(getZeze().getConfig().getServerId());
		if (myRoot != indexRoot)
			fixRootBoundary(myRoot, nodeId, prevNodeId, nextNodeId);
		// 延迟回收空节点，避免并发访问时立即删除引发问题
		_tNodes.delayRemove(nodeId);
	}

	private void cancel(int serverId, String timerId, long nodeId, @Nullable BNode node,
	                    @Nullable TimerHandle handle) {
		// 事务成功时，总是尝试cancel future
		Transaction.whileCommit(() -> cancelFuture(timerId));
		_tIndexs.remove(timerId);
		if (node != null) {
			var timers = node.getTimers();
			var bTimer = timers.remove(timerId);
			if (timers.isEmpty())
				unlinkEmptyNode(serverId, nodeId, node);
			if (handle != null && bTimer != null) {
				var rc = TaskSpec.ofProcedure(getZeze().newProcedure(() -> {
					handle.onTimerCancel(bTimer);
					return 0;
				}, "Timer.fireTimerCancel")).call();
				if (rc != 0) // 嵌套过程失败仅记录：取消路径已不可回滚，日志留给排障
					logger.warn("Timer.fireTimerCancel procedure rc={}", rc);
			}
		}
	}

	// 幂等链边界修正：仅当root的head/tail确实指向被摘除的节点才改写（不匹配不动），
	// 因此对多个root重复调用安全（cancel的接管窗口修正依赖这一点）。
	private static void fixRootBoundary(@Nullable BNodeRoot root, long nodeId, long prevNodeId, long nextNodeId) {
		if (root == null)
			return;
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

	// fire按oneByOneKey派发：空/null直跑，非空包executeOneByOne（同key串行）。
	// 全局族（scheduleSimple/scheduleCronNext/loadTimer）与在线族（TimerOnlineBase安装）共用。
	static void dispatchFire(@Nullable String oneByOneKey, Runnable fire) {
		if (oneByOneKey == null || oneByOneKey.isEmpty())
			fire.run();
		else
			TaskSpec.ofAction(fire::run).executeOneByOne(oneByOneKey);
	}

	private void scheduleSimple(long timerSerialId, int serverId, String timerId, long delay,
	                            long concurrentSerialNo, boolean putIfAbsent, @Nullable String oneByOneKey) {
		Transaction.whileCommit(() -> installFuture(timerId, delay, putIfAbsent,
				TaskSpec.ofAction(() -> dispatchFire(oneByOneKey,
						() -> fireSimple(timerSerialId, serverId, timerId, concurrentSerialNo, false)))));
	}

	private void cancelFailedTimer(String timerId, long timerSerialId) {
		TaskSpec.ofProcedure(getZeze().newProcedure(() -> {
			// 失败的 fire 已释放锁，注册条目可能已被替换或接管，须核对 serialId 与 serverId 后再取消
			var index = _tIndexs.get(timerId);
			if (index != null && index.getSerialId() == timerSerialId
					&& index.getServerId() == getZeze().getConfig().getServerId())
				cancel(timerId);
			return 0;
		}, "Timer.cancelTimer")).call();
	}

	private void fireSimple(long timerSerialId, int serverId, String timerId, long concurrentSerialNo,
	                        boolean missfire) {
		if (!started)
			return; // 已派发的任务可能在停机后才到达触发入口。
		if (TaskSpec.ofProcedure(getZeze().newProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (index == null)
				return 0; // 注册已取消，timerId可能已被其他族复用，不能清其future。
			if (index.getServerId() != getZeze().getConfig().getServerId()) { // 不是拥有者，取消本地调度。
				Transaction.whileCommit(() -> cancelFuture(timerId));
				return 0;
			}
			if (index.getSerialId() != timerSerialId)
				return 0; // 旧fire已排队；同名新timer的future不能按timerId取消。

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
			if (simpleTimer.getEndTime() > 0 && simpleTimer.getNextExpectedTime() > simpleTimer.getEndTime()) {
				cancel(serverId, timerId, nodeId, node, handle);
				return 0; // 兼容旧数据：endTime约束预定触发时间，过界首发也不能执行用户回调。
			}
			if (concurrentSerialNo == timer.getConcurrentFireSerialNo()) {
				SimpleTimerSpec.beforeCallSimpleTimer(simpleTimer, missfire);
				var context = new TimerContext(this, timer, simpleTimer.getHappenTimes(),
						simpleTimer.getExpectedTime(), simpleTimer.getNextExpectedTime());

				// 当调度发生了错误或者由于异步时序没有原子保证，导致同时（或某个瞬间）在多个Server进程调度时，
				// 这个系列号保证触发用户回调只会发生一次。这个并发问题不取消定时器，继续尝试调度（去争抢执行权）。
				// 定时器的调度生命期由其他地方保证最终一致。如果保证发生了错误，将一致并发争抢执行权。
				var serialSaved = index.getSerialId();
				var ret = TaskSpec.ofProcedure(getZeze().newProcedure(() -> {
					handle.onTimer(context);
					return 0;
				}, "Timer.fireSimpleUser." + timer.getHandleName())).call();

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
				SimpleTimerSpec.applyCallbackNextTime(simpleTimer, context.nextExpectedTimeMills);
				if (simpleTimer.getNextExpectedTime() == 0) {
					cancel(serverId, timerId, nodeId, node, handle);
					return 0;
				}
			}
			// else 发生了并发执行争抢，也需要再次进行本地调度。此时直接使用simpleTimer中的值，不需要再次进行计算。

			// continue period
			scheduleSimple(timerSerialId, serverId, timerId,
					Math.max(simpleTimer.getNextExpectedTime() - System.currentTimeMillis(), 1),
					concurrentSerialNo + 1, false, simpleTimer.getOneByOneKey());
			return 0;
		}, "Timer.fireSimple")).call() != 0)
			cancelFailedTimer(timerId, timerSerialId);
	}

	private void scheduleCron(long timerSerialId, int serverId, String timerId, BCronTimer cron,
	                          long concurrentSerialNo, boolean putIfAbsent, @Nullable String oneByOneKey) {
		try {
			long delay = Math.max(cron.getNextExpectedTime() - System.currentTimeMillis(), 1);
			scheduleCronNext(timerSerialId, serverId, timerId, delay, concurrentSerialNo, putIfAbsent, oneByOneKey);
		} catch (Exception ex) {
			// 此处错误无法进一步处置，仅记录日志。
			logger.error("", ex);
		}
	}

	private void scheduleCronNext(long timerSerialId, int serverId, String timerId, long delay,
	                              long concurrentSerialNo, boolean putIfAbsent, @Nullable String oneByOneKey) {
		Transaction.whileCommit(() -> installFuture(timerId, delay, putIfAbsent,
				TaskSpec.ofAction(() -> dispatchFire(oneByOneKey,
						() -> fireCron(timerSerialId, serverId, timerId, concurrentSerialNo, false)))));
	}

	private void fireCron(long timerSerialId, int serverId, String timerId, long concurrentSerialNo,
	                      boolean missfire) {
		if (!started)
			return; // 已派发的任务可能在停机后才到达触发入口。
		if (TaskSpec.ofProcedure(getZeze().newProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (index == null)
				return 0; // 注册已取消，timerId可能已被其他族复用，不能清其future。
			if (index.getServerId() != getZeze().getConfig().getServerId()) { // 不是拥有者，取消本地调度。
				Transaction.whileCommit(() -> cancelFuture(timerId));
				return 0;
			}
			if (index.getSerialId() != timerSerialId)
				return 0; // 旧fire已排队；同名新timer的future不能按timerId取消。

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
			if (cronTimer.getEndTime() > 0 && cronTimer.getNextExpectedTime() > cronTimer.getEndTime()) {
				cancel(serverId, timerId, nodeId, node, handle);
				return 0;
			}
			if (concurrentSerialNo == timer.getConcurrentFireSerialNo()) {
				var hasNext = CronTimerSpec.nextCronTimer(cronTimer, missfire);
				var context = new TimerContext(this, timer, cronTimer.getHappenTimes(),
						cronTimer.getExpectedTime(), cronTimer.getNextExpectedTime());

				// 并发触发防护同fireSimple（见其serialId说明）：serialId保证用户回调只发生一次，
				// 并发争抢不取消定时器，继续尝试调度（去争抢执行权）。
				var serialSaved = index.getSerialId();
				var ret = TaskSpec.ofProcedure(getZeze().newProcedure(() -> {
					handle.onTimer(context);
					return 0;
				}, "Timer.fireCronUser." + timer.getHandleName())).call();

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
					Math.max(cronTimer.getNextExpectedTime() - System.currentTimeMillis(), 1),
					concurrentSerialNo + 1, false, cronTimer.getOneByOneKey());
			return 0;
		}, "Timer.fireCron")).call() != 0)
			cancelFailedTimer(timerId, timerSerialId);
	}

	private void loadTimer() {
		var serverId = getZeze().getConfig().getServerId();
		// 接管作用域已在ctor注册（claim前），stamp由Takeover.start()完成；死者链表由
		// takeover.tryTransfer在同一事务内裁决+搬运，afterTransfer里对搬来的链重调度本地。
		var headNodeId = new long[1];
		var r = TaskSpec.ofProcedure(getZeze().newProcedure(() -> {
			headNodeId[0] = _tNodeRoot.getOrAdd(serverId).getHeadNodeId();
			return 0;
		}, "Timer.loadTimerLocal")).call();
		if (r != Procedure.Success)
			throw new IllegalStateException("Timer.loadTimerLocal failed: r=" + r);
		// last也填头节点是因为链表是循环的；headNodeId为0即空扫描（失败路径等价于原来的跳过调度）
		loadTimer(headNodeId[0], headNodeId[0]);
	}

	/**
	 * 本进程定时器双链表的接管作用域（tNodeRoot[serverId]）：负责 epoch 盖戳、搬运死者链（高版本 veto）与事务后本地重调度。
	 */
	private final class TimerTakeoverScope implements TakeoverScope {
		private long transferredFirst; // 事务内写入、afterTransfer 消费（由 tryTransfer 串行保证无并发）
		private long transferredLast;

		@Override
		public String name() {
			return "Timer";
		}

		@Override
		public void stamp(long epoch) {
			_tNodeRoot.getOrAdd(getZeze().getConfig().getServerId()).setLoadSerialNo(epoch);
		}

		@Override
		public long transferAll(int deadServerId, long deadEpoch) {
			// 先重置，避免回滚事务的残留值泄漏到后续 afterTransfer
			transferredFirst = 0;
			transferredLast = 0;
			var src = _tNodeRoot.get(deadServerId);
			long srcHeadNodeId, srcTailNodeId;
			if (src == null || (srcHeadNodeId = src.getHeadNodeId()) == 0 || (srcTailNodeId = src.getTailNodeId()) == 0)
				return 0;
			if (src.getVersion() > getZeze().getConfig().getAppVersion())
				return -1; // 高版本数据不接管，返回 veto 留给高版本进程
			var srcHead = _tNodes.get(srcHeadNodeId);
			var srcTail = _tNodes.get(srcTailNodeId);
			if (srcHead == null || srcTail == null)
				throw new IllegalStateException("maybe operate before timer created.");

			var root = _tNodeRoot.getOrAdd(getZeze().getConfig().getServerId());
			root.setVersion(Math.max(root.getVersion(), src.getVersion()));
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
			src.setLoadSerialNo(0); // 清零墓碑，供复活者认领空链继续写
			transferredFirst = srcHeadNodeId;
			transferredLast = headNodeId;
			return 1;
		}

		@Override
		public void afterTransfer(int deadServerId) {
			if (transferredFirst == 0)
				return;
			var first = transferredFirst;
			var last = transferredLast;
			transferredFirst = 0;
			transferredLast = 0;
			loadTimer(first, last);
		}
	}

	private final TimerTakeoverScope timerTakeoverScope = new TimerTakeoverScope();

	// 如果存在node，至少执行一次循环。
	private void loadTimer(long first, long last) {
		if (first == 0 && last == 0)
			return;
		var idSet = new LongHashSet();
		var node = new OutLong(first);
		do {
			var nodeId = node.value;
			if (!idSet.add(nodeId)) // 检测并避免死循环
				break;
			var r = TaskSpec.ofProcedure(getZeze().newProcedure(() -> {
				loadTimer(node, last);
				return 0;
			}, "Timer.loadTimer")).call();
			if (r != Procedure.Success)
				throw new IllegalStateException("Timer.loadTimer failed: r=" + r + ", nodeId=" + nodeId);
		} while (node.value != last);
	}

	private void loadTimer(OutLong nodeId, long last) {
		var node = _tNodes.get(nodeId.value);
		if (node == null) {
			logger.warn("loadTimer not found nodeId={}", nodeId.value);
			nodeId.value = last; // 马上结束外面的循环。last仅用在这里。
			return;
		}
		// 仅事务提交成功后推进游标，确保 redo 时不跳过当前节点
		var nextNodeId = node.getNextNodeId();
		Transaction.whileCommit(() -> nodeId.value = nextNodeId);

			var now = System.currentTimeMillis();
			var appVer = getZeze().getConfig().getAppVersion();
			var serverId = getZeze().getConfig().getServerId();
			var orphanRemoved = false;
			var nodeUnlinked = false;
			for (var it = node.getTimers().values().iterator(); it.hasNext(); ) {
				var timer = it.next();
				try {
					var index = _tIndexs.get(timer.getTimerName());
					if (index == null) {
						it.remove();
						orphanRemoved = true;
						continue;
					}
					if (index.getVersion() > appVer) // 无法保证高版本定时器的处理,等待各模块启动后重建定时器
						continue;

					// 须在 schedule 之前更新归属 serverId，防止断点恢复后部分条目仍保留旧 serverId
					if (index.getServerId() != serverId)
						index.setServerId(serverId);
					// 异步补触发仅捕获本轮快照，避免 worker 读取已被同名替换的受管 bean
					var serialId = index.getSerialId();
					var timerName = timer.getTimerName();
					var concurrentSerialNo = timer.getConcurrentFireSerialNo();
					if (timer.getTimerObj().getBean().typeId() == BSimpleTimer.TYPEID) {
						var simpleTimer = (BSimpleTimer)timer.getTimerObj().getBean();
						if (simpleTimer.getNextExpectedTime() < now) {
							switch (simpleTimer.getMissfirePolicy()) {
							case eMissfirePolicyRunOnce:
							case eMissfirePolicyRunOnceOldNext: {
								var oneByOneKey = simpleTimer.getOneByOneKey();
								// 用事务感知的 run() 在提交后入池执行补触发，不可在 whileCommit 线程内同步跑 fire
								TaskSpec.ofAction(() ->
										dispatchFire(oneByOneKey, () ->
												fireSimple(serialId, serverId, timerName, concurrentSerialNo, true))).run();
								continue;
							}

							case eMissfirePolicyNothing:
								// 重置启动时间，调度下一个（未来）间隔的时间。没有考虑对齐。
								var period = simpleTimer.getPeriod();
								if (period > Long.MAX_VALUE - now) {
									cancel(serverId, timer.getTimerName(), nodeId.value, node,
											findTimerHandleOrNull(timer.getHandleName()));
									if (node.getTimers().isEmpty())
										nodeUnlinked = true;
									continue; // 下个预定时刻溢出，取消调度
								}
								simpleTimer.setNextExpectedTime(period > 0 ? now + period : now);
								//TODO: 考虑nextExpectedTime超过endTime的情况要不要取消
								break;

							default:
								throw new UnsupportedOperationException("Unknown MissfirePolicy: "
										+ simpleTimer.getMissfirePolicy());
							}
						}
						scheduleSimple(index.getSerialId(), serverId, timer.getTimerName(),
								Math.max(simpleTimer.getNextExpectedTime() - now, 1), timer.getConcurrentFireSerialNo(),
								true, simpleTimer.getOneByOneKey());
					} else {
						var cronTimer = (BCronTimer)timer.getTimerObj().getBean();
						if (cronTimer.getNextExpectedTime() < now) {
							switch (cronTimer.getMissfirePolicy()) {
							case eMissfirePolicyRunOnce:
							case eMissfirePolicyRunOnceOldNext: {
								var oneByOneKey = cronTimer.getOneByOneKey();
								// 提交后入池执行补触发，不在 whileCommit 回调内同步运行
								TaskSpec.ofAction(() ->
										dispatchFire(oneByOneKey, () ->
												fireCron(serialId, serverId, timerName, concurrentSerialNo, true))).run();
								continue;
							}

							case eMissfirePolicyNothing: {
								// 计算下一次（未来）发生的时间。
								try {
									cronTimer.setNextExpectedTime(CronTimerSpec.cronNextTime(cronTimer.getCronExpression(), now));
								} catch (IllegalArgumentException e) {
									// 表达式已无后续可行时间，转为 ParseException 走下方坏数据统一取消路径
									throw new ParseException("cron expression has no next valid time: "
											+ cronTimer.getCronExpression(), 0);
								}
								//TODO: 考虑nextExpectedTime超过endTime的情况要不要取消
								break;
							}
							default:
								throw new UnsupportedOperationException("Unknown MissfirePolicy: "
										+ cronTimer.getMissfirePolicy());
							}
						}
						scheduleCron(index.getSerialId(), serverId, timer.getTimerName(), cronTimer,
								timer.getConcurrentFireSerialNo(), true, cronTimer.getOneByOneKey());
					}
				} catch (ParseException | UnsupportedOperationException e) {
					// 确定性坏数据走统一 cancel 路径，同步清理索引、节点、future 及离线簿记，不影响同节点其他定时器
					logger.error("loadTimer: cancel exhausted/corrupted timer. nodeId={}, timerName={}, bean={}",
							nodeId.value, timer.getTimerName(), timer.getTimerObj().getBean(), e);
					cancel(serverId, timer.getTimerName(), nodeId.value, node,
							findTimerHandleOrNull(timer.getHandleName()));
					if (node.getTimers().isEmpty())
						nodeUnlinked = true;
				}
			}
			if (orphanRemoved && !nodeUnlinked && node.getTimers().isEmpty())
				unlinkEmptyNode(serverId, nodeId.value, node);
		}
}
