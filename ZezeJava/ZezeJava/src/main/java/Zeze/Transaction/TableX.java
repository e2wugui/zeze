package Zeze.Transaction;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.function.Supplier;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Util.KV;
import Zeze.Util.Macro;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static Zeze.Services.GlobalCacheManagerConst.StateInvalid;
import static Zeze.Services.GlobalCacheManagerConst.StateModify;
import static Zeze.Services.GlobalCacheManagerConst.StateRemoved;
import static Zeze.Services.GlobalCacheManagerConst.StateShare;

public abstract class TableX<K extends Comparable<K>, V extends Bean> extends Table {
	private static final Logger logger = LogManager.getLogger(TableX.class);
	private static final boolean isDebugEnabled = logger.isDebugEnabled();

	private AutoKey autoKey;
	private TableCache<K, V> cache;
	private Storage<K, V> storage;
	private Database.Table oldTable;
	private DatabaseRocksDb.Table localRocksCacheTable;

	public TableX(String name) {
		super(name);
	}

	TableCache<K, V> getCache() {
		return cache;
	}

	protected final AutoKey getAutoKey() {
		return autoKey;
	}

	public final Storage<K, V> internalGetStorageForTestOnly(String IAmSure) {
		if (!IAmSure.equals("IKnownWhatIAmDoing"))
			throw new IllegalArgumentException();
		return storage;
	}

	@Override
	final Storage<K, V> getStorage() {
		return storage;
	}

	@Override
	final Database.Table getOldTable() {
		return oldTable;
	}

	final DatabaseRocksDb.Table getLocalRocksCacheTable() {
		return localRocksCacheTable;
	}

	public final int getCacheSize() {
		return cache != null ? cache.getDataMap().size() : 0;
	}

	final void rocksCachePut(K key, V value) {
		try (var t = getZeze().getLocalRocksCacheDb().beginTransaction()) {
			localRocksCacheTable.replace(t, encodeKey(key), ByteBuffer.encode(value));
			t.commit();
		} catch (Exception e) {
			logger.error("RocksCachePut exception:", e);
		}
	}

	final void rocksCacheRemove(K key) {
		try (var t = getZeze().getLocalRocksCacheDb().beginTransaction()) {
			localRocksCacheTable.remove(t, encodeKey(key));
			t.commit();
		} catch (Exception e) {
			logger.error("RocksCacheRemove exception:", e);
		}
	}

	public Supplier<ArrayList<TableX<K, V>>> getSimulateTables; // only for temp debug

	private void verifyGlobalRecordState(K key, boolean isModify) { // only for temp debug
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
						getName(), key, rs, isModify);
				LogManager.shutdown();
				Runtime.getRuntime().halt(-1);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private AtomicTupleRecord<K, V> load(K key) {
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
				r.setState(acquire.resultState);
				if (r.getState() == StateInvalid) {
					var txn = Transaction.getCurrent();
					if (txn == null)
						throw new IllegalStateException("Acquire Failed");
					txn.throwRedoAndReleaseLock(tkey + ":" + r, null);
				}
				verifyGlobalRecordState(key, r.getState() == StateModify);

				r.setTimestamp(Record.getNextTimestamp());
				r.setFreshAcquire();
				var beforeTimestamp = r.getTimestamp();

				if (storage != null) {
					if (Macro.enableStatistics) {
						TableStatistics.getInstance().getOrAdd(getId()).getStorageFindCount().increment();
					}
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
								var t = getOldTable().getDatabase().beginTransaction();
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
				if (isDebugEnabled)
					logger.debug("Load {}", r);
				return new AtomicTupleRecord<>(r, strongRef, beforeTimestamp);
			} finally {
				r.exitFairLock();
			}
		}
	}

	@Override
	public final int reduceShare(Reduce rpc, ByteBuffer bbKey) {
		var fresh = rpc.getResultCode();
		rpc.setResultCode(0);
		rpc.Result.globalKey = rpc.Argument.globalKey;
		rpc.Result.state = rpc.Argument.state;
		if (isDebugEnabled)
			logger.debug("Reduce NewState={}", rpc);

		K key = decodeKey(bbKey);
		var lockey = getZeze().getLocks().get(new TableKey(getId(), key));
		lockey.enterWriteLock();
		try {
			var r = cache.get(key);
			if (isDebugEnabled)
				logger.debug("Reduce NewState={} {}", rpc.Argument.state, r);
			if (r == null) {
				rpc.Result.state = StateInvalid;
				if (isDebugEnabled)
					logger.debug("Reduce SendResult 1 r=null");
				rpc.SendResultCode(GlobalCacheManagerConst.ReduceShareAlreadyIsInvalid);
				return 0;
			}
			r.enterFairLock();
			try {
				if (fresh != GlobalCacheManagerConst.AcquireFreshSource && r.isFreshAcquire()) {
					if (isDebugEnabled)
						logger.debug("Reduce SendResult fresh {}", r);
					rpc.Result.state = GlobalCacheManagerConst.StateReduceErrorFreshAcquire;
					rpc.SendResult();
					return 0;
				}
				r.setNotFresh(); // 被降级不再新鲜。
				switch (r.getState()) {
				case StateRemoved: // impossible! safe only.
				case StateInvalid:
					rpc.Result.state = StateInvalid;
					rpc.setResultCode(GlobalCacheManagerConst.ReduceShareAlreadyIsInvalid);

					if (r.getDirty())
						break;
					if (isDebugEnabled)
						logger.debug("Reduce SendResult 2 {}", r);
					rpc.SendResult();
					return 0;

				case StateShare:
					rpc.Result.state = StateShare;
					rpc.setResultCode(GlobalCacheManagerConst.ReduceShareAlreadyIsShare);
					if (r.getDirty())
						break;
					if (isDebugEnabled)
						logger.debug("Reduce SendResult 3 {}", r);
					rpc.SendResult();
					return 0;

				case StateModify:
					r.setState(StateShare); // 马上修改状态。事务如果要写会再次请求提升(Acquire)。
					rpc.Result.state = StateShare;
					if (r.getDirty())
						break;
					if (isDebugEnabled)
						logger.debug("Reduce SendResult * {}", r);
					rpc.SendResult();
					return 0;
				}
				// if (isDebugEnabled)
				// logger.warn("ReduceShare checkpoint begin. id={} {}", r, tkey);
				flushWhenReduce(r);
				if (isDebugEnabled)
					logger.debug("Reduce SendResult 4 {}", r);
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

	private void flushWhenReduce(Record r) {
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
	public final int reduceInvalid(Reduce rpc, ByteBuffer bbKey) {
		var fresh = rpc.getResultCode();
		rpc.setResultCode(0);
		rpc.Result.globalKey = rpc.Argument.globalKey;
		rpc.Result.state = rpc.Argument.state;

		K key = decodeKey(bbKey);
		var lockey = getZeze().getLocks().get(new TableKey(getId(), key));
		lockey.enterWriteLock();
		try {
			var r = cache.get(key);
			if (isDebugEnabled)
				logger.debug("Reduce NewState={} {}", rpc.Argument.state, r);
			if (r == null) {
				rpc.Result.state = StateInvalid;
				if (isDebugEnabled)
					logger.debug("Reduce SendResult 1 r=null");
				rpc.SendResultCode(GlobalCacheManagerConst.ReduceInvalidAlreadyIsInvalid);
				return 0;
			}
			r.enterFairLock();
			try {
				if (fresh != GlobalCacheManagerConst.AcquireFreshSource && r.isFreshAcquire()) {
					if (isDebugEnabled)
						logger.debug("Reduce SendResult fresh {}", r);
					rpc.Result.state = GlobalCacheManagerConst.StateReduceErrorFreshAcquire;
					rpc.SendResult();
					return 0;
				}
				r.setNotFresh(); // 被降级不再新鲜。
				switch (r.getState()) {
				case StateRemoved: // impossible! safe only.
				case StateInvalid:
					rpc.Result.state = StateInvalid;
					rpc.setResultCode(GlobalCacheManagerConst.ReduceInvalidAlreadyIsInvalid);
					if (r.getDirty())
						break;
					if (isDebugEnabled)
						logger.debug("Reduce SendResult 2 {}", r);
					rpc.SendResult();
					return 0;

				case StateShare:
					r.setState(StateInvalid);
					// 不删除记录，让TableCache.CleanNow处理。
					if (r.getDirty())
						break;
					if (isDebugEnabled)
						logger.debug("Reduce SendResult 3 {}", r);
					rpc.SendResult();
					return 0;

				case StateModify:
					r.setState(StateInvalid);
					if (r.getDirty())
						break;
					if (isDebugEnabled)
						logger.debug("Reduce SendResult * {}", r);
					rpc.SendResult();
					return 0;
				}
				// if (isDebugEnabled)
				// logger.warn("ReduceInvalid checkpoint begin. id={} {}", r, tkey);
				rpc.Result.state = StateInvalid;
				flushWhenReduce(r);
				if (isDebugEnabled)
					logger.debug("Reduce SendResult 4 {}", r);
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

	public final Binary encodeGlobalKey(K key) {
		var bb = ByteBuffer.Allocate();
		bb.WriteInt4(getId());
		var bbKey = encodeKey(key);
		bb.Append(bbKey.Bytes, bbKey.ReadIndex, bbKey.Size());
		return new Binary(bb);
	}

	@Override
	final void reduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex) {
		var globalAgent = getZeze().getGlobalAgent();
		var locks = getZeze().getLocks();
		var remain = new ArrayList<KV<Lockey, Record1<K, V>>>(cache.getDataMap().size());
		logger.info("ReduceInvalidAllLocalOnly CacheSize=" + cache.getDataMap().size());
		for (var e : cache.getDataMap().entrySet()) {
			var k = e.getKey();
			if (globalAgent.getGlobalCacheManagerHashIndex(encodeGlobalKey(k)) != GlobalCacheManagerHashIndex)
				continue;

			var v = e.getValue();
			var lockey = locks.get(new TableKey(getId(), k));
			if (!lockey.tryEnterWriteLock(0)) {
				remain.add(KV.create(lockey, v));
				continue;
			}
			try {
				if (!v.tryEnterFairLock()) {
					remain.add(KV.create(lockey, v));
					continue;
				}
				try {
					// 只是需要设置Invalid，放弃资源，后面的所有访问都需要重新获取。
					v.setState(StateInvalid);
					flushWhenReduce(v);
				} finally {
					v.exitFairLock();
				}
			} finally {
				lockey.exitWriteLock();
			}
		}

		if (!remain.isEmpty()) {
			logger.info("ReduceInvalidAllLocalOnly Remain=" + remain.size());
			for (var e : remain) {
				var k = e.getKey();
				k.enterWriteLock();
				try {
					var v = e.getValue();
					v.enterFairLock();
					try {
						v.setState(StateInvalid);
						flushWhenReduce(v);
					} finally {
						v.exitFairLock();
					}
				} finally {
					k.exitWriteLock();
				}
			}
		}
		/*
		while (!remain.isEmpty()) {
			logger.info("ReduceInvalidAllLocalOnly Remain=" + remain.size());
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

	public final V get(K key) {
		var currentT = Transaction.getCurrent();
		assert currentT != null;
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
		currentT.addRecordAccessed(r.record.createRootInfoIfNeed(tkey), new RecordAccessed(r));
		return r.strongRef;
	}

	public final boolean contains(K key) {
		return get(key) != null;
	}

	public final V getOrAdd(K key) {
		var currentT = Transaction.getCurrent();
		assert currentT != null;
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
			currentT.addRecordAccessed(r.record.createRootInfoIfNeed(tkey), cr);
			if (r.strongRef != null)
				return r.strongRef;
			// add
		}

		V add = newValue();
		add.initRootInfo(cr.atomicTupleRecord.record.createRootInfoIfNeed(tkey), null);
		cr.put(currentT, add);
		return add;
	}

	public final boolean tryAdd(K key, V value) {
		if (value == null)
			throw new IllegalArgumentException("value is null");
		if (get(key) != null)
			return false;

		var currentT = Transaction.getCurrent();
		assert currentT != null;

		var tkey = new TableKey(getId(), key);
		var cr = currentT.getRecordAccessed(tkey);
		value.initRootInfoWithRedo(cr.atomicTupleRecord.record.createRootInfoIfNeed(tkey), null);
		cr.put(currentT, value);
		return true;
	}

	public final void insert(K key, V value) {
		if (!tryAdd(key, value)) {
			throw new IllegalArgumentException(String.format("table:%s insert key:%s exists",
					getClass().getName(), key));
		}
	}

	public final void put(K key, V value) {
		var currentT = Transaction.getCurrent();
		assert currentT != null;
		if (key == null)
			throw new IllegalArgumentException("key is null");
		if (value == null)
			throw new IllegalArgumentException("value is null");

		var tkey = new TableKey(getId(), key);
		var cr = currentT.getRecordAccessed(tkey);
		if (cr == null) {
			var r = load(key);
			cr = new RecordAccessed(r);
			currentT.addRecordAccessed(r.record.createRootInfoIfNeed(tkey), cr);
		}
		value.initRootInfoWithRedo(cr.atomicTupleRecord.record.createRootInfoIfNeed(tkey), null);
		cr.put(currentT, value);
	}

	@Override
	public void removeEncodedKey(Binary encodedKey) {
		var key = decodeKey(ByteBuffer.Wrap(encodedKey));
		remove(key);
	}

	// 几乎和Put一样，还是独立开吧。
	public final void remove(K key) {
		var currentT = Transaction.getCurrent();
		assert currentT != null;
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
		cr.put(currentT, null);
		currentT.addRecordAccessed(r.record.createRootInfoIfNeed(tkey), cr);
	}

	@Override
	final Storage<?, ?> open(Application app, Database database) {
		if (storage != null)
			throw new IllegalStateException("table has opened: " + getName());

		setZeze(app);
		setDatabase(database);

		if (isAutoKey())
			autoKey = app.getServiceManager().getAutoKey(getName());

		setTableConf(app.getConfig().getTableConf(getName()));
		cache = new TableCache<>(app, this);
		storage = isMemory() ? null : new Storage<>(this, database, getName());
		oldTable = getTableConf().getDatabaseOldMode() == 1
				? app.getDatabase(getTableConf().getDatabaseOldName()).openTable(getName()) : null;
		localRocksCacheTable = app.getLocalRocksCacheDb().openTable(getName());
		return storage;
	}

	@Override
	final void close() {
		if (storage != null) {
			storage.close();
			storage = null;
		}
		if (cache != null) {
			cache.close();
			cache = null;
		}
	}

	// Key 都是简单变量，系列化方法都不一样，需要生成。
	public abstract ByteBuffer encodeKey(K key);

	@SuppressWarnings("unchecked")
	public ByteBuffer encodeKey(Object key) {
		return encodeKey((K)key);
	}

	public abstract K decodeKey(ByteBuffer bb);
	public abstract K decodeKeyResultSet(ResultSet rs) throws java.sql.SQLException;
	public abstract void encodeKeySQLStatement(SQLStatement st, K _v_);

	public final void delayRemove(K key) {
		getZeze().getDelayRemove().remove(this, key);
	}

	@Override
	public abstract V newValue();

	/**
	 * 解码系列化的数据到对象。
	 *
	 * @param bb bean encoded data
	 * @return Value
	 */
	public final V decodeValue(ByteBuffer bb) {
		V value = newValue();
		value.decode(bb);
		return value;
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
	public final long walk(TableWalkHandle<K, V> callback) {
		return walk(callback, null);
	}

	public final K walk(K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback) {
		return walk(exclusiveStartKey, proposeLimit, callback, null);
	}

	public final K walk(K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback, Runnable afterLock) {
		return storage.getDatabaseTable().walk(this, exclusiveStartKey, proposeLimit, callback, afterLock);
	}

	public final K walkDesc(K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback) {
		return walkDesc(exclusiveStartKey, proposeLimit, callback, null);
	}

	public final K walkDesc(K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback, Runnable afterLock) {
		return storage.getDatabaseTable().walkDesc(this, exclusiveStartKey, proposeLimit, callback, afterLock);
	}

	public final K walkKey(K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback) {
		return walkKey(exclusiveStartKey, proposeLimit, callback, null);
	}

	public final K walkKey(K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback, Runnable afterLock) {
		return storage.getDatabaseTable().walkKey(this, exclusiveStartKey, proposeLimit, callback, afterLock);
	}

	public final K walkKeyDesc(K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback) {
		return walkKeyDesc(exclusiveStartKey, proposeLimit, callback, null);
	}

	public final K walkKeyDesc(K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback, Runnable afterLock) {
		return storage.getDatabaseTable().walkKeyDesc(this, exclusiveStartKey, proposeLimit, callback, afterLock);
	}

	public final long walk(TableWalkHandle<K, V> callback, Runnable afterLock) {
		return storage.getDatabaseTable().walk(this, callback, afterLock);
	}

	public final long walkDesc(TableWalkHandle<K, V> callback) {
		return walkDesc(callback, null);
	}

	public final long walkDesc(TableWalkHandle<K, V> callback, Runnable afterLock) {
		return storage.getDatabaseTable().walkDesc(this, callback, afterLock);
	}

	public final long walkCacheKey(TableWalkKey<K> callback) {
		return cache.walkKey(callback);
	}

	public final long WalkDatabaseKey(TableWalkKey<K> callback) {
		return walkDatabaseKey(callback);
	}

	public final long walkDatabaseKey(TableWalkKey<K> callback) {
		return storage.getDatabaseTable().walkDatabaseKey(this, callback);
	}

	/**
	 * 遍历数据库中的表。看不到本地缓存中的数据。
	 * 【并发】后台数据库处理并发。
	 *
	 * @param callback walk callback
	 * @return count
	 */
	public final long walkDatabase(TableWalkHandleRaw callback) {
		if (storage.getDatabaseTable() instanceof Database.AbstractKVTable) {
			return ((Database.AbstractKVTable)storage.getDatabaseTable()).walk(callback);
		}
		throw new UnsupportedOperationException("Not A KV Table.");
	}

	public final long walkDatabaseDesc(TableWalkHandleRaw callback) {
		if (storage.getDatabaseTable() instanceof Database.AbstractKVTable) {
		return ((Database.AbstractKVTable)storage.getDatabaseTable()).walkDesc(callback);
		}
		throw new UnsupportedOperationException("Not A KV Table.");
	}

	/**
	 * 遍历数据库中的表。看不到本地缓存中的数据。
	 * 【并发】后台数据库处理并发。
	 *
	 * @param callback walk callback
	 * @return count
	 */
	public final long walkDatabase(TableWalkHandle<K, V> callback) {
		return storage.getDatabaseTable().walkDatabase(this, callback);
	}

	public final long walkDatabaseDesc(TableWalkHandle<K, V> callback) {
		return storage.getDatabaseTable().walkDatabaseDesc(this, callback);
	}

	/**
	 * 事务外调用
	 * 遍历缓存
	 *
	 * @return count
	 */
	public final long walkCache(TableWalkHandle<K, V> callback) {
		return walkCache(callback, null);
	}

	public final long walkCache(TableWalkHandle<K, V> callback, Runnable afterLock) {
		if (Transaction.getCurrent() != null)
			throw new IllegalStateException("must be called without transaction");

		int count = 0;
		for (var entry : cache.getDataMap().entrySet()) {
			var r = entry.getValue();
			var lockey = getZeze().getLocks().get(new TableKey(getId(), entry.getKey()));
			lockey.enterReadLock();
			try {
				if (r.getState() == StateShare
						|| r.getState() == StateModify) {
					@SuppressWarnings("unchecked")
					var strongRef = (V)r.getSoftValue();
					if (strongRef == null)
						continue;
					count++;
					if (!callback.handle(r.getObjectKey(), strongRef))
						break;
				}
			} finally {
				lockey.exitReadLock();
			}
			if (afterLock != null)
				afterLock.run();
		}
		return count;
	}

	/**
	 * 获得记录的拷贝。
	 * 1. 一般在事务外使用。
	 * 2. 如果在事务内使用：
	 * a)已经访问过的记录，得到最新值的拷贝。不建议这种用法。
	 * b)没有访问过的记录，从后台查询并拷贝，但不会加入RecordAccessed。
	 * 3. 得到的结果一般不用于修改，应用传递时可以使用ReadOnly接口修饰保护一下。
	 *
	 * @param key record key
	 * @return record value
	 */
	@SuppressWarnings("unchecked")
	public final V selectCopy(K key) {
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
			currentT.setAlwaysReleaseLockWhenRedo();
		}

		var lockey = getZeze().getLocks().get(tkey);
		lockey.enterReadLock();
		try {
			var v = load(key).strongRef;
			return v != null ? (V)v.copy() : null;
		} finally {
			lockey.exitReadLock();
		}
	}

	@SuppressWarnings("unchecked")
	public final V selectDirty(K key) {
		if (key == null)
			throw new IllegalArgumentException("key is null");
		var currentT = Transaction.getCurrent();
		if (currentT != null) {
			var cr = currentT.getRecordAccessed(new TableKey(getId(), key));
			if (cr != null)
				return (V)cr.newestValue();
		}
		return load(key).strongRef;
	}

	@Override
	public final boolean isNew() {
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
	public ByteBuffer encodeChangeListenerWithSpecialName(String specialName, Object key, Changes.Record r) {
		var bb = ByteBuffer.Allocate();
		bb.WriteString(null == specialName ? getName() : specialName);
		bb.WriteByteBuffer(encodeKey(key));
		r.encode(bb);
		return bb;
	}
}
