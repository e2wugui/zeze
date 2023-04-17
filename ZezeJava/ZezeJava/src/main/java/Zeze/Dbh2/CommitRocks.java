package Zeze.Dbh2;

import java.util.ArrayList;
import java.util.HashMap;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskCompletionSource;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

public class CommitRocks {
	private final Dbh2AgentManager manager;
	private final RocksDatabase database;
	private final RocksDatabase.Table commitPoint;
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();

	public CommitRocks(Dbh2AgentManager manager) throws RocksDBException {
		this.manager = manager;
		database = new RocksDatabase("CommitRocks");
		commitPoint = database.openTable("CommitPoint");
	}

	public Dbh2AgentManager getManager() {
		return manager;
	}

	public void close() {
		database.close();
	}

	public void setWriteOptions(WriteOptions writeOptions) {
		this.writeOptions = writeOptions;
	}

	public long query(Binary sortedNames, long tid) throws RocksDBException {
		var key = ByteBuffer.Allocate(sortedNames.size() + 11);
		key.WriteBinary(sortedNames);
		key.WriteLong(tid);
		var value = commitPoint.get(key.Bytes, key.ReadIndex, key.size());
		if (null == value)
			return Commit.eCommitNotExist;
		return Commit.eCommitPoint;
	}

	public void saveCommitPoint(HashMap<Dbh2Agent, Database.BatchWithTid> batches) throws RocksDBException {
		var batch = database.newBatch();
		for (var e : batches.entrySet()) {
			var bucketName = e.getKey().getRaftConfig().getSortedNamesUtf8();
			var key = ByteBuffer.Allocate(bucketName.length + 11); // 2 bytes.size + 9 long
			// key = name(-ip:port-ip:port-ip:port)+tid
			// 这里的编码和下面commitTransaction第二步的编码一样。
			key.WriteBytes(bucketName);
			key.WriteLong(e.getValue().getTid());
			commitPoint.put(batch, new Binary(key), Binary.Empty);
		}
		batch.commit(writeOptions);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// 独立CommitServer
	public void commitTransaction(HashMap<String, Long> batches) throws RocksDBException {
		// 第一步：先打开Dbh2Agent.
		var agents = new HashMap<Dbh2Agent, Long>();
		for (var e : batches.entrySet()) {
			var agent = manager.startWithSortedNames(e.getKey());
			var tid = e.getValue();
			agents.put(agent, tid);
		}

		// 第二步：saveCommitPoint
		var batch = database.newBatch();
		for (var e : agents.entrySet()) {
			var bucketNameUtf8 = e.getKey().getRaftConfig().getSortedNamesUtf8();
			var tid = e.getValue();
			var key = ByteBuffer.Allocate(bucketNameUtf8.length + 11); // 2 bytes.size + 9 long
			// 这里的编码和上面saveCommitPoint一样。
			key.WriteBytes(bucketNameUtf8);
			key.WriteLong(tid);
			commitPoint.put(batch, new Binary(key), Binary.Empty);
		}
		batch.commit(writeOptions);

		// 第三步：执行提交，批量发送，批量等待。
		var futures = new ArrayList<TaskCompletionSource<?>>();
		for (var e : agents.entrySet())
			futures.add(e.getKey().commitBatch(e.getValue()));
		for (var future : futures)
			future.await();
	}

	public static Binary encodeTransaction(HashMap<Dbh2Agent, Database.BatchWithTid> batches) {
		var bb = ByteBuffer.Allocate();
		bb.WriteInt(batches.size());
		for (var e : batches.entrySet()) {
			bb.WriteString(e.getKey().getRaftConfig().getSortedNames());
			bb.WriteLong(e.getValue().getTid());
		}
		return new Binary(bb);
	}

	public static HashMap<String, Long> decodeTransaction(Binary data) {
		var result = new HashMap<String, Long>();
		var bb = ByteBuffer.Wrap(data);
		for (int count = bb.ReadInt(); count > 0; --count) {
			var raft = bb.ReadString();
			var tid = bb.ReadLong();
			result.put(raft, tid);
		}
		return result;
	}
}
