package Zeze.Dbh2;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import Zeze.Util.Func2;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Str;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskCompletionSourceX;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

/**
 * Dbh2 事务提交点存储（RocksDB），负责事务 prepare/commit/undo 记录与失败重做。
 */
public class CommitRocks {
	private static final Logger logger = LogManager.getLogger(CommitRocks.class);

	private final Dbh2AgentManager manager;
	private final RocksDatabase database;
	private final RocksDatabase.Table commitPoint;
	private final RocksDatabase.Table commitIndex;
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();
	// 2PC决定档（eCommitting）专用sync写（复用RocksDatabase共享单例，勿每调用new）：
	// 决定必须先于效果（commitBatch分发）fsync落盘——非sync下协调者机器断电重启后
	// commit-point随WAL丢失，存活桶侧onTimer超时query得eCommitNotExist会undo掉
	// 已向客户端确认成功的事务。ePreparing是可撤销中间态，保持非sync。
	private final WriteOptions syncWriteOptions = RocksDatabase.getSyncWriteOptions();
	// 周期守护：redoTimer(RocksDB迭代+逐桶RPC get阻塞等待)进worker池不占调度线程；
	// close有界等待在飞一轮
	private final DaemonTimer redoDaemon = new DaemonTimer("CommitRocks.redoTimer", 60_000, this::redoTimer);
	// 本进程在途事务tid：登记必须先于ePreparing落盘，移除只能随removeTransactionRecord
	//（记录删除之后）。redoTimer对ePreparing先查此集：命中=在途，同进程慢prepare不得
	// 误判为崩溃残留undo；未命中=本进程重启后的真残留（集合随进程消失），照常清理。
	private final Set<Long> inFlightTids = ConcurrentHashMap.newKeySet();

	public CommitRocks(Dbh2AgentManager manager, int serverId) throws RocksDBException {
		this.manager = manager;
		// home前缀可配（默认cwd下CommitRocks{serverId}），测试重定向到临时目录。
		database = new RocksDatabase(PropertiesHelper.getString("Dbh2CommitRocksHome", "CommitRocks") + serverId);
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
		redoDaemon.start();
	}

	private void redoTimer() throws RocksDBException {
		try (var it = commitIndex.iterator()) {
			for (it.seekToFirst(); it.isValid(); it.next()) {
				var value = it.value();
				var state = ByteBuffer.Wrap(value).ReadUInt();
				switch (state) {
				case Commit.eCommitting:
					redo(it.key(), Dbh2Agent::commitBatch);
					break;
				case Commit.ePreparing:
					// 迭代值可能陈旧（事务此刻已推进到eCommitting甚至已完结）：在途集合的移除
					// 只发生在记录删除之后，命中即可安全跳过；未命中时redo内重读commitPoint兜底。
					if (inFlightTids.contains(ByteBuffer.ToLongBE(it.key(), 0)))
						break;
					redo(it.key(), Dbh2Agent::undoBatch);
					break;
				}
			}
		}
	}

	private void redo(byte[] key, Func2<Dbh2Agent, Long, TaskCompletionSource<
			RaftRpc<BBatchTid.Data, EmptyBean.Data>>> func) throws RocksDBException {

		var tid = ByteBuffer.ToLongBE(key, 0);
		var value = commitPoint.get(key);
		if (null == value) {
			// 索引迭代到redo执行的间隙记录可能已被并发删除（事务正常完结），非崩溃残留。
			logger.warn("redo but commit point not found. tid={}", tid);
			return;
		}
		var state = new BTransactionState.Data();
		state.decode(ByteBuffer.Wrap(value));

		try {
			var futures = new ArrayList<TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>>>();
			for (var e : state.getBuckets()) {
				futures.add(func.call(manager.openBucket(e), tid));
			}
			for (var e : futures) {
				var r = e.get();
				// appendLog失败（丢leader/多数派未达成）时服务端以非零码正常回包（Dbh2.ProcessCommitBatchRequest），
				// 不检查就删重做索引会使该桶的事务永久滞留在eCommitting，客户端却已拿到成功。对齐prepare路径的检查。
				if (r.getResultCode() != 0 && r.getResultCode() != Procedure.RaftApplied)
					throw new RuntimeException("redo error=" + IModule.getErrorCode(r.getResultCode()));
			}
			removeTransactionRecord(key);
		} catch (Throwable ex) {
			logger.error("", ex);
		}
	}

	public void close() {
		redoDaemon.stop(); // 有界等待在飞一轮；超预算逃逸轮撞已关database由body的catch容错（记日志）
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
		var futures = new ArrayList<TaskCompletionSource<?>>();
		for (var e : state.getBuckets()) {
			futures.add(manager.openBucket(e).undoBatch(tid));
		}
		for (var e : futures)
			e.await();
	}

	private ArrayList<TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>> processPrepareFutures(
			long tid, String queryHost, int queryPort,
			ArrayList<TaskCompletionSourceX<RaftRpc<BPrepareBatch.Data, BRefused.Data>>> futures) {
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
		inFlightTids.add(tid); // 先登记再写ePreparing：redoTimer"先读索引、后查在途"的次序才无竞态
		try {
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
			try {
				undo(tid, state);
			} catch (Throwable undoEx) {
				ex.addSuppressed(undoEx); // undo失败不能掩盖原始异常
			}
			removeTransactionRecord(tidBytes);
			throw new RuntimeException(ex);
		}

		if (System.currentTimeMillis() - prepareTime > manager.getDbh2Config().getPrepareMaxTime()) {
			undo(tid, state);
			removeTransactionRecord(tidBytes);
			throw new RuntimeException(Str.format("max prepare time exceed. time={}", manager.getDbh2Config().getPrepareMaxTime()));
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
			try {
				undo(tid, state);
			} catch (Throwable undoEx) {
				ex.addSuppressed(undoEx); // undo失败不能掩盖原始异常
			}
			removeTransactionRecord(tidBytes);
			throw new RuntimeException(ex);
		}

		try {
			var futures = new ArrayList<TaskCompletionSource<RaftRpc<BBatchTid.Data, EmptyBean.Data>>>();
			for (var e : state.getBuckets()) {
				futures.add(manager.openBucket(e).commitBatch(tid));
			}
			for (var e : futures) {
				var r = e.get();
				// 同redo：非零回包（raft appendLog失败）不抛异常但事务未apply，必须留给redoTimer重试
				if (r.getResultCode() != 0 && r.getResultCode() != Procedure.RaftApplied)
					throw new RuntimeException("commit error=" + IModule.getErrorCode(r.getResultCode()));
			}
			removeTransactionRecord(tidBytes);
		} catch (Throwable ex) {
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
		bbIndex.WriteUInt(state);
		try (var batch = database.borrowBatch()) {
			commitPoint.put(batch, tidBytes, tidBytes.length, bb.Bytes, bb.WriteIndex);
			commitIndex.put(batch, tidBytes, tidBytes.length, bbIndex.Bytes, bbIndex.WriteIndex);
			batch.commit(state == Commit.eCommitting ? syncWriteOptions : writeOptions);
		}
	}

	// 事务完结（commit/undo/prepare失败）即同时删除commitIndex与commitPoint，两者必须同批：
	// 只删index会让commitPoint随事务总量无界增长（默认本地提交模式下每事务一条死记录）。
	// 已完结事务无人再查询（桶侧onTimer只查map中存活事务；tid不复用），迟到query按
	// eCommitNotExist处理（Commit.ProcessQueryRequest对null的映射）。
	private void removeTransactionRecord(byte[] tidBytes) {
		try {
			try (var batch = database.borrowBatch()) {
				commitIndex.delete(batch, tidBytes);
				commitPoint.delete(batch, tidBytes);
				batch.commit(writeOptions);
			}
		} catch (RocksDBException e) {
			// 这个错误仅仅记录日志，所有没有删除的index，以后重启和Timer会尝试重做。
			logger.error("", e);
		} finally {
			// 移除在途登记必须在记录删除之后（含删除失败：残留index仍交由Timer重做，
			// redo重发的undo与协调者终局路径已发的undo幂等）。
			inFlightTids.remove(ByteBuffer.ToLongBE(tidBytes, 0));
		}
	}
}
