package Zeze.Raft.RocksRaft;

import java.util.Objects;

import java.lang.invoke.MethodHandle;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.Func1;
import Zeze.Util.Func2;
import Zeze.Util.Reflect;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.rocksdb.RocksDBException;
import org.jspecify.annotations.Nullable;

/**
 * RocksRaft 表：{@link Record} LRU 缓存与 RocksDB 存储的读写入口。
 * 普通表不提供同 key 并发隔离，需由调用方通过 {@link PessimismLock} 或外部串行化保证隔离；
 * 容量驱逐的在用保护仅保证引用期间不被驱逐。
 */
public final class Table<K, V extends Bean> {
	private final Rocks rocks;
	private final String templateName;
	private final int templateId;
	private final String name;
	private final BiConsumer<ByteBuffer, K> keyEncodeFunc;
	private final Function<IByteBuffer, K> keyDecodeFunc;
	private final MethodHandle valueFactory;
	private int cacheCapacity = 10000;
	private RocksDatabase.Table rocksTable;
	private @Nullable ConcurrentLruLike<K, Record<K>> lruCache;
	private @Nullable BiPredicate<K, Record<K>> lruTryRemoveCallback;

	public @Nullable ConcurrentLruLike<K, Record<K>> getLruCache() {
		return lruCache;
	}

	public Table(Rocks rocks, String templateName, int templateId, Class<K> keyClass, Class<V> valueClass, @Nullable BiPredicate<K, Record<K>> callback) {
		this.rocks = rocks;
		this.templateName = templateName;
		this.templateId = templateId;
		name = String.format("%s#%d", this.templateName, this.templateId);
		keyEncodeFunc = SerializeHelper.createEncodeFunc(keyClass);
		keyDecodeFunc = SerializeHelper.createDecodeFunc(keyClass);
		valueFactory = Reflect.getDefaultConstructor(valueClass);
		lruTryRemoveCallback = callback;
		open();
	}

	public void open() {
		try {
			rocksTable = rocks.openTable(name);
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		}
		// 重开表时关闭旧 lruCache 的周期任务，防止随 restore/reset 泄漏缓存引用。
		var oldLru = lruCache;
		if (oldLru != null)
			oldLru.close();
		// 始终安装带在用保护的驱逐回调，使用方自定义回调在其后执行。
		lruCache = new ConcurrentLruLike<>(name, cacheCapacity, this::tryRemoveRecord, 200, 2000, 1024);
	}

	/** 关闭记录缓存的周期任务并置空缓存引用，由 {@link Rocks#close()} 级联调用。 */
	public void close() {
		var lru = lruCache;
		if (lru != null) {
			lru.close();
			lruCache = null; // 阻止关闭后继续访问存储句柄
		}
	}

	/**
	 * 带在用保护的 LRU 驱逐回调：记录在用时拒绝驱逐；确无在用时在 {@code r.mutex} 内置 removed 并 pair-remove，
	 * 配合 {@link #getOrLoad} 的重试环避免分裂缓存与丢失更新。
	 */
	private boolean tryRemoveRecord(K key, Record<K> r) {
		if (r.isAccessed())
			return false;
		if (!r.mutex.tryLock())
			return false;
		try {
			if (r.isAccessed() || r.getRemoved()) // 在 r.mutex 内复查，与 beginAccess/evict 互斥
				return false;
			var callback = lruTryRemoveCallback;
			if (callback != null) {
				// 自定义回调在复查在用后执行，返回 true 后置 removed 并幂等 pair-remove，避免并发 getOrLoad 分裂缓存。
				if (!callback.test(key, r))
					return false;
				r.setRemoved(true);
				var lru = lruCache;
				if (lru != null)
					lru.remove(key, r);
				return true;
			}
			r.setRemoved(true);
			//noinspection DataFlowIssue
			return lruCache.remove(key, r); // 仅当映射仍指向本记录时删除
		} finally {
			r.mutex.unlock();
		}
	}

	public Rocks getRocks() {
		return rocks;
	}

	public String getTemplateName() {
		return templateName;
	}

	public int getTemplateId() {
		return templateId;
	}

	public String getName() {
		return name;
	}

	public int getCacheCapacity() {
		return cacheCapacity;
	}

	public void setCacheCapacity(int value) {
		cacheCapacity = value;
	}

	public RocksDatabase.Table getRocksTable() {
		return rocksTable;
	}

	public @Nullable BiPredicate<K, Record<K>> getLruTryRemoveCallback() {
		return lruTryRemoveCallback;
	}

	public void setLruTryRemoveCallback(BiPredicate<K, Record<K>> value) {
		lruTryRemoveCallback = value;
	}

	public void encodeKey(ByteBuffer bb, K key) {
		keyEncodeFunc.accept(bb, key);
	}

	public K decodeKey(IByteBuffer bb) {
		return keyDecodeFunc.apply(bb);
	}

	@SuppressWarnings("unchecked")
	public V newValue() {
		try {
			return (V)valueFactory.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}

	// RocksRaft 表读写需在显式事务（rocks.newProcedure）内执行；仅 selectDirty 允许无事务直读存储。
	private Transaction requireCurrentTransaction() {
		var currentT = Transaction.getCurrent();
		if (currentT == null)
			throw new IllegalStateException(
					"no current transaction: RocksRaft table '" + name + "' requires rocks.newProcedure(...)");
		currentT.bindRocks(rocks);
		return currentT;
	}

	@SuppressWarnings("unchecked")
	public @Nullable V get(K key) {
		Transaction currentT = requireCurrentTransaction();
		TableKey tkey = new TableKey(name, key);

		var cr = currentT.getRecordAccessed(tkey);
		if (cr != null)
			return (V)cr.newestValue();

		Record<K> r = getOrLoad(key);
		currentT.addRecordAccessed(r.createRootInfoIfNeed(tkey), new Transaction.RecordAccessed(currentT, r));
		return (V)r.getValue();
	}

	@SuppressWarnings("unchecked")
	public V getOrAdd(K key) {
		Transaction currentT = requireCurrentTransaction();
		TableKey tkey = new TableKey(name, key);

		var cr = currentT.getRecordAccessed(tkey);
		if (cr != null) {
			V crv = (V)cr.newestValue();
			if (crv != null)
				return crv;
		} else {
			Record<K> r = getOrLoad(key);
			cr = new Transaction.RecordAccessed(currentT, r);
			currentT.addRecordAccessed(r.createRootInfoIfNeed(tkey), cr);
			if (r.getValue() != null)
				return (V)Objects.requireNonNull(r.getValue());
		}

		V add = newValue();
		add.initRootInfo(cr.getOrigin().createRootInfoIfNeed(tkey), null);
		cr.put(currentT, add);
		return add;
	}

	private Record<K> getOrLoad(K key) {
		return getOrLoad(key, null);
	}

	private Record<K> getOrLoad(K key, @Nullable Bean putValue) {
		TableKey tkey = new TableKey(name, key);
		while (true) {
			var r = Objects.requireNonNull(lruCache).getOrAdd(key, () -> {
				var newR = new Record<>(keyEncodeFunc);
				newR.setTable(this);
				newR.setKey(key);
				return newR;
			});
			r.mutex.lock();
			try {
				if (r.getRemoved())
					continue;

				if (putValue != null) {
					r.setValue(putValue);
					Objects.requireNonNull(r.getValue()).initRootInfo(r.createRootInfoIfNeed(tkey), null);
					r.setTimestamp(Record.getNextTimestamp());
					r.setState(Record.StateLoad);
				} else if (r.getState() == Record.StateNew) {
					r.setValue(storageLoad(key));
					if (r.getValue() != null)
						Objects.requireNonNull(r.getValue()).initRootInfo(r.createRootInfoIfNeed(tkey), null);
					r.setTimestamp(Record.getNextTimestamp());
					r.setState(Record.StateLoad);
				}
				// 在 r.mutex 内登记在用，与驱逐回调互斥。
				r.beginAccess();
				return r;
			} finally {
				r.mutex.unlock();
			}
		}
	}

	private @Nullable V storageLoad(K key) {
		var keyBB = ByteBuffer.Allocate();
		keyEncodeFunc.accept(keyBB, key);
		byte[] valueBytes;
		try {
			valueBytes = rocksTable.get(keyBB.Bytes, 0, keyBB.WriteIndex);
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		}
		if (valueBytes == null)
			return null;
		var valueBB = ByteBuffer.Wrap(valueBytes);
		var value = newValue();
		value.decode(valueBB);
		return value;
	}

	/** 同实例的事务记录可覆盖存储读。其余调用直接读取目标存储的当前值。 */
	@SuppressWarnings("unchecked")
	public @Nullable V selectDirty(K key) {
		Transaction currentT = Transaction.getCurrent();
		if (currentT != null && currentT.ownsRocks(rocks)) {
			var cr = currentT.getRecordAccessed(new TableKey(name, key));
			if (cr != null)
				return (V)cr.newestValue();
		}
		return storageLoad(key);
	}

	public boolean tryAdd(K key, V value) {
		if (get(key) != null)
			return false;

		Transaction currentT = requireCurrentTransaction();
		TableKey tkey = new TableKey(name, key);
		var cr = currentT.getRecordAccessed(tkey);
		//noinspection DataFlowIssue
		value.initRootInfo(cr.getOrigin().createRootInfoIfNeed(tkey), null);
		cr.put(currentT, value);
		return true;
	}

	public void insert(K key, V value) {
		if (!tryAdd(key, value))
			throw new IllegalArgumentException(String.format("table:%s insert key:%s exists", getClass().getName(), key));
	}

	public void put(K key, V value) {
		Transaction currentT = requireCurrentTransaction();
		TableKey tkey = new TableKey(name, key);

		var cr = currentT.getRecordAccessed(tkey);
		if (cr == null) {
			var r = getOrLoad(key);
			cr = new Transaction.RecordAccessed(currentT, r);
			currentT.addRecordAccessed(r.createRootInfoIfNeed(tkey), cr);
		}
		value.initRootInfo(cr.getOrigin().createRootInfoIfNeed(tkey), null);
		cr.put(currentT, value);
	}

	public void remove(K key) {
		Transaction currentT = requireCurrentTransaction();
		TableKey tkey = new TableKey(name, key);

		var cr = currentT.getRecordAccessed(tkey);
		if (cr != null) {
			cr.put(currentT, null);
			return;
		}

		Record<K> r = getOrLoad(key);
		cr = new Transaction.RecordAccessed(currentT, r);
		cr.put(currentT, null);
		currentT.addRecordAccessed(r.createRootInfoIfNeed(tkey), cr);
	}

	public boolean walk(Func2<K, V, Boolean> callback) throws Exception {
		try (var it = rocksTable.iterator()) {
			for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next()) {
				var key = keyDecodeFunc.apply(ByteBuffer.Wrap(it.key()));
				var value = newValue();
				value.decode(ByteBuffer.Wrap(it.value()));
				if (!callback.call(key, value))
					return false;
			}
			return true;
		}
	}

	public boolean walkKey(Func1<K, Boolean> callback) throws Exception {
		try (var it = rocksTable.iterator()) {
			for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next()) {
				var key = keyDecodeFunc.apply(ByteBuffer.Wrap(it.key()));
				if (!callback.call(key))
					return false;
			}
			return true;
		}
	}

	public Record<K> followerApply(K key, Changes.Record rLog) {
		Record<K> r;
		switch (rLog.getState()) {
		case Changes.Record.Remove:
			r = getOrLoad(key);
			r.setValue(null);
			r.setTimestamp(Record.getNextTimestamp());
			break;

		case Changes.Record.Put:
			r = getOrLoad(key, rLog.getPutValue());
			break;

		case Changes.Record.Edit:
			r = getOrLoad(key);
			// 记录缺失即状态机分歧，直接抛 NPE 由 Rocks.followerApply 统一 fatalKill。
			for (var log : rLog.getLogBean())
				//noinspection DataFlowIssue
				r.getValue().followerApply(log); // 最多一个。
			break;

		default:
			throw new IllegalStateException("unknown Changes.Record.State. table=" + name
					+ " key=" + key + " state=" + rLog.getState());
		}
		return r;
	}
}
