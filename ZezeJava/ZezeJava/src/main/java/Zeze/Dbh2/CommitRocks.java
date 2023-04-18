package Zeze.Dbh2;

import java.util.ArrayList;
import java.util.HashMap;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Commit.BTransactionState;
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

	public WriteOptions getWriteOptions() {
		return writeOptions;
	}

	public RocksDatabase.Table getCommitPoint() {
		return commitPoint;
	}

	public BTransactionState.Data query(Binary tid) throws RocksDBException {
		var value = commitPoint.get(tid.bytesUnsafe(), tid.getOffset(), tid.size());
		if (null == value)
			return null;
		var state = new BTransactionState.Data();
		state.decode(ByteBuffer.Wrap(value));
		return state;
	}

	////////////////////////////////////////////////////////////////////////////////////
	// commit local
	public void committing(Binary tid, HashMap<Dbh2Agent, BPrepareBatch.Data> batches) throws RocksDBException {
		var state = new BTransactionState.Data();
		state.setState(Commit.eCommitting);
		for (var e : batches.entrySet()) {
			state.getBuckets().add(e.getKey().getRaftConfigString());
		}
		var bb = ByteBuffer.Allocate();
		state.encode(bb);
		commitPoint.put(writeOptions, tid.bytesUnsafe(), tid.getOffset(), tid.size(), bb.Bytes, bb.ReadIndex, bb.size());
	}

	public void commitDone(Binary tid, HashMap<Dbh2Agent, BPrepareBatch.Data> batches) {
		// 这里没有使用get-modify-put。
		// 从效率上来讲，重新构造一次还快。
		var state = new BTransactionState.Data();
		state.setState(Commit.eCommitDone);
		for (var e : batches.entrySet()) {
			state.getBuckets().add(e.getKey().getRaftConfigString());
		}
		var bb = ByteBuffer.Allocate();
		state.encode(bb);
		try {
			commitPoint.put(writeOptions, tid.bytesUnsafe(), tid.getOffset(), tid.size(), bb.Bytes, bb.ReadIndex, bb.size());
		} catch (RocksDBException e) {
			throw new RuntimeException(e);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// 独立CommitServer
	public void commitTransaction(Binary tid, ArrayList<String> rafts) throws RocksDBException {
		// 第一步：saveCommitPoint
		var state = new BTransactionState.Data();
		state.setState(Commit.eCommitting);
		for (var raft : rafts) {
			state.getBuckets().add(raft);
		}
		var bb = ByteBuffer.Allocate();
		state.encode(bb);
		commitPoint.put(writeOptions, tid.bytesUnsafe(), tid.getOffset(), tid.size(), bb.Bytes, bb.ReadIndex, bb.size());

		// 第二步：先打开Dbh2Agent.
		var agents = new ArrayList<Dbh2Agent>();
		for (var raft : rafts) {
			agents.add(manager.start(raft));
		}

		// 第三步：执行提交，批量发送，批量等待。
		var futures = new ArrayList<TaskCompletionSource<?>>();
		for (var e : agents)
			futures.add(e.commitBatch(tid));
		for (var future : futures)
			future.await();
	}
}