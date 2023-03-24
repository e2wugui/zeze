package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.KV;
import Zeze.Util.ShutdownHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static Zeze.Services.GlobalCacheManagerConst.StateModify;
import static Zeze.Services.GlobalCacheManagerConst.StateRemoved;
import static Zeze.Services.GlobalCacheManagerConst.StateShare;

/**
 * 数据访问的效率主要来自TableCache的命中。根据以往的经验，命中率是很高的。
 * 所以数据库层就不要求很高的效率。马马虎虎就可以了。
 */
public abstract class Database {
	protected static final Logger logger = LogManager.getLogger(Database.class);

	// 当数据库对Key长度有限制时，使用这个常量。这个数字来自 PorlarDb-X。其中MySql 8是3072.
	// 以后需要升级时，修改这个常量。但是对于已经存在的表，需要自己完成Alter。
	public final static int eMaxKeyLength = 3070;

	private static final boolean isDebugEnabled = logger.isDebugEnabled();

	static {
		ShutdownHook.init();
	}

	private final ConcurrentHashMap<String, Zeze.Transaction.Table> tables = new ConcurrentHashMap<>();
	private final ArrayList<Storage<?, ?>> storages = new ArrayList<>();
	private final DatabaseConf conf;
	private final String databaseUrl;
	private Operates directOperates;

	public Database(DatabaseConf conf) {
		this.conf = conf;
		databaseUrl = conf.getDatabaseUrl();
	}

	public final Collection<Zeze.Transaction.Table> getTables() {
		return tables.values();
	}

	public final Zeze.Transaction.Table getTable(String name) {
		return tables.get(name);
	}

	public final void addTable(Zeze.Transaction.Table table) {
		if (null != tables.putIfAbsent(table.getName(), table))
			throw new IllegalStateException("duplicate table=" + table.getName());
	}

	public final void removeTable(Zeze.Transaction.Table table) {
		table.close();
		tables.remove(table.getName());
	}

	public final DatabaseConf getConf() {
		return conf;
	}

	public final String getDatabaseUrl() {
		return databaseUrl;
	}

	public final Operates getDirectOperates() {
		return directOperates;
	}

	protected final void setDirectOperates(Operates value) {
		directOperates = value;
	}

	public final void open(Application app) {
		for (Zeze.Transaction.Table table : tables.values()) {
			var storage = table.open(app, this);
			if (storage != null)
				storages.add(storage);
		}
	}

	public final void openDynamicTable(Application app, Zeze.Transaction.Table table) {
		var storage = table.open(app, this);
		if (null != storage)
			storages.add(storage);
	}

	public void close() {
		for (Zeze.Transaction.Table table : tables.values())
			table.close();
		tables.clear();
		storages.clear();
	}

	public final void encodeN() {
		// try encode. 可以多趟。
		for (int i = 1; i <= 1; ++i) {
			int countEncodeN = 0;
			for (var storage : storages)
				countEncodeN += storage.encodeN();
			if (isDebugEnabled)
				logger.debug("Checkpoint EncodeN {}@{}", i, countEncodeN);
		}
	}

	public final void snapshot() {
		int countEncode0 = 0;
		int countSnapshot = 0;
		for (var storage : storages)
			countEncode0 += storage.encode0();
		for (var storage : storages)
			countSnapshot += storage.snapshot();

		logger.info("Checkpoint Encode0 And Snapshot countEncode0={} countSnapshot={}", countEncode0, countSnapshot);
	}

	public final void flush(Database.Transaction t, HashMap<Database, Transaction> tss, Database.Transaction lct) {
		int countFlush = 0;
		for (var storage : storages)
			countFlush += storage.flush(t, tss, lct);
		logger.info("Checkpoint Flush count={}", countFlush);
	}

	public final void cleanup() {
		for (var storage : storages)
			storage.cleanup();
	}

	public abstract Table openTable(String name);

	public static byte[] copyIf(java.nio.ByteBuffer bb) {
		if (bb.limit() == bb.capacity() && bb.arrayOffset() == 0)
			return bb.array();
		return Arrays.copyOfRange(bb.array(), bb.arrayOffset(), bb.limit());
	}

	public static byte[] copyIf(ByteBuffer bb) {
		if (bb.ReadIndex == 0 && bb.WriteIndex == bb.capacity())
			return bb.Bytes;
		return Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.WriteIndex);
	}

	public interface Table {
		boolean isNew();

		Database getDatabase();

		///////////////////////////////////////////////////////////
		// TableX类型下沉到这里，准备添加关系表映射。
		<K extends Comparable<K>, V extends Bean> V find(TableX<K, V> table, Object key);
		// 这里的key，value具体含义由Table实现解释。
		// 对于KV表，key,value都是ByteBuffer类型。
		// 对于关系表，key,value是SQLStatement类型。
		void replace(Transaction t, Object key, Object value);
		void remove(Transaction t, Object key);

		<K extends Comparable<K>, V extends Bean>
		long walk(TableX<K, V> table, TableWalkHandle<K, V> callback, Runnable afterLock);
		<K extends Comparable<K>, V extends Bean>
		long walkDesc(TableX<K, V> table, TableWalkHandle<K, V> callback, Runnable afterLock);
		<K extends Comparable<K>, V extends Bean>
		K walk(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback, Runnable afterLock);
		<K extends Comparable<K>, V extends Bean>
		K walkKey(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback, Runnable afterLock);
		<K extends Comparable<K>, V extends Bean>
		K walkDesc(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback, Runnable afterLock);
		<K extends Comparable<K>, V extends Bean>
		K walkKeyDesc(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback, Runnable afterLock);

		<K extends Comparable<K>, V extends Bean>
		long walkDatabase(TableX<K, V> table, TableWalkHandle<K, V> callback);
		<K extends Comparable<K>, V extends Bean>
		long walkDatabaseDesc(TableX<K, V> table, TableWalkHandle<K, V> callback);

		<K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(TableX<K, V> table, TableWalkKey<K> callback);
		<K extends Comparable<K>, V extends Bean>
		long walkDatabaseKeyDesc(TableX<K, V> table, TableWalkKey<K> callback);

		void close();
	}

	// KV表辅助类，实现所有的下沉的带类型接口。
	public static abstract class AbstractKVTable implements Table {
		////////////////////////////////////////////////////////////
		// KV表操作接口。
		public abstract ByteBuffer find(ByteBuffer key);

		public abstract void replace(Transaction t, ByteBuffer key, ByteBuffer value);

		public abstract void remove(Transaction t, ByteBuffer key);

		/**
		 * 每一条记录回调。回调返回true继续遍历，false中断遍历。
		 *
		 * @return 返回已经遍历的数量
		 */
		public abstract long walk(TableWalkHandleRaw callback);

		public abstract long walkKey(TableWalkKeyRaw callback);

		public abstract long walkDesc(TableWalkHandleRaw callback);

		public abstract long walkKeyDesc(TableWalkKeyRaw callback);

		public abstract ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback);

		public abstract ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback);

		public abstract ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback);

		public abstract ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback);

		@Override
		public <K extends Comparable<K>, V extends Bean>
		V find(TableX<K, V> table, Object key) {
			var bbKey = table.encodeKey(key);
			var bbValue = find(bbKey);
			if (null == bbValue)
				return null;
			var value = table.newValue();
			value.decode(bbValue);
			return value;
		}

		@Override
		public void replace(Transaction t, Object key, Object value) {
			replace(t, (ByteBuffer)key, (ByteBuffer)value);
		}

		@Override
		public void remove(Transaction t, Object key) {
			remove(t, (ByteBuffer)key);
		}

		private <K extends Comparable<K>, V extends Bean>
		boolean invokeCallback(TableX<K, V> table, byte[] key, byte[] value, TableWalkHandle<K, V> callback) {
			K k = table.decodeKey(ByteBuffer.Wrap(key));
			var lockey = table.getZeze().getLocks().get(new TableKey(table.getId(), k));
			lockey.enterReadLock();
			try {
				var r = table.getCache().get(k);
				if (r != null && r.getState() != StateRemoved) {
					if (r.getState() == StateShare || r.getState() == StateModify) {
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
				lockey.exitReadLock();
			}
			// 缓存中不存在或者正在被删除，使用数据库中的数据。
			return callback.handle(k, table.decodeValue(ByteBuffer.Wrap(value)));
		}

		private <K extends Comparable<K>, V extends Bean>
		boolean invokeCallback(TableX<K, V> table, byte[] key, TableWalkKey<K> callback) {
			K k = table.decodeKey(ByteBuffer.Wrap(key));
			var lockey = table.getZeze().getLocks().get(new TableKey(table.getId(), k));
			lockey.enterReadLock();
			try {
				var r = table.getCache().get(k);
				if (r != null && r.getState() != StateRemoved) {
					if (r.getState() == StateShare || r.getState() == StateModify) {
						// 拥有正确的状态：
						@SuppressWarnings("unchecked")
						var strongRef = (V)r.getSoftValue();
						if (strongRef == null)
							return true; // 已经被删除，但是还没有checkpoint的记录看不到。
						return callback.handle(r.getObjectKey());
					}
					// else GlobalCacheManager.StateInvalid
					// 继续后面的处理：使用数据库中的数据。
				}
			} finally {
				lockey.exitReadLock();
			}
			// 缓存中不存在或者正在被删除，使用数据库中的数据。
			return callback.handle(k);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walk(TableX<K, V> table, TableWalkHandle<K, V> callback, Runnable afterLock) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			return walk((key, value) -> {
				if (invokeCallback(table, key, value, callback)) {
					if (afterLock != null)
						afterLock.run();
					return true;
				}
				return false;
			});

		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(TableX<K, V> table, TableWalkKey<K> callback) {
			return walkKey(key -> callback.handle(table.decodeKey(ByteBuffer.Wrap(key))));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDesc(TableX<K, V> table, TableWalkHandle<K, V> callback, Runnable afterLock) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			return walkDesc((key, value) -> {
				if (invokeCallback(table, key, value, callback)) {
					if (afterLock != null)
						afterLock.run();
					return true;
				}
				return false;
			});		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKeyDesc(TableX<K, V> table, TableWalkKey<K> callback) {
			return walkKeyDesc(key -> callback.handle(table.decodeKey(ByteBuffer.Wrap(key))));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walk(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback, Runnable afterLock) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walk(encodedExclusiveStartKey, proposeLimit, (key, value) -> {
				if (invokeCallback(table, key, value, callback)) {
					if (afterLock != null)
						afterLock.run();
					return true;
				}
				return false;
			});

			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkKey(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback, Runnable afterLock) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walkKey(encodedExclusiveStartKey, proposeLimit, key -> {
				if (invokeCallback(table, key, callback)) {
					if (afterLock != null)
						afterLock.run();
					return true;
				}
				return false;
			});

			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDesc(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback, Runnable afterLock) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walkDesc(encodedExclusiveStartKey, proposeLimit, (key, value) -> {
				if (invokeCallback(table, key, value, callback)) {
					if (afterLock != null)
						afterLock.run();
					return true;
				}
				return false;
			});

			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkKeyDesc(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback, Runnable afterLock) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walkKeyDesc(encodedExclusiveStartKey, proposeLimit, key -> {
				if (invokeCallback(table, key, callback)) {
					if (afterLock != null)
						afterLock.run();
					return true;
				}
				return false;
			});

			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabase(TableX<K, V> table, TableWalkHandle<K, V> callback) {
			return walk((key, value) -> {
				K k = table.decodeKey(ByteBuffer.Wrap(key));
				V v = table.decodeValue(ByteBuffer.Wrap(value));
				return callback.handle(k, v);
			});
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseDesc(TableX<K, V> table, TableWalkHandle<K, V> callback) {
			return walkDesc((key, value) -> {
				K k = table.decodeKey(ByteBuffer.Wrap(key));
				V v = table.decodeValue(ByteBuffer.Wrap(value));
				return callback.handle(k, v);
			});
		}
	}

	public abstract Transaction beginTransaction();

	public interface Transaction extends AutoCloseable {
		void commit();

		void rollback();
	}

	public static class DataWithVersion implements Serializable {
		public ByteBuffer data;
		public long version;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteByteBuffer(data);
			bb.WriteLong(version);
		}

		@Override
		public void decode(ByteBuffer bb) {
			data = ByteBuffer.Wrap(bb.ReadBytes());
			version = bb.ReadLong();
		}

		public static DataWithVersion decode(byte[] bytes) {
			var dv = new DataWithVersion();
			if (bytes != null)
				dv.decode(ByteBuffer.Wrap(bytes));
			return dv;
		}
	}

	/**
	 * 由后台数据库直接支持的存储过程。
	 * 直接操作后台数据库，不经过cache。
	 */
	public interface Operates {
		/*
		  table zeze_global {string global} 一条记录
		  table zeze_instances {int localId} 每个启动的gs一条记录
		  SetInUse(localId, global) // 没有启用cache-sync时，global是""
		    if (false == zeze_instances.insert(localId)) {
		      rollback;
		      return false; // 同一个localId只能启动一个。
		    }
		    globalNow = zeze_global.getOrAdd(global); // sql 应该是没有这样的方法的
		    if (globalNow != global) {
		      // 不管是否启用cache-sync，global都必须一致
		      rollback;
		      return false;
		    }
		    if (zeze_instances.count == 1)
		      return true; // 只有一个实例，肯定成功。
		    if (global.Count == 0) {
		      // 没有启用global，但是实例超过1。
		      rollback;
		      return false;
		    }
		    commit;
		    return true;
		*/
		void setInUse(int localId, String global);

		int clearInUse(int localId, String global);

		/*
		  if (Exist(key)) {
		    if (CurrentVersion != version)
		      return false;
		    UpdateData(data);
		    return (++CurrentVersion, true);
		  }
		  InsertData(data);
		  return (CurrentVersion = version, true);
		*/
		KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version);

		DataWithVersion getDataWithVersion(ByteBuffer key);
	}

	public static class NullOperates implements Operates {

		@Override
		public void setInUse(int localId, String global) {

		}

		@Override
		public int clearInUse(int localId, String global) {
			return 0;
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			return KV.create(version, true);
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			return null;
		}
	}
}
