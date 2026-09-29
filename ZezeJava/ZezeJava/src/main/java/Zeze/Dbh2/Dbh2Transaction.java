package Zeze.Dbh2;

import java.io.Closeable;
import java.util.HashMap;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Serialize.ByteBuffer;
import org.rocksdb.RocksDBException;

/**
 * Dbh2 桶内事务：构造时按 batch 记录键加锁，执行 prepare/commit/undo 的存储操作。
 */
public class Dbh2Transaction implements Closeable {
	private final HashMap<Lockey, Lockey> locks = new HashMap<>();
	private final BBatch.Data batch;
	private final long createTime;
	// 单调钟年龄基准（FND29 dbh2-03）：onTimer 超时围栏的判据。墙钟（createTime）在 NTP
	// 步进/VM 恢复下可前跳越配置余量，把仍在协调者合法 prepare 窗口内的事务误判超时
	//（误 undo 已决定提交的事务=客户端确认成功而数据灭失）；nanoTime 不受步进影响，
	// 两机真实速率漂移（ppm 级）远小于配置余量。createTime 保留供日志展示。
	private final long createNanos = System.nanoTime();

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

	/** 单调钟年龄（毫秒），onTimer 超时围栏判据（见 createNanos 注释）。 */
	public long elapsedMillis() {
		return (System.nanoTime() - createNanos) / 1_000_000L;
	}

	/**
	 * 锁住输入batch中的所有记录。
	 *
	 * @param batch batch parameter
	 */
	public Dbh2Transaction(Dbh2 dbh2, BBatch.Data batch) throws InterruptedException {
		this.batch = batch;
		this.createTime = System.currentTimeMillis();

		try {
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
		} catch (RuntimeException | InterruptedException e) {
			// serialize模式下中途键冲突抛出时必须释放已获取的锁，否则泄漏的锁会让
			// 该键的事务在GC清理WeakHashSet之前全部prepare失败。unlock只作用于
			// locked=true的条目，map中未获取成功的（含冲突键）不会被误放。
			close();
			throw e;
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
		var tidBytes = ByteBuffer.Allocate();
		tidBytes.WriteLong(tid);
		bucket.getTrans().put(tidBytes.Bytes, tidBytes.ReadIndex, tidBytes.size(),
				value.Bytes, value.ReadIndex, value.WriteIndex);
	}

	public void undoBatch(Bucket bucket) throws RocksDBException {
		var tid = batch.getTid();
		var tidBytes = ByteBuffer.Allocate();
		tidBytes.WriteLong(tid);
		bucket.getTrans().delete(tidBytes.Bytes, tidBytes.ReadIndex, tidBytes.size());
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
		var tidBytes = ByteBuffer.Allocate();
		tidBytes.WriteLong(tid);
		bucket.getTrans().delete(b, tidBytes.Bytes, tidBytes.ReadIndex, tidBytes.size());

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
