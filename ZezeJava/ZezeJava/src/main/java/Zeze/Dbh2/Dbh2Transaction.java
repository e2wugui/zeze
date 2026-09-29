package Zeze.Dbh2;

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

	// trans列族键形态：blob=varint(tid)→BBatch编码；墓碑marker=varint(tid)+0x01尾字节
	//→空值（varint终结字节<0x80，0x01尾字节不可能被并入varint，形态无歧义）。marker
	// 仅在墓碑化apply写入、终局与blob同批删除，loadSnapshot据此分态重建。
	// 包内可见供确定性测试直接构造/检查键。
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
	static Long decodeTombstoneMarkerTid(byte[] key) {
		var bb = ByteBuffer.Wrap(key);
		var tid = bb.ReadLong();
		return bb.size() == 1 && key[key.length - 1] == TransTombstoneMarker ? tid : null;
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
			for (var put : batch.getPuts().entrySet())
				acquire(dbh2, put.getKey());
			for (var del : batch.getDeletes())
				acquire(dbh2, del);
		} catch (RuntimeException | InterruptedException e) {
			// serialize模式下中途键冲突抛出时必须释放已获取的锁，否则泄漏的锁会让
			// 该键的事务在GC清理WeakHashSet之前全部prepare失败。map中只含成功获取的
			// 条目（见acquire），失败键不触持有者的共享实例。
			close();
			throw e;
		}
	}

	// 成功获取后才入map：Lockey是Locks按值去重的共享canonical实例，失败键若留在map，
	// close()按locked标志release时放掉的是持有者的信号量（locked=true为持有者所置）——
	// 互斥破坏+permit凭空膨胀（unlock不复位），热键无自愈。先判重复再加锁，同batch重复
	// 键（puts/deletes交叠）只锁一次。
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

	/** 无锁重建（loadSnapshot墓碑路径）：墓碑的锁在墓碑化时已释放，装载时刻无并发
	 * 持锁者，直接以持久化blob构造内存对象。 */
	Dbh2Transaction(BBatch.Data batch) {
		this.batch = batch;
		this.createTime = System.currentTimeMillis();
	}

	/**
	 * 把batch数据写入db，并且构造出undo logs。
	 *
	 * @param bucket bucket
	 */
	public void prepareBatch(Bucket bucket) throws RocksDBException {
		var value = ByteBuffer.encode(batch);
		var tidBytes = transBlobKey(batch.getTid());
		bucket.getTrans().put(tidBytes, 0, tidBytes.length, value.Bytes, value.ReadIndex, value.WriteIndex);
	}

	/** 墓碑marker落盘：空值put幂等（apply重放/重复墓碑化安全），与blob共存于trans列族，
	 * loadSnapshot据此分态重建。 */
	public void markTombstoned(Bucket bucket) throws RocksDBException {
		var marker = transTombstoneMarkerKey(batch.getTid());
		bucket.getTrans().put(bucket.getWriteOptions(), marker, 0, marker.length, ByteBuffer.Empty, 0, 0);
	}

	public void undoBatch(Bucket bucket) throws RocksDBException {
		// blob与marker同批删除（终局原子）；独立newBatch，不占用apply路径独占的共享batch。
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
		b.clear();
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
