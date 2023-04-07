package Zeze.Dbh2;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

/**
 * 桶管理一张表的局部范围的记录。
 */
public class Bucket {
	private static final Logger logger = LogManager.getLogger(Bucket.class);

	private final OptimisticTransactionDB db;
	private final HashMap<String, ColumnFamilyHandle> cfHandles = new HashMap<>();
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();
	private volatile BBucketMeta.Data meta;
	private long tid;
	private final ColumnFamilyHandle cfMeta;
	private final byte[] metaKey = new byte[]{1};
	private final byte[] metaTid = new byte[0];

	private ColumnFamilyHandle openFamily(String name) {
		return cfHandles.computeIfAbsent(name, (_name) -> {
			try {
				return db.createColumnFamily(new ColumnFamilyDescriptor(
						name.getBytes(StandardCharsets.UTF_8), RocksDatabase.getDefaultCfOptions()));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public void setWriteOptions(WriteOptions options) {
		writeOptions = options;
	}

	public OptimisticTransactionDB getDb() {
		return db;
	}

	public Bucket(RaftConfig raftConfig) {
		try {
			// 读取meta，meta创建在Bucket创建流程中写入。
			var path = Path.of(raftConfig.getDbHome(), "statemachine").toAbsolutePath().toString();
			logger.info("RocksDB.open: '{}'", path);
			var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
			for (var cf : OptimisticTransactionDB.listColumnFamilies(RocksDatabase.getCommonOptions(), path))
				columnFamilies.add(new ColumnFamilyDescriptor(cf, RocksDatabase.getDefaultCfOptions()));
			if (columnFamilies.isEmpty())
				columnFamilies.add(new ColumnFamilyDescriptor(
						"default".getBytes(StandardCharsets.UTF_8), RocksDatabase.getDefaultCfOptions()));
			var cfHandlesOut = new ArrayList<ColumnFamilyHandle>();
			this.db = OptimisticTransactionDB.open(RocksDatabase.getCommonDbOptions(), path, columnFamilies, cfHandlesOut);
			for (var i = 0; i < columnFamilies.size(); ++i) {
				var cfName = new String(columnFamilies.get(i).getName(), StandardCharsets.UTF_8);
				this.cfHandles.put(cfName, cfHandlesOut.get(i));
			}
			this.cfMeta = openFamily("meta");
			var metaValue = db.get(cfMeta, metaKey);
			if (null != metaValue) {
				var bb = ByteBuffer.Wrap(metaValue);
				this.meta = new BBucketMeta.Data();
				this.meta.decode(bb);
			}
			var tidValue = db.get(cfMeta, metaTid);
			if (null != tidValue) {
				var bb = ByteBuffer.Wrap(tidValue);
				tid = bb.ReadLong();
			}
		} catch (RocksDBException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void setMeta(BBucketMeta.Data meta) throws RocksDBException {
		var bb = ByteBuffer.Allocate();
		meta.encode(bb);
		db.put(cfMeta, writeOptions, metaKey, 0, metaKey.length, bb.Bytes, bb.ReadIndex, bb.size());
		this.meta = meta;
	}

	public void setTid(long tid) throws RocksDBException {
		var bb = ByteBuffer.Allocate();
		bb.WriteLong(tid);
		db.put(cfMeta, writeOptions, metaTid, 0, metaTid.length, bb.Bytes, bb.ReadIndex, bb.size());
		this.tid = tid;
	}

	public long getTid() {
		return tid;
	}

	public Dbh2Transaction beginTransaction() {
		return new Dbh2Transaction(db.beginTransaction(writeOptions));
	}

	public byte[] get(Binary key) throws RocksDBException {
		var lock = Lock.get(key.bytesUnsafe());
		lock.lock();
		try {
			return db.get(RocksDatabase.getDefaultReadOptions(), key.bytesUnsafe(), key.getOffset(), key.size());
		} finally {
			lock.unlock();
		}
	}

	public boolean inBucket(String databaseName, String tableName) {
		return databaseName.equals(meta.getDatabaseName()) && tableName.equals(meta.getTableName());
	}

	public boolean inBucket(Binary key) {
		return key.compareTo(meta.getKeyFirst()) >= 0
				&& (meta.getKeyLast().size() == 0 || key.compareTo(meta.getKeyLast()) < 0);
	}

	public boolean inBucket(String databaseName, String tableName, Binary key) {
		return inBucket(databaseName, tableName) && inBucket(key);
	}

	public void close() {
		db.close();
	}

	public BBucketMeta.Data getMeta() {
		return meta;
	}
}
