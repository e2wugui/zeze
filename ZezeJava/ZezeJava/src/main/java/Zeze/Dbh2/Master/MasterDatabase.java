package Zeze.Dbh2.Master;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
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

	// bucket1 分桶的前半部分，实际是新创建的拷贝目标。
	// bucket2 分桶的后半部分，实际是保留的一半。
	public long endSplit(BBucketMeta.Data bucketNew, BBucketMeta.Data bucketRemain) throws Exception {
		var tableName = bucketNew.getTableName();
		var table = tables.get(tableName);
		if (null == table)
			return master.errorCode(Master.eTableNotFound);

		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (table) {
			var splitting = this.splitting.computeIfAbsent(tableName, __ -> new MasterTable.Data());
			var splittingBucket = table.buckets.get(bucketNew.getKeyFirst());
			if (splittingBucket != null
					&& splittingBucket.getDatabaseName().equals(bucketNew.getDatabaseName())
					&& splittingBucket.getTableName().equals(bucketNew.getTableName())
					&& splittingBucket.getKeyFirst().equals(bucketNew.getKeyFirst())
				// 进行中的分桶的KeyLast不是真正的，而是一个magic值，不需要比较。
			) {
				// set bucket meta, prepare to public.
				setBucketMeta(bucketNew); // 新桶初始化。
				splitting.buckets.remove(bucketNew.getKeyFirst());
				// 保留桶的信息不在这里，不需要remove。

				// todo 确认结束分桶的流程，保留下来的桶信息是否由源manager自己完成，
				//  这里的流程实际上是分桶的源manager发起的。
				//  setBucketMeta(bucketRemain); // 保留桶修改Meta。

				// 下面这两步修改了以后，外面就能看到了。
				// 最好是保存以后外面才能看到，
				// 一般没问题先这样了。
				// 【修订方法】复制一份完整的拷贝，修改拷贝，保存，把拷贝引用替换过去，但这里有点问题，这个容器是concurrent的，
				// 本来就是为了能多线程共享，现在又变成需要加锁了。
				table.buckets.put(bucketRemain.getKeyFirst(), bucketRemain); // 这里反而是新的项。
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
				return 0;
			}
		}
		return master.errorCode(Master.eSplittingBucketNotFound);
	}

	public BBucketMeta.Data createBucket(String tableName, Binary first, Binary last) throws Exception {
		if (null == tables.get(tableName))
			return null; // table not exist.

		var table = splitting.computeIfAbsent(tableName, __ -> new MasterTable.Data());
		{
			// 桶已经存在，一般是分桶中断以后又重新开始，此时继续使用原来创建的桶。
			// 【需要重置】
			// 分桶过程中，如果分过去的记录有被删除，中断时，很难可靠的保证删除的记录被同步，
			// 最简单的办法就是中断以后的分桶重新开始，此时重置目标桶即可。
			// 【todo】
			// 要实现中断以后的分桶能断点续传，需要记录（持久化）拷贝中的key，
			// 并且如果已拷贝记录发生增删的事务也需要持久化，
			// 所以不是很容易。
			var bucket = table.locate(first);
			// 需要精确判断。
			if (null != bucket && bucket.getKeyFirst().equals(first) && bucket.getKeyLast().equals(last)) {
				var agent = new Dbh2Agent(bucket.getRaftConfig());
				try {
					//agent.resetBucket(bucket);
				} finally {
					agent.close();
				}
				return bucket;
			}
			// 下面将创建新的桶。
		}
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (table) {
			var bucket = new BBucketMeta.Data();
			bucket.setDatabaseName(databaseName);
			bucket.setTableName(tableName);
			bucket.setKeyFirst(first);
			bucket.setKeyLast(last);
			table.buckets.put(bucket.getKeyFirst(), bucket);

			// allocate first bucket service and setup table
			var managers = master.choiceSmallLoadManagers();
			if (managers.size() < 3)
				return null;

			var raftNames = buildRaftConfig(bucket, managers);
			createBucketRafts(managers, bucket, raftNames);
			saveRocks(rocksSplitting, tableName, table);
			return bucket;
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
