package Zeze.Component;

import java.nio.charset.StandardCharsets;
import Zeze.Builtin.RedoQueue.BQueueTask;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Builtin.RedoQueue.RunTask;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Services.HandshakeClient;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.rocksdb.RocksDBException;

/**
 * 连接：
 * 1.每个队列一个连接服务。
 * 2.可以从可用的zeze-server中选择部分，配置到zeze.xml中。
 * 3.【可选】使用ServiceManager动态发现zeze-server。感觉没有必要。
 */
public class RedoQueue extends HandshakeClient {
	private RocksDatabase db;
	private RocksDatabase.Table tableLastDoneTaskId;
	private RocksDatabase.Table tableTaskQueue;
	private long lastTaskId;
	private long lastDoneTaskId;
	private final byte[] lastDoneTaskIdKey = "LastDoneTaskId".getBytes(StandardCharsets.UTF_8);
	private RunTask pending;
	private AsyncSocket socket;

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
			if (rpc.getResultCode() == 0L || rpc.getResultCode() == Procedure.ErrorRequestId) {
				lastDoneTaskId = rpc.Result.getTaskId();
				var value = ByteBuffer.Allocate(9);
				value.WriteLong(lastDoneTaskId);
				tableLastDoneTaskId.put(lastDoneTaskIdKey, 0, lastDoneTaskIdKey.length, value.Bytes, 0, value.WriteIndex);
				tryStartSendNextTask(null, rpc.getSender());
				return 0L;
			}

			return rpc.getResultCode();
		} finally {
			unlock();
		}
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);
		lock();
		try {
			tryStartSendNextTask(null, so);
		} finally {
			unlock();
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable ex) throws Exception {
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
