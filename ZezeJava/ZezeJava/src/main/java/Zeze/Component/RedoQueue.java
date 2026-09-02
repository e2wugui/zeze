package Zeze.Component;

import java.nio.charset.StandardCharsets;
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
import org.jspecify.annotations.NonNull;
import org.rocksdb.RocksDBException;

/**
 * 连接：
 * 1.每个队列一个连接服务。
 * 2.可以从可用的zeze-server中选择部分，配置到zeze.xml中。
 * 3.【可选】使用ServiceManager动态发现zeze-server。感觉没有必要。
 */
public class RedoQueue extends HandshakeClient {
	private static final org.apache.logging.log4j.Logger logger =
			org.apache.logging.log4j.LogManager.getLogger(RedoQueue.class);
	private RocksDatabase db;
	private RocksDatabase.Table tableLastDoneTaskId;
	private RocksDatabase.Table tableTaskQueue;
	private long lastTaskId;
	private long lastDoneTaskId;
	private final byte[] lastDoneTaskIdKey = "LastDoneTaskId".getBytes(StandardCharsets.UTF_8);
	private RunTask pending;
	private AsyncSocket socket;
	// 失败/超时后的延迟重试，单flight（已在途则不重复排）。stop必须取消，否则触发时tableTaskQueue已close。
	private Future<?> retryTask;
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
				if (qit.isValid()) {
					var last = ByteBuffer.Wrap(qit.key());
					lastTaskId = last.ReadLong();
				}
			}
			var done = tableLastDoneTaskId.get(lastDoneTaskIdKey);
			if (done != null)
				lastDoneTaskId = ByteBuffer.Wrap(done).ReadLong();
			super.start();
		} finally {
			unlock();
		}
	}

	@Override
	public void stop() throws Exception {
		lock();
		try {
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
			var key = ByteBuffer.Allocate(9);
			key.WriteLong(++lastTaskId);

			var task = new BQueueTask();
			task.setQueueName(getName());
			task.setPrevTaskId(lastTaskId - 1);
			task.setTaskId(lastTaskId);
			task.setTaskType(taskType);
			var param = ByteBuffer.Allocate(1024 + 16);
			taskParam.encode(param);
			task.setTaskParam(new Binary(param.Bytes, 0, param.WriteIndex));
			var value = ByteBuffer.Allocate(1024 + 16);
			task.encode(value);

			// 保存完整的rpc请求，重新发送的时候不用再次打包。
			tableTaskQueue.put(key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);
			tryStartSendNextTask(task, null);
		} catch (RocksDBException e) {
			Task.forceThrow(e);
		} finally {
			unlock();
		}
	}

	private void tryStartSendNextTask(BQueueTask add, AsyncSocket socket) throws RocksDBException {
		if (pending != null)
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
				var value = tableTaskQueue.get(key.Bytes, 0, key.WriteIndex);
				if (value == null)
					return; // error
				rpc.Argument.decode(ByteBuffer.Wrap(value));
			}
			if (this.socket == null) {
				this.socket = socket;
				if (this.socket == null) {
					this.socket = GetSocket();
					if (this.socket == null)
						return;
				}
			}
			if (rpc.Send(this.socket, this::processRunTaskResult))
				pending = rpc;
		}
	}

	private long processRunTaskResult(Rpc<BQueueTask, BTaskId> rpc) throws Exception {
		lock();
		try {
			if (pending != rpc)
				return Procedure.LogicError;

			pending = null;
			if (null == tableTaskQueue)
				return Procedure.LogicError; // stop与响应回调竞态：stop持锁清理时未清pending，表已置null（同scheduleRetry内的守卫）
			if (rpc.getResultCode() == 0L || rpc.getResultCode() == Procedure.ErrorRequestId) {
				lastDoneTaskId = rpc.Result.getTaskId();
				var value = ByteBuffer.Allocate(9);
				value.WriteLong(lastDoneTaskId);
				tableLastDoneTaskId.put(lastDoneTaskIdKey, 0, lastDoneTaskIdKey.length, value.Bytes, 0, value.WriteIndex);
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
		} finally {
			unlock();
		}
	}

	private void scheduleRetry() {
		if (retryTask != null)
			return; // 单flight：已在途的重试足够驱动泵

		retryTask = TaskSpec.ofAction(() -> {
			lock();
			try {
				retryTask = null;
				if (null == tableTaskQueue)
					return; // stop与已触发的重试竞态：stop持锁先完成（cancel拦不住已启动的任务），db已关闭
				tryStartSendNextTask(null, null);
			} catch (RocksDBException e) {
				Task.forceThrow(e);
			} finally {
				unlock();
			}
		}).scheduleNow(RETRY_DELAY_MS);
	}

	@Override
	public void OnHandshakeDone(@NonNull AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);
		lock();
		try {
			tryStartSendNextTask(null, so);
		} finally {
			unlock();
		}
	}

	@Override
	public void OnSocketClose(@NonNull AsyncSocket so, Throwable ex) throws Exception {
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
