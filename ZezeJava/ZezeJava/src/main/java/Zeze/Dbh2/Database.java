package Zeze.Dbh2;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Transaction.TableWalkKeyRaw;
import Zeze.Util.KV;

/**
 * 适配zeze-Database
 */
public class Database extends Zeze.Transaction.Database {
	private final String masterName;
	private final String databaseName;
	private final MasterAgent masterAgent;

	public Database(Config.DatabaseConf conf) {
		super(conf);
		// dbh2://ip:port/databaseName?user=xxx&passwd=xxx
		try {
			var url = new URL(getDatabaseUrl());
			masterName = url.getHost() + ":" + url.getPort();
			databaseName = url.getFile();
			/*
			var query = HttpExchange.parseQuery(url.getQuery());
			user = query.get("user");
			passwd = query.get("passwd");
			*/
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		setDirectOperates(new NullOperates()); // todo operates

		// todo 构造的时候创建Database可行？
		masterAgent = Dbh2AgentManager.getInstance().openDatabase(masterName, databaseName);
	}

	@Override
	public Table openTable(String name) {
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

		public void replace(String tableName, ByteBuffer key, ByteBuffer value) {
			var manager = Dbh2AgentManager.getInstance();
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			var bValue = new Binary(value.Bytes, value.ReadIndex, value.size());
			// 最多执行两次。
			Long tid;
			var agent = manager.open(
					Database.this.masterAgent, Database.this.masterName,
					Database.this.databaseName, tableName,
					bKey);
			tid = transactions.get(agent);
			if (null == tid)
				tid = agent.beginTransaction();
			if (null == tid) // begin 检查 database & table，不能失败。
				throw new RuntimeException("begin transaction fail");
			transactions.put(agent, tid);
			// 迁移中的桶，数据已经被迁移到新的节点。
			var raftNew = agent.put(tid, bKey, bValue);
			if (raftNew.isEmpty())
				return; // done success

			// 直接打开目标，建立新的事务执行put。
			for (int i = 0; i < 2; ++i) {
				agent = manager.open(Database.this.databaseName, tableName, raftNew);
				tid = transactions.get(agent);
				if (null == tid)
					tid = agent.beginTransaction();
				if (null == tid) // begin 检查 database & table，不能失败。
					throw new RuntimeException("begin transaction fail");
				transactions.put(agent, tid);
				raftNew = agent.put(tid, bKey, bValue);
				if (raftNew.isEmpty())
					return; // done success
			}
			throw new RuntimeException("put too many try.");
		}

		// todo 这个流程和put完全一样，等完整的算法确定下来以后，实现成lambda.action模式。共享同一个流程。
		public void remove(String tableName, ByteBuffer key) {
			var manager = Dbh2AgentManager.getInstance();
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			// 最多执行两次。
			Long tid;
			var agent = manager.open(
					Database.this.masterAgent, Database.this.masterName,
					Database.this.databaseName, tableName,
					bKey);
			tid = transactions.get(agent);
			if (null == tid)
				tid = agent.beginTransaction();
			if (null == tid) // begin 检查 database & table，不能失败。
				throw new RuntimeException("begin transaction fail");
			transactions.put(agent, tid);
			// 迁移中的桶，数据已经被迁移到新的节点。
			var raftNew = agent.delete(tid, bKey);
			if (raftNew.isEmpty())
				return; // done success

			// 直接打开目标，建立新的事务执行put。
			for (int i = 0; i < 2; ++i) {
				agent = manager.open(Database.this.databaseName, tableName, raftNew);
				tid = transactions.get(agent);
				if (null == tid)
					tid = agent.beginTransaction();
				transactions.put(agent, tid);
				if (null == tid) // begin 检查 database & table，不能失败。
					throw new RuntimeException("begin transaction fail");
				raftNew = agent.delete(tid, bKey);
				if (raftNew.isEmpty())
					return; // done success
			}
			throw new RuntimeException("too many try.");
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

	public class Dbh2Table implements Zeze.Transaction.Database.Table {
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
				var kv = agent.get(bKey);
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
			txn.replace(name, key, value);
		}

		@Override
		public void remove(Transaction t, ByteBuffer key) {
			var txn = (Dbh2Transaction)t;
			txn.remove(name, key);
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

	public class Dbh2Operates implements Zeze.Transaction.Database.Operates {

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
