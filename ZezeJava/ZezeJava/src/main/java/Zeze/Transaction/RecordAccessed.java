package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RecordAccessed extends Bean {
	public static final class PutLog extends Log1<RecordAccessed, Bean> {
		public PutLog(RecordAccessed bean, Bean putValue, boolean removeWhileRollback) {
			super(bean, 0, putValue);
			if (removeWhileRollback) {
				Transaction.whileRollback(() -> {
					// 1. 目前这是memory表专用的。
					// 2. rollback的时候调用。
					// 3. 锁定方式，
					// a) 简单就是单个记录锁定，需要考虑死锁可能。rollback有可能持有锁。
					// b) rollback的时候，收集这种特别的记录，按commit方式锁定，允许多个表。
					//    这个复杂点，估计需要重构代码。
					// c) 无锁？
					// 4. 删除条件（AND）
					// a) timestamp 不变
					// b) value == null，这是保护性，限定条件，使得功能仅限于cache.remove，专用。
					//noinspection DataFlowIssue
					var lockey = Transaction.getCurrent().getLockey(bean.tableKey());
					lockey.enterReadLock();
					try {
						var tr = bean.atomicTupleRecord;
						var r = tr.record;
						if (r.getTimestamp() == tr.timestamp && r.getSoftValue() == null)
							r.removeFromTableCache();
					} finally {
						lockey.exitReadLock();
					}
				});
			}
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

	public RecordAccessed(@NotNull AtomicTupleRecord<?, ?> a) {
		atomicTupleRecord = a;
	}

	public @Nullable Bean newestValue() {
		//noinspection ConstantConditions
		var log = Transaction.getCurrent().getLog(objectId());
		return log instanceof PutLog ? ((PutLog)log).getValue() : atomicTupleRecord.strongRef;
	}

	public void put(Transaction current, Bean putValue, boolean removeWhileRollback) {
		current.putLog(new PutLog(this, putValue, removeWhileRollback));
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
