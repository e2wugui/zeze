package Zeze.Transaction;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;

public class DatabaseRedis extends Database {
	private final JedisPool pool;

	public DatabaseRedis(@NotNull Application zeze, @NotNull Config.DatabaseConf conf) {
		super(zeze, conf);
		try {
			var url = new URI(getDatabaseUrl());
			pool = new JedisPool(new JedisPoolConfig(), url.getHost(), url.getPort());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesRedis());
	}

	@Override
	public @NotNull Table openTable(@NotNull String name) {
		return new RedisTable(name);
	}

	@Override
	public @NotNull Transaction beginTransaction() {
		return new RedisTransaction();
	}

	public final class RedisTransaction implements Zeze.Transaction.Database.Transaction {
		private final redis.clients.jedis.Transaction jedisTrans;

		public RedisTransaction() {
			var jedis = pool.getResource();
			jedisTrans = jedis.multi();
		}

		public void replace(byte[] table, byte[] key, byte[] value) {
			jedisTrans.hset(table, key, value);
		}

		public void remove(byte[] table, byte[] key) {
			jedisTrans.hdel(table, key);
		}

		@Override
		public void commit() {
			jedisTrans.exec();
			jedisTrans.close();
		}

		@Override
		public void rollback() {
			jedisTrans.discard();
			jedisTrans.close();
		}

		@Override
		public void close() throws Exception {
			jedisTrans.close();
		}
	}

	public final class RedisTable extends Zeze.Transaction.Database.AbstractKVTable {
		private final byte[] keyOfSet;

		public RedisTable(String name) {
			keyOfSet = name.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public boolean isNew() {
			return false;
		}

		@Override
		public @NotNull Database getDatabase() {
			return DatabaseRedis.this;
		}

		@Override
		public void close() {
		}

		@Override
		public @Nullable ByteBuffer find(@NotNull ByteBuffer key) {
			try (var jedis = pool.getResource()) {
				var value = jedis.hget(keyOfSet, copyIf(key));
				if (null == value)
					return null;
				return ByteBuffer.Wrap(value);
			}
		}

		@Override
		public void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			var redisT = (RedisTransaction)t;
			redisT.replace(keyOfSet, copyIf(key), copyIf(value));
		}

		@Override
		public void remove(@NotNull Transaction t, @NotNull ByteBuffer key) {
			var redisT = (RedisTransaction)t;
			redisT.remove(keyOfSet, copyIf(key));
		}

		@Override
		public long walk(@NotNull TableWalkHandleRaw callback) {
			var count = 0L;
			try (var jedis = pool.getResource()) {
				byte[] cursor = SCAN_POINTER_START_BINARY;
				while (true) {
					var result = jedis.hscan(keyOfSet, cursor);
					for (var entry : result.getResult()) {
						if (!callback.handle(entry.getKey(), entry.getValue()))
							return count;
						count ++; // callback 是可中断的，所以这里不用+=size();
					}
					cursor = result.getCursorAsBytes();
					if (Arrays.equals(cursor, SCAN_POINTER_START_BINARY))
						return count;
				}
			}
		}

		@Override
		public long walkKey(@NotNull TableWalkKeyRaw callback) {
			return walk((key, value) -> callback.handle(key));
		}

		@Override
		public long walkDesc(@NotNull TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long walkKeyDesc(@NotNull TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}
	}

	public final class OperatesRedis implements Operates {

		@Override
		public void setInUse(int localId, String global) {

		}

		@Override
		public int clearInUse(int localId, String global) {
			return 0;
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			return null;
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			return null;
		}
	}
}
