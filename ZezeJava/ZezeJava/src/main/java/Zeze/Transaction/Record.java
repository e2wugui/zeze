package Zeze.Transaction;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Record {
	public static final class RootInfo {
		private final Record Record;
		private final TableKey TableKey;

		public RootInfo(Record record, TableKey tableKey) {
			Record = record;
			TableKey = tableKey;
		}

		public Record getRecord() {
			return Record;
		}

		public TableKey getTableKey() {
			return TableKey;
		}
	}

	// 时戳生成器，运行时状态，需要持久化时，再考虑保存到数据库。
	// 0 保留给不存在记录的的时戳。
	private static final AtomicLong _TimestampGen = new AtomicLong(1);

	public static long getNextTimestamp() {
		return _TimestampGen.getAndIncrement();
	}

	private final ReentrantLock FairLock = new ReentrantLock(true);
	/**
	 * Record.Dirty 的问题
	 * 对于新的CheckpointMode，需要实现新的Dirty。
	 * CheckpointMode.Period
	 * Snapshot时记住timestamp，Cleanup的时候ClearDirty(snapshot_timestamp)，需要记录锁。
	 * CheckpointMode.Immediately
	 * Commit完成以后马上进行不需要锁的ClearDirty. (实际实现为根本不修改Dirty)
	 * CheckpointMode.Table
	 * Flush(rrs): foreach (r in rrs) r.ClearDirty 不需要锁。
	 */
	private boolean Dirty;
	protected volatile Bean StrongDirtyValue;

	private volatile long Timestamp;
	private volatile SoftReference<Bean> SoftValue;
	private volatile RelativeRecordSet RelativeRecordSet = new RelativeRecordSet();
	private volatile int State;

	// too many try
	private boolean fresh;
	private long acquireTime;

	private Database.Transaction DatabaseTransactionTmp;
	private Database.Transaction DatabaseTransactionOldTmp;

	public Record(Bean value) {
		State = Zeze.Services.GlobalCacheManagerServer.StateInvalid;
		SoftValue = new SoftReference<>(value);
		// Timestamp = NextTimestamp; // Table.FindInCacheOrStorage 可能发生数据变化，这里初始化一次不够。
	}

	final void EnterFairLock() {
		FairLock.lock();
	}

	final boolean TryEnterFairLock() {
		return FairLock.tryLock();
	}

	final boolean TryEnterFairLockWhenIdle() {
		if (FairLock.hasQueuedThreads())
			return false;
		return FairLock.tryLock();
	}

	final void ExitFairLock() {
		FairLock.unlock();
	}

	final boolean getDirty() {
		return Dirty;
	}

	final void setDirty(boolean value) {
		Dirty = value;
		StrongDirtyValue = value ? SoftValue.get() : null; // 脏数据在记录内保持一份强引用。
	}

	final long getTimestamp() {
		return Timestamp;
	}

	final void setTimestamp(long value) {
		Timestamp = value;
	}

	final Bean getSoftValue() {
		return SoftValue.get();
	}

	final void setSoftValue(Bean value) {
		SoftValue = new SoftReference<>(value);
	}

	final RelativeRecordSet getRelativeRecordSet() {
		return RelativeRecordSet;
	}

	final void setRelativeRecordSet(RelativeRecordSet value) {
		RelativeRecordSet = value;
	}

	final int getState() {
		return State;
	}

	final void setState(int value) {
		State = value;
	}

	final boolean isFresh() {
		return fresh;
	}

	final boolean isFreshAcquire() {
		return fresh && System.currentTimeMillis() - acquireTime < 1000;
	}

	final void setNotFresh() {
		fresh = false;
	}

	final void setFreshAcquire() {
		acquireTime = System.currentTimeMillis();
		fresh = true;
	}

	final Database.Transaction getDatabaseTransactionTmp() {
		return DatabaseTransactionTmp;
	}
	final Database.Transaction getDatabaseTransactionOldTmp() { return DatabaseTransactionOldTmp; }

	final void setDatabaseTransactionTmp(Database.Transaction value) {
		DatabaseTransactionTmp = value;
	}

	final void setDatabaseTransactionOldTmp(Database.Transaction value) {
		DatabaseTransactionOldTmp = value;
	}

	public final RootInfo CreateRootInfoIfNeed(TableKey tkey) {
		var strongRef = getSoftValue();
		var cur = strongRef != null ? strongRef.RootInfo : null;
		return cur != null ? cur : new RootInfo(this, tkey);
	}

	public abstract Table getTable();

	public abstract Object getObjectKey();

	public abstract void SetDirty();

	public abstract IGlobalAgent.AcquireResult Acquire(int state, boolean fresh);

	public abstract void Encode0();

	public abstract void Flush(Database.Transaction t, Database.Transaction lct);

	public abstract void Commit(RecordAccessed accessed);

	public abstract void Cleanup();
}
