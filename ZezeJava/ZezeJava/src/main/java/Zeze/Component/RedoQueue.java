package Zeze.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.RedoQueue.BQueueTask;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Builtin.RedoQueue.RunTask;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
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
public class RedoQueue extends Zeze.Services.HandshakeClient {
	private RocksDB Db;
	private final ConcurrentHashMap<String, ColumnFamilyHandle> Families = new ConcurrentHashMap<>();
	private ColumnFamilyHandle FamilyLastDoneTaskId;
	private ColumnFamilyHandle FamilyTaskQueue;
	private long LastTaskId;
	private long LastDoneTaskId;
	private final byte[] LastDoneTaskIdKey = "LastDoneTaskId".getBytes(StandardCharsets.UTF_8);
	private RunTask Pending;
	private AsyncSocket Socket;

	static {
		RocksDB.loadLibrary();
	}

	public RedoQueue(String name, Zeze.Config config) throws Throwable {
		super(name, config);
	}

	ColumnFamilyHandle getOrAddFamily(String name) {
		return Families.computeIfAbsent(name, (key) -> {
			try {
				return Db.createColumnFamily(new ColumnFamilyDescriptor(
						key.getBytes(StandardCharsets.UTF_8), DatabaseRocksDb.getDefaultCfOptions()));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public synchronized void Start() throws Throwable {
		if (null != Db)
			return;

		var dbHome = super.getName();
		var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
		for (var cf : RocksDB.listColumnFamilies(DatabaseRocksDb.getCommonOptions(), dbHome)) {
			columnFamilies.add(new ColumnFamilyDescriptor(cf, DatabaseRocksDb.getDefaultCfOptions()));
		}
		if (columnFamilies.isEmpty()) {
			columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(), DatabaseRocksDb.getDefaultCfOptions()));
		}
		var outHandles = new ArrayList<ColumnFamilyHandle>();
		Db = RocksDB.open(DatabaseRocksDb.getCommonDbOptions(), dbHome, columnFamilies, outHandles);
		for (int i = 0; i < columnFamilies.size(); ++i) {
			var cf = columnFamilies.get(i);
			var str = new String(cf.getName(), StandardCharsets.UTF_8);
			Families.put(str, outHandles.get(i));
		}
		FamilyLastDoneTaskId = getOrAddFamily("FamilyLastDoneTaskId");
		FamilyTaskQueue = getOrAddFamily("FamilyTaskQueue");
		try (var qit = Db.newIterator(FamilyTaskQueue, DatabaseRocksDb.getDefaultReadOptions())) {
			qit.seekToLast();
			if (qit.isValid()) {
				var last = ByteBuffer.Wrap(qit.key());
				LastTaskId = last.ReadLong();
			}
		}
		var done = Db.get(FamilyLastDoneTaskId, DatabaseRocksDb.getDefaultReadOptions(), LastDoneTaskIdKey);
		if (done != null) {
			LastDoneTaskId = ByteBuffer.Wrap(done).ReadLong();
		}
		super.Start();
	}

	@Override
	public synchronized void Stop() throws Throwable {
		super.Stop();
		Db.close();
		Db = null;
	}

	public synchronized void add(int taskType, Zeze.Serialize.Serializable taskParam) {
		try {
			var key = ByteBuffer.Allocate(16);
			++LastTaskId;
			key.WriteLong(LastTaskId);

			var task = new BQueueTask();
			task.setQueueName(getName());
			task.setPrevTaskId(LastTaskId - 1);
			task.setTaskId(LastTaskId);
			task.setTaskType(taskType);
			var value = ByteBuffer.Allocate(1024 + 16);
			task.encode(value);

			// 保存完整的rpc请求，重新发送的时候不用再次打包。
			Db.put(FamilyTaskQueue, DatabaseRocksDb.getDefaultWriteOptions(), key.Bytes, 0, key.WriteIndex,
					value.Bytes, 0, value.WriteIndex);
			tryStartSendNextTask(task, null);
		} catch (RocksDBException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void tryStartSendNextTask(BQueueTask add, AsyncSocket socket) throws RocksDBException {
		if (null != Pending)
			return;

		if (LastDoneTaskId < LastTaskId) {
			var taskId = LastDoneTaskId + 1;
			var rpc = new RunTask();
			if (add != null && taskId == add.getTaskId()) {
				rpc.Argument = add; // 最近加入的就是要发送的。优化！
			} else {
				// 最近加入的不是要发送的，从Db中读取。
				var key = ByteBuffer.Allocate(16);
				key.WriteLong(taskId);
				var value = Db.get(FamilyTaskQueue, DatabaseRocksDb.getDefaultReadOptions(),
						key.Bytes, 0, key.WriteIndex);
				if (null == value)
					return; // error
				rpc.Argument.decode(ByteBuffer.Wrap(value));
			}
			if (null == Socket) {
				Socket = socket;
				if (null == Socket) {
					Socket = GetSocket();
					if (null == Socket)
						return;
				}
			}
			if (rpc.Send(Socket, this::ProcessRunTaskResult))
				Pending = rpc;
		}
	}

	private synchronized long ProcessRunTaskResult(Rpc<BQueueTask, BTaskId> rpc) throws Throwable {
		if (Pending != rpc)
			return Procedure.LogicError;

		Pending = null;
		if (rpc.getResultCode() == 0L || rpc.getResultCode() == Procedure.ErrorRequestId) {
			LastDoneTaskId = rpc.Result.getTaskId();
			var value = ByteBuffer.Allocate(9);
			value.WriteLong(LastDoneTaskId);
			Db.put(FamilyLastDoneTaskId, DatabaseRocksDb.getDefaultWriteOptions(),
					LastDoneTaskIdKey, 0, LastDoneTaskIdKey.length, value.Bytes, 0, value.WriteIndex);
			tryStartSendNextTask(null, rpc.getSender());
			return 0L;
		}

		return rpc.getResultCode();
	}

	@Override
	public void OnHandshakeDone(AsyncSocket sender) throws Throwable {
		super.OnHandshakeDone(sender);
		synchronized (this) {
			tryStartSendNextTask(null, sender);
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket socket, Throwable ex) throws Throwable {
		super.OnSocketClose(socket, ex);
		synchronized (this) {
			if (Socket == socket) {
				Socket = null;
			}
		}
	}
}
