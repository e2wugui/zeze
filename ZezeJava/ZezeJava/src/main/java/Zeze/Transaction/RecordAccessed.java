package Zeze.Transaction;

import java.util.Objects;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

/** 事务内对某记录的访问登记：包装原子快照，持有 dirty 标记与 PutLog（put/remove 语义）。 */
public final class RecordAccessed extends Bean {
	static final class PutLog extends LogSpecial<RecordAccessed, Bean> {
		PutLog(RecordAccessed bean, @Nullable Bean putValue) {
			super(bean, 0, putValue);
		}

		@Override
		public int getTypeId() {
			return 0; // 现在Log1仅用于特殊目的，不支持相关日志系列化。
		}

		@Override
		public long getLogKey() {
			return Objects.requireNonNull(getBelong()).objectId();
		}

		@Override
		public void commit() {
			((RecordAccessed)Objects.requireNonNull(getBelong())).committedPutLog = this; // 肯定最多只有一个 PutLog。由 LogKey 保证。
		}
	}

	final AtomicTupleRecord<?, ?> atomicTupleRecord;
	volatile boolean dirty;
	@Nullable PutLog committedPutLog; // Record 修改日志先提交到这里(Savepoint.Commit里面调用）。处理完Savepoint后再处理 Dirty 记录。

	public RecordAccessed(AtomicTupleRecord<?, ?> a) {
		atomicTupleRecord = a;
	}

	public @Nullable Bean newestValue() {
		//noinspection ConstantConditions
		var log = Transaction.getCurrent().getLog(objectId());
		return log instanceof PutLog ? ((PutLog)log).getValue() : atomicTupleRecord.strongRef();
	}

	public void put(Transaction current, @Nullable Bean putValue) {
		current.putLog(new PutLog(this, putValue));
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteByte(0);
	}

	@Override
	public void decode(IByteBuffer bb) {
		bb.SkipUnknownField(ByteBuffer.BEAN);
	}
}
