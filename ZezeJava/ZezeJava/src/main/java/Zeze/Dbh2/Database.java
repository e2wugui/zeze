package Zeze.Dbh2;

import java.net.URI;
import java.util.HashMap;
import Zeze.Application;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Transaction.TableWalkKeyRaw;
import Zeze.Util.Func2;
import Zeze.Util.KV;

/**
 * 适配zeze-Database
 */
public class Database extends Zeze.Transaction.Database {
	private final String masterName;
	private final String databaseName;
	private final MasterAgent masterAgent;

	public Database(Application zeze, Config.DatabaseConf conf) {
		super(zeze, conf);
		// dbh2://ip:port/databaseName?user=xxx&passwd=xxx
		try {
			var url = new URI(getDatabaseUrl());
			masterName = url.getHost() + ":" + url.getPort();
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

		// todo 构造的时候创建Database可行？
		masterAgent = Dbh2AgentManager.getInstance().openDatabase(masterName, databaseName);
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

		private final HashMap<Dbh2Agent, Long> transactions = new HashMap<>();
		private boolean rollBacked = false;

		@Override
		public void commit() {
			for (var e : transactions.entrySet()) {
				e.getKey().commitTransaction(e.getValue());
			}
		}

		private KV<Long, Dbh2Agent> beginTransactionIf(String tableName, Binary bKey) {
			var manager = Dbh2AgentManager.getInstance();
			for (int i = 0; i < 2; ++i) {
				var agent = manager.open(
						masterAgent, masterName,
						databaseName, tableName,
						bKey);
				var tid = transactions.get(agent);
				if (null == tid)
					tid = agent.beginTransaction(databaseName, tableName);
				if (null != tid) {
					transactions.put(agent, tid);
					return KV.create(tid, agent);
				}
				manager.reload(masterAgent, masterName, databaseName, tableName);
			}
			throw new RuntimeException("begin transaction fail.");
		}

		private long beginTransactionIf(Dbh2Agent agent, String tableName) {
			var tid = transactions.get(agent);
			if (null == tid)
				tid = agent.beginTransaction(databaseName, tableName);
			if (null != tid) {
				transactions.put(agent, tid);
				return tid;
			}
			throw new RuntimeException("begin transaction 2 fail.");
		}

		private void operate(String tableName,
							 Binary bKey,
							 Func2<Long, Dbh2Agent, String> action) throws Exception {
			var manager = Dbh2AgentManager.getInstance();
			String raftNew = null;
			for (int i = 0; i < 2; ++i) {
				var kv = beginTransactionIf(tableName, bKey); // KV<tid, Dbh2Agent>
				raftNew = action.call(kv.getKey(), kv.getValue());
				if (raftNew == null) {
					// miss match bucket
					manager.reload(masterAgent, masterName, databaseName, tableName);
					continue;
				}
				if (raftNew.isEmpty())
					return; // done success

				// 迁移中的桶，数据已经被迁移到新的节点。
				break;
			}
			var agent = manager.open(raftNew);
			var tid = beginTransactionIf(agent, tableName);
			raftNew = action.call(tid, agent);
			if (raftNew == null)
				throw new RuntimeException("moving bucket target miss.");
			if (raftNew.isEmpty())
				return; // done success
			// 直接打开目标，建立新的事务执行put。
			throw new RuntimeException("put too many try.");
		}

		public void replace(String tableName, ByteBuffer key, ByteBuffer value) throws Exception {
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			var bValue = new Binary(value.Bytes, value.ReadIndex, value.size());
			operate(tableName, bKey, (tid, agent)
					-> agent.put(databaseName, tableName, tid, bKey, bValue));
		}

		public void remove(String tableName, ByteBuffer key) throws Exception {
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			operate(tableName, bKey, (tid, agent)
					-> agent.delete(databaseName, tableName, tid, bKey));
		}

		@Override
		public void rollback() {
			if (rollBacked)
				return;
			rollBacked = true;
			for (var e : transactions.entrySet()) {
				e.getKey().rollbackTransaction(e.getValue());
			}
		}

		@Override
		public void close() throws Exception {
			rollback();
		}
	}

	public class Dbh2Table extends Zeze.Transaction.Database.AbstractKVTable {
		private final String name;
		private final boolean isNew;

		public Dbh2Table(String tableName) {
			this.name = tableName;
			isNew = Dbh2AgentManager.getInstance().createTable(
					Database.this.masterAgent, Database.this.masterName,
					Database.this.databaseName, tableName);
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
			var manager = Dbh2AgentManager.getInstance();
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			// 最多执行两次。
			for (int i = 0; i < 2; ++i) {
				var agent = manager.open(
						Database.this.masterAgent, Database.this.masterName,
						Database.this.databaseName, name,
						bKey);
				var kv = agent.get(Database.this.databaseName, name, bKey);
				if (kv.getKey())
					return kv.getValue();

				// miss match bucket
				manager.reload(
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
			return 0;
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) {
			return 0;
		}

		@Override
		public long walkDesc(TableWalkHandleRaw callback) {
			return 0;
		}

		@Override
		public long walkKeyDesc(TableWalkKeyRaw callback) {
			return 0;
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			return null;
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			return null;
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			return null;
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			return null;
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
