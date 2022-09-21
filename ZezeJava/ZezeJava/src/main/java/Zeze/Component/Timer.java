package Zeze.Component;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderWithOnline;
import Zeze.Arch.RedirectToServer;
import Zeze.Builtin.Timer.*;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Action1;
import Zeze.Util.Action2;
import Zeze.Util.LongConcurrentHashMap;
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
	final LongConcurrentHashMap<Future<?>> timersFuture = new LongConcurrentHashMap<>();

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
		if (null != providerApp && null != providerApp.providerImplement) {
			if (providerApp.providerImplement instanceof ProviderWithOnline arch)
				timerAccount = new TimerAccount(arch.online);
			else if (providerApp.providerImplement instanceof Zeze.Game.ProviderImplementWithOnline game)
				timerRole = new TimerRole(game.online);
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
	public long schedule(long delay, long period, String name, Bean customData) {
		return schedule(delay, period, -1, name, customData);
	}

	// 调度一个Timeout，即仅执行一次的Timer。
	public long schedule(long delay, String name, Bean customData) {
		return schedule(delay, -1, 1, name, customData);
	}

	public long schedule(long delay, long period, long times, String name, Bean customData) {
		return schedule(delay, period, times, -1, name, customData);
	}

	public long schedule(long delay, long period, long times, long endTime, String name, Bean customData) {
		if (delay < 0)
			throw new IllegalArgumentException();

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
				var timerId = timerIdAutoKey.nextId();
				var timer = new BTimer();
				timer.setTimerId(timerId);
				timer.setName(name);

				var simpleTimer = new BSimpleTimer();
				initSimpleTimer(simpleTimer, delay, period, times, endTime);
				timer.setTimerObj(simpleTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					register(customData.getClass());
					timer.getCustomData().setBean(customData);
				}

				var index = new BIndex();
				index.setServerId(serverId);
				index.setNodeId(nodeId);
				_tIndexs.tryAdd(timerId, index);

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
	public long scheduleMonth(int monthDay, int hour, int minute, int second, String name, Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " " + monthDay + " * ?";
		return schedule(cron, name, customData);
	}

	// 每周第N(weekDay)天的某个时刻(hour, minute, second)。
	public long scheduleWeek(int weekDay, int hour, int minute, int second, String name, Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " * * " + weekDay;
		return schedule(cron, name, customData);
	}

	// 每天的某个时刻(hour, minute, second)。
	public long scheduleDay(int hour, int minute, int second, String name, Bean customData) throws ParseException {
		var cron = second + " " + minute + " " + hour + " * * ?";
		return schedule(cron, name, customData);
	}

	public long schedule(String cronExpression, String name, Bean customData) throws ParseException {
		return schedule(cronExpression, -1, -1, name, customData);
	}
	public long schedule(String cronExpression, long times, long endTime, String name, Bean customData) throws ParseException {
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
				var timerId = timerIdAutoKey.nextId();
				var timer = new BTimer();
				timer.setTimerId(timerId);
				timer.setName(name);

				node.getTimers().put(timerId, timer);
				if (null != customData) {
					register(customData.getClass());
					timer.getCustomData().setBean(customData);
				}
				var cronTimer = new BCronTimer();
				initCronTimer(cronTimer, cronExpression, times, endTime);
				timer.setTimerObj(cronTimer);

				var index = new BIndex();
				index.setServerId(serverId);
				index.setNodeId(nodeId);
				_tIndexs.tryAdd(timerId, index);

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
	public boolean scheduleNamed(String timerName, long delay, String handleName, Bean customData) {
		return scheduleNamed(timerName, delay, -1, -1, handleName, customData);
	}

	public boolean scheduleNamed(String timerName, long delay, long period, String handleName, Bean customData) {
		return scheduleNamed(timerName, delay, period, -1, handleName, customData);
	}

	public boolean scheduleNamed(String timerName, long delay, long period, long times, String handleName, Bean customData) {
		var timerId = _tNamed.get(timerName);
		if (null != timerId)
			return false;
		timerId = new BTimerId(schedule(delay, period, times, handleName, customData));
		_tNamed.insert(timerName, timerId);
		_tIndexs.get(timerId.getTimerId()).setNamedName(timerName);
		return true;
	}

	public boolean scheduleNamed(String timerName, String cron, String handleName, Bean customData) throws ParseException {
		return scheduleNamed(timerName, cron, -1, -1, handleName, customData);
	}

	public boolean scheduleNamed(String timerName, String cron, long times, long endTime, String handleName, Bean customData) throws ParseException {
		var timerId = _tNamed.get(timerName);
		if (null != timerId)
			return false;
		timerId = new BTimerId(schedule(cron, times, endTime, handleName, customData));
		_tNamed.insert(timerName, timerId);
		_tIndexs.get(timerId.getTimerId()).setNamedName(timerName);
		return true;
	}

	public void cancelNamed(String timerName) {
		var timerId = _tNamed.get(timerName);
		cancel(timerId.getTimerId());
		_tNamed.remove(timerName);
	}

	// 取消一个具体的Timer实例。
	public void cancel(long timerId) {
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

	tOnlineNamed tOnlineNamed() {
		return _tOnlineNamed;
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
	protected void redirectCancel(int serverId, long timerId) {
		// 尽可能的执行取消操作，不做严格判断。
		var index = _tIndexs.get(timerId);
		if (null == index) {
			cancel(serverId, timerId, null, null);
			return;
		}
		cancel(serverId, timerId, index, _tNodes.get(index.getNodeId()));
	}

	void cancelFuture(long timerId) {
		var local = timersFuture.remove(timerId);
		if (null != local)
			local.cancel(false);
	}

	private void cancel(int serverId, long timerId, BIndex index, BNode node) {
		cancelFuture(timerId);
		if (null == node || null == index)
			return;

		if (!index.getNamedName().isEmpty())
			_tNamed.remove(index.getNamedName());

		var timers = node.getTimers();
		var timer = timers.remove(timerId);
		var cancelHandle = timerCancelHandles.get(timer.getName());
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

	private void scheduleSimple(int serverId, long timerId, long delay, String name, long concurrentSerialNo) {
		timersFuture.put(timerId, Task.scheduleUnsafe(delay,
				() -> fireSimple(serverId, timerId, name, concurrentSerialNo)));
	}

	public static void initSimpleTimer(BSimpleTimer simpleTimer, long delay, long period, long times, long endTime) {
		var now = System.currentTimeMillis();
		//timer.setDelay(delay);
		simpleTimer.setPeriod(period);
		//times == -1, this means Infinite number of times ----lwj
		simpleTimer.setRemainTimes(times);
		simpleTimer.setEndTime(endTime);
		long expectedTime = now + delay;
		simpleTimer.setNextExpectedTime(expectedTime);
		simpleTimer.setStartTime(expectedTime);
	}

	public static boolean nextSimpleTimer(BSimpleTimer simpleTimer) {
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
//		long delta = now - simpleTimer.getStartTime();
//		simpleTimer.setExpectedTime(now + (delta / simpleTimer.getPeriod() * simpleTimer.getPeriod())); // X：有点不明白之前为什么要这样写，改成下面这样暂时结果是对的
		simpleTimer.setExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());
		simpleTimer.setNextExpectedTime(simpleTimer.getExpectedTime() + simpleTimer.getPeriod());

		// check endTime
		if (simpleTimer.getEndTime() > 0 && simpleTimer.getNextExpectedTime() > simpleTimer.getEndTime())
			return false;
		return true;
	}

	private long fireSimple(int serverId, long timerId, String name, long concurrentSerialNo) {
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
				if (!nextSimpleTimer(simpleTimer)) {
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

	private void scheduleCron(int serverId, long timerId, BCronTimer cron, String name, long concurrentSerialNo) {
		try {
			long delay = cron.getNextExpectedTime() - System.currentTimeMillis();
			scheduleCronNext(serverId, timerId, delay, name, concurrentSerialNo);
		} catch (Exception ex) {
			// 这个错误是在不好处理。先只记录日志吧。
			logger.error("", ex);
		}
	}

	private void scheduleCronNext(int serverId, long timerId, long delay, String name, long concurrentSerialNo) {
		timersFuture.put(timerId, Task.scheduleUnsafe(delay, () -> fireCron(serverId, timerId, name, concurrentSerialNo)));
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

	public static boolean nextCronTimer(BCronTimer cronTimer) throws ParseException {
		// check remain times
		if (cronTimer.getRemainTimes() > 0) {
			cronTimer.setRemainTimes(cronTimer.getRemainTimes() - 1);
			if (cronTimer.getRemainTimes() == 0)
				return false;
		}

		cronTimer.setExpectedTime(cronTimer.getNextExpectedTime());
		cronTimer.setNextExpectedTime(cronNextTime(cronTimer.getCronExpression(), cronTimer.getExpectedTime()));
		cronTimer.setHappenTime(System.currentTimeMillis());

		// check endTime
		if (cronTimer.getEndTime() > 0 && cronTimer.getNextExpectedTime() > cronTimer.getEndTime())
			return false;

		return true;
	}

	private void fireCron(int serverId, long timerId, String name, long concurrentSerialNo) {
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

				if (!Timer.nextCronTimer(cronTimer)) {
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

			for (var timer : node.getTimers().values()) {
				if (timer.getTimerObj().getBean().typeId() == BSimpleTimer.TYPEID) {
					var simpleTimer = (BSimpleTimer)timer.getTimerObj().getBean();
					scheduleSimple(serverId, timer.getTimerId(),
							simpleTimer.getNextExpectedTime() - System.currentTimeMillis(),
							timer.getName(), timer.getConcurrentFireSerialNo());
				} else {
					var cronTimer = (BCronTimer)timer.getTimerObj().getBean();
					scheduleCron(serverId, timer.getTimerId(), cronTimer,
							timer.getName(), timer.getConcurrentFireSerialNo());
				}
				if (serverId != zeze.getConfig().getServerId()) {
					Task.call(zeze.newProcedure(() -> {
						var index = _tIndexs.get(timer.getTimerId());
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
