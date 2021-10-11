package Zeze.Transaction;

import Zeze.Serialize.*;
import Zeze.Services.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.ConcurrentHashMap;

public class Record1<K, V extends Bean> extends Record {
	private static final Logger logger = LogManager.getLogger(Record1.class);

	private K Key;
	public final K getKey() {
		return Key;
	}

	private Table1<K, V> TTable;
	public final Table1<K, V> getTTable() {
		return TTable;
	}

	@Override
	public Table getTable() {
		return getTTable();
	}

	@SuppressWarnings("unchecked")
	public final V getValueTyped() {
		return (V)getValue();
	}

	public Record1(Table1<K, V> table, K key, V value) {
		super(value);
		this.TTable = table;
		this.Key = key;
	}

	@Override
	public String toString() {
		return String.format("T %1$s:%2$s K %3$s S %4$s T %5$s", getTTable().getId(), getTTable().getName(), getKey(), getState(), getTimestamp()); // V {Value}";
		// 记录的log可能在Transaction.AddRecordAccessed之前进行，不能再访问了。
	}

	@Override
	public int Acquire(int state) {
		if (null == getTTable().TStorage) {
			return state; // 不支持内存表cache同步。
		}

		GlobalCacheManager.GlobalTableKey gkey = new GlobalCacheManager.GlobalTableKey(getTTable().getName(), getTTable().EncodeKey(getKey()));
		logger.debug("Acquire NewState={0} {1}", state, this);

		var stat = TableStatistics.getInstance().GetOrAdd(getTTable().getId());
		switch (state) {
			case GlobalCacheManager.StateInvalid:
				stat.getGlobalAcquireInvalid().incrementAndGet();
				break;

			case GlobalCacheManager.StateShare:
				stat.getGlobalAcquireShare().incrementAndGet();
				break;

			case GlobalCacheManager.StateModify:
				stat.getGlobalAcquireModify().incrementAndGet();
				break;
		}

		return getTTable().getZeze().getGlobalAgent().Acquire(gkey, state);
	}

	private long SavedTimestampForCheckpointPeriod;
	public final long getSavedTimestampForCheckpointPeriod() {
		return SavedTimestampForCheckpointPeriod;
	}
	public final void setSavedTimestampForCheckpointPeriod(long value) {
		SavedTimestampForCheckpointPeriod = value;
	}
	private boolean ExistInBackDatabase;
	public final boolean getExistInBackDatabase() {
		return ExistInBackDatabase;
	}
	public final void setExistInBackDatabase(boolean value) {
		ExistInBackDatabase = value;
	}

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
		Lockey lockey = Locks.getInstance().Get(new TableKey(getTTable().getId(), getKey()));
		if (false == lockey.TryEnterReadLock(0)) {
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
		snapshotKey = getTTable().EncodeKey(getKey());
		snapshotValue = getValue() != null ? getTTable().EncodeValue(getValueTyped()) : null;
	}

	/*
	internal void Snapshot()
	{

	}
	*/

	@Override
	public boolean Flush(Database.Transaction t) {
		if (null != snapshotValue) {
			// changed
			if (getTTable().TStorage != null) {
				getTTable().TStorage.getDatabaseTable().Replace(t, snapshotKey, snapshotValue);
			}
		}
		else {
			// removed
			if (getExistInBackDatabase()) { // 优化，仅在后台db存在时才去删除。
				if (getTTable().TStorage != null) {
					getTTable().TStorage.getDatabaseTable().Remove(t, snapshotKey);
				}
			}

			// 需要同步删除OldTable，否则下一次查找又会找到。
			// 这个违背了OldTable不修改的原则，但没办法了。
			// XXX 从旧表中删除，使用独立临时事务。
			// 如果要纳入完整事务，有点麻烦。这里反正是个例外，那就再例外一次了。
			if (null != getTTable().getOldTable()) {
				var transTmp = getTTable().getOldTable().getDatabase().BeginTransaction();
				getTTable().getOldTable().Remove(transTmp, snapshotKey);
				transTmp.Commit();
			}
		}
		return true;
	}

	@Override
	public void Cleanup() {
		this.setDatabaseTransactionTmp(null);

		TableKey tkey = new TableKey(getTable().getId(), getKey());
		Lockey lockey = Locks.getInstance().Get(tkey);
		lockey.EnterWriteLock();
		try {
			if (getSavedTimestampForCheckpointPeriod() == super.getTimestamp()) {
				setDirty(false);
			}

			// ExistInBackDatabase = null != snapshotValue;
			// 修改很少，下面这样会更快？
			if (null != snapshotValue) {
				// replace
				if (false == getExistInBackDatabase()) {
					setExistInBackDatabase(true);
				}
			}
			else {
				// remove
				if (getExistInBackDatabase()) {
					setExistInBackDatabase(false);
				}
			}
		}
		finally {
			lockey.ExitWriteLock();
		}

		snapshotKey = null;
		snapshotValue = null;
	}

	private Zeze.Util.HugeConcurrentDictionary<K, Record1<K, V>> LruNode;
	public final Zeze.Util.HugeConcurrentDictionary<K, Record1<K, V>> getLruNode() {
		return LruNode;
	}
	public final void setLruNode(Zeze.Util.HugeConcurrentDictionary<K, Record1<K, V>> value) {
		LruNode = value;
	}
}