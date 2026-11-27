package Zeze.Transaction;

import java.util.Objects;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Action1;
import Zeze.Util.KV;
import Zeze.Util.OutInt;
import Zeze.Util.ShutdownHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import static Zeze.Services.GlobalCacheManagerConst.StateModify;
import static Zeze.Services.GlobalCacheManagerConst.StateShare;

/**
 * 数据库后端抽象基类：管理表集合与事务生命周期，提供 KV 表公共遍历封装与 Operates 契约。
 */
public abstract class Database extends ReentrantLock {
	protected static final Logger logger = LogManager.getLogger(Database.class);

	// KV 表统一 key 长度上限（取 SqlServer 聚集索引键限制 900 字节），便于表跨后端迁移。
	public static final int eMaxKeyLength = 900;

	// 遍历游标（exclusiveStartKey）的长度上限，放行存量超长键并拦截异常游标。
	public static final int eMaxLegacyKeyLength = 2712;

	// 在 TableX 读写入口及后端 replace 兜底检查，超限在事务提交前拒绝。
	protected static void checkKvKeyLength(String tableName, ByteBuffer key) {
		if (key.size() > eMaxKeyLength)
			throw new IllegalArgumentException(
				"key too large for kv table '" + tableName + "': " + key.size() + " > " + eMaxKeyLength);
	}

	// 游标长度护栏，放行存量上限内的键。
	protected static void checkKvKeyLengthForLegacyAccess(String tableName, ByteBuffer key) {
		if (key.size() > eMaxLegacyKeyLength)
			throw new IllegalArgumentException("key too large for kv table legacy access '" + tableName
				+ "': " + key.size() + " > " + eMaxLegacyKeyLength);
	}

	static {
		ShutdownHook.init();
	}

	private final ConcurrentHashMap<String, Zeze.Transaction.Table> tables = new ConcurrentHashMap<>();
	// CopyOnWriteArrayList：启动 open 与运行期动态表 openDynamicTable 可并发注册，遍历免锁快照。
	private final CopyOnWriteArrayList<Storage<?, ?>> storages = new CopyOnWriteArrayList<>();
	private final DatabaseConf conf;
	private final String databaseUrl;
	private @Nullable Operates directOperates;
	private final @Nullable Application zeze;

	public Database(@Nullable Application zeze, DatabaseConf conf) {
		this.zeze = zeze;
		this.conf = conf;
		databaseUrl = conf.getDatabaseUrl();
	}

	public void __add_storage__(@Nullable Storage<?, ?> storage) {
		if (storage != null)
			storages.add(storage);
	}

	public @Nullable Application getZeze() {
		return zeze;
	}

	public final Collection<Zeze.Transaction.Table> getTables() {
		return tables.values();
	}

	public final Zeze.Transaction.@Nullable Table getTable(String name) {
		return tables.get(name);
	}

	public final void addTable(Zeze.Transaction.Table table) {
		if (tables.putIfAbsent(table.getName(), table) != null)
			throw new IllegalStateException("duplicate table=" + table.getName());
	}

	public final void removeTable(Zeze.Transaction.Table table) {
		try {
			table.close();
		} finally {
			tables.remove(table.getName(), table);
		}
	}

	public final DatabaseConf getConf() {
		return conf;
	}

	public final String getDatabaseUrl() {
		return databaseUrl;
	}

	public final @Nullable Operates getDirectOperates() {
		return directOperates;
	}

	protected final void setDirectOperates(Operates value) {
		directOperates = value;
	}

	public final void open(Application app) {
		var ts = tables.values().toArray(new Zeze.Transaction.Table[0]);
		var names = new String[ts.length];
		var ids = new int[ts.length];
		for (var i = 0; i < ts.length; ++i) {
			var table = ts[i];
			names[i] = table.getName();
			ids[i] = table.getId();
		}
		//noinspection DataFlowIssue
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

	public final void openDynamicTable(Application app, Zeze.Transaction.Table table) {
		var storage = table.open(app, this, null);
		if (storage != null) {
			storage.getDatabaseTable().waitReady();
			storages.add(storage);
		}
	}

	public void close() {
		for (Zeze.Transaction.Table table : tables.values())
			table.close();
		tables.clear();
		storages.clear();
	}

	public abstract Table openTable(String name, int id);

	public Table[] openTables(String[] names, int[] ids) {
		if (names.length != ids.length)
			throw new RuntimeException("tables name & id count mismatch.");
		var tables = new Table[names.length];
		for (int i = 0, n = names.length; i < n; i++)
			tables[i] = openTable(names[i], ids[i]);
		return tables;
	}

	/** 返回nio缓冲的remaining区间；仅窗口覆盖完整可访问数组时共享，否则复制且不改游标。 */
	public static byte[] copyIf(java.nio.ByteBuffer bb) {
		// slice可以从数组开头开始却只有较小capacity，必须与实际数组长度比较。
		if (bb.hasArray() && bb.position() == 0 && bb.arrayOffset() == 0 && bb.remaining() == bb.array().length)
			return bb.array();
		var bytes = new byte[bb.remaining()];
		bb.get(bb.position(), bytes);
		return bytes;
	}

	public static byte[] copyIf(byte[] bytes, int offset, int len) {
		if (offset == 0 && bytes.length == len)
			return bytes;
		return Arrays.copyOfRange(bytes, offset, offset + len);
	}

	public void relationalSql(String sql, Action1<ResultSet> handle) throws Exception {
		throw new UnsupportedOperationException();
	}

	public void renameTable(String tableOldName, String tableNewName) throws Exception {
		throw new UnsupportedOperationException();
	}

	public void dropTable(String tableName) {
		throw new UnsupportedOperationException();
	}

	public interface Table {
		boolean isNew();

		default void waitReady() {
		}

		Database getDatabase();

		default int keyOffsetInRawKey() {
			return 0;
		}

		/// ////////////////////////////////////////////////////////
		// TableX类型下沉到这里，准备添加关系表映射。
		<K extends Comparable<K>, V extends Bean> @Nullable V find(TableX<K, V> table, Object key);

		<K extends Comparable<K>, V extends Bean> boolean containsKey(TableX<K, V> table, Object key);

		// 这里的key，value具体含义由Table实现解释。
		// 对于KV表，key,value都是ByteBuffer类型。
		// 对于关系表，key,value是SQLStatement类型。
		void replace(Transaction t, Object key, Object value);

		void remove(Transaction t, Object key);

		<K extends Comparable<K>, V extends Bean>
		long walk(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		long walkDesc(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		long walkKey(TableX<K, V> table, TableWalkKey<K> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		long walkKeyDesc(TableX<K, V> table, TableWalkKey<K> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		@Nullable K walk(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
						 TableWalkHandle<K, V> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		@Nullable K walkDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
							 TableWalkHandle<K, V> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		@Nullable K walkKey(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
							TableWalkKey<K> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		@Nullable K walkKeyDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
								TableWalkKey<K> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		long walkDatabase(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		long walkDatabaseDesc(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(TableX<K, V> table, TableWalkKey<K> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		long walkDatabaseKeyDesc(TableX<K, V> table, TableWalkKey<K> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabase(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
								 TableWalkHandle<K, V> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
									 TableWalkHandle<K, V> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseKey(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
									TableWalkKey<K> callback) throws Exception;

		<K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseKeyDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
										TableWalkKey<K> callback) throws Exception;

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

		default void drop() {
			throw new UnsupportedOperationException();
		}

		default void tryAlter() {
			throw new UnsupportedOperationException();
		}
	}

	// KV表辅助类，实现所有的下沉的带类型接口。
	public static abstract class AbstractKVTable implements Table {
		/// /////////////////////////////////////////////////////////
		// KV表操作接口。
		public abstract @Nullable ByteBuffer find(ByteBuffer key);

		public abstract void replace(Transaction t, ByteBuffer key, ByteBuffer value);

		public abstract void remove(Transaction t, ByteBuffer key);

		/**
		 * 每一条记录回调。回调返回true继续遍历，false中断遍历。
		 * 被回调且返回 false 的中断记录也计入返回值（各后端统一契约）。
		 *
		 * @return 返回已经遍历的数量
		 */
		public abstract long walk(TableWalkHandleRaw callback) throws Exception;

		public abstract long walkKey(TableWalkKeyRaw callback) throws Exception;

		public abstract long walkDesc(TableWalkHandleRaw callback) throws Exception;

		public abstract long walkKeyDesc(TableWalkKeyRaw callback) throws Exception;

		public abstract @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												  TableWalkHandleRaw callback) throws Exception;

		public abstract @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
													 TableWalkKeyRaw callback) throws Exception;

		public abstract @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
													  TableWalkHandleRaw callback) throws Exception;

		public abstract @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
														 TableWalkKeyRaw callback) throws Exception;

		@Override
		public <K extends Comparable<K>, V extends Bean> @Nullable V find(TableX<K, V> table,
																		  Object key) {
			var bbValue = find(table.encodeKey(key));
			if (bbValue == null)
				return null;
			var value = table.newValue();
			value.decode(bbValue);
			return value;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean> boolean containsKey(TableX<K, V> table,
																			 Object key) {
			return find(table.encodeKey(key)) != null;
		}

		@Override
		public void replace(Transaction t, Object key, Object value) {
			replace(t, (ByteBuffer)key, (ByteBuffer)value);
		}

		@Override
		public void remove(Transaction t, Object key) {
			remove(t, (ByteBuffer)key);
		}

		private static <K extends Comparable<K>, V extends Bean>
		boolean invokeCallback(TableX<K, V> table, byte[] key, byte[] value,
							   TableWalkHandle<K, V> callback) throws Exception {
			K k = table.decodeKey(key);
			V v = null;
			var r = table.requireCache().get(k);
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
		boolean invokeCallback(TableX<K, V> table, byte[] key,
							   TableWalkKey<K> callback) throws Exception {
			K k = table.decodeKey(key);
			var r = table.requireCache().get(k);
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

		// 带游标遍历统一在此编码并检查游标长度上限。
		private static <K extends Comparable<K>, V extends Bean> @Nullable ByteBuffer encodeStartKeyChecked(
			TableX<K, V> table, @Nullable K exclusiveStartKey) {
			if (exclusiveStartKey == null)
				return null;
			var encoded = table.encodeKey(exclusiveStartKey);
			checkKvKeyLengthForLegacyAccess(table.getName(), encoded);
			return encoded;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walk(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var count = walk((key, value) -> invokeCallback(table, key, value, callback));
			return callback.endWalk(count);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDesc(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var count = walkDesc((key, value) -> invokeCallback(table, key, value, callback));
			return callback.endWalk(count);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkKey(TableX<K, V> table, TableWalkKey<K> callback) throws Exception {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var count = walkKey(key -> invokeCallback(table, key, callback));
			return callback.endWalk(count);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkKeyDesc(TableX<K, V> table, TableWalkKey<K> callback) throws Exception {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var count = walkKeyDesc(key -> invokeCallback(table, key, callback));
			return callback.endWalk(count);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walk(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
						 TableWalkHandle<K, V> callback) throws Exception {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = encodeStartKeyChecked(table, exclusiveStartKey);
			var lastKey = walk(encodedExclusiveStartKey, proposeLimit,
				(key, value) -> invokeCallback(table, key, value, callback));
			callback.endWalk(proposeLimit);
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
							 TableWalkHandle<K, V> callback) throws Exception {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = encodeStartKeyChecked(table, exclusiveStartKey);
			var lastKey = walkDesc(encodedExclusiveStartKey, proposeLimit,
				(key, value) -> invokeCallback(table, key, value, callback));
			callback.endWalk(proposeLimit);
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkKey(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
							TableWalkKey<K> callback) throws Exception {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = encodeStartKeyChecked(table, exclusiveStartKey);
			var lastKey = walkKey(encodedExclusiveStartKey, proposeLimit,
				key -> invokeCallback(table, key, callback));
			callback.endWalk(proposeLimit);
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkKeyDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
								TableWalkKey<K> callback) throws Exception {
			if (Zeze.Transaction.Transaction.getCurrent() != null)
				throw new IllegalStateException("must be called without transaction");

			var encodedExclusiveStartKey = encodeStartKeyChecked(table, exclusiveStartKey);
			var lastKey = walkKeyDesc(encodedExclusiveStartKey, proposeLimit,
				key -> invokeCallback(table, key, callback));
			callback.endWalk(proposeLimit);
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabase(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception {
			var count = walk((key, value) -> {
				K k = table.decodeKey(key);
				V v = table.decodeValue(value);
				return callback.handle(k, v);
			});
			return callback.endWalk(count);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseDesc(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception {
			var count = walkDesc((key, value) -> {
				K k = table.decodeKey(key);
				V v = table.decodeValue(value);
				return callback.handle(k, v);
			});
			return callback.endWalk(count);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(TableX<K, V> table, TableWalkKey<K> callback) throws Exception {
			var count = walkKey(key -> callback.handle(table.decodeKey(key)));
			return callback.endWalk(count);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKeyDesc(TableX<K, V> table, TableWalkKey<K> callback) throws Exception {
			var count = walkKeyDesc(key -> callback.handle(table.decodeKey(key)));
			return callback.endWalk(count);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabase(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
								 TableWalkHandle<K, V> callback) throws Exception {
			var encodedExclusiveStartKey = encodeStartKeyChecked(table, exclusiveStartKey);
			var lastKey = walk(encodedExclusiveStartKey, proposeLimit, (key, value) -> {
				K k = table.decodeKey(key);
				V v = table.decodeValue(value);
				return callback.handle(k, v);
			});
			callback.endWalk(proposeLimit);
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
									 TableWalkHandle<K, V> callback) throws Exception {
			var encodedExclusiveStartKey = encodeStartKeyChecked(table, exclusiveStartKey);
			var lastKey = walkDesc(encodedExclusiveStartKey, proposeLimit, (key, value) -> {
				K k = table.decodeKey(key);
				V v = table.decodeValue(value);
				return callback.handle(k, v);
			});
			callback.endWalk(proposeLimit);
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseKey(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
									TableWalkKey<K> callback) throws Exception {
			var encodedExclusiveStartKey = encodeStartKeyChecked(table, exclusiveStartKey);
			var lastKey = walkKey(encodedExclusiveStartKey, proposeLimit,
				key -> callback.handle(table.decodeKey(key)));
			callback.endWalk(proposeLimit);
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseKeyDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
										TableWalkKey<K> callback) throws Exception {
			var encodedExclusiveStartKey = encodeStartKeyChecked(table, exclusiveStartKey);
			var lastKey = walkKeyDesc(encodedExclusiveStartKey, proposeLimit,
				key -> callback.handle(table.decodeKey(key)));
			callback.endWalk(proposeLimit);
			return lastKey != null ? table.decodeKey(lastKey) : null;
		}
	}

	public abstract Transaction beginTransaction();

	/**
	 * 判定写失败是否为可重放的暂时性写冲突（如 Mongo WriteConflict），默认 false。
	 */
	public boolean isTransientWriteConflict(Throwable e) {
		return false;
	}

	public interface Transaction extends AutoCloseable {
		void commit();

		void rollback();
	}

	public static class DataWithVersion implements Serializable {
		public @Nullable ByteBuffer data;
		public long version;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteByteBuffer(Objects.requireNonNull(data));
			bb.WriteLong(version);
		}

		@Override
		public void decode(IByteBuffer bb) {
			data = ByteBuffer.Wrap(bb.ReadBytes());
			version = bb.ReadLong();
		}

		public static DataWithVersion decode(byte @Nullable [] bytes) {
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

		/**
		 * 按版本条件保存数据，返回 &lt;版本, 是否成功&gt;（true 含插入与更新，false 表示版本冲突）。
		 * 失败时须重新读取；注意首次插入时 JDBC 存储过程返回入参 version，其余后端返回 version + 1。
		 */
		@Nullable KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data,
															long version);

		@Nullable DataWithVersion getDataWithVersion(ByteBuffer key);

		// 只有mysql,postgres,sqlserver,dbh2实现这个。
		default boolean tryLock() {
			return true;
		}

		default void unlock() {
		}
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
		public @Nullable KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data,
																   long version) {
			return KV.create(version, true);
		}

		@Override
		public @Nullable DataWithVersion getDataWithVersion(ByteBuffer key) {
			return null;
		}
	}

	public static final class ReentrantLockHelper {
		private final ThreadLocal<@Nullable OutInt> count = new ThreadLocal<>();

		/**
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
		 * @return true 计数达到0，可以执行真正的unlock实现。
		 */
		public boolean tryUnlock() {
			var c = count.get();
			if (c == null)
				throw new IllegalStateException("tryUnlock without lock held");
			return --c.value == 0;
		}

		public void unlockSuccess() {
			count.remove();
		}
	}
}
