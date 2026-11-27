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
			// 排空队列重启时 tableTaskQueue 为空，须以独立持久化的完成水位为下界避免任务号回绕
			lastTaskId = Math.max(lastTaskId, lastDoneTaskId);
			deleteDoneTasks(); // 清理崩溃残留的已完成条目
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
			// 先落盘成功再推进内存 lastTaskId，避免写失败留下任务号空洞
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

	// 删除已完成任务 [0, doneTaskId+1)；变长键须按 WriteIndex 截断再传入 deleteRange，避免误删前缀匹配的下一跳
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
					// 下一跳任务缺失属于不可自愈空洞，记录 FATAL 停泵待运维介入
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
				// 服务端水位须落在 [lastDoneTaskId, lastTaskId] 内，越界则 FATAL 停泵防热循环或误删未执行任务
				var serverTaskId = rpc.Result.getTaskId();
				if (serverTaskId < lastDoneTaskId || serverTaskId > lastTaskId) {
					logger.fatal("server watermark out of range, halt pump for ops. queue={}, localDone={}, localLast={}, server={}",
							getName(), lastDoneTaskId, lastTaskId, serverTaskId);
					return 0L;
				}
				persistCompletion(serverTaskId);
				tryStartSendNextTask(null, rpc.getSender());
				return 0L;
			}

			// 失败或超时时延迟重发，协议按 prevTaskId 幂等保证安全
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
