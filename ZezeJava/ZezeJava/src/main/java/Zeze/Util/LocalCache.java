package Zeze.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.DatabaseRocksDb;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class LocalCache<T extends Serializable> {
	private String name;
	private RocksDB db;
	private ConcurrentLruLike<Long, T> lru;
	private volatile long todayDays;
	private volatile FileOutputStream file;

	/**
	 * 创建LocalCache
	 * @param name Cache名字，直接作为目录名字，需要注意有些字符可能不能用。
	 */
	public LocalCache(String name) {
		this.name = name;
		new File(name).mkdir();

		// 每天6:30尝试删除旧的项。
		Task.scheduleAt(6, 30, this::tryRemove);
	}

	public void close() throws Exception {
		if (null != file)
			file.close();

		db.close();
		db = null;
		lru = null;
	}

	public T get(long id) throws Exception {
		var value = lru.get(id);
		if (null != value)
			return value;

		var key = ByteBuffer.Allocate(9);
		key.WriteLong(id);
		var bytes = db.get(DatabaseRocksDb.getDefaultReadOptions(), key.Bytes, 0, key.WriteIndex);
		if (null == bytes)
			return null;
		var bb = ByteBuffer.Wrap(bytes);
		return null;
	}

	public void put(long id, T value) throws Exception {
		lru.getOrAdd(id, () -> value);
		var bb = ByteBuffer.Allocate();
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
					if (null != file)
						file.close();
					file = new FileOutputStream(Paths.get(name, "days_" + todayDays).toFile());
				}
				return file;
			}
		}
		return file;
	}

	private void tryRemove() throws IOException, RocksDBException {
		var prefix = "days_";
		var nowDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
		for (var file : new File(name).listFiles()) {
			if (file.getName().startsWith(prefix)) {
				var days = Long.parseLong(file.getName().substring(prefix.length()));
				// a month ago && not today
				if (nowDays - days > 30 && days != todayDays)
					tryRemove(file);
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
