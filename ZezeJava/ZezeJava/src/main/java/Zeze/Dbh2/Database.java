package Zeze.Dbh2;

import java.net.URI;
import Zeze.Application;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Transaction.TableWalkKeyRaw;
import Zeze.Util.KV;
import Zeze.Util.TaskCompletionSource;
import org.jetbrains.annotations.Nullable;

/**
 * 适配zeze-Database
 */
public class Database extends Zeze.Transaction.Database {
	private final String masterName;
	private final String databaseName;
	private final MasterAgent masterAgent;
	private final Dbh2AgentManager dbh2AgentManager;

	public Database(@Nullable Application zeze, Dbh2AgentManager dbh2AgentManager, Config.DatabaseConf conf) {
		super(zeze, conf);

		this.dbh2AgentManager = dbh2AgentManager;
		// dbh2://ip:port/databaseName?user=xxx&passwd=xxx
		try {
			var url = new URI(getDatabaseUrl());
			masterName = url.getHost() + "_" + url.getPort();
			databaseName = new java.io.File(url.getPath()).getName();
			if (databaseName.contains("@"))
				throw new RuntimeException("'@' is reserve.");
			/*
			var query = HttpExchange.parseQuery(url.getQuery());
			user = query.get("user");
			passwd = query.get("passwd");
			*/
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		setDirectOperates(new NullOperates()); // todo operates

		masterAgent = dbh2AgentManager.openDatabase(masterName, databaseName);
	}

	@Override
	public Table openTable(String name) {
		if (name.contains("@"))
			throw new RuntimeException("'@' is reserve.");
		return new Dbh2Table(name);
	}

	@Override
	public Transaction beginTransaction() {
		return new Dbh2Transaction();
	}

	public class Dbh2Transaction implements Zeze.Transaction.Database.Transaction {
		private final BPrepareBatches.Data batches = new BPrepareBatches.Data();

		public void commitBreakAfterPrepareForDebugOnly() {
			dbh2AgentManager.commitBreakAfterPrepareForDebugOnly(batches);
		}

		@Override
		public void commit() {
			dbh2AgentManager.commit(batches);
		}

		public void replace(String tableName, ByteBuffer key, ByteBuffer value) {
			if (value.size() <= 0)
				throw new RuntimeException("value.size <= 0.");
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			var bValue = new Binary(value.Bytes, value.ReadIndex, value.size());
			var agent = dbh2AgentManager.locateBucket(masterAgent, masterName, databaseName, tableName, bKey);
			var batch = batches.getDatas().computeIfAbsent(agent,
					_agent_ -> new BPrepareBatch.Data(masterName, databaseName, tableName, null));
			batch.getBatch().getPuts().put(bKey, bValue);
		}

		public void remove(String tableName, ByteBuffer key) {
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			var agent = dbh2AgentManager.locateBucket(masterAgent, masterName, databaseName, tableName, bKey);
			var batch = batches.getDatas().computeIfAbsent(agent,
					_agent_ -> new BPrepareBatch.Data(masterName, databaseName, tableName, null));
			batch.getBatch().getDeletes().add(bKey);
		}

		@Override
		public void rollback() {
		}

		@Override
		public void close() throws Exception {
		}
	}

	public class Dbh2Table extends Zeze.Transaction.Database.AbstractKVTable {
		private final String name;
		private boolean isNew;
		private final TaskCompletionSource<Integer> ready = new TaskCompletionSource<>();

		public Dbh2Table(String tableName) {
			this.name = tableName;
			dbh2AgentManager.createTableAsync(
					Database.this.masterAgent, Database.this.masterName,
					Database.this.databaseName, tableName,
					(rc, _isNew) -> {
						isNew = _isNew;
						if (rc == 0)
							ready.setResult(0);
						else
							ready.setException(new RuntimeException("rc=" + rc));
					});
		}

		@Override
		public void waitReady() {
			ready.await();
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		@Override
		public Zeze.Transaction.Database getDatabase() {
			return Database.this;
		}

		@Override
		public ByteBuffer find(ByteBuffer key) {
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			// 最多执行两次。
			for (int i = 0; i < 2; ++i) {
				var raft = dbh2AgentManager.locateBucket(
						Database.this.masterAgent, Database.this.masterName,
						Database.this.databaseName, name,
						bKey);
				var agent = dbh2AgentManager.openBucket(raft);
				var kv = agent.get(Database.this.databaseName, name, bKey);
				if (kv.getKey())
					return kv.getValue();

				// miss match bucket
				dbh2AgentManager.reload(
						Database.this.masterAgent, Database.this.masterName,
						Database.this.databaseName, name);
			}
			throw new RuntimeException("fail too many try.");
		}

		@Override
		public void replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			var txn = (Dbh2Transaction)t;
			try {
				txn.replace(name, key, value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void remove(Transaction t, ByteBuffer key) {
			var txn = (Dbh2Transaction)t;
			try {
				txn.remove(name, key);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public long walk(TableWalkHandleRaw callback) {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name, callback, false);
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name, callback, false);
		}

		@Override
		public long walkDesc(TableWalkHandleRaw callback) {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name, callback, true);
		}

		@Override
		public long walkKeyDesc(TableWalkKeyRaw callback) {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name, callback, true);
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, false);
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, false);
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, true);
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, true);
		}

		@Override
		public void close() {

		}
	}

	public static class Dbh2Operates implements Zeze.Transaction.Database.Operates {

		@Override
		public void setInUse(int localId, String global) {
		}

		@Override
		public int clearInUse(int localId, String global) {
			return 0;
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			return null;
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			return null;
		}
	}

}
