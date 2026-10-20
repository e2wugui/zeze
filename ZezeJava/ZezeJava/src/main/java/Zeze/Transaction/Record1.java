package Zeze.Transaction;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentHashMap;

import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.Id128;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import Zeze.Transaction.IGlobalAgent.AcquireResult;

/**
 * 强类型表记录实现：绑定 TableX 与具体 key，管理软引用值和 LRU 节点。
 * 单次刷盘的编码结果和事务由 Checkpoint 持有，编码与写入由 TableX 执行。
 */
public final class Record1<K extends Comparable<K>, V extends Bean> extends Record {
	private static final Logger logger = LogManager.getLogger(Record1.class);
	private static final boolean isTraceEnabled = logger.isTraceEnabled();
	private static final VarHandle LRU_NODE_HANDLE;

	static {
		try {
			LRU_NODE_HANDLE = MethodHandles.lookup().findVarHandle(Record1.class, "lruNode", ConcurrentHashMap.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final TableX<K, V> table;
	private final K key;
	private volatile @Nullable ConcurrentHashMap<K, Record1<K, V>> lruNode;
	private @Nullable Id128 tid;

	public void setTid(@Nullable Id128 tid) {
		this.tid = tid;
	}

	public @Nullable Id128 getTid() {
		return tid;
	}

	public Record1(TableX<K, V> table, K key, @Nullable V value) {
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
	public TableX<K, V> getTable() {
		return table;
	}

	@Override
	public K getObjectKey() {
		return key;
	}

	@Nullable ConcurrentHashMap<K, Record1<K, V>> getLruNode() {
		return lruNode;
	}

	void setLruNode(ConcurrentHashMap<K, Record1<K, V>> value) {
		lruNode = value;
	}

	@SuppressWarnings("unchecked")
	@Nullable ConcurrentHashMap<K, Record1<K, V>> getAndSetLruNodeNull() {
		return (ConcurrentHashMap<K, Record1<K, V>>)LRU_NODE_HANDLE.getAndSet(this, null);
	}

	boolean compareAndSetLruNodeNull(ConcurrentHashMap<K, Record1<K, V>> c) {
		return LRU_NODE_HANDLE.compareAndSet(this, c, null);
	}

	@Override
	public String toString() {
		return String.format("T=%s K=%s S=%d T=%d", table.getName(), key, getState(), getTimestamp());
		// 记录的log可能在Transaction.AddRecordAccessed之前进行，不能再访问了。
	}

	@Override
	public @Nullable AcquireResult acquire(int state, boolean fresh, boolean noWait) {
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
	public void commit(RecordAccessed accessed) {
		enterFairLock();
		try {
			var committedPutLog = accessed.committedPutLog;
			if (committedPutLog != null) {
				setSoftValue(committedPutLog.getValue());
				// 内存表启用了soft，不能马上删除，按正常逻辑执行。
				// 计算内存表的大小。
				if (table.isMemory()) {
					if (accessed.atomicTupleRecord.strongRef() == null && committedPutLog.getValue() != null) // add
						table.requireCache().getSizeCounter().increment();
					else if (accessed.atomicTupleRecord.strongRef() != null && committedPutLog.getValue() == null) // remove
						table.requireCache().getSizeCounter().decrement();
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
}
