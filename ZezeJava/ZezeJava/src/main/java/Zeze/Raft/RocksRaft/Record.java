package Zeze.Raft.RocksRaft;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;

public final class Record<K> {
	public static final class RootInfo {
		private final Record<?> Record;
		private final TableKey TableKey;

		public RootInfo(Record<?> record, TableKey tableKey) {
			Record = record;
			TableKey = tableKey;
		}

		public Record<?> getRecord() {
			return Record;
		}

		public TableKey getTableKey() {
			return TableKey;
		}
	}

	public static final int StateNew = 0;
	public static final int StateLoad = 1;
	private static final AtomicLong _TimestampGen = new AtomicLong();

	public static long getNextTimestamp() {
		return _TimestampGen.incrementAndGet();
	}

	private final BiConsumer<ByteBuffer, K> keyEncodeFunc;
	private int State = StateNew;
	private long Timestamp;
	private boolean Removed;
	private Table<K, ?> Table;
	private K Key;
	private Bean Value;

	public Record(Class<K> keyClass) {
		keyEncodeFunc = SerializeHelper.createEncodeFunc(keyClass);
	}

	public Record(BiConsumer<ByteBuffer, K> keyEncodeFunc) {
		this.keyEncodeFunc = keyEncodeFunc;
	}

	public int getState() {
		return State;
	}

	public void setState(int value) {
		State = value;
	}

	public long getTimestamp() {
		return Timestamp;
	}

	public void setTimestamp(long value) {
		Timestamp = value;
	}

	public boolean getRemoved() {
		return Removed;
	}

	public void setRemoved(boolean value) {
		Removed = value;
	}

	public Table<K, ?> getTable() {
		return Table;
	}

	public void setTable(Table<K, ?> value) {
		Table = value;
	}

	public K getKey() {
		return Key;
	}

	public void setKey(K value) {
		Key = value;
	}

	public Bean getValue() {
		return Value;
	}

	public void setValue(Bean value) {
		Value = value;
	}

	public RootInfo CreateRootInfoIfNeed(TableKey tkey) {
		var cur = Value != null ? Value.getRootInfo() : null;
		return cur != null ? cur : new RootInfo(this, tkey);
	}

	public void LeaderApply(Transaction.RecordAccessed accessed) {
		if (accessed.getPutLog() != null)
			setValue(accessed.getPutLog().Value);
		Timestamp = getNextTimestamp(); // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。
	}

	public void Flush(WriteBatch batch) throws RocksDBException {
		var keyBB = ByteBuffer.Allocate();
		keyEncodeFunc.accept(keyBB, Key);
		if (Value != null) {
			int preAllocSize = Value.getPreAllocSize();
			ByteBuffer valueBB = ByteBuffer.Allocate(Math.min(preAllocSize, 65536));
			Value.Encode(valueBB);
			int size = valueBB.WriteIndex;
			if (size > preAllocSize)
				Value.setPreAllocSize(size);
			batch.put(Table.getColumnFamily(), keyBB.Copy(), valueBB.Copy());
		} else
			batch.delete(Table.getColumnFamily(), keyBB.Copy());
	}
}
