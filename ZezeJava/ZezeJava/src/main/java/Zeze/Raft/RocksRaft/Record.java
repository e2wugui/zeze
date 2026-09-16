package Zeze.Raft.RocksRaft;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.FastLock;
import Zeze.Util.RocksDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

public final class Record<K> {
	private static final Logger logger = LogManager.getLogger(Record.class);
	public static final class RootInfo {
		private final Record<?> record;
		private final TableKey tableKey;

		public RootInfo(Record<?> record, TableKey tableKey) {
			this.record = record;
			this.tableKey = tableKey;
		}

		public Record<?> getRecord() {
			return record;
		}

		public TableKey getTableKey() {
			return tableKey;
		}
	}

	public static final int StateNew = 0;
	public static final int StateLoad = 1;
	private static final AtomicLong timestampGen = new AtomicLong(1);

	public static long getNextTimestamp() {
		return timestampGen.getAndIncrement();
	}

	private final BiConsumer<ByteBuffer, K> keyEncodeFunc;
	private int state = StateNew;
	private long timestamp;
	private boolean removed;
	private Table<K, ?> table;
	private K key;
	// 跨线程发布（FND4-33）：apply线程在raft锁内setValue发布业务线程构造的bean，
	// 读线程（另一业务过程）经Table.get→r.mutex读取——两锁无交点，JMM上无
	// happens-before保障。volatile让发布语义由字段自身保证（写侧仍在raft锁内，
	// 语义不变；x86上无额外开销语义变化）。
	private volatile Bean value;
	final FastLock mutex = new FastLock();

	// 【FND7-14】在用计数：Table.getOrLoad在r.mutex临界区内beginAccess，驱逐回调在
	// r.mutex内复查（互斥，不会漏见），保证"事务持有Record引用期间不可被LRU驱逐"——
	// leader事务原位修改缓存Record的bean，提交时经事务捕获的origin应用并flush，驱逐后
	// 同key再访问会从storage装载出旧值的新记录，已提交更新被静默覆盖丢失。
	// 释放点：Transaction.perform收尾（业务访问的记录）与Rocks.followerApply的flush
	// 成功后（followerApply装载的记录）。pendingFlush补偿集由登记统一持有（putPendingFlush
	// 补记、消费成功/过期丢弃/重登记换手时释放），迟到flush重试窗口内记录不可驱逐。
	// 减法钳制到0：吸收极端时序下的重复释放。
	private final AtomicInteger accessors = new AtomicInteger();

	void beginAccess() {
		accessors.incrementAndGet();
	}

	void endAccess() {
		accessors.updateAndGet(v -> v > 0 ? v - 1 : v);
	}

	boolean isAccessed() {
		return accessors.get() != 0;
	}

	/**
	 * 【FND7-14联动·截断污染驱逐】flush 补偿因 term 不匹配被丢弃（{@code Rocks.takePendingFlush}
	 * 的过期分支）时调用：记录的内存 bean 可能已被截断条目应用过（apply 是"先改内存、后flush"，
	 * flush 失败的补偿窗口内内存态停留在截断条目应用后的样子）。污染记录留在缓存中，后续
	 * {@code Table.followerApply/getOrLoad} 会命中它，把新条目的增量日志叠加到旧条目的残迹上，
	 * 双重应用被提交复制出去即 leader/follower 静默分歧。这里把它驱逐出缓存，后续访问从
	 * storage 重载干净基线（flush 失败时 storage 仍是截断前已提交的状态）。
	 * <p>
	 * 【决策】不经使用方回调（{@code lruTryRemoveCallback}）直接 pair-remove：回调语义是
	 * "容量驱逐时征询使用方"（GCM 拒绝协议未完结的记录），而这里是正确性要求的失效——
	 * 回调拒绝会让污染永久滞留；且使用方的删除变体不置 removed，并发
	 * {@code getOrLoad} 竞争者仍可能拿到污染引用。在用（isAccessed）时放弃本轮驱逐：
	 * 强制摘除会让在用方提交时经 origin 应用 flush 后，与驱逐后重装载的记录互相整值覆盖
	 * （FND7-14 同型丢失更新）；此窗口内污染对并发读方的可见性由"普通表无同 key 并发
	 * 隔离"契约覆盖（见 Table 类头）。
	 */
	void evictPolluted() {
		var t = table;
		var lru = t != null ? t.getLruCache() : null;
		if (lru == null)
			return; // 记录未挂表（测试直建）或表已关闭（lruCache置null）：无缓存可驱逐
		if (isAccessed()) {
			logger.warn("{}: polluted record(key={}) still in-use, skip eviction this round;"
					+ " concurrent read of it is covered by the no-same-key-isolation contract.",
					t.getName(), key);
			return;
		}
		if (!mutex.tryLock())
			return; // 并发getOrLoad临界区内：该路径返回前必然beginAccess，按在用处理
		try {
			if (isAccessed()) {
				logger.warn("{}: polluted record(key={}) still in-use, skip eviction this round.",
						t.getName(), key);
				return;
			}
			setRemoved(true); // 并发getOrLoad竞争者经removed重试环换新记录
			lru.remove(key, this); // pair-remove：仅当映射仍是本记录时删除
		} finally {
			mutex.unlock();
		}
	}

	public Record(Class<K> keyClass) {
		keyEncodeFunc = SerializeHelper.createEncodeFunc(keyClass);
	}

	public Record(BiConsumer<ByteBuffer, K> keyEncodeFunc) {
		this.keyEncodeFunc = keyEncodeFunc;
	}

	public int getState() {
		return state;
	}

	public void setState(int value) {
		state = value;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long value) {
		timestamp = value;
	}

	public boolean getRemoved() {
		return removed;
	}

	public void setRemoved(boolean value) {
		removed = value;
	}

	public Table<K, ?> getTable() {
		return table;
	}

	public void setTable(Table<K, ?> value) {
		table = value;
	}

	public K getKey() {
		return key;
	}

	public void setKey(K value) {
		key = value;
	}

	public Bean getValue() {
		return value;
	}

	public void setValue(Bean value) {
		this.value = value;
	}

	public RootInfo createRootInfoIfNeed(TableKey tkey) {
		var cur = value != null ? value.rootInfo() : null;
		return cur != null ? cur : new RootInfo(this, tkey);
	}

	public void leaderApply(Transaction.RecordAccessed accessed) {
		if (accessed.getPutLog() != null)
			setValue(accessed.getPutLog().value);
		timestamp = getNextTimestamp(); // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。
	}

	public void flush(RocksDatabase.Batch batch) throws RocksDBException {
		var keyBB = ByteBuffer.Allocate();
		keyEncodeFunc.accept(keyBB, key);
		if (value != null) {
			int preAllocSize = value.preAllocSize();
			ByteBuffer valueBB = ByteBuffer.Allocate(Math.min(preAllocSize, 65536));
			value.encode(valueBB);
			int size = valueBB.WriteIndex;
			if (size > preAllocSize)
				value.preAllocSize(size);
			table.getRocksTable().put(batch, keyBB.Bytes, keyBB.WriteIndex, valueBB.Bytes, size);
		} else
			table.getRocksTable().delete(batch, keyBB.Bytes, keyBB.WriteIndex);
	}
}
