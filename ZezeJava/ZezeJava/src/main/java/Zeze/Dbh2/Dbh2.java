package Zeze.Dbh2;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.Dbh2.CommitBatch;
import Zeze.Builtin.Dbh2.KeepAlive;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Builtin.Dbh2.SetBucketMeta;
import Zeze.Builtin.Dbh2.UndoBatch;
import Zeze.Config;
import Zeze.Net.Protocol;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.FuncLong;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

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
			for (var put : r.Argument.getBatch().getPuts().entrySet()) {
				var key = put.getKey();
				var value = put.getValue();
				counterPut.incrementAndGet();
				sizePut.addAndGet(value.size() + key.size());
				if (!stateMachine.getBucket().inBucket(key))
					return errorCode(eBucketMissmatch);
			}
			for (var del : r.Argument.getBatch().getDeletes()) {
				counterDelete.incrementAndGet();
				if (!stateMachine.getBucket().inBucket(del))
					return errorCode(eBucketMissmatch);
			}
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
}
