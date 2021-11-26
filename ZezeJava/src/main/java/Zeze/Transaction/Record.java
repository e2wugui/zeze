package Zeze.Transaction;

import Zeze.Services.GlobalCacheManager.Acquire;

import java.util.concurrent.locks.ReentrantLock;

public abstract class Record {
	public static class RootInfo {
		private Record Record;
		public final Record getRecord() {
			return Record;
		}
		private TableKey TableKey;
		public final TableKey getTableKey() {
			return TableKey;
		}

		public RootInfo(Record record, TableKey tableKey) {
			Record = record;
			TableKey = tableKey;
		}
	}

	public final RootInfo CreateRootInfoIfNeed(TableKey tkey) {
		var cur = getValue() == null ? null : getValue().RootInfo;
		if (null == cur) {
			cur = new RootInfo(this, tkey);
		}
		return cur;
	}

	private volatile long Timestamp;
	public final long getTimestamp() {
		return Timestamp;
	}
	public final void setTimestamp(long value) {
		Timestamp = value;
	}

	private final ReentrantLock FairLock = new ReentrantLock(true);

	public final void EnterFairLock() {
		FairLock.lock();
	}
	public final void ExitFairLock() {
		FairLock.unlock();
	}

	/**
	 Record.Dirty 的问题
	 对于新的CheckpointMode，需要实现新的Dirty。
	 CheckpointMode.Period
	 Snapshot时记住timestamp，Cleanup的时候ClearDirty(snapshot_timestamp)，需要记录锁。
	 CheckpointMode.Immediately
	 Commit完成以后马上进行不需要锁的ClearDirty. (实际实现为根本不修改Dirty)
	 CheckpointMode.Table
	 Flush(rrs): foreach (r in rrs) r.ClearDirty 不需要锁。
	*/
	private boolean Dirty = false;
	public final boolean getDirty() {
		return Dirty;
	}
	
	final void setDirty(boolean value) {
		Dirty = value;
	}

	private Bean Value;
	public final Bean getValue() {
		return Value;
	}
	public final void setValue(Bean value) {
		Value = value;
	}
	private int State;
	public final int getState() {
		return State;
	}
	public final void setState(int value) {
		State = value;
	}
	public long LastErrorGlobalSerialId;

	public abstract Table getTable();

	private RelativeRecordSet RelativeRecordSet = new RelativeRecordSet();
	public final RelativeRecordSet getRelativeRecordSet() {
		return RelativeRecordSet;
	}
	public final void setRelativeRecordSet(RelativeRecordSet value) {
		RelativeRecordSet = value;
	}

	public Record(Bean value) {
		setState(Zeze.Services.GlobalCacheManagerServer.StateInvalid);
		setValue(value);
		//Timestamp = NextTimestamp; // Table.FindInCacheOrStorage 可能发生数据变化，这里初始化一次不够。
	}

	// 时戳生成器，运行时状态，需要持久化时，再考虑保存到数据库。
	// 0 保留给不存在记录的的时戳。
	private static java.util.concurrent.atomic.AtomicLong _TimestampGen = new java.util.concurrent.atomic.AtomicLong();
	public static long getNextTimestamp() {
		return _TimestampGen.incrementAndGet();
	}

	public abstract void Commit(Zeze.Transaction.RecordAccessed accessed);

	public abstract Acquire Acquire(int state) throws Throwable;

	public abstract void Encode0();
	public abstract void Flush(Database.Transaction t);
	public abstract void Cleanup();

	private Database.Transaction DatabaseTransactionTmp;
	public final Database.Transaction getDatabaseTransactionTmp() {
		return DatabaseTransactionTmp;
	}
	public final void setDatabaseTransactionTmp(Database.Transaction value) {
		DatabaseTransactionTmp = value;
	}
	public abstract void SetDirty();
}