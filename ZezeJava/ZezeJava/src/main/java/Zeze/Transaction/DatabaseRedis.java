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
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SetParams;

/**
 * Redis 数据库后端：以 hash 承载 KV 表，MULTI/EXEC 批量事务落库，
 * 并提供带租期的全局启动锁与 InUse 登记。
 */
// 需要redis支持以下命令(Redis 2.8+, Kvrocks 2.02+, Pika 3.5.2+)
// get, set(含 set key value nx ex), del
// hget, hset, hdel, hscan
// multi, exec, discard
public class DatabaseRedis extends Database {
	private static final @NotNull Logger logger = LogManager.getLogger(DatabaseRedis.class);

	private final @NotNull JedisPool pool;

	public DatabaseRedis(@Nullable Application zeze, @NotNull Config.DatabaseConf conf) {
		super(zeze, conf);
		try {
			logger.info("open: {}", getDatabaseUrl());
			var config = new JedisPoolConfig();
			config.setMaxTotal(1024); // 并发连接上限,默认8
			config.setMaxIdle(8); // 空闲连接上限,默认8
			config.setMaxWait(Duration.ofMillis(10_000)); // 等待可用连接的时长上限,超时会抛JedisConnectionException,默认-1表示没有超时
			pool = new JedisPool(config, new URI(getDatabaseUrl()));
		} catch (URISyntaxException e) {
			throw Task.forceThrow(e);
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
		private int n;

		public RedisTransaction(@NotNull JedisPool pool) {
			jedis = pool.getResource();
			jedisTrans = new redis.clients.jedis.Transaction(jedis);
		}

		public void replace(byte @NotNull [] table, byte @NotNull [] key, byte @NotNull [] value) {
			jedisTrans.hset(table, key, value);
			n++;
		}

		public void remove(byte @NotNull [] table, byte @NotNull [] key) {
			jedisTrans.hdel(table, key);
			n++;
		}

		@Override
		public void commit() {
			if (n >= 10000)
				logger.warn("RedisTransaction commit too many records: {}", n);
			// MULTI/EXEC 中命令级错误（WRONGTYPE、内存超限等）不打断 exec，
			// 而是作为异常元素出现在 exec 的返回列表里（已核 Jedis 5.2.0 Transaction.exec 源码）。
			// 丢弃返回值会让部分写失败被静默吞掉：checkpoint 误判 flush 成功并清除脏标记，已提交数据无声丢失。
			// 抛出让 checkpoint 走失败重试（hset/hdel 幂等，重放安全）。
			var results = jedisTrans.exec();
			if (results == null) {
				// exec 返回 null（已核 Jedis 5.2.0 Transaction.exec 源码）：RESP2 下服务端因入队期错误
				// （如 maxmemory 时对 hset/hdel 回错误而非 +QUEUED）中止整个事务，EXEC 回 nil——
				// 本批命令一条都没有执行。当作成功会让 checkpoint 清除脏标记，整批已提交数据永久丢失。
				// 本批为空（n==0）时无数据可丢，无需重试。
				if (n > 0)
					throw new JedisDataException("redis transaction aborted (queue-time error), n=" + n);
				return;
			}
			for (var r : results)
				if (r instanceof JedisDataException e)
					throw e;
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
		private final @NotNull String name;

		public RedisTable(@NotNull String name) {
			this.name = name;
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
			checkKvKeyLength(name, key);
			try (var jedis = pool.getResource()) {
				var value = jedis.hget(keyOfSet, key.CopyIf());
				return value != null ? ByteBuffer.Wrap(value) : null;
			}
		}

		@Override
		public void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			checkKvKeyLength(name, key);
			var redisT = (RedisTransaction)t;
			redisT.replace(keyOfSet, key.CopyIf(), value.CopyIf());
		}

		@Override
		public void remove(@NotNull Transaction t, @NotNull ByteBuffer key) {
			checkKvKeyLength(name, key);
			var redisT = (RedisTransaction)t;
			redisT.remove(keyOfSet, key.CopyIf());
		}

		@Override
		public long walk(@NotNull TableWalkHandleRaw callback) throws Exception {
			var count = 0L;
			var cursor = ScanParams.SCAN_POINTER_START_BINARY;
			try (var jedis = pool.getResource()) {
				while (true) {
					var result = jedis.hscan(keyOfSet, cursor);
					for (var entry : result.getResult()) {
						count++; // 被回调且返回 false 的中断项也计入（契约见 AbstractKVTable.walk），所以不用+=size()
						if (!callback.handle(entry.getKey(), entry.getValue()))
							return count;
					}
					cursor = result.getCursorAsBytes();
					if (Arrays.equals(cursor, ScanParams.SCAN_POINTER_START_BINARY))
						return count;
				}
			}
		}

		@Override
		public long walkKey(@NotNull TableWalkKeyRaw callback) throws Exception {
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
			if (bytes != null)
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
		// 全局锁的租期：必须显著大于持锁窗口的最坏耗时（atomicOpenDatabase 全程，含 renameTable
		// 与大表 tryAlter，分钟级），避免正常启动期间锁被误过期导致两实例并发进入 schemasCompatible；
		// 同时给崩溃（kill -9/OOM/断电）后残留的锁一个自动恢复上限：调用方 atomicOpenDatabase
		// 每秒轮询 tryLock，锁到期后自动获取，无需再人工 redis-cli DEL。
		private static final int LOCK_LEASE_SECONDS = 600;

		private final @NotNull JedisPool pool;
		private final ReentrantLockHelper lockHelper = new ReentrantLockHelper();

		public OperatesRedis(@NotNull JedisPool pool) {
			this.pool = pool;
		}

		@Override
		public void setInUse(int localId, @NotNull String global) {
			for (int i = 0; i < 64; ++i) {
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
					Thread.sleep(150);
				} catch (InterruptedException e) {
					throw Task.forceThrow(e);
				}
			}
			throw new IllegalStateException("setInUse tryLock fail.");
		}

		// 对齐 setInUse 的 64 次上限：不得无界自旋
		// ——停机路径无守卫，阻塞使 stop() 持 Application 锁无界；Daemon 形态放大：
		// achillesHeelDaemon 先停→Monitor 判死→SIGTERM 对 hook 内自旋无效→重启同 serverId
		// 短暂双开+旧进程 RMW 可能删新注册。放弃=不执行 RMW，残留与 kill -9 崩溃残留
		// 同构（锁租期 600s 后自愈；重启路径有 -DZeze.Database.ClearInUse 属性通道，
		// 见 Application.start 与 Daemon.main）。已知取舍：崩溃后 600s 内的
		// Daemon 自动重启，clearInUse 将 64 次（约 9.6s）耗尽失败→启动短命退出→
		// Daemon 按 MinAliveTime 判定终结守护，需人工重启——响亮失败优于无界停摆。
		@Override
		public int clearInUse(int localId, @NotNull String global) {
			for (int i = 0; i < 64; ++i) {
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
					Thread.sleep(150);
				} catch (InterruptedException e) {
					throw Task.forceThrow(e);
				}
			}
			throw new IllegalStateException("clearInUse tryLock fail. redis=" + global);
		}

		@Override
		public boolean tryLock() {
			if (lockHelper.tryLock())
				return true;
			try (var jedis = pool.getResource()) {
				// set nx ex：获取锁的同时设置租期。持锁进程在持锁窗口内崩溃后，
				// 锁最多残留 LOCK_LEASE_SECONDS，后续实例不会再无限挂死。
				// 注：不做续期与持有者校验（unlock 直接 del），慢启动超过租期时存在
				// 锁误过期与误删他人锁的理论窗口。
				var success = "OK".equals(jedis.set(lockKey, "1",
						SetParams.setParams().nx().ex(LOCK_LEASE_SECONDS)));
				if (success)
					lockHelper.lockSuccess();
				return success;
			}
		}

		@Override
		public void unlock() {
			if (lockHelper.tryUnlock()) {
				try (var jedis = pool.getResource()) {
					// 锁可能已因租期到期被 redis 清除（del 返回 0），本地计数同样需要清理。
					jedis.del(lockKey);
				} finally {
					lockHelper.unlockSuccess();
				}
			}
		}

		// 全家族唯独Redis的读-判-写非服务端原子（其余走存储过程/事务）：热更
		// （__upgrade_schemas__）不进启动锁，并发save会双双通过版本校验互相覆盖。
		// Lua单RT原子化：版本是DataWithVersion编码的末8字节，按字节串比较，
		// 避开Redis Lua 5.1的64位数字精度问题。
		private static final String SAVE_CAS_LUA = """
				local cur = redis.call('HGET', KEYS[1], ARGV[1])
				if cur and string.sub(cur, -8) ~= ARGV[3] then
					return 0
				end
				redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
				return 1
				""";

		@Override
		public @NotNull KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key, @NotNull ByteBuffer data,
																  long version) {
			try (var jedis = pool.getResource()) {
				var dv = new DataWithVersion();
				dv.data = data;
				// 版本必须递增（对齐 RocksDb/Mongo/Dynamo 实现）：schemasCompatible 的重读重试环
				// 依赖“读到陈旧数据时 exist.version != version 返回 false”这一防线；
				// 原样写回会让存储的 version 永远停在首次值，版本冲突检测名存实亡。
				dv.version = version + 1;
				var dvBb = ByteBuffer.Allocate();
				dv.encode(dvBb);
				var expectedVersion = ByteBuffer.Allocate(Long.BYTES);
				expectedVersion.WriteLong(version);
				var written = (Long)jedis.eval(SAVE_CAS_LUA.getBytes(StandardCharsets.UTF_8),
						java.util.List.of(keyDataVersion),
						java.util.List.of(key.CopyIf(), dvBb.CopyIf(), expectedVersion.CopyIf()));
				return written == 1 ? KV.create(dv.version, true) : KV.create(version, false);
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
			if (value == null)
				return null; // no data version
			return DataWithVersion.decode(value);
		}
	}
}
