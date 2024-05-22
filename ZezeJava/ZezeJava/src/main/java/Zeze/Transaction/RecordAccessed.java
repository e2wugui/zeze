package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RecordAccessed extends Bean {
	static final class PutLog extends LogSpecial<RecordAccessed, Bean> {
		PutLog(@NotNull RecordAccessed bean, @Nullable Bean putValue) {
			super(bean, 0, putValue);
		}

		@Override
		public int getTypeId() {
			return 0; // 现在Log1仅用于特殊目的，不支持相关日志系列化。
		}

		@Override
		public long getLogKey() {
			return getBean().objectId();
		}

		@Override
		public void commit() {
			((RecordAccessed)getBean()).committedPutLog = this; // 肯定最多只有一个 PutLog。由 LogKey 保证。
		}
	}

	final @NotNull AtomicTupleRecord<?, ?> atomicTupleRecord;
	boolean dirty;
	PutLog committedPutLog; // Record 修改日志先提交到这里(Savepoint.Commit里面调用）。处理完Savepoint后再处理 Dirty 记录。
	TableCache<?, ?> tableCache;

	public RecordAccessed(@NotNull AtomicTupleRecord<?, ?> a, @NotNull TableCache<?, ?> tableCache) {
		atomicTupleRecord = a;
		this.tableCache = tableCache;
	}

	public @Nullable Bean newestValue() {
		//noinspection ConstantConditions
		var log = Transaction.getCurrent().getLog(objectId());
		return log instanceof PutLog ? ((PutLog)log).getValue() : atomicTupleRecord.strongRef;
	}

	public void put(@NotNull Transaction current, @Nullable Bean putValue) {
		current.putLog(new PutLog(this, putValue));
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteByte(0);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		bb.SkipUnknownField(ByteBuffer.BEAN);
	}
}
