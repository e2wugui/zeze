package Zeze.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

/**
 * # 本地持久化只读缓存（内存Lru + RocksDb + 定期退役）
 *
 * 面向装载后不再修改的参照数据（配置、id映射、资源索引等）：
 * - **查询顺序**：Lru → RocksDb → loader。装载成功即写RocksDb并登记当天days_清单，之后重启也只读RocksDb。
 * - **只读假设**：get返回Lru中的共享实例，无写回API；调用方修改不落库、不同步，淘汰或退役后即丢失。
 * - **刷新**：唯一途径是写入约30天后被每天6:30的清理任务退役（见dbSave/tryRemove），下次get重走loader，即接受最长约30天陈旧。
 * - **并发**：同id的get无互斥，decoder/loader可能重复执行，仅装载开销重复；getOrAdd保证Lru只有一个条目。
 * - **null cache**：loader返回null时放5分钟NullCache占位（不落RocksDb），防止对不存在的id反复穿透。
 */
public class Cache {
	private static final @NotNull Logger logger = LogManager.getLogger(Cache.class);
	private final @NotNull String name;
	private final @NotNull Function<String, CacheObject> loader;
	private final @NotNull BiFunction<String, ByteBuffer, CacheObject> decoder;
	private RocksDB db;
	private ConcurrentLruLike<String, CacheObject> lru;
	private final ScheduledFuture<?> cleanTimer;
	// 保护当天清单状态（todayDays/todayFile）：开新流、写清单、关旧流、close关流都必须同处其临界区，
	// 否则无锁的写者可能拿到刚被关闭的旧流而抛"Stream Closed"，该key当天的清单条目丢失。
	private final ReentrantLock todayLock = new ReentrantLock();
	// tryRemove 的清理筛选在锁外读（见那里的说明），保持 volatile。
	private volatile long todayDays;
	// 只在持有 todayLock 时访问。
	private FileOutputStream todayFile;
	// 终态标志：在 close() 的 todayLock 临界区内置位，appendToday 在同一锁下检查，
	// 密闭地拦住"close后写当日清单"——null流NPE与关后重开新流泄漏两种形态一并消灭。
	private volatile boolean closed;

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
		cleanTimer.cancel(false); // 幂等：已取消时为no-op
		todayLock.lock();
		try {
			closed = true; // 先立终态再关流：appendToday在同一临界区检查，此后不可能再触碰流
			if (todayFile != null) {
				todayFile.close();
				todayFile = null; // 置null后重入不再触碰已关流
			}
		} finally {
			todayLock.unlock();
		}

		var db = this.db;
		if (db == null)
			return; // 已close（幂等重入；并发下后到者退出）
		this.db = null;
		db.close(); // RocksDB.close 自身幂等，并发重复close无害
		// 级联取消LRU构造器内建的两个周期任务（FND7-36）：只置null不清任务的话，
		// 任务仍每200ms/2s永续执行并强引用整个缓存对象图，"关闭"语义不成立。
		var lru = this.lru;
		this.lru = null;
		if (lru != null)
			lru.close();
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

		var db = this.db;
		if (db == null)
			throw new IllegalStateException("cache is closed: " + name); // 入口检查后loader执行期间close可完成（FND8-08），写路径二次确认

		var bb = ByteBuffer.Allocate();
		bb.WriteString(value.cacheId());
		value.encode(bb);

		var key = ByteBuffer.Allocate(128);
		key.WriteString(id);
		db.put(RocksDatabase.getDefaultWriteOptions(), key.Bytes, 0, key.WriteIndex, bb.Bytes, 0, bb.WriteIndex);

		appendToday(id);
	}

	// 当天清单的唯一写入口：取流（含跨天滚动开新流、关旧流）与写流同处一个临界区。
	// 该key当天的清单条目一旦丢失，它的db记录从此再没有退役记录。
	private void appendToday(@NotNull String id) throws IOException {
		var nowDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
		todayLock.lock();
		try {
			if (closed)
				throw new IllegalStateException("cache is closed: " + name); // close的置位同在此临界区：检查密闭，关后不再写流/重开流
			if (todayDays != nowDays) {
				// 第一次执行时如果nowDays等于0（todayDays的初始值），不会走到这里，这种情况不处理了。
				// 追加模式：同日重启（新实例todayDays初始0必进此分支）打开已存在的当天清单，
				// 截断会把前次运行登记的条目清掉——这些key的RocksDb记录从此再没有退役记录（FND4-10）。
				var oldFile = todayFile;
				todayFile = new FileOutputStream(Paths.get(name, "days_" + nowDays).toFile(), true);
				todayDays = nowDays;
				if (oldFile != null)
					oldFile.close();
			}
			todayFile.write((id + "\n").getBytes(StandardCharsets.UTF_8));
		} finally {
			todayLock.unlock();
		}
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
					// 单文件解析包try/catch（FND7-37）：days_前缀+非数字后缀的畸形文件（运维残留、
					// 复制/崩溃半成品）会让parseLong抛NumberFormatException穿透整个循环——
					// 毒文件位于delete之前永不会被删，次日起每天在同一文件上复发，排在它后面的
					// 合法清单从此永远不被退役。跳过畸形文件继续处理其余清单。
					long days;
					try {
						days = Long.parseLong(file.getName().substring(prefix.length()));
					} catch (NumberFormatException e) {
						logger.warn("Cache {}: skip malformed manifest file '{}'", name, file.getName());
						continue;
					}
					// a month ago && not today。todayDays在锁外volatile读：陈旧无害，
					// nowDays-days>30已排除近期文件，days!=todayDays只是对当天清单的额外保险。
					if (nowDays - days > 30 && days != todayDays) {
						var skipped = tryRemove(db, lru, file);
						// 整个文件逐行处理成功后才删除清单文件，闭合其生命周期，免得已退役的清单
						// 每天被重复读取、重复删除同一批key；若半途抛异常，异常直接冒泡到这里之上，
						// 不会执行下面的删除，这批key的清理不会永久丢失。
						// 因"当前使用中"被跳过的id必须转移登记到当天的清单里（下个月再试），
						// 否则删掉文件就永久失去它们的退役记录，RocksDB里的记录再没人清理。
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

	private static @NotNull ArrayList<String> tryRemove(@NotNull RocksDB db, @NotNull ConcurrentLruLike<String, CacheObject> lru,
														@NotNull File file) throws IOException, RocksDBException {
		var skipped = new ArrayList<String>();
		try (var r = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
			for (var id = r.readLine(); id != null; id = r.readLine()) {
				if (lru.get(id) != null) {
					skipped.add(id); // 当前使用中的项不删除，交给调用方转移登记到当天的清单里。
					continue;
				}
				var key = ByteBuffer.Allocate(9);
				key.WriteString(id);
				db.delete(RocksDatabase.getDefaultWriteOptions(), key.Bytes, 0, key.WriteIndex);
			}
		}
		return skipped;
	}
}
