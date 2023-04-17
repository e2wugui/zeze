package Zeze.Dbh2.Master;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutObject;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class MasterDatabase {
	private static final Logger logger = LogManager.getLogger(MasterDatabase.class);
	private final String databaseName;
	private final ConcurrentHashMap<String, MasterTable.Data> tables = new ConcurrentHashMap<>();
	private final RocksDB db;
	private final Master master;

	public MasterDatabase(Master master, String databaseName) {
		try {
			this.master = master;
			this.databaseName = databaseName;
			this.db = RocksDatabase.open(RocksDatabase.getCommonOptions(),
					Path.of(master.getHome(), databaseName).toString());

			try (var it = this.db.newIterator(RocksDatabase.getDefaultReadOptions())) {
				it.seekToFirst();
				while (it.isValid()) {
					var tableName = new String(it.key(), StandardCharsets.UTF_8);
					var bTable = new MasterTable.Data();
					var bb = ByteBuffer.Wrap(it.value());
					bTable.decode(bb);
					tables.put(tableName, bTable);
					logger.info("addTable: {}", tableName);
					it.next();
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
		db.close();
	}

	public MasterTable.Data createTable(String tableName, OutObject<Boolean> outIsNew) throws Exception {
		outIsNew.value = false;
		var table = this.tables.computeIfAbsent(tableName, __ -> new MasterTable.Data());
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
			bucket.setMoving(false);
			bucket.setKeyFirst(Binary.Empty);
			bucket.setKeyLast(Binary.Empty);
			table.buckets.put(bucket.getKeyFirst(), bucket);

			// allocate first bucket service and setup table

			var managers = master.choiceManagers();
			if (managers.size() < 3)
				return null;
			// 构建raft-config，基本的用于客户端，用于manager服务器的需要replace RaftName.
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

			// 第一条Dbh2桶协议，桶必须初始化以后才能使用。
			var raftConf = RaftConfig.loadFromString(bucket.getRaftConfig());
			var agent = new Dbh2Agent(raftConf);
			try {
				agent.setBucketMeta(bucket);
			} finally {
				agent.close();
			}

			table.created = true;

			// master数据马上存数据库。
			var bbValue = ByteBuffer.Allocate();
			table.encode(bbValue);
			var key = tableName.getBytes(StandardCharsets.UTF_8);
			this.db.put(RocksDatabase.getDefaultWriteOptions(), key, 0, key.length, bbValue.Bytes, 0, bbValue.WriteIndex);

			// 保存在内存中，用来快速查询。
			this.tables.put(tableName, table);

		}
		return table;
	}
}
