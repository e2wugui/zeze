package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.KV;
import Zeze.Util.OutInt;
import Zeze.Util.ShutdownHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Services.GlobalCacheManagerConst.StateModify;
import static Zeze.Services.GlobalCacheManagerConst.StateShare;

/**
 * 数据访问的效率主要来自TableCache的命中。根据以往的经验，命中率是很高的。
 * 所以数据库层就不要求很高的效率。马马虎虎就可以了。
 */
public abstract class Database extends ReentrantLock {
	protected static final Logger logger = LogManager.getLogger(Database.class);

	// 当数据库对Key长度有限制时，使用这个常量。这个数字来自 PolarDb-X。其中MySql 8是3072.
	// 以后需要升级时，修改这个常量。但是对于已经存在的表，需要自己完成Alter。
	public static final int eMaxKeyLength = 3070;

	private static final boolean isDebugEnabled = logger.isDebugEnabled();

	static {
		ShutdownHook.init();
	}

	private final ConcurrentHashMap<String, Zeze.Transaction.Table> tables = new ConcurrentHashMap<>();
	private final ArrayList<Storage<?, ?>> storages = new ArrayList<>();
	private final @NotNull DatabaseConf conf;
	private final @NotNull String databaseUrl;
	private Operates directOperates;
	private final Application zeze;

	public Database(@Nullable Application zeze, @NotNull DatabaseConf conf) {
		this.zeze = zeze;
		this.conf = conf;
		databaseUrl = conf.getDatabaseUrl();
	}

	public void __add_storage__(Storage<?, ?> storage) {
		if (null != storage)
			storages.add(storage);
	}

	void replaceStorage(Storage<?, ?> exist, Storage<?, ?> replace) {
		if (null == replace) {
			for (var i = 0; i < storages.size(); ++i) {
				if (storages.get(i) == exist) {
					storages.remove(i);
					break;
				}
			}
		} else {
			for (var i = 0; i < storages.size(); ++i) {
				if (storages.get(i) == exist) {
					storages.set(i, replace);
					break;
				}
			}
		}
	}

	public @NotNull Application getZeze() {
		return zeze;
	}

	public final @NotNull Collection<Zeze.Transaction.Table> getTables() {
		return tables.values();
	}

	public final @Nullable Zeze.Transaction.Table getTable(@NotNull String name) {
		return tables.get(name);
	}

	public final void replaceTable(@NotNull Zeze.Transaction.Table table) {
		tables.put(table.getName(), table); // always put.
	}

	public final void addTable(@NotNull Zeze.Transaction.Table table) {
		if (null != tables.putIfAbsent(table.getName(), table))
			throw new IllegalStateException("duplicate table=" + table.getName());
	}

	public final void removeTable(@NotNull Zeze.Transaction.Table table) {
		table.close();
		tables.remove(table.getName());
	}

	public final @NotNull DatabaseConf getConf() {
		return conf;
	}

	public final @NotNull String getDatabaseUrl() {
		return databaseUrl;
	}

	public final Operates getDirectOperates() {
		return directOperates;
	}

	protected final void setDirectOperates(@NotNull Operates value) {
		directOperates = value;
	}

	public final void open(@NotNull Application app) {
		var ts = tables.values().toArray(new Zeze.Transaction.Table[tables.size()]);
		var names = new String[ts.length];
		var ids = new int[ts.length];
		for (var i = 0; i < ts.length; ++i) {
			var table = ts[i];
			names[i] = table.getName();
			ids[i] = table.getId();
		}
		var localTables = app.getLocalRocksCacheDb().openTables(names, ids);
		for (var i = 0; i < ts.length; ++i) {
			var table = ts[i];
			var storage = table.open(app, this, localTables[i]);
			if (storage != null)
				storages.add(storage);
		}
		for (var storage : storages) {
			storage.getDatabaseTable().waitReady();
		}
	}

	public final void openDynamicTable(@NotNull Application app, @NotNull Zeze.Transaction.Table table) {
		var storage = table.open(app, this, null);
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

	public abstract @NotNull Table openTable(@NotNull String name, int id);

	public @NotNull Table @NotNull [] openTables(String @NotNull [] names, int @NotNull [] ids) {
		if (names.length != ids.length)
			throw new RuntimeException("tables name & id count mismatch.");
		var tables = new Table[names.length];
		for (int i = 0, n = names.length; i < n; i++)
			tables[i] = openTable(names[i], ids[i]);
		return tables;
	}

	public static byte @NotNull [] copyIf(@NotNull java.nio.ByteBuffer bb) {
		if (bb.limit() == bb.capacity() && bb.arrayOffset() == 0)
			return bb.array();
		return Arrays.copyOfRange(bb.array(), bb.arrayOffset(), bb.limit());
	}

	public static byte @NotNull [] copyIf(byte @NotNull [] bytes, int offset, int len) {
		if (offset == 0 && bytes.length == len)
			return bytes;
		return Arrays.copyOfRange(bytes, offset, offset + len);
	}

	public interface Table {
		boolean isNew();

		void waitReady();

		@NotNull Database getDatabase();

		default int keyOffsetInRawKey() {
			return 0;
		}

		///////////////////////////////////////////////////////////
		// TableX类型下沉到这里，准备添加关系表映射。
		<K extends Comparable<K>, V extends Bean> @Nullable V find(@NotNull TableX<K, V> table, @NotNull Object key);

		<K extends Comparable<K>, V extends Bean> boolean containsKey(@NotNull TableX<K, V> table, @NotNull Object key);

		// 这里的key，value具体含义由Table实现解释。
		// 对于KV表，key,value都是ByteBuffer类型。
		// 对于关系表，key,value是SQLStatement类型。
		void replace(@NotNull Transaction t, @NotNull Object key, @NotNull Object value);

		void remove(@NotNull Transaction t, @NotNull Object key);

		<K extends Comparable<K>, V extends Bean>
		long walk(@NotNull TableX<K, V> table, @NotNull TableWalkHandle<K, V> callback);

		<K extends Comparable<K>, V extends Bean>
		long walkDesc(@NotNull TableX<K, V> table, @NotNull TableWalkHandle<K, V> callback);

		<K extends Comparable<K>, V extends Bean>
		long walkKey(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback);

		<K extends Comparable<K>, V extends Bean>
		long walkKeyDesc(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback);

		<K extends Comparable<K>, V extends Bean>
		K walk(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
			   @NotNull TableWalkHandle<K, V> callback);

		<K extends Comparable<K>, V extends Bean>
		K walkDesc(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
				   @NotNull TableWalkHandle<K, V> callback);

		<K extends Comparable<K>, V extends Bean>
		K walkKey(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
				  @NotNull TableWalkKey<K> callback);

		<K extends Comparable<K>, V extends Bean>
		K walkKeyDesc(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
					  @NotNull TableWalkKey<K> callback);

		<K extends Comparable<K>, V extends Bean>
		long walkDatabase(@NotNull TableX<K, V> table, @NotNull TableWalkHandle<K, V> callback);

		<K extends Comparable<K>, V extends Bean>
		long walkDatabaseDesc(@NotNull TableX<K, V> table, @NotNull TableWalkHandle<K, V> callback);

		<K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback);

		<K extends Comparable<K>, V extends Bean>
		long walkDatabaseKeyDesc(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback);

		<K extends Comparable<K>, V extends Bean>
		K walkDatabase(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
					   @NotNull TableWalkHandle<K, V> callback);

		<K extends Comparable<K>, V extends Bean>
		K walkDatabaseDesc(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
						   @NotNull TableWalkHandle<K, V> callback);

		<K extends Comparable<K>, V extends Bean>
		K walkDatabaseKey(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
						  @NotNull TableWalkKey<K> callback);

		<K extends Comparable<K>, V extends Bean>
		K walkDatabaseKeyDesc(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
							  @NotNull TableWalkKey<K> callback);

		void close();

		default void clear() {
			throw new UnsupportedOperationException();
		}

		default long getSize() {
			throw new UnsupportedOperationException();
		}

		default long getSizeApproximation() {
			throw new UnsupportedOperationException();
		}
	}

	// KV表辅助类，实现所有的下沉的带类型接口。
	public static abstract class AbstractKVTable implements Table {
		@Override
		public void waitReady() {
		}

		////////////////////////////////////////////////////////////
		// KV表操作接口。
		public abstract @Nullable ByteBuffer find(@NotNull ByteBuffer key);

		public abstract void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value);

		public abstract void remove(@NotNull Transaction t, @NotNull ByteBuffer key);

		/**
		 * 每一条记录回调。回调返回true继续遍历，false中断遍历。
		 *
		 * @return 返回已经遍历的数量
		 */
		public abstract long walk(@NotNull TableWalkHandleRaw callback);

		public abstract long walkKey(@NotNull TableWalkKeyRaw callback);

		public abstract long walkDesc(@NotNull TableWalkHandleRaw callback);

		public abstract long walkKeyDesc(@NotNull TableWalkKeyRaw callback);

		public abstract @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												  @NotNull TableWalkHandleRaw callback);

		public abstract @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
													 @NotNull TableWalkKeyRaw callback);

		public abstract @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
													  @NotNull TableWalkHandleRaw callback);

		public abstract @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
														 @NotNull TableWalkKeyRaw callback);

		@Override
		public <K extends Comparable<K>, V extends Bean> V find(@NotNull TableX<K, V> table, @NotNull Object key) {
			var bbKey = table.encodeKey(key);
			var bbValue = find(bbKey);
			if (bbValue == null)
				return null;
			var value = table.newValue();
			value.decode(bbValue);
			return value;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean> boolean containsKey(@NotNull TableX<K, V> table,
																			 @NotNull Object key) {
			return find(table.encodeKey(key)) != null;
		}

		@Override
		public void replace(@NotNull Transaction t, @NotNull Object key, @NotNull Object value) {
			replace(t, (ByteBuffer)key, (ByteBuffer)value);
		}

		@Override
		public void remove(@NotNull Transaction t, @NotNull Object key) {
			remove(t, (ByteBuffer)key);
		}

		private static <K extends Comparable<K>, V extends Bean>
		boolean invokeCallback(TableX<K, V> table, byte[] key, byte[] value, TableWalkHandle<K, V> callback) {
			K k = table.decodeKey(key);
			V v = null;
			var r = table.getCache().get(k);
			if (r != null) {
				r.enterFairLock();
				try {
					if (r.getState() == StateShare || r.getState() == StateModify) {
						// 拥有正确的状态：
						v = r.copyValue();
						if (v == null)
							return true; // 已经被删除，但是还没有checkpoint的记录看不到。返回true，继续循环。
					}
				} finally {
					r.exitFairLock();
				}
				// else GlobalCacheManager.StateInvalid
				// 继续后面的处理：使用数据库中的数据。
			}
			// cache中有数据，使用最新的数据; 缓存中不存在或者正在被删除但还没提交，使用数据库中的数据。
			return callback.handle(k, v != null ? v : table.decodeValue(value));
		}

		private static <K extends Comparable<K>, V extends Bean>
		boolean invokeCallback(TableX<K, V> table, byte[] key, TableWalkKey<K> callback) {
			K k = table.decodeKey(key);
			var r = table.getCache().get(k);
			if (r != null) {
				r.enterFairLock();
				try {
					if (r.getState() == StateShare || r.getState() == StateModify) {
						// 拥有正确的状态：
						if (!r.containsValue())
							return true; // 已经被删除，但是还没有checkpoint的记录看不到。返回true，继续循环。
					}
				} finally {
					r.exitFairLock();
				}
				// else GlobalCacheManager.StateInvalid
				// 继续后面的处理：使用数据库中的数据。
			}
			// 缓存中不存在或者正在被删除，使用数据库中的数据。
			return callback.handle(k);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walk(TableX<K, V> table, TableWalkHandle<K, V> callback) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			return walk((key, value) -> invokeCallback(table, key, value, callback));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDesc(TableX<K, V> table, TableWalkHandle<K, V> callback) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			return walkDesc((key, value) -> invokeCallback(table, key, value, callback));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkKey(TableX<K, V> table, TableWalkKey<K> callback) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			return walkKey(key -> invokeCallback(table, key, callback));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkKeyDesc(TableX<K, V> table, TableWalkKey<K> callback) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			return walkKeyDesc(key -> invokeCallback(table, key, callback));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walk(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walk(encodedExclusiveStartKey, proposeLimit,
					(key, value) -> invokeCallback(table, key, value, callback));

			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDesc(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walkDesc(encodedExclusiveStartKey, proposeLimit,
					(key, value) -> invokeCallback(table, key, value, callback));

			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkKey(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walkKey(encodedExclusiveStartKey, proposeLimit,
					key -> invokeCallback(table, key, callback));

			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkKeyDesc(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback) {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walkKeyDesc(encodedExclusiveStartKey, proposeLimit,
					key -> invokeCallback(table, key, callback));

			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabase(TableX<K, V> table, TableWalkHandle<K, V> callback) {
			return walk((key, value) -> {
				K k = table.decodeKey(key);
				V v = table.decodeValue(value);
				return callback.handle(k, v);
			});
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseDesc(TableX<K, V> table, TableWalkHandle<K, V> callback) {
			return walkDesc((key, value) -> {
				K k = table.decodeKey(key);
				V v = table.decodeValue(value);
				return callback.handle(k, v);
			});
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(TableX<K, V> table, TableWalkKey<K> callback) {
			return walkKey(key -> callback.handle(table.decodeKey(key)));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKeyDesc(TableX<K, V> table, TableWalkKey<K> callback) {
			return walkKeyDesc(key -> callback.handle(table.decodeKey(key)));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDatabase(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback) {
			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walk(encodedExclusiveStartKey, proposeLimit, (key, value) -> {
				K k = table.decodeKey(key);
				V v = table.decodeValue(value);
				return callback.handle(k, v);
			});
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDatabaseDesc(TableX<K, V> table, K exclusiveStartKey, int proposeLimit,
						   TableWalkHandle<K, V> callback) {
			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walkDesc(encodedExclusiveStartKey, proposeLimit, (key, value) -> {
				K k = table.decodeKey(key);
				V v = table.decodeValue(value);
				return callback.handle(k, v);
			});
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDatabaseKey(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback) {
			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walkKey(encodedExclusiveStartKey, proposeLimit,
					key -> callback.handle(table.decodeKey(key)));
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDatabaseKeyDesc(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback) {
			var encodedExclusiveStartKey = exclusiveStartKey != null ? table.encodeKey(exclusiveStartKey) : null;
			var lastKey = walkKeyDesc(encodedExclusiveStartKey, proposeLimit,
					key -> callback.handle(table.decodeKey(key)));
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}
	}

	public abstract @NotNull Transaction beginTransaction();

	public interface Transaction extends AutoCloseable {
		void commit();

		void rollback();
	}

	public static class DataWithVersion implements Serializable {
		public ByteBuffer data;
		public long version;

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteByteBuffer(data);
			bb.WriteLong(version);
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			data = ByteBuffer.Wrap(bb.ReadBytes());
			version = bb.ReadLong();
		}

		public static @NotNull DataWithVersion decode(byte @Nullable [] bytes) {
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
		void setInUse(int localId, @NotNull String global);

		int clearInUse(int localId, @NotNull String global);

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
		@Nullable KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key, @NotNull ByteBuffer data,
															long version);

		@Nullable DataWithVersion getDataWithVersion(@NotNull ByteBuffer key);

		// 只有mysql,dbh2实现这个。
		default boolean tryLock() {
			return true;
		}

		default void unlock() {
		}
	}

	public static class NullOperates implements Operates {
		@Override
		public void setInUse(int localId, @NotNull String global) {
		}

		@Override
		public int clearInUse(int localId, @NotNull String global) {
			return 0;
		}

		@Override
		public @Nullable KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key, @NotNull ByteBuffer data,
																   long version) {
			return KV.create(version, true);
		}

		@Override
		public @Nullable DataWithVersion getDataWithVersion(@NotNull ByteBuffer key) {
			return null;
		}
	}

	public static final class ReentrantLockHelper {
		private final ThreadLocal<OutInt> count = new ThreadLocal<>();

		/**
		 * lock
		 *
		 * @return false 表示第一次调用，此时需要执行真正的lock实现。
		 */
		public boolean tryLock() {
			var c = count.get();
			if (c != null && c.value > 0) {
				c.value++;
				return true;
			}
			return false;
		}

		public void lockSuccess() {
			count.set(new OutInt(1));
		}

		/**
		 * unlock
		 *
		 * @return true 计数达到0，可以执行真正的unlock实现。
		 */
		public boolean tryUnlock() {
			var c = count.get();
			return --c.value == 0;
		}

		public void unlockSuccess() {
			count.remove();
		}
	}
}
