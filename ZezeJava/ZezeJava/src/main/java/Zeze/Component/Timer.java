package Zeze.Component;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderImplement;
import Zeze.Arch.ProviderWithOnline;
import Zeze.Arch.RedirectToServer;
import Zeze.Builtin.Timer.*;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.CronExpression;

public class Timer extends AbstractTimer {
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.getSpecialTypeIdFromBean(bean);
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static final int CountPerNode = 200;

	static final Logger logger = LogManager.getLogger(Timer.class);
	public final Zeze.Application zeze;
	private AutoKey nodeIdAutoKey;
	AutoKey timerIdAutoKey;
	// 在这台服务器进程内调度的所有Timer。key是timerId，value是ThreadPool.schedule的返回值。
	final ConcurrentHashMap<String, Future<?>> timersFuture = new ConcurrentHashMap<>();

	public Timer(Zeze.Application zeze) {
		this.zeze = zeze;
		RegisterZezeTables(zeze);
	}

	void register(Class<? extends Bean> cls) {
		beanFactory.register(cls);
		_tCustomClasses.getOrAdd(1).getCustomClasses().add(cls.getName());
	}

	@SuppressWarnings("unchecked")
	private long loadCustomClass() throws ClassNotFoundException {
		var classes = _tCustomClasses.getOrAdd(1);
		for (var cls : classes.getCustomClasses()) {
			beanFactory.register((Class<? extends Bean>)Class.forName(cls));
		}
		return 0L;
	}

	/**
	 * 非事务环境调用。用于启动Timer服务。
	 * @throws Throwable 可能抛出任何异常。一般框架处理。调用者不需要捕捉。
	 */
	public void start() throws Throwable {
		nodeIdAutoKey = zeze.getAutoKey("Zeze.Component.Timer.NodeId");
		timerIdAutoKey = zeze.getAutoKey("Zeze.Component.Timer.TimerId");
		if (0L != zeze.newProcedure(this::loadCustomClass, "").call()) {
			throw new IllegalStateException("Load Item Classes Failed.");
		}
		Task.run(this::loadTimer, "LoadTimerLocal");
	}

	/**
	 * 初始化在线Timer。在线Timer需要ProviderApp。
	 * @param providerApp 参数
	 */
	public void initializeOnlineTimer(ProviderApp providerApp) {
		ProviderImplement impl;
		if (null != providerApp && null != (impl = providerApp.providerImplement)) {
			if (impl instanceof ProviderWithOnline)
				timerAccount = new TimerAccount(((ProviderWithOnline)impl).online);
			else if (impl instanceof Zeze.Game.ProviderImplementWithOnline)
				timerRole = new TimerRole(((Zeze.Game.ProviderImplementWithOnline)impl).online);
		}
	}

	/**
	 * 停止Timer服务。
	 * @throws Throwable 抛出任何异常。
	 */
	public void stop() throws Throwable {
		UnRegisterZezeTables(this.zeze);
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
	 * @param delay 延迟
	 * @param period 间隔
	 * @param handle Timer处理Class
	 * @param customData 自定义数据
	 * @return TimerId
	 */
	public String schedule(long delay, long period, Class<? extends TimerHandle> handle, Bean customData) {
		return schedule(delay, period, -1, handle, customData);
	}

	/**
	 * 调度一个Timeout，即仅执行一次的Timer。
	 * 需要在事务内使用。
	 * @param delay 延迟
	 * @param handle Timer处理Class
	 * @param customData 自定义数据
	 * @return TimerId
	 */
	public String schedule(long delay, Class<? extends TimerHandle> handle, Bean customData) {
		return schedule(delay, -1, 1, handle, customData);
	}

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 * @param delay 延迟
	 * @param period 间隔
	 * @param times 最大触发次数
	 * @param handle Timer处理Class
	 * @param customData 自定义数据
	 * @return TimerId
	 */
	public String schedule(long delay, long period, long times, Class<? extends TimerHandle> handle, Bean customData) {
		return schedule(delay, period, times, -1, handle, customData);
	}

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 * @param delay 延迟
	 * @param period 间隔
	 * @param times 最大触发次数
	 * @param endTime 结束时间
	 * @param handle Timer处理Class
	 * @param customData 自定义数据
	 * @return TimerId
	 */
	public String schedule(long delay, long period, long times, long endTime, Class<? extends TimerHandle> handle, Bean customData) {
		return schedule(delay, period, times, endTime, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个Timer。
	 * 需要在事务内使用。
	 * @param delay 延迟
	 * @param period 间隔
	 * @param times 最大触发次数
	 * @param endTime 结束时间
	 * @param missfirePolicy 错失触发策略
	 * @param handle Timer处理Class
	 * @param customData 自定义数据
	 * @return TimerId
	 */
	public String schedule(long delay, long period, long times, long endTime, int missfirePolicy, Class<? extends TimerHandle> handle, Bean customData) {
		var simpleTimer = new BSimpleTimer();
		initSimpleTimer(simpleTimer, delay, period, times, endTime);
		simpleTimer.setMissfirePolicy(missfirePolicy);
		return schedule(simpleTimer, handle, customData);
	}

	// 直接传递BSimpleTimer，需要自己调用它Timer.initSimpleTimer初始化。所以暂时不开放了。
	private String schedule(BSimpleTimer simpleTimer, Class<? extends TimerHandle> handle, Bean customData) {
		// auto name
		return schedule("@" + timerIdAutoKey.nextString(), simpleTimer, handle, customData);
	}

	private String schedule(String timerId, BSimpleTimer simpleTimer, Class<? extends TimerHandle> handle, Bean customData) {
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
					head.setPrevNodeId(nodeId);
					node.setNextNodeId(root.getHeadNodeId());
					node.setPrevNodeId(root.getTailNodeId());
					root.setHeadNodeId(nodeId);
					var tailNode = _tNodes.get(root.getTailNodeId());
					tailNode.setNextNodeId(root.getHeadNodeId());
				}
			}

			if (node.getTimers().size() < CountPerNode) {
				var index = new BIndex();
				index.setServerId(serverId);
				index.setNodeId(nodeId);
				_tIndexs.tryAdd(timerId, index);

				var timer = new BTimer();
				timer.setTimerName(timerId);
				timer.setHandleName(handle.getName());
				timer.setTimerObj(simpleTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					register(customData.getClass());
					timer.getCustomData().setBean(customData);
				}

				Transaction.whileCommit(() -> scheduleSimple(serverId, timerId,
						simpleTimer.getExpectedTime() - System.currentTimeMillis(),
						timer.getConcurrentFireSerialNo()));
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
	 * @param monthDay 月内第几天
	 * @param hour 小时
	 * @param minute 分钟
	 * @param second 秒
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return TimerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public String scheduleMonth(int monthDay, int hour, int minute, int second,
								Class<? extends TimerHandle> handle, Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " " + monthDay + " * ?";
		return schedule(cron, handle, customData);
	}

	/**
	 * 每周第N(weekDay)天的某个时刻(hour, minute, second)。
	 * 需要在事务内使用。
	 * @param weekDay 一周的第几天
	 * @param hour 小时
	 * @param minute 分钟
	 * @param second 秒
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return TimerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public String scheduleWeek(int weekDay, int hour, int minute, int second,
							   Class<? extends TimerHandle> handle, Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " * * " + weekDay;
		return schedule(cron, handle, customData);
	}

	/**
	 * 每天的某个时刻(hour, minute, second)。
	 * 需要在事务内使用。
	 * @param hour 小时
	 * @param minute 分钟
	 * @param second 秒
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return TimerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public String scheduleDay(int hour, int minute, int second,
							  Class<? extends TimerHandle> handle, Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " * * ?";
		return schedule(cron, handle, customData);
	}

	/**
	 * 调度一个根据Cron表达式描述的Timer。
	 * 需要在事务内使用。
	 * @param cronExpression cron 表达式
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return TimerId
	 * @throws ParseException CronTimer表达式解析异常
	 */
	public String schedule(String cronExpression,
						   Class<? extends TimerHandle> handle, Bean customData) throws ParseException {
		return schedule(cronExpression, -1, -1, handle, customData);
	}

	/**
	 * 调度一个根据Cron表达式描述的Timer。
	 * 需要在事务内使用。
	 * @param cronExpression cron 表达式
	 * @param times 次数限制
	 * @param endTime 结束时间限制
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return TimerId
	 * @throws ParseException cron解析异常
	 */
	public String schedule(String cronExpression, long times, long endTime,
						   Class<? extends TimerHandle> handle, Bean customData) throws ParseException {
		return schedule(cronExpression, times, endTime, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个根据Cron表达式描述的Timer。
	 * 需要在事务内使用。
	 * @param cronExpression cron 表达式
	 * @param times 次数限制
	 * @param endTime 结束时间限制
	 * @param missfirePolicy 触发丢失处理策略
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return TimerId
	 * @throws ParseException cron解析异常
	 */
	public String schedule(String cronExpression, long times, long endTime, int missfirePolicy,
						   Class<? extends TimerHandle> handle, Bean customData) throws ParseException {
		var cronTimer = new BCronTimer();
		initCronTimer(cronTimer, cronExpression, times, endTime);
		cronTimer.setMissfirePolicy(missfirePolicy);
		return schedule(cronTimer, handle, customData);
	}

	// 直接传递BCronTimer需要自动调用Timer.initCronTimer初始化。先不开放了。
	private String schedule(BCronTimer cronTimer,
							Class<? extends TimerHandle> name, Bean customData) {
		return schedule("@" + timerIdAutoKey.nextString(), cronTimer, name, customData);
	}

	private String schedule(String timerId, BCronTimer cronTimer,
							Class<? extends TimerHandle> name, Bean customData) {
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
					head.setPrevNodeId(nodeId);
					node.setNextNodeId(root.getHeadNodeId());
					node.setPrevNodeId(root.getTailNodeId());
					root.setHeadNodeId(nodeId);
					var tailNode = _tNodes.get(root.getTailNodeId());
					tailNode.setNextNodeId(root.getHeadNodeId());
				}
			}

			if (node.getTimers().size() < CountPerNode) {
				var index = new BIndex();
				index.setServerId(serverId);
				index.setNodeId(nodeId);
				_tIndexs.insert(timerId, index);

				var timer = new BTimer();
				timer.setTimerName(timerId);
				timer.setHandleName(name.getName());
				timer.setTimerObj(cronTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					register(customData.getClass());
					timer.getCustomData().setBean(customData);
				}

				Transaction.whileCommit(() -> scheduleCron(serverId, timerId, cronTimer, timer.getConcurrentFireSerialNo()));
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
	 * @param timerId 名字
	 * @param delay 延迟
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(String timerId, long delay,
								 Class<? extends TimerHandle> handle, Bean customData) {
		return scheduleNamed(timerId, delay, -1, -1, -1, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 * @param timerId 名字
	 * @param delay 延迟
	 * @param period 间隔
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(String timerId, long delay, long period,
								 Class<? extends TimerHandle> handle, Bean customData) {
		return scheduleNamed(timerId, delay, period, -1, -1, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 * @param timerId 名字
	 * @param delay 延迟
	 * @param period 间隔
	 * @param times 次数
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(String timerId, long delay, long period, long times,
								 Class<? extends TimerHandle> handle, Bean customData) {
		return scheduleNamed(timerId, delay, period, times, -1, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 * @param timerId 名字
	 * @param delay 延迟
	 * @param period 间隔
	 * @param times 次数
	 * @param endTime 结束时间
	 * @param missfirePolicy 触发丢失策略
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 */
	public boolean scheduleNamed(String timerId, long delay, long period, long times, long endTime, int missfirePolicy,
								 Class<? extends TimerHandle> handle, Bean customData) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timer name. startsWith '@' is reserved.");

		var index = _tIndexs.get(timerId);
		if (null != index)
			return false;

		var simpleTimer = new BSimpleTimer();
		initSimpleTimer(simpleTimer, delay, period, times, endTime);
		simpleTimer.setMissfirePolicy(missfirePolicy);
		schedule(timerId, simpleTimer, handle, customData);
		return true;
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 * @param timerName 名字，即TimerId
	 * @param cron cron 表达式
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 * @throws ParseException cron解析异常
	 */
	public boolean scheduleNamed(String timerName, String cron,
								 Class<? extends TimerHandle> handle, Bean customData) throws ParseException {
		return scheduleNamed(timerName, cron, -1, -1, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 * @param timerName 名字，即TimerId
	 * @param cron cron 表达式
	 * @param times 次数
	 * @param endTime 结束时间
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 * @throws ParseException cron解析异常
	 */
	public boolean scheduleNamed(String timerName, String cron, long times, long endTime,
								 Class<? extends TimerHandle> handle, Bean customData) throws ParseException {
		return scheduleNamed(timerName, cron, times, endTime, eMissfirePolicyNothing, handle, customData);
	}

	/**
	 * 调度一个有名的Timer。
	 * 需要在事务内调用。
	 * @param timerName 名字，即TimerId
	 * @param cron cron 表达式
	 * @param times 次数
	 * @param endTime 结束时间
	 * @param missfirePolicy 触发丢失策略
	 * @param handle 回调class
	 * @param customData 自定义数据
	 * @return 调度是否成功
	 * @throws ParseException cron解析异常
	 */
	public boolean scheduleNamed(String timerName, String cron, long times, long endTime, int missfirePolicy,
								 Class<? extends TimerHandle> handle, Bean customData) throws ParseException {
		var timerId = _tIndexs.get(timerName);
		if (null != timerId)
			return false;
		var cronTimer = new BCronTimer();
		initCronTimer(cronTimer, cron, times, endTime);
		cronTimer.setMissfirePolicy(missfirePolicy);
		schedule(timerName, cronTimer, handle, customData);
		return true;
	}

	/**
	 * 取消一个具体的Timer实例。
	 * 需要在事务内调用。
	 * @param timerId timerId
	 */
	public void cancel(String timerId) {
		/*
		try {
			// XXX 统一通过这里取消定时器，可能会浪费一次内存表查询。
			// 还是让账户相关Timer自己取消吧。
			if (null != timerRole && timerRole.cancel(timerId))
				return; // done

			if (null != timerAccount && timerAccount.cancel(timerId))
				return; // done

		} catch (Throwable ex) {
			logger.error("ignore cancel error.", ex);
			return; // done;
		}
		*/

		var index = _tIndexs.get(timerId);
		if (null == index) {
			// 尽可能的执行取消操作，不做严格判断。
			cancel(zeze.getConfig().getServerId(), timerId, null, null);
			return;
		}

		redirectCancel(index.getServerId(), timerId);
	}

	///////////////////////////////////////////////////////////////////////////////
	// Online Timer
	tAccountTimers tAccountTimers() {
		return _tAccountTimers;
	}

	tRoleTimers tRoleTimers() {
		return _tRoleTimers;
	}

	tAccountOfflineTimers tAccountOfflineTimers() {
		return _tAccountOfflineTimers;
	}

	tRoleOfflineTimers tRoleOfflineTimers() {
		return _tRoleOfflineTimers;
	}

	private TimerAccount timerAccount;
	private TimerRole timerRole;

	public TimerAccount getAccountTimer() {
		return timerAccount;
	}

	public TimerRole getRoleTimer() {
		return timerRole;
	}

	/////////////////////////////////////////////////////////////
	// 内部实现
	@RedirectToServer
	protected void redirectCancel(int serverId, String timerId) {
		// 尽可能的执行取消操作，不做严格判断。
		var index = _tIndexs.get(timerId);
		if (null == index) {
			cancel(serverId, timerId, null, null);
			return;
		}
		cancel(serverId, timerId, index, _tNodes.get(index.getNodeId()));
	}

	void cancelFuture(String timerName) {
		var local = timersFuture.remove(timerName);
		if (null != local)
			local.cancel(false);
	}

	private void cancel(int serverId, String timerName, BIndex index, BNode node) {
		cancelFuture(timerName);
		if (null == node || null == index)
			return;

		var timers = node.getTimers();
		timers.remove(timerName);

		if (timers.isEmpty()) {
			var prev = _tNodes.get(node.getPrevNodeId());
			var next = _tNodes.get(node.getNextNodeId());
			var root = _tNodeRoot.get(serverId);
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

	private void scheduleSimple(int serverId, String timerId, long delay, long concurrentSerialNo) {
		timersFuture.put(timerId, Task.scheduleUnsafe(delay,
				() -> fireSimple(serverId, timerId, concurrentSerialNo, false)));
	}

	public static void initSimpleTimer(BSimpleTimer simpleTimer, long delay, long period, long times, long endTime) {
		if (delay < 0)
			throw new IllegalArgumentException();
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
	}

	public static boolean nextSimpleTimer(BSimpleTimer simpleTimer, boolean missfire) {
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
		if (missfire) {
			if (simpleTimer.getMissfirePolicy() == eMissfirePolicyRunOnce) {
				// 这种策略重置时间，定时器将在新的开始时间之后按原来的间隔执行。
				// simpleTimer.setStartTime(now); // todo 这个要不要重置？
				simpleTimer.setExpectedTime(now);
				simpleTimer.setNextExpectedTime(now + simpleTimer.getPeriod());
			} else {
				simpleTimer.setExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());
				simpleTimer.setNextExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());
			}
		} else {
			simpleTimer.setExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());
			simpleTimer.setNextExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());
		}

		// check endTime
		return simpleTimer.getEndTime() <= 0 || simpleTimer.getNextExpectedTime() <= simpleTimer.getEndTime();
	}

	private long fireSimple(int serverId, String timerId, long concurrentSerialNo, boolean missfire) {
		if (0 != Task.call(zeze.newProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (null == index || index.getServerId() != zeze.getConfig().getServerId()) {
				cancelFuture(timerId);
				return 0; // 不是拥有者，取消本地调度，应该是不大可能发生的。
			}

			var node = _tNodes.get(index.getNodeId());
			if (null == node) {
				cancel(serverId, timerId, index, null);
				return 0; // procedure done
			}

			var timer = node.getTimers().get(timerId);
			@SuppressWarnings("unchecked")
			var handleClass = (Class<? extends TimerHandle>)Class.forName(timer.getHandleName());
			final var handle = handleClass.getDeclaredConstructor().newInstance();
			var simpleTimer = timer.getTimerObj_Zeze_Builtin_Timer_BSimpleTimer();
			if (concurrentSerialNo == timer.getConcurrentFireSerialNo()) {

				final var context = new TimerContext(this, timer, simpleTimer.getHappenTime(),
						simpleTimer.getNextExpectedTime(), simpleTimer.getExpectedTime());

				// 当调度发生了错误或者由于异步时序没有原子保证，导致同时（或某个瞬间）在多个Server进程调度时，
				// 这个系列号保证触发用户回调只会发生一次。这个并发问题不取消定时器，继续尝试调度（去争抢执行权）。
				// 定时器的调度生命期由其他地方保证最终一致。如果保证发生了错误，将一致并发争抢执行权。
				var ret = Task.call(zeze.newProcedure(() -> {
					handle.onTimer(context);
					return 0;
				}, "fireSimpleUser"));
				if (ret == Procedure.Exception) {
					// 用户处理不允许异常，其他错误记录忽略，日志已经记录。
					cancel(serverId, timerId, index, node);
					return 0; // procedure done
				}
				timer.setConcurrentFireSerialNo(concurrentSerialNo + 1);
				// 其他错误忽略

				// 准备下一个间隔
				if (!nextSimpleTimer(simpleTimer, missfire)) {
					cancel(serverId, timerId, index, node);
					return 0;
				}
			}
			// else 发生了并发执行争抢，也需要再次进行本地调度。此时直接使用simpleTimer中的值，不需要再次进行计算。

			// continue period
			long delay = simpleTimer.getNextExpectedTime() - System.currentTimeMillis();
			scheduleSimple(serverId, timerId, delay, concurrentSerialNo + 1);

			return 0L;
		}, "fireSimple"))) {
			Task.call(zeze.newProcedure(() -> {cancel(timerId);return 0L;}, "cancelTimer"));
		}
		return 0L;
	}

	private void scheduleCron(int serverId, String timerName, BCronTimer cron, long concurrentSerialNo) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(serverId, timerName, delay, concurrentSerialNo);
		} catch (Exception ex) {
			// 这个错误是在不好处理。先只记录日志吧。
			logger.error("", ex);
		}
	}

	private void scheduleCronNext(int serverId, String timerName, long delay, long concurrentSerialNo) {
		timersFuture.put(timerName, Task.scheduleUnsafe(delay,
				() -> fireCron(serverId, timerName, concurrentSerialNo, false)));
	}

	public static long cronNextTime(String cron, long time) throws ParseException {
		var cronExpression = new CronExpression(cron);
		return cronExpression.getNextValidTimeAfter(new Date(time)).getTime();
	}

	public static void initCronTimer(BCronTimer cronTimer, String cron, long times, long endTime) throws ParseException {
		cronTimer.setCronExpression(cron);
		long expectedTime = cronNextTime(cron, System.currentTimeMillis());
		cronTimer.setNextExpectedTime(expectedTime);
		cronTimer.setRemainTimes(times);
		cronTimer.setEndTime(endTime);
	}

	public static boolean nextCronTimer(BCronTimer cronTimer, boolean missfire) throws ParseException {
		// check remain times
		if (cronTimer.getRemainTimes() > 0) {
			cronTimer.setRemainTimes(cronTimer.getRemainTimes() - 1);
			if (cronTimer.getRemainTimes() == 0)
				return false;
		}

		var now = System.currentTimeMillis();
		cronTimer.setHappenTime(now);

		// 下面这段代码可以写的更简洁，但这样写，思路更清楚。
		if (missfire) {
			if (cronTimer.getMissfirePolicy() == eMissfirePolicyRunOnce) {
				// 这种策略重置时间，定时器将在新的开始时间之后按原来的间隔执行。
				// cronTimer.setStartTime(now); // todo 这个要不要重置？
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

	private void fireCron(int serverId, String timerId, long concurrentSerialNo, boolean missfire) {
		if (0 != Task.call(zeze.newProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (null == index || index.getServerId() != zeze.getConfig().getServerId()) {
				cancelFuture(timerId);
				return 0; // 不是拥有者，取消本地调度，应该是不大可能发生的。
			}

			var node = _tNodes.get(index.getNodeId());
			if (null == node) {
				// maybe concurrent cancel
				cancel(serverId, timerId, index, null);
				return 0; // procedure done
			}
			var timer = node.getTimers().get(timerId);
			@SuppressWarnings("unchecked")
			var handleClass = (Class<? extends TimerHandle>)Class.forName(timer.getHandleName());
			final var handle = handleClass.getDeclaredConstructor().newInstance();
			var cronTimer = timer.getTimerObj_Zeze_Builtin_Timer_BCronTimer();
			if (concurrentSerialNo == timer.getConcurrentFireSerialNo()) {
				final var context = new TimerContext(this, timer, cronTimer.getHappenTime(),
						cronTimer.getNextExpectedTime(), cronTimer.getExpectedTime());

				// 当调度发生了错误或者由于异步时序没有原子保证，导致同时（或某个瞬间）在多个Server进程调度时，
				// 这个系列号保证触发用户回调只会发生一次。这个并发问题不取消定时器，继续尝试调度（去争抢执行权）。
				// 定时器的调度生命期由其他地方保证最终一致。如果保证发生了错误，将一致并发争抢执行权。
				var ret = Task.call(zeze.newProcedure(() -> {
					handle.onTimer(context);
					return 0;
				}, "fireCronUser"));
				if (ret == Procedure.Exception) {
					// 用户处理不允许异常，其他错误记录忽略，日志已经记录。
					cancel(serverId, timerId, index, node);
					return 0; // procedure done
				}
				timer.setConcurrentFireSerialNo(concurrentSerialNo + 1);

				if (!Timer.nextCronTimer(cronTimer, missfire)) {
					cancel(serverId, timerId, index, node);
					return 0; // procedure done
				}
			}
			// else 发生了并发执行争抢，也需要再次进行本地调度。此时直接使用cronTimer中的值，不需要再次进行计算。

			// continue period
			long delay = cronTimer.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(serverId, timerId, delay, concurrentSerialNo + 1);
			return 0L; // procedure done
		}, "fireCron"))) {
			Task.call(zeze.newProcedure(() -> { cancel(timerId); return 0L; }, "cancelTimer"));
		}
	}

	private void loadTimer() throws ParseException {
		var serverId = zeze.getConfig().getServerId();
		final var out = new OutObject<BNodeRoot>();
		if (Procedure.Success == Task.call(zeze.newProcedure(() -> {
			var root = _tNodeRoot.getOrAdd(serverId);
			// 本地每次load都递增。用来处理和接管的并发。
			root.setLoadSerialNo(root.getLoadSerialNo() + 1);
			out.value = root.copy();
			return 0L;
		}, "LoadTimerLocal"))) {
			var root = out.value;
			loadTimer(root.getHeadNodeId(), root.getHeadNodeId(), serverId);
		}
	}

	// 收到接管通知的服务器调用这个函数进行接管处理。
	// @serverId 需要接管的服务器Id。
	private long spliceLoadTimer(int serverId, long loadSerialNo) throws ParseException {
		if (serverId == zeze.getConfig().getServerId())
			return 0; // skip self

		final var first = new OutObject<Long>();
		final var last = new OutObject<Long>();

		var result = Task.call(zeze.newProcedure(() -> {
			// 当接管别的服务器的定时器时，有可能那台服务器有新的CustomData，这个时候重新加载一次。
			loadCustomClass();

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
			//var tail = _tNodes.get(root.getTailNodeId());

			// 先保存存储过程退出以后需要装载的timer范围。
			first.value = src.getHeadNodeId();
			last.value = root.getHeadNodeId();
			// splice
			srcTail.setNextNodeId(root.getHeadNodeId());
			root.setHeadNodeId(src.getHeadNodeId());
			head.setPrevNodeId(src.getTailNodeId());
			srcHead.setPrevNodeId(root.getTailNodeId());
			// clear src
			src.setHeadNodeId(0L);
			src.setTailNodeId(0L);
			return 0L;
		}, "SpliceAndLoadTimerLocal"));

		if (0L == result) {
			return loadTimer(first.value, last.value, serverId);
		}
		return result;
	}

	// 如果存在node，至少执行一次循环。
	private long loadTimer(long first, long last, int serverId) throws ParseException {
		var node = new OutLong(first);
		while (node.value != last) {
			// skip error. 使用node返回的值决定是否继续循环。
			Task.call(zeze.newProcedure(() -> loadTimer(node, last, serverId), "loadTimer"));
		}
		return 0L;
	}

	private long loadTimer(OutLong first, long last, int serverId) throws ParseException {
		var node = _tNodes.get(first.value);
		if (null == node) {
			first.value = last; // 马上结束外面的循环。last仅用在这里。
			return 0; // when root is empty。no node。skip error.
		}
		first.value = node.getNextNodeId(); // 设置下一个node。

		var now = System.currentTimeMillis();
		for (var timer : node.getTimers().values()) {
			if (timer.getTimerObj().getBean().typeId() == BSimpleTimer.TYPEID) {
				var simpleTimer = (BSimpleTimer)timer.getTimerObj().getBean();
				if (simpleTimer.getNextExpectedTime() < now) { // missfire found
					switch (simpleTimer.getMissfirePolicy()) {
					case eMissfirePolicyRunOnce:
					case eMissfirePolicyRunOnceOldNext:
						Task.run(() -> fireSimple(serverId, timer.getTimerName(),
								timer.getConcurrentFireSerialNo(), true), "missfireSimple");
						continue; // loop done, continue

					case eMissfirePolicyNothing:
						// 重置启动时间，调度下一个（未来）间隔的时间。没有考虑对齐。
						simpleTimer.setNextExpectedTime(System.currentTimeMillis() + simpleTimer.getPeriod());
						break;

					default:
						throw new RuntimeException("Unknown MissfirePolicy");
					}
				}
				scheduleSimple(serverId, timer.getTimerName(),
						simpleTimer.getNextExpectedTime() - System.currentTimeMillis(),
						timer.getConcurrentFireSerialNo());
			} else {
				var cronTimer = (BCronTimer)timer.getTimerObj().getBean();
				if (cronTimer.getNextExpectedTime() < now) {
					switch (cronTimer.getMissfirePolicy()) {
					case eMissfirePolicyRunOnce:
					case eMissfirePolicyRunOnceOldNext:
						Task.run(() -> fireCron(serverId, timer.getTimerName(),
								timer.getConcurrentFireSerialNo(), true), "missfireCron");
						continue; // loop done, continue

					case eMissfirePolicyNothing:
						// 计算下一次（未来）发生的时间。
						cronTimer.setNextExpectedTime(cronNextTime(cronTimer.getCronExpression(), System.currentTimeMillis()));
						break;

					default:
						throw new RuntimeException("Unknown MissfirePolicy");
					}
				}
				scheduleCron(serverId, timer.getTimerName(), cronTimer, timer.getConcurrentFireSerialNo());
			}
			if (serverId != zeze.getConfig().getServerId()) {
				var index = _tIndexs.get(timer.getTimerName());
				index.setServerId(serverId);
			}
		}
		return 0L;
	}

}
