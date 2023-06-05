package Zeze.Dbh2;

import java.io.Closeable;
import java.util.HashMap;
import Zeze.Builtin.Dbh2.BBatch;
import org.rocksdb.RocksDBException;
import org.rocksdb.Transaction;

public class Dbh2Transaction implements Closeable {
	private final HashMap<Lockey, Lockey> locks = new HashMap<>();
	private final Transaction transaction;
	private final BBatch.Data batch;
	private final long createTime;

	public BBatch.Data getBatch() {
		return batch;
	}

	@Override
	public String toString() {
		return batch.getQueryIp() + ":" + batch.getQueryPort();
	}

	public String getQueryIp() {
		return batch.getQueryIp();
	}

	public int getQueryPort() {
		return batch.getQueryPort();
	}

	public long getCreateTime() {
		return createTime;
	}

	/**
	 * 锁住输入batch中的所有记录。
	 *
	 * @param batch batch parameter
	 */
	public Dbh2Transaction(Dbh2 dbh2, BBatch.Data batch) throws InterruptedException {
		this.transaction = dbh2.getStateMachine().getBucket().getDb().beginTransaction();
		this.batch = batch;
		this.createTime = System.currentTimeMillis();

		for (var put : batch.getPuts().entrySet()) {
			var key = put.getKey();
			var lock = dbh2.getLocks().get(key);
			if (null == locks.putIfAbsent(lock, lock))
				lock.lock(dbh2);
		}
		for (var del : batch.getDeletes()) {
			var lock = dbh2.getLocks().get(del);
			if (null == locks.putIfAbsent(lock, lock))
				lock.lock(dbh2);
		}
	}

	/**
	 * 把batch数据写入db，并且构造出undo logs。
	 *
	 * @param bucket bucket
	 * @param batch prepare batch
	 */
	public void prepareBatch(Bucket bucket, BBatch.Data batch) throws RocksDBException {
		for (var put : batch.getPuts().entrySet()) {
			var key = put.getKey();
			var value = put.getValue();
			bucket.getTData().put(transaction, key, value);
		}
		for (var del : batch.getDeletes()) {
			bucket.getTData().delete(transaction, del);
		}
		// Two phase commit not supported for optimistic transactions.
		//transaction.prepare();
	}

	public void undoBatch(Bucket bucket) throws RocksDBException {
		if (null != transaction)
			transaction.rollback();
	}

	public void commitBatch() throws RocksDBException {
		if (null != transaction)
			transaction.commit();
	}

	/**
	 * 完成事务，释放锁。
	 */
	@Override
	public void close() {
		if (null != transaction)
			transaction.close();
		for (var lock : locks.values())
			lock.unlock();
		locks.clear();
	}
}
