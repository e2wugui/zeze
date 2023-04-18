package Zeze.Dbh2;

import java.util.ArrayList;
import Zeze.Builtin.Dbh2.Commit.BTransactionState;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

public class CommitRocks {
	private static final Logger logger = LogManager.getLogger(CommitRocks.class);
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

	public RocksDatabase.Table getCommitPoint() {
		return commitPoint;
	}

	public int query(Binary tid) throws RocksDBException {
		logger.warn("query tid=" + tid);
		var value = commitPoint.get(tid.bytesUnsafe(), tid.getOffset(), tid.size());
		if (null == value)
			return Commit.eCommitNotExist;
		var state = new BTransactionState.Data();
		state.decode(ByteBuffer.Wrap(value));
		logger.warn("query tid=" + tid + " state=" + state.getState());
		return state.getState();
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
		commitPoint.put(tid.bytesUnsafe(), tid.getOffset(), tid.size(), bb.Bytes, bb.ReadIndex, bb.size());

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
