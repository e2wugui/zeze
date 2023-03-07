package Zeze.Dbh2;

import java.util.HashMap;
import Zeze.Builtin.Dbh2.BBeginTransactionArgumentData;
import Zeze.Builtin.Dbh2.BCommitTransactionArgumentData;
import Zeze.Builtin.Dbh2.BDeleteArgumentData;
import Zeze.Builtin.Dbh2.BPutArgumentData;
import Zeze.Builtin.Dbh2.BRollbackTransactionArgumentData;
import Zeze.Net.Binary;
import Zeze.Util.PersistentAtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

public class Dbh2StateMachine extends Zeze.Raft.StateMachine {
	private static final Logger logger = LogManager.getLogger(Dbh2StateMachine.class);
	private final PersistentAtomicLong atomicLong; // todo 不能使用这个。
	private final HashMap<Long, Transaction> transactionMap = new HashMap<>();
	private Bucket bucket;

	public Dbh2StateMachine(String raftName) {
		atomicLong = PersistentAtomicLong.getOrAdd(raftName);
		bucket = new Bucket(raftName);

		super.addFactory(LogBeginTransaction.TypeId_, LogBeginTransaction::new);
		super.addFactory(LogCommitTransaction.TypeId_, LogCommitTransaction::new);
		super.addFactory(LogRollbackTransaction.TypeId_, LogRollbackTransaction::new);
		super.addFactory(LogPut.TypeId_, LogPut::new);
		super.addFactory(LogDelete.TypeId_, LogDelete::new);
	}

	/////////////////////////////////////////////////////////////////////
	// 下面这些方法用于Log.apply，不能失败，失败将停止程序。
	public void beginTransaction(BBeginTransactionArgumentData argument) {
		var tid = atomicLong.next();
		var transaction = bucket.beginTransaction();
		if (null != transactionMap.putIfAbsent(tid, transaction))
			getRaft().fatalKill();
	}

	public void commitTransaction(BCommitTransactionArgumentData argument) {
		try {
			var transaction = transactionMap.get(argument.getTransactionId());
			transaction.commit();
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void rollbackTransaction(BRollbackTransactionArgumentData argument) {
		try {
			var transaction = transactionMap.get(argument.getTransactionId());
			transaction.rollback();
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void put(BPutArgumentData argument) {
		try {
			var transaction = transactionMap.get(argument.getTransactionId());
			transaction.put(argument.getKey().bytesUnsafe(), argument.getValue().bytesUnsafe());
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	public void delete(BDeleteArgumentData argument) {
		try {
			var transaction = transactionMap.get(argument.getTransactionId());
			transaction.delete(argument.getKey().bytesUnsafe());
		} catch (RocksDBException e) {
			logger.error("", e);
			getRaft().fatalKill();
		}
	}

	/////////////////////////////////////////////////////////////////////
	// 用于协议处理错误检查。进到Raft.AppendLog之后就不能出错了。对于Follower使用getOrAdd.
	/**
	 * 用于 beginTransaction
	 */
	public boolean inBucket(String database, String table) {
		return bucket.inBucket(database, table);
	}

	/**
	 * 用于 Put, Delete
	 */
	public boolean inBucket(String database, String table, Binary key) {
		return bucket.inBucket(database, table, key);
	}

	@Override
	public SnapshotResult snapshot(String path) throws Exception {
		return null;
	}

	@Override
	public void loadSnapshot(String path) throws Exception {

	}
}
