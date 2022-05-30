package Zeze.Transaction;

import org.apache.logging.log4j.LogManager;
import org.rocksdb.*;
import org.apache.logging.log4j.Logger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.*;
import Zeze.Serialize.ByteBuffer;

public class DatabaseRocksDb extends Database {
	private static final Logger logger = LogManager.getLogger(DatabaseRocksDb.class);

	private final RocksDB Db;
	private final WriteOptions WriteOptions = new WriteOptions();
	private final ReadOptions ReadOptions = new ReadOptions();
	private final ColumnFamilyOptions CfOptions = new ColumnFamilyOptions();
	private final ConcurrentHashMap<String, ColumnFamilyHandle> ColumnFamilies = new ConcurrentHashMap<>();

	static{
		RocksDB.loadLibrary();
	}

	public static RocksDB Open(final DBOptions options, final String path,
							   final List<ColumnFamilyDescriptor> columnFamilyDescriptors,
							   final List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
		RocksDBException lastE = null;
		for (int i = 0; i < 10; ++i) {
			try {
				return RocksDB.open(options, path, columnFamilyDescriptors, columnFamilyHandles);
			} catch (RocksDBException e) {
				logger.info("RocksDB.open " + path, e);
				lastE = e;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
				}
			}
		}
		throw lastE;
	}

	public DatabaseRocksDb(Config.DatabaseConf conf) {
		super(conf);
		// logger.info("--- create rocksdb: " + conf.getDatabaseUrl(), new Exception());
		DBOptions dbOptions = new DBOptions();
		dbOptions.setCreateIfMissing(true);
		final String dbHome = conf.getDatabaseUrl().isEmpty() ? "db" : conf.getDatabaseUrl();
		try {
			var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
			org.rocksdb.Options options = new Options();
			for (var cf : RocksDB.listColumnFamilies(options, dbHome)) {
				columnFamilies.add(new ColumnFamilyDescriptor(cf, CfOptions));
			}
			if (columnFamilies.isEmpty()) {
				columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(), CfOptions));
			}

			// DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
			var outHandles = new ArrayList<ColumnFamilyHandle>();
			Db = Open(dbOptions, dbHome, columnFamilies, outHandles);

			for (int i = 0; i < columnFamilies.size(); ++i){
				var cf = columnFamilies.get(i);
				var str = new String(cf.getName(), StandardCharsets.UTF_8);
				ColumnFamilies.put(str, outHandles.get(i));
			}
			setDirectOperates(new OperatesRocksDb(this));
			// logger.info("--- create rocksdb: " + conf.getDatabaseUrl() + " OK!");
		} catch (RocksDBException dbEx) {
			throw new RuntimeException(dbEx);
		}
	}

	@Override
	public void Close() {
		// logger.info("--- close rocksdb: " + getDatabaseUrl(), new Exception());
		if (Db.isOwningHandle()) {
			try {
				Db.syncWal();
			} catch (RocksDBException ignored) {
			}
		}
		Db.close();
		// logger.info("--- close rocksdb: " + getDatabaseUrl() + " OK!");
		super.Close();
	}

	public static class RocksDbTrans implements Transaction {
		private final DatabaseRocksDb Database;
		private final WriteBatch Batch;

		public RocksDbTrans(DatabaseRocksDb database) {
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

		@Override
		public final void Commit() {
			try {
				Database.Db.write(Database.WriteOptions, Batch);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public final void Rollback() {
		}

		@Override
		public final void close() {
		}
	}

	@Override
	public Transaction BeginTransaction() {
		return new RocksDbTrans(this);
	}

	ColumnFamilyHandle getOrAddFamily(String name, Zeze.Util.OutObject<Boolean> isNew) {
		if (null != isNew)
			isNew.Value = false;

		return ColumnFamilies.computeIfAbsent(name, (key) -> {
			try {
				if (null != isNew)
					isNew.Value = true;
				return Db.createColumnFamily(new ColumnFamilyDescriptor(key.getBytes(StandardCharsets.UTF_8), CfOptions));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public Table OpenTable(String name) {
		var isNew = new Zeze.Util.OutObject<Boolean>();
		var cfh = getOrAddFamily(name, isNew);
		return new TableRocksDb(this, name, cfh, isNew.Value);
	}

	public final static class TableRocksDb implements Database.Table {
		private final DatabaseRocksDb DatabaseReal;
		public DatabaseRocksDb getDatabaseReal() {
			return DatabaseReal;
		}
		@Override
		public Database getDatabase() {
			return getDatabaseReal();
		}
		private final String Name;
		public String getName() {
			return Name;
		}
		private final ColumnFamilyHandle ColumnFamily;
		private ColumnFamilyHandle getColumnFamily() {
			return ColumnFamily;
		}
		private final boolean isNew;

		public TableRocksDb(DatabaseRocksDb database, String name, ColumnFamilyHandle cfh, boolean isNew) {
			DatabaseReal = database;
			Name = name;
			ColumnFamily = cfh;
			this.isNew = isNew;
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		@Override
		public void Close() {
		}

		@Override
		public ByteBuffer Find(ByteBuffer key) {
			byte[] value;
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

		@Override
		public void Remove(Transaction t, ByteBuffer key) {
			var txn = (RocksDbTrans)t;
			txn.Remove(key.Copy(), getColumnFamily());
		}

		@Override
		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			var txn = (RocksDbTrans)t;
			txn.Put(key.Copy(), value.Copy(), getColumnFamily());
		}

		@Override
		public long Walk(TableWalkHandleRaw callback) {
			try (var it = getDatabaseReal().Db.newIterator(getColumnFamily(), getDatabaseReal().ReadOptions)) {
				long countWalked = 0;
				it.seekToFirst();
				while (it.isValid()) {
					++countWalked;
					if (!callback.handle(it.key(), it.value())) {
						return countWalked;
					}
					it.next();
				}
				return countWalked;
			}
		}
	}

	public final static class OperatesRocksDb implements Operates {
		private final DatabaseRocksDb DatabaseReal;
		public DatabaseRocksDb getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}
		public static final String ColumnFamilyName = "zeze.OperatesRocksDb.Schemas";
		private final ColumnFamilyHandle ColumnFamily;

		public OperatesRocksDb(DatabaseRocksDb database) {
			DatabaseReal = database;
			ColumnFamily = getDatabaseReal().getOrAddFamily(ColumnFamilyName, null);
		}

		@Override
		public int ClearInUse(int localId, String global) {
			// rocksdb 独占由它自己打开的时候保证。
			return 0;
		}

		private static class DVRocks extends DataWithVersion implements Zeze.Serialize.Serializable {
			@Override
			public final void Decode(ByteBuffer bb) {
				Data = ByteBuffer.Wrap(bb.ReadBytes());
				Version = bb.ReadLong();
			}

			@Override
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
				int dataSize = Data.Size();
				var bb = ByteBuffer.Allocate(ByteBuffer.writeUIntSize(dataSize) + dataSize + ByteBuffer.writeLongSize(Version));
				this.Encode(bb);
				return bb.Bytes;
			}
		}

		@Override
		public synchronized DataWithVersion GetDataWithVersion(ByteBuffer key) {
			try {
				return DVRocks.Decode(DatabaseReal.Db.get(ColumnFamily, key.Copy()));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public synchronized Zeze.Util.KV<Long, Boolean> SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			DVRocks dv;
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

		@Override
		public void SetInUse(int localId, String global) {
			// rocksdb 独占由它自己打开的时候保证。
		}
	}
}
