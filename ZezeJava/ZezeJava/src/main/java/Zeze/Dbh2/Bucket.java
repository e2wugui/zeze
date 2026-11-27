package Zeze.Dbh2;

import java.util.Objects;

import java.nio.file.Path;
import java.util.Arrays;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteOptions;
import org.jspecify.annotations.Nullable;
import Zeze.Builtin.Dbh2.BBucketMeta.Data;

/**
 * 桶管理一张表的局部范围的记录。
 */
public class Bucket {
	private static final Logger logger = LogManager.getLogger(Bucket.class);
	private final RocksDatabase db;
	private final RocksDatabase.Table data;
	private final RocksDatabase.Table trans;
	private final RocksDatabase.Table meta;
	private final RocksDatabase.Batch batch;
	private @Nullable Thread applyingThread; // Raft apply独占；其他线程的ACK不加入本批次
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();
	private volatile @Nullable Data bucketMeta;
	private volatile @Nullable Data splittingMeta;
	private final MasterTable.Data splitMetaHistory;
	private long tid;
	private final byte[] metaKey = new byte[]{1};
	private final byte[] metaTid = ByteBuffer.Empty;
	private final byte[] metaSplittingKey = new byte[]{2};
	private final byte[] metaSplitKeyHistory = new byte[]{3};
	private final byte[] metaPendingSettleKey = new byte[]{4};

	// splitMetaHistory 上限：本桶连续分裂边界严格递减，超限时淘汰最老世代（最大 keyFirst）
	private static final int SplitMetaHistoryMaxEntries = 64;

	/**
	 * 待结算迁移标志：由 LogEndSplit/LogEndMove 的 apply 派生并随快照持久化，供 leader-ready 幂等补发 settle；from 为 null 表示 move。
	 */
	public static final class PendingSettle {
		private final @Nullable Data from; // null 表示 move
		private final BBucketMeta.Data to;

		PendingSettle(@Nullable Data from, BBucketMeta.Data to) {
			this.from = from;
			this.to = to;
		}

		public @Nullable Data getFrom() {
			return from;
		}

		public BBucketMeta.Data getTo() {
			return to;
		}
	}

	// 死桶 meta 哨兵（endMove 将 keyFirst/keyLast 均置为 {1}），活桶边界不可能同时为此值
	public static final Binary DeadBucketMetaBound = new Binary(new byte[]{1});

	private volatile @Nullable PendingSettle pendingSettle;

	public WriteOptions getWriteOptions() {
		return writeOptions;
	}

	public void setWriteOptions(WriteOptions options) {
		writeOptions = options;
	}

	public RocksDatabase getDb() {
		return db;
	}

	public RocksDatabase.Table getData() {
		return data;
	}

	public RocksDatabase.Table getTrans() {
		return trans;
	}

	public @Nullable Data getSplittingMeta() {
		return splittingMeta;
	}

	public void deleteSplittingMeta() throws RocksDBException {
		delete(meta, metaSplittingKey);
		splittingMeta = null;
	}

	public RocksDatabase.Batch getBatch() {
		return batch;
	}

	void beginApply() {
		if (applyingThread != null)
			throw new IllegalStateException("nested bucket apply");
		batch.clear();
		applyingThread = Thread.currentThread();
	}

	boolean isApplying() {
		return applyingThread == Thread.currentThread();
	}

	void endApply() {
		applyingThread = null;
		batch.clear();
	}

	void put(RocksDatabase.Table table, byte[] key, int keyOffset, int keyLength,
			 byte[] value, int valueOffset, int valueLength) throws RocksDBException {
		if (isApplying())
			table.put(batch, key, keyOffset, keyLength, value, valueOffset, valueLength);
		else
			table.put(writeOptions, key, keyOffset, keyLength, value, valueOffset, valueLength);
	}

	void delete(RocksDatabase.Table table, byte[] key) throws RocksDBException {
		if (isApplying())
			table.delete(batch, key);
		else
			table.delete(writeOptions, key);
	}

	void deleteToEnd(RocksIterator it) throws RocksDBException {
		if (!RocksDatabase.isValidOrThrow(it))
			return;
		var first = it.key();
		it.seekToLast();
		if (!RocksDatabase.isValidOrThrow(it))
			return;
		var last = it.key();
		last = Arrays.copyOf(last, last.length + 1); // 与Table.deleteToEnd相同的独占上界
		if (isApplying())
			batch.getWriteBatch().deleteRange(data.getCfHandle(), first, last);
		else
			data.deleteRange(writeOptions, first, last);
	}

	public Bucket(RaftConfig raftConfig) {
		RocksDatabase opening = null;
		RocksDatabase.Batch openingBatch = null;
		try {
			// 读取meta，meta创建在Bucket创建流程中写入。
			var path = Path.of(Objects.requireNonNull(raftConfig.getDbHome()), "statemachine").toAbsolutePath().toString();
			db = opening = new RocksDatabase(path, RocksDatabase.DbType.eRocksDb);
			data = db.getOrAddTable("data");
			trans = db.getOrAddTable("transaction");
			meta = db.getOrAddTable("meta");
			batch = openingBatch = db.newBatch();
			var metaValue = meta.get(metaKey);
			if (null != metaValue) {
				var bb = ByteBuffer.Wrap(metaValue);
				var bucket = new BBucketMeta.Data();
				bucket.decode(bb);
				this.bucketMeta = bucket;
			}
			var splittingMetaValue = meta.get(metaSplittingKey);
			if (null != splittingMetaValue) {
				var bb = ByteBuffer.Wrap(splittingMetaValue);
				var splitting = new BBucketMeta.Data();
				splitting.decode(bb);
				this.splittingMeta = splitting;
			}
			var pendingSettleValue = meta.get(metaPendingSettleKey);
			if (null != pendingSettleValue) {
				var bb = ByteBuffer.Wrap(pendingSettleValue);
				var from = bb.ReadBool() ? decodeMeta(bb) : null;
				this.pendingSettle = new PendingSettle(from, decodeMeta(bb));
			}
			var splitMetaHistoryValue = meta.get(metaSplitKeyHistory);
			if (null != splitMetaHistoryValue) {
				var bb = ByteBuffer.Wrap(splitMetaHistoryValue);
				this.splitMetaHistory = new MasterTable.Data();
				this.splitMetaHistory.decode(bb);
			} else {
				this.splitMetaHistory = new MasterTable.Data();
			}
			var tidValue = meta.get(metaTid);
			if (null != tidValue) {
				var bb = ByteBuffer.Wrap(tidValue);
				tid = bb.ReadLong();
			}
		} catch (Throwable ex) {
			// 元数据损坏也必须释放句柄，才能回退restore/reset。
			if (openingBatch != null) {
				try {
					openingBatch.close();
				} catch (Throwable cleanup) {
					ex.addSuppressed(cleanup);
				}
			}
			if (opening != null) {
				try {
					opening.close();
				} catch (Throwable cleanup) {
					ex.addSuppressed(cleanup);
				}
			}
			throw Task.forceThrow(ex);
		}
	}

	public void setBucketMeta(BBucketMeta.Data bucketMeta) throws RocksDBException {
		var bb = ByteBuffer.Allocate(32);
		bucketMeta.encode(bb);
		put(meta, metaKey, 0, metaKey.length, bb.Bytes, 0, bb.WriteIndex);
		this.bucketMeta = bucketMeta;
	}

	public void setSplittingMeta(BBucketMeta.Data meta) throws RocksDBException {
		var bb = ByteBuffer.Allocate(32);
		meta.encode(bb);
		put(this.meta, metaSplittingKey, 0, metaSplittingKey.length, bb.Bytes, 0, bb.WriteIndex);
		this.splittingMeta = meta;
	}

	private static BBucketMeta.Data decodeMeta(ByteBuffer bb) {
		var meta = new BBucketMeta.Data();
		meta.decode(bb);
		return meta;
	}

	public @Nullable PendingSettle getPendingSettle() {
		return pendingSettle;
	}

	// 仅在无旧标志或同迁移重设时写入，避免覆写未结算旧迁移的持久化恢复源
	public void setPendingSettle(@Nullable Data from, BBucketMeta.Data to) throws RocksDBException {
		var current = pendingSettle;
		if (null != current && !sameMeta(current.getTo(), to)) {
			logger.error("setPendingSettle keep uncleared old flag, skip set. old.from={} old.to={} skip.to={}",
					null == current.getFrom() ? "move" : current.getFrom(), current.getTo(), to);
			return;
		}
		var bb = ByteBuffer.Allocate(32);
		bb.WriteBool(null != from);
		if (null != from)
			from.encode(bb);
		to.encode(bb);
		put(meta, metaPendingSettleKey, 0, metaPendingSettleKey.length, bb.Bytes, 0, bb.WriteIndex);
		pendingSettle = new PendingSettle(from, to);
	}

	// 仅在完整 meta 身份匹配时清除，防止陈旧清除日志误清新一代标志
	public void clearPendingSettle(BBucketMeta.Data to) throws RocksDBException {
		var current = pendingSettle;
		if (null == current || !sameMeta(current.getTo(), to))
			return;
		delete(meta, metaPendingSettleKey);
		pendingSettle = null;
	}

	private static boolean sameMeta(BBucketMeta.Data a, BBucketMeta.Data b) {
		return a.getDatabaseName().equals(b.getDatabaseName())
				&& a.getTableName().equals(b.getTableName())
				&& a.getKeyFirst().equals(b.getKeyFirst())
				&& a.getKeyLast().equals(b.getKeyLast())
				&& a.getRaftConfig().equals(b.getRaftConfig());
	}

	public void addMoveMetaHistory(BBucketMeta.Data to) throws RocksDBException {
		splitMetaHistory.lock(); // 与读路径locate()互斥：写在raft apply线程，读在user-task线程
		try {
			this.splitMetaHistory.getBuckets().put(to.getKeyFirst(), to);
			var bb = ByteBuffer.Allocate();
			this.splitMetaHistory.encode(bb);
			put(meta, metaSplitKeyHistory, 0, metaSplitKeyHistory.length, bb.Bytes, 0, bb.WriteIndex);
		} finally {
			splitMetaHistory.unlock();
		}
	}

	public void addSplitMetaHistory(BBucketMeta.Data from, BBucketMeta.Data to) throws RocksDBException {
		splitMetaHistory.lock(); // 与读路径locate()互斥：写在raft apply线程，读在user-task线程
		try {
			this.splitMetaHistory.getBuckets().put(from.getKeyFirst(), from);
			this.splitMetaHistory.getBuckets().put(to.getKeyFirst(), to);
			// 超限时按最大 keyFirst 淘汰最老世代，保证各副本确定性裁剪
			while (this.splitMetaHistory.getBuckets().size() > SplitMetaHistoryMaxEntries)
				this.splitMetaHistory.getBuckets().pollLastEntry();
			var bb = ByteBuffer.Allocate();
			this.splitMetaHistory.encode(bb);
			put(meta, metaSplitKeyHistory, 0, metaSplitKeyHistory.length, bb.Bytes, 0, bb.WriteIndex);
		} finally {
			splitMetaHistory.unlock();
		}
	}

	public MasterTable.Data getSplitMetaHistory() {
		return splitMetaHistory;
	}

	public void setTid(long tid) throws RocksDBException {
		var bb = ByteBuffer.Allocate(9);
		bb.WriteLong(tid);
		put(meta, metaTid, 0, metaTid.length, bb.Bytes, 0, bb.WriteIndex);
		this.tid = tid;
	}

	public long getTid() {
		return tid;
	}

	public @Nullable Binary get(Binary key) throws RocksDBException {
		var value = data.get(key.bytesUnsafe(), key.getOffset(), key.size());
		if (null == value)
			return null;
		return new Binary(value);
	}

	public void deleteBatch(RocksDatabase.Batch batch, Binary key) throws RocksDBException {
		data.delete(batch, key);
	}

	// 使用指定 meta 快照判定，供查询路径的“快照→校验→读数据→复核”共享同一份视图
	public boolean inBucket(BBucketMeta.Data meta, Binary key) {
		return key.compareTo(meta.getKeyFirst()) >= 0
				&& (meta.getKeyLast().size() == 0 || key.compareTo(meta.getKeyLast()) < 0);
	}

	public boolean inBucket(BBucketMeta.Data meta, String databaseName, String tableName, Binary key) {
		return databaseName.equals(meta.getDatabaseName()) && tableName.equals(meta.getTableName())
				&& inBucket(meta, key);
	}

	public boolean inBucket(String databaseName, String tableName) {
		var bucketMeta = Objects.requireNonNull(this.bucketMeta);
		return databaseName.equals(bucketMeta.getDatabaseName()) && tableName.equals(bucketMeta.getTableName());
	}

	public boolean inBucket(Binary key) {
		return inBucket(Objects.requireNonNull(bucketMeta), key);
	}

	public boolean inBucket(String databaseName, String tableName, Binary key) {
		return inBucket(Objects.requireNonNull(bucketMeta), databaseName, tableName, key);
	}

	public void close() {
		batch.close();
		db.close();
	}

	public @Nullable Data getBucketMeta() {
		return bucketMeta;
	}
}
