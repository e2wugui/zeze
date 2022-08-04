package Zeze.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.function.IntFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseRocksDb;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class Cache {
	private final String name;
	private final IntFunction<CacheObject> factory;
	private RocksDB db;
	private ConcurrentLruLike<Long, CacheObject> lru;
	private volatile long todayDays;
	private volatile FileOutputStream todayFile;

	/**
	 * 创建LocalCache
	 *
	 * @param name Cache名字，直接作为目录名字，需要注意有些字符可能不能用。
	 * @param lruCapacity 解码后的对象lru容量。
	 * @param factory 根据id创建对象实例的工厂。
	 */
	public Cache(String name, int lruCapacity, IntFunction<CacheObject> factory) throws RocksDBException {
		this.name = name;
		this.factory = factory;

		//noinspection ResultOfMethodCallIgnored
		new File(name).mkdirs();
		db = RocksDB.open(name);
		lru = new ConcurrentLruLike<>(name, lruCapacity);
		// 每天6:30尝试删除旧的项。
		Task.scheduleAt(6, 30, this::tryRemove);
	}

	public void close() throws IOException {
		if (null != todayFile)
			todayFile.close();

		db.close();
		db = null;
		lru = null;
	}

	public CacheObject get(long id) throws RocksDBException {
		var value = lru.get(id);
		if (null != value)
			return value;

		var key = ByteBuffer.Allocate(9);
		key.WriteLong(id);
		var bytes = db.get(DatabaseRocksDb.getDefaultReadOptions(), key.Bytes, 0, key.WriteIndex);
		if (null == bytes)
			return null;

		var bb = ByteBuffer.Wrap(bytes);
		value = factory.apply(bb.ReadInt());
		value.Decode(bb);

		// 当出现并发get重复从db读取时，这里的getOrAdd会忽略后面读到的value，返回已经存在的。
		var tmpLambda = value;
		return lru.getOrAdd(id, () -> tmpLambda);
	}

	public void put(long id, CacheObject value) throws RocksDBException, IOException {
		lru.getOrAdd(id, () -> value);
		var bb = ByteBuffer.Allocate();
		bb.WriteInt(value.cacheId());
		value.Encode(bb);
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(id);
		db.put(DatabaseRocksDb.getDefaultWriteOptions(), key.Bytes, 0, key.WriteIndex, bb.Bytes, 0, bb.WriteIndex);

		today().write((id + "\n").getBytes(StandardCharsets.UTF_8));
	}

	private FileOutputStream today() throws IOException {
		var nowDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
		// 第一次执行时，如果nowDays等于0（即days的初始值），会返回null。
		// 这种情况就不处理了。
		if (todayDays != nowDays) {
			synchronized (this) {
				if (todayDays != nowDays) {
					todayDays = nowDays;
					if (null != todayFile)
						todayFile.close();
					todayFile = new FileOutputStream(Paths.get(name, "days_" + todayDays).toFile());
				}
				return todayFile;
			}
		}
		return todayFile;
	}

	private void tryRemove() throws IOException, RocksDBException {
		var prefix = "days_";
		var nowDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
		var files = new File(name).listFiles();
		if (files != null) {
			for (var file : files) {
				if (file.getName().startsWith(prefix)) {
					var days = Long.parseLong(file.getName().substring(prefix.length()));
					// a month ago && not today
					if (nowDays - days > 30 && days != todayDays)
						tryRemove(file);
				}
			}
		}
	}

	private void tryRemove(File file) throws IOException, RocksDBException {
		try (var r = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
			for (var line = r.readLine(); line != null; line = r.readLine()) {
				var id = Long.parseLong(line);
				if (lru.get(id) != null)
					continue; // 当前使用中的项不删除。
				var key = ByteBuffer.Allocate(9);
				key.WriteLong(id);
				db.delete(DatabaseRocksDb.getDefaultWriteOptions(), key.Bytes, 0, key.WriteIndex);
			}
		}
	}
}
