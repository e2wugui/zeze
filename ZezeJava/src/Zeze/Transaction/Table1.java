package Zeze.Transaction;

import Zeze.Serialize.*;
import Zeze.Services.*;
import Zeze.*;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

	private ServiceManager.Agent.AutoKey AutoKey;
	protected final ServiceManager.Agent.AutoKey getAutoKey() {
		return AutoKey;
	}
	private void setAutoKey(ServiceManager.Agent.AutoKey value) {
		AutoKey = value;
	}


	private Record1<K, V> FindInCacheOrStorage(K key) {
		return FindInCacheOrStorage(key, null);
	}

	private Record1<K, V> FindInCacheOrStorage(K key, tangible.Action1Param<V> copy) {
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
				Record1<K, V> r = getCache().GetOrAdd(key, (key) -> new Record1<K, V>(this, key, null));
				synchronized (r) { // 如果外面是 WriteLock 就不需要这个了。
					if (r.State == GlobalCacheManager.StateRemoved) {
						continue; // 正在被删除，重新 GetOrAdd 一次。以后 _lock_check_ 里面会再次检查这个状态。
					}

					if (r.State == GlobalCacheManager.StateShare || r.State == GlobalCacheManager.StateModify) {
						return r;
					}

					r.State = r.Acquire(GlobalCacheManager.StateShare);
					if (r.State == GlobalCacheManager.StateInvalid) {
						throw new RedoAndReleaseLockException(tkey.toString() + ":" + r.toString());
						//throw new RedoAndReleaseLockException();
					}

					r.Timestamp = Record.getNextTimestamp();

					if (null != getTStorage()) {
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if ENABLE_STATISTICS
						TableStatistics.getInstance().GetOrAdd(getId()).getStorageFindCount().IncrementAndGet();
//#endif
						r.Value = getTStorage().Find(key, this); // r.Value still maybe null

						// 【注意】这个变量不管 OldTable 中是否存在的情况。
						r.setExistInBackDatabase(null != r.Value);

						// 当记录删除时需要同步删除 OldTable，否则下一次又会从 OldTable 中找到。
						if (null == r.Value && null != getOldTable()) {
							ByteBuffer old = getOldTable().Find(EncodeKey(key));
							if (null != old) {
								r.Value = DecodeValue(old);
								// 从旧表装载时，马上设为脏，使得可以写入新表。
								// TODO CheckpointMode.Immediately 需要特殊处理。
								r.SetDirty();
							}
						}
						if (null != r.Value) {
							r.Value.InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
						}
					}
					logger.Debug("FindInCacheOrStorage {0}", r);
				}
				if (copy != null) {
					copy.invoke(r.getValueTyped());
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
		rpc.setResult(rpc.getArgument());
		K key = DecodeKey(ByteBuffer.Wrap(rpc.getArgument().getGlobalTableKey().getKey()));

		//logger.Debug("Reduce NewState={0}", rpc.Argument.State);

		TableKey tkey = new TableKey(getId(), key);
		Lockey lockey = Locks.getInstance().Get(tkey);
		lockey.EnterWriteLock();
		Record<K, V> r = null;
		try {
			r = getCache().Get(key);
			logger.Debug("Reduce NewState={0} {1}", rpc.getArgument().getState(), r);
			if (null == r) {
				rpc.getResult().setState(GlobalCacheManager.StateInvalid);
				logger.Debug("Reduce SendResult 1 {0}", r);
				rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsInvalid);
				return 0;
			}
			switch (r.State) {
				case GlobalCacheManager.StateInvalid:
					rpc.getResult().setState(GlobalCacheManager.StateInvalid);
					logger.Debug("Reduce SendResult 2 {0}", r);
					rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsInvalid);
					return 0;

				case GlobalCacheManager.StateShare:
					rpc.getResult().setState(GlobalCacheManager.StateShare);
					logger.Debug("Reduce SendResult 3 {0}", r);
					rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsShare);
					return 0;

				case GlobalCacheManager.StateModify:
					r.State = GlobalCacheManager.StateShare; // 马上修改状态。事务如果要写会再次请求提升(Acquire)。
					r.Timestamp = Record.getNextTimestamp();
					break;
			}
		}
		finally {
			lockey.ExitWriteLock();
		}
		//logger.Warn("ReduceShare checkpoint begin. id={0} {1}", r, tkey);
		rpc.getResult().setState(GlobalCacheManager.StateShare);
		FlushWhenReduce(r, () -> {
				logger.Debug("Reduce SendResult 4 {0}", r);
				rpc.SendResult();
		});
		//logger.Warn("ReduceShare checkpoint end. id={0} {1}", r, tkey);
		return 0;
	}

	private void FlushWhenReduce(Record r, tangible.Action0Param after) {
		switch (getZeze().getConfig().getCheckpointMode()) {
			case CheckpointMode.Period:
				getZeze().getCheckpoint().AddActionAndPulse(after);
				break;

			case CheckpointMode.Immediately:
				after.invoke();
				break;

			case CheckpointMode.Table:
				RelativeRecordSet.FlushWhenReduce(r, getZeze().getCheckpoint(), after);
				break;
		}
	}

	@Override
	public int ReduceInvalid(GlobalCacheManager.Reduce rpc) {
		rpc.setResult(rpc.getArgument());
		K key = DecodeKey(ByteBuffer.Wrap(rpc.getArgument().getGlobalTableKey().getKey()));

		//logger.Debug("Reduce NewState={0}", rpc.Argument.State);

		TableKey tkey = new TableKey(getId(), key);
		Lockey lockey = Locks.getInstance().Get(tkey);
		lockey.EnterWriteLock();
		Record<K, V> r = null;
		try {
			r = getCache().Get(key);
			logger.Debug("Reduce NewState={0} {1}", rpc.getArgument().getState(), r);
			if (null == r) {
				rpc.getResult().setState(GlobalCacheManager.StateInvalid);
				logger.Debug("Reduce SendResult 1 {0}", r);
				rpc.SendResultCode(GlobalCacheManager.ReduceInvalidAlreadyIsInvalid);
				return 0;
			}
			switch (r.State) {
				case GlobalCacheManager.StateInvalid:
					rpc.getResult().setState(GlobalCacheManager.StateInvalid);
					logger.Debug("Reduce SendResult 2 {0}", r);
					rpc.SendResultCode(GlobalCacheManager.ReduceInvalidAlreadyIsInvalid);
					return 0;

				case GlobalCacheManager.StateShare:
					r.State = GlobalCacheManager.StateInvalid;
					r.Timestamp = Record.getNextTimestamp();
					// 不删除记录，让TableCache.CleanNow处理。 
					logger.Debug("Reduce SendResult 3 {0}", r);
					rpc.SendResult();
					return 0;

				case GlobalCacheManager.StateModify:
					r.State = GlobalCacheManager.StateInvalid;
					r.Timestamp = Record.getNextTimestamp();
					break;
			}
		}
		finally {
			lockey.ExitWriteLock();
		}
		//logger.Warn("ReduceInvalid checkpoint begin. id={0} {1}", r, tkey);
		rpc.getResult().setState(GlobalCacheManager.StateInvalid);
		FlushWhenReduce(r, () -> {
				logger.Debug("Reduce SendResult 4 {0}", r);
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
				e.Value.State = GlobalCacheManager.StateInvalid;
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
			return (V)cr.NewestValue();
		}

		Record<K, V> r = FindInCacheOrStorage(key);
		currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), new Zeze.Transaction.RecordAccessed(r));
		return r.getValueTyped();
	}

	public final V GetOrAdd(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(getId(), key);

		Zeze.Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
		if (null != cr) {
			V crv = (V)cr.NewestValue();
			if (null != crv) {
				return crv;
			}
			// add
		}
		else {
			Record<K, V> r = FindInCacheOrStorage(key);
			cr = new Zeze.Transaction.RecordAccessed(r);
			currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);

			if (null != r.Value) {
				return r.getValueTyped();
			}
			// add
		}

		V add = NewValue();
		add.InitRootInfo(cr.getOriginRecord().CreateRootInfoIfNeed(tkey), null);
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
		value.InitRootInfo(cr.getOriginRecord().CreateRootInfoIfNeed(tkey), null);
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
			value.InitRootInfo(cr.getOriginRecord().CreateRootInfoIfNeed(tkey), null);
			cr.Put(currentT, value);
			return;
		}
		Record<K, V> r = FindInCacheOrStorage(key);
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

		Record<K, V> r = FindInCacheOrStorage(key);
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

	public final Storage<K, V> GetStorageForTestOnly(String IAmSure) {
		if (!IAmSure.equals("IKnownWhatIAmDoing")) {
			throw new RuntimeException();
		}
		return getTStorage();
	}

	private Database.Table OldTable;
	public final Database.Table getOldTable() {
		return OldTable;
	}
	private void setOldTable(Database.Table value) {
		OldTable = value;
	}
	private Storage<K, V> TStorage;
	public final Storage<K, V> getTStorage() {
		return TStorage;
	}
	private void setTStorage(Storage<K, V> value) {
		TStorage = value;
	}
	@Override
	public Storage getStorage() {
		return getTStorage();
	}

	@Override
	public Storage Open(Application app, Database database) {
		if (null != getTStorage()) {
			throw new RuntimeException("table has opened." + getName());
		}
		setZeze(app);
		if (this.isAutoKey()) {
			setAutoKey(app.getServiceManagerAgent().GetAutoKey(getName()));
		}

		super.TableConf = app.getConfig().GetTableConf(getName());
		setCache(new TableCache<K, V>(app, this));

		setTStorage(isMemory() ? null : new Storage<K, V>(this, database, getName()));
		setOldTable(getTableConf().getDatabaseOldMode() == 1 ? app.GetDatabase(getTableConf().getDatabaseOldName()).OpenTable(getName()) : null);
		return getTStorage();
	}

	@Override
	public void Close() {
		if (getTStorage() != null) {
			getTStorage().Close();
		}
		setTStorage(null);
	}

	// Key 都是简单变量，系列化方法都不一样，需要生成。
	public abstract Zeze.Serialize.ByteBuffer EncodeKey(K key);
	public abstract K DecodeKey(Zeze.Serialize.ByteBuffer bb);

	public final V NewValue() {
		return new V();
	}

	public final Zeze.Serialize.ByteBuffer EncodeValue(V value) {
		Zeze.Serialize.ByteBuffer bb = Zeze.Serialize.ByteBuffer.Allocate(value.getCapacityHintOfByteBuffer());
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
	public final long Walk(tangible.Func2Param<K, V, Boolean> callback) {
		return getTStorage().DatabaseTable.Walk((key, value) -> {
					K k = DecodeKey(ByteBuffer.Wrap(key));
					TableKey tkey = new TableKey(getId(), k);
					Lockey lockey = Locks.getInstance().Get(tkey);
					lockey.EnterReadLock();
					try {
						Record<K, V> r = getCache().Get(k);
						if (null != r && r.State != GlobalCacheManager.StateRemoved) {
							if (r.State == GlobalCacheManager.StateShare || r.State == GlobalCacheManager.StateModify) {
								// 拥有正确的状态：
								if (r.Value == null) {
									return true; // 已经被删除，但是还没有checkpoint的记录看不到。
								}
								return callback.invoke(r.getKey(), r.getValueTyped());
							}
							// else GlobalCacheManager.StateInvalid
							// 继续后面的处理：使用数据库中的数据。
						}
						// 缓存中不存在或者正在被删除，使用数据库中的数据。
						V v = DecodeValue(ByteBuffer.Wrap(value));
						return callback.invoke(k, v);
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
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public long WalkDatabase(Func<byte[], byte[], bool> callback)
	public final long WalkDatabase(tangible.Func2Param<byte[], byte[], Boolean> callback) {
		return getTStorage().DatabaseTable.Walk(callback);
	}

	/** 
	 遍历数据库中的表。看不到本地缓存中的数据。
	 【并发】后台数据库处理并发。
	 
	 @param callback
	 @return 
	*/
	public final long WalkDatabase(tangible.Func2Param<K, V, Boolean> callback) {
		return getTStorage().DatabaseTable.Walk((key, value) -> {
					K k = DecodeKey(ByteBuffer.Wrap(key));
					V v = DecodeValue(ByteBuffer.Wrap(value));
					return callback.invoke(k, v);
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
				return (V)(cr.NewestValue() == null ? null : cr.NewestValue().CopyBean());
			}
		}

		Bean copy = null;
		FindInCacheOrStorage(key, (v) -> copy = v == null ? null : v.CopyBean());
		return (V)copy;
	}
}