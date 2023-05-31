package Zeze.Dbh2;

import java.io.Closeable;
import java.util.HashMap;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import org.rocksdb.RocksDBException;

public class Dbh2Transaction implements Closeable {
	private final HashMap<Lockey, Lockey> locks = new HashMap<>();
	private final BBatch.Data logs = new BBatch.Data();
	private final BBatch.Data batch;
	private final long createTime;

	public BBatch.Data getBatch() {
		return batch;
	}

	@Override
	public String toString() {
		return batch.getQueryIp() + ":" + batch.getQueryPort() + " logs=" + logs;
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
	 * @param prepare prepare batch
	 */
	public void prepareBatch(Bucket bucket, BBatch.Data batch) throws RocksDBException {
		var b = bucket.getBatch();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (b) {
			b.clear();
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
		var b = bucket.getBatch();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (b) {
			b.clear();
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
