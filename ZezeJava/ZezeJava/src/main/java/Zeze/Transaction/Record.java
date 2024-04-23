package Zeze.Transaction;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Services.GlobalCacheManagerConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Record extends ReentrantLock {
	public static final class RootInfo {
		private final @NotNull Record record;
		private final @NotNull TableKey tableKey;

		public RootInfo(@NotNull Record record, @NotNull TableKey tableKey) {
			this.record = record;
			this.tableKey = tableKey;
		}

		public @NotNull Record getRecord() {
			return record;
		}

		public @NotNull TableKey getTableKey() {
			return tableKey;
		}
	}

	// 时戳生成器，运行时状态，需要持久化时，再考虑保存到数据库。
	// 0 保留给不存在记录的的时戳。
	private static final AtomicLong timestampGen = new AtomicLong(1);

	public static long getNextTimestamp() {
		return timestampGen.getAndIncrement();
	}

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
	protected volatile @Nullable Bean strongDirtyValue;

	private volatile long timestamp;
	private volatile @NotNull SoftReference<Bean> softValue;
	private volatile @NotNull RelativeRecordSet relativeRecordSet = new RelativeRecordSet();
	private volatile int state;

	// too many try
	private boolean fresh;
	private long acquireTime;

	private Database.Transaction databaseTransactionTmp;
	private Database.Transaction databaseTransactionOldTmp;

	public Record(@Nullable Bean value) {
		super(true);
		state = GlobalCacheManagerConst.StateInvalid;
		softValue = new SoftReference<>(value);
		// Timestamp = NextTimestamp; // Table.FindInCacheOrStorage 可能发生数据变化，这里初始化一次不够。
	}

	final void enterFairLock() {
		if (tryLock())
			return;
		var r = (Record1<?, ?>)this;
		try (var ignored = Profiler.begin("RecordWaitFairLock", r.getTable().getName(), r.getObjectKey())) {
			lock();
		}
	}

	final boolean tryEnterFairLock() {
		return tryLock();
	}

	final boolean tryEnterFairLockWhenIdle() {
		return !hasQueuedThreads() && tryLock();
	}

	final void exitFairLock() {
		unlock();
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

	final @Nullable Bean getSoftValue() {
		return softValue.get();
	}

	final void setSoftValue(@Nullable Bean value) {
		softValue = new SoftReference<>(value);
	}

	final @NotNull RelativeRecordSet getRelativeRecordSet() {
		return relativeRecordSet;
	}

	final void setRelativeRecordSet(@NotNull RelativeRecordSet value) {
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

	public final @NotNull RootInfo createRootInfoIfNeed(@NotNull TableKey tkey) {
		var strongRef = getSoftValue();
		var cur = strongRef != null ? strongRef.rootInfo : null;
		return cur != null ? cur : new RootInfo(this, tkey);
	}

	public abstract @NotNull Table getTable();

	public abstract @NotNull Object getObjectKey();

	public abstract void setDirty();

	public abstract @Nullable IGlobalAgent.AcquireResult acquire(int state, boolean fresh, boolean noWait);

	public abstract void encode0();

	public abstract void flush(@NotNull Database.Transaction t, @Nullable Database.Transaction lct);

	public abstract void commit(@NotNull RecordAccessed accessed);

	public abstract void cleanup();
}
