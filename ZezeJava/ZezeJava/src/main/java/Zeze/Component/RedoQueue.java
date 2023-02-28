package Zeze.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.RedoQueue.BQueueTask;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Builtin.RedoQueue.RunTask;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Services.HandshakeClient;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Transaction.Procedure;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

/**
 * 连接：
 * 1.每个队列一个连接服务。
 * 2.可以从可用的zeze-server中选择部分，配置到zeze.xml中。
 * 3.【可选】使用ServiceManager动态发现zeze-server。感觉没有必要。
 */
public class RedoQueue extends HandshakeClient {
	private RocksDB db;
	private final ConcurrentHashMap<String, ColumnFamilyHandle> families = new ConcurrentHashMap<>();
	private ColumnFamilyHandle familyLastDoneTaskId;
	private ColumnFamilyHandle familyTaskQueue;
	private long lastTaskId;
	private long lastDoneTaskId;
	private final byte[] lastDoneTaskIdKey = "LastDoneTaskId".getBytes(StandardCharsets.UTF_8);
	private RunTask pending;
	private AsyncSocket socket;

	static {
		RocksDB.loadLibrary();
	}

	public RedoQueue(String name, Config config) {
		super(name, config);
	}

	ColumnFamilyHandle getOrAddFamily(String name) {
		return families.computeIfAbsent(name, key -> {
			try {
				return db.createColumnFamily(new ColumnFamilyDescriptor(
						key.getBytes(StandardCharsets.UTF_8), DatabaseRocksDb.getDefaultCfOptions()));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public synchronized void start() throws Exception {
		if (db != null)
			return;

		var dbHome = super.getName();
		var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
		for (var cf : RocksDB.listColumnFamilies(DatabaseRocksDb.getCommonOptions(), dbHome))
			columnFamilies.add(new ColumnFamilyDescriptor(cf, DatabaseRocksDb.getDefaultCfOptions()));
		if (columnFamilies.isEmpty())
			columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(), DatabaseRocksDb.getDefaultCfOptions()));
		var outHandles = new ArrayList<ColumnFamilyHandle>();
		db = RocksDB.open(DatabaseRocksDb.getCommonDbOptions(), dbHome, columnFamilies, outHandles);
		for (int i = 0; i < columnFamilies.size(); ++i) {
			var cf = columnFamilies.get(i);
			var str = new String(cf.getName(), StandardCharsets.UTF_8);
			families.put(str, outHandles.get(i));
		}
		familyLastDoneTaskId = getOrAddFamily("FamilyLastDoneTaskId");
		familyTaskQueue = getOrAddFamily("FamilyTaskQueue");
		try (var qit = db.newIterator(familyTaskQueue, DatabaseRocksDb.getDefaultReadOptions())) {
			qit.seekToLast();
			if (qit.isValid()) {
				var last = ByteBuffer.Wrap(qit.key());
				lastTaskId = last.ReadLong();
			}
		}
		var done = db.get(familyLastDoneTaskId, DatabaseRocksDb.getDefaultReadOptions(), lastDoneTaskIdKey);
		if (done != null)
			lastDoneTaskId = ByteBuffer.Wrap(done).ReadLong();
		super.start();
	}

	@Override
	public synchronized void stop() throws Exception {
		super.stop();
		db.close();
		db = null;
	}

	public synchronized void add(int taskType, Serializable taskParam) {
		try {
			var key = ByteBuffer.Allocate(16);
			key.WriteLong(++lastTaskId);

			var task = new BQueueTask();
			task.setQueueName(getName());
			task.setPrevTaskId(lastTaskId - 1);
			task.setTaskId(lastTaskId);
			task.setTaskType(taskType);
			var value = ByteBuffer.Allocate(1024 + 16);
			task.encode(value);

			// 保存完整的rpc请求，重新发送的时候不用再次打包。
			db.put(familyTaskQueue, DatabaseRocksDb.getDefaultWriteOptions(), key.Bytes, 0, key.WriteIndex,
					value.Bytes, 0, value.WriteIndex);
			tryStartSendNextTask(task, null);
		} catch (RocksDBException ex) {
			throw new RuntimeException(ex);
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
				var key = ByteBuffer.Allocate(16);
				key.WriteLong(taskId);
				var value = db.get(familyTaskQueue, DatabaseRocksDb.getDefaultReadOptions(),
						key.Bytes, 0, key.WriteIndex);
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

	private synchronized long processRunTaskResult(Rpc<BQueueTask, BTaskId> rpc) throws Exception {
		if (pending != rpc)
			return Procedure.LogicError;

		pending = null;
		if (rpc.getResultCode() == 0L || rpc.getResultCode() == Procedure.ErrorRequestId) {
			lastDoneTaskId = rpc.Result.getTaskId();
			var value = ByteBuffer.Allocate(9);
			value.WriteLong(lastDoneTaskId);
			db.put(familyLastDoneTaskId, DatabaseRocksDb.getDefaultWriteOptions(),
					lastDoneTaskIdKey, 0, lastDoneTaskIdKey.length, value.Bytes, 0, value.WriteIndex);
			tryStartSendNextTask(null, rpc.getSender());
			return 0L;
		}

		return rpc.getResultCode();
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);
		synchronized (this) {
			tryStartSendNextTask(null, so);
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable ex) throws Exception {
		super.OnSocketClose(so, ex);
		synchronized (this) {
			if (this.socket == so)
				this.socket = null;
		}
	}
}
