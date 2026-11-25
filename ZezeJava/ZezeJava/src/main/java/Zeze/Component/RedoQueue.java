package Zeze.Component;

import java.util.Objects;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Future;
import Zeze.Builtin.RedoQueue.BQueueTask;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Builtin.RedoQueue.RunTask;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Services.HandshakeClient;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import org.rocksdb.RocksDBException;
import org.jspecify.annotations.Nullable;
import Zeze.Util.RocksDatabase.Table;

/**
 * 连接：
 * 1.每个队列一个连接服务。
 * 2.可以从可用的zeze-server中选择部分，配置到zeze.xml中。
 * 3.【可选】使用ServiceManager动态发现zeze-server。感觉没有必要。
 */
public class RedoQueue extends HandshakeClient {
	private static final org.apache.logging.log4j.Logger logger =
			org.apache.logging.log4j.LogManager.getLogger(RedoQueue.class);
	private @Nullable RocksDatabase db;
	private @Nullable Table tableLastDoneTaskId;
	private @Nullable Table tableTaskQueue;
	private long lastTaskId;
	private long lastDoneTaskId;
	private final byte[] lastDoneTaskIdKey = "LastDoneTaskId".getBytes(StandardCharsets.UTF_8);
	private @Nullable RunTask pending;
	private @Nullable AsyncSocket socket;
	// 失败/超时后的延迟重试，单flight（已在途则不重复排）。stop必须取消，否则触发时tableTaskQueue已close。
	private @Nullable Future<?> retryTask;
	private static final long RETRY_DELAY_MS = 5_000;

	public RedoQueue(String name, Config config) {
		super(name, config);
	}

	@Override
	public void start() throws Exception {
		lock();
		try {
			if (db != null)
				return;
			db = new RocksDatabase(getName());
			tableLastDoneTaskId = db.getOrAddTable("FamilyLastDoneTaskId");
			tableTaskQueue = db.getOrAddTable("FamilyTaskQueue");
			try (var qit = tableTaskQueue.iterator()) {
				qit.seekToLast();
				if (RocksDatabase.isValidOrThrow(qit)) {
					var last = ByteBuffer.Wrap(qit.key());
					lastTaskId = last.ReadLong();
				}
			}
			var done = tableLastDoneTaskId.get(lastDoneTaskIdKey);
			if (done != null)
				lastDoneTaskId = ByteBuffer.Wrap(done).ReadLong();
			// 排空队列重启后的水位回绕：队列排空时tableTaskQueue已无条目，
			// lastTaskId恢复为默认0，而lastDoneTaskId=N（水位表独立持久化）——水位"回绕"为
			// lastDoneTaskId>lastTaskId：泵条件lastDoneTaskId<lastTaskId永假，新增任务静默滞留；
			// 且新任务从id=1重新分配，落入水位之下的已删区间（下一次水位推进会连带误删）。
			// 钳制对齐：以水位为下界恢复lastTaskId，新增任务从N+1继续。
			lastTaskId = Math.max(lastTaskId, lastDoneTaskId);
			deleteDoneTasks(); // 清理崩溃窗口残留（水位已推进但删除未执行）
			super.start();
		} finally {
			unlock();
		}
	}

	@Override
	public void stop() throws Exception {
		lock();
		try {
			pending = null;
			socket = null;
			if (retryTask != null) {
				retryTask.cancel(false);
				retryTask = null;
			}
			super.stop();
			if (db != null) {
				db.close();
				db = null;
				tableLastDoneTaskId = null;
				tableTaskQueue = null;
			}
		} finally {
			unlock();
		}
	}

	public void add(int taskType, Serializable taskParam) {
		lock();
		try {
			// 先落盘成功再推进内存lastTaskId：先++lastTaskId后put、put抛
			// RocksDBException时内存已前进而盘上无此任务：后续泵读lastDoneTaskId+1命中空洞
			// 永久停摆，重启后lastTaskId从DB最大键恢复，洞仍在，不可自愈。
			var newTaskId = lastTaskId + 1;
			var key = ByteBuffer.Allocate(9);
			key.WriteLong(newTaskId);

			var task = new BQueueTask();
			task.setQueueName(getName());
			task.setPrevTaskId(newTaskId - 1);
			task.setTaskId(newTaskId);
			task.setTaskType(taskType);
			var param = ByteBuffer.Allocate(1024 + 16);
			taskParam.encode(param);
			task.setTaskParam(new Binary(param.Bytes, 0, param.WriteIndex));
			var value = ByteBuffer.Allocate(1024 + 16);
			task.encode(value);

			// 保存完整的rpc请求，重新发送的时候不用再次打包。
			//noinspection DataFlowIssue
			tableTaskQueue.put(key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);
			lastTaskId = newTaskId;
			tryStartSendNextTask(task, null);
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		} finally {
			unlock();
		}
	}

	// 删除水位（含）以下条目。重发只读lastDoneTaskId以上，以下条目（任务正文全量落盘）
	// 永不清理=本地RocksDB无界增长。key为WriteLong变长编码（非负时字节序保序，非定长8字节大端），
	// [key(0),key(lastDoneTaskId+1))即taskId<=lastDoneTaskId的全部；与水位推进同锁同线程，
	// 重启时start()再补一次（清崩溃残留）。边界必须按WriteIndex截断后传入：deleteRange
	// 按字节序整比较，未截断的enc(N+1)填充数组会把真前缀key(N+1)（下一跳待发任务）也括进区间。
	private void deleteDoneTasks() throws RocksDBException {
		deleteDoneTasks(lastDoneTaskId);
	}

	private void deleteDoneTasks(long doneTaskId) throws RocksDBException {
		var first = ByteBuffer.Allocate(9);
		first.WriteLong(0);
		var end = ByteBuffer.Allocate(9);
		end.WriteLong(doneTaskId + 1);
		Objects.requireNonNull(tableTaskQueue).deleteRange(Arrays.copyOf(first.Bytes, first.WriteIndex),
				Arrays.copyOf(end.Bytes, end.WriteIndex));
	}

	private void tryStartSendNextTask(@Nullable BQueueTask add, @Nullable AsyncSocket preferredSocket)
			throws RocksDBException {
		var taskQueue = tableTaskQueue;
		if (pending != null || taskQueue == null)
			return;

		if (lastDoneTaskId < lastTaskId) {
			var taskId = lastDoneTaskId + 1;
			var rpc = new RunTask();
			if (add != null && taskId == add.getTaskId())
				rpc.Argument = add; // 最近加入的就是要发送的。优化！
			else {
				// 最近加入的不是要发送的，从Db中读取。
				var key = ByteBuffer.Allocate(9);
				key.WriteLong(taskId);
				var value = taskQueue.get(key.Bytes, 0, key.WriteIndex);
				if (value == null) {
					// 水位下一跳任务在DB中不存在（历史put失败留下的空洞，或外部
					// 删数据）。洞不会自愈：重启后lastTaskId从DB最大键恢复，水位之下的洞
					// 保持，队列永久停摆且无任何日志。FATAL让运维介入（人工补洞或重置水位）。
					logger.fatal("task queue hole! queue={}, taskId={}, lastDoneTaskId={}, lastTaskId={}",
							getName(), taskId, lastDoneTaskId, lastTaskId);
					return;
				}
				rpc.Argument.decode(ByteBuffer.Wrap(value));
			}
			var sendSocket = socket;
			if (sendSocket == null || sendSocket.isClosed()) {
				// 迟到的应答也可能携带已执行完 OnSocketClose 的连接。
				sendSocket = preferredSocket != null && !preferredSocket.isClosed() ? preferredSocket : GetSocket();
				if (sendSocket == null || sendSocket.isClosed()) {
					socket = null;
					return;
				}
				socket = sendSocket;
			}
			if (rpc.Send(sendSocket, this::processRunTaskResult))
				pending = rpc;
			else {
				// Send=false（socket失效/输出背压丢包不断连）：无重连事件，复用单flight重试
				// 驱动泵；error带queue/taskId归因（任务体超outputBufferMaxSize持续失败留运维补洞）。
				logger.error("RunTask send fail, schedule retry. queue={}, taskId={}",
						getName(), rpc.Argument.getTaskId());
				scheduleRetry();
			}
		}
	}

	private void persistCompletion(long taskId) throws RocksDBException {
		var value = ByteBuffer.Allocate(9);
		value.WriteLong(taskId);
		// 完成水位先落盘，删除失败可重放本次应答；内存仅在两步均完成后发布。
		//noinspection DataFlowIssue
		tableLastDoneTaskId.put(lastDoneTaskIdKey, 0, lastDoneTaskIdKey.length, value.Bytes, 0, value.WriteIndex);
		deleteDoneTasks(taskId);
		lastDoneTaskId = taskId;
	}

	private long processRunTaskResult(Rpc<BQueueTask, BTaskId> rpc) throws Exception {
		lock();
		try {
			if (pending != rpc || tableTaskQueue == null)
				return Procedure.LogicError;

			pending = null;
			if (rpc.getResultCode() == 0L || rpc.getResultCode() == Procedure.ErrorRequestId) {
				// 服务端水位必须落在本地有效区间 [lastDoneTaskId, lastTaskId] 内：
				// 回退（< lastDoneTaskId）在服务端换库/清数据时出现，续泵会陷入无退避热循环写放大；
				// 超前（> lastTaskId）在本地回滚旧快照或双客户端复用同名队列时出现，盲目采纳会
				// 误删服务端从未执行过的本地任务并使 lastDoneTaskId > lastTaskId 导致泵静默停摆。
				var serverTaskId = rpc.Result.getTaskId();
				if (serverTaskId < lastDoneTaskId || serverTaskId > lastTaskId) {
					// 对齐hole分支（宁停不错）：FATAL停摆，等运维重置本地水位或重放服务端状态。
					logger.fatal("server watermark out of range, halt pump for ops. queue={}, localDone={}, localLast={}, server={}",
							getName(), lastDoneTaskId, lastTaskId, serverTaskId);
					return 0L;
				}
				persistCompletion(serverTaskId);
				tryStartSendNextTask(null, rpc.getSender());
				return 0L;
			}

			// 失败或超时：连接仍在时没有事件再驱动泵（仅add/重连会），队列会永久停摆。
			// 协议按prevTaskId幂等：超时后任务可能已被服务端应用，重发会得到ErrorRequestId并采纳服务端进度，安全。
			// NotImplement等永久配置错误下持续重试并刷warn，运维修复后队列自动继续。
			logger.warn("task fail, schedule retry. queue={}, taskId={}, resultCode={}",
					getName(), rpc.Argument.getTaskId(), rpc.getResultCode());
			scheduleRetry();
			return rpc.getResultCode();
		} catch (RocksDBException e) {
			scheduleRetry();
			throw e;
		} finally {
			unlock();
		}
	}

	private void scheduleRetry() {
		if (retryTask != null)
			return; // 单flight：已在途的重试足够驱动泵

		var retryTable = tableTaskQueue;
		retryTask = TaskSpec.ofAction(() -> {
			lock();
			try {
				if (retryTable == null || retryTable != tableTaskQueue)
					return; // 已开始的旧重试可能越过 stop/start，不能清理新一轮的重试句柄。
				retryTask = null;
				tryStartSendNextTask(null, null);
			} catch (RocksDBException e) {
				scheduleRetry();
				throw Task.forceThrow(e);
			} finally {
				unlock();
			}
		}).scheduleNow(RETRY_DELAY_MS);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);
		lock();
		try {
			// 等待队列锁期间，旧连接可能已关闭，队列也可能已重启。
			if (!so.isClosed())
				tryStartSendNextTask(null, so);
		} finally {
			unlock();
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket so, @Nullable Throwable ex) throws Exception {
		super.OnSocketClose(so, ex);
		lock();
		try {
			if (socket == so)
				socket = null;
		} finally {
			unlock();
		}
	}
}
