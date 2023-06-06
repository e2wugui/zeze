package Zeze.Dbh2;

import java.nio.file.Path;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

/**
 * 桶管理一张表的局部范围的记录。
 */
public class Bucket {
	private final RocksDatabase db;
	private final RocksDatabase.Table tData;
	private final RocksDatabase.Table tMeta;
	private final RocksDatabase.Batch batch;
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();
	private volatile BBucketMeta.Data meta;
	private volatile BBucketMeta.Data splittingMeta;
	private final MasterTable.Data splitMetaHistory;
	private volatile byte[] splitCleanKey;
	private long tid;
	private final byte[] metaKey = new byte[]{1};
	private final byte[] metaTid = ByteBuffer.Empty;
	private final byte[] metaSplittingKey = new byte[]{2};
	private final byte[] metaSplitKeyHistory = new byte[]{3};
	private final byte[] metaSplitCleanKey = new byte[]{4};

	public WriteOptions getWriteOptions() {
		return writeOptions;
	}

	public void setWriteOptions(WriteOptions options) {
		writeOptions = options;
	}

	public RocksDatabase getDb() {
		return db;
	}

	public RocksDatabase.Table getTData() {
		return tData;
	}

	public BBucketMeta.Data getSplittingMeta() {
		return splittingMeta;
	}

	public void deleteSplittingMeta() throws RocksDBException {
		tMeta.delete(metaSplittingKey);
		splittingMeta = null;
	}

	public byte[] getSplitCleanKey() {
		return splitCleanKey;
	}

	public void setSplitCleanKey(Binary key) throws RocksDBException {
		tMeta.put(writeOptions, metaSplitCleanKey, 0, metaSplitCleanKey.length,
				key.bytesUnsafe(), key.getOffset(), key.size());
		splitCleanKey = key.copyIf();
	}

	public void deleteSplitCleanKey() throws RocksDBException {
		tMeta.delete(metaSplitCleanKey);
		splitCleanKey = null;
	}

	public RocksDatabase.Batch getBatch() {
		return batch;
	}

	public Bucket(RaftConfig raftConfig) {
		try {
			// 读取meta，meta创建在Bucket创建流程中写入。
			var path = Path.of(raftConfig.getDbHome(), "statemachine").toAbsolutePath().toString();
			db = new RocksDatabase(path, RocksDatabase.DbType.eTransactionDb);
			tData = db.getOrAddTable("data");
			tMeta = db.getOrAddTable("meta");
			batch = db.newBatch();
			var metaValue = tMeta.get(metaKey);
			if (null != metaValue) {
				var bb = ByteBuffer.Wrap(metaValue);
				this.meta = new BBucketMeta.Data();
				this.meta.decode(bb);
			}
			var splittingMetaValue = tMeta.get(metaSplittingKey);
			if (null != splittingMetaValue) {
				var bb = ByteBuffer.Wrap(splittingMetaValue);
				this.splittingMeta = new BBucketMeta.Data();
				this.splittingMeta.decode(bb);
			}
			var splitMetaHistoryValue = tMeta.get(metaSplitKeyHistory);
			if (null != splitMetaHistoryValue) {
				var bb = ByteBuffer.Wrap(splitMetaHistoryValue);
				this.splitMetaHistory = new MasterTable.Data();
				this.splitMetaHistory.decode(bb);
			} else {
				this.splitMetaHistory = new MasterTable.Data();
			}
			var tidValue = tMeta.get(metaTid);
			if (null != tidValue) {
				var bb = ByteBuffer.Wrap(tidValue);
				tid = bb.ReadLong();
			}
			splitCleanKey = tMeta.get(metaSplitCleanKey);
		} catch (RocksDBException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void setMeta(BBucketMeta.Data meta) throws RocksDBException {
		var bb = ByteBuffer.Allocate(32);
		meta.encode(bb);
		tMeta.put(writeOptions, metaKey, 0, metaKey.length, bb.Bytes, 0, bb.WriteIndex);
		this.meta = meta;
	}

	public void setSplittingMeta(BBucketMeta.Data meta) throws RocksDBException {
		var bb = ByteBuffer.Allocate(32);
		meta.encode(bb);
		tMeta.put(writeOptions, metaSplittingKey, 0, metaSplittingKey.length, bb.Bytes, 0, bb.WriteIndex);
		this.splittingMeta = meta;
	}

	public void addSplitMetaHistory(BBucketMeta.Data from, BBucketMeta.Data to) throws RocksDBException {
		this.splitMetaHistory.getBuckets().put(from.getKeyFirst(), from);
		this.splitMetaHistory.getBuckets().put(to.getKeyFirst(), to);
		var bb = ByteBuffer.Allocate();
		this.splitMetaHistory.encode(bb);
		tMeta.put(writeOptions, metaSplitKeyHistory, 0, metaSplitKeyHistory.length, bb.Bytes, 0, bb.WriteIndex);
	}

	public MasterTable.Data getSplitMetaHistory() {
		return splitMetaHistory;
	}

	public void setTid(long tid) throws RocksDBException {
		var bb = ByteBuffer.Allocate(9);
		bb.WriteLong(tid);
		tMeta.put(writeOptions, metaTid, 0, metaTid.length, bb.Bytes, 0, bb.WriteIndex);
		this.tid = tid;
	}

	public long getTid() {
		return tid;
	}

	public Binary get(Binary key) throws RocksDBException {
		var value = tData.get(key.bytesUnsafe(), key.getOffset(), key.size());
		if (null == value)
			return null;
		return new Binary(value);
	}

	public void deleteBatch(RocksDatabase.Batch batch, Binary key) throws RocksDBException {
		tData.delete(batch, key);
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
		batch.close();
		db.close();
	}

	public BBucketMeta.Data getMeta() {
		return meta;
	}
}
