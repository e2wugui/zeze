package Zeze.Transaction;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.Id128;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 强类型表记录实现：绑定 TableX 与具体 key，管理软引用值、LRU 节点、
 * 编码快照（encode0）与落库 flush/cleanup 生命周期。
 */
public final class Record1<K extends Comparable<K>, V extends Bean> extends Record {
	private static final @NotNull Logger logger = LogManager.getLogger(Record1.class);
	private static final boolean isTraceEnabled = logger.isTraceEnabled();
	private static final @NotNull VarHandle LRU_NODE_HANDLE;

	static {
		try {
			LRU_NODE_HANDLE = MethodHandles.lookup().findVarHandle(Record1.class, "lruNode", ConcurrentHashMap.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final @NotNull TableX<K, V> table;
	private final @NotNull K key;
	private Object snapshotKey;
	private Object snapshotValue;
	private Object snapshotKeyLocal;
	private Object snapshotValueLocal;
	private volatile @Nullable ConcurrentHashMap<K, Record1<K, V>> lruNode;
	private @Nullable Id128 tid;

	public void setTid(@Nullable Id128 tid) {
		this.tid = tid;
	}

	public @Nullable Id128 getTid() {
		return tid;
	}

	public Record1(@NotNull TableX<K, V> table, @NotNull K key, @Nullable V value) {
		super(value);
		this.table = table;
		this.key = key;
	}

	boolean containsValue() {
		return getSoftValue() != null || !getDirty() && table.getLocalRocksCacheTable().containsKey(table, key);
	}

	@Nullable V copyValue() {
		var v = getSoftValue();
		if (v != null) {
			var lockey = table.getZeze().getLocks().get(new TableKey(table.getId(), key));
			// 注意：这个在fairLock里执行，而原则上为了避免死锁，锁的策略是先加lockey，再加fairLock，
			// 这里由于用的try，所以虽然违背了原则，但也不会死锁。
			if (lockey.tryEnterReadLock(0)) {
				try {
					@SuppressWarnings("unchecked")
					V v1 = (V)v.copy();
					return v1;
				} finally {
					lockey.exitReadLock();
				}
			}
		} else if (getDirty())
			return null;
		return table.getLocalRocksCacheTable().find(table, key);
	}

	@Nullable V loadValue() {
		@SuppressWarnings("unchecked")
		var v = (V)getSoftValue();
		if (v == null && !getDirty()) {
			v = table.getLocalRocksCacheTable().find(table, key);
			if (v != null) {
				v.initRootInfo(createRootInfoIfNeed(new TableKey(table.getId(), key)), null);
				setSoftValue(v);
			}
			// 镜像miss=不存在：镜像写失败从不被吞（rocksCachePut/Remove抛出），clean记录必有
			// 等于storage真相的镜像备份。残余仅为进程内镜像静默损坏，运维域，见TableX.rocksCachePut。
		}
		return v;
	}

	@Override
	public @NotNull TableX<K, V> getTable() {
		return table;
	}

	@Override
	public @NotNull K getObjectKey() {
		return key;
	}

	@Nullable ConcurrentHashMap<K, Record1<K, V>> getLruNode() {
		return lruNode;
	}

	void setLruNode(@NotNull ConcurrentHashMap<K, Record1<K, V>> value) {
		lruNode = value;
	}

	@SuppressWarnings("unchecked")
	@Nullable ConcurrentHashMap<K, Record1<K, V>> getAndSetLruNodeNull() {
		return (ConcurrentHashMap<K, Record1<K, V>>)LRU_NODE_HANDLE.getAndSet(this, null);
	}

	boolean compareAndSetLruNodeNull(@NotNull ConcurrentHashMap<K, Record1<K, V>> c) {
		return LRU_NODE_HANDLE.compareAndSet(this, c, null);
	}

	@Override
	public @NotNull String toString() {
		return String.format("T=%s K=%s S=%d T=%d", table.getName(), key, getState(), getTimestamp());
		// 记录的log可能在Transaction.AddRecordAccessed之前进行，不能再访问了。
	}

	@Override
	public @Nullable IGlobalAgent.AcquireResult acquire(int state, boolean fresh, boolean noWait) {
		IGlobalAgent agent;
		if (table.isMemory() || (agent = table.getZeze().getGlobalAgent()) == null) // 不支持内存表cache同步。
			return IGlobalAgent.AcquireResult.getSuccessResult(state);

		if (isTraceEnabled)
			logger.trace("Acquire NewState={} {}", state, this);
		var tableId = table.getId();
		switch (state) {
		case GlobalCacheManagerConst.StateInvalid:
			ZezeCounter.instance.tableCounter(tableId, ZezeCounter.TableMetric.ACQUIRE_INVALID).increment();
			break;
		case GlobalCacheManagerConst.StateShare:
			ZezeCounter.instance.tableCounter(tableId, ZezeCounter.TableMetric.ACQUIRE_SHARE).increment();
			break;
		case GlobalCacheManagerConst.StateModify:
			ZezeCounter.instance.tableCounter(tableId, ZezeCounter.TableMetric.ACQUIRE_MODIFY).increment();
			break;
		}
		return agent.acquire(table.encodeGlobalKey(key), state, fresh, noWait);
	}

	@Override
	public void commit(@NotNull RecordAccessed accessed) {
		enterFairLock();
		try {
			var committedPutLog = accessed.committedPutLog;
			if (committedPutLog != null) {
				setSoftValue(committedPutLog.getValue());
				// 内存表启用了soft，不能马上删除，按正常逻辑执行。
				// 计算内存表的大小。
				if (table.isMemory()) {
					if (accessed.atomicTupleRecord.strongRef() == null && committedPutLog.getValue() != null) // add
						table.getCache().getSizeCounter().increment();
					else if (accessed.atomicTupleRecord.strongRef() != null && committedPutLog.getValue() == null) // remove
						table.getCache().getSizeCounter().decrement();
				}
			}
			setTimestamp(getNextTimestamp()); // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。
			setDirty();
		} finally {
			exitFairLock();
		}
	}

	@Override
	public void setDirty() {
		// Table: 后台 Checkpoint.flush(RelativeRecordSet) 保存后清除；
		// Immediately: 提交流程 Checkpoint.flush(Iterable<Record>) 同步保存后清除。
		// see Checkpoint.flush(Iterable<Record> rs, Set<OnzProcedure>, History)
		setDirty(true);
	}

	@Override
	public void encode0() {
		if (!getDirty())
			return;
		// Under Lock：Table模式持有rrs锁（Checkpoint.flushInternal）；Immediately模式在事务持锁提交流程内。

		// 可能编码多次（flush失败重试时重新编码）。
		// 脏值经单次volatile快照获取；脏删除快照为null，编码为删除。
		var v = getDirtyValue();
		if (table.isRelationalMapping() && table.getDatabase() instanceof DatabaseRelationalMapping) {
			var sqlKey = new SQLStatement();
			table.encodeKeySQLStatement(sqlKey, key);
			if (v == null)
				snapshotValue = null;
			else {
				var sqlValue = new SQLStatement();
				var parents = new ArrayList<String>();
				v.encodeSQLStatement(parents, sqlValue);
				snapshotValue = sqlValue;
			}
			snapshotKey = sqlKey;

			// 编码用来存储到本地rocksdb的cache中。
			snapshotKeyLocal = table.encodeKey(key);
			snapshotValueLocal = v != null ? ByteBuffer.encode(v) : null;
		} else {
			// KV表，本地Cache和远程一样。
			snapshotKeyLocal = snapshotKey = table.encodeKey(key);
			snapshotValueLocal = snapshotValue = v != null ? ByteBuffer.encode(v) : null;
		}
	}

	@Override
	public void flush(@Nullable Database.Transaction t, @NotNull Database.Transaction lct) {
		if (!getDirty())
			return;

		var storage = table.getStorage();
		if (snapshotValue != null) {
			// changed
			if (storage != null && t != null)
				storage.getDatabaseTable().replace(t, snapshotKey, snapshotValue);

			table.getLocalRocksCacheTable().replace(lct, snapshotKeyLocal, snapshotValueLocal);
		} else {
			// removed
			if (storage != null && t != null)
				storage.getDatabaseTable().remove(t, snapshotKey);

			table.getLocalRocksCacheTable().remove(lct, snapshotKeyLocal);

			// 需要同步删除OldTable，否则下一次查找又会找到。
			// 这个违背了OldTable不修改的原则，但没办法了。
			var databaseTransactionOldTmp = getDatabaseTransactionOldTmp();
			if (databaseTransactionOldTmp != null) {
				// oldTable按KV语义使用（load迁移路径与下方localRocksCache同），必须传编码后的
				// snapshotKeyLocal；关系映射表的snapshotKey是SQLStatement，传入必抛CCE。
				var oldTable = table.getOldTable();
				if (oldTable != null) {
					oldTable.remove(databaseTransactionOldTmp, snapshotKeyLocal);
				}
			}
		}
	}

	@Override
	public void cleanup() {
		setDatabaseTransactionTmp(null);
		setDatabaseTransactionOldTmp(null);
		// CheckpointMode.Table
		snapshotKey = null;
		snapshotValue = null;
		snapshotKeyLocal = null;
		snapshotValueLocal = null;
	}
}
