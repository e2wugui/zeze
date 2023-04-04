package Zeze.Dbh2;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import Zeze.Builtin.Dbh2.BBeginTransactionArgument;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BCommitTransactionArgument;
import Zeze.Builtin.Dbh2.BDeleteArgument;
import Zeze.Builtin.Dbh2.BPutArgument;
import Zeze.Builtin.Dbh2.BRollbackTransactionArgument;
import Zeze.Raft.LogSequence;
import Zeze.Raft.Raft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.BackupEngine;
import org.rocksdb.BackupEngineOptions;
import org.rocksdb.Checkpoint;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.Env;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.RestoreOptions;
import org.rocksdb.RocksDBException;

public class Dbh2StateMachine extends Zeze.Raft.StateMachine {
	private static final Logger logger = LogManager.getLogger(Dbh2StateMachine.class);
	private final HashMap<Long, Dbh2Transaction> transactionMap = new HashMap<>();
	private Bucket bucket;
	private TidAllocator tidAllocator;

	public Dbh2StateMachine() {
		super.addFactory(LogBeginTransaction.TypeId_, LogBeginTransaction::new);
		super.addFactory(LogCommitTransaction.TypeId_, LogCommitTransaction::new);
		super.addFactory(LogRollbackTransaction.TypeId_, LogRollbackTransaction::new);
		super.addFactory(LogPut.TypeId_, LogPut::new);
		super.addFactory(LogDelete.TypeId_, LogDelete::new);
		super.addFactory(LogSetBucketMeta.TypeId_, LogSetBucketMeta::new);
		super.addFactory(LogAllocateTid.TypeId_, LogAllocateTid::new);
	}

	public Bucket getBucket() {
		return bucket;
	}

	public TidAllocator getTidAllocator() {
		return tidAllocator;
	}

	public void openBucket() {
		bucket = new Bucket(getRaft().getRaftConfig());
		tidAllocator = new TidAllocator();
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

	public void beginTransaction(BBeginTransactionArgument.Data argument) {
		var transaction = bucket.beginTransaction();
		if (null != transactionMap.putIfAbsent(argument.getTransactionId(), transaction)) {
			logger.error("duplicate tid=" + argument.getTransactionId());
			getRaft().fatalKill();
		}
	}

	public void commitTransaction(BCommitTransactionArgument.Data argument) {
		try {
			var transaction = transactionMap.remove(argument.getTransactionId());
			if (null != transaction)
				transaction.commit();
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void rollbackTransaction(BRollbackTransactionArgument.Data argument) {
		try {
			var transaction = transactionMap.remove(argument.getTransactionId());
			if (null != transaction)
				transaction.rollback();
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void put(BPutArgument.Data argument) {
		try {
			var transaction = transactionMap.get(argument.getTransactionId());
			transaction.put(argument.getKey().bytesUnsafe(), argument.getValue().bytesUnsafe());
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void delete(BDeleteArgument.Data argument) {
		try {
			var transaction = transactionMap.get(argument.getTransactionId());
			transaction.delete(argument.getKey().bytesUnsafe());
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
		backup(cpHome, backupDir);

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

	public void close() {
		bucket.close();
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

			try (var cp = Checkpoint.create(bucket.getDb())) {
				cp.createCheckpoint(checkpointDir);
			}
		} finally {
			raft.unlock();
		}
		return checkpointDir;
	}

	public static ArrayList<ColumnFamilyDescriptor> getColumnFamilies(String dir) throws RocksDBException {
		var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
		if (new File(dir).isDirectory()) {
			for (var cf : OptimisticTransactionDB.listColumnFamilies(Bucket.getCommonOptions(), dir))
				columnFamilies.add(new ColumnFamilyDescriptor(cf, Bucket.getDefaultCfOptions()));
		}
		if (columnFamilies.isEmpty())
			columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(), Bucket.getDefaultCfOptions()));
		return columnFamilies;
	}

	public static void backup(String checkpointDir, String backupDir) throws RocksDBException {
		var outHandles = new ArrayList<ColumnFamilyHandle>();
		try (var src = OptimisticTransactionDB.open(Bucket.getCommonDbOptions(), checkpointDir,
				getColumnFamilies(checkpointDir), outHandles);
			 var backupOptions = new BackupEngineOptions(backupDir);
			 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
			backup.createNewBackup(src, true);
		}
	}

	public void restore(String backupDir) throws RocksDBException {
		getRaft().lock();
		try {
			if (bucket != null) {
				bucket.close(); // close current
				bucket = null;
			}

			var dbName = Paths.get(getDbHome(), "statemachine").toString();
			try (var restoreOptions = new RestoreOptions(false);
				 var backupOptions = new BackupEngineOptions(backupDir);
				 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
				backup.restoreDbFromLatestBackup(dbName, dbName, restoreOptions);
			}

			openBucket(); // reopen
		} finally {
			getRaft().unlock();
		}
	}
}
