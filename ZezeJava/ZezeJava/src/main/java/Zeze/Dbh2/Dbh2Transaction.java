package Zeze.Dbh2;

import java.io.Closeable;
import java.util.HashMap;
import Zeze.Builtin.Dbh2.BBatch;
import org.rocksdb.RocksDBException;

public class Dbh2Transaction implements Closeable {
	private final HashMap<Lock, Lock> locks = new HashMap<>();
	private final BBatch.Data logs = new BBatch.Data();
	private final String queryIp;
	private final int queryPort;
	private final long createTime;

	public String getQueryIp() {
		return queryIp;
	}

	public int getQueryPort() {
		return queryPort;
	}

	public long getCreateTime() {
		return createTime;
	}

	/**
	 * 锁住输入batch中的所有记录。
	 *
	 * @param batch batch parameter
	 */
	public Dbh2Transaction(BBatch.Data batch) {
		this.queryIp = batch.getQueryIp();
		this.queryPort = batch.getQueryPort();
		this.createTime = System.currentTimeMillis();

		for (var put : batch.getPuts().entrySet()) {
			var key = put.getKey();
			var lock = Lock.get(key);
			if (null == locks.putIfAbsent(lock, lock))
				lock.lock();
		}
		for (var del : batch.getDeletes()) {
			var lock = Lock.get(del);
			if (null == locks.putIfAbsent(lock, lock))
				lock.lock();
		}
	}

	/**
	 * 把batch数据写入db，并且构造出undo logs。
	 *
	 * @param bucket bucket
	 * @param batch  batch
	 */
	public void prepareBatch(Bucket bucket, BBatch.Data batch) throws RocksDBException {
		try (var b = bucket.getDb().newBatch()) {
			for (var put : batch.getPuts().entrySet()) {
				var key = put.getKey();
				var exist = bucket.get(key);
				if (exist != null)
					logs.getPuts().put(key, exist);
				else
					logs.getDeletes().add(key);
				bucket.getTData().put(b, key, put.getValue());
			}
			for (var del : batch.getDeletes()) {
				var exist = bucket.get(del);
				if (exist != null)
					logs.getPuts().put(del, exist);
				// else delete not exist record. skip.
				bucket.getTData().delete(b, del);
			}
			b.commit(bucket.getWriteOptions());
		}
	}

	public void undoBatch(Bucket bucket) throws RocksDBException {
		try (var b = bucket.getDb().newBatch()) {
			for (var put : logs.getPuts().entrySet()) {
				bucket.getTData().put(b, put.getKey(), put.getValue());
			}
			for (var del : logs.getDeletes()) {
				bucket.getTData().delete(b, del);
			}
			b.commit(bucket.getWriteOptions());
		}
	}

	/**
	 * 完成事务，释放锁。
	 */
	@Override
	public void close() {
		for (var lock : locks.values())
			lock.unlock();
		locks.clear();
	}
}
