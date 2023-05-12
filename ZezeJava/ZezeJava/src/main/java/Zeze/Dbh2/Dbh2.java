package Zeze.Dbh2;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.CommitBatch;
import Zeze.Builtin.Dbh2.KeepAlive;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Builtin.Dbh2.SetBucketMeta;
import Zeze.Builtin.Dbh2.SplitPut;
import Zeze.Builtin.Dbh2.UndoBatch;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Raft.Agent;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.FuncLong;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class Dbh2 extends AbstractDbh2 implements Closeable {
	private static final Logger logger = LogManager.getLogger(Dbh2.class);
	private final Dbh2Config config = new Dbh2Config();
	private final Raft raft;
	private final Dbh2StateMachine stateMachine;
	private final Dbh2Manager manager;

	// 性能统计。
	private final AtomicLong counterGet = new AtomicLong();
	private final AtomicLong counterPut = new AtomicLong();
	private final AtomicLong sizeGet = new AtomicLong();
	private final AtomicLong sizePut = new AtomicLong();
	private final AtomicLong counterDelete = new AtomicLong();
	private final AtomicLong counterPrepareBatch = new AtomicLong();
	private final AtomicLong counterCommitBatch = new AtomicLong();
	private final AtomicLong counterUndoBatch = new AtomicLong();

	private long lastGet;
	private long lastPut;
	private long lastSizeGet;
	private long lastSizePut;
	private long lastDelete;
	private long lastPrepareBatch;
	private long lastCommitBatch;
	private long lastUndoBatch;
	private long lastReportTime = System.currentTimeMillis();

	public double load() {
		var now = System.currentTimeMillis();
		var elapse = (now - lastReportTime) / 1000.0f;
		lastReportTime = now;

		var nowGet = counterGet.get();
		var nowPut = counterPut.get();
		var nowSizeGet = sizeGet.get();
		var nowSizePut = sizePut.get();
		var nowDelete = counterDelete.get();
		var nowPrepareBatch = counterPrepareBatch.get();
		var nowCommitBatch = counterCommitBatch.get();
		var nowUndoBatch = counterUndoBatch.get();

		var diffGet = nowGet - lastGet;
		var diffPut = nowPut - lastPut;
		var diffSizeGet = nowSizeGet - lastSizeGet;
		var diffSizePut = nowSizePut - lastSizePut;
		var diffDelete = nowDelete - lastDelete;
		var diffPrepareBatch = nowPrepareBatch - lastPrepareBatch;
		var diffCommitBatch = nowCommitBatch - lastCommitBatch;
		var diffUndoBatch = nowUndoBatch - lastUndoBatch;

		if (diffGet > 0 || diffPut > 0 || diffDelete > 0 || diffSizeGet > 0 || diffSizePut > 0
				|| diffPrepareBatch > 0 || diffCommitBatch > 0 || diffUndoBatch > 0) {
			lastGet = nowGet;
			lastPut = nowPut;
			lastSizeGet = nowSizeGet;
			lastSizePut = nowSizePut;
			lastDelete = nowDelete;
			lastPrepareBatch = nowPrepareBatch;
			lastCommitBatch = nowCommitBatch;
			lastUndoBatch = nowUndoBatch;

			//noinspection StringBufferReplaceableByString
			var sb = new StringBuilder();
			var avgGet = diffGet / elapse;
			var avgPut = diffPut / elapse;
			var avgDelete = diffDelete / elapse;

			sb.append(" get=").append(avgGet);
			sb.append(" put=").append(avgPut);
			sb.append(" getSize=").append(diffSizeGet / elapse);
			sb.append(" putSize=").append(diffSizePut / elapse);
			sb.append(" delete=").append(avgDelete);
			sb.append(" prepare=").append(diffPrepareBatch / elapse);
			sb.append(" commit=").append(diffCommitBatch / elapse);
			sb.append(" undo=").append(diffUndoBatch / elapse);

			logger.info(sb.toString());

			// 负载，put，delete全算，get算1%。
			return (avgPut + avgDelete) + avgGet * 0.01;
		}
		return 0.0;
	}

	public class Dbh2RaftServer extends Zeze.Raft.Server {
		public Dbh2RaftServer(Raft raft, String name, Config config) {
			super(raft, name, config);
		}

		@Override
		public void dispatchRaftRequest(Protocol<?> p, FuncLong func, String name, Action0 cancel,
										DispatchMode mode) throws Exception {
			raft.getUserThreadExecutor().execute(() -> Task.call(func, p));
		}
	}

	public Raft getRaft() {
		return raft;
	}

	public Dbh2Manager getManager() {
		return manager;
	}

	public Dbh2Config getConfig() {
		return config;
	}

	public Dbh2(Dbh2Manager manager, String raftName, RaftConfig raftConf, Config config, boolean writeOptionSync) {
		this.manager = manager;

		if (config == null)
			config = new Config().addCustomize(this.config).loadAndParse();

		try {
			stateMachine = new Dbh2StateMachine(this);
			raft = new Raft(stateMachine, raftName, raftConf, config, "Zeze.Dbh2.Server", Dbh2RaftServer::new);
			raftConf.setSnapshotCommitDelayed(true);
			logger.info("newRaft: {}", raft.getName());
			stateMachine.openBucket();
			var writeOptions = writeOptionSync ? RocksDatabase.getSyncWriteOptions() : RocksDatabase.getDefaultWriteOptions();
			raft.getLogSequence().setWriteOptions(writeOptions);
			stateMachine.getBucket().setWriteOptions(writeOptions);

			RegisterProtocols(raft.getServer());
			raft.getServer().start();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void close() throws IOException {
		logger.info("closeRaft: " + raft.getName());
		try {
			raft.shutdown();
			stateMachine.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected long ProcessSetBucketMetaRequest(SetBucketMeta r) throws Exception {
		var log = new LogSetBucketMeta(r);
		raft.appendLog(log, r.Result, (raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException)); // result is empty
		return 0;
	}

	@Override
	protected long ProcessGetRequest(Zeze.Builtin.Dbh2.Get r) throws RocksDBException {
		counterGet.incrementAndGet();
		var lock = Lock.get(r.Argument.getKey());
		lock.lock();
		try {
			// 直接读取数据库。是否可以读取由raft控制。raft启动时有准备阶段。
			var bucket = stateMachine.getBucket();
			if (!bucket.inBucket(r.Argument.getDatabase(), r.Argument.getTable(), r.Argument.getKey()))
				return errorCode(eBucketMissmatch);
			var value = bucket.get(r.Argument.getKey());
			if (null == value)
				r.Result.setNull(true);
			else {
				r.Result.setValue(value);
				sizeGet.addAndGet(value.size());
			}
			r.SendResult();
		} finally {
			lock.unlock();
		}
		return 0;
	}

	@Override
	protected long ProcessKeepAliveRequest(KeepAlive r) throws Exception {
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessPrepareBatchRequest(PrepareBatch r) throws Exception {
		//var tid = stateMachine.getTidAllocator().next(stateMachine);
		// lock
		counterPrepareBatch.incrementAndGet();
		var txn = new Dbh2Transaction(r.Argument.getBatch());
		try {
			// save txn
			if (null != stateMachine.getTransactions().putIfAbsent(r.Argument.getBatch().getTid(), txn))
				return errorCode(eDuplicateTid);
			// check inBucket
			if (!stateMachine.getBucket().inBucket(r.Argument.getDatabase(), r.Argument.getTable()))
				return errorCode(eBucketMissmatch);

			var refused = r.Result; // 对于puts可以考虑只传key，网络占用少一些。
			for (var e : r.Argument.getBatch().getPuts().entrySet()) {
				var key = e.getKey();
				var value = e.getValue();
				counterPut.incrementAndGet();
				sizePut.addAndGet(value.size() + key.size());
				if (!stateMachine.getBucket().inBucket(key)) {
					refused.getPuts().put(key, value);
				}
			}
			for (var del : r.Argument.getBatch().getDeletes()) {
				counterDelete.incrementAndGet();
				if (!stateMachine.getBucket().inBucket(del)) {
					refused.getDeletes().add(del);
				}
			}

			// 移除拒绝的数据。
			for (var e : refused.getPuts().entrySet())
				r.Argument.getBatch().getPuts().remove(e.getKey());
			for (var e : refused.getDeletes())
				r.Argument.getBatch().getDeletes().remove(e);

			// apply to raft
			getRaft().appendLog(new LogPrepareBatch(r), (raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
			// 操作成功，释放所有权。see finally.
			txn = null;
		} finally {
			if (null != txn) {
				stateMachine.getTransactions().remove(r.Argument.getBatch().getTid()); // undo putIfAbsent
				txn.close();
			}
		}
		return 0;
	}

	@Override
	protected long ProcessCommitBatchRequest(CommitBatch r) throws Exception {
		counterCommitBatch.incrementAndGet();
		getRaft().appendLog(new LogCommitBatch(r), (raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

	@Override
	protected long ProcessUndoBatchRequest(UndoBatch r) throws Exception {
		counterUndoBatch.incrementAndGet();
		getRaft().appendLog(new LogUndoBatch(r), (raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

	class RaftAgentNetClient extends Agent.NetClient {

		public RaftAgentNetClient(Agent agent, String name, Config config) {
			super(agent, name, config);
		}

		@Override
		public <P extends Protocol<?>> void dispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle) {
			raft.getUserThreadExecutor().execute(() -> {
				try {
					responseHandle.handle(rpc);
				} catch (Throwable e) { // run handle. 必须捕捉所有异常。logger.error
					logger.error("Agent.NetClient.dispatchRpcResponse", e);
				}
			});
		}

		@Override
		public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) {
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			// 虚拟线程创建太多Critical线程反而容易卡,以后考虑跑另个虚拟线程池里
			if (p.getTypeId() == Zeze.Raft.LeaderIs.TypeId_) {
				Task.getCriticalThreadPool().execute(() -> Task.call(() -> p.handle(this, factoryHandle), "InternalRequest"));
			} else {
				raft.getUserThreadExecutor().execute(() -> Task.runUnsafe(
						() -> p.handle(this, factoryHandle),
						p,
						null,
						null,
						DispatchMode.Normal));
			}
		}
	}

	public void tryStartSplit() throws Exception {
		if (!raft.isLeader())
			return;
		var bucket = stateMachine.getBucket();
		var splitting = bucket.getMetaSplitting();
		if (null != splitting) {
			var meta = bucket.getMeta();
			logger.info("start but splitting found. database={} table={}", meta.getDatabaseName(), meta.getTableName());
			return; // splitting
		}

		startSplit();
	}

	private volatile long splitSerialNo;
	private volatile Dbh2Agent dbh2Splitting;
	private volatile BBucketMeta.Data splittingMeta;

	private RocksIterator locateMiddle() throws RocksDBException {
		var bucket = stateMachine.getBucket();
		var it = bucket.getTData().iterator();
		var count = bucket.getTData().getKeyNumbers() / 2;
		for (it.seekToFirst(); it.isValid() && count > 0; it.next(), --count) {
			// searching middle
		}
		if (!it.isValid()) {
			it.close();
			throw new RocksDBException("middle key not found.");
		}
		return it;
	}

	private RocksIterator locateMiddle(Binary middleKey) throws RocksDBException {
		var it = stateMachine.getBucket().getTData().iterator();
		it.seek(Database.copyIf(middleKey.bytesUnsafe(), middleKey.getOffset(), middleKey.size()));
		if (!it.isValid()) {
			it.close();
			throw new RocksDBException("middle key not found.");
		}
		return it;
	}

	// 开始分桶流程有两个线程需要访问：timer & raft.UserThreadExecutor
	private synchronized void startSplit() throws Exception {
		// 后面需要在lambda中传递系列号作为上下文，使用成员变量是不是会跟随变化？
		var serialNo = manager.atomicSerialNo.incrementAndGet();
		splitSerialNo = serialNo;
		var bucket = stateMachine.getBucket();
		RocksIterator it = null;
		var splitting = bucket.getMetaSplitting(); // 对于timer，这个会调用两次，先这样。
		try {
			if (null == splitting) {
				// 第一次开始分桶，准备阶段。
				// 这个阶段在timer回调中执行，可以同步调用一些网络接口。
				var metaCopy = bucket.getMeta().copy();
				it = locateMiddle();
				splitting.setKeyFirst(new Binary(it.key()));
				splitting.setKeyLast(metaCopy.getKeyLast());
				// 以当前的Meta拿去创建分桶目标。所以，分桶目标开始是以源桶的FirstKey为索引。
				// 目标分桶并不会保存Meta，以后分桶完成，会设置正确的Meta。
				// see Master.MasterDatabase.createSplitBucket
				splitting = manager.getMasterAgent().createSplitBucket(metaCopy);

				// 设置分桶meta，即标记到raft集群中。
				getRaft().appendLog(new LogSetSplittingMeta(splitting));
				// 创建到分桶目标的客户端。
				dbh2Splitting = new Dbh2Agent(splitting.getRaftConfig(), RaftAgentNetClient::new);
				splittingMeta = splitting;
				logger.info("splitting start... {}@{}", splitting.getTableName(), splitting.getDatabaseName());
			}
			if (null == it) {
				// 重新开始分桶时走这个分支，根据上次找到的middle，定位it。
				it = locateMiddle(splitting.getKeyFirst());
				logger.info("splitting restart... {}@{}", splitting.getTableName(), splitting.getDatabaseName());
			}

			// 开始同步数据，这个阶段对于rocks时同步访问的，对于网络是异步的。
			var puts = buildSplitPut(it);
			var fit = it;
			dbh2Splitting.getRaftAgent().send(puts, (p) -> splitPutNext((SplitPut)p, fit, serialNo));
			it = null;
		} finally {
			if (null != it)
				it.close();
		}
	}

	private static SplitPut buildSplitPut(RocksIterator it) {
		var r = new SplitPut();
		r.Argument.setFromTransaction(false);
		var count = 100; // todo config
		for (; it.isValid() && count > 0; it.next(), --count) {
			r.Argument.getPuts().put(new Binary(it.key()), new Binary(it.value()));
		}
		return r;
	}

	public long splitPutNext(SplitPut r, RocksIterator it, long serialNo) {
		try {
			if (!raft.isLeader() || dbh2Splitting == null || serialNo != splitSerialNo) {
				it.close();
				dbh2Splitting.close();
				return 0;
			}

			if (r.getResultCode() != 0) {
				startSplit();
				return 0;
			}

			var puts = buildSplitPut(it);
			if (puts.Argument.getPuts().isEmpty()) {
				it.close();

				// 启动新的线程执行结束任务，需要多次同步调用。
				Task.run(this::endSplit, "endSplit");
				return 0; // split done.
			}

			dbh2Splitting.getRaftAgent().send(puts, (p) -> splitPutNext((SplitPut)p, it, serialNo));
		} catch (Exception ex) {
			logger.error("", ex);
		}
		return 0;
	}

	private synchronized void endSplit() throws Exception {
		logger.info("splitting end... {}@{}", splittingMeta.getTableName(), splittingMeta.getDatabaseName());

		// 第一步
		// 设置目标桶的meta
		dbh2Splitting.setBucketMeta(stateMachine.getBucket().getMetaSplitting());
		// 公布新桶信息。新桶已经准备好，可以使用了。
		manager.getMasterAgent().publishSplitBucketNew(splittingMeta);

		// 【时间窗口】【需要仔细考虑一下】
		// 此时，本Dbh2还以为自己是分之前桶，所以事务还在进行，会被同步到新桶。

		// 第二步
		// 修改源桶的meta。
		// 源桶信息修改后，本Dbh2就进入拒绝模式。
		var copy = stateMachine.getBucket().getMeta().copy();
		copy.setKeyLast(splittingMeta.getKeyFirst());
		getRaft().appendLog(new LogSetBucketMeta(copy));
		manager.getMasterAgent().publishSplitBucketOld(copy);

		// 第四步
		// 清除raft集群分桶标志。
		getRaft().appendLog(new LogDeleteSplittingMeta());
		// 切换回主线程清除进程内标记，防止将要清除的变量还被使用中。特别是dbh2Splitting。
		getRaft().getUserThreadExecutor().execute(this::clearSplitting);
	}

	private void clearSplitting() {
		try {
			dbh2Splitting.close();
		} catch (Exception ex) {
			logger.error("", ex);
		}
		dbh2Splitting = null;
		splittingMeta = null;
	}

	public void onCommitBatch(Dbh2Transaction txn) {
		if (splittingMeta == null || dbh2Splitting == null)
			return;

		var r = new SplitPut();
		r.Argument.setFromTransaction(true);

		// 如果修改的记录落在分桶目标桶中，则同步过去。
		for (var e : txn.getBatch().getPuts().entrySet()) {
			if (splittingMeta.getKeyFirst().compareTo(e.getKey()) <= 0)
				r.Argument.getPuts().put(e.getKey(), e.getValue());
		}
		for (var delete : txn.getBatch().getDeletes()) {
			if (splittingMeta.getKeyFirst().compareTo(delete) <= 0)
				r.Argument.getPuts().put(delete, Binary.Empty);
		}

		// 事务同步流程不能重试，因为提交之后就有新的并发事务过来，而这里是异步的，重试时数据可能不是最新的了。
		dbh2Splitting.getRaftAgent().send(r, (p) -> {
			try {
				if (r.getResultCode() != 0)
					startSplit(); // restart split.
				return 0;
			} catch (Exception ex) {
				logger.error("", ex);
				return Procedure.Exception;
			}
		});
	}

	@Override
	protected long ProcessSplitPutRequest(SplitPut r) throws Exception {
		raft.appendLog(new LogSplitPut(r),
				(raftLog, result) -> r.SendResultCode(result ? 0 : Procedure.CancelException));
		return 0;
	}

}
