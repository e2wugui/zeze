package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

public class DatabaseRocksDb extends Database {
	private static final Logger logger = LogManager.getLogger(DatabaseRocksDb.class);

	private static final Options commonOptions = new Options()
			.setCreateIfMissing(true)
			.setDbWriteBufferSize(64 << 20) // 总的写缓存大小(字节),对所有columns的总限制
			.setKeepLogFileNum(5); // 保留"LOG.old.*"文件的数量
	private static final DBOptions commonDbOptions = new DBOptions()
			.setCreateIfMissing(true)
			.setDbWriteBufferSize(64 << 20) // 总的写缓存大小(字节),对所有columns的总限制
			.setKeepLogFileNum(5); // 保留"LOG.old.*"文件的数量
	private static final ColumnFamilyOptions defaultCfOptions = new ColumnFamilyOptions();
	private static final ReadOptions defaultReadOptions = new ReadOptions();
	private static final WriteOptions defaultWriteOptions = new WriteOptions();
	private static final WriteOptions syncWriteOptions = new WriteOptions().setSync(true);

	private final RocksDB rocksDb;
	private final ConcurrentHashMap<String, ColumnFamilyHandle> columnFamilies = new ConcurrentHashMap<>();

	static {
		RocksDB.loadLibrary();
	}

	public static Options getCommonOptions() {
		return commonOptions;
	}

	public static DBOptions getCommonDbOptions() {
		return commonDbOptions;
	}

	public static ColumnFamilyOptions getDefaultCfOptions() {
		return defaultCfOptions;
	}

	public static ReadOptions getDefaultReadOptions() {
		return defaultReadOptions;
	}

	public static WriteOptions getDefaultWriteOptions() {
		return defaultWriteOptions;
	}

	public static WriteOptions getSyncWriteOptions() {
		return syncWriteOptions;
	}

	public static RocksDB Open(DBOptions options, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors,
							   List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
		for (int i = 0; ; ) {
			try {
				return RocksDB.open(options, path, columnFamilyDescriptors, columnFamilyHandles);
			} catch (RocksDBException e) {
				logger.warn("RocksDB.open " + path + " failed:", e);
				if (++i >= 10)
					throw e;
				try {
					//noinspection BusyWait
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
					throw e;
				}
			}
		}
	}

	public DatabaseRocksDb(Config.DatabaseConf conf) {
		super(conf);
		logger.info("new: {}", conf.getDatabaseUrl());

		var dbHome = conf.getDatabaseUrl().isEmpty() ? "db" : conf.getDatabaseUrl();
		try {
			var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
			for (var cf : RocksDB.listColumnFamilies(commonOptions, dbHome))
				columnFamilies.add(new ColumnFamilyDescriptor(cf, defaultCfOptions));
			if (columnFamilies.isEmpty())
				columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(StandardCharsets.UTF_8), defaultCfOptions));

			// DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
			var outHandles = new ArrayList<ColumnFamilyHandle>();
			rocksDb = Open(commonDbOptions, dbHome, columnFamilies, outHandles);

			for (int i = 0; i < columnFamilies.size(); i++) {
				var name = new String(columnFamilies.get(i).getName(), StandardCharsets.UTF_8);
				this.columnFamilies.put(name, outHandles.get(i));
			}
			setDirectOperates(new OperatesRocksDb());
		} catch (RocksDBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void Close() {
		logger.info("Close: {}", getDatabaseUrl());
		if (rocksDb.isOwningHandle()) {
			try {
				rocksDb.syncWal();
			} catch (RocksDBException ignored) {
			}
		}
		rocksDb.close();
		super.Close();
	}

	@Override
	public Transaction BeginTransaction() {
		return new RocksDbTrans();
	}

	private final class RocksDbTrans implements Transaction {
		private WriteBatch batch;

		private WriteBatch getBatch() {
			var wb = batch;
			if (wb == null)
				batch = wb = new WriteBatch();
			return wb;
		}

		void Put(byte[] key, byte[] value, ColumnFamilyHandle family) {
			try {
				getBatch().put(family, key, value);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		void Remove(byte[] key, ColumnFamilyHandle family) {
			try {
				getBatch().delete(family, key);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void Commit() {
			if (batch == null)
				return;
			try {
				rocksDb.write(defaultWriteOptions, batch);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void Rollback() {
		}

		@Override
		public void close() {
			if (batch != null) {
				batch.close();
				batch = null;
			}
		}
	}

	private ColumnFamilyHandle getOrAddFamily(String name, OutObject<Boolean> isNew) {
		if (isNew != null)
			isNew.Value = false;

		return columnFamilies.computeIfAbsent(name, key -> {
			try {
				if (isNew != null)
					isNew.Value = true;
				return rocksDb.createColumnFamily(
						new ColumnFamilyDescriptor(key.getBytes(StandardCharsets.UTF_8), defaultCfOptions));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public Table OpenTable(String name) {
		var isNew = new OutObject<Boolean>();
		var cfh = getOrAddFamily(name, isNew);
		return new TableRocksDb(cfh, isNew.Value);
	}

	private final class TableRocksDb implements Database.Table {
		private final ColumnFamilyHandle columnFamily;
		private final boolean isNew;

		TableRocksDb(ColumnFamilyHandle columnFamily, boolean isNew) {
			this.columnFamily = columnFamily;
			this.isNew = isNew;
		}

		@Override
		public DatabaseRocksDb getDatabase() {
			return DatabaseRocksDb.this;
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
			try {
				var value = rocksDb.get(columnFamily, defaultReadOptions, key.Bytes, key.ReadIndex, key.WriteIndex);
				return value != null ? ByteBuffer.Wrap(value) : null;
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void Remove(Transaction txn, ByteBuffer key) {
			((RocksDbTrans)txn).Remove(key.Copy(), columnFamily);
		}

		@Override
		public void Replace(Transaction txn, ByteBuffer key, ByteBuffer value) {
			((RocksDbTrans)txn).Put(key.Copy(), value.Copy(), columnFamily);
		}

		@Override
		public long Walk(TableWalkHandleRaw callback) {
			try (var it = rocksDb.newIterator(columnFamily, defaultReadOptions)) {
				long countWalked = 0;
				for (it.seekToFirst(); it.isValid(); it.next()) {
					countWalked++;
					if (!callback.handle(it.key(), it.value()))
						break;
				}
				return countWalked;
			}
		}

		@Override
		public long WalkKey(TableWalkKeyRaw callback) {
			try (var it = rocksDb.newIterator(columnFamily, defaultReadOptions)) {
				long countWalked = 0;
				for (it.seekToFirst(); it.isValid(); it.next()) {
					countWalked++;
					if (!callback.handle(it.key()))
						break;
				}
				return countWalked;
			}
		}
	}

	private static final class DVRocks extends DataWithVersion implements Zeze.Serialize.Serializable {
		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteByteBuffer(Data);
			bb.WriteLong(Version);
		}

		@Override
		public void Decode(ByteBuffer bb) {
			Data = ByteBuffer.Wrap(bb.ReadBytes());
			Version = bb.ReadLong();
		}

		byte[] Encode() {
			int size = Data.Size();
			var bb = ByteBuffer.Allocate(ByteBuffer.writeUIntSize(size) + size + ByteBuffer.writeLongSize(Version));
			Encode(bb);
			return bb.Bytes;
		}

		static DVRocks Decode(byte[] bytes) {
			var dv = new DVRocks();
			if (bytes != null)
				dv.Decode(ByteBuffer.Wrap(bytes));
			return dv;
		}
	}

	private final class OperatesRocksDb implements Operates {
		private final ColumnFamilyHandle columnFamily = getOrAddFamily("zeze.OperatesRocksDb.Schemas", null);

		@Override
		public synchronized DataWithVersion GetDataWithVersion(ByteBuffer key) {
			try {
				return DVRocks.Decode(rocksDb.get(columnFamily, key.Copy()));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public synchronized KV<Long, Boolean> SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			try {
				var dv = DVRocks.Decode(rocksDb.get(columnFamily, key.Copy()));
				if (dv.Version != version)
					return KV.Create(version, false);

				dv.Version = ++version;
				dv.Data = data;
				rocksDb.put(columnFamily, defaultWriteOptions, key.Copy(), dv.Encode());
				return KV.Create(version, true);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void SetInUse(int localId, String global) {
			// rocksdb 独占由它自己打开的时候保证。
		}

		@Override
		public int ClearInUse(int localId, String global) {
			// rocksdb 独占由它自己打开的时候保证。
			return 0;
		}
	}
}
