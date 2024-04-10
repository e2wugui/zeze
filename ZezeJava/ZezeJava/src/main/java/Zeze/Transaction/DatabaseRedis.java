package Zeze.Transaction;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.IntHashSet;
import Zeze.Util.KV;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.ScanParams;

// 需要redis支持以下命令(Redis 2.8+, Kvrocks 2.02+, Pika 3.5.2+)
// get, set, setnx, del
// hget, hset, hdel, hscan
// multi, exec, discard
public class DatabaseRedis extends Database {
	private static final Logger logger = LogManager.getLogger(DatabaseRedis.class);

	private final @NotNull JedisPool pool;

	public DatabaseRedis(@NotNull Application zeze, @NotNull Config.DatabaseConf conf) {
		super(zeze, conf);
		try {
			logger.info("open: {}", getDatabaseUrl());
			var config = new JedisPoolConfig();
			config.setMaxTotal(1024); // 并发连接上限,默认8
			config.setMaxIdle(8); // 空闲连接上限,默认8
			config.setMaxWait(Duration.ofMillis(10_000)); // 等待可用连接的时长上限,超时会抛JedisConnectionException,默认-1表示没有超时
			pool = new JedisPool(config, new URI(getDatabaseUrl()));
		} catch (URISyntaxException e) {
			Task.forceThrow(e);
			throw new AssertionError(); // never run here
		}
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesRedis(pool));
	}

	@Override
	public void close() {
		lock();
		try {
			logger.info("close: {}", getDatabaseUrl());
			super.close();
			pool.close();
		} finally {
			unlock();
		}
	}

	@Override
	public @NotNull Table openTable(@NotNull String name, int id) {
		return new RedisTable(name);
	}

	@Override
	public @NotNull Transaction beginTransaction() {
		return new RedisTransaction(pool);
	}

	public static final class RedisTransaction implements Transaction {
		private final @NotNull Jedis jedis;
		private final @NotNull redis.clients.jedis.Transaction jedisTrans;

		public RedisTransaction(@NotNull JedisPool pool) {
			jedis = pool.getResource();
			jedisTrans = new redis.clients.jedis.Transaction(jedis);
		}

		public void replace(byte @NotNull [] table, byte @NotNull [] key, byte @NotNull [] value) {
			jedisTrans.hset(table, key, value);
		}

		public void remove(byte @NotNull [] table, byte @NotNull [] key) {
			jedisTrans.hdel(table, key);
		}

		@Override
		public void commit() {
			jedisTrans.exec();
		}

		@Override
		public void rollback() {
			jedisTrans.discard();
		}

		@Override
		public void close() {
			jedisTrans.close();
			jedis.close();
		}
	}

	public final class RedisTable extends AbstractKVTable {
		private final byte @NotNull [] keyOfSet;

		public RedisTable(@NotNull String name) {
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
				var value = jedis.hget(keyOfSet, key.CopyIf());
				return value != null ? ByteBuffer.Wrap(value) : null;
			}
		}

		@Override
		public void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			var redisT = (RedisTransaction)t;
			redisT.replace(keyOfSet, key.CopyIf(), value.CopyIf());
		}

		@Override
		public void remove(@NotNull Transaction t, @NotNull ByteBuffer key) {
			var redisT = (RedisTransaction)t;
			redisT.remove(keyOfSet, key.CopyIf());
		}

		@Override
		public long walk(@NotNull TableWalkHandleRaw callback) {
			var count = 0L;
			var cursor = ScanParams.SCAN_POINTER_START_BINARY;
			try (var jedis = pool.getResource()) {
				while (true) {
					var result = jedis.hscan(keyOfSet, cursor);
					for (var entry : result.getResult()) {
						if (!callback.handle(entry.getKey(), entry.getValue()))
							return count;
						count++; // callback 是可中断的，所以这里不用+=size();
					}
					cursor = result.getCursorAsBytes();
					if (Arrays.equals(cursor, ScanParams.SCAN_POINTER_START_BINARY))
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
		public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
										 @NotNull TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											@NotNull TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											 @NotNull TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												@NotNull TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}
	}

	public static final class InUse implements Serializable {
		public final IntHashSet instances = new IntHashSet();
		public String global;

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteUInt(instances.size());
			for (var it = instances.iterator(); it.moveToNext(); )
				bb.WriteInt(it.value());
			bb.WriteString(global);
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			for (var count = bb.ReadUInt(); count > 0; count--)
				instances.add(bb.ReadInt());
			global = bb.ReadString();
		}

		public static @NotNull InUse decode(byte @Nullable [] bytes) {
			var inUse = new InUse();
			if (null != bytes)
				inUse.decode(ByteBuffer.Wrap(bytes));
			return inUse;
		}

		public @NotNull ByteBuffer encode() {
			var bb = ByteBuffer.Allocate();
			encode(bb);
			return bb;
		}
	}

	public static final class OperatesRedis implements Operates {
		private static final byte @NotNull [] keyDataVersion = "_ZezeDataWithVersion_".getBytes(StandardCharsets.UTF_8);
		private static final byte @NotNull [] keyInUse = "_ZezeInstances_".getBytes(StandardCharsets.UTF_8);
		private static final String lockKey = "_Zeze_Redis_Global_Lock_";

		private final @NotNull JedisPool pool;
		private final ReentrantLockHelper lockHelper = new ReentrantLockHelper();

		public OperatesRedis(@NotNull JedisPool pool) {
			this.pool = pool;
		}

		@Override
		public void setInUse(int localId, @NotNull String global) {
			while (true) {
				if (tryLock()) {
					try (var jedis = pool.getResource()) {
						var inUse = InUse.decode(jedis.get(keyInUse));
						if (inUse.instances.contains(localId))
							throw new IllegalStateException("Instance Exist. " + localId + ", " + global);
						inUse.instances.add(localId);

						if (inUse.global != null && !inUse.global.equals(global))
							throw new IllegalStateException("Global Not Equals. " + localId + ", " + global);

						inUse.global = global;
						if (inUse.instances.size() > 1 && global.isEmpty()) {
							throw new IllegalStateException("Instance Greater Than One But No Global. "
									+ localId + ", " + global);
						}
						jedis.set(keyInUse, inUse.encode().CopyIf());
						return;
					} finally {
						unlock();
					}
				}
				try {
					//noinspection BusyWait
					Thread.sleep(150);
				} catch (InterruptedException e) {
					Task.forceThrow(e);
					// never run here
				}
			}
		}

		@Override
		public int clearInUse(int localId, @NotNull String global) {
			while (true) {
				if (tryLock()) {
					try (var jedis = pool.getResource()) {
						var inUse = InUse.decode(jedis.get(keyInUse));
						var result = 1;
						if (inUse.global != null) {
							// has data
							result = inUse.instances.remove(localId) ? 0 : 2;
							if (inUse.instances.isEmpty())
								jedis.del(keyInUse);
							else
								jedis.set(keyInUse, inUse.encode().CopyIf()); // save
						}
						// 不抛出异常，仅仅返回;
						return result;
					} finally {
						unlock();
					}
				}
				try {
					//noinspection BusyWait
					Thread.sleep(150);
				} catch (InterruptedException e) {
					Task.forceThrow(e);
					// never run here
				}
			}
		}

		@Override
		public boolean tryLock() {
			if (lockHelper.tryLock())
				return true;
			try (var jedis = pool.getResource()) {
				var success = 1 == jedis.setnx(lockKey, "1");
				if (success)
					lockHelper.lockSuccess();
				return success;
			}
		}

		@Override
		public void unlock() {
			if (lockHelper.tryUnlock()) {
				try (var jedis = pool.getResource()) {
					var success = 1 == jedis.del(lockKey);
					if (success)
						lockHelper.unlockSuccess();
				}
			}
		}

		@Override
		public @NotNull KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key, @NotNull ByteBuffer data,
																  long version) {
			try (var jedis = pool.getResource()) {
				var exist = getDataWithVersion(jedis, key.CopyIf());
				if (null != exist && exist.version != version)
					return KV.create(version, false);
				var dv = new DataWithVersion();
				dv.data = data;
				dv.version = version;
				var dvBb = ByteBuffer.Allocate();
				dv.encode(dvBb);
				jedis.hset(keyDataVersion, key.CopyIf(), dvBb.CopyIf());
				return KV.create(version, true);
			}
		}

		@Override
		public @Nullable DataWithVersion getDataWithVersion(@NotNull ByteBuffer key) {
			try (var jedis = pool.getResource()) {
				return getDataWithVersion(jedis, key.CopyIf());
			}
		}

		private static @Nullable DataWithVersion getDataWithVersion(@NotNull Jedis jedis, byte @NotNull [] field) {
			var value = jedis.hget(keyDataVersion, field);
			if (null == value)
				return null; // no data version
			return DataWithVersion.decode(value);
		}
	}
}
