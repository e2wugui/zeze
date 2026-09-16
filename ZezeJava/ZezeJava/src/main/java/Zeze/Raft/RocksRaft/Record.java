package Zeze.Raft.RocksRaft;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.FastLock;
import Zeze.Util.RocksDatabase;
import org.rocksdb.RocksDBException;

public final class Record<K> {
	public static final class RootInfo {
		private final Record<?> record;
		private final TableKey tableKey;

		public RootInfo(Record<?> record, TableKey tableKey) {
			this.record = record;
			this.tableKey = tableKey;
		}

		public Record<?> getRecord() {
			return record;
		}

		public TableKey getTableKey() {
			return tableKey;
		}
	}

	public static final int StateNew = 0;
	public static final int StateLoad = 1;
	private static final AtomicLong timestampGen = new AtomicLong(1);

	public static long getNextTimestamp() {
		return timestampGen.getAndIncrement();
	}

	private final BiConsumer<ByteBuffer, K> keyEncodeFunc;
	private int state = StateNew;
	private long timestamp;
	private boolean removed;
	private Table<K, ?> table;
	private K key;
	// 跨线程发布（FND4-33）：apply线程在raft锁内setValue发布业务线程构造的bean，
	// 读线程（另一业务过程）经Table.get→r.mutex读取——两锁无交点，JMM上无
	// happens-before保障。volatile让发布语义由字段自身保证（写侧仍在raft锁内，
	// 语义不变；x86上无额外开销语义变化）。
	private volatile Bean value;
	final FastLock mutex = new FastLock();

	// 【FND7-14】在用计数：Table.getOrLoad在r.mutex临界区内beginAccess，驱逐回调在
	// r.mutex内复查（互斥，不会漏见），保证"事务持有Record引用期间不可被LRU驱逐"——
	// leader事务原位修改缓存Record的bean，提交时经事务捕获的origin应用并flush，驱逐后
	// 同key再访问会从storage装载出旧值的新记录，已提交更新被静默覆盖丢失。
	// 释放点：Transaction.perform收尾（业务访问的记录）与Rocks.followerApply的flush
	// 成功后（followerApply装载的记录）。pendingFlush补偿集由登记统一持有（putPendingFlush
	// 补记、消费成功/过期丢弃/重登记换手时释放），迟到flush重试窗口内记录不可驱逐。
	// 减法钳制到0：吸收极端时序下的重复释放。
	private final AtomicInteger accessors = new AtomicInteger();

	void beginAccess() {
		accessors.incrementAndGet();
	}

	void endAccess() {
		accessors.updateAndGet(v -> v > 0 ? v - 1 : v);
	}

	boolean isAccessed() {
		return accessors.get() != 0;
	}

	public Record(Class<K> keyClass) {
		keyEncodeFunc = SerializeHelper.createEncodeFunc(keyClass);
	}

	public Record(BiConsumer<ByteBuffer, K> keyEncodeFunc) {
		this.keyEncodeFunc = keyEncodeFunc;
	}

	public int getState() {
		return state;
	}

	public void setState(int value) {
		state = value;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long value) {
		timestamp = value;
	}

	public boolean getRemoved() {
		return removed;
	}

	public void setRemoved(boolean value) {
		removed = value;
	}

	public Table<K, ?> getTable() {
		return table;
	}

	public void setTable(Table<K, ?> value) {
		table = value;
	}

	public K getKey() {
		return key;
	}

	public void setKey(K value) {
		key = value;
	}

	public Bean getValue() {
		return value;
	}

	public void setValue(Bean value) {
		this.value = value;
	}

	public RootInfo createRootInfoIfNeed(TableKey tkey) {
		var cur = value != null ? value.rootInfo() : null;
		return cur != null ? cur : new RootInfo(this, tkey);
	}

	public void leaderApply(Transaction.RecordAccessed accessed) {
		if (accessed.getPutLog() != null)
			setValue(accessed.getPutLog().value);
		timestamp = getNextTimestamp(); // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。
	}

	public void flush(RocksDatabase.Batch batch) throws RocksDBException {
		var keyBB = ByteBuffer.Allocate();
		keyEncodeFunc.accept(keyBB, key);
		if (value != null) {
			int preAllocSize = value.preAllocSize();
			ByteBuffer valueBB = ByteBuffer.Allocate(Math.min(preAllocSize, 65536));
			value.encode(valueBB);
			int size = valueBB.WriteIndex;
			if (size > preAllocSize)
				value.preAllocSize(size);
			table.getRocksTable().put(batch, keyBB.Bytes, keyBB.WriteIndex, valueBB.Bytes, size);
		} else
			table.getRocksTable().delete(batch, keyBB.Bytes, keyBB.WriteIndex);
	}
}
