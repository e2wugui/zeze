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
import org.jspecify.annotations.Nullable;

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
	private @Nullable Future<?> timer;
	private @Nullable AutoKey jobIdAutoKey;

	public DelayRemove(Application zz) {
		this.zeze = zz;

		var serverId = zz.getConfig().getServerId();
		queue = zz.getQueueModule().open("__GCTableQueue#" + serverId, BTableKey.class);
		RegisterZezeTables(zeze);
	}

	public void start() {
		if (null != timer)
			return;

		// 根据配置时间窗随机生成每日首次执行时间，此后按 24 小时间隔执行 GC
		var firstTime = Calendar.getInstance();
		firstTime.set(Calendar.HOUR_OF_DAY, zeze.getConfig().getDelayRemoveHourStart());
		firstTime.set(Calendar.MINUTE, 0);
		firstTime.set(Calendar.SECOND, 0);
		firstTime.set(Calendar.MILLISECOND, 0);

		var minutes = 60 * (zeze.getConfig().getDelayRemoveHourEnd() - zeze.getConfig().getDelayRemoveHourStart());
		if (minutes <= 0)
			minutes = 60;
		minutes = Random.getInstance().nextInt(minutes);
		firstTime.add(Calendar.MINUTE, minutes);

		if (firstTime.before(Calendar.getInstance()))
			firstTime.add(Calendar.DAY_OF_MONTH, 1);

		var delay = firstTime.getTime().getTime() - System.currentTimeMillis();
		var period = 24 * 3600 * 1000;
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
		if (jobIdAutoKey == null)
			throw new IllegalStateException("DelayRemove not started. call start() before addJob().");
		if (!jobHandles.containsKey(handleName))
			throw new IllegalStateException("JobHandle not registered: " + handleName);
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

		Transaction.whileCommit(() -> startJob(jobId, bJob.getJobHandleName(), bJob.getJobState()));
	}

	/**
	 * 更新任务状态。
	 *
	 * @param jobId 任务 ID
	 * @param state 任务状态，为 null 表示任务已完成并移除记录
	 */
	public void setJobState(String jobId, @Nullable Bean state) {
		if (null != state) {
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

	// 装载尚未完成的 Job，需在所有模块 start 之后调用
	public void continueJobs() {
		zeze.newProcedure(() -> {
			var jobs = _tJobs.getOrAdd(zeze.getConfig().getServerId());
			for (var it = jobs.getJobs().entrySet().iterator(); it.hasNext(); ) {
				var e = it.next();
				// 清理未注册 handle 的存量僵尸 Job，避免每次重启重复失败
				if (!jobHandles.containsKey(e.getValue().getJobHandleName())) {
					logger.error("DelayRemove.continueJobs: JobHandle not registered, discard zombie job."
							+ " jobId={}, handleName={}", e.getKey(), e.getValue().getJobHandleName());
					it.remove();
					continue;
				}
				var jobId = e.getKey();
				var job = e.getValue();
				Transaction.whileCommit(() -> startJob(jobId, job.getJobHandleName(), job.getJobState()));
			}
			return 0;
		}, "DelayRemove.continueJobs").call();
	}

	private void startJob(String jobId, String handleName, Binary jobState) {
		TaskSpec.ofAction(() -> {
			var handle = jobHandles.get(handleName);
			handle.process(this, jobId, jobState);
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

	void onTimer() {
		// 按队列节点分事务删除，避免单事务删除过多记录
		var days = zeze.getConfig().getDelayRemoveDays();
		if (days < 7)
			days = 7; // 至少保留7天。
		long diffMills = (long)days * 24 * 3600 * 1000;
		var removing = new OutObject<>(true);
		while (java.util.Objects.requireNonNull(removing.value)) {
			var rc = zeze.newProcedure(() -> {
				var node = queue.peekNode(); // 先 peek 检查到期再 pollNode，未到期不出队
				if (node == null) {
					removing.value = false;
					return 0;
				}

				// 仅按节点首项（最老项）判定过期：未过期则整节点保留并结束，过期则整节点一并清理
				if (!node.getValues().isEmpty()) {
					var first = (BTableKey)node.getValues().getFirst().getValue().getBean();
					if (diffMills > System.currentTimeMillis() - first.getEnqueueTime()) {
						removing.value = false;
						return 0;
					}
				}

				queue.pollNode();

				// 空节点 maxTime 保持 0，会继续尝试清理下一节点
				long maxTime = 0;
				for (var value : node.getValues()) {
					var tableKey = (BTableKey)value.getValue().getBean();
					maxTime = tableKey.getEnqueueTime();
					var table = zeze.getTable(tableKey.getTableName());
					if (null != table)
						table.removeEncodedKey(tableKey.getEncodedKey());
				}
				removing.value = diffMills < System.currentTimeMillis() - maxTime;
				return 0;
			}, "DelayRemove.delayRemoveProcedure").call();
			if (rc != Procedure.Success) {
				// 节点事务失败已回滚，告警并退出本轮以避免确定性故障死循环，留待下个周期重试
				logger.warn("DelayRemove.onTimer procedure failed: rc={}. " +
						"Remaining nodes are deferred to the next period.", rc);
				break;
			}
			if (Thread.currentThread().isInterrupted())
				break;
		}
	}
}
