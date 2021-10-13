package Zeze.Transaction;

import Zeze.Services.*;
import Zeze.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AutoKey;

public abstract class Table1<K, V extends Bean> extends Table {
	private static final Logger logger = LogManager.getLogger(Table1.class);

	public Table1(String name) {
		super(name);
	}
	private Application Zeze;
	public final Application getZeze() {
		return Zeze;
	}
	private void setZeze(Application value) {
		Zeze = value;
	}

	private AutoKey autoKey;
	protected final AutoKey getAutoKey() {
		return autoKey;
	}
	private void setAutoKey(AutoKey value) {
		autoKey = value;
	}


	private Record1<K, V> FindInCacheOrStorage(K key) {
		return FindInCacheOrStorage(key, null);
	}

	private Record1<K, V> FindInCacheOrStorage(K key, Zeze.Util.Action1<V> copy) {
		TableKey tkey = new TableKey(getId(), key);
		Lockey lockey = Locks.getInstance().Get(tkey);
		lockey.EnterReadLock();
		// 严格来说，这里应该是WriteLock,但是这会涉及Transaction持有的锁的升级问题，
		// 虽然这里只是临时锁一下也会和持有冲突。
		// 由于装载仅在StateInvalid或者第一次载入的时候发生，
		// 还有lock(r)限制线程的重入，所以这里仅加个读锁限制一下state的修改，
		// 防止和Reduce冲突（由于StateInvalid才会申请权限和从storage装载，
		// 应该是不会发生Reduce的，加这个锁为了保险起见）。
		try {
			while (true) {
				Record1<K, V> r = getCache().GetOrAdd(key, () -> new Record1<K, V>(this, key, null));
				synchronized (r) { // 如果外面是 WriteLock 就不需要这个了。
					if (r.getState() == GlobalCacheManager.StateRemoved) {
						continue; // 正在被删除，重新 GetOrAdd 一次。以后 _lock_check_ 里面会再次检查这个状态。
					}

					if (r.getState() == GlobalCacheManager.StateShare || r.getState() == GlobalCacheManager.StateModify) {
						return r;
					}

					r.setState(r.Acquire(GlobalCacheManager.StateShare));
					if (r.getState() == GlobalCacheManager.StateInvalid) {
						throw new RedoAndReleaseLockException(tkey.toString() + ":" + r.toString());
						//throw new RedoAndReleaseLockException();
					}

					r.setTimestamp(Record.getNextTimestamp());

					if (null != TStorage) {
						TableStatistics.getInstance().GetOrAdd(getId()).getStorageFindCount().incrementAndGet();
						r.setValue(TStorage.Find(key, this)); // r.Value still maybe null

						// 【注意】这个变量不管 OldTable 中是否存在的情况。
						r.setExistInBackDatabase(null != r.getValue());

						// 当记录删除时需要同步删除 OldTable，否则下一次又会从 OldTable 中找到。
						if (null == r.getValue() && null != getOldTable()) {
							ByteBuffer old = getOldTable().Find(EncodeKey(key));
							if (null != old) {
								r.setValue(DecodeValue(old));
								// 从旧表装载时，马上设为脏，使得可以写入新表。
								// TODO CheckpointMode.Immediately 需要特殊处理。
								r.SetDirty();
							}
						}
						if (null != r.getValue()) {
							r.getValue().InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
						}
					}
					logger.debug("FindInCacheOrStorage {}", r);
				}
				if (copy != null) {
					copy.run(r.getValueTyped());
				}
				return r;
			}
		}
		finally {
			lockey.ExitReadLock();
		}
	}

	@Override
	public int ReduceShare(GlobalCacheManager.Reduce rpc) {
		rpc.Result = rpc.Argument;
		K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));

		//logger.Debug("Reduce NewState={0}", rpc.Argument.State);

		TableKey tkey = new TableKey(getId(), key);
		Lockey lockey = Locks.getInstance().Get(tkey);
		lockey.EnterWriteLock();
		Record1<K, V> r = null;
		try {
			r = getCache().Get(key);
			logger.debug("Reduce NewState={} {}", rpc.Argument.State, r);
			if (null == r) {
				rpc.Result.State = GlobalCacheManager.StateInvalid;
				logger.debug("Reduce SendResult 1 {}", r);
				rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsInvalid);
				return 0;
			}
			switch (r.getState()) {
				case GlobalCacheManager.StateInvalid:
					rpc.Result.State = GlobalCacheManager.StateInvalid;
					logger.debug("Reduce SendResult 2 {}", r);
					rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsInvalid);
					return 0;

				case GlobalCacheManager.StateShare:
					rpc.Result.State = GlobalCacheManager.StateShare;
					logger.debug("Reduce SendResult 3 {}", r);
					rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsShare);
					return 0;

				case GlobalCacheManager.StateModify:
					r.setState(GlobalCacheManager.StateShare); // 马上修改状态。事务如果要写会再次请求提升(Acquire)。
					r.setTimestamp(Record.getNextTimestamp());
					break;
			}
		}
		finally {
			lockey.ExitWriteLock();
		}
		//logger.Warn("ReduceShare checkpoint begin. id={0} {1}", r, tkey);
		rpc.Result.State = GlobalCacheManager.StateShare;
		final var fr = r;
		FlushWhenReduce(r, () -> {
				logger.debug("Reduce SendResult 4 {}", fr);
				rpc.SendResult();
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
	public int ReduceInvalid(GlobalCacheManager.Reduce rpc) {
		rpc.Result= rpc.Argument;
		K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));

		//logger.Debug("Reduce NewState={0}", rpc.Argument.State);

		TableKey tkey = new TableKey(getId(), key);
		Lockey lockey = Locks.getInstance().Get(tkey);
		lockey.EnterWriteLock();
		Record1<K, V> r = null;
		try {
			r = getCache().Get(key);
			logger.debug("Reduce NewState={} {}", rpc.Argument.State, r);
			if (null == r) {
				rpc.Result.State = GlobalCacheManager.StateInvalid;
				logger.debug("Reduce SendResult 1 {}", r);
				rpc.SendResultCode(GlobalCacheManager.ReduceInvalidAlreadyIsInvalid);
				return 0;
			}
			switch (r.getState()) {
				case GlobalCacheManager.StateInvalid:
					rpc.Result.State = GlobalCacheManager.StateInvalid;
					logger.debug("Reduce SendResult 2 {}", r);
					rpc.SendResultCode(GlobalCacheManager.ReduceInvalidAlreadyIsInvalid);
					return 0;

				case GlobalCacheManager.StateShare:
					r.setState(GlobalCacheManager.StateInvalid);
					r.setTimestamp(Record.getNextTimestamp());
					// 不删除记录，让TableCache.CleanNow处理。 
					logger.debug("Reduce SendResult 3 {}", r);
					rpc.SendResult();
					return 0;

				case GlobalCacheManager.StateModify:
					r.setState(GlobalCacheManager.StateInvalid);
					r.setTimestamp(Record.getNextTimestamp());
					break;
			}
		}
		finally {
			lockey.ExitWriteLock();
		}
		//logger.Warn("ReduceInvalid checkpoint begin. id={0} {1}", r, tkey);
		rpc.Result.State = GlobalCacheManager.StateInvalid;
		final var fr = r;
		FlushWhenReduce(r, () -> {
				logger.debug("Reduce SendResult 4 {}", fr);
				rpc.SendResult();
		});
		//logger.Warn("ReduceInvalid checkpoint end. id={0} {1}", r, tkey);
		return 0;
	}

	@Override
	public void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex) {
		for (var e : getCache().getDataMap()) {
			var gkey = new GlobalCacheManager.GlobalTableKey(getName(), EncodeKey(e.getKey()));
			if (getZeze().getGlobalAgent().GetGlobalCacheManagerHashIndex(gkey) != GlobalCacheManagerHashIndex) {
				// 不是断开连接的GlobalCacheManager。跳过。
				continue;
			}

			TableKey tkey = new TableKey(getId(), e.getKey());
			Lockey lockey = Locks.getInstance().Get(tkey);
			lockey.EnterWriteLock();
			try {
				// 只是需要设置Invalid，放弃资源，后面的所有访问都需要重新获取。
				e.getValue().setState(GlobalCacheManager.StateInvalid);
			}
			finally {
				lockey.ExitWriteLock();
			}
		}
	}

	public final V Get(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(getId(), key);

		Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
		if (null != cr) {
			@SuppressWarnings("unchecked")
			var r = (V)cr.NewestValue();
			return r;
		}

		Record1<K, V> r = FindInCacheOrStorage(key);
		currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), new Zeze.Transaction.RecordAccessed(r));
		return r.getValueTyped();
	}

	public final V GetOrAdd(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(getId(), key);

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
			Record1<K, V> r = FindInCacheOrStorage(key);
			cr = new Zeze.Transaction.RecordAccessed(r);
			currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);

			if (null != r.getValue()) {
				return r.getValueTyped();
			}
			// add
		}

		V add = NewValue();
		add.InitRootInfo(cr.OriginRecord.CreateRootInfoIfNeed(tkey), null);
		cr.Put(currentT, add);
		return add;
	}

	public final boolean TryAdd(K key, V value) {
		if (null != Get(key)) {
			return false;
		}

		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(getId(), key);
		Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
		value.InitRootInfo(cr.OriginRecord.CreateRootInfoIfNeed(tkey), null);
		cr.Put(currentT, value);
		return true;
	}

	public final void Insert(K key, V value) {
		if (false == TryAdd(key, value)) {
			throw new IllegalArgumentException(String.format("table:%1$s insert key:%2$s exists", this.getClass().getName(), key));
		}
	}

	public final void Put(K key, V value) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(getId(), key);

		Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
		if (null != cr) {
			value.InitRootInfo(cr.OriginRecord.CreateRootInfoIfNeed(tkey), null);
			cr.Put(currentT, value);
			return;
		}
		Record1<K, V> r = FindInCacheOrStorage(key);
		cr = new Zeze.Transaction.RecordAccessed(r);
		cr.Put(currentT, value);
		currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
	}

	// 几乎和Put一样，还是独立开吧。
	public final void Remove(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(getId(), key);

		Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
		if (null != cr) {
			cr.Put(currentT, null);
			return;
		}

		Record1<K, V> r = FindInCacheOrStorage(key);
		cr = new Zeze.Transaction.RecordAccessed(r);
		cr.Put(currentT, null);
		currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
	}

	private TableCache<K, V> Cache;
	public final TableCache<K, V> getCache() {
		return Cache;
	}
	private void setCache(TableCache<K, V> value) {
		Cache = value;
	}

	public final Storage1<K, V> GetStorageForTestOnly(String IAmSure) {
		if (!IAmSure.equals("IKnownWhatIAmDoing")) {
			throw new RuntimeException();
		}
		return TStorage;
	}

	private Database.Table OldTable;
	public final Database.Table getOldTable() {
		return OldTable;
	}
	private void setOldTable(Database.Table value) {
		OldTable = value;
	}

	Storage1<K, V> TStorage;

	@Override
	public Storage getStorage() {
		return TStorage;
	}

	@Override
	public Storage Open(Application app, Database database) {
		if (null != TStorage) {
			throw new RuntimeException("table has opened." + getName());
		}
		setZeze(app);
		if (this.isAutoKey()) {
			setAutoKey(app.getServiceManagerAgent().GetAutoKey(getName()));
		}

		super.setTableConf(app.getConfig().GetTableConf(getName()));
		setCache(new TableCache<K, V>(app, this));

		TStorage = isMemory() ? null : new Storage1<K, V>(this, database, getName());
		setOldTable(getTableConf().getDatabaseOldMode() == 1
				? app.GetDatabase(getTableConf().getDatabaseOldName()).OpenTable(getName()) : null);
		return TStorage;
	}

	@Override
	public void Close() {
		if (TStorage != null) {
			TStorage.Close();
		}
		TStorage = null;
	}

	// Key 都是简单变量，系列化方法都不一样，需要生成。
	public abstract Zeze.Serialize.ByteBuffer EncodeKey(K key);
	public abstract K DecodeKey(Zeze.Serialize.ByteBuffer bb);

	public abstract V NewValue();

	public final Zeze.Serialize.ByteBuffer EncodeValue(V value) {
		Zeze.Serialize.ByteBuffer bb = ByteBuffer.Allocate(value.getCapacityHintOfByteBuffer());
		value.Encode(bb);
		return bb;
	}

	/** 
	 解码系列化的数据到对象。
	 
	 @param bb bean encoded data
	 @return 
	*/
	public final V DecodeValue(Zeze.Serialize.ByteBuffer bb) {
		V value = NewValue();
		value.Decode(bb);
		return value;
	}

	/** 
	 遍历表格。能看到记录的最新数据。
	 【注意】这里看不到新增的但没有提交(checkpoint)的记录。实现这个有点麻烦。
	 【并发】每个记录回调时加读锁，回调完成马上释放。
	 
	 @param callback
	 @return 
	*/
	public final long Walk(TableWalkHandle<K, V> callback) {
		return TStorage.getDatabaseTable().Walk((key, value) -> {
					K k = DecodeKey(ByteBuffer.Wrap(key));
					TableKey tkey = new TableKey(getId(), k);
					Lockey lockey = Locks.getInstance().Get(tkey);
					lockey.EnterReadLock();
					try {
						Record1<K, V> r = getCache().Get(k);
						if (null != r && r.getState() != GlobalCacheManager.StateRemoved) {
							if (r.getState() == GlobalCacheManager.StateShare || r.getState() == GlobalCacheManager.StateModify) {
								// 拥有正确的状态：
								if (r.getValue() == null) {
									return true; // 已经被删除，但是还没有checkpoint的记录看不到。
								}
								return callback.handle(r.getKey(), r.getValueTyped());
							}
							// else GlobalCacheManager.StateInvalid
							// 继续后面的处理：使用数据库中的数据。
						}
						// 缓存中不存在或者正在被删除，使用数据库中的数据。
						V v = DecodeValue(ByteBuffer.Wrap(value));
						return callback.handle(k, v);
					}
					finally {
						lockey.ExitReadLock();
					}
		});
	}

	/** 
	 遍历数据库中的表。看不到本地缓存中的数据。
	 【并发】后台数据库处理并发。
	 
	 @param callback
	 @return 
	*/
	public final long WalkDatabase(TableWalkHandleRaw callback) {
		return TStorage.getDatabaseTable().Walk(callback);
	}

	/** 
	 遍历数据库中的表。看不到本地缓存中的数据。
	 【并发】后台数据库处理并发。
	 
	 @param callback
	 @return 
	*/
	public final long WalkDatabase(TableWalkHandle<K, V> callback) {
		return TStorage.getDatabaseTable().Walk((key, value) -> {
					K k = DecodeKey(ByteBuffer.Wrap(key));
					V v = DecodeValue(ByteBuffer.Wrap(value));
					return callback.handle(k, v);
		});
	}

	/** 
	 获得记录的拷贝。
	 1. 一般在事务外使用。
	 2. 如果在事务内使用：
		a)已经访问过的记录，得到最新值的拷贝。不建议这种用法。
		b)没有访问过的记录，从后台查询并拷贝，但不会加入RecordAccessed。
	 3. 得到的结果一般不用于修改，应用传递时可以使用ReadOnly接口修饰保护一下。
	 
	 @param key
	 @return 
	*/
	public final V SelectCopy(K key) {
		Transaction currentT = Transaction.getCurrent();
		if (null != currentT) {
			TableKey tkey = new TableKey(getId(), key);
			Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
			if (null != cr) {
				@SuppressWarnings("unchecked")
				var r = (V)(cr.NewestValue() == null ? null : cr.NewestValue().CopyBean());
				return r;
			}
		}

		final var copy = new Zeze.Util.OutObject<Bean>();
		FindInCacheOrStorage(key, (v) -> copy.Value = v == null ? null : v.CopyBean());
		@SuppressWarnings("unchecked")
		var r = (V)copy.Value;
		return r;
	}
}