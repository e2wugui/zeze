package Zeze.Transaction;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.KV;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;
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
		private final Jedis jedis;
		private final redis.clients.jedis.Transaction jedisTrans;

		public RedisTransaction() {
			jedis = pool.getResource();
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
			jedis.close();
		}

		@Override
		public void rollback() {
			jedisTrans.discard();
			jedisTrans.close();
			jedis.close();
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
		private static final byte[] keyDataVersion = "_ZezeDataWithVersion_".getBytes(StandardCharsets.UTF_8);
		private static final byte[] keyInUse = "_ZezeInstances_".getBytes(StandardCharsets.UTF_8);

		public OperatesRedis() {
		}

		public static class InUse implements Serializable {
			public final HashSet<Integer> instances = new HashSet<>();
			public String global;

			@Override
			public void encode(@NotNull ByteBuffer bb) {
				bb.WriteInt(instances.size());
				for (var ins : instances)
					bb.WriteInt(ins);
				bb.WriteString(global);
			}

			@Override
			public void decode(@NotNull IByteBuffer bb) {
				for (var count = bb.ReadInt(); count > 0; --count)
					instances.add(bb.ReadInt());
				global = bb.ReadString();
			}

			public static InUse decode(byte[] bytes) {
				var inUse = new InUse();
				if (null != bytes)
					inUse.decode(ByteBuffer.Wrap(bytes));
				return inUse;
			}

			public ByteBuffer encode() {
				var bb = ByteBuffer.Allocate();
				encode(bb);
				return bb;
			}
		}

		@Override
		public void setInUse(int localId, String global) {
			while (true) {
				if (tryLock()) {
					try (var jedis = pool.getResource()) {
						var inUse = InUse.decode(jedis.hget(keyInUse, ByteBuffer.Empty));
						if (inUse.instances.contains(localId))
							throw new IllegalStateException("Instance Exist." + localId + " " + global);
						inUse.instances.add(localId);

						if (inUse.global != null && !inUse.global.equals(global))
							throw new IllegalStateException("Global Not Equals" + localId + " " + global);

						inUse.global = global;
						if (inUse.instances.size() == 1) {
							jedis.hset(keyInUse, ByteBuffer.Empty, copyIf(inUse.encode()));
							return; // success;
						}

						if (global.isEmpty())
							throw new IllegalStateException("Instance Greater Than One But No Global" + localId + " " + global);
						jedis.hset(keyInUse, ByteBuffer.Empty, copyIf(inUse.encode()));
						return; // success
					} finally {
						unlock();
					}
				}
				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		@Override
		public int clearInUse(int localId, String global) {
			while (true) {
				if (tryLock()) {
					try (var jedis = pool.getResource()) {
						var inUse = InUse.decode(jedis.hget(keyInUse, ByteBuffer.Empty));
						var result = 1;
						if (inUse.global != null) {
							// has data
							result = inUse.instances.remove(localId) ? 0 : 2;
							if (inUse.instances.isEmpty())
								jedis.hdel(keyInUse, ByteBuffer.Empty);
							else
								jedis.hset(keyInUse, ByteBuffer.Empty, copyIf(inUse.encode())); // save
						}
						// 不抛出异常，仅仅返回;
						return result;
					} finally {
						unlock();
					}
				}
				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		private final static String lockKey = "_Zeze_Redis_Global_Lock_";

		@Override
		public boolean tryLock() {
			try (var jedis = pool.getResource()) {
				return 1 == jedis.setnx( lockKey, "1");
			}
		}

		@Override
		public void unlock() {
			try (var jedis = pool.getResource()) {
				jedis.del(lockKey);
			}
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			try (var jedis = pool.getResource()) {
				var exist = getDataWithVersion(jedis, copyIf(key));
				if (null != exist && exist.version != version)
					return KV.create(version, false);
				var dv = new DataWithVersion();
				dv.data = data;
				dv.version = version;
				var dvBb = ByteBuffer.Allocate();
				dv.encode(dvBb);
				jedis.hset(keyDataVersion, copyIf(key), copyIf(dvBb));
				return KV.create(version, true);
			}
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			try (var jedis = pool.getResource()) {
				return getDataWithVersion(jedis, copyIf(key));
			}
		}

		private DataWithVersion getDataWithVersion(Jedis jedis, byte[] field) {
			var value = jedis.hget(keyDataVersion, field);
			if (null == value)
				return null; // no data version
			return DataWithVersion.decode(value);
		}
	}
}
