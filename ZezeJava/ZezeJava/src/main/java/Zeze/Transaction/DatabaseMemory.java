package Zeze.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Zeze.Transaction.Table.storage 为 null 时，就表示内存表了。这个实现是为了测试 checkpoint 流程。
 */
public final class DatabaseMemory extends Database implements Database.Operates {
	private static final HashMap<ByteBuffer, DataWithVersion> dataWithVersions = new HashMap<>();
	private static final byte @NotNull [] removed = ByteBuffer.Empty;
	private static final HashMap<String, HashMap<String, TableMemory>> databaseTables = new HashMap<>();
	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public static void clear() {
		lock.writeLock().lock();
		try {
			databaseTables.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public DatabaseMemory(@Nullable Application zeze, @NotNull DatabaseConf conf) {
		super(zeze, conf);
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : this);
	}

	@Override
	public int clearInUse(int localId, @NotNull String global) {
		return 0;
	}

	@Override
	public void setInUse(int localId, @NotNull String global) {
	}

	@Override
	public @Nullable DataWithVersion getDataWithVersion(@NotNull ByteBuffer key) {
		lock();
		try {
			var exist = dataWithVersions.get(key);
			if (exist == null)
				return null;
			var copy = new DataWithVersion();
			copy.data = ByteBuffer.Wrap(exist.data.Copy());
			copy.version = exist.version;
			return copy;
		} finally {
			unlock();
		}
	}

	@Override
	public @NotNull KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key, @NotNull ByteBuffer data,
															  long version) {
		lock();
		try {
			var exist = dataWithVersions.get(key);
			if (exist != null) {
				if (exist.version != version)
					return KV.create(exist.version, false);
				exist.data = ByteBuffer.Wrap(data.Copy());
				return KV.create(++exist.version, true);
			}
			DataWithVersion tempVar = new DataWithVersion();
			tempVar.data = ByteBuffer.Wrap(data.Copy());
			tempVar.version = version;
			dataWithVersions.put(ByteBuffer.Wrap(key.Copy()), tempVar);
			return KV.create(version, true);
		} finally {
			unlock();
		}
	}

	public final class MemTrans implements Transaction {
		private final HashMap<String, HashMap<ByteBuffer, byte[]>> batch = new HashMap<>();

		@Override
		public void commit() {
			int n = 0;
			// 整个db同步。
			lock.writeLock().lock();
			try {
				var db = databaseTables.computeIfAbsent(getDatabaseUrl(), __ -> new HashMap<>());
				for (var e : batch.entrySet()) {
					//if (e.getValue().size() > 2)
					//	System.err.println("commit for: " + e.getKey() + " keys:" + e.getValue().keySet());
					var map = db.computeIfAbsent(e.getKey(), TableMemory::new).map;
					for (var r : e.getValue().entrySet()) {
						if (r.getValue() == removed)
							map.remove(r.getKey());
						else
							map.put(r.getKey(), r.getValue());
						n++;
					}
				}
			} finally {
				lock.writeLock().unlock();
				if (n >= 10000)
					logger.warn("MemTrans commit too many records: {}", n);
			}
		}

		@Override
		public void rollback() {
		}

		@Override
		public void close() {
		}

		public void remove(@NotNull String tableName, @NotNull ByteBuffer key) {
			batch.computeIfAbsent(tableName, __ -> new HashMap<>()).put(ByteBuffer.Wrap(key.Copy()), removed);
		}

		public void replace(@NotNull String tableName, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			batch.computeIfAbsent(tableName, __ -> new HashMap<>()).put(ByteBuffer.Wrap(key.Copy()), value.Copy());
		}
	}

	@Override
	public @NotNull Transaction beginTransaction() {
		return new MemTrans();
	}

	@Override
	public @NotNull Database.Table openTable(@NotNull String name, int id) {
		lock.writeLock().lock();
		try {
			var tables = databaseTables.computeIfAbsent(getDatabaseUrl(), __ -> new HashMap<>());
			return tables.computeIfAbsent(name, TableMemory::new);
		} finally {
			lock.writeLock().unlock();
		}
	}

	// 仅支持从一个db原子的查询数据。

	// 多表原子查询。
	public @NotNull HashMap<String, Map<ByteBuffer, ByteBuffer>> finds(
			@NotNull Map<String, Set<ByteBuffer>> tableKeys) {
		var result = new HashMap<String, Map<ByteBuffer, ByteBuffer>>(tableKeys.size());
		for (var tks : tableKeys.entrySet())
			result.put(tks.getKey(), new HashMap<>(tks.getValue().size()));
		lock.readLock().lock();
		try {
			var db = databaseTables.get(getDatabaseUrl());
			for (var tks : tableKeys.entrySet()) {
				var tableName = tks.getKey();
				var table = db != null ? db.get(tableName) : null;
				var map = table != null ? table.map : null;
				var tableFinds = result.get(tableName);
				for (var key : tks.getValue()) {
					ByteBuffer value = null;
					if (map != null) {
						var v = map.get(key);
						if (v != null)
							value = ByteBuffer.Wrap(ByteBuffer.Copy(v));
					}
					tableFinds.put(key, value); // also put null value
				}
			}
		} finally {
			lock.readLock().unlock();
		}
		return result;
	}

	// 单表原子查询
	public @NotNull HashMap<ByteBuffer, ByteBuffer> finds(@NotNull String tableName, @NotNull Set<ByteBuffer> keys) {
		var result = new HashMap<ByteBuffer, ByteBuffer>(keys.size());
		// System.err.println("finds for: " + tableName + " keys.size=" + keys.size());
		lock.readLock().lock();
		try {
			var db = databaseTables.get(getDatabaseUrl());
			var table = db != null ? db.get(tableName) : null;
			var map = table != null ? table.map : null;
			for (var key : keys) {
				ByteBuffer value = null;
				if (map != null) {
					var v = map.get(key);
					if (v != null)
						value = ByteBuffer.Wrap(ByteBuffer.Copy(v));
				}
				result.put(key, value); // also put null value
			}
		} finally {
			lock.readLock().unlock();
		}
		return result;
	}

	public final class TableMemory extends Database.AbstractKVTable {
		private final @NotNull String name;
		private final TreeMap<ByteBuffer, byte[]> map = new TreeMap<>(ByteBuffer::compareTo);

		public TableMemory(@NotNull String name) {
			this.name = name;
		}

		@Override
		public @NotNull DatabaseMemory getDatabase() {
			return DatabaseMemory.this;
		}

		public @NotNull String getName() {
			return name;
		}

		@Override
		public boolean isNew() {
			return true;
		}

		@Override
		public @Nullable ByteBuffer find(@NotNull ByteBuffer key) {
			lock.readLock().lock();
			try {
				var value = map.get(key);
				return value != null ? ByteBuffer.Wrap(ByteBuffer.Copy(value)) : null;
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public void remove(@NotNull Transaction t, @NotNull ByteBuffer key) {
			((MemTrans)t).remove(name, key);
		}

		@Override
		public void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			((MemTrans)t).replace(name, key, value);
		}

		@Override
		public void clear() {
			map.clear();
		}

		@Override
		public long getSize() {
			return map.size();
		}

		@Override
		public long getSizeApproximation() {
			return map.size();
		}

		@Override
		public long walk(@NotNull TableWalkHandleRaw callback) throws Exception {
			ByteBuffer[] keys;
			byte[][] values;
			int i = 0, n;
			lock.readLock().lock();
			try {
				n = map.size();
				keys = new ByteBuffer[n];
				values = new byte[n][];
				for (var e : map.entrySet()) {
					keys[i] = e.getKey();
					values[i++] = e.getValue();
				}
			} finally {
				lock.readLock().unlock();
			}
			long count = 0;
			for (i = 0; i < n; i++) {
				count++;
				if (!callback.handle(keys[i].Copy(), values[i].clone()))
					break;
			}
			return count;
		}

		@Override
		public long walkKey(@NotNull TableWalkKeyRaw callback) throws Exception {
			ByteBuffer[] keys;
			lock.readLock().lock();
			try {
				keys = map.keySet().toArray(new ByteBuffer[map.size()]);
			} finally {
				lock.readLock().unlock();
			}
			long count = 0;
			for (var key : keys) {
				count++;
				if (!callback.handle(key.Copy()))
					break;
			}
			return count;
		}

		@Override
		public long walkDesc(@NotNull TableWalkHandleRaw callback) throws Exception {
			ByteBuffer[] keys;
			byte[][] values;
			int i = 0, n;
			lock.readLock().lock();
			try {
				n = map.size();
				keys = new ByteBuffer[n];
				values = new byte[n][];
				for (var e : map.descendingMap().entrySet()) {
					keys[i] = e.getKey();
					values[i++] = e.getValue();
				}
			} finally {
				lock.readLock().unlock();
			}
			long count = 0;
			for (i = 0; i < n; i++) {
				count++;
				if (!callback.handle(keys[i].Copy(), values[i].clone()))
					break;
			}
			return count;
		}

		@Override
		public long walkKeyDesc(@NotNull TableWalkKeyRaw callback) throws Exception {
			ByteBuffer[] keys;
			lock.readLock().lock();
			try {
				keys = map.descendingKeySet().toArray(new ByteBuffer[map.size()]);
			} finally {
				lock.readLock().unlock();
			}
			long count = 0;
			for (var key : keys) {
				count++;
				if (!callback.handle(key.Copy()))
					break;
			}
			return count;
		}

		@Override
		public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
										 @NotNull TableWalkHandleRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			final var keys = new ArrayList<ByteBuffer>();
			final var values = new ArrayList<byte[]>();
			lock.readLock().lock();
			try {
				var it = exclusiveStartKey == null
						? map.entrySet().iterator()
						: map.tailMap(exclusiveStartKey).entrySet().iterator();
				if (it.hasNext()) {
					var next = it.next();
					if (!next.getKey().equals(exclusiveStartKey)) { // 第一个Item可能需要忽略
						keys.add(next.getKey());
						values.add(next.getValue());
						proposeLimit--;
					}
					while (proposeLimit-- > 0 && it.hasNext()) {
						next = it.next();
						keys.add(next.getKey());
						values.add(next.getValue());
					}
				}
			} finally {
				lock.readLock().unlock();
			}
			byte[] lastKey = null;
			for (int i = 0, n = keys.size(); i < n; i++) {
				lastKey = keys.get(i).Copy();
				if (!callback.handle(lastKey, values.get(i).clone()))
					break;
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											@NotNull TableWalkKeyRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			final var keys = new ArrayList<ByteBuffer>();
			lock.readLock().lock();
			try {
				var it = exclusiveStartKey == null
						? map.keySet().iterator()
						: map.tailMap(exclusiveStartKey).keySet().iterator();
				if (it.hasNext()) {
					var key = it.next();
					if (!key.equals(exclusiveStartKey)) { // 第一个Item可能需要忽略
						keys.add(key);
						proposeLimit--;
					}
					while (proposeLimit-- > 0 && it.hasNext())
						keys.add(it.next());
				}
			} finally {
				lock.readLock().unlock();
			}
			byte[] lastKey = null;
			for (var key : keys) {
				lastKey = key.Copy();
				if (!callback.handle(lastKey))
					break;
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											 @NotNull TableWalkHandleRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			final var keys = new ArrayList<ByteBuffer>();
			final var values = new ArrayList<byte[]>();
			lock.readLock().lock();
			try {
				var it = exclusiveStartKey == null
						? map.descendingMap().entrySet().iterator()
						: map.descendingMap().tailMap(exclusiveStartKey).entrySet().iterator();
				if (it.hasNext()) {
					var next = it.next();
					if (!next.getKey().equals(exclusiveStartKey)) { // 第一个Item可能需要忽略
						keys.add(next.getKey());
						values.add(next.getValue());
						proposeLimit--;
					}
					while (proposeLimit-- > 0 && it.hasNext()) {
						next = it.next();
						keys.add(next.getKey());
						values.add(next.getValue());
					}
				}
			} finally {
				lock.readLock().unlock();
			}
			byte[] lastKey = null;
			for (int i = 0, n = keys.size(); i < n; i++) {
				lastKey = keys.get(i).Copy();
				if (!callback.handle(lastKey, values.get(i).clone()))
					break;
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												@NotNull TableWalkKeyRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			final var keys = new ArrayList<ByteBuffer>();
			lock.readLock().lock();
			try {
				var it = exclusiveStartKey == null
						? map.descendingMap().keySet().iterator()
						: map.descendingMap().tailMap(exclusiveStartKey).keySet().iterator();
				if (it.hasNext()) {
					var key = it.next();
					if (!key.equals(exclusiveStartKey)) { // 第一个Item可能需要忽略
						keys.add(key);
						proposeLimit--;
					}
					while (proposeLimit-- > 0 && it.hasNext())
						keys.add(it.next());
				}
			} finally {
				lock.readLock().unlock();
			}
			byte[] lastKey = null;
			for (var key : keys) {
				lastKey = key.Copy();
				if (!callback.handle(lastKey))
					break;
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public void close() {
		}
	}
}
