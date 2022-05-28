package Zeze.Transaction;

import java.util.Map;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Services.GlobalCacheManagerServer;
import Zeze.Services.ServiceManager.AutoKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class TableX<K extends Comparable<K>, V extends Bean> extends Table {
	private static final Logger logger = LogManager.getLogger(TableX.class);

	public TableX(String name) {
		super(name);
	}

	private AutoKey autoKey;
	protected final AutoKey getAutoKey() {
		return autoKey;
	}
	private void setAutoKey(AutoKey value) {
		autoKey = value;
	}

	void RocksCachePut(K key, V value) {
		var t = getZeze().getLocalRocksCacheDb().BeginTransaction();
		LocalRocksCacheTable.Replace(t, EncodeKey(key), EncodeValue(value));
		t.Commit();
		try {
			t.close();
		} catch (Exception e) {
			logger.error(e);
		}
	}

	void RocksCacheRemove(K key) {
		var t = getZeze().getLocalRocksCacheDb().BeginTransaction();
		LocalRocksCacheTable.Remove(t, EncodeKey(key));
		t.Commit();
		try {
			t.close();
		} catch (Exception e) {
			logger.error(e);
		}
	}

	//public static boolean flag = true;

	@SuppressWarnings("unchecked")
	private AtomicTupleRecord<K, V> Load(K key) {
		var tkey = new TableKey(getId(), key);
		while (true) {
			Record1<K, V> r = getCache().GetOrAdd(key, () -> new Record1<>(this, key, null));
			r.EnterFairLock(); // 对同一个记录，不允许重入。
			V strongRef = null;
			try {
				if (r.getState() == GlobalCacheManagerServer.StateRemoved) {
					continue; // 正在被删除，重新 GetOrAdd 一次。以后 _lock_check_ 里面会再次检查这个状态。
				}

				if (r.getState() == GlobalCacheManagerServer.StateShare
						|| r.getState() == GlobalCacheManagerServer.StateModify) {
					var beforeTimestamp = r.getTimestamp(); // read timestamp before read value。see Record1.Commit
					strongRef = (V)r.getSoftValue();
					if (null == strongRef && !r.getDirty()) {
						var find = LocalRocksCacheTable.Find(EncodeKey(key));
						if (null != find) {
							strongRef = DecodeValue(find);
							strongRef.InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
							r.setSoftValue(strongRef);
						}
					}
					/*
					if (!flag) {
						flag = true;
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
					*/
					return AtomicTupleRecord.create(r, strongRef, beforeTimestamp);
				}

				var acquire = r.Acquire(GlobalCacheManagerServer.StateShare);
				r.setState(acquire.ResultState);
				if (r.getState() == GlobalCacheManagerServer.StateInvalid) {
					r.LastErrorGlobalSerialId = acquire.ResultGlobalSerialId; // save
					var txn = Transaction.getCurrent();
					if (null == txn)
						throw new IllegalStateException("Acquire Failed");
					txn.LastTableKeyOfRedoAndRelease = tkey;
					txn.LastGlobalSerialIdOfRedoAndRelease = acquire.ResultGlobalSerialId;
					txn.ThrowRedoAndReleaseLock(tkey + ":" + r, null);
				}

				r.setTimestamp(Record.getNextTimestamp());
				var beforeTimestamp = r.getTimestamp();
				if (null != TStorage) {
					TableStatistics.getInstance().GetOrAdd(getId()).getStorageFindCount().incrementAndGet();
					strongRef = TStorage.Find(key, this);
					if (null != strongRef){
						RocksCachePut(key, strongRef);
					}
					r.setSoftValue(strongRef); // r.Value still maybe null
					// 【注意】这个变量不管 OldTable 中是否存在的情况。
					r.setExistInBackDatabase(null != strongRef);

					// 当记录删除时需要同步删除 OldTable，否则下一次又会从 OldTable 中找到。
					if (null == strongRef && null != getOldTable()) {
						ByteBuffer old = getOldTable().Find(EncodeKey(key));
						if (null != old) {
							strongRef = DecodeValue(old);
							r.setSoftValue(strongRef);
							// 从旧表装载时，马上设为脏，使得可以写入新表。
							// TODO CheckpointMode.Immediately
							// 需要马上保存，否则，直到这个记录被访问才有机会保存。
							r.SetDirty();
						}
					}
					if (null != strongRef) {
						strongRef.InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
					}
				}
				logger.debug("Load {}", r);
				return AtomicTupleRecord.create(r, strongRef, beforeTimestamp);
			} finally {
				r.ExitFairLock();
			}
		}
	}

	@Override
	public int ReduceShare(Reduce rpc, ByteBuffer bbKey) {
		rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
		rpc.Result.State = rpc.Argument.State;
		rpc.Result.GlobalSerialId = rpc.Argument.GlobalSerialId;

		K key = DecodeKey(bbKey);

		logger.debug("Reduce NewState={}", rpc);

		TableKey tkey = new TableKey(getId(), key);
		Record1<K, V> r;
		Lockey lockey = getZeze().getLocks().Get(tkey);
		lockey.EnterWriteLock();
		try {
			r = getCache().Get(key);
			logger.debug("Reduce NewState={} {}", rpc.Argument.State, r);
			if (null == r) {
				rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
				logger.debug("Reduce SendResult 1 r=null");
				rpc.SendResultCode(GlobalCacheManagerServer.ReduceShareAlreadyIsInvalid);
				getZeze().__SetLastGlobalSerialId(tkey, rpc.Argument.GlobalSerialId);
				return 0;
			}
			r.EnterFairLock();
			try {
				r.LastErrorGlobalSerialId = rpc.Argument.GlobalSerialId;
				switch (r.getState()) {
					case GlobalCacheManagerServer.StateRemoved: // impossible! safe only.
					case GlobalCacheManagerServer.StateInvalid:
						rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
						rpc.setResultCode(GlobalCacheManagerServer.ReduceShareAlreadyIsInvalid);

						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult 2 {}", r);
						rpc.SendResult();
						getZeze().__SetLastGlobalSerialId(tkey, rpc.Argument.GlobalSerialId);
						return 0;

					case GlobalCacheManagerServer.StateShare:
						rpc.Result.State = GlobalCacheManagerServer.StateShare;
						rpc.setResultCode(GlobalCacheManagerServer.ReduceShareAlreadyIsShare);
						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult 3 {}", r);
						rpc.SendResult();
						getZeze().__SetLastGlobalSerialId(tkey, rpc.Argument.GlobalSerialId);
						return 0;

					case GlobalCacheManagerServer.StateModify:
						r.setState(GlobalCacheManagerServer.StateShare); // 马上修改状态。事务如果要写会再次请求提升(Acquire)。
						rpc.Result.State = GlobalCacheManagerServer.StateShare;
						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult * {}", r);
						rpc.SendResult();
						getZeze().__SetLastGlobalSerialId(tkey, rpc.Argument.GlobalSerialId);
						return 0;
				}
			} finally {
				r.ExitFairLock();
			}
		} finally {
			lockey.ExitWriteLock();
		}
		//logger.Warn("ReduceShare checkpoint begin. id={0} {1}", r, tkey);
		final var fr = r;
		FlushWhenReduce(r, () -> {
			logger.debug("Reduce SendResult 4 {}", fr);
			rpc.SendResult();
			getZeze().__SetLastGlobalSerialId(tkey, rpc.Argument.GlobalSerialId);
		});
		//logger.Warn("ReduceShare checkpoint end. id={0} {1}", r, tkey);
		return 0;
	}

	private void FlushWhenReduce(Record r, Runnable after) {
		switch (getZeze().getConfig().getCheckpointMode()) {
			case Period:
				getZeze().getCheckpoint().AddActionAndPulse(after);
				break;

			case Immediately:
				after.run();
				break;

			case Table:
				RelativeRecordSet.FlushWhenReduce(r, getZeze().getCheckpoint(), after);
				break;
		}
	}

	@Override
	public int ReduceInvalid(Reduce rpc, ByteBuffer bbKey) {
		rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
		rpc.Result.State = rpc.Argument.State;
		rpc.Result.GlobalSerialId = rpc.Argument.GlobalSerialId;

		K key = DecodeKey(bbKey);

		//logger.Debug("Reduce NewState={0}", rpc.Argument.State);

		TableKey tkey = new TableKey(getId(), key);
		Record1<K, V> r;
		Lockey lockey = getZeze().getLocks().Get(tkey);
		lockey.EnterWriteLock();
		try {
			r = getCache().Get(key);
			logger.debug("Reduce NewState={} {}", rpc.Argument.State, r);
			if (null == r) {
				rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
				logger.debug("Reduce SendResult 1 r=null");
				rpc.SendResultCode(GlobalCacheManagerServer.ReduceInvalidAlreadyIsInvalid);
				getZeze().__SetLastGlobalSerialId(tkey, rpc.Argument.GlobalSerialId);
				return 0;
			}
			r.EnterFairLock();
			try {
				r.LastErrorGlobalSerialId = rpc.Argument.GlobalSerialId;
				switch (r.getState()) {
					case GlobalCacheManagerServer.StateRemoved: // impossible! safe only.
					case GlobalCacheManagerServer.StateInvalid:
						rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
						rpc.setResultCode(GlobalCacheManagerServer.ReduceInvalidAlreadyIsInvalid);
						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult 2 {}", r);
						rpc.SendResult();
						getZeze().__SetLastGlobalSerialId(tkey, rpc.Argument.GlobalSerialId);
						return 0;

					case GlobalCacheManagerServer.StateShare:
						r.setState(GlobalCacheManagerServer.StateInvalid);
						// 不删除记录，让TableCache.CleanNow处理。
						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult 3 {}", r);
						rpc.SendResult();
						getZeze().__SetLastGlobalSerialId(tkey, rpc.Argument.GlobalSerialId);
						return 0;

					case GlobalCacheManagerServer.StateModify:
						r.setState(GlobalCacheManagerServer.StateInvalid);
						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult * {}", r);
						rpc.SendResult();
						getZeze().__SetLastGlobalSerialId(tkey, rpc.Argument.GlobalSerialId);
						return 0;
				}
			} finally {
				r.ExitFairLock();
			}
		}
		finally {
			lockey.ExitWriteLock();
		}
		//logger.Warn("ReduceInvalid checkpoint begin. id={0} {1}", r, tkey);
		rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
		final var fr = r;
		FlushWhenReduce(r, () -> {
			logger.debug("Reduce SendResult 4 {}", fr);
			rpc.SendResult();
			getZeze().__SetLastGlobalSerialId(tkey, rpc.Argument.GlobalSerialId);
		});
		//logger.Warn("ReduceInvalid checkpoint end. id={0} {1}", r, tkey);
		return 0;
	}

	public Binary EncodeGlobalKey(K key) {
		var bb = ByteBuffer.Allocate();
		bb.WriteInt4(getId());
		var bbKey = EncodeKey(key);
		bb.Append(bbKey.Bytes, bbKey.ReadIndex, bbKey.Size());
		return new Binary(bb);
	}
	@Override
	void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex) {
		for (var e : getCache().getDataMap().entrySet()) {
			var gkey = EncodeGlobalKey(e.getKey());
			if (getZeze().getGlobalAgent().GetGlobalCacheManagerHashIndex(gkey) != GlobalCacheManagerHashIndex) {
				// 不是断开连接的GlobalCacheManager。跳过。
				continue;
			}

			TableKey tkey = new TableKey(getId(), e.getKey());
			Lockey lockey = getZeze().getLocks().Get(tkey);
			lockey.EnterWriteLock();
			try {
				// 只是需要设置Invalid，放弃资源，后面的所有访问都需要重新获取。
				e.getValue().setState(GlobalCacheManagerServer.StateInvalid);
			}
			finally {
				lockey.ExitWriteLock();
			}
		}
	}

	public final V get(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(getId(), key);

		assert currentT != null;
		Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
		if (null != cr) {
			@SuppressWarnings("unchecked")
			var r = (V)cr.NewestValue();
			return r;
		}

		var r = Load(key);
		currentT.AddRecordAccessed(r.Record.CreateRootInfoIfNeed(tkey), new Zeze.Transaction.RecordAccessed(r));
		return r.StrongRef;
	}

	public final V getOrAdd(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(getId(), key);

		assert currentT != null;
		Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
		if (null != cr) {
			@SuppressWarnings("unchecked")
			V crv = (V)cr.NewestValue();
			if (null != crv) {
				return crv;
			}
			// add
		}
		else {
			var r = Load(key);
			cr = new Zeze.Transaction.RecordAccessed(r);
			currentT.AddRecordAccessed(r.Record.CreateRootInfoIfNeed(tkey), cr);

			if (null != r.StrongRef) {
				return r.StrongRef;
			}
			// add
		}

		V add = NewValue();
		add.InitRootInfo(cr.AtomicTupleRecord.Record.CreateRootInfoIfNeed(tkey), null);
		cr.Put(currentT, add);
		return add;
	}

	public final boolean tryAdd(K key, V value) {
		if (null != get(key)) {
			return false;
		}

		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(getId(), key);
		assert currentT != null;
		Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
		value.InitRootInfo(cr.AtomicTupleRecord.Record.CreateRootInfoIfNeed(tkey), null);
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
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(getId(), key);

		assert currentT != null;
		Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
		if (null != cr) {
			value.InitRootInfo(cr.AtomicTupleRecord.Record.CreateRootInfoIfNeed(tkey), null);
			cr.Put(currentT, value);
			return;
		}
		var r = Load(key);
		cr = new Zeze.Transaction.RecordAccessed(r);
		cr.Put(currentT, value);
		currentT.AddRecordAccessed(r.Record.CreateRootInfoIfNeed(tkey), cr);
	}

	// 几乎和Put一样，还是独立开吧。
	public final void remove(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(getId(), key);

		assert currentT != null;
		Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
		if (null != cr) {
			cr.Put(currentT, null);
			return;
		}

		var r = Load(key);
		cr = new Zeze.Transaction.RecordAccessed(r);
		cr.Put(currentT, null);
		currentT.AddRecordAccessed(r.Record.CreateRootInfoIfNeed(tkey), cr);
	}

	private TableCache<K, V> Cache;
	final TableCache<K, V> getCache() {
		return Cache;
	}
	public int getCacheSize() {
		if (null == Cache)
			return 0;
		return Cache.getDataMap().size();
	}
	private void setCache(TableCache<K, V> value) {
		Cache = value;
	}

	public final Storage1<K, V> InternalGetStorageForTestOnly(String IAmSure) {
		if (!IAmSure.equals("IKnownWhatIAmDoing")) {
			throw new IllegalArgumentException();
		}
		return TStorage;
	}

	private Database.Table OldTable;
	final Database.Table getOldTable() {
		return OldTable;
	}
	private void setOldTable(Database.Table value) {
		OldTable = value;
	}

	Storage1<K, V> TStorage;
	private DatabaseRocksDb.Table LocalRocksCacheTable;
	DatabaseRocksDb.Table getLocalRocksCacheTable() {
		return LocalRocksCacheTable;
	}

	@Override
	Storage GetStorage() {
		return TStorage;
	}

	@Override
	Storage Open(Application app, Database database) {
		if (null != TStorage) {
			throw new IllegalStateException("table has opened: " + getName());
		}
		setZeze(app);
		setDatabase(database);

		if (this.isAutoKey()) {
			setAutoKey(app.getServiceManagerAgent().GetAutoKey(getName()));
		}

		super.setTableConf(app.getConfig().GetTableConf(getName()));
		setCache(new TableCache<>(app, this));

		TStorage = isMemory() ? null : new Storage1<>(this, database, getName());
		setOldTable(getTableConf().getDatabaseOldMode() == 1
				? app.GetDatabase(getTableConf().getDatabaseOldName()).OpenTable(getName()) : null);
		LocalRocksCacheTable = app.getLocalRocksCacheDb().OpenTable(getName());
		return TStorage;
	}

	@Override
	void Close() {
		if (TStorage != null) {
			TStorage.Close();
		}
		TStorage = null;
		if (null != Cache)
			Cache.close();
		Cache = null;
	}

	// Key 都是简单变量，系列化方法都不一样，需要生成。
	public abstract ByteBuffer EncodeKey(K key);
	public abstract K DecodeKey(ByteBuffer bb);

	public void delayRemove(K key) {
		Zeze.Component.DelayRemove.remove(this, key);
	}

	public abstract V NewValue();

	@Override
	public Bean NewBeanValue() {
		return NewValue();
	}

	public final ByteBuffer EncodeValue(V sa) {
		return ByteBuffer.Encode(sa);
	}

	/**
	 解码系列化的数据到对象。

	 @param bb bean encoded data
	 @return Value
	*/
	public final V DecodeValue(ByteBuffer bb) {
		V value = NewValue();
		value.Decode(bb);
		return value;
	}

	/**
	 事务外调用
	 遍历表格。能看到记录的最新数据。
	 【注意】这里看不到新增的但没有提交(checkpoint)的记录。实现这个有点麻烦。
	 【并发】每个记录回调时加读锁，回调完成马上释放。

	 @param callback walk callback
	 @return count
	*/
	public final long Walk(TableWalkHandle<K, V> callback) {
		if (Transaction.getCurrent() != null) {
			throw new IllegalStateException("must be called without transaction");
		}
		return TStorage.getDatabaseTable().Walk((key, value) -> {
			K k = DecodeKey(ByteBuffer.Wrap(key));
			TableKey tkey = new TableKey(getId(), k);
			Lockey lockey = getZeze().getLocks().Get(tkey);
			lockey.EnterReadLock();
			try {
				Record1<K, V> r = getCache().Get(k);
				if (null != r && r.getState() != GlobalCacheManagerServer.StateRemoved) {
					if (r.getState() == GlobalCacheManagerServer.StateShare
							|| r.getState() == GlobalCacheManagerServer.StateModify) {
						// 拥有正确的状态：
						@SuppressWarnings("unchecked")
						var strongRef = (V)r.getSoftValue();
						if (strongRef == null) {
							return true; // 已经被删除，但是还没有checkpoint的记录看不到。
						}
						return callback.handle(r.getKey(), strongRef);
					}
					// else GlobalCacheManager.StateInvalid
					// 继续后面的处理：使用数据库中的数据。
				}
			}
			finally {
				lockey.ExitReadLock();
			}
			// 缓存中不存在或者正在被删除，使用数据库中的数据。
			V v = DecodeValue(ByteBuffer.Wrap(value));
			return callback.handle(k, v);
		});
	}

	/**
	 遍历数据库中的表。看不到本地缓存中的数据。
	 【并发】后台数据库处理并发。

	 @param callback walk callback
	 @return count
	*/
	public final long WalkDatabase(TableWalkHandleRaw callback) {
		return TStorage.getDatabaseTable().Walk(callback);
	}

	/**
	 遍历数据库中的表。看不到本地缓存中的数据。
	 【并发】后台数据库处理并发。

	 @param callback walk callback
	 @return count
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
	 * @return count
	 */
	public final long WalkCache(TableWalkHandle<K, V> callback) {
		return WalkCache(callback, null);
	}

	public final long WalkCache(TableWalkHandle<K, V> callback, Runnable afterLock) {
		if (Transaction.getCurrent() != null) {
			throw new IllegalStateException("must be called without transaction");
		}
		int count = 0;
		for (Map.Entry<K, Record1<K, V>> entry : this.getCache().getDataMap().entrySet()) {
			K k = entry.getKey();
			Record1<K, V> r = entry.getValue();
			TableKey tkey = new TableKey(getId(), k);
			Lockey lockey = getZeze().getLocks().Get(tkey);
			lockey.EnterReadLock();
			try {
				if (r.getState() == GlobalCacheManagerServer.StateShare
					|| r.getState() == GlobalCacheManagerServer.StateModify) {
					@SuppressWarnings("unchecked")
					var strongRef = (V)r.getSoftValue();
					if (strongRef == null) {
						continue;
					}
					count++;
					if (!callback.handle(r.getKey(), strongRef)) {
						break;
					}
				}
			} finally {
				lockey.ExitLock();
			}
			if (null != afterLock)
				afterLock.run();
		}
		return count;
	}

	/**
	 获得记录的拷贝。
	 1. 一般在事务外使用。
	 2. 如果在事务内使用：
		a)已经访问过的记录，得到最新值的拷贝。不建议这种用法。
		b)没有访问过的记录，从后台查询并拷贝，但不会加入RecordAccessed。
	 3. 得到的结果一般不用于修改，应用传递时可以使用ReadOnly接口修饰保护一下。

	 @param key record key
	 @return record value
	*/
	public final V selectCopy(K key) {
		TableKey tkey = new TableKey(getId(), key);
		Transaction currentT = Transaction.getCurrent();
		if (null != currentT) {
			Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
			if (null != cr) {
				@SuppressWarnings("unchecked")
				var r = (V)(cr.NewestValue() == null ? null : cr.NewestValue().CopyBean());
				return r;
			}
			currentT.SetAlwaysReleaseLockWhenRedo();
		}

		var lockey = getZeze().getLocks().Get(tkey);
		lockey.EnterReadLock();
		try {
			var r= Load(key);
			if (null == r.StrongRef)
				return null;
			@SuppressWarnings("unchecked")
			var v = (V)r.StrongRef.CopyBean();
			return v;
		} finally {
			lockey.ExitReadLock();
		}
	}

	public final V selectDirty(K key) {
		TableKey tkey = new TableKey(getId(), key);
		Transaction currentT = Transaction.getCurrent();
		if (null != currentT) {
			Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
			if (null != cr) {
				@SuppressWarnings("unchecked")
				var r = (V)(cr.NewestValue() == null ? null : cr.NewestValue());
				return r;
			}
		}
		return Load(key).StrongRef;
	}

	@Override
	public boolean isNew() {
		return TStorage == null // memory table always return true
				|| TStorage.getDatabaseTable().isNew();
	}
}
