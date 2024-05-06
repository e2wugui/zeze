package Zeze.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.function.Supplier;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Schemas;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import Zeze.Util.PerfCounter;
import Zeze.Util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Services.GlobalCacheManagerConst.StateInvalid;
import static Zeze.Services.GlobalCacheManagerConst.StateModify;
import static Zeze.Services.GlobalCacheManagerConst.StateRemoved;
import static Zeze.Services.GlobalCacheManagerConst.StateShare;

public abstract class TableX<K extends Comparable<K>, V extends Bean> extends Table {
	private static final Logger logger = LogManager.getLogger(TableX.class);
	private static final boolean isTraceEnabled = logger.isTraceEnabled();

	private @Nullable AutoKey autoKey;
	private volatile TableCache<K, V> cache;
	private volatile @Nullable Storage<K, V> storage;
	private @Nullable Database.Table oldTable;
	private DatabaseRocksDb.Table localRocksCacheTable;
	private boolean useRelationalMapping;

	@Override
	public void open(Table exist, Application app) {
		if (getId() != exist.getId())
			throw new IllegalStateException("hot table's id changed.");

		// database 继承，实际上热更新表也没法读到新的database配置。
		var database = exist.getDatabase();

		// replaceTable允许重复调用，热更回滚需要能在旧表上重新打开。see cache init below
		//if (cache != null)
		//	throw new IllegalStateException("table has opened: " + getName());

		setZeze(app);
		setDatabase(database);

		if (isAutoKey()) // autoKey可以发生变化，重新读取。
			autoKey = app.getServiceManager().getAutoKey(getName());

		setTableConf(exist.getTableConf()); // Old
		if (!isMemory() || null == cache) // 如果时内存表，并且是已经存在的表的回滚，保持cache不变。
			cache = new TableCache<>(app, this); // New
		relationalTable = getZeze().getSchemas().relationalTables.get(getName()); // maybe null
		storage = isMemory() ? null : new Storage<>(this, database, getName()); // New
		database.replaceStorage(exist.getStorage(), storage);

		oldTable = exist.getOldTable(); // Old
		// Old 但是需要清除
		localRocksCacheTable = exist.getLocalRocksCacheTable();
		localRocksCacheTable.clear();
		// Old：oldConfig & oldDatabase 刚好是oldRseRelationalMapping
		useRelationalMapping = exist.isRelationalMapping() && database instanceof DatabaseMySql;
	}

	@Override
	public DatabaseRocksDb.Table getLocalRocksCacheTable() {
		return localRocksCacheTable;
	}

	@Override
	public void disable() {
		// 去掉这些，应该所有的操作都无法完成了。
		this.cache = null;
		this.storage = null; // 【这里不处理storage的关闭】
	}

	public TableX(int id, @NotNull String name) {
		super(id, name);
	}

	public TableX(int id, @NotNull String name, @Nullable String suffix) {
		super(id, name, suffix);
	}

	TableCache<K, V> getCache() {
		return cache;
	}

	protected final @Nullable AutoKey getAutoKey() {
		return autoKey;
	}

	public final @Nullable Storage<K, V> internalGetStorageForTestOnly(@NotNull String IAmSure) {
		if (!IAmSure.equals("IKnownWhatIAmDoing"))
			throw new IllegalArgumentException();
		return storage;
	}

	@Override
	final @Nullable Storage<K, V> getStorage() {
		return storage;
	}

	@Override
	final @Nullable Database.Table getOldTable() {
		return oldTable;
	}

	public int keyOffsetInRawKey() {
		var s = storage;
		return s != null ? s.getDatabaseTable().keyOffsetInRawKey() : 0;
	}

	public boolean isUseRelationalMapping() {
		return useRelationalMapping;
	}

	public final int getCacheSize() {
		return cache != null ? cache.getDataMap().size() : 0;
	}

	final void rocksCachePut(@NotNull K key, @NotNull V value) {
		try (var t = getZeze().getLocalRocksCacheDb().beginTransaction()) {
			localRocksCacheTable.replace(t, encodeKey(key), ByteBuffer.encode(value));
			t.commit();
		} catch (Exception e) {
			logger.error("RocksCachePut exception:", e);
		}
	}

	final void rocksCacheRemove(@NotNull K key) {
		try (var t = getZeze().getLocalRocksCacheDb().beginTransaction()) {
			localRocksCacheTable.remove(t, encodeKey(key));
			t.commit();
		} catch (Exception e) {
			logger.error("RocksCacheRemove exception:", e);
		}
	}

	public @Nullable Supplier<ArrayList<TableX<K, V>>> getSimulateTables; // only for temp debug

	private void verifyGlobalRecordState(@NotNull K key, boolean isModify) { // only for temp debug
		var getSimulateTables = this.getSimulateTables;
		if (getSimulateTables == null)
			return;

		var checkState = isModify ? StateInvalid : StateShare;
		for (var table : getSimulateTables.get()) {
			if (table == this || table.cache == null)
				continue; // skip self
			var r = table.cache.get(key);
			int rs;
			if (r != null && (rs = r.getState()) > checkState && rs != StateRemoved) {
				logger.error("VerifyGlobalRecordState failed: serverId={}/{}, table={}, key={}, state={}, isModify={}",
						getZeze().getConfig().getServerId(), table.getZeze().getConfig().getServerId(),
						getName(), key, rs, isModify, new AssertionError());
				LogManager.shutdown();
				Runtime.getRuntime().halt(-1);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private @NotNull AtomicTupleRecord<K, V> load(@NotNull K key) {
		var tkey = new TableKey(getId(), key);
		while (true) {
			var r = cache.getOrAdd(key, () -> new Record1<>(this, key, null));
			r.enterFairLock(); // 对同一个记录，不允许重入。
			V strongRef = null;
			try {
				if (r.getState() == StateRemoved)
					continue; // 正在被删除，重新 GetOrAdd 一次。以后 _lock_check_ 里面会再次检查这个状态。

				if (r.getState() == StateShare
						|| r.getState() == StateModify) {
					var beforeTimestamp = r.getTimestamp(); // read timestamp before read value。see Record1.Commit
					strongRef = (V)r.getSoftValue();
					if (strongRef == null && !r.getDirty()) {
						var find = localRocksCacheTable.find(this, key);
						if (find != null) {
							strongRef = find;
							strongRef.initRootInfo(r.createRootInfoIfNeed(tkey), null);
							r.setSoftValue(strongRef);
						}
					}
					return new AtomicTupleRecord<>(r, strongRef, beforeTimestamp);
				}

				var acquire = r.acquire(StateShare, false, false);
				//noinspection DataFlowIssue
				r.setState(acquire.resultState);
				if (r.getState() == StateInvalid) {
					var txn = Transaction.getCurrent();
					if (txn == null || txn.isCompleted())
						throw new GoBackZeze("Acquire Failed: " + tkey + ':' + r);
					txn.throwRedoAndReleaseLock(tkey + ":" + r, null);
					// never run here
				}
				try {
					verifyGlobalRecordState(key, r.getState() == StateModify);

					r.setTimestamp(Record.getNextTimestamp());
					r.setFreshAcquire();
					var beforeTimestamp = r.getTimestamp();

					var storage = this.storage;
					if (storage != null) {
						PerfCounter.instance.getOrAddTableInfo(getId()).storageGet.increment();
						strongRef = storage.getDatabaseTable().find(this, key);
						if (strongRef != null) {
							rocksCachePut(key, strongRef);
						} else {
							rocksCacheRemove(key);
						}
						r.setSoftValue(strongRef); // r.Value still maybe null
						// 【注意】这个变量不管 OldTable 中是否存在的情况。
						r.setExistInBackDatabase(strongRef != null);

						// 当记录删除时需要同步删除 OldTable，否则下一次又会从 OldTable 中找到。
						// see Record1.Flush
						if (strongRef == null && null != oldTable) {
							var old = oldTable.find(this, key);
							if (old != null) {
								strongRef = old;
								r.setSoftValue(strongRef);
								// 从旧表装载时，马上设为脏，使得可以写入新表。
								// 否则直到被修改前，都不会被保存到当前数据库中。
								r.setDirty();
								// Immediately 需要特别在此单独处理。
								if (getZeze().getConfig().getCheckpointMode() == CheckpointMode.Immediately) {
									var lct = getZeze().getLocalRocksCacheDb().beginTransaction();
									//noinspection DataFlowIssue
									var t = oldTable.getDatabase().beginTransaction();
									try {
										oldTable.replace(t, key, old);
										localRocksCacheTable.replace(lct, key, old);
										lct.commit();
										t.commit();
									} catch (Throwable ex) { // logger.error
										logger.error("", ex);
										// rollback.
										lct.rollback();
										t.rollback();
									} finally {
										try {
											lct.close();
										} catch (Exception e) {
											logger.error("", e);
										}
										try {
											t.close();
										} catch (Exception e) {
											logger.error("", e);
										}
									}
								}
							}
						}
						if (strongRef != null)
							strongRef.initRootInfo(r.createRootInfoIfNeed(tkey), null);
					}
					if (isTraceEnabled)
						logger.trace("Load {}", r);
					return new AtomicTupleRecord<>(r, strongRef, beforeTimestamp);
				} catch (Throwable e) { // rethrow
					if (!r.getDirty())
						cache.remove(key, r); // 异常情况就从缓存中移除非dirty的记录,避免下次访问得到不正常的记录
					throw e;
				}
			} finally {
				r.exitFairLock();
			}
		}
	}

	@Override
	public final int reduceShare(@NotNull Reduce rpc, @NotNull ByteBuffer bbKey) {
		var fresh = rpc.getResultCode();
		rpc.setResultCode(0);
		rpc.Result.globalKey = rpc.Argument.globalKey;
		rpc.Result.state = rpc.Argument.state;
		if (isTraceEnabled)
			logger.trace("Reduce NewState={}", rpc);

		K key = decodeKey(bbKey);
		var lockey = getZeze().getLocks().get(new TableKey(getId(), key));
		lockey.enterWriteLock();
		try {
			var r = cache.get(key);
			if (isTraceEnabled)
				logger.trace("Reduce NewState={} {}", rpc.Argument.state, r);
			if (r == null) {
				rpc.Result.state = StateInvalid;
				if (isTraceEnabled)
					logger.trace("Reduce SendResult 1 r=null");
				rpc.SendResultCode(GlobalCacheManagerConst.ReduceShareAlreadyIsInvalid);
				return 0;
			}
			r.enterFairLock();
			try {
				if (fresh != GlobalCacheManagerConst.AcquireFreshSource && r.isFreshAcquire()) {
					if (isTraceEnabled)
						logger.trace("Reduce SendResult fresh {}", r);
					rpc.Result.state = GlobalCacheManagerConst.StateReduceErrorFreshAcquire;
					rpc.SendResult();
					return 0;
				}
				r.setNotFresh(); // 被降级不再新鲜。
				switch (r.getState()) {
				case StateRemoved: // impossible! safe only.
				case StateInvalid:
					rpc.Result.state = StateInvalid;
					rpc.Result.reducedTid = r.getTid();
					r.setTid(0);
					rpc.setResultCode(GlobalCacheManagerConst.ReduceShareAlreadyIsInvalid);

					if (r.getDirty())
						break;
					if (isTraceEnabled)
						logger.trace("Reduce SendResult 2 {}", r);
					rpc.SendResult();
					return 0;

				case StateShare:
					rpc.Result.state = StateShare;
					rpc.Result.reducedTid = r.getTid();
					r.setTid(0);
					rpc.setResultCode(GlobalCacheManagerConst.ReduceShareAlreadyIsShare);
					if (r.getDirty())
						break;
					if (isTraceEnabled)
						logger.trace("Reduce SendResult 3 {}", r);
					rpc.SendResult();
					return 0;

				case StateModify:
					r.setState(StateShare); // 马上修改状态。事务如果要写会再次请求提升(Acquire)。
					rpc.Result.state = StateShare;
					rpc.Result.reducedTid = r.getTid();
					r.setTid(0);
					if (r.getDirty())
						break;
					if (isTraceEnabled)
						logger.trace("Reduce SendResult * {}", r);
					rpc.SendResult();
					return 0;
				}
				// if (isDebugEnabled)
				// logger.warn("ReduceShare checkpoint begin. id={} {}", r, tkey);
				flushWhenReduce(r);
				if (isTraceEnabled)
					logger.trace("Reduce SendResult 4 {}", r);
				rpc.SendResult();
				// if (isDebugEnabled)
				// logger.warn("ReduceShare checkpoint end. id={} {}", r, tkey);
			} finally {
				r.exitFairLock();
			}
		} finally {
			lockey.exitWriteLock();
		}
		return 0;
	}

	private void flushWhenReduce(@NotNull Record r) {
		switch (getZeze().getConfig().getCheckpointMode()) {
		case Period:
			throw new IllegalStateException("Global Can Not Work With CheckpointMode.Period.");

		case Immediately:
			break;

		case Table:
			RelativeRecordSet.flushWhenReduce(r, getZeze().getCheckpoint());
			break;
		}
	}

	@Override
	public final int reduceInvalid(@NotNull Reduce rpc, @NotNull ByteBuffer bbKey) {
		var fresh = rpc.getResultCode();
		rpc.setResultCode(0);
		rpc.Result.globalKey = rpc.Argument.globalKey;
		rpc.Result.state = rpc.Argument.state;

		K key = decodeKey(bbKey);
		var lockey = getZeze().getLocks().get(new TableKey(getId(), key));
		var timeBegin = System.nanoTime();
		lockey.enterWriteLock();
		var timeNs = System.nanoTime() - timeBegin;
		if (timeNs >= 3_000_000_000L) {
			logger.warn("reduceInvalid wait lockey write lock too long! table={}, key={}, time={}ms",
					getName(), key, timeNs / 1_000_000);
		}
		try {
			var r = cache.get(key);
			if (isTraceEnabled)
				logger.trace("Reduce NewState={} {}", rpc.Argument.state, r);
			if (r == null) {
				rpc.Result.state = StateInvalid;
				if (isTraceEnabled)
					logger.trace("Reduce SendResult 1 r=null");
				rpc.SendResultCode(GlobalCacheManagerConst.ReduceInvalidAlreadyIsInvalid);
				return 0;
			}
			timeBegin = System.nanoTime();
			r.enterFairLock();
			timeNs = System.nanoTime() - timeBegin;
			if (timeNs >= 3_000_000_000L) {
				logger.warn("reduceInvalid wait record lock too long! table={}, key={}, time={}ms",
						getName(), key, timeNs / 1_000_000);
			}
			try {
				if (fresh != GlobalCacheManagerConst.AcquireFreshSource && r.isFreshAcquire()) {
					if (isTraceEnabled)
						logger.trace("Reduce SendResult fresh {}", r);
					rpc.Result.state = GlobalCacheManagerConst.StateReduceErrorFreshAcquire;
					rpc.SendResult();
					return 0;
				}
				r.setNotFresh(); // 被降级不再新鲜。
				switch (r.getState()) {
				case StateRemoved: // impossible! safe only.
				case StateInvalid:
					rpc.Result.state = StateInvalid;
					rpc.Result.reducedTid = r.getTid();
					r.setTid(0);
					rpc.setResultCode(GlobalCacheManagerConst.ReduceInvalidAlreadyIsInvalid);
					if (r.getDirty())
						break;
					if (isTraceEnabled)
						logger.trace("Reduce SendResult 2 {}", r);
					rpc.SendResult();
					return 0;

				case StateShare:
					r.setState(StateInvalid);
					rpc.Result.reducedTid = r.getTid();
					r.setTid(0);
					PerfCounter.instance.getOrAddTableInfo(getId()).reduceInvalid.increment();
					// 不删除记录，让TableCache.CleanNow处理。
					if (r.getDirty())
						break;
					if (isTraceEnabled)
						logger.trace("Reduce SendResult 3 {}", r);
					rpc.SendResult();
					return 0;

				case StateModify:
					r.setState(StateInvalid);
					rpc.Result.reducedTid = r.getTid();
					r.setTid(0);
					PerfCounter.instance.getOrAddTableInfo(getId()).reduceInvalid.increment();
					if (r.getDirty())
						break;
					if (isTraceEnabled)
						logger.trace("Reduce SendResult * {}", r);
					rpc.SendResult();
					return 0;
				}
				// if (isDebugEnabled)
				// logger.warn("ReduceInvalid checkpoint begin. id={} {}", r, tkey);
				rpc.Result.state = StateInvalid;
				flushWhenReduce(r);
				if (isTraceEnabled)
					logger.trace("Reduce SendResult 4 {}", r);
				rpc.SendResult();
				// if (isDebugEnabled)
				// logger.warn("ReduceInvalid checkpoint end. id={} {}", r, tkey);
			} finally {
				r.exitFairLock();
			}
		} finally {
			lockey.exitWriteLock();
		}
		return 0;
	}

	public final @NotNull Binary encodeGlobalKey(@NotNull K key) {
		var bbKey = encodeKey(key);
		var bb = ByteBuffer.Allocate(4 + bbKey.size());
		bb.WriteInt4(getId());
		bb.Append(bbKey.Bytes, bbKey.ReadIndex, bbKey.size());
		return new Binary(bb);
	}

	@Override
	final void reduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex) {
		var globalAgent = getZeze().getGlobalAgent();
		var locks = getZeze().getLocks();
		var remain = new ArrayList<KV<Lockey, Record1<K, V>>>(cache.getDataMap().size());
		logger.info("ReduceInvalidAllLocalOnly Table={} CacheSize={}", getName(), cache.getDataMap().size());
		cache.getDataMap().entrySet().parallelStream().forEach((e) -> {
			var k = e.getKey();
			var v = e.getValue();
			if (globalAgent.getGlobalCacheManagerHashIndex(encodeGlobalKey(k)) == GlobalCacheManagerHashIndex) {
				var lockey = locks.get(new TableKey(getId(), k));
				if (lockey.tryEnterWriteLock(0)) {
					try {
						if (v.tryEnterFairLock()) {
							try {
								// 只是需要设置Invalid，放弃资源，后面的所有访问都需要重新获取。
								v.setState(StateInvalid);
								// flushWhenReduce(v); // 改成使用全服checkpoint.
							} finally {
								v.exitFairLock();
							}
						} else {
							remain.add(KV.create(lockey, v));
						}
					} finally {
						lockey.exitWriteLock();
					}
				} else {
					remain.add(KV.create(lockey, v));
				}
			}
		});

		if (!remain.isEmpty()) {
			logger.info("ReduceInvalidAllLocalOnly Table={} Remain={}", getName(), remain.size());
			remain.parallelStream().forEach((e) -> {
				var k = e.getKey();
				k.enterWriteLock();
				try {
					var v = e.getValue();
					v.enterFairLock();
					try {
						v.setState(StateInvalid);
						// flushWhenReduce(v); // 改成使用全服checkpoint.
					} finally {
						v.exitFairLock();
					}
				} finally {
					k.exitWriteLock();
				}
			});
		}
		/*
		while (!remain.isEmpty()) {
			logger.info("ReduceInvalidAllLocalOnly Table={} Remain={}", getName(), remain.size());
			var remain2 = new ArrayList<KV<Lockey, Record1<K, V>>>(remain.size());
			for (var e : remain) {
				var k = e.getKey();
				if (!k.TryEnterWriteLock(0)) {
					remain2.add(e);
					continue;
				}
				try {
					var v = e.getValue();
					if (!v.TryEnterFairLock()) {
						remain2.add(e);
						continue;
					}
					try {
						v.setState(StateInvalid);
						FlushWhenReduce(v);
					} finally {
						v.ExitFairLock();
					}
				} finally {
					k.ExitWriteLock();
				}
			}
			remain = remain2;
		}
		*/
	}

	public final @Nullable V get(@NotNull K key) {
		var currentT = Transaction.getCurrent();
		assert currentT != null;
		//noinspection ConstantValue
		if (key == null)
			throw new IllegalArgumentException("key is null");

		var tkey = new TableKey(getId(), key);
		var cr = currentT.getRecordAccessed(tkey);
		if (cr != null) {
			@SuppressWarnings("unchecked")
			var r = (V)cr.newestValue();
			return r;
		}

		var r = load(key);
		var v = r.strongRef;
		currentT.addRecordAccessed(r.record.createRootInfoIfNeed(tkey), new RecordAccessed(r), v == null && isMemory());
		return v;
	}

	public final boolean contains(@NotNull K key) {
		return get(key) != null;
	}

	public final @NotNull V getOrAdd(@NotNull K key) {
		return getOrAdd(key, null);
	}

	public final @NotNull V getOrAdd(@NotNull K key, @Nullable OutObject<Boolean> isAdd) {
		var currentT = Transaction.getCurrent();
		assert currentT != null;
		//noinspection ConstantValue
		if (key == null)
			throw new IllegalArgumentException("key is null");

		var tkey = new TableKey(getId(), key);
		var cr = currentT.getRecordAccessed(tkey);
		if (cr != null) {
			@SuppressWarnings("unchecked")
			V crv = (V)cr.newestValue();
			if (crv != null)
				return crv;
			// add
		} else {
			var r = load(key);
			cr = new RecordAccessed(r);
			var v = r.strongRef;
			currentT.addRecordAccessed(r.record.createRootInfoIfNeed(tkey), cr, v == null && isMemory());
			if (v != null)
				return v;
			// add
		}
		if (null != isAdd)
			isAdd.value = true;
		V add = newValue();
		add.initRootInfo(cr.atomicTupleRecord.record.createRootInfoIfNeed(tkey), null);
		cr.put(currentT, add);
		return add;
	}

	public final boolean tryAdd(@NotNull K key, @NotNull V value) {
		//noinspection ConstantValue
		if (value == null)
			throw new IllegalArgumentException("value is null");
		if (get(key) != null)
			return false;

		var currentT = Transaction.getCurrent();
		assert currentT != null;

		var tkey = new TableKey(getId(), key);
		var cr = currentT.getRecordAccessed(tkey);
		//noinspection DataFlowIssue
		value.initRootInfoWithRedo(cr.atomicTupleRecord.record.createRootInfoIfNeed(tkey), null);
		cr.put(currentT, value);
		return true;
	}

	public final void insert(@NotNull K key, @NotNull V value) {
		if (!tryAdd(key, value)) {
			throw new IllegalArgumentException(String.format("table:%s insert key:%s exists",
					getClass().getName(), key));
		}
	}

	public final void put(@NotNull K key, @NotNull V value) {
		var currentT = Transaction.getCurrent();
		assert currentT != null;
		//noinspection ConstantValue
		if (key == null)
			throw new IllegalArgumentException("key is null");
		//noinspection ConstantValue
		if (value == null)
			throw new IllegalArgumentException("value is null");

		var tkey = new TableKey(getId(), key);
		var cr = currentT.getRecordAccessed(tkey);
		if (cr == null) {
			var r = load(key);
			cr = new RecordAccessed(r);
			currentT.addRecordAccessed(r.record.createRootInfoIfNeed(tkey), cr, r.strongRef == null && isMemory());
		}
		value.initRootInfoWithRedo(cr.atomicTupleRecord.record.createRootInfoIfNeed(tkey), null);
		cr.put(currentT, value);
	}

	@Override
	public void removeEncodedKey(@NotNull Binary encodedKey) {
		var key = decodeKey(ByteBuffer.Wrap(encodedKey));
		remove(key);
	}

	// 几乎和Put一样，还是独立开吧。
	public final void remove(@NotNull K key) {
		var currentT = Transaction.getCurrent();
		assert currentT != null;
		//noinspection ConstantValue
		if (key == null)
			throw new IllegalArgumentException("key is null");

		var tkey = new TableKey(getId(), key);
		var cr = currentT.getRecordAccessed(tkey);
		if (cr != null) {
			cr.put(currentT, null);
			return;
		}

		var r = load(key);
		cr = new RecordAccessed(r);
		currentT.addRecordAccessed(r.record.createRootInfoIfNeed(tkey), cr, r.strongRef == null && isMemory());
		cr.put(currentT, null);
	}

	@Override
	public final @Nullable Storage<?, ?> open(@NotNull Application app, @NotNull Database database,
											  @Nullable DatabaseRocksDb.Table localTable) {
		if (cache != null)
			throw new IllegalStateException("table has opened: " + getName());

		setZeze(app);
		setDatabase(database);

		if (isAutoKey())
			autoKey = app.getServiceManager().getAutoKey(getName());

		setTableConf(app.getConfig().getTableConf(getName()));
		cache = new TableCache<>(app, this);
		relationalTable = getZeze().getSchemas().relationalTables.get(getName()); // maybe null
		storage = isMemory() ? null : new Storage<>(this, database, getName());
		oldTable = getTableConf().getDatabaseOldMode() == 1
				? app.getDatabase(getTableConf().getDatabaseOldName()).openTable(getName(), getId()) : null;
		localRocksCacheTable = localTable != null ? localTable : app.getLocalRocksCacheDb().openTable(getName(), getId());
		useRelationalMapping = isRelationalMapping() && database instanceof DatabaseMySql;
		return storage;
	}

	@Override
	final void close() {
		var st = storage;
		if (st != null) {
			st.close();
			storage = null;
		}
		var ca = cache;
		if (ca != null) {
			ca.close();
			cache = null;
		}
	}

	// Key 都是简单变量，系列化方法都不一样，需要生成。
	public abstract @NotNull ByteBuffer encodeKey(@NotNull K key);

	@Override
	@SuppressWarnings("unchecked")
	public @NotNull ByteBuffer encodeKey(@NotNull Object key) {
		return encodeKey((K)key);
	}

	public @NotNull K decodeKey(byte @NotNull [] bytes) {
		int keyOffset = keyOffsetInRawKey();
		return decodeKey(ByteBuffer.Wrap(bytes, keyOffset, bytes.length - keyOffset));
	}

	@Override
	public abstract @NotNull K decodeKey(@NotNull ByteBuffer bb);

	public abstract @NotNull K decodeKeyResultSet(@NotNull ResultSet rs) throws SQLException;

	public abstract void encodeKeySQLStatement(@NotNull SQLStatement st, @NotNull K _v_);

	private Schemas.RelationalTable relationalTable;

	@Override
	public Schemas.RelationalTable getRelationalTable() {
		return relationalTable;
	}

	@SuppressWarnings("unchecked")
	public void encodeKeySQLStatement(@NotNull SQLStatement st, @NotNull Object _v_) {
		encodeKeySQLStatement(st, (K)_v_);
	}

	public final void delayRemove(@NotNull K key) {
		getZeze().getDelayRemove().remove(this, key);
	}

	@Override
	public abstract @NotNull V newValue();

	/**
	 * 解码系列化的数据到对象。
	 *
	 * @param bb bean encoded data
	 * @return Value
	 */
	public final @NotNull V decodeValue(@NotNull ByteBuffer bb) {
		V value = newValue();
		value.decode(bb);
		return value;
	}

	public final @NotNull V decodeValue(byte @NotNull [] bytes) {
		return decodeValue(ByteBuffer.Wrap(bytes));
	}

	public long getDatabaseSize() {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().getSize();
	}

	public long getDatabaseSizeApproximation() {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().getSizeApproximation();
	}

	public final K walk(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandle<K, V> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walk(this, exclusiveStartKey, proposeLimit, callback);
	}

	public final K walkDesc(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandle<K, V> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkDesc(this, exclusiveStartKey, proposeLimit, callback);
	}

	public final K walkKey(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkKey<K> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkKey(this, exclusiveStartKey, proposeLimit, callback);
	}

	public final K walkKeyDesc(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkKey<K> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkKeyDesc(this, exclusiveStartKey, proposeLimit, callback);
	}

	/**
	 * 事务外调用
	 * 遍历表格。能看到记录的最新数据。
	 * 【注意】这里看不到新增的但没有提交(checkpoint)的记录。实现这个有点麻烦。
	 * 【并发】每个记录回调时加读锁，回调完成马上释放。
	 *
	 * @param callback walk callback
	 * @return count
	 */
	public final long walk(@NotNull TableWalkHandle<K, V> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walk(this, callback);
	}

	public final long walkDesc(@NotNull TableWalkHandle<K, V> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkDesc(this, callback);
	}

	public final long walkKey(@NotNull TableWalkKey<K> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkKey(this, callback);
	}

	public final long walkKeyDesc(@NotNull TableWalkKey<K> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkKeyDesc(this, callback);
	}

	public final long walkCacheKey(@NotNull TableWalkKey<K> callback) {
		return cache.walkKey(callback);
	}

	/**
	 * 遍历数据库中的表。看不到本地缓存中的数据。
	 * 【并发】后台数据库处理并发。
	 *
	 * @param callback walk callback
	 * @return count
	 */
	public final long walkDatabaseRaw(@NotNull TableWalkHandleRaw callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		if (storage.getDatabaseTable() instanceof Database.AbstractKVTable)
			return ((Database.AbstractKVTable)storage.getDatabaseTable()).walk(callback);
		throw new UnsupportedOperationException("Not A KV Table.");
	}

	public final long walkDatabaseRawDesc(@NotNull TableWalkHandleRaw callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		if (storage.getDatabaseTable() instanceof Database.AbstractKVTable)
			return ((Database.AbstractKVTable)storage.getDatabaseTable()).walkDesc(callback);
		throw new UnsupportedOperationException("Not A KV Table.");
	}

	public final long walkDatabaseRawKey(@NotNull TableWalkKeyRaw callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		if (storage.getDatabaseTable() instanceof Database.AbstractKVTable)
			return ((Database.AbstractKVTable)storage.getDatabaseTable()).walkKey(callback);
		throw new UnsupportedOperationException("Not A KV Table.");
	}

	public final long walkDatabaseRawKeyDesc(@NotNull TableWalkKeyRaw callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		if (storage.getDatabaseTable() instanceof Database.AbstractKVTable)
			return ((Database.AbstractKVTable)storage.getDatabaseTable()).walkKeyDesc(callback);
		throw new UnsupportedOperationException("Not A KV Table.");
	}

	public final ByteBuffer walkDatabaseRaw(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											@NotNull TableWalkHandleRaw callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		if (storage.getDatabaseTable() instanceof Database.AbstractKVTable)
			return ((Database.AbstractKVTable)storage.getDatabaseTable()).walk(
					exclusiveStartKey, proposeLimit, callback);
		throw new UnsupportedOperationException("Not A KV Table.");
	}

	public final ByteBuffer walkDatabaseRawDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												@NotNull TableWalkHandleRaw callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		if (storage.getDatabaseTable() instanceof Database.AbstractKVTable)
			return ((Database.AbstractKVTable)storage.getDatabaseTable()).walkDesc(
					exclusiveStartKey, proposeLimit, callback);
		throw new UnsupportedOperationException("Not A KV Table.");
	}

	public final ByteBuffer walkDatabaseRawKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											   @NotNull TableWalkKeyRaw callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		if (storage.getDatabaseTable() instanceof Database.AbstractKVTable)
			return ((Database.AbstractKVTable)storage.getDatabaseTable()).walkKey(
					exclusiveStartKey, proposeLimit, callback);
		throw new UnsupportedOperationException("Not A KV Table.");
	}

	public final ByteBuffer walkDatabaseRawKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												   @NotNull TableWalkKeyRaw callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		if (storage.getDatabaseTable() instanceof Database.AbstractKVTable)
			return ((Database.AbstractKVTable)storage.getDatabaseTable()).walkKeyDesc(
					exclusiveStartKey, proposeLimit, callback);
		throw new UnsupportedOperationException("Not A KV Table.");
	}

	/**
	 * 遍历数据库中的表。看不到本地缓存中的数据。
	 * 【并发】后台数据库处理并发。
	 *
	 * @param callback walk callback
	 * @return count
	 */
	public final long walkDatabase(@NotNull TableWalkHandle<K, V> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkDatabase(this, callback);
	}

	public final long walkDatabaseDesc(@NotNull TableWalkHandle<K, V> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkDatabaseDesc(this, callback);
	}

	public final long walkDatabaseKey(@NotNull TableWalkKey<K> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkDatabaseKey(this, callback);
	}

	public final long walkDatabaseKeyDesc(@NotNull TableWalkKey<K> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkDatabaseKeyDesc(this, callback);
	}

	public final K walkDatabase(@Nullable K exclusiveStartKey, int proposeLimit,
								@NotNull TableWalkHandle<K, V> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkDatabase(this, exclusiveStartKey, proposeLimit, callback);
	}

	public final K walkDatabaseDesc(@Nullable K exclusiveStartKey, int proposeLimit,
									@NotNull TableWalkHandle<K, V> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkDatabaseDesc(this, exclusiveStartKey, proposeLimit, callback);
	}

	public final K walkDatabaseKey(@Nullable K exclusiveStartKey, int proposeLimit,
								   @NotNull TableWalkKey<K> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkDatabaseKey(this, exclusiveStartKey, proposeLimit, callback);
	}

	public final K walkDatabaseKeyDesc(@Nullable K exclusiveStartKey, int proposeLimit,
									   @NotNull TableWalkKey<K> callback) {
		var storage = this.storage;
		if (storage == null)
			throw new IllegalStateException("storage is in-memory or closed");
		return storage.getDatabaseTable().walkDatabaseKeyDesc(this, exclusiveStartKey, proposeLimit, callback);
	}

	@Override
	public long walkMemoryAny(TableWalkHandle<Object, Bean> handle) {
		return walkMemory(handle::handle);
	}

	@Override
	public void __direct_put_cache__(Object key, Bean value, int state) {
		@SuppressWarnings("unchecked")
		var kk = (K)key;
		var tKey = new TableKey(getId(), key);
		var r = cache.getOrAdd(kk, () -> new Record1<>(this, kk, null));
		value.initRootInfo(r.createRootInfoIfNeed(tKey), null);
		r.setState(state);
		r.setSoftValue(value);
		r.setTimestamp(Record.getNextTimestamp()); // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。
		r.setDirty(); // 这个目前仅由内存表使用，本来不需要调用这个。
		//logger.info("__direct_put_cache__ " + key + ", " + value);
	}

	/**
	 * 事务外调用
	 * 遍历缓存
	 *
	 * @return count
	 */
	public final long walkMemory(@NotNull TableWalkHandle<K, V> callback) {
		if (Transaction.getCurrent() != null)
			throw new IllegalStateException("must be called without transaction");
		// 还是先不限制，可以用于特殊地方。
		//if (storage != null)
		//	throw new IllegalStateException("this is not a memory table.");

		int count = 0;
		for (var entry : cache.getDataMap().entrySet()) {
			var r = entry.getValue();
			var v = cacheCopy(r);
			if (null != v) {
				count++;
				if (!callback.handle(r.getObjectKey(), v))
					break;
			}
		}
		return count;
	}

	private V cacheCopy(Record1<K, V> r) {
		r.enterFairLock();
		try {
			// 这个条件表示本地拥有读或写状态的才能遍历到。对于内存表，能看到全部。
			if (r.getState() == StateShare || r.getState() == StateModify) {
				@SuppressWarnings("unchecked")
				var strongRef = (V)r.getSoftValue();
				if (strongRef == null) {
					strongRef = localRocksCacheTable.find(this, r.getObjectKey());
					if (strongRef == null)
						return null;
					// 被交换出去的记录，装载以后临时用，不保存下来。
					//strongRef.initRootInfo(r.createRootInfoIfNeed(tKey), null);
					//r.setSoftValue(strongRef);
				}
				@SuppressWarnings("unchecked")
				var v = (V)strongRef.copy();
				return v;
			}
			return null;
		} finally {
			r.exitFairLock();
		}
	}

	/**
	 * 获得记录的拷贝。
	 * 1. 一般在事务外使用，不能在WhileCommit中访问事务中没访问过的记录，以防死锁。
	 * 2. 如果在事务内使用：
	 * a)已经访问过的记录，得到最新值的拷贝。不建议这种用法。
	 * b)没有访问过的记录，从后台查询并拷贝，但不会加入RecordAccessed。
	 * 3. 得到的结果一般不用于修改，应用传递时可以使用ReadOnly接口修饰保护一下。
	 *
	 * @param key record key
	 * @return record value
	 */
	@SuppressWarnings("unchecked")
	public final @Nullable V selectCopy(@NotNull K key) {
		//noinspection ConstantValue
		if (key == null)
			throw new IllegalArgumentException("key is null");
		var tkey = new TableKey(getId(), key);
		var currentT = Transaction.getCurrent();
		if (currentT != null) {
			var cr = currentT.getRecordAccessed(tkey);
			if (cr != null) {
				var v = cr.newestValue();
				return v != null ? (V)v.copy() : null;
			}
//			if (currentT.isCompleted())
//				throw new IllegalStateException("completed transaction can not selectCopy record not accessed");
			currentT.setAlwaysReleaseLockWhenRedo();
		}

		Lockey lockey = null;
		for (int tryCount = 0; ; tryCount++) {
			try {
				V v;
				if (isMemory()) {
					var r = cache.get(key);
					if (r == null)
						return null;
					r.enterFairLock();
					try {
						return r.copyValue();
					} finally {
						r.exitFairLock();
					}
				}
				v = load(key).strongRef;
				if (v == null)
					return null;
				if (lockey == null)
					lockey = getZeze().getLocks().get(tkey);
				if (lockey.tryEnterReadLock(0)) {
					try {
						return (V)v.copy();
					} finally {
						lockey.exitReadLock();
					}
				}
				return localRocksCacheTable.find(this, key);
			} catch (GoBackZeze e) {
				if (currentT != null && currentT.isRunning() || tryCount >= 256)
					throw e;
			}
			try {
				//noinspection BusyWait
				Thread.sleep(Random.getInstance().nextInt(10) + 5);
			} catch (InterruptedException e) {
				logger.error("", e);
			}
		}
	}

	/**
	 * @see TableX#selectDirty(K, int)
	 */
	public final @Nullable V selectDirty(@NotNull K key) {
		return selectDirty(key, 3_000);
	}

	/**
	 * 通常用于事务外(包括whileCommit)访问记录,不需要从global服务获取权限,得到的值不保证一致性,有本地缓存但可控制其有效时间
	 *
	 * @param cacheTTL 可以接受读取本地缓存的有效时长(毫秒),避免读取过旧的值,0表示总是直接从数据库中取最新值,默认3秒
	 */
	@SuppressWarnings("unchecked")
	public final @Nullable V selectDirty(@NotNull K key, int cacheTTL) {
		//noinspection ConstantValue
		if (key == null)
			throw new IllegalArgumentException("key is null");
		var tkey = new TableKey(getId(), key);
		var currentT = Transaction.getCurrent();
		if (currentT != null) {
			var cr = currentT.getRecordAccessed(tkey);
			if (cr != null)
				return (V)cr.newestValue();
		}

		if (isMemory()) {
			var r = cache.get(key);
			if (r == null)
				return null;
			r.enterFairLock();
			try {
				return r.loadValue();
			} finally {
				r.exitFairLock();
			}
		}

		while (true) {
			var r = cache.getOrAdd(key, () -> new Record1<>(this, key, null));
			r.enterFairLock(); // 对同一个记录，不允许重入。
			try {
				if (r.getState() == StateRemoved)
					continue; // 正在被删除，重新 GetOrAdd 一次。以后 _lock_check_ 里面会再次检查这个状态。
				var storage = this.storage;
				if (r.getState() == StateInvalid && storage != null) {
					var now = System.currentTimeMillis();
					var ts = r.getTimestamp();
					if (ts >= 0 || now + ts >= cacheTTL) { // 距上次selectDirty超过5秒则从数据库里加载最新值
						PerfCounter.instance.getOrAddTableInfo(getId()).storageGet.increment();
						V strongRef = storage.getDatabaseTable().find(this, key);
						r.setSoftValue(strongRef); // r.Value still maybe null
						// 【注意】这个变量不管 OldTable 中是否存在的情况。
						r.setExistInBackDatabase(strongRef != null);
						if (strongRef != null) {
							rocksCachePut(key, strongRef);
							strongRef.initRootInfo(r.createRootInfoIfNeed(tkey), null);
						} else
							rocksCacheRemove(key);
						r.setTimestamp(-now);
						if (isTraceEnabled)
							logger.trace("LoadDirty {}", r);
						return strongRef;
					}
				}
				return r.loadValue();
			} finally {
				r.exitFairLock();
			}
		}
	}

	@Override
	public final boolean isNew() {
		var storage = this.storage;
		return storage == null // memory table always return true
				|| storage.getDatabaseTable().isNew();
	}

	/**
	 * 这个方法用来编码服务器的ChangeListener，
	 * 客户端解码参见 c# Zeze.Transaction.ChangesRecord。
	 *
	 * @param specialName special name, use table.Name if null.
	 * @param key         Object Key From ChangeListener
	 * @param r           Changes.Record From ChangeListener
	 * @return ByteBuffer Encoded Change Log
	 */
	public @NotNull ByteBuffer encodeChangeListenerWithSpecialName(@Nullable String specialName, @NotNull Object key,
																   @NotNull Changes.Record r) {
		var bb = ByteBuffer.Allocate();
		bb.WriteString(null == specialName ? getName() : specialName);
		bb.WriteByteBuffer(encodeKey(key));
		r.encode(bb);
		return bb;
	}

	@Override
	public void tryAlter() {
		var storage = this.storage;
		var dbTable = storage != null ? storage.getDatabaseTable() : null;
		if (dbTable instanceof DatabaseMySql.TableMysqlRelational)
			((DatabaseMySql.TableMysqlRelational)dbTable).tryAlter();
	}

	/**
	 * 内部方法，必须在checkpoint之后并且没有正在执行的事务才是安全的。
	 */
	public void __ClearTableCacheUnsafe__() {
		//System.out.println(getName() + " __ClearTableCacheUnsafe__");
		// 直接new一个更加干净。
		cache = new TableCache<>(getZeze(), this);
	}
}
