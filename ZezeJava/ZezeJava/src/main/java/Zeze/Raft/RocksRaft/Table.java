package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.Func2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDBException;

public final class Table<K, V extends Bean> {
	private static final Logger logger = LogManager.getLogger(Table.class);

	private final Rocks Rocks;
	private final String TemplateName;
	private final int TemplateId;
	private final String Name;
	private final BiConsumer<ByteBuffer, K> keyEncodeFunc;
	private final Function<ByteBuffer, K> keyDecodeFunc;
	private final MethodHandle valueFactory;
	private int CacheCapacity = 10000;
	private ColumnFamilyHandle ColumnFamily;
	private ConcurrentLruLike<K, Record<K>> LruCache;
	private BiPredicate<K, Record<K>> LruTryRemoveCallback;

	public Table(Rocks rocks, String templateName, int templateId, Class<K> keyClass, Class<V> valueClass) {
		Rocks = rocks;
		TemplateName = templateName;
		TemplateId = templateId;
		Name = String.format("%s#%d", TemplateName, TemplateId);
		keyEncodeFunc = SerializeHelper.createEncodeFunc(keyClass);
		keyDecodeFunc = SerializeHelper.createDecodeFunc(keyClass);
		try {
			valueFactory = MethodHandles.lookup().findConstructor(valueClass, MethodType.methodType(void.class));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		Open();
	}

	public void Open() {
		ColumnFamily = Rocks.OpenFamily(Name);
		LruCache = new ConcurrentLruLike<>(CacheCapacity, LruTryRemoveCallback, 200, 2000, 1024);
	}

	public Rocks getRocks() {
		return Rocks;
	}

	public String getTemplateName() {
		return TemplateName;
	}

	public int getTemplateId() {
		return TemplateId;
	}

	public String getName() {
		return Name;
	}

	public int getCacheCapacity() {
		return CacheCapacity;
	}

	public void setCacheCapacity(int value) {
		CacheCapacity = value;
	}

	public ColumnFamilyHandle getColumnFamily() {
		return ColumnFamily;
	}

	public BiPredicate<K, Record<K>> getLruTryRemoveCallback() {
		return LruTryRemoveCallback;
	}

	public void setLruTryRemoveCallback(BiPredicate<K, Record<K>> value) {
		LruTryRemoveCallback = value;
	}

	public void EncodeKey(ByteBuffer bb, K key) {
		keyEncodeFunc.accept(bb, key);
	}

	public K DecodeKey(ByteBuffer bb) {
		return keyDecodeFunc.apply(bb);
	}

	@SuppressWarnings("unchecked")
	public V NewValue() {
		try {
			return (V)valueFactory.invoke();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public V Get(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);

		var cr = currentT.GetRecordAccessed(tkey);
		if (cr != null)
			return (V)cr.NewestValue();

		Record<K> r = GetOrLoad(key);
		currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), new Transaction.RecordAccessed(r));
		return (V)r.getValue();
	}

	@SuppressWarnings("unchecked")
	public V GetOrAdd(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);

		var cr = currentT.GetRecordAccessed(tkey);
		if (cr != null) {
			V crv = (V)cr.NewestValue();
			if (crv != null)
				return crv;
			// add
		} else {
			Record<K> r = GetOrLoad(key);
			cr = new Transaction.RecordAccessed(r);
			currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
			if (r.getValue() != null)
				return (V)r.getValue();
			// add
		}

		V add = NewValue();
		add.InitRootInfo(cr.getOrigin().CreateRootInfoIfNeed(tkey), null);
		cr.Put(currentT, add);
		return add;
	}

	private Record<K> GetOrLoad(K key) {
		return GetOrLoad(key, null);
	}

	private Record<K> GetOrLoad(K key, Bean putValue) {
		TableKey tkey = new TableKey(Name, key);
		while (true) {
			Record<K> tempVar = new Record<>(keyEncodeFunc);
			tempVar.setTable(this);
			tempVar.setKey(key);
			var r = LruCache.GetOrAdd(key, () -> tempVar);
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (r) {
				if (r.getRemoved())
					continue;

				if (putValue != null) {
					// from FollowerApply
					r.setValue(putValue);
					r.getValue().InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
					r.setTimestamp(Record.getNextTimestamp());
					r.setState(Record.StateLoad);
				} else if (r.getState() == Record.StateNew) {
					// fresh record
					r.setValue(StorageLoad(key));
					if (r.getValue() != null)
						r.getValue().InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
					r.setTimestamp(Record.getNextTimestamp());
					r.setState(Record.StateLoad);
				}
				// else in cache
				return r;
			}
		}
	}

	private V StorageLoad(K key) {
		var keyBB = ByteBuffer.Allocate();
		keyEncodeFunc.accept(keyBB, key);
		byte[] valueBytes = null;
		try {
			valueBytes = Rocks.getStorage().get(ColumnFamily, new ReadOptions(), keyBB.Bytes, 0, keyBB.Size());
		} catch (RocksDBException e) {
			logger.error("RocksDB.get error! key=" + key, e);
		}
		if (valueBytes == null)
			return null;
		var valueBB = ByteBuffer.Wrap(valueBytes);
		var value = NewValue();
		value.Decode(valueBB);
		return value;
	}

	public boolean TryAdd(K key, V value) {
		if (Get(key) != null)
			return false;

		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);
		var cr = currentT.GetRecordAccessed(tkey);
		value.InitRootInfo(cr.getOrigin().CreateRootInfoIfNeed(tkey), null);
		cr.Put(currentT, value);
		return true;
	}

	public void Insert(K key, V value) {
		if (!TryAdd(key, value))
			throw new IllegalArgumentException(String.format("table:%s insert key:%s exists", getClass().getName(), key));
	}

	public void Put(K key, V value) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);

		var cr = currentT.GetRecordAccessed(tkey);
		if (cr != null) {
			value.InitRootInfo(cr.getOrigin().CreateRootInfoIfNeed(tkey), null);
			cr.Put(currentT, value);
			return;
		}
		Record<K> r = GetOrLoad(key);
		cr = new Transaction.RecordAccessed(r);
		cr.Put(currentT, value);
		currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
	}

	// 几乎和Put一样，还是独立开吧。
	public void Remove(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);

		var cr = currentT.GetRecordAccessed(tkey);
		if (cr != null) {
			cr.Put(currentT, null);
			return;
		}

		Record<K> r = GetOrLoad(key);
		cr = new Transaction.RecordAccessed(r);
		cr.Put(currentT, null);
		currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
	}

	public boolean Walk(Func2<K, V, Boolean> callback) throws Throwable {
		try (var it = Rocks.getStorage().newIterator(ColumnFamily)) {
			for (it.seekToFirst(); it.isValid(); it.next()) {
				var key = keyDecodeFunc.apply(ByteBuffer.Wrap(it.key()));
				var value = NewValue();
				value.Decode(ByteBuffer.Wrap(it.value()));
				if (!callback.call(key, value))
					return false;
			}
			return true;
		}
	}

	public Record<K> FollowerApply(K key, Changes.Record rLog) {
		Record<K> r;
		switch (rLog.getState()) {
		case Changes.Record.Remove:
			r = GetOrLoad(key);
			r.setValue(null);
			r.setTimestamp(Record.getNextTimestamp());
			break;

		case Changes.Record.Put:
			r = GetOrLoad(key, rLog.getPutValue());
			break;

		case Changes.Record.Edit:
			r = GetOrLoad(key);
			if (r.getValue() == null) {
				logger.fatal("editing bug record not exist.");
				Rocks.getRaft().FatalKill();
			}
			for (var log : rLog.getLogBean())
				r.getValue().FollowerApply(log); // 最多一个。
			break;

		default:
			logger.fatal("unknown Changes.Record.State.");
			Rocks.getRaft().FatalKill();
			return null;
		}
		return r;
	}
}
