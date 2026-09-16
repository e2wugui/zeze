package Zeze.Raft.RocksRaft;

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

/**
 * RocksRaft 的表：{@link Record} 缓存（{@link ConcurrentLruLike}）+ RocksDB 存储的读写面。
 * 【隔离契约】普通表（未接使用方串行化）没有同 key 并发隔离：多个并发事务对同一 key
 * 的读写交织在同一个缓存 Record/bean 上（无乐观冲突检测——{@code Transaction.
 * _lock_and_check_} 恒通过；无记录锁），后提交者的整值 flush 会覆盖并发方的修改，
 * 回滚也不还原内存态（见 {@link Savepoint#rollback} 的契约声明）。同 key 串行化责任
 * 在使用方：GCM 的悲观锁是现成接线（{@code Transaction.addPessimismLock}，
 * 参考 {@code GlobalCacheManagerWithRaft.acquireShare} 持锁后再访问表的模式），
 * 或由使用方保证同 key 过程不并发。容量驱逐的"在用保护"（{@link #tryRemoveRecord}）
 * 只保护引用期间不被驱逐，不提供上述隔离。
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
	private ConcurrentLruLike<K, Record<K>> lruCache;
	private BiPredicate<K, Record<K>> lruTryRemoveCallback;

	public ConcurrentLruLike<K, Record<K>> getLruCache() {
		return lruCache;
	}

	public Table(Rocks rocks, String templateName, int templateId, Class<K> keyClass, Class<V> valueClass, BiPredicate<K, Record<K>> callback) {
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
		// 【FND7-36联动】restore/reset重开Table时先关闭旧lruCache：其构造器注册的两个
		// 周期任务（热点轮转+cleanNow）不取消的话，旧实例任务永续执行并强引用dataMap
		//（每表最多容量条Record/Bean），随InstallSnapshot恢复/状态机重置的重开次数无界泄漏。
		var oldLru = lruCache;
		if (oldLru != null)
			oldLru.close();
		// 【FND7-14】总是安装自带在用保护的驱逐回调（生成代码注册表模板时不会传callback，
		// 原实现走ConcurrentLruLike.cleanNow的无回调分支无条件remove，事务正在使用的记录
		// 也会被驱逐）。使用方回调（如GlobalCacheManagerWithRaft）在保护检查之后执行。
		lruCache = new ConcurrentLruLike<>(name, cacheCapacity, this::tryRemoveRecord, 200, 2000, 1024);
	}

	/**
	 * 【FND7-36联动】关闭记录缓存内建的两个周期任务（热点轮转+cleanNow），实例随之可
	 * 被整体回收。由 {@link Rocks#close()} 级联调用；缓存条目不清理（实例已到生命周期末尾）。
	 */
	public void close() {
		var lru = lruCache;
		if (lru != null) {
			lru.close();
			lruCache = null; // 阻止关闭后的访问（lazy load会触碰已关闭的存储句柄）
		}
	}

	/**
	 * 【FND7-14】LRU驱逐的在用保护（对齐经典Zeze.Transaction.TableCache的驱逐语义）。
	 * leader业务事务从get()拿到Record引用后原位修改其bean，提交时leaderApply经事务
	 * 捕获的origin记录应用并flush到RocksDB——若驱逐无保护，驱逐后同key再访问会从
	 * storage装载出旧值的新记录C：后续读经过期值，再修改提交则已提交更新被C的旧值
	 * 全量覆盖静默丢失（follower侧单线程apply自愈，无此问题）。Record.removed标志与
	 * getOrLoad的重试环正是为此设计（原为死代码，setRemoved零调用者）。
	 * 在用（isAccessed）时拒绝驱逐；确无在用时在r.mutex内置removed=true（让并发拿到
	 * 旧引用的getOrLoad重试换新记录）并pair-remove。
	 */
	private boolean tryRemoveRecord(K key, Record<K> r) {
		if (r.isAccessed())
			return false;
		var callback = lruTryRemoveCallback; // 使用方回调在保护之后执行
		if (callback != null)
			return callback.test(key, r);
		if (!r.mutex.tryLock())
			return false;
		try {
			if (r.isAccessed()) // 复查：beginAccess在r.mutex内，与此处互斥
				return false;
			r.setRemoved(true);
			return lruCache.remove(key, r); // pair-remove：仅当映射仍是本记录时删除
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

	public BiPredicate<K, Record<K>> getLruTryRemoveCallback() {
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

	// 【FND-R2-8】RocksRaft的Table要求显式事务（rocks.newProcedure(...)内部）：
	// 无事务时Transaction.getCurrent()返回null，直接解引用只会NPE且堆栈指向框架内部，
	// 迁移自经典Zeze Table（自动隐式建事务）的代码很容易在启动初始化/后台线程/定时器
	// 任务体里踩中；给出带表名与用法的明确错误。selectDirty是唯一例外：无事务时降级直读存储。
	private Transaction requireCurrentTransaction() {
		var currentT = Transaction.getCurrent();
		if (currentT == null)
			throw new IllegalStateException(
					"no current transaction: RocksRaft table '" + name + "' requires rocks.newProcedure(...)");
		return currentT;
	}

	@SuppressWarnings("unchecked")
	public V get(K key) {
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
			// add
		} else {
			Record<K> r = getOrLoad(key);
			cr = new Transaction.RecordAccessed(currentT, r);
			currentT.addRecordAccessed(r.createRootInfoIfNeed(tkey), cr);
			if (r.getValue() != null)
				return (V)r.getValue();
			// add
		}

		V add = newValue();
		add.initRootInfo(cr.getOrigin().createRootInfoIfNeed(tkey), null);
		cr.put(currentT, add);
		return add;
	}

	private Record<K> getOrLoad(K key) {
		return getOrLoad(key, null);
	}

	private Record<K> getOrLoad(K key, Bean putValue) {
		TableKey tkey = new TableKey(name, key);
		while (true) {
			var r = lruCache.getOrAdd(key, () -> {
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
					// from followerApply
					r.setValue(putValue);
					r.getValue().initRootInfo(r.createRootInfoIfNeed(tkey), null);
					r.setTimestamp(Record.getNextTimestamp());
					r.setState(Record.StateLoad);
				} else if (r.getState() == Record.StateNew) {
					// fresh record
					r.setValue(storageLoad(key));
					if (r.getValue() != null)
						r.getValue().initRootInfo(r.createRootInfoIfNeed(tkey), null);
					r.setTimestamp(Record.getNextTimestamp());
					r.setState(Record.StateLoad);
				}
				// else in cache
				// 【FND7-14】在r.mutex临界区内登记在用：与驱逐回调的r.mutex互斥，
				// 保证调用方拿到引用时驱逐方不可能漏见在用状态。
				r.beginAccess();
				return r;
			} finally {
				r.mutex.unlock();
			}
		}
	}

	private V storageLoad(K key) {
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

	@SuppressWarnings("unchecked")
	public V selectDirty(K key) {
		Transaction currentT = Transaction.getCurrent();
		if (currentT != null) {
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

	// 几乎和Put一样，还是独立开吧。
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
			for (it.seekToFirst(); it.isValid(); it.next()) {
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
			for (it.seekToFirst(); it.isValid(); it.next()) {
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
			// record不存在即先行分歧：直接getValue().followerApply抛NPE，由Rocks.followerApply
			// 统一catch并fatalKill（宁死不糊），不再内联防御。
			for (var log : rLog.getLogBean())
				r.getValue().followerApply(log); // 最多一个。
			break;

		default:
			// 未知状态：直接抛出，由Rocks.followerApply统一catch并fatalKill。
			throw new IllegalStateException("unknown Changes.Record.State. table=" + name
					+ " key=" + key + " state=" + rLog.getState());
		}
		return r;
	}
}
