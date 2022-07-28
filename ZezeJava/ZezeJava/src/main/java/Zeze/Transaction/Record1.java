package Zeze.Transaction;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.Macro;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Record1<K extends Comparable<K>, V extends Bean> extends Record {
	private static final Logger logger = LogManager.getLogger(Record1.class);
	private static final boolean isDebugEnabled = logger.isDebugEnabled();
	private static final VarHandle LRU_NODE_HANDLE;

	static {
		try {
			LRU_NODE_HANDLE = MethodHandles.lookup().findVarHandle(Record1.class, "LruNode", ConcurrentHashMap.class);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private final TableX<K, V> TTable;
	private final K Key;
	private ByteBuffer snapshotKey;
	private ByteBuffer snapshotValue;
	private long SavedTimestampForCheckpointPeriod;
	private boolean ExistInBackDatabase;
	private boolean ExistInBackDatabaseSavedForFlushRemove;
	private volatile ConcurrentHashMap<K, Record1<K, V>> LruNode;

	public Record1(TableX<K, V> table, K key, V value) {
		super(value);
		TTable = table;
		Key = key;
	}

	@Override
	public Table getTable() {
		return TTable;
	}

	@Override
	public K getObjectKey() {
		return Key;
	}

	void setSavedTimestampForCheckpointPeriod(long value) {
		SavedTimestampForCheckpointPeriod = value;
	}

	void setExistInBackDatabase(boolean value) {
		ExistInBackDatabase = value;
	}

	ConcurrentHashMap<K, Record1<K, V>> getLruNode() {
		return LruNode;
	}

	void setLruNode(ConcurrentHashMap<K, Record1<K, V>> value) {
		LruNode = value;
	}

	@SuppressWarnings("unchecked")
	public ConcurrentHashMap<K, Record1<K, V>> getAndSetLruNodeNull() {
		return (ConcurrentHashMap<K, Record1<K, V>>)LRU_NODE_HANDLE.getAndSet(this, null);
	}

	public boolean compareAndSetLruNodeNull(ConcurrentHashMap<K, Record1<K, V>> c) {
		return LRU_NODE_HANDLE.compareAndSet(this, c, null);
	}

	@Override
	public String toString() {
		return String.format("T=%s K=%s S=%d T=%d", TTable.getName(), Key, getState(), getTimestamp()); // V {Value}";
		// 记录的log可能在Transaction.AddRecordAccessed之前进行，不能再访问了。
	}

	@Override
	public IGlobalAgent.AcquireResult Acquire(int state, boolean fresh, boolean noWait) {
		IGlobalAgent agent;
		if (TTable.GetStorage() == null || (agent = TTable.getZeze().getGlobalAgent()) == null) // 不支持内存表cache同步。
			return IGlobalAgent.AcquireResult.getSuccessResult(state);

		if (isDebugEnabled)
			logger.debug("Acquire NewState={} {}", state, this);
		if (Macro.EnableStatistics) {
			var stat = TableStatistics.getInstance().GetOrAdd(TTable.getId());
			switch (state) {
			case GlobalCacheManagerConst.StateInvalid:
				stat.getGlobalAcquireInvalid().increment();
				break;

			case GlobalCacheManagerConst.StateShare:
				stat.getGlobalAcquireShare().increment();
				break;

			case GlobalCacheManagerConst.StateModify:
				stat.getGlobalAcquireModify().increment();
				break;
			}
		}
		return agent.Acquire(TTable.EncodeGlobalKey(Key), state, fresh, noWait);
	}

	@Override
	public void Commit(Zeze.Transaction.RecordAccessed accessed) {
		if (null != accessed.CommittedPutLog) {
			setSoftValue(accessed.CommittedPutLog.getValue());
		}
		setTimestamp(getNextTimestamp()); // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。
		SetDirty();
		//System.out.println("commit: " + this + " put=" + accessed.CommittedPutLog + " atr=" + accessed.AtomicTupleRecord);
	}

	@Override
	public void SetDirty() {
		switch (TTable.getZeze().getConfig().getCheckpointMode()) {
		case Period:
			setDirty(true);
			if (TTable.GetStorage() != null) {
				TTable.GetStorage().OnRecordChanged(this);
			}
			break;
		case Table:
			setDirty(true);
			break;
		case Immediately:
			// 立即模式需要马上保存到RocksCache中。在下面两个地方保存：
			// 1. 在public void Flush(Iterable<Record> rs)流程中直接保存。
			// 2. TableX.Load。
			break;
		}
	}

	public boolean TryEncodeN(ConcurrentHashMap<K, Record1<K, V>> changed, ConcurrentHashMap<K, Record1<K, V>> encoded) {
		Lockey lockey = TTable.getZeze().getLocks().Get(new TableKey(TTable.getId(), Key));
		if (!lockey.TryEnterReadLock(0)) {
			return false;
		}
		try {
			Encode0();
			encoded.put(Key, this);
			changed.remove(Key);
			return true;
		} finally {
			lockey.ExitReadLock();
		}
	}

	@Override
	public void Encode0() {
		if (!getDirty())
			return;
		// Under Lock：this.TryEncodeN & Storage.Snapshot

		// 【注意】可能保存多次：TryEncodeN 记录读锁；Snapshot FlushWriteLock;
		// 从 Storage.Snapshot 里面修改移到这里，避免Snapshot遍历，减少FlushWriteLock时间。
		SavedTimestampForCheckpointPeriod = getTimestamp();

		// 可能编码多次：TryEncodeN 记录读锁；Snapshot FlushWriteLock;
		snapshotKey = TTable.EncodeKey(Key);
		snapshotValue = StrongDirtyValue != null ? ByteBuffer.Encode(StrongDirtyValue) : null;

		// 【注意】
		// 这个标志本来应该在真正写到Database之后修改才是最合适的；
		// 但这样需要再次锁定记录写锁，并发效率比较低，增加Flush时间；
		// 由于Encode0()之后肯定会进行写Database操作，而写Database是不会并发的，
		// ExistInBackDatabase也仅在写Database操作使用，所以提前到这里修改；
		// 【并发简单分析】
		// 1) FindInCacheOrStorage
		//    第一次装载时，只会装载一次，记录读锁+lock(record)；
		// 2.1) CheckpointMode.Period
		//    a) TryEncodeN 记录读锁，看起来这个锁定是不够的，
		//       但是由于记录在TableCache中存在时，不会引起再次装载，
		//       所以实际上不会和FindInCacheOrStorage并发;
		//    b) Snapshot FlushWriteLock
		//       此时世界都暂停了，改一点状态完全没问题。
		// 2.2) CheckpointMode.Table
		//    rrs.lock()，使得Encode0()不会并发，其他理由同上面2.1)a)，
		//    这种模式也是可以直接修改的。
		//【ExistInBackDatabaseSavedForFlushRemove】
		//    由于这里提前修改，所以需要保存一个副本后面写Database时用。
		//    see this.Flush
		ExistInBackDatabaseSavedForFlushRemove = ExistInBackDatabase;
		ExistInBackDatabase = null != snapshotValue;
	}

	public void Flush(Database.Transaction t, HashMap<Database, Database.Transaction> tss, Database.Transaction lct) {
		if (null != TTable.getOldTable()) {
			// will clear in Cleanup.
			setDatabaseTransactionOldTmp(tss.get(TTable.getOldTable().getDatabase()));
		}
		Flush(t, lct);
	}

	@Override
	public void Flush(Database.Transaction t, Database.Transaction lct) {
		if (!getDirty())
			return;

		if (null != snapshotValue) {
			// changed
			if (TTable.GetStorage() != null) {
				TTable.GetStorage().getDatabaseTable().Replace(t, snapshotKey, snapshotValue);
			}
			if (null != lct) {
				TTable.getLocalRocksCacheTable().Replace(lct, snapshotKey, snapshotValue);
			}
		} else {
			// removed
			if (ExistInBackDatabaseSavedForFlushRemove) { // 优化，仅在后台db存在时才去删除。
				if (TTable.GetStorage() != null) {
					TTable.GetStorage().getDatabaseTable().Remove(t, snapshotKey);
				}
				if (null != lct) {
					TTable.getLocalRocksCacheTable().Remove(lct, snapshotKey);
				}
			}

			// 需要同步删除OldTable，否则下一次查找又会找到。
			// 这个违背了OldTable不修改的原则，但没办法了。
			if (null != getDatabaseTransactionOldTmp()) {
				TTable.getOldTable().Remove(getDatabaseTransactionOldTmp(), snapshotKey);
			}
		}
	}

	@Override
	public void Cleanup() {
		setDatabaseTransactionTmp(null);
		setDatabaseTransactionOldTmp(null);

		if (TTable.getZeze().getConfig().getCheckpointMode() == CheckpointMode.Period) {
			TableKey tkey = new TableKey(TTable.getId(), Key);
			Lockey lockey = TTable.getZeze().getLocks().Get(tkey);
			lockey.EnterWriteLock();
			try {
				if (SavedTimestampForCheckpointPeriod == getTimestamp()) {
					setDirty(false);
				}
				snapshotKey = null;
				snapshotValue = null;
				return;
			} finally {
				lockey.ExitWriteLock();
			}
		}
		// CheckpointMode.Table
		snapshotKey = null;
		snapshotValue = null;
	}
}
