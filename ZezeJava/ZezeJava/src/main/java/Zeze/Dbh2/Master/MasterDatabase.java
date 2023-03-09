package Zeze.Dbh2.Master;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMetaDaTa;
import Zeze.Builtin.Dbh2.Master.CreateBucket;
import Zeze.Dbh2.Bucket;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutObject;
import Zeze.Util.TaskCompletionSource;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class MasterDatabase {
	private final String databaseName;
	private final ConcurrentHashMap<String, MasterTableDaTa> tables = new ConcurrentHashMap<>();
	private final RocksDB db;
	private final Master master;

	public MasterDatabase(Master master, String databaseName) {
		try {
			this.master = master;
			this.databaseName = databaseName;
			this.db = RocksDB.open(databaseName);

			try (var it = this.db.newIterator(Bucket.getDefaultReadOptions())) {
				while (it.isValid()) {
					var tableName = new String(it.key(), StandardCharsets.UTF_8);
					var bTable = new MasterTableDaTa();
					var bb = ByteBuffer.Wrap(it.value());
					bTable.decode(bb);
					tables.put(tableName, bTable);
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

	public MasterTableDaTa getTable(String tableName) {
		return tables.get(tableName);
	}

	public BBucketMetaDaTa locateBucket(String tableName, Binary key) {
		var bTable = getTable(tableName);
		if (null == bTable)
			return null;
		return bTable.locate(key);
	}

	public MasterTableDaTa createTable(String tableName, OutObject<Boolean> outIsNew) throws RocksDBException {
		outIsNew.value = false;
		var table = tables.computeIfAbsent(tableName, (tbName) -> new MasterTableDaTa());
		if (table.created)
			return table;

		synchronized (table) {
			// 加锁后再次检查一次。
			if (table.created)
				return table;

			outIsNew.value = true;
			table = new MasterTableDaTa();
			var bucket = new BBucketMetaDaTa();
			bucket.setDatabaseName(databaseName);
			bucket.setTableName(tableName);
			bucket.setMoving(false);
			bucket.setKeyFirst(Binary.Empty);
			bucket.setKeyLast(Binary.Empty);
			table.buckets.put(bucket.getKeyFirst(), bucket);

			// allocate first bucket service and setup table

			var managers = master.choiceManagers();
			// 构建raft-config，基本的用于客户端，用于manager服务器的需要replace RaftName.
			var sbRaft = new StringBuilder();
			sbRaft.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			sbRaft.append("\n");
			sbRaft.append("<raft Name=\"RaftName\">");
			for (var e : managers.entrySet()) {
				sbRaft.append("    <node Host=\"");
				sbRaft.append(e.getValue().getDbh2RaftAcceptorName());
				sbRaft.append("\" Port=\"");
				sbRaft.append(e.getValue().getNextPort());
				e.getValue().setNextPort(e.getValue().getNextPort() + 1); // todo 线程安全
				sbRaft.append("\"/>\n");
			}
			sbRaft.append("</raft>");
			bucket.setRaftConfig(sbRaft.toString());

			var futures = new ArrayList<TaskCompletionSource<?>>();
			for (var e : managers.entrySet()) {
				var r = new CreateBucket();
				r.Argument.assign(bucket);
				// 用于manager服务器的需要replace RaftName.
				r.Argument.setRaftConfig(r.Argument.getRaftConfig().replaceAll(
						"RaftName", e.getValue().getDbh2RaftAcceptorName()));
				futures.add(r.SendForWait(e.getKey()));
			}
			for (var future : futures)
				future.await();

			// master数据马上存数据库。
			var bbValue = ByteBuffer.Allocate();
			table.encode(bbValue);
			var key = tableName.getBytes(StandardCharsets.UTF_8);
			this.db.put(Bucket.getDefaultWriteOptions(), key, 0, key.length, bbValue.Bytes, bbValue.ReadIndex, bbValue.size());

			// 保存在内存中，用来快速查询。
			this.tables.put(tableName, table);
			table.created = true;
		}
		return table;
	}
}
