package Zeze.Raft.RocksRaft;

import java.util.concurrent.atomic.AtomicLong;
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
	private Bean value;
	final FastLock mutex = new FastLock();

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
