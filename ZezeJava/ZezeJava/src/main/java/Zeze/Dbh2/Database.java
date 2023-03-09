package Zeze.Dbh2;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.Binary;
import Zeze.Netty.HttpExchange;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Data;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Transaction.TableWalkKeyRaw;
import Zeze.Util.KV;

/**
 * 适配zeze-Database
 */
public class Database extends Zeze.Transaction.Database {
	private final ConcurrentHashMap<String, Dbh2Agent> agents = new ConcurrentHashMap<>();
	private final String masterName;
	private final String databaseName;
	private final String user;
	private final String passwd;
	private final MasterAgent masterAgent;

	public Database(Config.DatabaseConf conf) {
		super(conf);
		// dbh2://ip:port/databaseName?user=xxx&passwd=xxx
		try {
			var url = new URL(getDatabaseUrl());
			masterName = url.getHost() + ":" + url.getPort();
			databaseName = url.getFile();

			var query = HttpExchange.parseQuery(url.getQuery());
			user = query.get("user");
			passwd = query.get("passwd");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		setDirectOperates(new NullOperates()); // todo operates

		// todo 构造的时候创建Database可行？
		masterAgent = Dbh2AgentManager.getInstance().openDatabase(masterName, databaseName, user, passwd);
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

		@Override
		public void commit() {

		}

		public void replace(String tableName, ByteBuffer key, ByteBuffer value) {

		}

		public void remove(String talbeName, ByteBuffer key) {

		}

		@Override
		public void rollback() {

		}

		@Override
		public void close() throws Exception {

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
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			var agent = Dbh2AgentManager.getInstance().open(
					Database.this.masterAgent, Database.this.masterName, Database.this.databaseName, name,
					bKey);
			var value = agent.get(bKey);
			if (null == value)
				return null;
			return ByteBuffer.Wrap(value);
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
