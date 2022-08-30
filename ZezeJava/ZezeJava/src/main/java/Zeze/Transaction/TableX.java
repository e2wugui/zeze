package Zeze.Transaction;

import java.util.ArrayList;
import java.util.function.Supplier;
import Zeze.Application;
import Zeze.Component.DelayRemove;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
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
	private TableCache<K, V> Cache;
	private Storage<K, V> TStorage;
	private Database.Table OldTable;
	private DatabaseRocksDb.Table LocalRocksCacheTable;

	public TableX(String name) {
		super(name);
	}

	protected final AutoKey getAutoKey() {
		return autoKey;
	}

	public final Storage<K, V> InternalGetStorageForTestOnly(String IAmSure) {
		if (!IAmSure.equals("IKnownWhatIAmDoing"))
			throw new IllegalArgumentException();
		return TStorage;
	}

	@Override
	final Storage<K, V> GetStorage() {
		return TStorage;
	}

	@Override
	final Database.Table getOldTable() {
		return OldTable;
	}

	final DatabaseRocksDb.Table getLocalRocksCacheTable() {
		return LocalRocksCacheTable;
	}

	public final int getCacheSize() {
		return Cache != null ? Cache.getDataMap().size() : 0;
	}

	final void RocksCachePut(K key, V value) {
		try (var t = getZeze().getLocalRocksCacheDb().BeginTransaction()) {
			LocalRocksCacheTable.Replace(t, EncodeKey(key), ByteBuffer.Encode(value));
			t.Commit();
		} catch (Exception e) {
			logger.error("RocksCachePut exception:", e);
		}
	}

	final void RocksCacheRemove(K key) {
		try (var t = getZeze().getLocalRocksCacheDb().BeginTransaction()) {
			LocalRocksCacheTable.Remove(t, EncodeKey(key));
			t.Commit();
		} catch (Exception e) {
			logger.error("RocksCacheRemove exception:", e);
		}
	}

	public Supplier<ArrayList<TableX<K, V>>> GetSimulateTables; // only for temp debug

	private void VerifyGlobalRecordState(K key, boolean isModify) { // only for temp debug
		var getSimulateTables = GetSimulateTables;
		if (getSimulateTables == null)
			return;

		var checkState = isModify ? StateInvalid : StateShare;
		for (var table : getSimulateTables.get()) {
			if (table == this || table.Cache == null)
				continue; // skip self
			var r = table.Cache.Get(key);
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
	private AtomicTupleRecord<K, V> Load(K key) {
		var tkey = new TableKey(getId(), key);
		while (true) {
			var r = Cache.GetOrAdd(key, () -> new Record1<>(this, key, null));
			r.EnterFairLock(); // 对同一个记录，不允许重入。
			V strongRef = null;
			try {
				if (r.getState() == StateRemoved)
					continue; // 正在被删除，重新 GetOrAdd 一次。以后 _lock_check_ 里面会再次检查这个状态。

				if (r.getState() == StateShare
						|| r.getState() == StateModify) {
					var beforeTimestamp = r.getTimestamp(); // read timestamp before read value。see Record1.Commit
					strongRef = (V)r.getSoftValue();
					if (strongRef == null && !r.getDirty()) {
						var find = LocalRocksCacheTable.Find(EncodeKey(key));
						if (find != null) {
							strongRef = DecodeValue(find);
							strongRef.InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
							r.setSoftValue(strongRef);
						}
					}
					return new AtomicTupleRecord<>(r, strongRef, beforeTimestamp);
				}

				var acquire = r.Acquire(StateShare, false, false);
				r.setState(acquire.ResultState);
				if (r.getState() == StateInvalid) {
					var txn = Transaction.getCurrent();
					if (txn == null)
						throw new IllegalStateException("Acquire Failed");
					txn.ThrowRedoAndReleaseLock(tkey + ":" + r, null);
				}
				VerifyGlobalRecordState(key, r.getState() == StateModify);

				r.setTimestamp(Record.getNextTimestamp());
				r.setFreshAcquire();
				var beforeTimestamp = r.getTimestamp();

				if (TStorage != null) {
					if (Macro.EnableStatistics) {
						TableStatistics.getInstance().GetOrAdd(getId()).getStorageFindCount().increment();
					}
					strongRef = TStorage.Find(key, this);
					if (strongRef != null) {
						RocksCachePut(key, strongRef);
					} else {
						RocksCacheRemove(key);
					}
					r.setSoftValue(strongRef); // r.Value still maybe null
					// 【注意】这个变量不管 OldTable 中是否存在的情况。
					r.setExistInBackDatabase(strongRef != null);

					// 当记录删除时需要同步删除 OldTable，否则下一次又会从 OldTable 中找到。
					// see Record1.Flush
					if (strongRef == null && null != OldTable) {
						var ekey = EncodeKey(key);
						var old = OldTable.Find(ekey);
						if (old != null) {
							strongRef = DecodeValue(old);
							r.setSoftValue(strongRef);
							// 从旧表装载时，马上设为脏，使得可以写入新表。
							// 否则直到被修改前，都不会被保存到当前数据库中。
							r.SetDirty();
							// Immediately 需要特别在此单独处理。
							if (getZeze().getConfig().getCheckpointMode() == CheckpointMode.Immediately) {
								var lct = getZeze().getLocalRocksCacheDb().BeginTransaction();
								var t = getOldTable().getDatabase().BeginTransaction();
								try {
									OldTable.Replace(t, ekey, old);
									LocalRocksCacheTable.Replace(lct, ekey, old);
									lct.Commit();
									t.Commit();
								} catch (Throwable ex) {
									lct.Rollback();
									t.Rollback();
								} finally {
									try {
										lct.close();
									} catch (Exception e) {
										logger.error(e);
									}
									try {
										t.close();
									} catch (Exception e) {
										logger.error(e);
									}
								}
							}
						}
					}
					if (strongRef != null)
						strongRef.InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
				}
				if (isDebugEnabled)
					logger.debug("Load {}", r);
				return new AtomicTupleRecord<>(r, strongRef, beforeTimestamp);
			} finally {
				r.ExitFairLock();
			}
		}
	}

	@Override
	public final int ReduceShare(Reduce rpc, ByteBuffer bbKey) {
		var fresh = rpc.getResultCode();
		rpc.setResultCode(0);
		rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
		rpc.Result.State = rpc.Argument.State;
		if (isDebugEnabled)
			logger.debug("Reduce NewState={}", rpc);

		K key = DecodeKey(bbKey);
		var lockey = getZeze().getLocks().Get(new TableKey(getId(), key));
		lockey.EnterWriteLock();
		try {
			var r = Cache.Get(key);
			if (isDebugEnabled)
				logger.debug("Reduce NewState={} {}", rpc.Argument.State, r);
			if (r == null) {
				rpc.Result.State = StateInvalid;
				if (isDebugEnabled)
					logger.debug("Reduce SendResult 1 r=null");
				rpc.SendResultCode(GlobalCacheManagerConst.ReduceShareAlreadyIsInvalid);
				return 0;
			}
			r.EnterFairLock();
			try {
				if (fresh != GlobalCacheManagerConst.AcquireFreshSource && r.isFreshAcquire()) {
					if (isDebugEnabled)
						logger.debug("Reduce SendResult fresh {}", r);
					rpc.Result.State = GlobalCacheManagerConst.StateReduceErrorFreshAcquire;
					rpc.SendResult();
					return 0;
				}
				r.setNotFresh(); // 被降级不再新鲜。
				switch (r.getState()) {
				case StateRemoved: // impossible! safe only.
				case StateInvalid:
					rpc.Result.State = StateInvalid;
					rpc.setResultCode(GlobalCacheManagerConst.ReduceShareAlreadyIsInvalid);

					if (r.getDirty())
						break;
					if (isDebugEnabled)
						logger.debug("Reduce SendResult 2 {}", r);
					rpc.SendResult();
					return 0;

				case StateShare:
					rpc.Result.State = StateShare;
					rpc.setResultCode(GlobalCacheManagerConst.ReduceShareAlreadyIsShare);
					if (r.getDirty())
						break;
					if (isDebugEnabled)
						logger.debug("Reduce SendResult 3 {}", r);
					rpc.SendResult();
					return 0;

				case StateModify:
					r.setState(StateShare); // 马上修改状态。事务如果要写会再次请求提升(Acquire)。
					rpc.Result.State = StateShare;
					if (r.getDirty())
						break;
					if (isDebugEnabled)
						logger.debug("Reduce SendResult * {}", r);
					rpc.SendResult();
					return 0;
				}
				// if (isDebugEnabled)
				// logger.warn("ReduceShare checkpoint begin. id={} {}", r, tkey);
				FlushWhenReduce(r);
				if (isDebugEnabled)
					logger.debug("Reduce SendResult 4 {}", r);
				rpc.SendResult();
				// if (isDebugEnabled)
				// logger.warn("ReduceShare checkpoint end. id={} {}", r, tkey);
			} finally {
				r.ExitFairLock();
			}
		} finally {
			lockey.ExitWriteLock();
		}
		return 0;
	}

	private void FlushWhenReduce(Record r) {
		switch (getZeze().getConfig().getCheckpointMode()) {
		case Period:
			throw new RuntimeException("Global Can Not Work With CheckpointMode.Period.");

		case Immediately:
			break;

		case Table:
			RelativeRecordSet.FlushWhenReduce(r, getZeze().getCheckpoint());
			break;
		}
	}

	@Override
	public final int ReduceInvalid(Reduce rpc, ByteBuffer bbKey) {
		var fresh = rpc.getResultCode();
		rpc.setResultCode(0);
		rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
		rpc.Result.State = rpc.Argument.State;

		K key = DecodeKey(bbKey);
		var lockey = getZeze().getLocks().Get(new TableKey(getId(), key));
		lockey.EnterWriteLock();
		try {
			var r = Cache.Get(key);
			if (isDebugEnabled)
				logger.debug("Reduce NewState={} {}", rpc.Argument.State, r);
			if (r == null) {
				rpc.Result.State = StateInvalid;
				if (isDebugEnabled)
					logger.debug("Reduce SendResult 1 r=null");
				rpc.SendResultCode(GlobalCacheManagerConst.ReduceInvalidAlreadyIsInvalid);
				return 0;
			}
			r.EnterFairLock();
			try {
				if (fresh != GlobalCacheManagerConst.AcquireFreshSource && r.isFreshAcquire()) {
					if (isDebugEnabled)
						logger.debug("Reduce SendResult fresh {}", r);
					rpc.Result.State = GlobalCacheManagerConst.StateReduceErrorFreshAcquire;
					rpc.SendResult();
					return 0;
				}
				r.setNotFresh(); // 被降级不再新鲜。
				switch (r.getState()) {
				case StateRemoved: // impossible! safe only.
				case StateInvalid:
					rpc.Result.State = StateInvalid;
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
				rpc.Result.State = StateInvalid;
				FlushWhenReduce(r);
				if (isDebugEnabled)
					logger.debug("Reduce SendResult 4 {}", r);
				rpc.SendResult();
				// if (isDebugEnabled)
				// logger.warn("ReduceInvalid checkpoint end. id={} {}", r, tkey);
			} finally {
				r.ExitFairLock();
			}
		} finally {
			lockey.ExitWriteLock();
		}
		return 0;
	}

	public final Binary EncodeGlobalKey(K key) {
		var bb = ByteBuffer.Allocate();
		bb.WriteInt4(getId());
		var bbKey = EncodeKey(key);
		bb.Append(bbKey.Bytes, bbKey.ReadIndex, bbKey.Size());
		return new Binary(bb);
	}

	@Override
	final void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex) {
		var globalAgent = getZeze().getGlobalAgent();
		var locks = getZeze().getLocks();
		var remain = new ArrayList<KV<Lockey, Record1<K, V>>>(Cache.getDataMap().size());
		logger.info("ReduceInvalidAllLocalOnly CacheSize=" + Cache.getDataMap().size());
		for (var e : Cache.getDataMap().entrySet()) {
			var k = e.getKey();
			if (globalAgent.GetGlobalCacheManagerHashIndex(EncodeGlobalKey(k)) != GlobalCacheManagerHashIndex)
				continue;

			var v = e.getValue();
			var lockey = locks.Get(new TableKey(getId(), k));
			if (!lockey.TryEnterWriteLock(0)) {
				remain.add(KV.Create(lockey, v));
				continue;
			}
			try {
				if (!v.TryEnterFairLock()) {
					remain.add(KV.Create(lockey, v));
					continue;
				}
				try {
					// 只是需要设置Invalid，放弃资源，后面的所有访问都需要重新获取。
					v.setState(StateInvalid);
					FlushWhenReduce(v);
				} finally {
					v.ExitFairLock();
				}
			} finally {
				lockey.ExitWriteLock();
			}
		}

		if (!remain.isEmpty()) {
			logger.info("ReduceInvalidAllLocalOnly Remain=" + remain.size());
			for (var e : remain) {
				var k = e.getKey();
				k.EnterWriteLock();
				try {
					var v = e.getValue();
					v.EnterFairLock();
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

		var tkey = new TableKey(getId(), key);
		var cr = currentT.GetRecordAccessed(tkey);
		if (cr != null) {
			@SuppressWarnings("unchecked")
			var r = (V)cr.NewestValue();
			return r;
		}

		var r = Load(key);
		currentT.AddRecordAccessed(r.Record.CreateRootInfoIfNeed(tkey), new RecordAccessed(r));
		return r.StrongRef;
	}

	public final boolean contains(K key) {
		return get(key) != null;
	}

	public final V getOrAdd(K key) {
		var currentT = Transaction.getCurrent();
		assert currentT != null;

		var tkey = new TableKey(getId(), key);
		var cr = currentT.GetRecordAccessed(tkey);
		if (cr != null) {
			@SuppressWarnings("unchecked")
			V crv = (V)cr.NewestValue();
			if (crv != null)
				return crv;
			// add
		} else {
			var r = Load(key);
			cr = new RecordAccessed(r);
			currentT.AddRecordAccessed(r.Record.CreateRootInfoIfNeed(tkey), cr);
			if (r.StrongRef != null)
				return r.StrongRef;
			// add
		}

		V add = NewValue();
		add.InitRootInfo(cr.AtomicTupleRecord.Record.CreateRootInfoIfNeed(tkey), null);
		cr.Put(currentT, add);
		return add;
	}

	public final boolean tryAdd(K key, V value) {
		if (get(key) != null)
			return false;

		var currentT = Transaction.getCurrent();
		assert currentT != null;

		var tkey = new TableKey(getId(), key);
		var cr = currentT.GetRecordAccessed(tkey);
		value.InitRootInfoWithRedo(cr.AtomicTupleRecord.Record.CreateRootInfoIfNeed(tkey), null);
		cr.Put(currentT, value);
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

		var tkey = new TableKey(getId(), key);
		var cr = currentT.GetRecordAccessed(tkey);
		if (cr != null) {
			value.InitRootInfoWithRedo(cr.AtomicTupleRecord.Record.CreateRootInfoIfNeed(tkey), null);
			cr.Put(currentT, value);
			return;
		}
		var r = Load(key);
		cr = new RecordAccessed(r);
		cr.Put(currentT, value);
		currentT.AddRecordAccessed(r.Record.CreateRootInfoIfNeed(tkey), cr);
	}

	// 几乎和Put一样，还是独立开吧。
	public final void remove(K key) {
		var currentT = Transaction.getCurrent();
		assert currentT != null;

		var tkey = new TableKey(getId(), key);
		var cr = currentT.GetRecordAccessed(tkey);
		if (cr != null) {
			cr.Put(currentT, null);
			return;
		}

		var r = Load(key);
		cr = new RecordAccessed(r);
		cr.Put(currentT, null);
		currentT.AddRecordAccessed(r.Record.CreateRootInfoIfNeed(tkey), cr);
	}

	@Override
	final Storage<?, ?> Open(Application app, Database database) {
		if (TStorage != null)
			throw new IllegalStateException("table has opened: " + getName());

		setZeze(app);
		setDatabase(database);

		if (isAutoKey())
			autoKey = app.getServiceManagerAgent().GetAutoKey(getName());

		setTableConf(app.getConfig().GetTableConf(getName()));
		Cache = new TableCache<>(app, this);
		TStorage = isMemory() ? null : new Storage<>(this, database, getName());
		OldTable = getTableConf().getDatabaseOldMode() == 1
				? app.GetDatabase(getTableConf().getDatabaseOldName()).OpenTable(getName()) : null;
		LocalRocksCacheTable = app.getLocalRocksCacheDb().OpenTable(getName());
		return TStorage;
	}

	@Override
	final void Close() {
		if (TStorage != null) {
			TStorage.Close();
			TStorage = null;
		}
		if (Cache != null) {
			Cache.close();
			Cache = null;
		}
	}

	// Key 都是简单变量，系列化方法都不一样，需要生成。
	public abstract ByteBuffer EncodeKey(K key);

	public abstract K DecodeKey(ByteBuffer bb);

	public final void delayRemove(K key) {
		DelayRemove.remove(this, key);
	}

	@Override
	public abstract V NewValue();

	/**
	 * 解码系列化的数据到对象。
	 *
	 * @param bb bean encoded data
	 * @return Value
	 */
	public final V DecodeValue(ByteBuffer bb) {
		V value = NewValue();
		value.Decode(bb);
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
	public final long Walk(TableWalkHandle<K, V> callback) {
		return Walk(callback, null);
	}

	private boolean invokeCallback(byte[] key, byte[] value, TableWalkHandle<K, V> callback) {
		K k = DecodeKey(ByteBuffer.Wrap(key));
		var lockey = getZeze().getLocks().Get(new TableKey(getId(), k));
		lockey.EnterReadLock();
		try {
			var r = Cache.Get(k);
			if (r != null && r.getState() != StateRemoved) {
				if (r.getState() == StateShare
						|| r.getState() == StateModify) {
					// 拥有正确的状态：
					@SuppressWarnings("unchecked")
					var strongRef = (V)r.getSoftValue();
					if (strongRef == null)
						return true; // 已经被删除，但是还没有checkpoint的记录看不到。
					return callback.handle(r.getObjectKey(), strongRef);
				}
				// else GlobalCacheManager.StateInvalid
				// 继续后面的处理：使用数据库中的数据。
			}
		} finally {
			lockey.ExitReadLock();
		}
		// 缓存中不存在或者正在被删除，使用数据库中的数据。
		return callback.handle(k, DecodeValue(ByteBuffer.Wrap(value)));
	}

	public final long Walk(TableWalkHandle<K, V> callback, Runnable afterLock) {
		if (Transaction.getCurrent() != null)
			throw new IllegalStateException("must be called without transaction");

		return TStorage.getDatabaseTable().Walk((key, value) -> {
			if (invokeCallback(key, value, callback)) {
				if (afterLock != null)
					afterLock.run();
				return true;
			}
			return false;
		});
	}

	public final long WalkCacheKey(TableWalkKey<K> callback) {
		return Cache.WalkKey(callback);
	}

	public final long WalkDatabaseKey(TableWalkKey<K> callback) {
		return TStorage.getDatabaseTable().WalkKey(key -> callback.handle(DecodeKey(ByteBuffer.Wrap(key))));
	}

	/**
	 * 遍历数据库中的表。看不到本地缓存中的数据。
	 * 【并发】后台数据库处理并发。
	 *
	 * @param callback walk callback
	 * @return count
	 */
	public final long WalkDatabase(TableWalkHandleRaw callback) {
		return TStorage.getDatabaseTable().Walk(callback);
	}

	/**
	 * 遍历数据库中的表。看不到本地缓存中的数据。
	 * 【并发】后台数据库处理并发。
	 *
	 * @param callback walk callback
	 * @return count
	 */
	public final long WalkDatabase(TableWalkHandle<K, V> callback) {
		return TStorage.getDatabaseTable().Walk((key, value) -> {
			K k = DecodeKey(ByteBuffer.Wrap(key));
			V v = DecodeValue(ByteBuffer.Wrap(value));
			return callback.handle(k, v);
		});
	}

	/**
	 * 事务外调用
	 * 遍历缓存
	 *
	 * @return count
	 */
	public final long WalkCache(TableWalkHandle<K, V> callback) {
		return WalkCache(callback, null);
	}

	public final long WalkCache(TableWalkHandle<K, V> callback, Runnable afterLock) {
		if (Transaction.getCurrent() != null)
			throw new IllegalStateException("must be called without transaction");

		int count = 0;
		for (var entry : Cache.getDataMap().entrySet()) {
			var r = entry.getValue();
			var lockey = getZeze().getLocks().Get(new TableKey(getId(), entry.getKey()));
			lockey.EnterReadLock();
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
				lockey.ExitLock();
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
		var tkey = new TableKey(getId(), key);
		var currentT = Transaction.getCurrent();
		if (currentT != null) {
			var cr = currentT.GetRecordAccessed(tkey);
			if (cr != null) {
				var v = cr.NewestValue();
				return v != null ? (V)v.CopyBean() : null;
			}
			currentT.SetAlwaysReleaseLockWhenRedo();
		}

		var lockey = getZeze().getLocks().Get(tkey);
		lockey.EnterReadLock();
		try {
			var v = Load(key).StrongRef;
			return v != null ? (V)v.CopyBean() : null;
		} finally {
			lockey.ExitReadLock();
		}
	}

	@SuppressWarnings("unchecked")
	public final V selectDirty(K key) {
		var currentT = Transaction.getCurrent();
		if (currentT != null) {
			var cr = currentT.GetRecordAccessed(new TableKey(getId(), key));
			if (cr != null)
				return (V)cr.NewestValue();
		}
		return Load(key).StrongRef;
	}

	@Override
	public final boolean isNew() {
		return TStorage == null // memory table always return true
				|| TStorage.getDatabaseTable().isNew();
	}
}
