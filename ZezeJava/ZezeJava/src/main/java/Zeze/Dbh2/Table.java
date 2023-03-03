package Zeze.Dbh2;

import java.nio.file.Path;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

/**
 * 桶管理一张表的局部范围的记录。
 */
public class Table {
	private final OptimisticTransactionDB db;
	private final String path;
	private final Database database;
	private final String name;
	private final WriteOptions writeOptions = new WriteOptions();

	public Table(Database database, String tableName) {
		this.database = database;
		this.name = tableName;
		this.path = Path.of(database.getName(), tableName).toAbsolutePath().toString();
		var options = new Options().setCreateIfMissing(true);
		try {
			this.db = OptimisticTransactionDB.open(options, path);
		} catch (RocksDBException ex) {
			throw new RuntimeException(ex);
		}
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

	public void close() {
		db.close();
	}

	public Database getDatabase() {
		return database;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}
}
