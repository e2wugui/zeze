package Zeze.Dbh2;

import java.io.Closeable;
import java.util.HashMap;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import org.rocksdb.RocksDBException;

public class Dbh2Transaction implements Closeable {
	private final HashMap<Lockey, Lockey> locks = new HashMap<>();
	private final BBatch.Data logs = new BBatch.Data();
	private final BPrepareBatch.Data prepare;
	private final long createTime;

	public BPrepareBatch.Data getPrepareBatch() {
		return prepare;
	}

	@Override
	public String toString() {
		return prepare.getBatch().getQueryIp() + ":" + prepare.getBatch().getQueryPort() + " logs=" + logs;
	}

	public String getQueryIp() {
		return prepare.getBatch().getQueryIp();
	}

	public int getQueryPort() {
		return prepare.getBatch().getQueryPort();
	}

	public long getCreateTime() {
		return createTime;
	}

	/**
	 * 锁住输入batch中的所有记录。
	 *
	 * @param prepare prepare batch parameter
	 */
	public Dbh2Transaction(Dbh2 dbh2, BPrepareBatch.Data prepare) throws InterruptedException {
		this.prepare = prepare;
		this.createTime = System.currentTimeMillis();

		for (var put : prepare.getBatch().getPuts().entrySet()) {
			var key = put.getKey();
			var lock = dbh2.getManager().getLock(prepare.getDatabase(), prepare.getTable(), key);
			if (null == locks.putIfAbsent(lock, lock))
				lock.lock(dbh2);
		}
		for (var del : prepare.getBatch().getDeletes()) {
			var lock = dbh2.getManager().getLock(prepare.getDatabase(), prepare.getTable(), del);
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
	public void prepareBatch(Bucket bucket, BPrepareBatch.Data prepare) throws RocksDBException {
		var b = bucket.getBatch();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (b) {
			b.clear();
			for (var put : prepare.getBatch().getPuts().entrySet()) {
				var key = put.getKey();
				var exist = bucket.get(key);
				if (exist != null)
					logs.getPuts().put(key, exist);
				else
					logs.getDeletes().add(key);
				bucket.getTData().put(b, key, put.getValue());
			}
			for (var del : prepare.getBatch().getDeletes()) {
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
