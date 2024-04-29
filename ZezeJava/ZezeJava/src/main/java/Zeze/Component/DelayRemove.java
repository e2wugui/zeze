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
import Zeze.Transaction.TableX;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutObject;
import Zeze.Util.Random;
import Zeze.Util.Task;

/**
 * 每个ServerId分配一个独立的GC队列。Server之间不会争抢。如果一个Server一直没有起来，那么它的GC就一直不会执行。
 */
public class DelayRemove extends AbstractDelayRemove {
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

	public DelayRemove(Application zz) {
		this.zeze = zz;

		var serverId = zz.getConfig().getServerId();
		queue = zz.getQueueModule().open("__GCTableQueue#" + serverId, BTableKey.class);
		RegisterZezeTables(zeze);
	}

	public void start() {
		if (null != timer)
			return;

		// start timer to gc. work on queue.pollNode? peekNode? poll? peek?
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
		timer = Task.scheduleUnsafe(delay, period, this::onTimer);
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
		var jobId = jobIdAutoKey.nextString();
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
		Task.run(() -> {
			var handle = jobHandles.get(job.getJobHandleName());
			handle.process(this, jobId, job.getJobState());
		}, "DelayRemove.startJob");
	}

	public void stop() {
		if (null != timer) {
			timer.cancel(true);
			timer = null;
		}
	}

	private void onTimer() {
		// delayRemove可能需要删除很多记录，不能在一个事务内完成全部删除。
		// 这里按每个节点的记录的删除在一个事务中执行，节点间用不同的事务。
		var days = zeze.getConfig().getDelayRemoveDays();
		if (days < 7)
			days = 7; // xxx 至少保留7天。
		var diffMills = days * 24 * 3600 * 1000;
		var removing = new OutObject<>(true);
		while (removing.value) {
			zeze.newProcedure(() -> {
				var node = queue.pollNode();
				if (node == null) {
					removing.value = false;
					return 0;
				}

				// 检查节点的第一个（最老的）项是否需要删除。
				// 如果不需要，那么整个节点都不会删除，并且中断循环。
				// 如果需要，那么整个节点都删除，即使中间有一些没有达到过期。
				// 这是个不精确的删除过期的方法。
				if (!node.getValues().isEmpty()) {
					var first = (BTableKey)node.getValues().get(0).getValue().getBean();
					if (diffMills > System.currentTimeMillis() - first.getEnqueueTime()) {
						removing.value = false;
						return 0;
					}
				}

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
		}
	}
}
