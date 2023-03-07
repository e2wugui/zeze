package Zeze.Dbh2;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Transaction.TableWalkKeyRaw;
import Zeze.Util.KV;

/**
 * 适配zeze-Database
 */
public class Database extends Zeze.Transaction.Database {
	private final ConcurrentHashMap<String, Dbh2Agent> agents = new ConcurrentHashMap<>();

	public Database(Config.DatabaseConf conf) {
		super(conf);
		setDirectOperates(new NullOperates()); // todo operates
	}

	@Override
	public Table openTable(String name) {
		return new Dbh2Table(name);
	}

	public ByteBuffer get(String tableName, ByteBuffer key) {
		return null;
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

		public Dbh2Table(String tableName) {
			this.name = tableName;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean isNew() {
			return false;
		}

		@Override
		public Zeze.Transaction.Database getDatabase() {
			return Database.this;
		}

		@Override
		public ByteBuffer find(ByteBuffer key) {
			return Database.this.get(name, key);
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
