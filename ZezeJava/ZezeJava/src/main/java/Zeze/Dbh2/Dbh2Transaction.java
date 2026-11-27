package Zeze.Dbh2;

import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.util.HashMap;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import org.rocksdb.RocksDBException;

/**
 * Dbh2 桶内事务：构造时按 batch 记录键加锁，执行 prepare/commit/undo 的存储操作。
 */
public class Dbh2Transaction implements Closeable {
	private final HashMap<Lockey, Lockey> locks = new HashMap<>();
	private final BBatch.Data batch;
	private final long createTime;
	// 单调钟年龄基准，作为 onTimer 超时判据以规避墙钟跳变；createTime 仅用于日志展示
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

	/** 单调钟年龄（毫秒），作为 onTimer 超时判据。 */
	public long elapsedMillis() {
		return (System.nanoTime() - createNanos) / 1_000_000L;
	}

	// trans 列族键格式：blob 为 varint(tid)，墓碑 marker 为 varint(tid)+0x01 尾字节（空值），供 loadSnapshot 区分状态
	static final byte TransTombstoneMarker = 1;

	static byte[] transBlobKey(long tid) {
		var bb = ByteBuffer.Allocate(9);
		bb.WriteLong(tid);
		return bb.Copy();
	}

	static byte[] transTombstoneMarkerKey(long tid) {
		var bb = ByteBuffer.Allocate(10);
		bb.WriteLong(tid);
		bb.WriteByte(TransTombstoneMarker);
		return bb.Copy();
	}

	/** key为墓碑marker（varint(tid)+0x01尾字节）时返回tid，blob（恰为varint(tid)）返回null。 */
	static @Nullable Long decodeTombstoneMarkerTid(byte[] key) {
		var bb = ByteBuffer.Wrap(key);
		var tid = bb.ReadLong();
		return bb.size() == 1 && key[key.length - 1] == TransTombstoneMarker ? tid : null;
	}

	/**
	 * 锁住输入 batch 中的所有记录。
	 *
	 * @param batch 事务批次数据
	 */
	public Dbh2Transaction(Dbh2 dbh2, BBatch.Data batch) throws InterruptedException {
		this.batch = batch;
		this.createTime = System.currentTimeMillis();

		try {
			for (var put : batch.getPuts().entrySet())
				acquire(dbh2, put.getKey());
			for (var del : batch.getDeletes())
				acquire(dbh2, del);
		} catch (RuntimeException | InterruptedException e) {
			// 中途加锁失败必须释放已获取的锁，避免泄漏
			close();
			throw e;
		}
	}

	// 同 batch 重复键去重且仅在加锁成功后保留在 locks 中，防止 close() 误释他事务持有的共享 Lockey
	private void acquire(Dbh2 dbh2, Binary key) throws InterruptedException {
		var lock = dbh2.getLocks().get(key);
		if (null != locks.putIfAbsent(lock, lock))
			return;
		try {
			lock.lock(dbh2);
		} catch (RuntimeException | InterruptedException e) {
			locks.remove(lock); // 未由本事务获取，不得进入待释放集合
			throw e;
		}
	}

	/** loadSnapshot 墓碑路径无锁重建（墓碑化时已释放锁）。 */
	Dbh2Transaction(BBatch.Data batch) {
		this.batch = batch;
		this.createTime = System.currentTimeMillis();
	}

	/**
	 * 把 batch 数据写入 trans 表，供后续提交或回滚使用。
	 *
	 * @param bucket 目标桶
	 */
	public void prepareBatch(Bucket bucket) throws RocksDBException {
		var value = ByteBuffer.encode(batch);
		var tidBytes = transBlobKey(batch.getTid());
		bucket.put(bucket.getTrans(), tidBytes, 0, tidBytes.length, value.Bytes, value.ReadIndex, value.WriteIndex);
	}

	/** 写入墓碑 marker（幂等），与 blob 共存于 trans 列族供 loadSnapshot 分态重建。 */
	public void markTombstoned(Bucket bucket) throws RocksDBException {
		var marker = transTombstoneMarkerKey(batch.getTid());
		bucket.put(bucket.getTrans(), marker, 0, marker.length, ByteBuffer.Empty, 0, 0);
	}

	public void undoBatch(Bucket bucket) throws RocksDBException {
		if (bucket.isApplying()) {
			bucket.delete(bucket.getTrans(), transBlobKey(batch.getTid()));
			bucket.delete(bucket.getTrans(), transTombstoneMarkerKey(batch.getTid()));
			return;
		}
		// 直接调用也保持blob与marker同批删除。
		try (var b = bucket.getDb().newBatch()) {
			var tidBytes = transBlobKey(batch.getTid());
			bucket.getTrans().delete(b, tidBytes, 0, tidBytes.length);
			var marker = transTombstoneMarkerKey(batch.getTid());
			bucket.getTrans().delete(b, marker, 0, marker.length);
			b.commit(bucket.getWriteOptions());
		}
	}

	public void commitBatch(Bucket bucket) throws RocksDBException {
		var b = bucket.getBatch();
		if (!bucket.isApplying())
			b.clear(); // apply的批次可能已含同步队列，不得清空
		for (var put : batch.getPuts().entrySet()) {
			var key = put.getKey();
			var value = put.getValue();
			bucket.getData().put(b, key, value);
		}
		for (var del : batch.getDeletes()) {
			bucket.getData().delete(b, del);
		}

		// 终局：blob与marker同批删除（与数据写同一WriteBatch，apply内原子重放）。
		var tidBytes = transBlobKey(batch.getTid());
		bucket.getTrans().delete(b, tidBytes, 0, tidBytes.length);
		var markerBytes = transTombstoneMarkerKey(batch.getTid());
		bucket.getTrans().delete(b, markerBytes, 0, markerBytes.length);

		if (!bucket.isApplying())
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
