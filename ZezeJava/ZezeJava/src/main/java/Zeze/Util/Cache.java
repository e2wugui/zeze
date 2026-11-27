package Zeze.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

/**
 * 本地持久化只读缓存（内存 LRU + RocksDB + 约 30 天定期退役）。
 * 查询顺序：LRU → RocksDB → loader；loader 返回 null 时放 5 分钟 NullCache 占位防穿透。
 * 同 id 并发 get 不互斥，由 getOrAdd 保证 LRU 中仅保留一个共享实例。
 */
public class Cache {
	private static final Logger logger = LogManager.getLogger(Cache.class);
	private final String name;
	private final Function<String, @Nullable CacheObject> loader;
	private final BiFunction<String, ByteBuffer, @Nullable CacheObject> decoder;
	private @Nullable RocksDB db;
	private volatile @Nullable ConcurrentLruLike<String, CacheObject> lru;
	private final ScheduledFuture<?> cleanTimer;
	// 保护 RocksDB 句柄与当日清单状态；锁外执行 loader/encoder，锁内检查并使用原生句柄。
	private final ReentrantLock resourceLock = new ReentrantLock();
	// tryRemove 在锁外读取，保持 volatile。
	private volatile long todayDays;
	// 仅在 resourceLock 内访问。
	private @Nullable FileOutputStream todayFile;
	// 在 close() 的 resourceLock 内置位，与 appendToday 串行化以拦住关后写入。
	private volatile boolean closed;

	/**
	 * 创建LocalCache
	 *
	 * @param name        Cache名字，直接作为目录名字，需要注意有些字符可能不能用。
	 * @param lruCapacity 解码后的对象lru容量。
	 * @param loader      根据id装载对象实例的。
	 * @param decoder     根据id和数据创建出对象实例。
	 */
	public Cache(String name, int lruCapacity, Function<String, @Nullable CacheObject> loader,
				 BiFunction<String, ByteBuffer, @Nullable CacheObject> decoder) throws RocksDBException {
		this.name = name;
		this.loader = loader;
		this.decoder = decoder;

		//noinspection ResultOfMethodCallIgnored
		new File(name).mkdirs();
		var openedDb = RocksDB.open(name);
		db = openedDb;
		try {
			lru = new ConcurrentLruLike<>(name, lruCapacity);
			// 每天6:30尝试删除旧的项，保存周期句柄供close取消。
			cleanTimer = TaskSpec.ofAction(this::tryRemove).scheduleAtPeriodNow(6, 30, 24 * 60 * 60 * 1000);
		} catch (Throwable failure) {
			var lru = this.lru;
			if (lru != null) {
				try {
					lru.close();
				} catch (Throwable cleanupFailure) {
					failure.addSuppressed(cleanupFailure);
				}
			}
			try {
				openedDb.close();
			} catch (Throwable cleanupFailure) {
				failure.addSuppressed(cleanupFailure);
			}
			throw failure;
		}
	}

	public void close() throws IOException {
		cleanTimer.cancel(false);
		resourceLock.lock();
		try {
			closed = true; // 先置终态再关流，拦住并发 appendToday
			if (todayFile != null) {
				try {
					todayFile.close();
				} catch (IOException e) {
					// 关流失败不中断后续 db/lru 释放
					logger.error("Cache.close todayFile exception: {}", name, e);
				}
				todayFile = null;
			}
			var db = this.db;
			if (db == null)
				return;
			this.db = null;
			db.close();
		} finally {
			resourceLock.unlock();
		}

		// 级联取消 LRU 内建的后台周期任务
		var lru = this.lru;
		this.lru = null;
		if (lru != null)
			lru.close();
	}

	public @Nullable CacheObject get(String id) throws RocksDBException, IOException {
		if (id.isEmpty())
			throw new IllegalArgumentException();

		var lru = this.lru;
		if (closed || lru == null)
			throw new IllegalStateException("cache is closed: " + name);

		var value = lru.get(id);
		if (value != null) {
			if (!CacheObject.isNull(value))
				return value;

			var nullCache = (CacheObject.NullCache)value;
			if (System.currentTimeMillis() - nullCache.CreateTime < 5 * 60 * 1000)
				return null; // 5 分钟负缓存不落 RocksDB

			// 只移除本次观察到的占位，避免误删并发新装载的对象
			lru.remove(id, nullCache);
		}

		var key = ByteBuffer.Allocate(128);
		key.WriteString(id);
		byte[] bytes;
		resourceLock.lock();
		try {
			bytes = getOpenDatabase().get(RocksDatabase.getDefaultReadOptions(), key.Bytes, 0, key.WriteIndex);
		} finally {
			resourceLock.unlock();
		}
		if (bytes != null) {
			var bb = ByteBuffer.Wrap(bytes);
			bb.ReadString(); // 跳过 cacheId
			// decoder 返回 null 时用 NullCache 哨兵占位（getOrAdd 要求非空）
			var decoded = lru.getOrAdd(id, () -> {
				var v = decoder.apply(id, bb);
				return v != null ? v : new CacheObject.NullCache();
			});
			return CacheObject.isNull(decoded) ? null : decoded; // 哨兵对象不对外返回
		}

		value = loader.apply(id);
		if (value != null)
			dbSave(value);
		else
			value = new CacheObject.NullCache();

		var tmpLambda = value;
		var loaded = lru.getOrAdd(id, () -> tmpLambda);
		return CacheObject.isNull(loaded) ? null : loaded; // 哨兵对象不对外返回
	}

	private void dbSave(CacheObject value) throws RocksDBException, IOException {
		var id = value.cacheId();
		if (id.isEmpty())
			throw new IllegalArgumentException();

		var bb = ByteBuffer.Allocate();
		bb.WriteString(value.cacheId());
		value.encode(bb);

		var key = ByteBuffer.Allocate(128);
		key.WriteString(id);
		resourceLock.lock();
		try {
			var database = getOpenDatabase();
			// 先登记退役：写清单失败时拒绝发布，否则后续读会命中数据库并跳过补登记。
			appendToday(id);
			database.put(RocksDatabase.getDefaultWriteOptions(), key.Bytes, 0, key.WriteIndex,
					bb.Bytes, 0, bb.WriteIndex);
		} finally {
			resourceLock.unlock();
		}
	}

	private RocksDB getOpenDatabase() {
		assert resourceLock.isHeldByCurrentThread();
		var db = this.db;
		if (db == null)
			throw new IllegalStateException("cache is closed: " + name);
		return db;
	}

	// 当天清单唯一写入口：跨天滚动开关流与写入同处一个临界区。
	private void appendToday(String id) throws IOException {
		var nowDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
		resourceLock.lock();
		try {
			if (closed)
				throw new IllegalStateException("cache is closed: " + name);
			if (todayDays != nowDays) {
				// 追加模式打开当天清单，避免同日重启截断已登记条目。
				var oldFile = todayFile;
				todayFile = new FileOutputStream(Paths.get(name, "days_" + nowDays + ".b64").toFile(), true);
				todayDays = nowDays;
				if (oldFile != null)
					oldFile.close();
			}
			// 新文件显式标记编码版本；旧 days_N 清单继续按原格式读取。
			String encoded = Base64.getEncoder().encodeToString(id.getBytes(StandardCharsets.UTF_8));
			//noinspection DataFlowIssue
			todayFile.write((encoded + "\n").getBytes(StandardCharsets.US_ASCII));
		} finally {
			resourceLock.unlock();
		}
	}

	private void tryRemove() throws IOException, RocksDBException {
		var lru = this.lru;
		if (closed || lru == null)
			return;
		var prefix = "days_";
		var nowDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
		var files = new File(name).listFiles();
		if (files != null) {
			for (var file : files) {
				if (file.getName().startsWith(prefix)) {
					// 跳过畸形文件名，避免单文件异常阻断后续合法清单退役
					long days;
					try {
						String suffix = file.getName().substring(prefix.length());
						if (suffix.endsWith(".b64"))
							suffix = suffix.substring(0, suffix.length() - 4);
						days = Long.parseLong(suffix);
					} catch (NumberFormatException e) {
						logger.warn("Cache {}: skip malformed manifest file '{}'", name, file.getName());
						continue;
					}
					// 超过 30 天且非当天清单（todayDays 锁外 volatile 读，nowDays - days > 30 已排除近期文件）
					if (nowDays - days > 30 && days != todayDays) {
						ArrayList<String> skipped;
						try {
							skipped = tryRemove(lru, file);
						} catch (IllegalStateException e) {
							return; // close 竞态：中止本轮清理，文件保留待下次重试
						} catch (IOException e) {
							// 跳过损坏清单，避免阻断后续清单退役
							logger.error("Cache {}: skip corrupted manifest file '{}'", name, file.getName(), e);
							continue;
						}
						// 逐行处理成功后才删除清单文件；仍在 LRU 使用中的 id 转登记到当天清单延后退役
						if (!skipped.isEmpty()) {
							for (var id : skipped)
								appendToday(id);
						}
						//noinspection ResultOfMethodCallIgnored
						file.delete();
					}
				}
			}
		}
	}

	private ArrayList<String> tryRemove(ConcurrentLruLike<String, CacheObject> lru,
														File file) throws IOException, RocksDBException {
		var skipped = new ArrayList<String>();
		boolean encoded = file.getName().endsWith(".b64");
		try (var r = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
			for (var id = r.readLine(); id != null; id = r.readLine()) {
				if (encoded) {
					try {
						id = new String(Base64.getDecoder().decode(id), StandardCharsets.UTF_8);
					} catch (IllegalArgumentException e) {
						throw new IOException("invalid encoded cache manifest: " + file, e);
					}
				}
				if (lru.get(id, false) != null) {
					skipped.add(id); // 仍在使用中（不刷新 LRU 热度），延后到当天清单
					continue;
				}
				var key = ByteBuffer.Allocate(9);
				key.WriteString(id);
				if (!deleteUnderLock(key))
					// close 与清理交叠时中止本轮，保留清单文件待下次重试
					throw new IllegalStateException("Cache closed during clean: " + name);
			}
		}
		return skipped;
	}

	/** 在 resourceLock 内重取 db 句柄执行删除；已关闭时返回 false。 */
	private boolean deleteUnderLock(ByteBuffer key) throws RocksDBException {
		resourceLock.lock();
		try {
			var db = this.db;
			if (db == null)
				return false;
			db.delete(RocksDatabase.getDefaultWriteOptions(), key.Bytes, 0, key.WriteIndex);
			return true;
		} finally {
			resourceLock.unlock();
		}
	}
}
