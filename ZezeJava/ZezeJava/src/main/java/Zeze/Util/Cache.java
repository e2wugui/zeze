package Zeze.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class Cache extends ReentrantLock {
	private final @NotNull String name;
	private final @NotNull Function<String, CacheObject> loader;
	private final @NotNull BiFunction<String, ByteBuffer, CacheObject> decoder;
	private RocksDB db;
	private ConcurrentLruLike<String, CacheObject> lru;
	private final ScheduledFuture<?> cleanTimer;
	private volatile long todayDays;
	private volatile FileOutputStream todayFile;

	/**
	 * 创建LocalCache
	 *
	 * @param name        Cache名字，直接作为目录名字，需要注意有些字符可能不能用。
	 * @param lruCapacity 解码后的对象lru容量。
	 * @param loader      根据id装载对象实例的。
	 * @param decoder     根据id和数据创建出对象实例。
	 */
	public Cache(@NotNull String name, int lruCapacity, @NotNull Function<String, CacheObject> loader,
				 @NotNull BiFunction<String, ByteBuffer, CacheObject> decoder) throws RocksDBException {
		this.name = name;
		this.loader = loader;
		this.decoder = decoder;

		//noinspection ResultOfMethodCallIgnored
		new File(name).mkdirs();
		db = RocksDB.open(name);
		lru = new ConcurrentLruLike<>(name, lruCapacity);
		// 每天6:30尝试删除旧的项。period>0才是周期调度（scheduleAt(hour,minute)默认只触发一次）；
		// 用scheduleAtNow拿到句柄，close时取消。
		cleanTimer = TaskSpec.ofAction(this::tryRemove).scheduleAtPeriodNow(6, 30, 24 * 60 * 60 * 1000);
	}

	public void close() throws IOException {
		if (cleanTimer != null)
			cleanTimer.cancel(false);
		if (todayFile != null)
			todayFile.close();

		db.close();
		db = null;
		lru = null;
	}

	public @Nullable CacheObject get(@NotNull String id) throws RocksDBException, IOException {
		if (id.isEmpty())
			throw new IllegalArgumentException();

		var db = this.db;
		var lru = this.lru;
		if (db == null || lru == null)
			throw new IllegalStateException("cache is closed: " + name); // 对齐 tryRemove 的快照防御；close 后继续 get 是使用错误

		var value = lru.get(id);
		if (value != null) {
			if (!CacheObject.isNull(value))
				return value;

			var nullCache = (CacheObject.NullCache)value;
			if (System.currentTimeMillis() - nullCache.CreateTime < 5 * 60 * 1000) // 5 minutes
				return null; // null cache 不会写入RocksDb，短时间内就会允许再次尝试。

			// remove and try load，下面的流程会浪费一次RocksDb的查询，先这样了。
			lru.remove(id);
		}

		// 当Lru不命中，并且同时多个线程并发执行到这里，会执行多次decoder/loader操作。
		// 也就是说同一个进程对同一个数据的decoder/loader没有互斥。

		var key = ByteBuffer.Allocate(128);
		key.WriteString(id);
		var bytes = db.get(RocksDatabase.getDefaultReadOptions(), key.Bytes, 0, key.WriteIndex);
		if (bytes != null) {
			// decoder
			var bb = ByteBuffer.Wrap(bytes);
			bb.ReadString(); // skip cacheId.
			// 当出现并发get重复从db读取时，这里的getOrAdd会忽略后面读到的value，返回已经存在的。
			return lru.getOrAdd(id, () -> decoder.apply(id, bb));
		}

		// do user loader to load object.
		value = loader.apply(id);
		if (value != null)
			dbSave(value);
		else
			value = new CacheObject.NullCache();

		// 当出现并发get重复从db读取时，这里的getOrAdd会忽略后面读到的value，返回已经存在的。
		var tmpLambda = value;
		return lru.getOrAdd(id, () -> tmpLambda);
	}

	private void dbSave(@NotNull CacheObject value) throws RocksDBException, IOException {
		var id = value.cacheId();
		if (id.isEmpty())
			throw new IllegalArgumentException();

		var bb = ByteBuffer.Allocate();
		bb.WriteString(value.cacheId());
		value.encode(bb);

		var key = ByteBuffer.Allocate(128);
		key.WriteString(id);
		db.put(RocksDatabase.getDefaultWriteOptions(), key.Bytes, 0, key.WriteIndex, bb.Bytes, 0, bb.WriteIndex);

		today().write((id + "\n").getBytes(StandardCharsets.UTF_8));
	}

	private @NotNull FileOutputStream today() throws IOException {
		var nowDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
		// 第一次执行时，如果nowDays等于0（即days的初始值），会返回null。
		// 这种情况就不处理了。
		if (todayDays != nowDays) {
			lock();
			try {
				if (todayDays != nowDays) {
					// 顺序：(1)先开新流并赋值 todayFile（volatile），(2)再写 todayDays（volatile），(3)最后关旧流。
					// (2)在(1)之后保证快路径读者看到新 todayDays 时必然看到新 todayFile；
					// 原实现先写 todayDays 再换流，跨天窗口内无锁读者会拿到已 close 的旧流。
					var oldFile = todayFile;
					todayFile = new FileOutputStream(Paths.get(name, "days_" + nowDays).toFile());
					todayDays = nowDays;
					if (oldFile != null)
						oldFile.close();
				}
				return todayFile;
			} finally {
				unlock();
			}
		}
		return todayFile;
	}

	private void tryRemove() throws IOException, RocksDBException {
		var db = this.db;
		var lru = this.lru;
		if (db == null || lru == null)
			return; // 已close（cancel与正在执行的任务之间的窗口），不再访问。
		var prefix = "days_";
		var nowDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
		var files = new File(name).listFiles();
		if (files != null) {
			for (var file : files) {
				if (file.getName().startsWith(prefix)) {
					var days = Long.parseLong(file.getName().substring(prefix.length()));
					// a month ago && not today
					if (nowDays - days > 30 && days != todayDays)
						tryRemove(db, lru, file);
				}
			}
		}
	}

	private static void tryRemove(@NotNull RocksDB db, @NotNull ConcurrentLruLike<String, CacheObject> lru,
								  @NotNull File file) throws IOException, RocksDBException {
		try (var r = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
			for (var id = r.readLine(); id != null; id = r.readLine()) {
				if (lru.get(id) != null)
					continue; // 当前使用中的项不删除。
				var key = ByteBuffer.Allocate(9);
				key.WriteString(id);
				db.delete(RocksDatabase.getDefaultWriteOptions(), key.Bytes, 0, key.WriteIndex);
			}
		}
	}
}
