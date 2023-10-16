package Zeze.Dbh2;

import java.io.Closeable;
import java.util.HashMap;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Serialize.ByteBuffer;
import org.rocksdb.RocksDBException;

public class Dbh2Transaction implements Closeable {
	private final HashMap<Lockey, Lockey> locks = new HashMap<>();
	private final BBatch.Data batch;
	private final long createTime;

	public BBatch.Data getBatch() {
		return batch;
	}

	@Override
	public String toString() {
		return batch.getQueryIp() + "_" + batch.getQueryPort();
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
	public Dbh2Transaction(Dbh2 dbh2, BBatch.Data batch) throws InterruptedException, RocksDBException {
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
	 */
	public void prepareBatch(Bucket bucket) throws RocksDBException {
		var tid = batch.getTid();
		var value = ByteBuffer.encode(batch);
		bucket.getTrans().put(tid.bytesUnsafe(), tid.getOffset(), tid.size(),
				value.Bytes, value.ReadIndex, value.WriteIndex);
	}

	public void undoBatch(Bucket bucket) throws RocksDBException {
		var tid = batch.getTid();
		bucket.getTrans().delete(tid.bytesUnsafe(), tid.getOffset(), tid.size());
	}

	public void commitBatch(Bucket bucket) throws RocksDBException {
		var b = bucket.getBatch();
		b.clear();
		for (var put : batch.getPuts().entrySet()) {
			var key = put.getKey();
			var value = put.getValue();
			bucket.getData().put(b, key, value);
		}
		for (var del : batch.getDeletes()) {
			bucket.getData().delete(b, del);
		}

		var tid = batch.getTid();
		bucket.getTrans().delete(b, tid.bytesUnsafe(), tid.getOffset(), tid.size());

		b.commit(bucket.getWriteOptions());
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
