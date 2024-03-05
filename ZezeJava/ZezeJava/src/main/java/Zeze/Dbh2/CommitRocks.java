package Zeze.Dbh2;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import Zeze.Builtin.Dbh2.BBatchTid;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.BRefused;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Builtin.Dbh2.Commit.BTransactionState;
import Zeze.IModule;
import Zeze.Raft.RaftRpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Func2;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskCompletionSourceX;
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

	public CommitRocks(Dbh2AgentManager manager, int serverId) throws RocksDBException {
		this.manager = manager;
		database = new RocksDatabase("CommitRocks" + serverId);
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

	private void redo(byte[] key, Func2<Dbh2Agent, Long, TaskCompletionSource<
			RaftRpc<BBatchTid.Data, EmptyBean.Data>>> func) throws RocksDBException {

		var value = Objects.requireNonNull(commitPoint.get(key));
		var state = new BTransactionState.Data();
		state.decode(ByteBuffer.Wrap(value));

		var tid = ByteBuffer.ToLongBE(key, 0);
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
		if (null != redoTimer)
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

	public BTransactionState.Data query(long tid) throws RocksDBException {
		var tidBytes = new byte[8];
		ByteBuffer.longBeHandler.set(tidBytes, 0, tid);
		var value = commitPoint.get(tidBytes);
		if (null == value) {
			logger.warn("query but not found {}", tid);
			return null;
		}
		var state = new BTransactionState.Data();
		state.decode(ByteBuffer.Wrap(value));
		logger.info("query {}:{}", tid, state);
		return state;
	}

	private void undo(long tid, BTransactionState.Data state) {
		// undo
		var futures = new ArrayList<TaskCompletionSource<?>>();
		for (var e : state.getBuckets()) {
			futures.add(manager.openBucket(e).undoBatch(tid));
		}
		for (var e : futures)
			e.await();
	}

	private ArrayList<TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>> processPrepareFutures(
			long tid, String queryHost, int queryPort,
			ArrayList<TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>> futures)
			throws ExecutionException, InterruptedException {

		var futuresRedirect = new ArrayList<TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>>();
		for (var e : futures) {
			var r = e.get();
			if (r.getResultCode() != 0 && r.getResultCode() != Procedure.RaftApplied)
				throw new RuntimeException("prepare error=" + IModule.getErrorCode(r.getResultCode()));
			// 【dbh2 拒绝模式结果处理】
			var refused = r.Result.getRefused();
			if (!refused.isEmpty()) {
				manager.startRefreshMasterTable(r.Argument.getMaster(), r.Argument.getDatabase(), r.Argument.getTable());
				for (var eRefuse : refused.entrySet()) {
					var batch = new BPrepareBatch.Data();
					batch.setMaster(r.Argument.getMaster());
					batch.setDatabase(r.Argument.getDatabase());
					batch.setTable(r.Argument.getTable());

					batch.getBatch().setQueryIp(queryHost);
					batch.getBatch().setQueryPort(queryPort);
					batch.getBatch().setTid(tid);
					batch.getBatch().setPuts(eRefuse.getValue().getPuts());
					batch.getBatch().setDeletes(eRefuse.getValue().getDeletes());
					futuresRedirect.add(manager.openBucket(eRefuse.getKey()).prepareBatch(batch).setContext(eRefuse.getKey()));
				}
			}
		}
		return futuresRedirect;
	}

	public long prepare(String queryHost, int queryPort, BTransactionState.Data state, BPrepareBatches.Data batches,
						byte[] tidEncoded) {
		var tid = manager.nextTransactionId();
		var tidBytes = null != tidEncoded ? tidEncoded : new byte[8];
		ByteBuffer.longBeHandler.set(tidBytes, 0, tid);
		var prepareTime = System.currentTimeMillis();
		try {
			// prepare
			saveCommitPoint(tidBytes, state, Commit.ePreparing);
			var futures = new ArrayList<TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>>();
			for (var e : batches.getDatas().entrySet()) {
				var batch = e.getValue();
				batch.getBatch().setQueryIp(queryHost);
				batch.getBatch().setQueryPort(queryPort);
				batch.getBatch().setTid(tid);
				futures.add(manager.openBucket(e.getKey()).prepareBatch(batch));
			}

			// 处理prepare结果，碰到【拒绝模式重定向】的请求，需要循环处理。
			while (!futures.isEmpty()) {
				futures = processPrepareFutures(tid, queryHost, queryPort, futures);
				if (!futures.isEmpty()) {
					for (var future : futures) {
						state.getBuckets().add((String)future.getContext());
					}
					saveCommitPoint(tidBytes, state, Commit.ePreparing);
				}
			}

		} catch (Throwable ex) {
			undo(tid, state);
			removeCommitIndex(tidBytes);
			throw new RuntimeException(ex);
		}

		if (System.currentTimeMillis() - prepareTime > manager.getDbh2Config().getPrepareMaxTime()) {
			undo(tid, state);
			removeCommitIndex(tidBytes);
			throw new RuntimeException("max prepare time exceed.");
		}
		return tid;
	}

	public void commit(String queryHost, int queryPort, BPrepareBatches.Data batches) {
		var state = buildTransactionState(batches);
		var tidBytes = new byte[8];
		var tid = prepare(queryHost, queryPort, state, batches, tidBytes);

		try {
			// 保存 commit-point，如果失败，则 undo。
			saveCommitPoint(tidBytes, state, Commit.eCommitting);
		} catch (Throwable ex) {
			undo(tid, state);
			removeCommitIndex(tidBytes);
			throw new RuntimeException(ex);
		}

		// commit
		try {
			var futures = new ArrayList<TaskCompletionSource<?>>();
			for (var e : state.getBuckets()) {
				futures.add(manager.openBucket(e).commitBatch(tid));
			}
			for (var e : futures)
				e.await();
			removeCommitIndex(tidBytes);
		} catch (Throwable ex) {
			// timer will redo
			logger.error("", ex);
		}
	}

	public static BTransactionState.Data buildTransactionState(BPrepareBatches.Data batches) {
		var bState = new BTransactionState.Data();
		for (var e : batches.getDatas().entrySet()) {
			bState.getBuckets().add(e.getKey());
		}
		return bState;
	}

	private void saveCommitPoint(byte[] tidBytes, BTransactionState.Data bState, int state) throws RocksDBException {
		bState.setState(state);
		var bb = ByteBuffer.Allocate();
		bState.encode(bb);
		var bbIndex = ByteBuffer.Allocate(5);
		bbIndex.WriteInt(state);
		try (var batch = database.borrowBatch()) {
			// putIfAbsent ？？？ 报错！
			commitPoint.put(batch, tidBytes, tidBytes.length, bb.Bytes, bb.WriteIndex);
			commitIndex.put(batch, tidBytes, tidBytes.length, bbIndex.Bytes, bbIndex.WriteIndex);
			batch.commit(writeOptions);
		}
	}

	private void removeCommitIndex(byte[] tidBytes) {
		try {
			commitIndex.delete(tidBytes);
		} catch (RocksDBException e) {
			// 这个错误仅仅记录日志，所有没有删除的index，以后重启和Timer会尝试重做。
			logger.error("", e);
		}
	}
}
