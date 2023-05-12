package Zeze.Dbh2.Master;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Builtin.Dbh2.Master.PublishSplitBucketNew;
import Zeze.Builtin.Dbh2.Master.PublishSplitBucketOld;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Net.Binary;
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

	public MasterTable.Data createTable(String tableName, OutObject<Boolean> outIsNew) throws Exception {
		outIsNew.value = false;
		var table = tables.computeIfAbsent(tableName, __ -> new MasterTable.Data());
		if (table.created)
			return table;

		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (table) {
			// 加锁后再次检查一次。
			if (table.created)
				return table;

			outIsNew.value = true;

			var bucket = new BBucketMeta.Data();
			bucket.setDatabaseName(databaseName);
			bucket.setTableName(tableName);
			bucket.setKeyFirst(Binary.Empty);
			bucket.setKeyLast(Binary.Empty);
			table.buckets.put(bucket.getKeyFirst(), bucket);

			// allocate first bucket service and setup table
			var managers = master.choiceManagers();
			if (managers.size() < 3)
				return null;

			var raftNames = buildRaftConfig(bucket, managers);
			createBucketRafts(managers, bucket, raftNames);
			setBucketMeta(bucket);
			table.created = true;
			saveRocks(rocksTables, tableName, table);
		}
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
			sbRaft.append(e.data.getDbh2RaftAcceptorName());
			sbRaft.append("\" Port=\"");
			var portId = master.nextBucketPortId(e.data.getDbh2RaftAcceptorName());
			sbRaft.append(portId);
			sbRaft.append("\"/>\n");
			raftNames.add(e.data.getDbh2RaftAcceptorName() + ":" + portId);
		}
		sbRaft.append("</raft>");
		bucket.setRaftConfig(sbRaft.toString());
		//System.out.println(bucket.getRaftConfig());
		return raftNames;
	}

	private static void createBucketRafts(ArrayList<Master.Manager> managers, BBucketMeta.Data bucket, ArrayList<String> raftNames) {
		var futures = new ArrayList<TaskCompletionSource<?>>();
		var i = 0;
		for (var e : managers) {
			var r = new CreateBucket();
			r.Argument.assign(bucket);
			// 用于manager服务器的需要replace RaftName.
			r.Argument.setRaftConfig(r.Argument.getRaftConfig().replaceAll("RaftName", raftNames.get(i++)));
			//System.out.println(r.Argument.getRaftConfig());
			futures.add(r.SendForWait(e.socket));
		}
		for (var future : futures)
			future.await();
	}

	private static void setBucketMeta(BBucketMeta.Data bucket) throws Exception {
		// 第一条Dbh2桶协议，桶必须初始化以后才能使用。
		var agent = new Dbh2Agent(bucket.getRaftConfig());
		try {
			agent.setBucketMeta(bucket);
		} finally {
			agent.close();
		}
	}

	public long publishSplitBucketOld(PublishSplitBucketOld r) throws Exception {
		var bucketOld = r.Argument;
		var tableName = bucketOld.getTableName();
		var table = tables.get(tableName);
		if (null == table)
			return master.errorCode(Master.eTableNotFound);

		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (table) {
			table.buckets.put(bucketOld.getKeyFirst(), bucketOld);
		}
		r.SendResult();
		return 0;
	}

	public long publishSplitBucketNew(PublishSplitBucketNew r) throws Exception {
		var bucketNew = r.Argument;
		var tableName = bucketNew.getTableName();
		var table = tables.get(tableName);
		if (null == table)
			return master.errorCode(Master.eTableNotFound);

		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (table) {
			var splitting = this.splitting.computeIfAbsent(tableName, __ -> new MasterTable.Data());
			var bucket = splitting.buckets.get(bucketNew.getKeyFirst());
			if (bucket != null
					&& bucket.getDatabaseName().equals(bucketNew.getDatabaseName())
					&& bucket.getTableName().equals(bucketNew.getTableName())
					&& bucket.getKeyFirst().equals(bucketNew.getKeyFirst())
					&& bucket.getKeyLast().equals(bucketNew.getKeyLast())
			) {
				splitting.buckets.remove(bucketNew.getKeyFirst());
				table.buckets.put(bucketNew.getKeyFirst(), bucketNew); // 这里实际上是replace。
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
			}
		}
		return master.errorCode(Master.eSplittingBucketNotFound);
	}

	public long createSplitBucket(CreateSplitBucket r) throws Exception {
		var bucket = r.Argument;
		String tableName = bucket.getTableName();
		if (null == tables.get(tableName)) {
			logger.error("createBucket but table not found. database=" + databaseName + " table=" + tableName);
			return master.errorCode(Master.eTableNotFound);
		}

		var table = splitting.computeIfAbsent(tableName, __ -> new MasterTable.Data());
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (table) {
			if (table.buckets.get(bucket.getKeyFirst()) != null) {
				// 桶已经存在，断点续传由manager自己负责，不能重复创建桶。
				logger.info("bucket exist. database=" + databaseName + " table=" + tableName);
				return master.errorCode(Master.eSplittingBucketExist);
			}

			table.buckets.put(bucket.getKeyFirst(), bucket);

			// allocate first bucket service and setup table
			var managers = master.choiceSmallLoadManagers();
			if (managers.size() < 3) {
				logger.info("too few small load manager. database=" + databaseName + " table=" + tableName);
				return master.errorCode(Master.eTooFewManager);
			}

			var raftNames = buildRaftConfig(bucket, managers);
			createBucketRafts(managers, bucket, raftNames);
			saveRocks(rocksSplitting, tableName, table);

			r.Result = bucket;
			r.SendResult();
			return 0;
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
