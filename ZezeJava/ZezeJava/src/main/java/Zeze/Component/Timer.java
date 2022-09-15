package Zeze.Component;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Arch.RedirectToServer;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Action1;
import Zeze.Builtin.Timer.*;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.CronExpression;

public class Timer extends AbstractTimer {
	private static final BeanFactory beanFactory = new BeanFactory();

	public static long GetSpecialTypeIdFromBean(Bean bean) {
		return BeanFactory.GetSpecialTypeIdFromBean(bean);
	}

	public static Bean CreateBeanFromSpecialTypeId(long typeId) {
		return beanFactory.CreateBeanFromSpecialTypeId(typeId);
	}

	public static final int CountPerNode = 200;

	static final Logger logger = LogManager.getLogger(Timer.class);
	public final Zeze.Application zeze;
	private AutoKey nodeIdAutoKey;
	AutoKey timerIdAutoKey;
	// 保存所有可用的timer处理回调，由于可能需要把timer的触发派发到其他服务器执行，必须静态注册。
	// 一般在Module.Initialize中注册即可。
	final ConcurrentHashMap<String, Action1<TimerContext>> timerHandles = new ConcurrentHashMap<>();
	// 在这台服务器进程内调度的所有Timer。key是timerId，value是ThreadPool.schedule的返回值。
	final ConcurrentHashMap<Long, Future<?>> timersFuture = new ConcurrentHashMap<>();

	public Timer(Zeze.Application zeze) {
		this.zeze = zeze;
		RegisterZezeTables(zeze);
	}
	@SuppressWarnings("unchecked")
	public void Start() throws Throwable {
		nodeIdAutoKey = zeze.GetAutoKey("Zeze.Component.Timer.NodeId");
		timerIdAutoKey = zeze.GetAutoKey("Zeze.Component.Timer.TimerId");
		if (0L != zeze.NewProcedure(() -> {
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

	public void Stop() throws Throwable {
		UnRegisterZezeTables(this.Zeze);
	}

	public void addHandle(String name, Action1<TimerContext> action) {
		if (null != timerHandles.putIfAbsent(name, action))
			throw new RuntimeException("duplicate timer handle name of: " + name);
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
				long curMills = System.currentTimeMillis();
				var timerId = timerIdAutoKey.nextId();
				var timer = new BTimer();
				timer.setTimerId(timerId);
				timer.setName(name);

				var simpleTimer = new BSimpleTimer();
				initSimpleTimer(simpleTimer, delay, period, times);
				timer.setTimerObj(simpleTimer);
				node.getTimers().put(timerId, timer);

				if (customData != null) {
					beanFactory.register(customData.getClass());
					_tCustomClasses.getOrAdd(1).getCustomClasses().add(customData.getClass().getName());
					timer.getCustomData().setBean(customData);
				}

				var index = new BIndex();
				index.setServerId(serverId);
				index.setNodeId(nodeId);
				_tIndexs.tryAdd(timerId, index);

				Transaction.whileCommit(
						() -> scheduleSimpleLocal(serverId, timerId,
						simpleTimer.getExpectedTimeMills() - System.currentTimeMillis(),
						period, name));
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
				//long curMills = System.currentTimeMillis();
				var timerId = timerIdAutoKey.nextId();
				var timer = new BTimer();
				timer.setTimerId(timerId);
				timer.setName(name);

				var cronTimer = new BCronTimer();
				cronTimer.setCronExpression(cronExpression);

				node.getTimers().put(timerId, timer);
				if (null != customData) {
					beanFactory.register(customData.getClass());
					_tCustomClasses.getOrAdd(1).getCustomClasses().add(customData.getClass().getName());
					timer.getCustomData().setBean(customData);
				}

				long expectedTimeMills = getNextValidTimeAfter(cronExpression, Calendar.getInstance()).getTimeInMillis();
				cronTimer.setNextExpectedTimeMills(expectedTimeMills);
				timer.setTimerObj(cronTimer);

				var index = new BIndex();
				index.setServerId(serverId);
				index.setNodeId(nodeId);
				_tIndexs.tryAdd(timerId, index);

				Transaction.whileCommit(() -> scheduleCronLocal(serverId, timerId, cronExpression, name));
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
	public boolean scheduleNamed(String timerName, long delay, long period, String handleName, Bean customData) {
		var timerId = _tNamed.get(timerName);
		if (null != timerId)
			return false;
		timerId = new BTimerId(schedule(delay, period, handleName, customData));
		_tNamed.insert(timerName, timerId);
		_tIndexs.get(timerId.getTimerId()).setNamedName(timerName);
		return true;
	}

	public boolean scheduleNamed(String timerName, String cron, String handleName, Bean customData) throws ParseException {
		var timerId = _tNamed.get(timerName);
		if (null != timerId)
			return false;
		timerId = new BTimerId(schedule(cron, handleName, customData));
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
		// try cancel online timer
		if (null != timerArchOnline && timerArchOnline.cancel(timerId))
			return;

		var index = _tIndexs.get(timerId);
		if (null == index) {
			// 尽可能的执行取消操作，不做严格判断。
			cancelTimerLocal(zeze.getConfig().getServerId(), timerId, null, null);
			return;
		}

		redirectCancel(index.getServerId(), timerId);
	}

	private TimerArchOnline timerArchOnline;

	public TimerArchOnline getArchOnlineTimer() {
		return timerArchOnline;
	}

	/////////////////////////////////////////////////////////////
	// 内部实现
	@RedirectToServer
	protected void redirectCancel(int serverId, long timerId) {
		// 尽可能的执行取消操作，不做严格判断。
		var index = _tIndexs.get(timerId);
		if (null == index) {
			cancelTimerLocal(serverId, timerId, null, null);
			return;
		}
		cancelTimerLocal(serverId, timerId, index, _tNodes.get(index.getNodeId()));
	}

	private void cancelTimerLocal(int serverId, long timerId, BIndex index, BNode node) {

		var local = timersFuture.remove(timerId);
		if (null != local)
			local.cancel(false);

		if (null == node || null == index)
			return;

		if (!index.getNamedName().isEmpty())
			_tNamed.remove(index.getNamedName());

		var timers = node.getTimers();
		timers.remove(timerId);

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

	private void scheduleSimpleLocal(int serverId, long timerId, long delay, long period, String name) {
		if (period > 0) {
			timersFuture.put(timerId, Task.scheduleUnsafe(delay, period,
					() -> fireSimpleLocal(serverId, timerId, name)));
		} else {
			timersFuture.put(timerId, Task.scheduleUnsafe(delay,
					() -> fireSimpleLocal(serverId, timerId, name)));
		}
	}

	public static void initSimpleTimer(BSimpleTimer simpleTimer, long delay, long period, long times) {
		var curMills = System.currentTimeMillis();
		//timer.setDelay(delay);
		simpleTimer.setPeriod(period);
		//times == -1, this means Infinite number of times ----lwj
		simpleTimer.setRemainTimes(times);
		long expectedTimeMills = curMills + delay;
		simpleTimer.setNextExpectedTimeMills(expectedTimeMills);
		simpleTimer.setStartTimeInMills(expectedTimeMills);
	}

	public static void nextSimpleTimer(BSimpleTimer simpleTimer) {
		simpleTimer.setHappenTimes(simpleTimer.getHappenTimes() + 1);
		long curTimeMills = System.currentTimeMillis();
		simpleTimer.setHappenTimeMills(curTimeMills);
		if (simpleTimer.getPeriod() <= 0) {
			simpleTimer.setExpectedTimeMills(simpleTimer.getStartTimeInMills());
			simpleTimer.setNextExpectedTimeMills(0);
		} else {
			long delta = curTimeMills - simpleTimer.getStartTimeInMills();
			simpleTimer.setExpectedTimeMills(delta / simpleTimer.getPeriod() * simpleTimer.getPeriod());
			simpleTimer.setNextExpectedTimeMills(simpleTimer.getExpectedTimeMills() + simpleTimer.getPeriod());
		}
	}

	private long fireSimpleLocal(int serverId, long timerId, String name) {
		final var handle = timerHandles.get(name);

		Task.Call(zeze.NewProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (null != index) {
				var node = _tNodes.get(index.getNodeId());
				if (null != node) {
					if (handle == null) {
						cancelTimerLocal(serverId, timerId, index, node);
					} else {
						var timer = node.getTimers().get(timerId);

						//timer cancel will delay, so here need to judge
						if (timer != null) {
							if (timer.getTimerObj().getBean().typeId() == BSimpleTimer.TYPEID) {
								var simpleTimer = (BSimpleTimer) timer.getTimerObj().getBean();
								nextSimpleTimer(simpleTimer);
								/* skip nest procedure result */
								Task.Call(zeze.NewProcedure(() -> {
									var context = new TimerContext(timer,
											simpleTimer.getHappenTimes(),
											simpleTimer.getNextExpectedTimeMills(),
											simpleTimer.getExpectedTimeMills());
									handle.run(context);
									return Procedure.Success;
								}, "fireLocalHandle"));

								// 不管任何结果都递减次数。
								if (simpleTimer.getRemainTimes() > 0) {
									simpleTimer.setRemainTimes(simpleTimer.getRemainTimes() - 1);
									if (simpleTimer.getRemainTimes() == 0) {
										cancelTimerLocal(serverId, timerId, index, node);
									}
								}
							}
						}
					}
				}
			}
			return 0L;
		}, "TriggerSimpleTimerLocal"));
		return 0L;
	}

	private void scheduleCronLocal(int serverId, long timerId, String cronExpression, String name) {
		try {
			long delay = getNextValidTimeAfter(cronExpression, Calendar.getInstance()).getTimeInMillis() - System.currentTimeMillis();
			scheduleCronNext(serverId, timerId, delay, name);
		} catch (Exception ex) {
			logger.error("", ex);
		}
	}

	private void scheduleCronNext(int serverId, long timerId, long delay, String name) {
		timersFuture.put(timerId, Task.scheduleUnsafe(delay, () -> fireCronLocal(serverId, timerId, name)));
	}

	public static Calendar getNextValidTimeAfter(String cron, Calendar calendar) throws ParseException {
		var cronExpression = new CronExpression(cron);
		var nextCalender = Calendar.getInstance();
		var nextDate = cronExpression.getNextValidTimeAfter(calendar.getTime());
		nextCalender.setTime(nextDate);
		return nextCalender;
	}

	public static Calendar getNextValidTimeAfter(String cron, long timeMills) throws ParseException {
		var cronExpression = new CronExpression(cron);
		var nextCalender = Calendar.getInstance();
		var date = new Date();
		date.setTime(timeMills);
		var calendar = Calendar.getInstance();
		calendar.setTime(date);
		var nextDate = cronExpression.getNextValidTimeAfter(calendar.getTime());
		nextCalender.setTime(nextDate);
		return nextCalender;
	}

	public static void nextCronTimer(BCronTimer cronTimer) throws ParseException {
		cronTimer.setExpectedTimeMills(cronTimer.getNextExpectedTimeMills());
		long tempMills = cronTimer.getExpectedTimeMills();
		var nextTimeMills = getNextValidTimeAfter(cronTimer.getCronExpression(), tempMills).getTimeInMillis();
		cronTimer.setNextExpectedTimeMills(nextTimeMills);
		long curTimeMills = System.currentTimeMillis();
		cronTimer.setHappenTimeMills(curTimeMills);
	}

	private void fireCronLocal(int serverId, long timerId, String name) {
		final var handle = timerHandles.get(name);
		Task.Call(zeze.NewProcedure(() -> {
			var index = _tIndexs.get(timerId);
			if (null != index) {
				var node = _tNodes.get(index.getNodeId());
				if (null != node) {
					if (handle == null) {
						cancelTimerLocal(serverId, timerId, index, node);
					} else {
						var timer = node.getTimers().get(timerId);
						var cronTimer = timer.getTimerObj_Zeze_Builtin_Timer_BCronTimer();
						nextCronTimer(cronTimer);
						final var context = new TimerContext(timer,
								cronTimer.getHappenTimeMills(),
								cronTimer.getNextExpectedTimeMills(),
								cronTimer.getExpectedTimeMills());
						/* skip nest procdure result */
						Task.Call(zeze.NewProcedure(() -> {
							handle.run(context);
							return Procedure.Success;
						}, "fireLocalHandle"));

						long delay = context.nextExpectedTimeMills - System.currentTimeMillis();
						scheduleCronNext(serverId, timerId, delay, name);
					}
				}
			}
			return 0L;
		}, "fireCronLocal"));
	}

	private void loadTimer() {
		var serverId = zeze.getConfig().getServerId();
		final var out = new OutObject<BNodeRoot>();
		if (Procedure.Success == Task.Call(zeze.NewProcedure(() ->
		{
			var root = _tNodeRoot.getOrAdd(serverId);
			// 本地每次load都递增。用来处理和接管的并发。
			root.setLoadSerialNo(root.getLoadSerialNo() + 1);
			out.Value = root.Copy(); // TODO zyao runwhileSucc
			return 0L;
		}, "LoadTimerLocal"))) {
			var root = out.Value;
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

		var result = Task.Call(zeze.NewProcedure(() ->
		{
			var src = _tNodeRoot.get(serverId);
			if (null == src || src.getHeadNodeId() == 0 || src.getTailNodeId() == 0)
				return 0L; // nothing need to do.

			if (src.getLoadSerialNo() != loadSerialNo)
				return 0L; // 需要接管的机器已经活过来了。

			// prepare splice
			var root = _tNodeRoot.getOrAdd(zeze.getConfig().getServerId());
			var srchead = _tNodes.get(src.getHeadNodeId());
			var srctail = _tNodes.get(src.getTailNodeId());
			var head = _tNodes.get(root.getHeadNodeId());
			//var tail = _tNodes.get(root.getTailNodeId());

			// 先保存存储过程退出以后需要装载的timer范围。
			first.Value = src.getHeadNodeId();
			last.Value = root.getHeadNodeId();
			// splice
			srctail.setNextNodeId(root.getHeadNodeId());
			root.setHeadNodeId(src.getHeadNodeId());
			head.setPrevNodeId(src.getTailNodeId());
			srchead.setPrevNodeId(root.getTailNodeId());
			// clear src
			src.setHeadNodeId(0L);
			src.setTailNodeId(0L);
			return 0L;
		}, "SpliceAndLoadTimerLocal"));

		if (0L == result) {
			return loadTimer(first.Value, last.Value, serverId);
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
					var simpleTimer = (BSimpleTimer) timer.getTimerObj().getBean();
					scheduleSimpleLocal(serverId, timer.getTimerId(), simpleTimer.getNextExpectedTimeMills() - System.currentTimeMillis(), simpleTimer.getPeriod(), timer.getName());
				} else {
					var cronTimer = (BCronTimer) timer.getTimerObj().getBean();
					scheduleCronLocal(serverId, timer.getTimerId(), cronTimer.getCronExpression(), timer.getName());
				}
				if (serverId != zeze.getConfig().getServerId()) {
					Task.Call(zeze.NewProcedure(() -> {
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
