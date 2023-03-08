package Zeze.Dbh2.Master;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMetaData;
import Zeze.Dbh2.Bucket;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class MasterDatabase {
	private final String databaseName;
	private final ConcurrentHashMap<String, BMasterTable> tables = new ConcurrentHashMap<>();
	private final RocksDB db;

	public MasterDatabase(String databaseName) throws RocksDBException {
		this.databaseName = databaseName;
		this.db = RocksDB.open(databaseName);

		try (var it = this.db.newIterator(Bucket.getDefaultReadOptions())) {
			while (it.isValid()) {
				var tableName = new String(it.key(), StandardCharsets.UTF_8);
				var bTable = new BMasterTable();
				var bb = ByteBuffer.Wrap(it.value());
				bTable.decode(bb);
				tables.put(tableName, bTable);
				it.next();
			}
		}
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public BMasterTable getTable(String tableName) {
		return tables.get(tableName);
	}

	public BBucketMetaData locateBucket(String tableName, Binary key) {
		var bTable = getTable(tableName);
		if (null == bTable)
			return null;
		return bTable.locate(key);
	}

	public boolean createTable(String tableName) throws RocksDBException {
		var key = tableName.getBytes(StandardCharsets.UTF_8);
		if (this.db.get(key) != null)
			return false; // table exist

		var table = new BMasterTable();
		var bucket = new BBucketMetaData();
		// todo allocate first bucket service and setup table

		bucket.setKeyFirst(Binary.Empty);
		bucket.setKeyLast(Binary.Empty);
		table.buckets.put(bucket.getKeyFirst(), bucket);

		// master数据马上存数据库。
		var bbValue = ByteBuffer.Allocate();
		table.encode(bbValue);
		this.db.put(Bucket.getDefaultWriteOptions(), key, 0, key.length, bbValue.Bytes, bbValue.ReadIndex, bbValue.size());

		// 保存在内存中，用来快速查询。
		this.tables.put(tableName, table);
		return true;
	}
}
