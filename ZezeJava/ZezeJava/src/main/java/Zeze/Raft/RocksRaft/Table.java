package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.Func1;
import Zeze.Util.Func2;
import Zeze.Util.Reflect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.ColumnFamilyHandle;
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

	public ConcurrentLruLike<K, Record<K>> getLruCache() {
		return LruCache;
	}

	public Table(Rocks rocks, String templateName, int templateId, Class<K> keyClass, Class<V> valueClass, BiPredicate<K, Record<K>> callback) {
		Rocks = rocks;
		TemplateName = templateName;
		TemplateId = templateId;
		Name = String.format("%s#%d", TemplateName, TemplateId);
		keyEncodeFunc = SerializeHelper.createEncodeFunc(keyClass);
		keyDecodeFunc = SerializeHelper.createDecodeFunc(keyClass);
		valueFactory = Reflect.getDefaultConstructor(valueClass);
		LruTryRemoveCallback = callback;
		Open();
	}

	public void Open() {
		ColumnFamily = Rocks.OpenFamily(Name);
		LruCache = new ConcurrentLruLike<>(Name, CacheCapacity, LruTryRemoveCallback, 200, 2000, 1024);
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

	public void encodeKey(ByteBuffer bb, K key) {
		keyEncodeFunc.accept(bb, key);
	}

	public K decodeKey(ByteBuffer bb) {
		return keyDecodeFunc.apply(bb);
	}

	@SuppressWarnings("unchecked")
	public V newValue() {
		try {
			return (V)valueFactory.invoke();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public V get(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);

		var cr = currentT.GetRecordAccessed(tkey);
		if (cr != null)
			return (V)cr.NewestValue();

		Record<K> r = getOrLoad(key);
		currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), new Transaction.RecordAccessed(r));
		return (V)r.getValue();
	}

	@SuppressWarnings("unchecked")
	public V getOrAdd(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);

		var cr = currentT.GetRecordAccessed(tkey);
		if (cr != null) {
			V crv = (V)cr.NewestValue();
			if (crv != null)
				return crv;
			// add
		} else {
			Record<K> r = getOrLoad(key);
			cr = new Transaction.RecordAccessed(r);
			currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
			if (r.getValue() != null)
				return (V)r.getValue();
			// add
		}

		V add = newValue();
		add.initRootInfo(cr.getOrigin().CreateRootInfoIfNeed(tkey), null);
		cr.Put(currentT, add);
		return add;
	}

	private Record<K> getOrLoad(K key) {
		return getOrLoad(key, null);
	}

	private Record<K> getOrLoad(K key, Bean putValue) {
		TableKey tkey = new TableKey(Name, key);
		while (true) {
			var r = LruCache.getOrAdd(key, () -> {
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
					r.getValue().initRootInfo(r.CreateRootInfoIfNeed(tkey), null);
					r.setTimestamp(Record.getNextTimestamp());
					r.setState(Record.StateLoad);
				} else if (r.getState() == Record.StateNew) {
					// fresh record
					r.setValue(storageLoad(key));
					if (r.getValue() != null)
						r.getValue().initRootInfo(r.CreateRootInfoIfNeed(tkey), null);
					r.setTimestamp(Record.getNextTimestamp());
					r.setState(Record.StateLoad);
				}
				// else in cache
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
			valueBytes = Rocks.getStorage().get(ColumnFamily, DatabaseRocksDb.getDefaultReadOptions(),
					keyBB.Bytes, 0, keyBB.WriteIndex);
		} catch (RocksDBException e) {
			throw new RuntimeException(e);
		}
		if (valueBytes == null)
			return null;
		var valueBB = ByteBuffer.Wrap(valueBytes);
		var value = newValue();
		value.decode(valueBB);
		return value;
	}

	public boolean tryAdd(K key, V value) {
		if (get(key) != null)
			return false;

		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);
		var cr = currentT.GetRecordAccessed(tkey);
		value.initRootInfo(cr.getOrigin().CreateRootInfoIfNeed(tkey), null);
		cr.Put(currentT, value);
		return true;
	}

	public void insert(K key, V value) {
		if (!tryAdd(key, value))
			throw new IllegalArgumentException(String.format("table:%s insert key:%s exists", getClass().getName(), key));
	}

	public void put(K key, V value) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);

		var cr = currentT.GetRecordAccessed(tkey);
		if (cr == null) {
			var r = getOrLoad(key);
			cr = new Transaction.RecordAccessed(r);
			currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
		}
		value.initRootInfo(cr.getOrigin().CreateRootInfoIfNeed(tkey), null);
		cr.Put(currentT, value);
	}

	// 几乎和Put一样，还是独立开吧。
	public void remove(K key) {
		Transaction currentT = Transaction.getCurrent();
		TableKey tkey = new TableKey(Name, key);

		var cr = currentT.GetRecordAccessed(tkey);
		if (cr != null) {
			cr.Put(currentT, null);
			return;
		}

		Record<K> r = getOrLoad(key);
		cr = new Transaction.RecordAccessed(r);
		cr.Put(currentT, null);
		currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
	}

	public boolean walk(Func2<K, V, Boolean> callback) throws Throwable {
		try (var it = Rocks.getStorage().newIterator(ColumnFamily, DatabaseRocksDb.getDefaultReadOptions())) {
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

	public boolean walkKey(Func1<K, Boolean> callback) throws Throwable {
		try (var it = Rocks.getStorage().newIterator(ColumnFamily, DatabaseRocksDb.getDefaultReadOptions())) {
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
			if (r.getValue() == null) {
				logger.fatal("editing bug record not exist. table={} key={} state={}",
						Name, key, r.getState(), new Exception());
				Rocks.getRaft().FatalKill();
			}
			for (var log : rLog.getLogBean())
				r.getValue().followerApply(log); // 最多一个。
			break;

		default:
			logger.fatal("unknown Changes.Record.State. table={} key={} state={}",
					Name, key, rLog.getState(), new Exception());
			Rocks.getRaft().FatalKill();
			return null;
		}
		return r;
	}
}
