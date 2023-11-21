package Zeze.Transaction;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.PerfCounter;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Record1<K extends Comparable<K>, V extends Bean> extends Record {
	private static final Logger logger = LogManager.getLogger(Record1.class);
	private static final boolean isTraceEnabled = logger.isTraceEnabled();
	private static final @NotNull VarHandle LRU_NODE_HANDLE;

	static {
		try {
			LRU_NODE_HANDLE = MethodHandles.lookup().findVarHandle(Record1.class, "lruNode", ConcurrentHashMap.class);
		} catch (ReflectiveOperationException e) {
			Task.forceThrow(e);
			throw new AssertionError(); // never run here
		}
	}

	private final @NotNull TableX<K, V> table;
	private final @NotNull K key;
	private Object snapshotKey;
	private Object snapshotValue;
	private Object snapshotKeyLocal;
	private Object snapshotValueLocal;
	private long savedTimestampForCheckpointPeriod;
	private boolean existInBackDatabase;
	private boolean existInBackDatabaseSavedForFlushRemove;
	private volatile ConcurrentHashMap<K, Record1<K, V>> lruNode;

	public Record1(@NotNull TableX<K, V> table, @NotNull K key, @Nullable V value) {
		super(value);
		this.table = table;
		this.key = key;
	}

	@Override
	public @NotNull TableX<K, V> getTable() {
		return table;
	}

	@Override
	public @NotNull K getObjectKey() {
		return key;
	}

	void setExistInBackDatabase(boolean value) {
		existInBackDatabase = value;
	}

	ConcurrentHashMap<K, Record1<K, V>> getLruNode() {
		return lruNode;
	}

	void setLruNode(ConcurrentHashMap<K, Record1<K, V>> value) {
		lruNode = value;
	}

	@SuppressWarnings("unchecked")
	ConcurrentHashMap<K, Record1<K, V>> getAndSetLruNodeNull() {
		return (ConcurrentHashMap<K, Record1<K, V>>)LRU_NODE_HANDLE.getAndSet(this, null);
	}

	boolean compareAndSetLruNodeNull(ConcurrentHashMap<K, Record1<K, V>> c) {
		return LRU_NODE_HANDLE.compareAndSet(this, c, null);
	}

	void removeFromTableCache() {
		table.getCache().remove(key, this);
	}

	@Override
	public String toString() {
		return String.format("T=%s K=%s S=%d T=%d", table.getName(), key, getState(), getTimestamp()); // V {Value}";
		// 记录的log可能在Transaction.AddRecordAccessed之前进行，不能再访问了。
	}

	@Override
	public IGlobalAgent.AcquireResult acquire(int state, boolean fresh, boolean noWait) {
		IGlobalAgent agent;
		if (table.getStorage() == null || (agent = table.getZeze().getGlobalAgent()) == null) // 不支持内存表cache同步。
			return IGlobalAgent.AcquireResult.getSuccessResult(state);

		if (isTraceEnabled)
			logger.trace("Acquire NewState={} {}", state, this);
		var tInfo = PerfCounter.instance.getOrAddTableInfo(table.getId());
		switch (state) {
		case GlobalCacheManagerConst.StateInvalid:
			tInfo.acquireInvalid.increment();
			break;
		case GlobalCacheManagerConst.StateShare:
			tInfo.acquireShare.increment();
			break;
		case GlobalCacheManagerConst.StateModify:
			tInfo.acquireModify.increment();
			break;
		}
		return agent.acquire(table.encodeGlobalKey(key), state, fresh, noWait);
	}

	@Override
	public void commit(RecordAccessed accessed) {
		if (null != accessed.committedPutLog) {
			setSoftValue(accessed.committedPutLog.getValue());
			if (table.isMemory() && accessed.committedPutLog.getValue() == null) {
				// 记录删除并且是内存表，马上删除。
				table.getCache().remove(key, this);
				return; // 内存表已经删除，done
			}
		}
		setTimestamp(getNextTimestamp()); // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。
		setDirty();
		//System.out.println("commit: " + this + " put=" + accessed.CommittedPutLog + " atr=" + accessed.AtomicTupleRecord);
	}

	@Override
	public void setDirty() {
		if (table.getStorage() == null)
			return; // 内存表永远不设置脏，让SoftReference能工作。

		switch (table.getZeze().getConfig().getCheckpointMode()) {
		case Period:
			setDirty(true);
			if (table.getStorage() != null) {
				table.getStorage().onRecordChanged(this);
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

	boolean tryEncodeN(ConcurrentHashMap<K, Record1<K, V>> changed, ConcurrentHashMap<K, Record1<K, V>> encoded) {
		Lockey lockey = table.getZeze().getLocks().get(new TableKey(table.getId(), key));
		if (!lockey.tryEnterReadLock(0)) {
			return false;
		}
		try {
			encode0();
			encoded.put(key, this);
			changed.remove(key);
			return true;
		} finally {
			lockey.exitReadLock();
		}
	}

	@Override
	public void encode0() {
		if (!getDirty())
			return;
		// Under Lock：this.TryEncodeN & Storage.Snapshot

		// 【注意】可能保存多次：TryEncodeN 记录读锁；Snapshot FlushWriteLock;
		// 从 Storage.Snapshot 里面修改移到这里，避免Snapshot遍历，减少FlushWriteLock时间。
		savedTimestampForCheckpointPeriod = getTimestamp();

		// 可能编码多次：TryEncodeN 记录读锁；Snapshot FlushWriteLock;
		if (table.isUseRelationalMapping()) {
			var sqlKey = new SQLStatement();
			table.encodeKeySQLStatement(sqlKey, key);
			if (strongDirtyValue == null) {
				snapshotValue = null;
			} else {
				var sqlValue = new SQLStatement();
				var parents = new ArrayList<String>();
				strongDirtyValue.encodeSQLStatement(parents, sqlValue);
				snapshotValue = sqlValue;
			}
			snapshotKey = sqlKey;

			// 编码用来存储到本地rocksdb的cache中。
			snapshotKeyLocal = table.encodeKey(key);
			snapshotValueLocal = strongDirtyValue != null ? ByteBuffer.encode(strongDirtyValue) : null;
		} else {
			// KV表，本地Cache和远程一样。
			snapshotKeyLocal = snapshotKey = table.encodeKey(key);
			snapshotValueLocal = snapshotValue = strongDirtyValue != null ? ByteBuffer.encode(strongDirtyValue) : null;
		}
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
		existInBackDatabaseSavedForFlushRemove = existInBackDatabase;
		existInBackDatabase = null != snapshotValue;
	}

	void flush(Database.Transaction t, HashMap<Database, Database.Transaction> tss, Database.Transaction lct) {
		if (null != table.getOldTable()) {
			// will clear in Cleanup.
			setDatabaseTransactionOldTmp(tss.get(table.getOldTable().getDatabase()));
		}
		flush(t, lct);
	}

	@Override
	public void flush(Database.Transaction t, Database.Transaction lct) {
		if (!getDirty())
			return;

		if (null != snapshotValue) {
			// changed
			if (table.getStorage() != null) {
				table.getStorage().getDatabaseTable().replace(t, snapshotKey, snapshotValue);
			}
			if (null != lct) {
				table.getLocalRocksCacheTable().replace(lct, snapshotKeyLocal, snapshotValueLocal);
			}
		} else {
			// removed
			if (existInBackDatabaseSavedForFlushRemove) { // 优化，仅在后台db存在时才去删除。
				if (table.getStorage() != null) {
					table.getStorage().getDatabaseTable().remove(t, snapshotKey);
				}
				if (null != lct) {
					table.getLocalRocksCacheTable().remove(lct, snapshotKeyLocal);
				}
			}

			// 需要同步删除OldTable，否则下一次查找又会找到。
			// 这个违背了OldTable不修改的原则，但没办法了。
			if (null != getDatabaseTransactionOldTmp()) {
				//noinspection DataFlowIssue
				table.getOldTable().remove(getDatabaseTransactionOldTmp(), snapshotKey);
			}
		}
	}

	@Override
	public void cleanup() {
		setDatabaseTransactionTmp(null);
		setDatabaseTransactionOldTmp(null);

		if (table.getZeze().getConfig().getCheckpointMode() == CheckpointMode.Period) {
			TableKey tkey = new TableKey(table.getId(), key);
			Lockey lockey = table.getZeze().getLocks().get(tkey);
			lockey.enterWriteLock();
			try {
				if (savedTimestampForCheckpointPeriod == getTimestamp()) {
					setDirty(false);
				}
				snapshotKey = null;
				snapshotValue = null;
				return;
			} finally {
				lockey.exitWriteLock();
			}
		}
		// CheckpointMode.Table
		snapshotKey = null;
		snapshotValue = null;
		snapshotKeyLocal = null;
		snapshotValueLocal = null;
	}
}
