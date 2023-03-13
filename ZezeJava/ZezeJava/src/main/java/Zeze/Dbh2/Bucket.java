package Zeze.Dbh2;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

/**
 * 桶管理一张表的局部范围的记录。
 */
public class Bucket {
	private static final Options commonOptions = new Options()
			.setCreateIfMissing(true)
			.setDbWriteBufferSize(64 << 20) // total write buffer bytes, include all the columns
			.setKeepLogFileNum(5); // reserve "LOG.old.*" file count
	private static final DBOptions commonDbOptions = new DBOptions()
			.setCreateIfMissing(true)
			.setDbWriteBufferSize(64 << 20) // total write buffer bytes, include all the columns
			.setKeepLogFileNum(5); // reserve "LOG.old.*" file count
	private static final ColumnFamilyOptions defaultCfOptions = new ColumnFamilyOptions();
	private static final ReadOptions defaultReadOptions = new ReadOptions();
	private static final WriteOptions defaultWriteOptions = new WriteOptions();
	private static final WriteOptions syncWriteOptions = new WriteOptions().setSync(true);

	public static Options getCommonOptions() {
		return commonOptions;
	}

	public static DBOptions getCommonDbOptions() {
		return commonDbOptions;
	}

	public static ColumnFamilyOptions getDefaultCfOptions() {
		return defaultCfOptions;
	}

	public static ReadOptions getDefaultReadOptions() {
		return defaultReadOptions;
	}

	public static WriteOptions getDefaultWriteOptions() {
		return defaultWriteOptions;
	}

	public static WriteOptions getSyncWriteOptions() {
		return syncWriteOptions;
	}

	private final OptimisticTransactionDB db;
	private final HashMap<String, ColumnFamilyHandle> cfHandles = new HashMap<>();
	private final WriteOptions writeOptions = new WriteOptions();
	private volatile BBucketMeta.Data meta;
	private long tid;
	private final ColumnFamilyHandle cfMeta;
	private final byte[] metaKey = new byte[] { 1 };
	private final byte[] metaTid = new byte[0];

	private ColumnFamilyHandle cfOpen(String name) {
		return cfHandles.computeIfAbsent(name, (_name) -> {
			try {
				return db.createColumnFamily(
						new ColumnFamilyDescriptor(name.getBytes(StandardCharsets.UTF_8), defaultCfOptions));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public OptimisticTransactionDB getDb() {
		return db;
	}

	public Bucket(RaftConfig raftConfig) {
		try {
			// 读取meta，meta创建在Bucket创建流程中写入。
			var path = Path.of(raftConfig.getDbHome(), "statemachine").toAbsolutePath().toString();
			var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
			for (var cf : OptimisticTransactionDB.listColumnFamilies(new Options(), path))
				columnFamilies.add(new ColumnFamilyDescriptor(cf, defaultCfOptions));
			if (columnFamilies.isEmpty())
				columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(StandardCharsets.UTF_8), defaultCfOptions));
			var dbOptions = new DBOptions().setCreateIfMissing(true);
			var cfHandlesOut = new ArrayList<ColumnFamilyHandle>();
			this.db = OptimisticTransactionDB.open(dbOptions, path, columnFamilies, cfHandlesOut);
			for (var i = 0; i < columnFamilies.size(); ++i) {
				var cfName = new String(columnFamilies.get(i).getName(), StandardCharsets.UTF_8);
				this.cfHandles.put(cfName, cfHandlesOut.get(i));
			}
			this.cfMeta = cfOpen("meta");
			var metaValue = db.get(cfMeta, metaKey);
			if (null != metaValue) {
				var bb = ByteBuffer.Wrap(metaValue);
				this.meta = new BBucketMeta.Data();
				this.meta.decode(bb);
			}
			var tidValue = db.get(cfMeta, metaTid);
			if (null != tidValue) {
				var bb = ByteBuffer.Wrap(metaValue);
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
			return db.get(getDefaultReadOptions(), key.bytesUnsafe(), key.getOffset(), key.size());
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
