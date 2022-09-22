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
import Zeze.Util.Action1;
import Zeze.Util.Action2;
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
	// 保存所有可用的timer处理回调，由于可能需要把timer的触发派发到其他服务器执行，必须静态注册。
	// 一般在Module.Initialize中注册即可。
	final ConcurrentHashMap<String, Action1<TimerContext>> timerHandles = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Action2<BIndex, BTimer>> timerCancelHandles = new ConcurrentHashMap<>();
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
	public void start() throws Throwable {
		nodeIdAutoKey = zeze.getAutoKey("Zeze.Component.Timer.NodeId");
		timerIdAutoKey = zeze.getAutoKey("Zeze.Component.Timer.TimerId");
		if (0L != zeze.newProcedure(() -> {
			var classes = _tCustomClasses.getOrAdd(1);
			for (var cls : classes.getCustomClasses()) {
				beanFactory.register((Class<? extends Bean>)Class.forName(cls));
			}
			return 0L;
		}, "").Call()) {
			throw new IllegalStateException("Load Item Classes Failed.");
		}
		Task.run(this::loadTimer, "LoadTimerLocal");
	}

	public void initializeOnlineTimer(ProviderApp providerApp) {
		ProviderImplement impl;
		if (null != providerApp && null != (impl = providerApp.providerImplement)) {
			if (impl instanceof ProviderWithOnline)
				timerAccount = new TimerAccount(((ProviderWithOnline)impl).online);
			else if (impl instanceof Zeze.Game.ProviderImplementWithOnline)
				timerRole = new TimerRole(((Zeze.Game.ProviderImplementWithOnline)impl).online);
		}
	}

	public void stop() throws Throwable {
		UnRegisterZezeTables(this.zeze);
	}

	public void addHandle(String name, Action1<TimerContext> action) {
		addHandle(name, action, null);
	}

	public void addHandle(String name, Action1<TimerContext> action, Action2<BIndex, BTimer> cancel) {
		if (null != timerHandles.putIfAbsent(name, action))
			throw new RuntimeException("duplicate timer handle name of: " + name);
		if (null != cancel && null != timerCancelHandles.putIfAbsent(name, cancel))
			throw new RuntimeException("duplicate timer cancel handle name of: " + name);
	}

	public void removeHandle(String name) {
		timerHandles.remove(name);
	}

	/////////////////////////////////////////////////////////////////////////
	// Simple Timer
	// 调度一个Timer实例。
	// name为静态注册到这个模块的处理名字。
	// 相同的name可以调度多个timer实例。
	// @return 返回 TimerId。
	public String schedule(long delay, long period, String name, Bean customData) {
		return schedule(delay, period, -1, name, customData);
	}

	// 调度一个Timeout，即仅执行一次的Timer。
	public String schedule(long delay, String name, Bean customData) {
		return schedule(delay, -1, 1, name, customData);
	}

	public String schedule(long delay, long period, long times, String name, Bean customData) {
		return schedule(delay, period, times, -1, name, customData);
	}

	public String schedule(long delay, long period, long times, long endTime, String name, Bean customData) {
		return schedule(delay, period, times, endTime, eMissfirePolicyNothing, name, customData);
	}

	public String schedule(long delay, long period, long times, long endTime, int missfirePolicy, String name, Bean customData) {
		var simpleTimer = new BSimpleTimer();
		initSimpleTimer(simpleTimer, delay, period, times, endTime);
		simpleTimer.setMissfirePolicy(missfirePolicy);
		return schedule(simpleTimer, name, customData);
	}

	// 直接传递BSimpleTimer，需要自己调用它Timer.initSimpleTimer初始化。所以暂时不开放了。
	private String schedule(BSimpleTimer simpleTimer, String name, Bean customData) {
		// auto name
		return schedule("@" + timerIdAutoKey.nextString(), simpleTimer, name, customData);
	}

	private String schedule(String timerId, BSimpleTimer simpleTimer, String name, Bean customData) {
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
				timer.setHandleName(name);
				timer.setTimerObj(simpleTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					register(customData.getClass());
					timer.getCustomData().setBean(customData);
				}

				Transaction.whileCommit(() -> scheduleSimple(serverId, timerId,
						simpleTimer.getExpectedTime() - System.currentTimeMillis(),
						name, timer.getConcurrentFireSerialNo()));
				return timerId;
			}
			nodeId = nodeIdAutoKey.nextId();
		}
	}

	//////////////////////////////////////////////////////////////////////////////////
	// Cron Timer
	// 每月第N(monthDay)天的某个时刻(hour,minute,second)。
	public String scheduleMonth(int monthDay, int hour, int minute, int second, String name, Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " " + monthDay + " * ?";
		return schedule(cron, name, customData);
	}

	// 每周第N(weekDay)天的某个时刻(hour, minute, second)。
	public String scheduleWeek(int weekDay, int hour, int minute, int second, String name, Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " * * " + weekDay;
		return schedule(cron, name, customData);
	}

	// 每天的某个时刻(hour, minute, second)。
	public String scheduleDay(int hour, int minute, int second, String name, Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " * * ?";
		return schedule(cron, name, customData);
	}

	public String schedule(String cronExpression, String name, Bean customData) throws ParseException {
		return schedule(cronExpression, -1, -1, name, customData);
	}

	public String schedule(String cronExpression, long times, long endTime, String name, Bean customData) throws ParseException {
		return schedule(cronExpression, times, endTime, eMissfirePolicyNothing, name, customData);
	}

	public String schedule(String cronExpression, long times, long endTime, int missfirePolicy, String name, Bean customData) throws ParseException {
		var cronTimer = new BCronTimer();
		initCronTimer(cronTimer, cronExpression, times, endTime);
		cronTimer.setMissfirePolicy(missfirePolicy);
		return schedule(cronTimer, name, customData);
	}

	// 直接传递BCronTimer需要自动调用Timer.initCronTimer初始化。先不开放了。
	private String schedule(BCronTimer cronTimer, String name, Bean customData) {
		return schedule("@" + timerIdAutoKey.nextString(), cronTimer, name, customData);
	}

	private String schedule(String timerId, BCronTimer cronTimer, String name, Bean customData) {
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
				timer.setHandleName(name);
				timer.setTimerObj(cronTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					register(customData.getClass());
					timer.getCustomData().setBean(customData);
				}

				Transaction.whileCommit(() -> scheduleCron(serverId, timerId, cronTimer, name, timer.getConcurrentFireSerialNo()));
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
	public boolean scheduleNamed(String timerId, long delay, String handleName, Bean customData) {
		return scheduleNamed(timerId, delay, -1, -1, -1, eMissfirePolicyNothing, handleName, customData);
	}

	public boolean scheduleNamed(String timerId, long delay, long period, String handleName, Bean customData) {
		return scheduleNamed(timerId, delay, period, -1, -1, eMissfirePolicyNothing, handleName, customData);
	}

	public boolean scheduleNamed(String timerId, long delay, long period, long times, String handleName, Bean customData) {
		return scheduleNamed(timerId, delay, period, times, -1, eMissfirePolicyNothing, handleName, customData);
	}

	public boolean scheduleNamed(String timerId, long delay, long period, long times, long endTime, int missfirePolicy, String handleName, Bean customData) {
		if (timerId.startsWith("@"))
			throw new IllegalArgumentException("invalid timer name. startsWith '@' is reserved.");

		var index = _tIndexs.get(timerId);
		if (null != index)
			return false;

		var simpleTimer = new BSimpleTimer();
		initSimpleTimer(simpleTimer, delay, period, times, endTime);
		simpleTimer.setMissfirePolicy(missfirePolicy);
		schedule(timerId, simpleTimer, handleName, customData);
		return true;
	}

	public boolean scheduleNamed(String timerName, String cron, String handleName, Bean customData) throws ParseException {
		return scheduleNamed(timerName, cron, -1, -1, eMissfirePolicyNothing, handleName, customData);
	}

	public boolean scheduleNamed(String timerName, String cron, long times, long endTime, String handleName, Bean customData) throws ParseException {
		return scheduleNamed(timerName, cron, times, endTime, eMissfirePolicyNothing, handleName, customData);
	}

	public boolean scheduleNamed(String timerName, String cron, long times, long endTime, int missfirePolicy, String handleName, Bean customData) throws ParseException {
		var timerId = _tIndexs.get(timerName);
		if (null != timerId)
			return false;
		var cronTimer = new BCronTimer();
		initCronTimer(cronTimer, cron, times, endTime);
		cronTimer.setMissfirePolicy(missfirePolicy);
		schedule(timerName, cronTimer, handleName, customData);
		return true;
	}

	// 取消一个具体的Timer实例。
	public void cancel(String timerId) {
		try {
			// XXX 统一通过这里取消定时器，可能会浪费一次内存表查询。
			if (null != timerRole && timerRole.cancel(timerId))
				return; // done

			if (null != timerAccount && timerAccount.cancel(timerId))
				return; // done

		} catch (Throwable ex) {
			logger.error("ignore cancel error.", ex);
			return; // done;
		}

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
	tArchOlineTimer tArchOlineTimer() {
		return _tArchOlineTimer;
	}

	tGameOlineTimer tGameOlineTimer() {
		return _tGameOlineTimer;
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
		var timer = timers.remove(timerName);
		var cancelHandle = timerCancelHandles.get(timer.getHandleName());
		if (null != cancelHandle) {
			try {
				cancelHandle.run(index, timer);
			} catch (Throwable e) {
				// log cancel error only.
				logger.error("", e);
			}
		}

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

	private void scheduleSimple(int serverId, String timerId, long delay, String name, long concurrentSerialNo) {
		timersFuture.put(timerId, Task.scheduleUnsafe(delay,
				() -> fireSimple(serverId, timerId, name, concurrentSerialNo, false)));
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

		if (missfire) {
			switch (simpleTimer.getMissfirePolicy()) {
			case eMissfirePolicyRunOnce:
				// 这种策略重置时间，定时器将在新的开始时间之后按原来的间隔执行。
				// simpleTimer.setStartTime(now); // todo 这个要不要重置？
				simpleTimer.setExpectedTime(now);
				simpleTimer.setNextExpectedTime(now + simpleTimer.getPeriod());
				break;

			default:
				simpleTimer.setExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());
				simpleTimer.setNextExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());
				break;
			}
		} else {
			simpleTimer.setExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());
			simpleTimer.setNextExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());
		}

		// check endTime
		if (simpleTimer.getEndTime() > 0 && simpleTimer.getNextExpectedTime() > simpleTimer.getEndTime())
			return false;
		return true;
	}

	private long fireSimple(int serverId, String timerId, String name, long concurrentSerialNo, boolean missfire) {
		final var handle = timerHandles.get(name);
		if (0 != Task.call(zeze.newProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (null == index || index.getServerId() != zeze.getConfig().getServerId()) {
				cancelFuture(timerId);
				return 0; // 不是拥有者，取消本地调度，应该是不大可能发生的。
			}

			var node = _tNodes.get(index.getNodeId());
			if (null == node || handle == null) {
				cancel(serverId, timerId, index, node);
				return 0; // procedure done
			}

			var timer = node.getTimers().get(timerId);
			var simpleTimer = timer.getTimerObj_Zeze_Builtin_Timer_BSimpleTimer();
			if (concurrentSerialNo == timer.getConcurrentFireSerialNo()) {

				final var context = new TimerContext(timer, simpleTimer.getHappenTime(),
						simpleTimer.getNextExpectedTime(), simpleTimer.getExpectedTime());

				// 当调度发生了错误或者由于异步时序没有原子保证，导致同时（或某个瞬间）在多个Server进程调度时，
				// 这个系列号保证触发用户回调只会发生一次。这个并发问题不取消定时器，继续尝试调度（去争抢执行权）。
				// 定时器的调度生命期由其他地方保证最终一致。如果保证发生了错误，将一致并发争抢执行权。
				var ret = Task.call(zeze.newProcedure(() -> {
					handle.run(context);
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
			scheduleSimple(serverId, timerId, delay, name, concurrentSerialNo + 1);

			return 0L;
		}, "fireSimple"))) {
			cancel(timerId);
		}
		return 0L;
	}

	private void scheduleCron(int serverId, String timerName, BCronTimer cron, String name, long concurrentSerialNo) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(serverId, timerName, delay, name, concurrentSerialNo);
		} catch (Exception ex) {
			// 这个错误是在不好处理。先只记录日志吧。
			logger.error("", ex);
		}
	}

	private void scheduleCronNext(int serverId, String timerName, long delay, String name, long concurrentSerialNo) {
		timersFuture.put(timerName, Task.scheduleUnsafe(delay, () -> fireCron(serverId, timerName, name, concurrentSerialNo, false)));
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
		if (missfire) {
			switch (cronTimer.getMissfirePolicy()) {
			case eMissfirePolicyRunOnce:
				// 这种策略重置时间，定时器将在新的开始时间之后按原来的间隔执行。
				// cronTimer.setStartTime(now); // todo 这个要不要重置？
				cronTimer.setExpectedTime(now);
				cronTimer.setNextExpectedTime(cronNextTime(cronTimer.getCronExpression(), now));
				break;

			default:
				cronTimer.setExpectedTime(cronTimer.getNextExpectedTime());
				cronTimer.setNextExpectedTime(cronNextTime(cronTimer.getCronExpression(), cronTimer.getExpectedTime()));
				break;
			}
		} else {
			cronTimer.setExpectedTime(cronTimer.getNextExpectedTime());
			cronTimer.setNextExpectedTime(cronNextTime(cronTimer.getCronExpression(), cronTimer.getExpectedTime()));
		}

		// check endTime
		if (cronTimer.getEndTime() > 0 && cronTimer.getNextExpectedTime() > cronTimer.getEndTime())
			return false;

		return true;
	}

	private void fireCron(int serverId, String timerId, String name, long concurrentSerialNo, boolean missfire) {
		final var handle = timerHandles.get(name);
		if (0 != Task.call(zeze.newProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (null == index || index.getServerId() != zeze.getConfig().getServerId()) {
				cancelFuture(timerId);
				return 0; // 不是拥有者，取消本地调度，应该是不大可能发生的。
			}

			var node = _tNodes.get(index.getNodeId());
			if (null == node || handle == null) {
				// maybe concurrent cancel
				cancel(serverId, timerId, index, node);
				return 0; // procedure done
			}
			var timer = node.getTimers().get(timerId);
			var cronTimer = timer.getTimerObj_Zeze_Builtin_Timer_BCronTimer();
			if (concurrentSerialNo == timer.getConcurrentFireSerialNo()) {
				final var context = new TimerContext(timer, cronTimer.getHappenTime(),
						cronTimer.getNextExpectedTime(), cronTimer.getExpectedTime());

				// 当调度发生了错误或者由于异步时序没有原子保证，导致同时（或某个瞬间）在多个Server进程调度时，
				// 这个系列号保证触发用户回调只会发生一次。这个并发问题不取消定时器，继续尝试调度（去争抢执行权）。
				// 定时器的调度生命期由其他地方保证最终一致。如果保证发生了错误，将一致并发争抢执行权。
				var ret = Task.call(zeze.newProcedure(() -> {
					handle.run(context);
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
			scheduleCronNext(serverId, timerId, delay, name, concurrentSerialNo + 1);
			return 0L; // procedure done
		}, "fireCron"))) {
			cancel(timerId);
		}
	}

	private void loadTimer() {
		var serverId = zeze.getConfig().getServerId();
		final var out = new OutObject<BNodeRoot>();
		if (Procedure.Success == Task.call(zeze.newProcedure(() -> {
			var root = _tNodeRoot.getOrAdd(serverId);
			// 本地每次load都递增。用来处理和接管的并发。
			root.setLoadSerialNo(root.getLoadSerialNo() + 1);
			out.value = root.copy(); // TODO zyao runwhileSucc
			return 0L;
		}, "LoadTimerLocal"))) {
			var root = out.value;
			loadTimer(root.getHeadNodeId(), root.getHeadNodeId(), serverId);
		}
	}

	// 收到接管通知的服务器调用这个函数进行接管处理。
	// @serverId 需要接管的服务器Id。
	private long spliceLoadTimer(int serverId, long loadSerialNo) {
		if (serverId == zeze.getConfig().getServerId())
			throw new IllegalArgumentException();

		final var first = new OutObject<Long>();
		final var last = new OutObject<Long>();

		var result = Task.call(zeze.newProcedure(() -> {
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
	private long loadTimer(long first, long last, int serverId) {
		while (true) {
			var node = _tNodes.selectDirty(first);
			if (null == node)
				break; // when root is empty。no node。

			var now = System.currentTimeMillis();
			for (var timer : node.getTimers().values()) {
				if (timer.getTimerObj().getBean().typeId() == BSimpleTimer.TYPEID) {
					var simpleTimer = (BSimpleTimer)timer.getTimerObj().getBean();
					if (simpleTimer.getNextExpectedTime() < now) { // missfire found
						switch (simpleTimer.getMissfirePolicy()) {
						case eMissfirePolicyRunOnce:
						case eMissfirePolicyRunOnceOldNext:
							Task.run(() -> fireSimple(serverId, timer.getTimerName(), timer.getHandleName(),
									timer.getConcurrentFireSerialNo(), true), "missfireSimple");
							continue; // loop done, continue

						// case eMissfirePolicyNothing: break;
						default:
							break;

						}
					}
					scheduleSimple(serverId, timer.getTimerName(),
							simpleTimer.getNextExpectedTime() - System.currentTimeMillis(),
							timer.getHandleName(), timer.getConcurrentFireSerialNo());
				} else {
					var cronTimer = (BCronTimer)timer.getTimerObj().getBean();
					if (cronTimer.getNextExpectedTime() < now) {
						switch (cronTimer.getMissfirePolicy()) {
						case eMissfirePolicyRunOnce:
						case eMissfirePolicyRunOnceOldNext:
							Task.run(() -> fireCron(serverId, timer.getTimerName(), timer.getHandleName(),
									timer.getConcurrentFireSerialNo(), true), "missfireCron");
							continue; // loop done, continue

						default:
							break;
						}
					}
					scheduleCron(serverId, timer.getTimerName(), cronTimer,
							timer.getHandleName(), timer.getConcurrentFireSerialNo());
				}
				if (serverId != zeze.getConfig().getServerId()) {
					Task.call(zeze.newProcedure(() -> {
						var index = _tIndexs.get(timer.getTimerName());
						index.setServerId(serverId);
						return 0L;
					}, "SetTimerServerIdWhenLoadTimerLocal"));
				}
			}

			first = node.getNextNodeId();
			if (first == last)
				break;
		}
		return 0L;
	}

}
