package Zeze.Component;

import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Builtin.DelayRemove.BJob;
import Zeze.Builtin.DelayRemove.BTableKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableX;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutObject;
import Zeze.Util.Random;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 每个ServerId分配一个独立的GC队列。Server之间不会争抢。如果一个Server一直没有起来，那么它的GC就一直不会执行。
 */
public class DelayRemove extends AbstractDelayRemove {
	static final Logger logger = LogManager.getLogger(DelayRemove.class);

	public <K extends Comparable<K>> void remove(TableX<K, ?> table, K key) {
		var value = new BTableKey();
		value.setTableName(table.getName());
		value.setEncodedKey(new Binary(table.encodeKey(key)));
		value.setEnqueueTime(System.currentTimeMillis());
		queue.add(value);
	}

	private final Zeze.Collections.Queue<BTableKey> queue;
	public final Application zeze;
	private Future<?> timer;
	private AutoKey jobIdAutoKey;

	// 见addJob（FND-C1-10）：start()之前的调用懒初始化。
	private AutoKey jobIdAutoKey() {
		var aka = jobIdAutoKey;
		return aka != null ? aka : (jobIdAutoKey = zeze.getAutoKey("__GCTableJobIdAutoKey"));
	}

	public DelayRemove(Application zz) {
		this.zeze = zz;

		var serverId = zz.getConfig().getServerId();
		queue = zz.getQueueModule().open("__GCTableQueue#" + serverId, BTableKey.class);
		RegisterZezeTables(zeze);
	}

	public void start() {
		if (null != timer)
			return;

		// start timer to gc. onTimer 采用"先peekNode检查头节点、到期才pollNode出队"的策略。
		// 根据配置的Timer的时间范围，按分钟精度随机出每天的开始时间，最后计算延迟，然后按24小时间隔执行。
		var firstTime = Calendar.getInstance();
		firstTime.set(Calendar.HOUR_OF_DAY, zeze.getConfig().getDelayRemoveHourStart());
		firstTime.set(Calendar.MINUTE, 0);
		firstTime.set(Calendar.SECOND, 0);
		firstTime.set(Calendar.MILLISECOND, 0);

		// rand to end
		var minutes = 60 * (zeze.getConfig().getDelayRemoveHourEnd() - zeze.getConfig().getDelayRemoveHourStart());
		if (minutes <= 0)
			minutes = 60;
		minutes = Random.getInstance().nextInt(minutes);
		firstTime.add(Calendar.MINUTE, minutes);

		if (firstTime.before(Calendar.getInstance())) // 如果第一次的时间比当前时间早，推到明天。
			firstTime.add(Calendar.DAY_OF_MONTH, 1); // tomorrow!

		var delay = firstTime.getTime().getTime() - System.currentTimeMillis();
		var period = 24 * 3600 * 1000; // 24 hours
		timer = TaskSpec.ofAction(this::onTimer).schedulePeriodNow(delay, period);
		jobIdAutoKey = zeze.getAutoKey("__GCTableJobIdAutoKey");
	}

	@FunctionalInterface
	public interface JobHandle {
		void process(DelayRemove delayRemove, String jobId, Binary jobState) throws Exception;
	}

	private final ConcurrentHashMap<String, JobHandle> jobHandles = new ConcurrentHashMap<>();

	public void register(String handleName, JobHandle handle) {
		if (jobHandles.putIfAbsent(handleName, handle) != null)
			throw new IllegalStateException("duplicate JobHandle Name = " + handleName);
	}

	public void addJob(String handleName, Bean state) {
		var bJob = new BJob();
		// FND-C1-10：jobIdAutoKey原仅在start()内初始化，而Application.start先逐模块impl.start()
		// （Application.java:735）后delayRemove.start()（:758）——模块Start()里触发LinkedMap.clear()
		// （内部addJob）时字段尚为null，以裸NPE失败且无提示。懒初始化放行合法的早期调用；
		// start()内的赋值保留（幂等，同一name返回同一实例）。addJob自身要求事务上下文，
		// 并发懒初始化按getOrAdd语义收敛到同一实例。
		var jobId = jobIdAutoKey().nextString();
		bJob.setJobHandleName(handleName);
		var preAllocSize = state.preAllocSize();
		var bb = ByteBuffer.Allocate(preAllocSize);
		state.encode(bb);
		if (bb.WriteIndex > preAllocSize)
			state.preAllocSize(bb.WriteIndex);
		bJob.setJobState(new Binary(bb));
		var jobs = _tJobs.getOrAdd(zeze.getConfig().getServerId());
		jobs.getJobs().put(jobId, bJob);

		Transaction.whileCommit(() -> startJob(jobId, bJob));
	}

	/**
	 * set job state
	 *
	 * @param jobId jobId
	 * @param state state, null means job is done.
	 */
	public void setJobState(String jobId, Bean state) {
		if (null != state) {
			// 修改数据表中的状态。
			var jobs = _tJobs.getOrAdd(zeze.getConfig().getServerId());
			var bJob = jobs.getJobs().get(jobId);
			if (bJob != null) {
				var preAllocSize = state.preAllocSize();
				var bb = ByteBuffer.Allocate(preAllocSize);
				state.encode(bb);
				if (bb.WriteIndex > preAllocSize)
					state.preAllocSize(bb.WriteIndex);
				bJob.setJobState(new Binary(bb));
			}
			return;
		}

		var jobs = _tJobs.getOrAdd(zeze.getConfig().getServerId());
		jobs.getJobs().remove(jobId);
	}

	// 装载还没有完成的Job。需要在所有模块都start之后调用。
	public void continueJobs() {
		zeze.newProcedure(() -> {
			var jobs = _tJobs.getOrAdd(zeze.getConfig().getServerId());
			for (var e : jobs.getJobs())
				startJob(e.getKey(), e.getValue());
			return 0;
		}, "DelayRemove.continueJobs").call();
	}

	private void startJob(String jobId, BJob job) {
		TaskSpec.ofAction(() -> {
			var handle = jobHandles.get(job.getJobHandleName());
			handle.process(this, jobId, job.getJobState());
		}).name("DelayRemove.startJob").run();
	}

	public void stop() {
		if (null != timer) {
			timer.cancel(true);
			timer = null;
		}
	}

	/**
	 * 当前未完成的Job数量。需要在事务内调用。
	 */
	public int jobCount() {
		var jobs = _tJobs.get(zeze.getConfig().getServerId());
		return null != jobs ? jobs.getJobs().size() : 0;
	}

	// 包内可见：测试在确定位置同步驱动一轮GC（见ZezeJavaTest的TestDelayRemoveOnTimer）。
	void onTimer() {
		// delayRemove可能需要删除很多记录，不能在一个事务内完成全部删除。
		// 这里按每个节点的记录的删除在一个事务中执行，节点间用不同的事务。
		var days = zeze.getConfig().getDelayRemoveDays();
		if (days < 7)
			days = 7; // xxx 至少保留7天。
		var diffMills = days * 24 * 3600 * 1000;
		var removing = new OutObject<>(true);
		while (removing.value) {
			var rc = zeze.newProcedure(() -> {
				var node = queue.peekNode(); // 先peek检查：未到期时节点不出队（pollNode会删行，事务提交后登记就脱离GC追踪）。
				if (node == null) {
					removing.value = false;
					return 0;
				}

				// 检查节点的第一个（最老的）项是否需要删除。
				// 如果不需要，那么整个节点都不会删除，并且中断循环。
				// 如果需要，那么整个节点都删除，即使中间有一些没有达到过期。
				// 这是个不精确的删除过期的方法。
				if (!node.getValues().isEmpty()) {
					var first = (BTableKey)node.getValues().getFirst().getValue().getBean();
					if (diffMills > System.currentTimeMillis() - first.getEnqueueTime()) {
						removing.value = false;
						return 0; // 未到期：本事务无任何写，直接提交即可，节点仍留在队列里。
					}
				}

				queue.pollNode(); // 确认到期后才真正出队删除。同一事务内快照一致，peek看到的头节点即本次删除的节点。

				// node.getValues().isEmpty，这一项将保持0，循环后设置removing.value将基本是true。
				// 即，空节点总是尝试继续删除。
				long maxTime = 0;
				for (var value : node.getValues()) {
					var tableKey = (BTableKey)value.getValue().getBean();
					// queue是按时间顺序的，记住最后一条即可。
					maxTime = tableKey.getEnqueueTime();
					var table = zeze.getTable(tableKey.getTableName());
					if (null != table)
						table.removeEncodedKey(tableKey.getEncodedKey());
				}
				removing.value = diffMills < System.currentTimeMillis() - maxTime;
				return 0;
			}, "DelayRemove.delayRemoveProcedure").call();
			if (rc != Procedure.Success) {
				// 失败的节点事务已回滚（队列无损）。确定性失败（如登记的EncodedKey损坏导致
				// removeEncodedKey解码异常）立即重试是纯热循环：告警并退出本轮，
				// 由固定延迟调度在下个周期整体重试（对比Timer.loadTimer的log+sleep(1s)重试，
				// 这里一天才跑一轮，立即重试没有意义）。
				logger.warn("DelayRemove.onTimer procedure failed: rc={}. " +
						"Remaining nodes are deferred to the next period.", rc);
				break;
			}
			if (Thread.currentThread().isInterrupted()) // stop()的cancel(true)：及时退出，剩余节点等下次调度。
				break;
		}
	}
}
