package Zeze.Dbh2;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
import Zeze.Util.DaemonTimer;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Str;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskCompletionSourceX;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;
import org.jspecify.annotations.Nullable;
import Zeze.Builtin.Dbh2.Commit.BTransactionState.Data;

/**
 * Dbh2 事务提交点存储（RocksDB），负责事务 prepare/commit/undo 记录与失败重做。
 */
public class CommitRocks {
	private static final Logger logger = LogManager.getLogger(CommitRocks.class);

	private final Dbh2AgentManager manager;
	private final RocksDatabase database;
	private final RocksDatabase.Table commitPoint;
	private final RocksDatabase.Table commitIndex;
	// 2PC 提交点默认非 sync 写以保吞吐，需掉电强持久可通过 setWriteOptions 注入 sync 写
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();
	private final DaemonTimer redoDaemon = new DaemonTimer("CommitRocks.redoTimer", 60_000, this::redoTimer);
	// 在途 prepare 集合：登记先于 ePreparing 落盘，防止 redoTimer 抢先 undo 在途事务
	private final Set<Long> inFlightTids = ConcurrentHashMap.newKeySet();

	public CommitRocks(Dbh2AgentManager manager, int serverId) throws RocksDBException {
		this.manager = manager;
		database = new RocksDatabase(PropertiesHelper.getString("Dbh2CommitRocksHome", "CommitRocks") + serverId);
		commitPoint = database.getOrAddTable("CommitPoint");
		commitIndex = database.getOrAddTable("CommitIndex");
	}

	public Dbh2AgentManager getManager() {
		return manager;
	}

	// RPC 等待兜底超时，防止极端竞态下 future 永不完成导致无界悬挂
	private long rpcAwaitTimeoutMs() {
		return manager.getDbh2Config().getRpcTimeout() + Dbh2Agent.SendForWaitGraceMs;
	}

	public void start() {
		try {
			redoTimer();
		} catch (Exception ex) {
			logger.error("first try.", ex);
		}
		redoDaemon.start();
	}

	private void redoTimer() throws RocksDBException {
		try (var it = commitIndex.iterator()) {
			for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next())
				redo(it.key());
		}
	}

	private void redo(byte[] key) throws RocksDBException {
		var tid = ByteBuffer.ToLongBE(key, 0);
		// 先读在途标志再读提交点，避免并发提交删记录后误判为待撤销
		var inFlight = inFlightTids.contains(tid);
		var value = commitPoint.get(key);
		if (null == value) {
			logger.warn("redo but commit point not found. tid={}", tid);
			return;
		}
		var state = new BTransactionState.Data();
		state.decode(ByteBuffer.Wrap(value));
		var committing = state.getState() == Commit.eCommitting;
		if (!committing && (state.getState() != Commit.ePreparing || inFlight))
			return;

		try {
			var futures = new ArrayList<TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>>>();
			for (var e : state.getBuckets()) {
				var agent = manager.openBucket(e);
				futures.add(committing ? agent.commitBatch(tid) : agent.undoBatch(tid));
			}
			for (var e : futures) {
				var r = e.getNonNull(rpcAwaitTimeoutMs(), TimeUnit.MILLISECONDS);
				// appendLog 失败时服务端返回非零码，必须抛错保留重做索引
				if (r.getResultCode() != 0 && r.getResultCode() != Procedure.RaftApplied)
					throw new RuntimeException("redo error=" + IModule.getErrorCode(r.getResultCode()));
			}
			removeTransactionRecord(key);
		} catch (Throwable ex) {
			logger.error("", ex);
		}
	}

	public void close() {
		redoDaemon.stop();
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

	public @Nullable Data query(long tid) throws RocksDBException {
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
		var futures = new ArrayList<TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>>>();
		for (var e : state.getBuckets()) {
			futures.add(manager.openBucket(e).undoBatch(tid));
		}
		for (var e : futures) {
			var response = e.getNonNull(rpcAwaitTimeoutMs(), TimeUnit.MILLISECONDS);
			if (response.getResultCode() != 0 && response.getResultCode() != Procedure.RaftApplied)
				throw new RuntimeException("undo error=" + IModule.getErrorCode(response.getResultCode()));
		}
	}

	private void rollbackPrepare(long tid, byte[] tidBytes, BTransactionState.Data state, Throwable failure) {
		try {
			undo(tid, state);
			removeTransactionRecord(tidBytes); // 仅在全部参与方确认undo后删除重放记录。
		} catch (Throwable undoError) {
			failure.addSuppressed(undoError);
		} finally {
			// 无论undo结果如何，本次调用已结束；保留的ePreparing由周期redo接管。
			inFlightTids.remove(tid);
		}
	}

	private ArrayList<Map.Entry<String, TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>>> processPrepareFutures(
			long tid, byte[] tidBytes, BTransactionState.Data state, String queryHost, int queryPort,
			ArrayList<Map.Entry<String, TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>>> futures)
			throws RocksDBException {
		var redirects = new ArrayList<Map.Entry<String, BPrepareBatch.Data>>();
		// 瞬时拒绝码需重发新 RPC（同 tid 服务端去重）
		var retries = new ArrayList<Map.Entry<String, TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>>>();
		for (var e : futures) {
			var r = e.getValue().getNonNull(rpcAwaitTimeoutMs(), TimeUnit.MILLISECONDS);
			var agent = manager.openBucket(e.getKey());
			var code = r.getResultCode();
			if (code == agent.errorCode(AbstractDbh2.eDuplicateTid))
				continue; // 同 tid 前次投递已受理，视为成功
			if (code == agent.errorCode(AbstractDbh2.eBucketNotReady) || code == Procedure.RaftRetry) {
				logger.info("prepare retry {} code={} tid={} master={}", e.getKey(),
						IModule.getErrorCode(code), tid, r.Argument.getMaster());
				retries.add(Map.entry(e.getKey(), agent.prepareBatch(r.Argument)));
				continue;
			}
			if (code != 0 && code != Procedure.RaftApplied)
				throw new RuntimeException("prepare error=" + IModule.getErrorCode(code));
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
					redirects.add(Map.entry(eRefuse.getKey(), batch));
					if (!state.getBuckets().contains(eRefuse.getKey()))
						state.getBuckets().add(eRefuse.getKey());
				}
			}
		}
		if (!retries.isEmpty())
			try {
				Thread.sleep(200); // NotReady拒绝很快，节流重发轮次，避免空转打服务端。
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		var futuresRedirect = new ArrayList<Map.Entry<String, TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>>>();
		if (!redirects.isEmpty()) {
			// 先收集整轮目标并保存，再投递；任意投递失败时持久记录都能覆盖已发目标。
			saveCommitPoint(tidBytes, state, Commit.ePreparing);
			for (var redirect : redirects)
				futuresRedirect.add(Map.entry(redirect.getKey(), manager.openBucket(redirect.getKey()).prepareBatch(redirect.getValue())));
		}
		retries.addAll(futuresRedirect);
		return retries;
	}

	public long prepare(String queryHost, int queryPort, BTransactionState.Data state, BPrepareBatches.Data batches,
						byte @Nullable [] tidEncoded) {
		var tid = manager.nextTransactionId();
		var tidBytes = null != tidEncoded ? tidEncoded : new byte[8];
		ByteBuffer.longBeHandler.set(tidBytes, 0, tid);
		// 桶侧也按单调钟计龄。协调者墙钟回拨不得延长 prepare 决策窗口。
		var prepareStartNanos = System.nanoTime();
		var prepareMaxTime = manager.getDbh2Config().getPrepareMaxTime();
		inFlightTids.add(tid); // 先登记再写ePreparing：redoTimer"先读索引、后查在途"的次序才无竞态
		try {
			saveCommitPoint(tidBytes, state, Commit.ePreparing);
			var futures = new ArrayList<Map.Entry<String, TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>>>();
			for (var e : batches.getDatas().entrySet()) {
				var batch = e.getValue();
				batch.getBatch().setQueryIp(queryHost);
				batch.getBatch().setQueryPort(queryPort);
				batch.getBatch().setTid(tid);
				futures.add(Map.entry(e.getKey(), manager.openBucket(e.getKey()).prepareBatch(batch)));
			}

			// 处理 prepare 结果，含拒绝模式重定向循环
			while (!futures.isEmpty()) {
				// 重定向轮数无上限，每轮顶按 prepareMaxTime 熔断交超时分支统一 undo
				if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - prepareStartNanos) > prepareMaxTime)
					break;
				futures = processPrepareFutures(tid, tidBytes, state, queryHost, queryPort, futures);
			}

		} catch (Throwable ex) {
			rollbackPrepare(tid, tidBytes, state, ex);
			throw new RuntimeException(ex);
		}

		if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - prepareStartNanos) > prepareMaxTime) {
			var ex = new RuntimeException(Str.format("max prepare time exceed. time={}", prepareMaxTime));
			rollbackPrepare(tid, tidBytes, state, ex);
			throw ex;
		}
		return tid;
	}

	/** decide+deliver的衔接载荷：应答点（decide返回）与投递点（异步deliver）间的传递。 */
	public static final class CommitDecision {
		public final long tid;
		public final byte[] tidBytes;
		public final BTransactionState.Data state;

		CommitDecision(long tid, byte[] tidBytes, BTransactionState.Data state) {
			this.tid = tid;
			this.tidBytes = tidBytes;
			this.state = state;
		}
	}

	// 决定阶段：prepare 并落盘 eCommitting，成功后事务终局为提交，后续投递由 redoTimer 兜底
	public CommitDecision commitDecide(String queryHost, int queryPort, BPrepareBatches.Data batches) {
		var state = buildTransactionState(batches);
		var tidBytes = new byte[8];
		var tid = prepare(queryHost, queryPort, state, batches, tidBytes);

		try {
			// 保存 commit-point，如果失败，则 undo。
			saveCommitPoint(tidBytes, state, Commit.eCommitting);
		} catch (Throwable ex) {
			rollbackPrepare(tid, tidBytes, state, ex);
			throw new RuntimeException(ex);
		}
		return new CommitDecision(tid, tidBytes, state);
	}

	// 投递阶段：向各参与桶发送 commitBatch，全部成功后删除提交点；失败仅记日志交 redoTimer 重试
	public void commitDeliver(CommitDecision decision) {
		try {
			var futures = new ArrayList<TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>>>();
			for (var e : decision.state.getBuckets()) {
				futures.add(manager.openBucket(e).commitBatch(decision.tid));
			}
			for (var e : futures) {
				var r = e.getNonNull(rpcAwaitTimeoutMs(), TimeUnit.MILLISECONDS);
				// 非零回包表示未 apply，保留提交点交 redoTimer 重试
				if (r.getResultCode() != 0 && r.getResultCode() != Procedure.RaftApplied)
					throw new RuntimeException("commit error=" + IModule.getErrorCode(r.getResultCode()));
			}
			removeTransactionRecord(decision.tidBytes);
		} catch (Throwable ex) {
			logger.error("", ex);
		}
	}

	// 本地提交模式：decide+deliver同步串行。
	public void commit(String queryHost, int queryPort, BPrepareBatches.Data batches) {
		commitDeliver(commitDecide(queryHost, queryPort, batches));
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
		bbIndex.WriteUInt(state);
		try (var batch = database.borrowBatch()) {
			commitPoint.put(batch, tidBytes, tidBytes.length, bb.Bytes, bb.WriteIndex);
			commitIndex.put(batch, tidBytes, tidBytes.length, bbIndex.Bytes, bbIndex.WriteIndex);
			batch.commit(writeOptions);
		}
	}

	// 事务终结时同批删除 commitIndex 与 commitPoint，防止 commitPoint 无界增长
	private void removeTransactionRecord(byte[] tidBytes) {
		try {
			try (var batch = database.borrowBatch()) {
				commitIndex.delete(batch, tidBytes);
				commitPoint.delete(batch, tidBytes);
				batch.commit(writeOptions);
			}
		} catch (RocksDBException e) {
			// 仅记日志，未删除的索引由重启与 redoTimer 重试
			logger.error("", e);
		} finally {
			// 必须在删除记录之后移除在途登记，避免 redo 并发误判
			inFlightTids.remove(ByteBuffer.ToLongBE(tidBytes, 0));
		}
	}
}
