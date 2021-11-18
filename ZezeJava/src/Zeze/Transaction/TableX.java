package Zeze.Transaction;

import Zeze.Util.Str;
import Zeze.Services.*;
import Zeze.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.GlobalCacheManager.*;

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

	private Record1<K, V> FindInCacheOrStorage(K key) {
		var tkey = new TableKey(Name, key);
		while (true) {
			Record1<K, V> r = getCache().GetOrAdd(key, () -> new Record1<K, V>(this, key, null));
			r.EnterFairLock(); // 对同一个记录，不允许重入。
			try {
				if (r.getState() == GlobalCacheManagerServer.StateRemoved) {
					continue; // 正在被删除，重新 GetOrAdd 一次。以后 _lock_check_ 里面会再次检查这个状态。
				}

				if (r.getState() == GlobalCacheManagerServer.StateShare
						|| r.getState() == GlobalCacheManagerServer.StateModify) {
					return r;
				}

				var acquire = r.Acquire(GlobalCacheManagerServer.StateShare);
				r.setState(acquire.Result.State);
				if (r.getState() == GlobalCacheManagerServer.StateInvalid) {
					r.LastErrorGlobalSerialId = acquire.Result.GlobalSerialId; // save
					throw new RedoAndReleaseLockException(tkey, acquire.Result.GlobalSerialId, tkey.toString() + ":" + r.toString());
					//throw new RedoAndReleaseLockException();
				}

				r.setTimestamp(Record.getNextTimestamp());

				if (null != TStorage) {
					TableStatistics.getInstance().GetOrAdd(Name).getStorageFindCount().incrementAndGet();
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
			} finally {
				r.ExitFairLock();
			}
			return r;
		}
	}

	@Override
	int ReduceShare(Reduce rpc) {
		rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
		rpc.Result.State = rpc.Argument.State;
		rpc.Result.GlobalSerialId = rpc.Argument.GlobalSerialId;

		K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));

		logger.debug("Reduce NewState={}", rpc);

		TableKey tkey = new TableKey(Name, key);
		Record1<K, V> r = null;
		Lockey lockey = getZeze().getLocks().Get(tkey);
		lockey.EnterWriteLock();
		try {
			r = getCache().Get(key);
			logger.debug("Reduce NewState={} {}", rpc.Argument.State, r);
			if (null == r) {
				rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
				logger.debug("Reduce SendResult 1 {}", r);
				rpc.SendResultCode(GlobalCacheManagerServer.ReduceShareAlreadyIsInvalid);
				getZeze().__GetOrAddLastFlushWhenReduce(tkey).SetLastGlobalSerialId(rpc.Argument.GlobalSerialId);
				return 0;
			}
			r.EnterFairLock();
			try {
				switch (r.getState()) {
					case GlobalCacheManagerServer.StateRemoved: // impossible! safe only.
					case GlobalCacheManagerServer.StateInvalid:
						rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
						rpc.setResultCode(GlobalCacheManagerServer.ReduceShareAlreadyIsInvalid);
						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult 2 {}", r);
						rpc.SendResult();
						getZeze().__GetOrAddLastFlushWhenReduce(tkey).SetLastGlobalSerialId(rpc.Argument.GlobalSerialId);
						return 0;

					case GlobalCacheManagerServer.StateShare:
						rpc.Result.State = GlobalCacheManagerServer.StateShare;
						rpc.setResultCode(GlobalCacheManagerServer.ReduceShareAlreadyIsShare);
						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult 3 {}", r);
						rpc.SendResult();
						getZeze().__GetOrAddLastFlushWhenReduce(tkey).SetLastGlobalSerialId(rpc.Argument.GlobalSerialId);
						return 0;

					case GlobalCacheManagerServer.StateModify:
						r.setState(GlobalCacheManagerServer.StateShare); // 马上修改状态。事务如果要写会再次请求提升(Acquire)。
						rpc.Result.State = GlobalCacheManagerServer.StateShare;
						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult * {}", r);
						rpc.SendResult();
						getZeze().__GetOrAddLastFlushWhenReduce(tkey).SetLastGlobalSerialId(rpc.Argument.GlobalSerialId);
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
				getZeze().__GetOrAddLastFlushWhenReduce(tkey).SetLastGlobalSerialId(rpc.Argument.GlobalSerialId);
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
	int ReduceInvalid(Reduce rpc) {
		rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
		rpc.Result.State = rpc.Argument.State;
		rpc.Result.GlobalSerialId = rpc.Argument.GlobalSerialId;

		K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));

		//logger.Debug("Reduce NewState={0}", rpc.Argument.State);

		TableKey tkey = new TableKey(Name, key);
		Record1<K, V> r = null;
		Lockey lockey = getZeze().getLocks().Get(tkey);
		lockey.EnterWriteLock();
		try {
			r = getCache().Get(key);
			logger.debug("Reduce NewState={} {}", rpc.Argument.State, r);
			if (null == r) {
				rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
				logger.debug("Reduce SendResult 1 {}", r);
				rpc.SendResultCode(GlobalCacheManagerServer.ReduceInvalidAlreadyIsInvalid);
				getZeze().__GetOrAddLastFlushWhenReduce(tkey).SetLastGlobalSerialId(rpc.Argument.GlobalSerialId);
				return 0;
			}
			r.EnterFairLock();
			try {
				switch (r.getState()) {
					case GlobalCacheManagerServer.StateRemoved: // impossible! safe only.
					case GlobalCacheManagerServer.StateInvalid:
						rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
						rpc.setResultCode(GlobalCacheManagerServer.ReduceInvalidAlreadyIsInvalid);
						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult 2 {}", r);
						rpc.SendResult();
						getZeze().__GetOrAddLastFlushWhenReduce(tkey).SetLastGlobalSerialId(rpc.Argument.GlobalSerialId);
						return 0;

					case GlobalCacheManagerServer.StateShare:
						r.setState(GlobalCacheManagerServer.StateInvalid);
						// 不删除记录，让TableCache.CleanNow处理。
						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult 3 {}", r);
						rpc.SendResult();
						getZeze().__GetOrAddLastFlushWhenReduce(tkey).SetLastGlobalSerialId(rpc.Argument.GlobalSerialId);
						return 0;

					case GlobalCacheManagerServer.StateModify:
						r.setState(GlobalCacheManagerServer.StateInvalid);
						if (r.getDirty())
							break;
						logger.debug("Reduce SendResult * {}", r);
						rpc.SendResult();
						getZeze().__GetOrAddLastFlushWhenReduce(tkey).SetLastGlobalSerialId(rpc.Argument.GlobalSerialId);
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
				getZeze().__GetOrAddLastFlushWhenReduce(tkey).SetLastGlobalSerialId(rpc.Argument.GlobalSerialId);
		});
		//logger.Warn("ReduceInvalid checkpoint end. id={0} {1}", r, tkey);
		return 0;
	}

	@Override
	void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex) {
		for (var e : getCache().getDataMap().entrySet()) {
			var gkey = new GlobalTableKey(getName(), EncodeKey(e.getKey()));
			if (getZeze().getGlobalAgent().GetGlobalCacheManagerHashIndex(gkey) != GlobalCacheManagerHashIndex) {
				// 不是断开连接的GlobalCacheManager。跳过。
				continue;
			}

			TableKey tkey = new TableKey(Name, e.getKey());
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
		TableKey tkey = new TableKey(Name, key);

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

	public final V getOrAdd(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);

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

	public final boolean tryAdd(K key, V value) {
		if (null != get(key)) {
			return false;
		}

		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);
		Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
		value.InitRootInfo(cr.OriginRecord.CreateRootInfoIfNeed(tkey), null);
		cr.Put(currentT, value);
		return true;
	}

	public final void insert(K key, V value) {
		if (false == tryAdd(key, value)) {
			throw new IllegalArgumentException(Str.format("table:{} insert key:{} exists",
					this.getClass().getName(), key));
		}
	}

	public final void put(K key, V value) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);

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
	public final void remove(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);

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

	public final Storage1<K, V> InternalGetStorageForTestOnly(String IAmSure) {
		if (!IAmSure.equals("IKnownWhatIAmDoing")) {
			throw new RuntimeException();
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

	@Override
	Storage GetStorage() {
		return TStorage;
	}

	@Override
	Storage Open(Application app, Database database) {
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
	void Close() {
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
					TableKey tkey = new TableKey(Name, k);
					Lockey lockey = getZeze().getLocks().Get(tkey);
					lockey.EnterReadLock();
					try {
						Record1<K, V> r = getCache().Get(k);
						if (null != r && r.getState() != GlobalCacheManagerServer.StateRemoved) {
							if (r.getState() == GlobalCacheManagerServer.StateShare
									|| r.getState() == GlobalCacheManagerServer.StateModify) {
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
		b)没有访问过的记录，从后台查询并拷贝，但不会加入RecordAccessed。【有死锁风险，已废弃】
	 3. 得到的结果一般不用于修改，应用传递时可以使用ReadOnly接口修饰保护一下。
	 
	 @param key
	 @return 
	*/
	public final V selectCopy(K key) {
		TableKey tkey = new TableKey(Name, key);
		Transaction currentT = Transaction.getCurrent();
		if (null != currentT) {
			Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
			if (null != cr) {
				@SuppressWarnings("unchecked")
				var r = (V)(cr.NewestValue() == null ? null : cr.NewestValue().CopyBean());
				return r;
			}
			throw new RuntimeException("SelectCopy A Not Accessed Record In Transaction Is Danger!");
		}

		var lockey = getZeze().getLocks().Get(tkey);
		lockey.EnterReadLock();
		try {
			var r= FindInCacheOrStorage(key);
			if (null == r.getValue())
				return null;
			@SuppressWarnings("unchecked")
			var v = (V)r.getValue().CopyBean();
			return v;
		} finally {
			lockey.ExitReadLock();
		}
	}
}