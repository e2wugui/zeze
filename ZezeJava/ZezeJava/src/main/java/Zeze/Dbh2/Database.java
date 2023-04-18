package Zeze.Dbh2;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import Zeze.Application;
import Zeze.Builtin.Dbh2.BBatchTid;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.Binary;
import Zeze.Raft.RaftRpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Transaction.TableWalkKeyRaw;
import Zeze.Util.KV;
import Zeze.Util.TaskCompletionSource;
import org.jetbrains.annotations.NotNull;

/**
 * 适配zeze-Database
 */
public class Database extends Zeze.Transaction.Database {
	private final String masterName;
	private final String databaseName;
	private final MasterAgent masterAgent;

	public Database(@NotNull Application zeze, Config.DatabaseConf conf) {
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

		private final HashMap<Dbh2Agent, BPrepareBatch.Data> batches = new HashMap<>();

		public void commitBreakAfterPrepareForDebugOnly() {
			var manager = Dbh2AgentManager.getInstance();
			var query = manager.choiceCommitServer();
			try {
				var tid = manager.nextTransactionId();
				logger.warn("prepare tid=" + tid);
				// prepare
				var futures = new ArrayList<TaskCompletionSource<RaftRpc<BPrepareBatch.Data, EmptyBean.Data>>>();
				for (var e : batches.entrySet()) {
					var batch = e.getValue();
					batch.getBatch().setQueryIp(query.getKey());
					batch.getBatch().setQueryPort(query.getValue());
					batch.getBatch().setTid(tid);
					futures.add(e.getKey().prepareBatch(batch));
				}
				for (var e : futures) {
					e.await();
				}
			} catch (Throwable ex) {
				// undo
				var futures = new ArrayList<TaskCompletionSource<?>>();
				for (var e : batches.entrySet()) {
					var tid = e.getValue().getBatch().getTid();
					if (tid.size() == 0)
						continue; // not prepared
					futures.add(e.getKey().undoBatch(tid));
				}
				for (var e : futures)
					e.await();
				throw new RuntimeException(ex);
			}
		}

		private void undo() {
			// undo
			var futures = new ArrayList<TaskCompletionSource<?>>();
			for (var e : batches.entrySet()) {
				var tid2 = e.getValue().getBatch().getTid();
				if (tid2.size() == 0)
					continue; // not prepared
				futures.add(e.getKey().undoBatch(tid2));
			}
			for (var e : futures)
				e.await();
		}

		@Override
		public void commit() {
			var manager = Dbh2AgentManager.getInstance();
			var query = manager.choiceCommitServer();
			boolean localCommit;
			var tid = manager.nextTransactionId();
			var prepareTime = System.currentTimeMillis();
			try {
				// prepare
				var futures = new ArrayList<TaskCompletionSource<RaftRpc<BPrepareBatch.Data, EmptyBean.Data>>>();
				for (var e : batches.entrySet()) {
					var batch = e.getValue();
					batch.getBatch().setQueryIp(query.getKey());
					batch.getBatch().setQueryPort(query.getValue());
					batch.getBatch().setTid(tid);
					futures.add(e.getKey().prepareBatch(batch));
				}
				for (var e : futures) {
					e.await();
				}
			} catch (Throwable ex) {
				undo();
				throw new RuntimeException(ex);
			}

			if (System.currentTimeMillis() - prepareTime > Database.this.getConf().getPrepareMaxTime()) {
				undo();
				throw new RuntimeException("max prepare time exceed.");
			}

			try {
				// 保存 commit-point，如果失败，则 undo。
				localCommit = manager.committing(query.getKey(), query.getValue(), tid, batches);
			} catch (Throwable ex) {
				undo();
				throw new RuntimeException(ex);
			}

			// commit
			if (localCommit) {
				var futures = new ArrayList<TaskCompletionSource<?>>();
				for (var e : batches.entrySet()) {
					futures.add(e.getKey().commitBatch(e.getValue().getBatch().getTid()));
				}
				for (var e : futures)
					e.await();
				//manager.commitDone(tid, batches);
			}
		}

		public void replace(String tableName, ByteBuffer key, ByteBuffer value) throws Exception {
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			var bValue = new Binary(value.Bytes, value.ReadIndex, value.size());
			var manager = Dbh2AgentManager.getInstance();
			var agent = manager.start(masterAgent, masterName, databaseName, tableName, bKey);
			var batch = batches.computeIfAbsent(agent, _agent_ -> new BPrepareBatch.Data(databaseName, tableName));
			batch.getBatch().getPuts().put(bKey, bValue);
		}

		public void remove(String tableName, ByteBuffer key) throws Exception {
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			var manager = Dbh2AgentManager.getInstance();
			var agent = manager.start(masterAgent, masterName, databaseName, tableName, bKey);
			var batch = batches.computeIfAbsent(agent, _agent_ -> new BPrepareBatch.Data(databaseName, tableName));
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
				var agent = manager.start(
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
