package Zeze.Dbh2.Master;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Builtin.Dbh2.Master.DestroyBucket;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutObject;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.rocksdb.RocksDBException;
import Zeze.Dbh2.Master.MasterTable.Data;

/**
 * Master 侧单数据库的元数据存储：主表与 splitting 分桶表的内存映射及 RocksDB 持久化。
 */
public class MasterDatabase {
	private static final Logger logger = LogManager.getLogger(MasterDatabase.class);
	private final String databaseName;
	private final RocksDatabase rocksDb;

	// tables
	private final ConcurrentHashMap<String, MasterTable.Data> tables = new ConcurrentHashMap<>();
	private final RocksDatabase.Table rocksTables;

	// tables 包含分桶目标。
	private final ConcurrentHashMap<String, MasterTable.Data> splitting = new ConcurrentHashMap<>();
	private final RocksDatabase.Table rocksSplitting;
	// splitting 条目年龄与世代副表（key=tableName+keyFirst → [时间戳, 可选源桶 raft 身份]），不改动 MasterTable 编码格式
	private final RocksDatabase.Table rocksSplittingAge;
	private final Master master;

	public MasterDatabase(Master master, String databaseName) {
		RocksDatabase opening = null;
		try {
			this.master = master;
			this.databaseName = databaseName;
			opening = new RocksDatabase(Path.of(master.getHome(), databaseName).toString());
			rocksDb = opening;
			rocksTables = rocksDb.getOrAddTable("tables");
			rocksSplitting = rocksDb.getOrAddTable("splitting");
			rocksSplittingAge = rocksDb.getOrAddTable("splittingAge");

			try (var it = rocksTables.iterator()) {
				it.seekToFirst();
				while (RocksDatabase.isValidOrThrow(it)) {
					var tableName = new String(it.key(), StandardCharsets.UTF_8);
					var bTable = new MasterTable.Data();
					var bb = ByteBuffer.Wrap(it.value());
					bTable.decode(bb);
					tables.put(tableName, bTable);
					it.next();
					logger.info("table: {}", bTable);
				}
			}

			try (var it = rocksSplitting.iterator()) {
				it.seekToFirst();
				while (RocksDatabase.isValidOrThrow(it)) {
					var tableName = new String(it.key(), StandardCharsets.UTF_8);
					var bTable = new MasterTable.Data();
					var bb = ByteBuffer.Wrap(it.value());
					bTable.decode(bb);
					splitting.put(tableName, bTable);
					it.next();
					logger.info("bucket: {}", bTable);
				}
			}
		} catch (Throwable ex) {
			// 半失败必须释放已打开的 RocksDB 句柄，避免目录锁泄漏阻塞后续同名库创建
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

	public String getDatabaseName() {
		return databaseName;
	}

	public @Nullable Data getTable(String tableName) {
		return tables.get(tableName);
	}

	public BBucketMeta.@Nullable Data locateBucket(String tableName, Binary key) {
		var bTable = getTable(tableName);
		if (null == bTable)
			return null;
		return bTable.locate(key);
	}

	public void close() {
		logger.info("closeDb: {}, {}", master.getHome(), databaseName);
		rocksDb.close();
	}

	public ConcurrentHashMap<String, MasterTable.Data> getTables() {
		return tables;
	}

	public @Nullable Data createTable(String tableName, OutObject<Boolean> outIsNew) throws Exception {
		outIsNew.value = false;
		while (true) {
			var table = tables.computeIfAbsent(tableName, __ -> new MasterTable.Data());
			table.lock();
			try {
				// created 也用于本次落盘的编码。持锁检查，确保写盘成功后才确认已有表。
				if (table.created) {
					logger.info("create table exist: {}.{}", databaseName, tableName);
					return table;
				}
				// 并发建表失败时会摘除旧空条目，持锁后须确认当前实例仍在 tables 中，失配则重试
				if (tables.get(tableName) != table)
					continue;

				outIsNew.value = true;

				var bucket = new BBucketMeta.Data();
				bucket.setDatabaseName(databaseName);
				bucket.setTableName(tableName);
				bucket.setKeyFirst(Binary.Empty);
				bucket.setKeyLast(Binary.Empty);

				// 分配首个桶服务并初始化表
				var managers = master.choiceManagers();
				if (managers.size() < master.getDbh2Config().getRaftClusterCount()) {
					logger.warn("managers.size({}) < raftClusterCount({})",
							managers.size(), master.getDbh2Config().getRaftClusterCount());
					// 摘除新建空条目，避免 GetBuckets 返回空桶快照
					tables.remove(tableName, table);
					return null;
				}

				// 须先建桶并初始化元数据成功后再 saveRocks，避免持久化未就绪的主表条目
				var raftNames = buildRaftConfig(bucket, managers);
				table.buckets.put(bucket.getKeyFirst(), bucket);
				try {
					createBucketRafts(managers, bucket, raftNames);
					setBucketMeta(bucket);
					table.created = true;
					saveRocks(rocksTables, tableName, table);
				} catch (Exception e) {
					// 失败回滚内存条目并通知各 manager 销毁已建 raft
					table.buckets.remove(bucket.getKeyFirst());
					table.created = false;
					tables.remove(tableName, table);
					destroyBucketRafts(managers, bucket, raftNames);
					throw e;
				}
			} finally {
				table.unlock();
			}
			logger.info("create table new: {}.{}", databaseName, tableName);
			return table;
		}
	}

	// Zeze.Dbh2.Master.SyncWrite=true 时为新建桶生成的 raft.xml 启用 SyncWrite（仅影响新建桶）
	private static final boolean SYNC_WRITE_RAFT = Boolean.parseBoolean(
			System.getProperty("Zeze.Dbh2.Master.SyncWrite", "false"));

	private ArrayList<String> buildRaftConfig(BBucketMeta.Data bucket, ArrayList<Master.Manager> managers) throws RocksDBException {
		bucket.getHost2Raft().clear();
		var sbRaft = new StringBuilder();
		sbRaft.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		sbRaft.append("\n");
		sbRaft.append("<raft Name=\"RaftName\"");
		if (SYNC_WRITE_RAFT)
			sbRaft.append(" SyncWrite=\"true\"");
		sbRaft.append(">\n");
		var raftNames = new ArrayList<String>(managers.size());
		for (var e : managers) {
			sbRaft.append("    <node Host=\"");
			sbRaft.append(e.data.getDbh2RaftAcceptorName()).append("\"");

			sbRaft.append(" Port=\"");
			var portId = master.nextBucketPortId(e.data.getDbh2RaftAcceptorName());
			sbRaft.append(portId).append("\"");

			sbRaft.append(" ProxyHost=\"").append(e.data.getDbh2RaftAcceptorName()).append("\"");
			sbRaft.append(" ProxyPort=\"").append(e.data.getPort()).append("\"");

			sbRaft.append("/>\n");
			var raftName = e.data.getDbh2RaftAcceptorName() + "_" + portId;
			raftNames.add(raftName);
			bucket.getHost2Raft().put(e.data.getDbh2RaftAcceptorName() + "_" + e.data.getPort(), raftName);
		}
		sbRaft.append("</raft>");
		bucket.setRaftConfig(sbRaft.toString());
		return raftNames;
	}

	private static void createBucketRafts(ArrayList<Master.Manager> managers,
										  BBucketMeta.Data bucket, ArrayList<String> raftNames) {
		var rpcs = new ArrayList<CreateBucket>();
		var futures = new ArrayList<TaskCompletionSource<?>>();
		var i = 0;
		for (var e : managers) {
			var r = new CreateBucket();
			r.Argument.assign(bucket);
			// 用于manager服务器的需要replace RaftName.
			//noinspection DynamicRegexReplaceableByCompiledPattern
			r.Argument.setRaftConfig(r.Argument.getRaftConfig().replaceAll("RaftName", raftNames.get(i++)));
			rpcs.add(r);
			futures.add(r.SendForWait(e.socket, 30_000));
		}
		for (var j = 0; j < futures.size(); ++j) {
			futures.get(j).await();
			// rpc失败不能静默：继续走saveRocks会让Master认为桶创建成功。
			var rc = rpcs.get(j).getResultCode();
			if (rc != 0)
				throw new RuntimeException("CreateBucket fail. manager=" + managers.get(j).data.getDbh2RaftAcceptorName()
						+ " rc=" + IModule.getErrorCode(rc));
		}
	}

	// 建桶半失败时尽力向各 manager 发送幂等 DestroyBucket，仅记日志不上抛以免掩盖原始异常
	private static void destroyBucketRafts(ArrayList<Master.Manager> managers,
										   BBucketMeta.Data bucket, ArrayList<String> raftNames) {
		var rpcs = new ArrayList<DestroyBucket>();
		var futures = new ArrayList<TaskCompletionSource<?>>();
		var i = 0;
		for (var e : managers) {
			var r = new DestroyBucket();
			r.Argument.assign(bucket);
			// 替换为该 manager 的 raftName
			//noinspection DynamicRegexReplaceableByCompiledPattern
			r.Argument.setRaftConfig(r.Argument.getRaftConfig().replaceAll("RaftName", raftNames.get(i++)));
			rpcs.add(r);
			futures.add(r.SendForWait(e.socket, 30_000));
		}
		for (var j = 0; j < futures.size(); ++j) {
			try {
				futures.get(j).await();
				var rc = rpcs.get(j).getResultCode();
				if (rc != 0)
					logger.error("DestroyBucket fail. manager={} rc={}",
							managers.get(j).data.getDbh2RaftAcceptorName(), IModule.getErrorCode(rc));
			} catch (Exception ex) {
				logger.error("DestroyBucket await fail. manager={}",
						managers.get(j).data.getDbh2RaftAcceptorName(), ex);
			}
		}
	}

	private static void setBucketMeta(BBucketMeta.Data bucket) throws Exception {
		// 第一条Dbh2桶协议，桶必须初始化以后才能使用。
		logger.info("setBucketMeta: new Dbh2Agent: {}", bucket.getRaftConfig());
		var agent = new Dbh2Agent(bucket.getRaftConfig());
		try {
			agent.setBucketMeta(bucket);
		} finally {
			agent.close();
		}
	}

	public long endMove(EndMove r) throws Exception {
		var to = r.Argument.getTo();
		var tableName = to.getTableName();
		var table = tables.get(tableName);
		if (null == table)
			return master.errorCode(Master.eTableNotFound);

		table.lock();
		try {
			var splitting = this.splitting.computeIfAbsent(tableName, __ -> new MasterTable.Data());
			return settleSplitting(table, splitting, tableName, to, null, r);
		} finally {
			table.unlock();
		}
	}

	public long endSplit(EndSplit r) throws Exception {
		var from = r.Argument.getFrom();
		var tableName = from.getTableName();
		var table = tables.get(tableName);
		if (null == table)
			return master.errorCode(Master.eTableNotFound);

		table.lock();
		try {
			var splitting = this.splitting.computeIfAbsent(tableName, __ -> new MasterTable.Data());
			return settleSplitting(table, splitting, tableName, r.Argument.getTo(), from, r);
		} finally {
			table.unlock();
		}
	}

	// endMove/endSplit 公共结算入口：校验 splitting 身份与主表单调收窄不变式，原子迁入主表并同步落盘。
	// 调用方须已持主表锁，锁序固定为主表→splitting。
	private long settleSplitting(MasterTable.Data table, MasterTable.Data splitting, String tableName,
								 BBucketMeta.Data to, BBucketMeta.@Nullable Data from, Rpc<?, ?> r) throws Exception {
		splitting.lock();
		try {
			var bucket = splitting.buckets.get(to.getKeyFirst());
			// 结算须匹配完整目标身份，防止旧目标迟到结算消费同区间重建的新目标
			if (bucket == null || !sameBucketMeta(bucket, to) || !bucket.getRaftConfig().equals(to.getRaftConfig()))
				return master.errorCode(Master.eSplittingBucketNotFound);

			var exist = table.buckets.get(to.getKeyFirst());
			// 主表已有全等条目说明本次迁移已结算过，返回终局码且不消费可能属于新世代的 splitting 条目
			if (null != exist && sameBucketMeta(exist, to) && exist.getRaftConfig().equals(to.getRaftConfig())) {
				logger.info("settleSplitting already settled, keep in-flight entry. to={}", to);
				return master.errorCode(Master.eSplittingBucketNotFound);
			}

			// endMove 迟到守卫：主表只会收窄；现存条目更窄属过期死信（消费并返回终局码），更宽属主表陈旧（返回可重试码待收敛）
			if (from == null && null != exist) {
				var cmp = compareKeyLast(exist.getKeyLast(), to.getKeyLast());
				if (cmp < 0) {
					logger.error("settleSplitting late endMove refused, dead entry consumed (INV1). exist={} to={}",
							exist, to);
					return consumeDeadSplitting(splitting, tableName, to.getKeyFirst());
				}
				if (cmp > 0) {
					logger.error("settleSplitting late endMove refused retryable, main stale-wide, keep entry. exist={} to={}",
							exist, to);
					return master.errorCode(Master.eSplittingStaleMain);
				}
			}

			// endSplit to 侧守卫：主表现存条目更窄说明该区间已被后续世代接管，拒绝并消费死信
			if (from != null && null != exist && compareKeyLast(exist.getKeyLast(), to.getKeyLast()) < 0) {
				logger.error("settleSplitting late endSplit refused, dead entry consumed (INV1). exist={} to={}",
						exist, to);
				return consumeDeadSplitting(splitting, tableName, to.getKeyFirst());
			}

			// 先改内存再落盘，若 commit 失败须按旧值精确还原内存，防止后续重试误判为已结算
			var settledSplitting = splitting.buckets.remove(to.getKeyFirst());
			var oldTo = table.buckets.put(to.getKeyFirst(), to);
			var fromReplaced = false;
			BBucketMeta.Data oldFrom = null;
			// 合法 split 满足 from.keyFirst < to.keyFirst；否则跳过 from 写入避免覆盖已发布的新桶
			if (from != null && from.getKeyFirst().compareTo(to.getKeyFirst()) < 0) {
				// 主表现存 from 条目更窄时跳过 from 覆盖，仅发布 to 以补齐目标区间
				var existFrom = table.buckets.get(from.getKeyFirst());
				if (null != existFrom && compareKeyLast(existFrom.getKeyLast(), from.getKeyLast()) < 0)
					logger.error("settleSplitting late from skipped (main narrower). existFrom={} from={}",
							existFrom, from);
				else {
					oldFrom = table.buckets.put(from.getKeyFirst(), from);
					fromReplaced = true;
				}
			} else if (from != null)
				logger.error("settleSplitting from.keyFirst>=to.keyFirst, skip from. from={} to={}", from, to);

			try (var batch = rocksDb.newBatch()) {
				var bbTable = table.encode();
				var bbSplitting = splitting.encode();
				var key = tableName.getBytes(StandardCharsets.UTF_8);
				rocksTables.put(batch, key, 0, key.length,
						bbTable.Bytes, bbTable.ReadIndex, bbTable.size());
				rocksSplitting.put(batch, key, 0, key.length,
						bbSplitting.Bytes, bbSplitting.ReadIndex, bbSplitting.size());
				batch.commit(RocksDatabase.getSyncWriteOptions());
			} catch (RocksDBException ex) {
				splitting.buckets.put(to.getKeyFirst(), settledSplitting);
				if (null != oldTo)
					table.buckets.put(to.getKeyFirst(), oldTo);
				else
					table.buckets.remove(to.getKeyFirst());
				if (fromReplaced) {
					if (null != oldFrom)
						table.buckets.put(from.getKeyFirst(), oldFrom);
					else
						table.buckets.remove(from.getKeyFirst());
				}
				throw ex;
			}
			splittingAgeRemoveQuietly(tableName, to.getKeyFirst());
			r.SendResult();
			return 0;
		} finally {
			splitting.unlock();
		}
	}

	// 消费死信 splitting 条目（调用方须已持主表锁与 splitting 锁），落盘失败还原内存
	private long consumeDeadSplitting(MasterTable.Data splitting, String tableName, Binary keyFirst)
			throws RocksDBException {
		var dead = splitting.buckets.remove(keyFirst);
		try {
			saveRocks(rocksSplitting, tableName, splitting);
		} catch (RocksDBException ex) {
			if (null != dead)
				splitting.buckets.put(keyFirst, dead);
			throw ex;
		}
		splittingAgeRemoveQuietly(tableName, keyFirst);
		return master.errorCode(Master.eSplittingBucketNotFound);
	}

	// 桶身份四元组等值判据
	private static boolean sameBucketMeta(BBucketMeta.Data a, BBucketMeta.Data b) {
		return a.getDatabaseName().equals(b.getDatabaseName())
				&& a.getTableName().equals(b.getTableName())
				&& a.getKeyFirst().equals(b.getKeyFirst())
				&& a.getKeyLast().equals(b.getKeyLast());
	}

	// keyLast 比较序：Binary.Empty 表示 +∞，其余按字典序
	private static int compareKeyLast(Binary a, Binary b) {
		if (a.size() == 0)
			return b.size() == 0 ? 0 : 1;
		if (b.size() == 0)
			return -1;
		return a.compareTo(b);
	}

	public long createSplitBucket(CreateSplitBucket r) throws Exception {
		var bucket = r.Argument;
		String tableName = bucket.getTableName();
		var mainTable = tables.get(tableName);
		if (null == mainTable) {
			logger.error("createBucket but table not found. database={} table={}", databaseName, tableName);
			return master.errorCode(Master.eTableNotFound);
		}

		var table = splitting.computeIfAbsent(tableName, __ -> new MasterTable.Data());
		BBucketMeta.Data created = null;
		while (true) {
			var resume = false;
			table.lock();
			try {
				var exist = table.buckets.get(bucket.getKeyFirst());
				if (exist == null) {
					// 分配分桶目标并落盘
					var managers = master.choiceSmallLoadManagers();
					if (managers.size() < master.getDbh2Config().getRaftClusterCount()) {
						logger.info("too few small load manager. database={} table={}", databaseName, tableName);
						return master.errorCode(Master.eTooFewManager);
					}

					var raftNames = buildRaftConfig(bucket, managers);
					table.buckets.put(bucket.getKeyFirst(), bucket);
					try {
						createBucketRafts(managers, bucket, raftNames);
						saveRocks(rocksSplitting, tableName, table);
					} catch (Exception e) {
						// 失败回滚内存条目并销毁刚建的 raft
						table.buckets.remove(bucket.getKeyFirst());
						destroyBucketRafts(managers, bucket, raftNames);
						throw e;
					}
					// 先立时间戳基线，出 splitting 锁后再按主表→splitting 锁序补写源桶世代
					splittingAgeWriteQuietly(tableName, bucket.getKeyFirst(), null);

					r.Result = bucket;
					r.SendResult();
					created = bucket;
					break;
				}
				if (sameBucketMeta(exist, bucket)) {
					// 同边界也可能属于已收窄的旧源桶，放锁后按主表→splitting 复查世代
					resume = true;
				}
				// 同 keyFirst 四元组不等时放锁后按主表→splitting 锁序两阶段复查死信
			} finally {
				table.unlock();
			}
			if (resume) {
				mainTable.lock();
				try {
					table.lock();
					try {
						var exist = table.buckets.get(bucket.getKeyFirst());
						if (exist == null || !sameBucketMeta(exist, bucket))
							continue; // 放锁期间已被消费或重建，交回主循环处理
						var age = readSplittingAge(tableName, exist.getKeyFirst());
						if (isDeadSplittingEntry(mainTable, exist, age != null ? age.sourceRaft() : null)) {
							logger.warn("bucket exist but source generation is obsolete, await recycling."
									+ " database={} table={}", databaseName, tableName);
							return master.errorCode(Master.eSplittingBucketExist);
						}
						// 同源在途条目的幂等重试返回原目标
						logger.info("bucket exist, resume. database={} table={}", databaseName, tableName);
						r.Result = exist;
						r.SendResult();
						return 0;
					} finally {
						table.unlock();
					}
				} finally {
					mainTable.unlock();
				}
			}
			if (!consumeDeadSplittingOnCollision(tableName, mainTable, table, bucket)) {
				logger.info("bucket exist but mismatch (in-flight). database={} table={}", databaseName, tableName);
				return master.errorCode(Master.eSplittingBucketExist);
			}
		}
		registerSplittingGeneration(tableName, created);
		return 0;
	}

	/**
	 * 碰撞条目的死信判定与消费（锁序固定主表→splitting）：当主表 floor 区间比条目更窄时，
	 * 说明条目已被更晚的结算越过成为死信，删除落盘后返回 true 供主循环重查；否则返回 false。
	 */
	private boolean consumeDeadSplittingOnCollision(String tableName, MasterTable.Data mainTable,
													MasterTable.Data splittingTable, BBucketMeta.Data request)
			throws RocksDBException {
		mainTable.lock();
		try {
			splittingTable.lock();
			try {
				var exist = splittingTable.buckets.get(request.getKeyFirst());
				if (null == exist || sameBucketMeta(exist, request))
					return true;
				var floor = mainTable.buckets.floorEntry(request.getKeyFirst());
				if (null == floor || compareKeyLast(floor.getValue().getKeyLast(), exist.getKeyLast()) >= 0)
					return false;
				logger.error("dead splitting entry consumed on collision (INV1): database={} table={} entry={}",
						databaseName, tableName, exist);
				var dead = splittingTable.buckets.remove(request.getKeyFirst());
				try {
					saveRocks(rocksSplitting, tableName, splittingTable);
				} catch (RocksDBException ex) {
					if (null != dead)
						splittingTable.buckets.put(request.getKeyFirst(), dead);
					throw ex;
				}
				splittingAgeRemoveQuietly(tableName, request.getKeyFirst());
				return true;
			} finally {
				splittingTable.unlock();
			}
		} finally {
			mainTable.unlock();
		}
	}

	// 年龄副表 key：4 字节大端 tableName 长度 + tableName + keyFirst
	private static byte[] splittingAgeKey(String tableName, Binary keyFirst) {
		var tn = tableName.getBytes(StandardCharsets.UTF_8);
		var key = new byte[4 + tn.length + keyFirst.size()];
		key[0] = (byte)(tn.length >>> 24);
		key[1] = (byte)(tn.length >>> 16);
		key[2] = (byte)(tn.length >>> 8);
		key[3] = (byte)tn.length;
		System.arraycopy(tn, 0, key, 4, tn.length);
		System.arraycopy(keyFirst.bytesUnsafe(), keyFirst.getOffset(), key, 4 + tn.length, keyFirst.size());
		return key;
	}

	// 写入年龄/世代副表（失败仅记日志不阻断主路径）；sourceRaft 为 null 时仅写时间戳基线
	private void splittingAgeWriteQuietly(String tableName, Binary keyFirst, @Nullable String sourceRaft) {
		try {
			var key = splittingAgeKey(tableName, keyFirst);
			var bb = ByteBuffer.Allocate(16 + (null != sourceRaft ? sourceRaft.length() * 2 : 0));
			bb.WriteLong(System.currentTimeMillis());
			if (null != sourceRaft)
				bb.WriteString(sourceRaft);
			rocksSplittingAge.put(key, 0, key.length, bb.Bytes, 0, bb.WriteIndex);
		} catch (RocksDBException ex) {
			logger.error("splittingAgeWrite fail (observation only). database={} table={}", databaseName, tableName, ex);
		}
	}

	private void splittingAgeRemoveQuietly(String tableName, Binary keyFirst) {
		try {
			var key = splittingAgeKey(tableName, keyFirst);
			rocksSplittingAge.delete(key, 0, key.length);
		} catch (RocksDBException ex) {
			logger.error("splittingAgeRemove fail (observation only). database={} table={}", databaseName, tableName, ex);
		}
	}

	/** 返回条目创建时间戳（毫秒）；无记录返回 null。 */
	public @Nullable Long getSplittingAgeCreateTime(String tableName, Binary keyFirst) {
		var rec = readSplittingAge(tableName, keyFirst);
		return null != rec ? rec.createTimeMs() : null;
	}

	// 年龄与世代记录：[时间戳][可选源桶 raft 身份]
	private record AgeRecord(long createTimeMs, @Nullable String sourceRaft) {
	}

	private @Nullable AgeRecord readSplittingAge(String tableName, Binary keyFirst) {
		try {
			var value = rocksSplittingAge.get(splittingAgeKey(tableName, keyFirst));
			if (null == value)
				return null;
			var bb = ByteBuffer.Wrap(value);
			var ts = bb.ReadLong();
			return bb.size() > 0 ? new AgeRecord(ts, bb.ReadString()) : new AgeRecord(ts, null);
		} catch (RocksDBException ex) {
			logger.error("readSplittingAge fail. database={} table={}", databaseName, tableName, ex);
			return null;
		}
	}

	// 解析桶的 raft 集群身份（sortedNames）；解析失败返回 null
	private static @Nullable String raftIdentityOf(BBucketMeta.Data bucket) {
		try {
			return RaftConfig.loadFromString(bucket.getRaftConfig()).getSortedNames();
		} catch (Exception e) {
			return null;
		}
	}

	// 判断区间包含关系（inner ⊆ outer），keyLast 为空表示 +∞
	private static boolean rangeContains(Binary outerFirst, Binary outerLast, Binary innerFirst, Binary innerLast) {
		if (innerFirst.compareTo(outerFirst) < 0)
			return false;
		if (outerLast.size() == 0)
			return true;
		return innerLast.size() > 0 && innerLast.compareTo(outerLast) <= 0;
	}

	// 按主表→splitting 锁序为 splitting 条目补登记源桶 raft 身份（必须在 splitting 锁外调用）
	private void registerSplittingGeneration(String tableName, BBucketMeta.Data entry) {
		var mainTable = tables.get(tableName);
		var splittingTable = splitting.get(tableName);
		if (mainTable == null || splittingTable == null)
			return;
		mainTable.lock();
		try {
			splittingTable.lock();
			try {
				// 防止迟到登记覆写同区间已重建的新目标
				var current = splittingTable.buckets.get(entry.getKeyFirst());
				if (current == null || !sameBucketMeta(current, entry)
						|| !current.getRaftConfig().equals(entry.getRaftConfig()))
					return;
				var floor = mainTable.buckets.floorEntry(entry.getKeyFirst());
				if (null != floor && rangeContains(floor.getValue().getKeyFirst(), floor.getValue().getKeyLast(),
						entry.getKeyFirst(), entry.getKeyLast())) {
					var source = raftIdentityOf(floor.getValue());
					if (source != null)
						splittingAgeWriteQuietly(tableName, entry.getKeyFirst(), source);
				}
			} finally {
				splittingTable.unlock();
			}
		} finally {
			mainTable.unlock();
		}
	}

	// 结构判死（调用方须已持主表锁）：有源桶世代时要求源桶仍在主表且覆盖条目区间，无世代时退回主表 floor 收窄判据
	private boolean isDeadSplittingEntry(MasterTable.Data mainTable, BBucketMeta.Data entry,
										 @Nullable String sourceRaft) {
		if (null != sourceRaft) {
			BBucketMeta.Data source = null;
			for (var b : mainTable.buckets.values())
				if (sourceRaft.equals(raftIdentityOf(b))) {
					source = b;
					break;
				}
			return null == source || !rangeContains(source.getKeyFirst(), source.getKeyLast(),
					entry.getKeyFirst(), entry.getKeyLast());
		}
		var floor = mainTable.buckets.floorEntry(entry.getKeyFirst());
		return null != floor && compareKeyLast(floor.getValue().getKeyLast(), entry.getKeyLast()) < 0;
	}

	// 向孤儿 splitting 条目的各 manager 发送幂等 DestroyBucket（调用方须持主表→splitting 锁）；任一未确认则返回 false 留待下轮重试
	private boolean destroyOrphanRafts(String tableName, BBucketMeta.Data dead) {
		var allAcked = true;
		for (var e : dead.getHost2Raft().entrySet()) {
			var manager = master.findManagerByIdentity(e.getKey());
			if (null == manager) {
				logger.warn("orphan splitting raft recycle: manager not registered, keep entry for retry."
								+ " database={} table={} manager={} raftName={}",
						databaseName, tableName, e.getKey(), e.getValue());
				allAcked = false;
				continue;
			}
			var r = new DestroyBucket();
			r.Argument.assign(dead);
			r.Argument.setRaftConfig(dead.getRaftConfig().replace("RaftName", e.getValue()));
			try {
				r.SendForWait(manager.socket, 30_000).await();
				if (r.getResultCode() != 0) {
					logger.error("orphan splitting raft recycle: DestroyBucket fail. database={} table={}"
									+ " manager={} raftName={} rc={}",
							databaseName, tableName, e.getKey(), e.getValue(), IModule.getErrorCode(r.getResultCode()));
					allAcked = false;
				}
			} catch (Exception ex) {
				logger.error("orphan splitting raft recycle: DestroyBucket await fail. database={} table={}"
								+ " manager={} raftName={}",
						databaseName, tableName, e.getKey(), e.getValue(), ex);
				allAcked = false;
			}
		}
		return allAcked;
	}

	private record AgedEntry(BBucketMeta.Data entry, long ageMs, @Nullable String sourceRaft) {
	}

	private record MissingBaseline(String tableName, BBucketMeta.Data entry) {
	}

	// 按主表→splitting 锁序二次确认条目仍存在且结构判死后，销毁孤儿 raft 并摘除 splitting 记录
	private boolean recycleDeadSplitting(String tableName, AgedEntry a) {
		var mainTable = tables.get(tableName);
		if (null == mainTable)
			return false;
		var splittingTable = splitting.get(tableName);
		if (splittingTable == null)
			return true;
		mainTable.lock();
		try {
			splittingTable.lock();
			try {
				var exist = splittingTable.buckets.get(a.entry().getKeyFirst());
				if (null == exist || !sameBucketMeta(exist, a.entry())
						|| !exist.getRaftConfig().equals(a.entry().getRaftConfig()))
					return true;
				if (!isDeadSplittingEntry(mainTable, exist, a.sourceRaft()))
					return false;
				logger.error("splitting entry aged ({}ms >= {}ms) and structurally dead, recycle. database={} table={} entry={}",
						a.ageMs(), master.getDbh2Config().getSplittingAgeWarnMs(), databaseName, tableName, exist);
				if (!destroyOrphanRafts(tableName, exist))
					return false;
				try {
					splittingTable.buckets.remove(a.entry().getKeyFirst());
					saveRocks(rocksSplitting, tableName, splittingTable);
				} catch (RocksDBException ex) {
					splittingTable.buckets.put(a.entry().getKeyFirst(), exist);
					logger.error("recycleDeadSplitting saveRocks fail, keep entry for retry."
							+ " database={} table={}", databaseName, tableName, ex);
					return false;
				}
				splittingAgeRemoveQuietly(tableName, exist.getKeyFirst());
				return true;
			} finally {
				splittingTable.unlock();
			}
		} finally {
			mainTable.unlock();
		}
	}

	/**
	 * 扫描 splitting 条目年龄：超龄且结构判死的回收孤儿 raft 并删除条目，超龄仍存活的仅告警，
	 * 无时间戳的存量条目建立基线并补登记世代。返回本次发现的超龄条目数。
	 */
	public int scanSplittingAge() {
		var now = System.currentTimeMillis();
		var warnMs = master.getDbh2Config().getSplittingAgeWarnMs();
		var aged = new ArrayList<AgedEntry>();
		var baselines = new ArrayList<MissingBaseline>();
		for (var e : splitting.entrySet()) {
			var tableName = e.getKey();
			var splittingTable = e.getValue();
			splittingTable.lock();
			try {
				for (var b : splittingTable.buckets.entrySet()) {
					var keyFirst = b.getKey();
					var rec = readSplittingAge(tableName, keyFirst);
					if (null == rec) {
						splittingAgeWriteQuietly(tableName, keyFirst, null);
						baselines.add(new MissingBaseline(tableName, b.getValue().copy()));
						continue;
					}
					var ageMs = now - rec.createTimeMs();
					if (ageMs >= warnMs)
						aged.add(new AgedEntry(b.getValue().copy(), ageMs, rec.sourceRaft()));
				}
			} finally {
				splittingTable.unlock();
			}
		}
		for (var m : baselines)
			registerSplittingGeneration(m.tableName(), m.entry());
		for (var a : aged) {
			if (recycleDeadSplitting(a.entry().getTableName(), a))
				continue;
			BBucketMeta.Data source = null;
			var mainTable = tables.get(a.entry().getTableName());
			if (null != mainTable) {
				mainTable.lock();
				try {
					var floor = mainTable.buckets.floorEntry(a.entry().getKeyFirst());
					source = null != floor ? floor.getValue() : null;
				} finally {
					mainTable.unlock();
				}
			}
			logger.error("splitting entry aged ({}ms >= {}ms), entry={} source={}",
					a.ageMs(), warnMs, a.entry(), source);
		}
		return aged.size();
	}

	private static void saveRocks(RocksDatabase.Table rocksTable,
								  String tableName, MasterTable.Data table) throws RocksDBException {
		// master数据马上存数据库。
		var bbValue = table.encode();
		var key = tableName.getBytes(StandardCharsets.UTF_8);
		// 建表/分裂目标在应答后即被外部使用，必须与结算批次一样同步 WAL。
		rocksTable.put(RocksDatabase.getSyncWriteOptions(), key, 0, key.length, bbValue.Bytes, 0, bbValue.WriteIndex);
	}
}
