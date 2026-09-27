package Zeze.Dbh2;

import java.nio.file.Path;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

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
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();
	private volatile BBucketMeta.Data bucketMeta;
	private volatile BBucketMeta.Data splittingMeta;
	private final MasterTable.Data splitMetaHistory;
	private long tid;
	private final byte[] metaKey = new byte[]{1};
	private final byte[] metaTid = ByteBuffer.Empty;
	private final byte[] metaSplittingKey = new byte[]{2};
	private final byte[] metaSplitKeyHistory = new byte[]{3};
	private final byte[] metaPendingSettleKey = new byte[]{4};

	/**
	 * pending-settle标志（GA-D01 A1/INV2）：最近一次已commit迁移（LogEndSplit/LogEndMove
	 * 的apply）的完整from/to meta。它是**派生状态**（从raft日志参数派生，不新增日志schema），
	 * 天然随raft复制、随快照持久化——commit过的日志在多数派上，任何后来当选的leader
	 * apply后即持有标志。这是(A)路径缺失的状态源：settle通知的持久载体，leader-ready时
	 * 据此幂等补发。from为null即move。
	 */
	public static final class PendingSettle {
		private final BBucketMeta.Data from; // null=move
		private final BBucketMeta.Data to;

		PendingSettle(BBucketMeta.Data from, BBucketMeta.Data to) {
			this.from = from;
			this.to = to;
		}

		public BBucketMeta.Data getFrom() {
			return from;
		}

		public BBucketMeta.Data getTo() {
			return to;
		}
	}

	private volatile PendingSettle pendingSettle;

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

	public BBucketMeta.Data getSplittingMeta() {
		return splittingMeta;
	}

	public void deleteSplittingMeta() throws RocksDBException {
		meta.delete(metaSplittingKey);
		splittingMeta = null;
	}

	public RocksDatabase.Batch getBatch() {
		return batch;
	}

	public Bucket(RaftConfig raftConfig) {
		try {
			// 读取meta，meta创建在Bucket创建流程中写入。
			var path = Path.of(raftConfig.getDbHome(), "statemachine").toAbsolutePath().toString();
			db = new RocksDatabase(path, RocksDatabase.DbType.eRocksDb);
			data = db.getOrAddTable("data");
			trans = db.getOrAddTable("transaction");
			meta = db.getOrAddTable("meta");
			batch = db.newBatch();
			var metaValue = meta.get(metaKey);
			if (null != metaValue) {
				var bb = ByteBuffer.Wrap(metaValue);
				this.bucketMeta = new BBucketMeta.Data();
				this.bucketMeta.decode(bb);
			}
			var splittingMetaValue = meta.get(metaSplittingKey);
			if (null != splittingMetaValue) {
				var bb = ByteBuffer.Wrap(splittingMetaValue);
				this.splittingMeta = new BBucketMeta.Data();
				this.splittingMeta.decode(bb);
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
		} catch (RocksDBException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void setBucketMeta(BBucketMeta.Data bucketMeta) throws RocksDBException {
		var bb = ByteBuffer.Allocate(32);
		bucketMeta.encode(bb);
		meta.put(writeOptions, metaKey, 0, metaKey.length, bb.Bytes, 0, bb.WriteIndex);
		this.bucketMeta = bucketMeta;
	}

	public void setSplittingMeta(BBucketMeta.Data meta) throws RocksDBException {
		var bb = ByteBuffer.Allocate(32);
		meta.encode(bb);
		this.meta.put(writeOptions, metaSplittingKey, 0, metaSplittingKey.length, bb.Bytes, 0, bb.WriteIndex);
		this.splittingMeta = meta;
	}

	private static BBucketMeta.Data decodeMeta(ByteBuffer bb) {
		var meta = new BBucketMeta.Data();
		meta.decode(bb);
		return meta;
	}

	public PendingSettle getPendingSettle() {
		return pendingSettle;
	}

	// 与既有meta写入同批（同一apply内顺序落盘），标志在LogEndSplit/LogEndMove的apply里设置。
	// 【条件覆写（FND22 GA-C02）】旧标志未清且属不同迁移（to身份不等，判据与clearPendingSettle
	// 同源）时不覆写：旧标志在=旧迁移的settle未到终局=其补发源仍被需要——单槽无条件覆盖会灭失
	// 旧迁移唯一的死亡恢复源（进程死后recoverSplitting只补发槽内标志，旧迁移永不结算，其to键域
	// 主表无主、读写永久失败）。保留旧标志的代价是新迁移失去标志载体，其settle在进程存活期内由
	// 内存30s重试链兜底；两害相权取其旧：旧迁移的settle已滞留更久，且保留旧标志在堆叠死亡链中
	// 数据面可完整收敛（旧settle补发即发布齐两半键域，仅留孤儿条目+告警），覆写则旧键域必失联。
	// 同身份重设幂等放行（raft日志每节点恰apply一次，仅防御）。堆叠窗口本身的完整闭口
	//（tryStartSplit对pending!=null加闸/多槽标志）二期。
	public void setPendingSettle(BBucketMeta.Data from, BBucketMeta.Data to) throws RocksDBException {
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
		meta.put(writeOptions, metaPendingSettleKey, 0, metaPendingSettleKey.length, bb.Bytes, 0, bb.WriteIndex);
		pendingSettle = new PendingSettle(from, to);
	}

	// 身份匹配才清除（四元组+raftConfig全等，两侧都是完整meta——与resume场景请求方
	// raftConfig=""不同，全等比较在此合法，不触碰R1钉死约束）：陈旧世代的清除日志不得
	// 清掉新世代的标志（跨世代倒灌防护：旧迁移终局时新迁移可能已apply了自己的标志）。
	public void clearPendingSettle(BBucketMeta.Data to) throws RocksDBException {
		var current = pendingSettle;
		if (null == current || !sameMeta(current.getTo(), to))
			return;
		meta.delete(writeOptions, metaPendingSettleKey, 0, metaPendingSettleKey.length);
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
			meta.put(writeOptions, metaSplitKeyHistory, 0, metaSplitKeyHistory.length, bb.Bytes, 0, bb.WriteIndex);
		} finally {
			splitMetaHistory.unlock();
		}
	}

	public void addSplitMetaHistory(BBucketMeta.Data from, BBucketMeta.Data to) throws RocksDBException {
		splitMetaHistory.lock(); // 与读路径locate()互斥：写在raft apply线程，读在user-task线程
		try {
			this.splitMetaHistory.getBuckets().put(from.getKeyFirst(), from);
			this.splitMetaHistory.getBuckets().put(to.getKeyFirst(), to);
			var bb = ByteBuffer.Allocate();
			this.splitMetaHistory.encode(bb);
			meta.put(writeOptions, metaSplitKeyHistory, 0, metaSplitKeyHistory.length, bb.Bytes, 0, bb.WriteIndex);
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
		meta.put(writeOptions, metaTid, 0, metaTid.length, bb.Bytes, 0, bb.WriteIndex);
		this.tid = tid;
	}

	public long getTid() {
		return tid;
	}

	public Binary get(Binary key) throws RocksDBException {
		var value = data.get(key.bytesUnsafe(), key.getOffset(), key.size());
		if (null == value)
			return null;
		return new Binary(value);
	}

	public void deleteBatch(RocksDatabase.Batch batch, Binary key) throws RocksDBException {
		data.delete(batch, key);
	}

	public boolean inBucket(String databaseName, String tableName) {
		return databaseName.equals(bucketMeta.getDatabaseName()) && tableName.equals(bucketMeta.getTableName());
	}

	public boolean inBucket(Binary key) {
		return key.compareTo(bucketMeta.getKeyFirst()) >= 0
				&& (bucketMeta.getKeyLast().size() == 0 || key.compareTo(bucketMeta.getKeyLast()) < 0);
	}

	public boolean inBucket(String databaseName, String tableName, Binary key) {
		return inBucket(databaseName, tableName) && inBucket(key);
	}

	public void close() {
		batch.close();
		db.close();
	}

	public BBucketMeta.Data getBucketMeta() {
		return bucketMeta;
	}
}
