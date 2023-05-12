package Zeze.Dbh2;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Future;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBatchTid;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Builtin.Dbh2.Commit.BTransactionState;
import Zeze.Net.Binary;
import Zeze.Raft.RaftRpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.Func2;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
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
	private final RocksDatabase.Table commitIndex;
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();
	private Future<?> redoTimer;

	public CommitRocks(Dbh2AgentManager manager) throws RocksDBException {
		this.manager = manager;
		database = new RocksDatabase("CommitRocks");
		commitPoint = database.getOrAddTable("CommitPoint");
		commitIndex = database.getOrAddTable("CommitIndex");
	}

	public Dbh2AgentManager getManager() {
		return manager;
	}

	public void start() {
		try {
			redoTimer();
		} catch (Exception ex) {
			logger.error("first try.", ex);
		}
		// 1 minute?
		redoTimer = Task.scheduleUnsafe(60000, 60000, this::redoTimer);
	}

	private void redoTimer() throws RocksDBException {
		try (var it = commitIndex.iterator()) {
			for (it.seekToFirst(); it.isValid(); it.next()) {
				var value = it.value();
				var state = ByteBuffer.Wrap(value).ReadInt();
				switch (state) {
				case Commit.eCommitting:
					redo(it.key(), Dbh2Agent::commitBatch);
					break;
				case Commit.ePreparing:
					redo(it.key(), Dbh2Agent::undoBatch);
					break;
				}
			}
		}
	}

	private void redo(byte[] key, Func2<Dbh2Agent, Binary, TaskCompletionSource<
			RaftRpc<BBatchTid.Data, EmptyBean.Data>>> func) throws RocksDBException {

		var value = Objects.requireNonNull(commitPoint.get(key));
		var state = new BTransactionState.Data();
		state.decode(ByteBuffer.Wrap(value));

		var tid = new Binary(key);
		try {
			var futures = new ArrayList<TaskCompletionSource<?>>();
			for (var e : state.getBuckets()) {
				futures.add(func.call(manager.openBucket(e), tid));
			}
			for (var e : futures)
				e.await();
			removeCommitIndex(key);
		} catch (Throwable ex) {
			// timer will redo
			logger.error("", ex);
		}
	}

	public void close() {
		redoTimer.cancel(false);
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

	private void undo(BPrepareBatches.Data batches) {
		// undo
		var futures = new ArrayList<TaskCompletionSource<?>>();
		for (var e : batches.getDatas().entrySet()) {
			var tid2 = e.getValue().getBatch().getTid();
			if (tid2.size() == 0)
				continue; // not prepared
			futures.add(manager.openBucket(e.getKey()).undoBatch(tid2));
		}
		for (var e : futures)
			e.await();
	}

	public byte[] prepare(String queryHost, int queryPort, BPrepareBatches.Data batches) {
		var tid = Dbh2AgentManager.nextTransactionId();
		var prepareTime = System.currentTimeMillis();
		try {
			// prepare
			saveCommitPoint(tid, batches, Commit.ePreparing);
			var futures = new ArrayList<TaskCompletionSource<RaftRpc<BPrepareBatch.Data, BBatch.Data>>>();
			var tidBinary = new Binary(tid);
			for (var e : batches.getDatas().entrySet()) {
				var batch = e.getValue();
				batch.getBatch().setQueryIp(queryHost);
				batch.getBatch().setQueryPort(queryPort);
				batch.getBatch().setTid(tidBinary);
				futures.add(manager.openBucket(e.getKey()).prepareBatch(batch));
			}
			for (var e : futures) {
				var refused = e.get();
				if (!refused.Result.getDeletes().isEmpty() || !refused.Result.getPuts().isEmpty()) {
					logger.info("todo process refused. "); // todo process refused
				}
			}
		} catch (Throwable ex) {
			undo(batches);
			removeCommitIndex(tid);
			throw new RuntimeException(ex);
		}

		if (System.currentTimeMillis() - prepareTime > manager.getDbh2Config().getPrepareMaxTime()) {
			undo(batches);
			removeCommitIndex(tid);
			throw new RuntimeException("max prepare time exceed.");
		}
		return tid;
	}

	public void commit(String queryHost, int queryPort, BPrepareBatches.Data batches) {
		var tid = prepare(queryHost, queryPort, batches);

		try {
			// 保存 commit-point，如果失败，则 undo。
			saveCommitPoint(tid, batches, Commit.eCommitting);
		} catch (Throwable ex) {
			undo(batches);
			removeCommitIndex(tid);
			throw new RuntimeException(ex);
		}

		// commit
		try {
			var futures = new ArrayList<TaskCompletionSource<?>>();
			for (var e : batches.getDatas().entrySet()) {
				futures.add(manager.openBucket(e.getKey()).commitBatch(e.getValue().getBatch().getTid()));
			}
			for (var e : futures)
				e.await();
			removeCommitIndex(tid);
		} catch (Throwable ex) {
			// timer will redo
			logger.error("", ex);
		}
	}

	private void saveCommitPoint(byte[] tid, BPrepareBatches.Data batches, int state) throws RocksDBException {
		var bState = new BTransactionState.Data();
		bState.setState(state);
		for (var e : batches.getDatas().entrySet()) {
			bState.getBuckets().add(e.getKey());
		}
		var bb = ByteBuffer.Allocate();
		bState.encode(bb);
		var bbIndex = ByteBuffer.Allocate(5);
		bbIndex.WriteInt(state);
		try (var batch = database.borrowBatch()) {
			commitPoint.put(batch, tid, tid.length, bb.Bytes, bb.WriteIndex);
			commitIndex.put(batch, tid, tid.length, bbIndex.Bytes, bbIndex.WriteIndex);
			batch.commit(writeOptions);
		}
	}

	private void removeCommitIndex(byte[] tid) {
		try {
			commitIndex.delete(tid);
		} catch (RocksDBException e) {
			// 这个错误仅仅记录日志，所有没有删除的index，以后重启和Timer会尝试重做。
			logger.error("", e);
		}
	}
}
