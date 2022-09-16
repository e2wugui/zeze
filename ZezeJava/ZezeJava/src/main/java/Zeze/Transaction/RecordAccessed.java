package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;

public final class RecordAccessed extends Bean {
	public static final class PutLog extends Log1<RecordAccessed, Bean> {
		public PutLog(RecordAccessed bean, Bean putValue) {
			super(bean, 0, putValue);
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

	final AtomicTupleRecord<?, ?> atomicTupleRecord;
	boolean dirty;
	PutLog committedPutLog; // Record 修改日志先提交到这里(Savepoint.Commit里面调用）。处理完Savepoint后再处理 Dirty 记录。

	public RecordAccessed(AtomicTupleRecord<?, ?> a) {
		atomicTupleRecord = a;
	}

	public Bean newestValue() {
		//noinspection ConstantConditions
		var log = Transaction.getCurrent().getLog(objectId());
		return log instanceof PutLog ? ((PutLog)log).getValue() : atomicTupleRecord.strongRef;
	}

	public void put(Transaction current, Bean putValue) {
		current.putLog(new PutLog(this, putValue));
	}

	public void remove(Transaction current) {
		put(current, null);
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	protected void resetChildrenRootInfo() {

	}

	@Override
	public void encode(ByteBuffer bb) {
	}

	@Override
	public void decode(ByteBuffer bb) {
	}
}
