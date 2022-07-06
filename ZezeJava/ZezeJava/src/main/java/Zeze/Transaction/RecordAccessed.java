package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;

public final class RecordAccessed extends Bean {
	public static final class PutLog extends Log1<RecordAccessed, Bean> {
		public PutLog(RecordAccessed bean, Bean putValue) {
			super(bean, 0, putValue);
		}

		@Override
		public long getLogKey() {
			return getBean().getObjectId();
		}

		@Override
		public void Commit() {
			((RecordAccessed)getBean()).CommittedPutLog = this; // 肯定最多只有一个 PutLog。由 LogKey 保证。
		}
	}

	final AtomicTupleRecord<?, ?> AtomicTupleRecord;
	boolean Dirty;
	PutLog CommittedPutLog; // Record 修改日志先提交到这里(Savepoint.Commit里面调用）。处理完Savepoint后再处理 Dirty 记录。

	public RecordAccessed(AtomicTupleRecord<?, ?> a) {
		AtomicTupleRecord = a;
	}

	public Bean NewestValue() {
		//noinspection ConstantConditions
		var log = Transaction.getCurrent().GetLog(getObjectId());
		return log instanceof PutLog ? ((PutLog)log).getValue() : AtomicTupleRecord.StrongRef;
	}

	public void Put(Transaction current, Bean putValue) {
		current.PutLog(new PutLog(this, putValue));
	}

	public void Remove(Transaction current) {
		Put(current, null);
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	public void Encode(ByteBuffer bb) {
	}

	@Override
	public void Decode(ByteBuffer bb) {
	}
}
