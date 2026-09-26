package Zeze.Dbh2.Master;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutObject;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

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
	private final Master master;

	public MasterDatabase(Master master, String databaseName) {
		try {
			this.master = master;
			this.databaseName = databaseName;
			rocksDb = new RocksDatabase(Path.of(master.getHome(), databaseName).toString());
			rocksTables = rocksDb.getOrAddTable("tables");
			rocksSplitting = rocksDb.getOrAddTable("splitting");

			try (var it = rocksTables.iterator()) {
				it.seekToFirst();
				while (it.isValid()) {
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
				while (it.isValid()) {
					var tableName = new String(it.key(), StandardCharsets.UTF_8);
					var bTable = new MasterTable.Data();
					var bb = ByteBuffer.Wrap(it.value());
					bTable.decode(bb);
					splitting.put(tableName, bTable);
					it.next();
					logger.info("bucket: {}", bTable);
				}
			}
		} catch (RocksDBException ex) {
			throw new RuntimeException(ex);
		}
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public MasterTable.Data getTable(String tableName) {
		return tables.get(tableName);
	}

	public BBucketMeta.Data locateBucket(String tableName, Binary key) {
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

	public MasterTable.Data createTable(String tableName, OutObject<Boolean> outIsNew) throws Exception {
		outIsNew.value = false;
		var table = tables.computeIfAbsent(tableName, __ -> new MasterTable.Data());
		if (table.created) {
			logger.info("create table exist: {}.{}", databaseName, tableName);
			return table;
		}

		table.lock();
		try {
			// 加锁后再次检查一次。
			if (table.created) {
				logger.info("create table exist: {}.{}", databaseName, tableName);
				return table;
			}

			outIsNew.value = true;

			var bucket = new BBucketMeta.Data();
			bucket.setDatabaseName(databaseName);
			bucket.setTableName(tableName);
			bucket.setKeyFirst(Binary.Empty);
			bucket.setKeyLast(Binary.Empty);

			// allocate first bucket service and setup table
			var managers = master.choiceManagers();
			if (managers.size() < master.getDbh2Config().getRaftClusterCount()) {
				logger.warn("managers.size({}) < raftClusterCount({})",
						managers.size(), master.getDbh2Config().getRaftClusterCount());
				return null;
			}

			var raftNames = buildRaftConfig(bucket, managers);
			table.buckets.put(bucket.getKeyFirst(), bucket);
			try {
				createBucketRafts(managers, bucket, raftNames);
				setBucketMeta(bucket);
				table.created = true;
				saveRocks(rocksTables, tableName, table);
			} catch (Exception e) {
				// 失败回滚，否则半初始化bucket残留在内存表中。
				table.buckets.remove(bucket.getKeyFirst());
				table.created = false;
				throw e;
			}
		} finally {
			table.unlock();
		}
		logger.info("create table new: {}.{}", databaseName, tableName);
		return table;
	}

	// 构建raft-config，基本的用于客户端，用于manager服务器的需要replace RaftName.
	private ArrayList<String> buildRaftConfig(BBucketMeta.Data bucket, ArrayList<Master.Manager> managers) throws RocksDBException {
		var sbRaft = new StringBuilder();
		sbRaft.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		sbRaft.append("\n");
		sbRaft.append("<raft Name=\"RaftName\">\n");
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
		//System.out.println(bucket.getRaftConfig());
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
			//System.out.println(r.Argument.getRaftConfig());
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

	// 确认splitting桶与决策一致后原子搬迁（endMove/endSplit的公共主体）：新桶移出splitting、
	// 入主表，from非null时替换主表源桶（endSplit分裂后源桶边界收窄；endMove传null），
	// 双表同批落库。splitting的TreeMap必须持它自己的锁访问：createSplitBucket持splitting锁
	// get/put，只持主表锁remove/encode的话两锁互不互斥，TreeMap并发读写可CME/死循环。
	// 锁序固定主表→splitting（createSplitBucket只取splitting锁，无环）。
	// 调用方须已持主表锁；新桶不在splitting或四元组不等值时不动任何状态，返回错误码。
	private long settleSplitting(MasterTable.Data table, MasterTable.Data splitting, String tableName,
								 BBucketMeta.Data to, BBucketMeta.Data from, Rpc<?, ?> r) throws Exception {
		splitting.lock();
		try {
			var bucket = splitting.buckets.get(to.getKeyFirst());
			if (bucket == null || !sameBucketMeta(bucket, to))
				return master.errorCode(Master.eSplittingBucketNotFound);

			splitting.buckets.remove(to.getKeyFirst());
			table.buckets.put(to.getKeyFirst(), to);
			if (from != null)
				table.buckets.put(from.getKeyFirst(), from); // replace

			try (var batch = rocksDb.newBatch()) {
				var bbTable = table.encode();
				var bbSplitting = splitting.encode();
				var key = tableName.getBytes(StandardCharsets.UTF_8);
				rocksTables.put(batch, key, 0, key.length,
						bbTable.Bytes, bbTable.ReadIndex, bbTable.size());
				rocksSplitting.put(batch, key, 0, key.length,
						bbSplitting.Bytes, bbSplitting.ReadIndex, bbSplitting.size());
				batch.commit();
			}
			r.SendResult();
			return 0;
		} finally {
			splitting.unlock();
		}
	}

	// 桶身份四元组等值：endMove/endSplit确认splitting桶与决策一致、createSplitBucket
	// 幂等重试识别同一桶用的同一判据。
	private static boolean sameBucketMeta(BBucketMeta.Data a, BBucketMeta.Data b) {
		return a.getDatabaseName().equals(b.getDatabaseName())
				&& a.getTableName().equals(b.getTableName())
				&& a.getKeyFirst().equals(b.getKeyFirst())
				&& a.getKeyLast().equals(b.getKeyLast());
	}

	public long createSplitBucket(CreateSplitBucket r) throws Exception {
		var bucket = r.Argument;
		String tableName = bucket.getTableName();
		if (null == tables.get(tableName)) {
			logger.error("createBucket but table not found. database={} table={}", databaseName, tableName);
			return master.errorCode(Master.eTableNotFound);
		}

		var table = splitting.computeIfAbsent(tableName, __ -> new MasterTable.Data());
		table.lock();
		try {
			var exist = table.buckets.get(bucket.getKeyFirst());
			if (exist != null) {
				// 桶已经存在。响应丢失/日志截断后manager重试时走这里：必须把已存在的桶
				// 幂等返回（对齐createTable"存在即返回"），否则eSplittingBucketExist让
				// agent端抛异常，源桶splittingMeta为null永远到不了endSplit，分桶永久卡死。
				if (sameBucketMeta(exist, bucket)) {
					logger.info("bucket exist, resume. database={} table={}", databaseName, tableName);
					r.Result = exist;
					r.SendResult();
					return 0;
				}
				logger.info("bucket exist but mismatch. database={} table={}", databaseName, tableName);
				return master.errorCode(Master.eSplittingBucketExist);
			}

			// allocate first bucket service and setup table
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
				// 失败回滚内存表，否则脏entry让之后所有重试被eSplittingBucketExist拒绝，分桶卡死直到Master重启。
				table.buckets.remove(bucket.getKeyFirst());
				throw e;
			}

			r.Result = bucket;
			r.SendResult();
			return 0;
		} finally {
			table.unlock();
		}
	}

	private static void saveRocks(RocksDatabase.Table rocksTable,
								  String tableName, MasterTable.Data table) throws RocksDBException {
		// master数据马上存数据库。
		var bbValue = table.encode();
		var key = tableName.getBytes(StandardCharsets.UTF_8);
		rocksTable.put(key, 0, key.length, bbValue.Bytes, 0, bbValue.WriteIndex);
	}
}
