package Zeze.Transaction;

import Zeze.Util.KV;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.*;
import Zeze.Serialize.ByteBuffer;

public class DatabaseRocksDb extends Database {
	private static final Logger logger = LogManager.getLogger(DatabaseMySql.class);

	private RocksDB Db;
	private WriteOptions WriteOptions = new WriteOptions();
	private ReadOptions ReadOptions = new ReadOptions();
	private ColumnFamilyOptions CfOptions = new ColumnFamilyOptions();
	private Options Options = new Options();
	private DBOptions DbOptions = new DBOptions();
	private ConcurrentHashMap<String, ColumnFamilyHandle> ColumnFamilies = new ConcurrentHashMap<>();

	public DatabaseRocksDb(Application zeze, Config.DatabaseConf conf) {
		super(conf);

		if (!zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().isEmpty()) {
			throw new RuntimeException("RocksDb Can Not Work With GlobalCacheManager.");
		}
		DbOptions.setCreateIfMissing(true);
		try {
			var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
			for (var cf : RocksDB.listColumnFamilies(Options, conf.getDatabaseUrl())) {
				columnFamilies.add(new ColumnFamilyDescriptor(cf, CfOptions));
			}

			// DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
			var outHandles = new ArrayList<ColumnFamilyHandle>();
			Db = RocksDB.open(DbOptions, conf.getDatabaseUrl(), columnFamilies, outHandles);

			for (int i = 0; i< columnFamilies.size(); ++i){
				var cf = columnFamilies.get(i);
				var str = new String(cf.getName(), StandardCharsets.UTF_8);
				ColumnFamilies.put(str, outHandles.get(i));
			}
			setDirectOperates(new OperatesRocksDb(this));
		} catch (RocksDBException dbex) {
			throw new RuntimeException(dbex);
		}
	}

	@Override
	public void Close() {
		Db.close();
		super.Close();
	}

	public static class RockdsDbTrans implements Transaction {
		private DatabaseRocksDb Database;
		private WriteBatch Batch;

		public RockdsDbTrans(DatabaseRocksDb database) {
			Database = database;
			Batch = new WriteBatch();
		}

		public final void Put(byte[] key, byte[] value, ColumnFamilyHandle family) {
			try {
				Batch.put(family, key, value);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		public final void Remove(byte[] key, ColumnFamilyHandle family) {
			try {
				Batch.delete(family, key);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		public final void Commit() {
			try {
				Database.Db.write(Database.WriteOptions, Batch);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		public final void Rollback() {
		}

		public final void close() {
		}
	}

	@Override
	public Transaction BeginTransaction() {
		return new RockdsDbTrans(this);
	}

	ColumnFamilyHandle getOrAddFamily(String name) {
		return ColumnFamilies.computeIfAbsent(name, (key) -> {
			try {
				return Db.createColumnFamily(new ColumnFamilyDescriptor(key.getBytes(StandardCharsets.UTF_8), CfOptions));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public Table OpenTable(String name) {
		return new TableRocksDb(this, name, getOrAddFamily(name));
	}

	public final static class TableRocksDb implements Database.Table {
		private DatabaseRocksDb DatabaseReal;
		public DatabaseRocksDb getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}
		private String Name;
		public String getName() {
			return Name;
		}
		private ColumnFamilyHandle ColumnFamily;
		private ColumnFamilyHandle getColumnFamily() {
			return ColumnFamily;
		}

		public TableRocksDb(DatabaseRocksDb database, String name, ColumnFamilyHandle cfh) {
			DatabaseReal = database;
			Name = name;
			ColumnFamily = cfh;
		}

		public void Close() {
		}

		private KV<byte[], Integer> GetBytes(ByteBuffer bb)
		{
			byte[] bytes;
			int byteslen;
			if (bb.ReadIndex == 0)
			{
				bytes = bb.Bytes;
				byteslen = bb.Size();
			}
			else
			{
				bytes = bb.Copy();
				byteslen = bytes.length;
			}
			return KV.Create(bytes, byteslen);
		}

		public ByteBuffer Find(ByteBuffer key) {
			byte[] value = new byte[0];
			try {
				value = getDatabaseReal().Db.get(
						getColumnFamily(), getDatabaseReal().ReadOptions, key.Bytes, key.ReadIndex, key.WriteIndex);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
			if (null == value) {
				return null;
			}
			return ByteBuffer.Wrap(value);
		}

		public void Remove(Transaction t, ByteBuffer key) {
			var txn = (RockdsDbTrans)t;
			txn.Remove(key.Copy(), getColumnFamily());
		}

		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			var txn = (RockdsDbTrans)t;
			txn.Put(key.Copy(), value.Copy(), getColumnFamily());
		}

		public long Walk(TableWalkHandleRaw callback) {
			var it = getDatabaseReal().Db.newIterator(getColumnFamily(), getDatabaseReal().ReadOptions);
			try {
				long countWalked = 0;
				it.seekToFirst();
				while (it.isValid()) {
					++countWalked;
					if (false == callback.handle(it.key(), it.value())) {
						return countWalked;
					}
					it.next();
				}
				return countWalked;
			} finally {
				it.close();
			}
		}
	}

	public final static class OperatesRocksDb implements Operates {
		private DatabaseRocksDb DatabaseReal;
		public DatabaseRocksDb getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}
		public static final String ColumnFamilyName = "zeze.OperatesRocksDb.Schemas";
		private ColumnFamilyHandle ColumnFamily;

		public OperatesRocksDb(DatabaseRocksDb database) {
			DatabaseReal = database;
			ColumnFamily = getDatabaseReal().getOrAddFamily(ColumnFamilyName);
		}

		public int ClearInUse(int localId, String global) {
			// rocksdb 独占由它自己打开的时候保证。
			return 0;
		}

		private static class DVRocks extends DataWithVersion implements Zeze.Serialize.Serializable {
			public final void Decode(ByteBuffer bb) {
				Data = ByteBuffer.Wrap(bb.ReadBytes());
				Version = bb.ReadLong();
			}

			public final void Encode(ByteBuffer bb) {
				bb.WriteByteBuffer(Data);
				bb.WriteLong(Version);
			}

			public static DVRocks Decode(byte[] bytes) {
				if (null == bytes) {
					return new DVRocks();
				}
				var dv = new DVRocks();
				dv.Decode(ByteBuffer.Wrap(bytes));
				return dv;
			}

			public final byte[] Encode() {
				var bb = ByteBuffer.Allocate();
				this.Encode(bb);
				return bb.Copy();
			}
		}

		public DataWithVersion GetDataWithVersion(ByteBuffer key) {
			synchronized (this) {
				try {
					return DVRocks.Decode(DatabaseReal.Db.get(ColumnFamily, key.Copy()));
				} catch (RocksDBException e) {
					throw new RuntimeException(e);
				}
			}
		}

		public Zeze.Util.KV<Long, Boolean>  SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			synchronized (this) {
				DVRocks dv = null;
				try {
					dv = DVRocks.Decode(getDatabaseReal().Db.get(ColumnFamily, key.Copy()));
				} catch (RocksDBException e) {
					throw new RuntimeException(e);
				}
				if (dv.Version != version) {
					return Zeze.Util.KV.Create(version, false);
				}

				version++;
				dv.Version = version;
				dv.Data = data;
				try {
					getDatabaseReal().Db.put(ColumnFamily, getDatabaseReal().WriteOptions, key.Copy(), dv.Encode());
				} catch (RocksDBException e) {
					throw new RuntimeException(e);
				}
				return Zeze.Util.KV.Create(version, true);
			}
		}

		public void SetInUse(int localId, String global) {
			// rocksdb 独占由它自己打开的时候保证。
		}
	}
}
