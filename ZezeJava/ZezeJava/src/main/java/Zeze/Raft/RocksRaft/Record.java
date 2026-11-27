package Zeze.Raft.RocksRaft;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

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

/**
 * 表的一条缓存记录：key、当前值 bean、时间戳与在用计数，配合 LRU 驱逐保护与批量 flush 落盘。
 */
public final class Record<K> {
	private static final Logger logger = LogManager.getLogger(Record.class);

	public record RootInfo(@Nullable Record<?> record, TableKey tableKey) {

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
	private @Nullable Table<K, ?> table;
	private @Nullable K key;
	// volatile 保证 apply 线程（持 raft 锁）与业务读线程（持 r.mutex）之间的跨线程可见性。
	private volatile @Nullable Bean value;
	final FastLock mutex = new FastLock();

	// 在用引用计数：在 r.mutex 内增计数并与驱逐回调互斥，防止事务访问、followerApply 装载或 pendingFlush 重试期间被 LRU 驱逐；
	// 减法钳制到 0，吸收极端时序下的重复释放。
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
	 * 驱逐可能被截断日志污染的缓存记录，迫使后续访问从 storage 重载干净基线。
	 * 绕过使用方容量驱逐回调直接 pair-remove；若记录正在被使用则跳过本轮，避免与在用事务互相覆盖。
	 */
	void evictPolluted() {
		var t = table;
		var lru = t != null ? t.getLruCache() : null;
		if (lru == null)
			return; // 记录未挂表或表已关闭，无缓存可驱逐
		if (isAccessed()) {
			logger.warn("{}: polluted record(key={}) still in-use, skip eviction this round;"
					+ " concurrent read of it is covered by the no-same-key-isolation contract.",
					t.getName(), key);
			return;
		}
		if (!mutex.tryLock())
			return; // 并发 getOrLoad 临界区内即将 beginAccess，按在用跳过
		try {
			if (isAccessed()) {
				logger.warn("{}: polluted record(key={}) still in-use, skip eviction this round.",
						t.getName(), key);
				return;
			}
			setRemoved(true); // 并发 getOrLoad 经 removed 标志重试换新记录
			lru.remove(Objects.requireNonNull(getKey()), this); // 仅当映射仍指向本记录时移除
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

	public @Nullable Table<K, ?> getTable() {
		return table;
	}

	public void setTable(Table<K, ?> value) {
		table = value;
	}

	public @Nullable K getKey() {
		return key;
	}

	public void setKey(K value) {
		key = value;
	}

	public @Nullable Bean getValue() {
		return value;
	}

	public void setValue(@Nullable Bean value) {
		this.value = value;
	}

	public RootInfo createRootInfoIfNeed(TableKey tkey) {
		var value = this.value;
		var cur = value != null ? value.rootInfo() : null;
		return cur != null ? cur : new RootInfo(this, tkey);
	}

	public void leaderApply(Transaction.RecordAccessed accessed) {
		if (accessed.getPutLog() != null)
			setValue(accessed.getPutLog().value);
		timestamp = getNextTimestamp(); // 必须在 setValue 之后更新，防止新事务读到新时间戳与旧值。
	}

	public void flush(RocksDatabase.Batch batch) throws RocksDBException {
		var keyBB = ByteBuffer.Allocate();
		keyEncodeFunc.accept(keyBB, Objects.requireNonNull(getKey()));
		var value = this.value;
		if (value != null) {
			int preAllocSize = value.preAllocSize();
			ByteBuffer valueBB = ByteBuffer.Allocate(Math.min(preAllocSize, 65536));
			value.encode(valueBB);
			int size = valueBB.WriteIndex;
			if (size > preAllocSize)
				value.preAllocSize(size);
			//noinspection DataFlowIssue
			getTable().getRocksTable().put(batch, keyBB.Bytes, keyBB.WriteIndex, valueBB.Bytes, size);
		} else
			//noinspection DataFlowIssue
			getTable().getRocksTable().delete(batch, keyBB.Bytes, keyBB.WriteIndex);
	}
}
