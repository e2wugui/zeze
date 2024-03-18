package Zeze.Transaction;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutObject;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class DatabaseRedis extends Database {
	private final @NotNull JedisPool jedisPool;
	private final ConcurrentHashMap<String, TableRedis> tableMap = new ConcurrentHashMap<>();

	public DatabaseRedis(@NotNull Application zeze, @NotNull Config.DatabaseConf conf) {
		super(zeze, conf);

		var config = new JedisPoolConfig();
		config.setMaxTotal(1024); // 并发连接上限,默认8
		config.setMaxIdle(8); // 空闲连接上限,默认8
		config.setMaxWait(Duration.ofMillis(10_000)); // 等待可用连接的时长上限,超时会抛JedisConnectionException,默认-1表示没有超时

		try {
			var uri = new URI(conf.getDatabaseUrl()); // redis://xxx.xxx.xxx.xxx:yyy/?password=zzz
			jedisPool = new JedisPool(config, uri);
			// DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
			// setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesRocksDb());
		} catch (URISyntaxException e) {
			Task.forceThrow(e);
			throw new AssertionError(); // never run here
		}
	}

	@Override
	public synchronized void close() {
		logger.info("Close: {}", getDatabaseUrl());
		super.close();
		jedisPool.close();
	}

	@Override
	public @NotNull Transaction beginTransaction() {
		return new RedisTrans();
	}

	private final class RedisTrans implements Transaction {
		private @Nullable RocksDatabase.Batch batch;

		private RocksDatabase.Batch getBatch() {
			throw new UnsupportedOperationException();
		}

		void put(byte[] key, byte[] value, TableRedis table) {
			try (var jedis = jedisPool.getResource()) {
				jedis.set(table.buildKey(key), value);
			}
		}

		void remove(byte[] key, TableRedis table) {
			try (var jedis = jedisPool.getResource()) {
				jedis.del(table.buildKey(key));
			}
		}

		@Override
		public void commit() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void rollback() {
		}

		@Override
		public void close() {
			if (batch != null) {
				batch.close();
				batch = null;
			}
		}
	}

	public synchronized @NotNull TableRedis getOrAddTable(@NotNull String name, @Nullable OutObject<Boolean> isNew) {
		var table = tableMap.get(name);
		if (table != null) {
			if (isNew != null)
				isNew.value = false;
			return table;
		}
		if (isNew != null)
			isNew.value = true;
		table = new TableRedis(name, true);
		tableMap.put(name, table);
		return table;
	}

	@Override
	public Table openTable(String name) {
		return getOrAddTable(name, null);
	}

	@Override
	public synchronized @NotNull Table @NotNull [] openTables(String @NotNull [] names) {
		var n = names.length;
		var tables = new Table[n];
		for (int i = 0; i < n; i++)
			tables[i] = getOrAddTable(names[i], null);
		return tables;
	}

	public final class TableRedis extends AbstractKVTable {
		private final byte[] tableName;
		private final boolean isNew;

		TableRedis(String tableName, boolean isNew) {
			this.tableName = tableName.getBytes(StandardCharsets.UTF_8);
			this.isNew = isNew;
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public DatabaseRedis getDatabase() {
			return DatabaseRedis.this;
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		@Override
		public void close() {
		}

		private byte[] buildKey(ByteBuffer key) {
			var keyBytes = Arrays.copyOf(tableName, tableName.length + 1 + key.size());
			System.arraycopy(key.Bytes, key.ReadIndex, keyBytes, tableName.length + 1, key.size());
			return keyBytes;
		}

		private byte[] buildKey(byte[] key) {
			var keyBytes = Arrays.copyOf(tableName, tableName.length + 1 + key.length);
			System.arraycopy(key, 0, keyBytes, tableName.length + 1, key.length);
			return keyBytes;
		}

		@Override
		public ByteBuffer find(ByteBuffer key) {
			try (var jedis = jedisPool.getResource()) {
				var value = jedis.get(buildKey(key));
				return value != null ? ByteBuffer.Wrap(value) : null;
			}
		}

		@Override
		public void remove(Transaction txn, ByteBuffer key) {
			((RedisTrans)txn).remove(key.CopyIf(), this);
		}

		@Override
		public void replace(Transaction txn, ByteBuffer key, ByteBuffer value) {
			((RedisTrans)txn).put(key.CopyIf(), value.CopyIf(), this);
		}

		@Override
		public long getSize() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getSizeApproximation() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long walk(TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long walkDesc(TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long walkKeyDesc(TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}
	}
/*
	private final class OperatesRocksDb implements Operates {
		private final TableRedis table = getOrAddTable("zeze.OperatesRocksDb.Schemas", null);

		@Override
		public synchronized DataWithVersion getDataWithVersion(ByteBuffer key) {
			return DataWithVersion.decode(table.get(key.Bytes, key.ReadIndex, key.size()));
		}

		@Override
		public synchronized KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			var dv = DataWithVersion.decode(table.get(key.Bytes, key.ReadIndex, key.size()));
			if (dv.version != version)
				return KV.create(version, false);

			dv.version = ++version;
			dv.data = data;
			var value = ByteBuffer.Allocate(5 + 9 + dv.data.size());
			dv.encode(value);
			table.put(key.Bytes, key.ReadIndex, key.size(), value.Bytes, 0, value.WriteIndex);
			return KV.create(version, true);
		}

		@Override
		public void setInUse(int localId, String global) {
			// rocksdb 独占由它自己打开的时候保证。
		}

		@Override
		public int clearInUse(int localId, String global) {
			// rocksdb 独占由它自己打开的时候保证。
			return 0;
		}
	}
*/
}
