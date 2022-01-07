package Zeze.Transaction;

import Zeze.Serialize.*;
import Zeze.Services.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Services.GlobalCacheManager.*;

public class Record1<K extends Comparable<K>, V extends Bean> extends Record {
	private static final Logger logger = LogManager.getLogger(Record1.class);

	private final K Key;
	final K getKey() {
		return Key;
	}
	@Override
	public final Object getObjectKey() { return Key; }

	private final TableX<K, V> TTable;
	final TableX<K, V> getTTable() {
		return TTable;
	}

	@Override
	public Table getTable() {
		return getTTable();
	}

	@SuppressWarnings("unchecked")
	final V getValueTyped() {
		return (V)getValue();
	}

	public Record1(TableX<K, V> table, K key, V value) {
		super(value);
		this.TTable = table;
		this.Key = key;
	}

	@Override
	public String toString() {
		return Zeze.Util.Str.format("T {}:{} K {} S {} T {}",
				getTTable().getName(), getKey(), getState(), getTimestamp()); // V {Value}";
		// 记录的log可能在Transaction.AddRecordAccessed之前进行，不能再访问了。
	}

	@Override
	public Acquire Acquire(int state) {
		if (null == getTTable().TStorage) {
			Acquire acquire = new Acquire(null, state); // 不支持内存表cache同步。
			acquire.Result.State = state;
			return acquire;
		}

		var gkey = new GlobalTableKey(getTTable().getName(), getTTable().EncodeKey(getKey()));
		logger.debug("Acquire NewState={0} {1}", state, this);

		var stat = TableStatistics.getInstance().GetOrAdd(getTTable().getName());
		switch (state) {
			case GlobalCacheManagerServer.StateInvalid:
				stat.getGlobalAcquireInvalid().incrementAndGet();
				break;

			case GlobalCacheManagerServer.StateShare:
				stat.getGlobalAcquireShare().incrementAndGet();
				break;

			case GlobalCacheManagerServer.StateModify:
				stat.getGlobalAcquireModify().incrementAndGet();
				break;
		}

		return getTTable().getZeze().getGlobalAgent().Acquire(gkey, state);
	}

	private long SavedTimestampForCheckpointPeriod;
	final long getSavedTimestampForCheckpointPeriod() {
		return SavedTimestampForCheckpointPeriod;
	}
	final void setSavedTimestampForCheckpointPeriod(long value) {
		SavedTimestampForCheckpointPeriod = value;
	}
	private boolean ExistInBackDatabase;
	final void setExistInBackDatabase(boolean value) {
		ExistInBackDatabase = value;
	}
	private boolean ExistInBackDatabaseSavedForFlushRemove;

	@Override
	public void Commit(Zeze.Transaction.RecordAccessed accessed) {
		if (null != accessed.CommittedPutLog) {
			setValue(accessed.CommittedPutLog.getValue());
		}
		setTimestamp(getNextTimestamp()); // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。
		SetDirty();
	}

	@Override
	public void SetDirty() {
		switch (getTTable().getZeze().getCheckpoint().getCheckpointMode()) {
			case Period:
				setDirty(true);
				if (getTTable().TStorage != null) {
					getTTable().TStorage.OnRecordChanged(this);
				}
				break;
			case Table:
				setDirty(true);
				break;
			case Immediately:
				// do nothing
				break;
		}
	}

	private ByteBuffer snapshotKey;
	private ByteBuffer snapshotValue;

	public final boolean TryEncodeN(ConcurrentHashMap<K, Record1<K, V>> changed, ConcurrentHashMap<K, Record1<K, V>> encoded) {
		Lockey lockey = getTable().getZeze().getLocks().Get(new TableKey(getTTable().getName(), getKey()));
		if (!lockey.TryEnterReadLock(0)) {
			return false;
		}
		try {
			Encode0();
			encoded.put(getKey(), this);
			changed.remove(getKey());
			return true;
		}
		finally {
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
		snapshotKey = getTTable().EncodeKey(getKey());
		snapshotValue = getValue() != null ? getTTable().EncodeValue(getValueTyped()) : null;

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

	/*
	internal void Snapshot()
	{

	}
	*/

	@Override
	public void Flush(Database.Transaction t) {
		if (!getDirty())
			return;

		if (null != snapshotValue) {
			// changed
			if (getTTable().TStorage != null) {
				getTTable().TStorage.getDatabaseTable().Replace(t, snapshotKey, snapshotValue);
			}
		}
		else {
			// removed
			if (ExistInBackDatabaseSavedForFlushRemove) { // 优化，仅在后台db存在时才去删除。
				if (TTable.TStorage != null) {
					TTable.TStorage.getDatabaseTable().Remove(t, snapshotKey);
				}
			}

			// 需要同步删除OldTable，否则下一次查找又会找到。
			// 这个违背了OldTable不修改的原则，但没办法了。
			// XXX 从旧表中删除，使用独立临时事务。
			// 如果要纳入完整事务，有点麻烦。这里反正是个例外，那就再例外一次了。
			if (null != getTTable().getOldTable()) {
				try (var transTmp = getTTable().getOldTable().getDatabase().BeginTransaction()) {
					getTTable().getOldTable().Remove(transTmp, snapshotKey);
					transTmp.Commit();
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}
	}

	@Override
	public void Cleanup() {
		this.setDatabaseTransactionTmp(null);

		if (getTable().getZeze().getCheckpoint().getCheckpointMode() == CheckpointMode.Period) {
			TableKey tkey = new TableKey(getTable().getName(), getKey());
			Lockey lockey = getTable().getZeze().getLocks().Get(tkey);
			lockey.EnterWriteLock();
			try {
				if (getSavedTimestampForCheckpointPeriod() == super.getTimestamp()) {
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

	private volatile ConcurrentHashMap<K, Record1<K, V>> LruNode;
	public final ConcurrentHashMap<K, Record1<K, V>> getLruNode() {
		return LruNode;
	}
	public final void setLruNode(ConcurrentHashMap<K, Record1<K, V>> value) {
		LruNode = value;
	}
}