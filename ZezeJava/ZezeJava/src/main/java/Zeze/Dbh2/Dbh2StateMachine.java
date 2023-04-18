package Zeze.Dbh2;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.Raft;
import Zeze.Util.Random;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.Checkpoint;
import org.rocksdb.RocksDBException;

public class Dbh2StateMachine extends Zeze.Raft.StateMachine {
	private static final Logger logger = LogManager.getLogger(Dbh2StateMachine.class);
	private Bucket bucket;
	private TidAllocator tidAllocator;
	private final ConcurrentHashMap<Binary, Dbh2Transaction> transactions = new ConcurrentHashMap<>();
	private Future<?> timer;
	private CommitAgent commitAgent;

	public Dbh2StateMachine() {
		super.addFactory(LogPrepareBatch.TypeId_, LogPrepareBatch::new);
		super.addFactory(LogCommitBatch.TypeId_, LogCommitBatch::new);
		super.addFactory(LogUndoBatch.TypeId_, LogUndoBatch::new);
		super.addFactory(LogSetBucketMeta.TypeId_, LogSetBucketMeta::new);
		super.addFactory(LogAllocateTid.TypeId_, LogAllocateTid::new);
	}

	public Bucket getBucket() {
		return bucket;
	}

	public TidAllocator getTidAllocator() {
		return tidAllocator;
	}

	public ConcurrentHashMap<Binary, Dbh2Transaction> getTransactions() {
		return transactions;
	}

	public void openBucket() {
		if (bucket != null)
			return;
		bucket = new Bucket(getRaft().getRaftConfig());
		tidAllocator = new TidAllocator();

		if (null == timer) {
			var period = getRaft().getRaftConfig().getAppendEntriesTimeout() + 200;
			var delay = Random.getInstance().nextLong(period);
			timer = Task.scheduleUnsafe(delay, period, this::onTimer);
		}

		if (null == commitAgent)
			commitAgent = new CommitAgent();
	}

	private void onTimer() {
		if (!getRaft().isLeader())
			return;

		var now = System.currentTimeMillis();
		var period = getRaft().getRaftConfig().getAppendEntriesTimeout() + 200;
		for (var e : transactions.entrySet()) {
			var t = e.getValue();
			if (now - t.getCreateTime() < period * 2L)
				continue;
			var tid = e.getKey();
			//logger.info("onTimer tid=" + tid);
			// todo timeout 状态检查需要重新想一下。
			if (Commit.eCommitNotExist == commitAgent.query(
					t.getQueryIp(), t.getQueryPort(),
					tid)) {
				logger.warn("Undo Timeout " + tid + " " + t);
				getRaft().appendLog(new LogUndoBatch(tid));
			}
		}
	}

	public void setBucketMeta(BBucketMeta.Data argument) {
		try {
			bucket.setMeta(argument);
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	/////////////////////////////////////////////////////////////////////
	// 下面这些方法用于Log.apply，不能失败，失败将停止程序。
	public void allocateTid(long range) {
		try {
			var start = bucket.getTid();
			var end = start + range;
			bucket.setTid(end);
			tidAllocator.setRange(start, end);
		} catch (RocksDBException ex) {
			logger.error("", ex);
			getRaft().fatalKill();
		}
	}

	public void prepareBatch(BBatch.Data bBatch) {
		try {
			var txn = transactions.computeIfAbsent(bBatch.getTid(), _tid -> new Dbh2Transaction(bBatch));
			txn.prepareBatch(bucket, bBatch);
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void commitBatch(Binary tid) {
		//noinspection EmptyTryBlock
		try (var ignored = transactions.remove(tid)) {
			// do nothing
		}
	}

	public void undoBatch(Binary tid) {
		try (var txn = transactions.remove(tid)) {
			if (null != txn)
				txn.undoBatch(bucket);
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	////////////////////////////////////////////////////////////
	// raft implement
	public String getDbHome() {
		return getRaft().getRaftConfig().getDbHome();
	}

	@Override
	public SnapshotResult snapshot(String path) throws Exception {
		long t0 = System.nanoTime();
		SnapshotResult result = new SnapshotResult();
		var cpHome = checkpoint(result);

		long t1 = System.nanoTime();
		var backupDir = Paths.get(getDbHome(), "backup").toString();
		var backupFile = new File(backupDir);
		if (!backupFile.isDirectory() && !backupFile.mkdirs())
			logger.error("create backup directory failed: {}", backupDir);
		RocksDatabase.backup(cpHome, backupDir);

		long t2 = System.nanoTime();
		LogSequence.deleteDirectory(new File(cpHome));
		Zeze.Raft.RocksRaft.Rocks.createZipFromDirectory(backupDir, path);

		long t3 = System.nanoTime();
		getRaft().getLogSequence().commitSnapshot(path, result.lastIncludedIndex);

		result.success = true;
		result.checkPointNanoTime = t1 - t0;
		result.backupNanoTime = t2 - t1;
		result.zipNanoTime = t3 - t2;
		result.totalNanoTime = System.nanoTime() - t0;
		return result;
	}

	public void close() throws Exception {
		for (var tran : transactions.values())
			tran.close();
		transactions.clear();

		if (bucket != null) {
			bucket.close();
			bucket = null;
		}

		if (null != timer) {
			timer.cancel(true);
			timer = null;
		}

		if (null != commitAgent) {
			commitAgent.stop();
			commitAgent = null;
		}
	}

	@Override
	public void reset() {
		var path = Path.of(getDbHome(), "statemachine").toAbsolutePath().toFile();
		LogSequence.deletedDirectoryAndCheck(path, 100);
	}

	@Override
	public void loadSnapshot(String path) throws Exception {
		var backupDir = Paths.get(getDbHome(), "backup").toString();
		var backupFile = new File(backupDir);
		if (!backupFile.isDirectory() || new File(path).lastModified() > backupFile.lastModified()) {
			LogSequence.deletedDirectoryAndCheck(backupFile, 100);
			Zeze.Raft.RocksRaft.Rocks.extractZipToDirectory(path, backupDir);
		}
		restore(backupDir);
	}

	public String checkpoint(SnapshotResult result) throws RocksDBException {
		var checkpointDir = Paths.get(getDbHome(), "checkpoint_" + System.currentTimeMillis()).toString();

		// fast checkpoint, will stop application apply.
		Raft raft = getRaft();
		raft.lock();
		try {
			var lastAppliedLog = raft.getLogSequence().lastAppliedLogTermIndex();
			result.lastIncludedIndex = lastAppliedLog.getIndex();
			result.lastIncludedTerm = lastAppliedLog.getTerm();

			try (var cp = Checkpoint.create(bucket.getDb().getRocksDb())) {
				cp.createCheckpoint(checkpointDir);
			}
		} finally {
			raft.unlock();
		}
		return checkpointDir;
	}

	public void restore(String backupDir) throws Exception {
		getRaft().lock();
		try {
			close();
			var dbName = Paths.get(getDbHome(), "statemachine").toString();
			RocksDatabase.restore(backupDir, dbName);
			openBucket(); // reopen
		} finally {
			getRaft().unlock();
		}
	}
}
