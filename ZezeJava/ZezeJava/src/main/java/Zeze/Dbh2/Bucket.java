package Zeze.Dbh2;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import Zeze.Builtin.Dbh2.BMeta;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

/**
 * 桶管理一张表的局部范围的记录。
 */
public class Bucket {
	private final OptimisticTransactionDB db;
	private BMeta meta = new BMeta();
	private final ColumnFamilyHandle cfMeta;
	private final WriteOptions writeOptions = new WriteOptions();

	private static ColumnFamilyHandle cfMeta(OptimisticTransactionDB db,
											 ArrayList<ColumnFamilyDescriptor> columnFamilies,
											 ArrayList<ColumnFamilyHandle> cfHandles) throws RocksDBException {
		for (var i = 0; i < columnFamilies.size(); ++i) {
			var cfName = new String(columnFamilies.get(i).getName(), StandardCharsets.UTF_8);
			if (cfName.equals("meta")) {
				return cfHandles.get(i);
			}
		}
		return db.createColumnFamily(new ColumnFamilyDescriptor(
				"meta".getBytes(StandardCharsets.UTF_8),
				new ColumnFamilyOptions()));
	}
	public Bucket(String raftName) {
		try {
			// 读取meta，meta创建在Bucket创建流程中写入。
			var path = Path.of(raftName).toAbsolutePath().toString();
			var cfOptions = new ColumnFamilyOptions();
			var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
			for (var cf : OptimisticTransactionDB.listColumnFamilies(new Options(), path))
				columnFamilies.add(new ColumnFamilyDescriptor(cf, cfOptions));
			var options = new DBOptions();
			var outHandles = new ArrayList<ColumnFamilyHandle>();
			this.db = OptimisticTransactionDB.open(options, path, columnFamilies, outHandles);
			cfMeta = cfMeta(db, columnFamilies, outHandles);
			var metaValue = db.get(cfMeta, new byte[0]);
			if (null != metaValue) {
				var bb = ByteBuffer.Wrap(metaValue);
				this.meta.decode(bb);
			}
		} catch (RocksDBException ex) {
			throw new RuntimeException(ex);
		}
		throw new RuntimeException("meta record not found");
	}

	public void setMeta(BMeta meta) throws RocksDBException {
		var bbMeta = ByteBuffer.Allocate();
		meta.encode(bbMeta);
		var key = new byte[0];
		db.put(cfMeta, writeOptions, key, 0, 0, bbMeta.Bytes, bbMeta.ReadIndex, bbMeta.size());
		this.meta = meta;
	}

	public Transaction beginTransaction() {
		return new Transaction(db.beginTransaction(writeOptions));
	}

	public byte[] get(byte[] key) throws RocksDBException {
		var lock = Lock.get(key);
		lock.lock();
		try {
			return db.get(key);
		} finally {
			lock.unlock();
		}
	}

	public boolean inBucket(String databaseName, String tableName) {
		return databaseName.equals(meta.getDatabaseName()) && tableName.equals(meta.getTableName());
	}

	public boolean inBucket(String databaseName, String tableName, Binary key) {
		return inBucket(databaseName, tableName)
				&& key.compareTo(meta.getKeyFirst()) >= 0
				&& key.compareTo(meta.getKeyLast()) < 0;
	}

	public void close() {
		db.close();
	}

	public BMeta getMeta() {
		return meta;
	}
}
