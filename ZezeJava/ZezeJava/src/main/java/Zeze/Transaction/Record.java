package Zeze.Transaction;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Services.GlobalCacheManagerConst;

public abstract class Record {
	public static final class RootInfo {
		private final Record record;
		private final TableKey tableKey;

		public RootInfo(Record record, TableKey tableKey) {
			this.record = record;
			this.tableKey = tableKey;
		}

		public Record getRecord() {
			return record;
		}

		public TableKey getTableKey() {
			return tableKey;
		}
	}

	// 时戳生成器，运行时状态，需要持久化时，再考虑保存到数据库。
	// 0 保留给不存在记录的的时戳。
	private static final AtomicLong timestampGen = new AtomicLong(1);

	public static long getNextTimestamp() {
		return timestampGen.getAndIncrement();
	}

	private final ReentrantLock fairLock = new ReentrantLock(true);
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
	private boolean dirty;
	protected volatile Bean strongDirtyValue;

	private volatile long timestamp;
	private volatile SoftReference<Bean> softValue;
	private volatile RelativeRecordSet relativeRecordSet = new RelativeRecordSet();
	private volatile int state;

	// too many try
	private boolean fresh;
	private long acquireTime;

	private Database.Transaction databaseTransactionTmp;
	private Database.Transaction databaseTransactionOldTmp;

	public Record(Bean value) {
		state = GlobalCacheManagerConst.StateInvalid;
		softValue = new SoftReference<>(value);
		// Timestamp = NextTimestamp; // Table.FindInCacheOrStorage 可能发生数据变化，这里初始化一次不够。
	}

	final void enterFairLock() {
		fairLock.lock();
	}

	final boolean tryEnterFairLock() {
		return fairLock.tryLock();
	}

	final boolean tryEnterFairLockWhenIdle() {
		if (fairLock.hasQueuedThreads())
			return false;
		return fairLock.tryLock();
	}

	final void exitFairLock() {
		fairLock.unlock();
	}

	final boolean getDirty() {
		return dirty;
	}

	final void setDirty(boolean value) {
		dirty = value;
		strongDirtyValue = value ? softValue.get() : null; // 脏数据在记录内保持一份强引用。
	}

	final long getTimestamp() {
		return timestamp;
	}

	final void setTimestamp(long value) {
		timestamp = value;
	}

	final Bean getSoftValue() {
		return softValue.get();
	}

	final void setSoftValue(Bean value) {
		softValue = new SoftReference<>(value);
	}

	final RelativeRecordSet getRelativeRecordSet() {
		return relativeRecordSet;
	}

	final void setRelativeRecordSet(RelativeRecordSet value) {
		relativeRecordSet = value;
	}

	final int getState() {
		return state;
	}

	final void setState(int value) {
		state = value;
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
		return databaseTransactionTmp;
	}

	final Database.Transaction getDatabaseTransactionOldTmp() {
		return databaseTransactionOldTmp;
	}

	final void setDatabaseTransactionTmp(Database.Transaction value) {
		databaseTransactionTmp = value;
	}

	final void setDatabaseTransactionOldTmp(Database.Transaction value) {
		databaseTransactionOldTmp = value;
	}

	public final RootInfo createRootInfoIfNeed(TableKey tkey) {
		var strongRef = getSoftValue();
		var cur = strongRef != null ? strongRef.rootInfo : null;
		return cur != null ? cur : new RootInfo(this, tkey);
	}

	public abstract Table getTable();

	public abstract Object getObjectKey();

	public abstract void setDirty();

	public abstract IGlobalAgent.AcquireResult acquire(int state, boolean fresh, boolean noWait);

	public abstract void encode0();

	public abstract void flush(Database.Transaction t, Database.Transaction lct);

	public abstract void commit(RecordAccessed accessed);

	public abstract void cleanup();
}
