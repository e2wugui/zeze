package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import org.tikv.common.TiConfiguration;
import org.tikv.common.TiSession;
import org.tikv.common.key.Key;
import org.tikv.kvproto.Kvrpcpb;
import org.tikv.raw.RawKVClient;
import org.tikv.shade.com.google.protobuf.ByteString;

public class DatabaseTikv extends Database {
	public static final int PAGE_SIZE = 512;

	private final TiSession session;
	private final RawKVClient client;

	public DatabaseTikv(Config.DatabaseConf conf) {
		super(conf);
		TiConfiguration tiConf = TiConfiguration.createRawDefault(conf.getDatabaseUrl());
		session = TiSession.create(tiConf);
		client = session.createRawClient();
		setDirectOperates(new OperatesTikv(this));
	}

	public static final class OperatesTikv implements Operates {
		private static final String name = "zeze.OperatesTikv.Schemas";

		private final DatabaseTikv DatabaseReal;
		private final Table table;

		public OperatesTikv(DatabaseTikv database) {
			DatabaseReal = database;
			table = database.OpenTable(name);
		}

		public DatabaseTikv getDatabase() {
			return DatabaseReal;
		}

		@Override
		public int ClearInUse(int localId, String global) {
			return 0;
		}

		@Override
		public synchronized KV<Long, Boolean> SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			ByteBuffer find = table.Find(key);
			DVTikv dv = new DVTikv();
			if (find != null)
				dv.Decode(find);
			if (dv.Version != version)
				return Zeze.Util.KV.Create(version, false);

			version++;
			dv.Version = version;
			dv.Data = data;
			try (var txn = getDatabase().BeginTransaction()) {
				table.Replace(txn, key, ByteBuffer.Wrap(dv.Encode()));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return Zeze.Util.KV.Create(version, true);
		}

		@Override
		public synchronized DataWithVersion GetDataWithVersion(ByteBuffer key) {
			ByteBuffer find = table.Find(key);
			DVTikv dv = new DVTikv();
			if (find != null)
				dv.Decode(find);
			return dv;
		}

		private static class DVTikv extends DataWithVersion implements Zeze.Serialize.Serializable {
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

			public static DVTikv Decode(byte[] bytes) {
				var dv = new DVTikv();
				if (bytes != null)
					dv.Decode(ByteBuffer.Wrap(bytes));
				return dv;
			}

			public final byte[] Encode() {
				int dataSize = Data.Size();
				var bb = ByteBuffer.Allocate(ByteBuffer.writeUIntSize(dataSize) + dataSize + ByteBuffer.writeLongSize(Version));
				Encode(bb);
				return bb.Bytes;
			}
		}

		@Override
		public void SetInUse(int localId, String global) {
		}
	}

	@Override
	public void Close() {
		try {
			client.close();
		} catch (Exception e) {
			logger.error("", e);
		}
		try {
			session.close();
		} catch (Exception e) {
			logger.error("", e);
		}
		super.Close();
	}

	public static class TikvTrans implements Transaction {
		private final DatabaseTikv database;
		private final Map<ByteString, ByteString> datas;
		private final List<ByteString> deleteKeys;

		public TikvTrans(DatabaseTikv database) {
			this.database = database;
			datas = new ConcurrentHashMap<>();
			deleteKeys = new CopyOnWriteArrayList<>();
		}

		public void put(ByteString key, ByteString value) {
			datas.put(key, value);
		}

		public void delete(ByteString key) {
			deleteKeys.add(key);
		}

		@Override
		public final void Commit() {
			if (!datas.isEmpty()) {
				database.client.batchPut(datas);
				datas.clear();
			}
			if (!deleteKeys.isEmpty()) {
				database.client.batchDelete(deleteKeys);
				deleteKeys.clear();
			}
		}

		@Override
		public final void Rollback() {
			datas.clear();
			deleteKeys.clear();
		}

		@Override
		public final void close() {
		}
	}

	@Override
	public Transaction BeginTransaction() {
		return new TikvTrans(this);
	}

	@Override
	public Table OpenTable(String name) {
		return new TikvTable(this, name);
	}

	public static final class TikvTable implements Table {
		private final DatabaseTikv database;
		private final String name;
		private final ByteBuffer keyPrefix;

		@Override
		public DatabaseTikv getDatabase() {
			return database;
		}

		public String getName() {
			return name;
		}

		// 这个属性用来忽略Schemas兼容判断的，而 tikv 不会在测试环境下使用，而且不能直接支持删表操作，所以总是返回false。
		// 即Schemas兼容总是不会被忽略。
		@Override
		public boolean isNew() {
			return false;
		}

		public TikvTable(DatabaseTikv database, String name) {
			this.database = database;
			this.name = name;
			var nameUtf8 = name.getBytes(StandardCharsets.UTF_8);
			keyPrefix = ByteBuffer.Allocate(nameUtf8.length + 1);
			keyPrefix.Append(nameUtf8);
			keyPrefix.WriteByte((byte)0);
		}

		@Override
		public void Close() {
		}

		@Override
		public ByteBuffer Find(ByteBuffer key) {
			var result = database.client.get(withKeySpace(keyPrefix, key));
			return result.isPresent() ? ByteBuffer.Wrap(result.get().toByteArray()) : null;
		}

		@Override
		public void Remove(Transaction t, ByteBuffer key) {
			TikvTrans trans = ((TikvTrans)t);
			trans.delete(withKeySpace(keyPrefix, key));
		}

		@Override
		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			TikvTrans trans = ((TikvTrans)t);
			trans.put(withKeySpace(keyPrefix, key), ByteString.copyFrom(value.Bytes, value.ReadIndex, value.Size()));
		}

		@Override
		public long Walk(TableWalkHandleRaw callback) {
			long countWalked = 0;
			ByteString startKey = ByteString.copyFrom(keyPrefix.Bytes, keyPrefix.ReadIndex, keyPrefix.Size());
			int length = name.getBytes(StandardCharsets.UTF_8).length + 1;
			final ByteString endKey = Key.toRawKey(startKey).nextPrefix().toByteString();
			Key maxKey = Key.MIN;
			while (true) {
				List<Kvrpcpb.KvPair> kvPairs = database.client.scan(startKey, endKey, PAGE_SIZE);
				countWalked += kvPairs.size();
				for (Kvrpcpb.KvPair pair : kvPairs) {
					ByteString key = pair.getKey();
					ByteString value = pair.getValue();
					if (!callback.handle(key.substring(length).toByteArray(), value.toByteArray())) {
						return countWalked;
					}
					Key currentKey = Key.toRawKey(key);
					if (currentKey.compareTo(maxKey) > 0) {
						maxKey = currentKey;
					}
				}
				if (kvPairs.size() < PAGE_SIZE) {
					return countWalked;
				}
				startKey = maxKey.next().toByteString();
			}
		}

		@Override
		public long WalkKey(TableWalkKeyRaw callback) {
			long countWalked = 0;
			ByteString startKey = ByteString.copyFrom(keyPrefix.Bytes, keyPrefix.ReadIndex, keyPrefix.Size());
			int length = name.getBytes(StandardCharsets.UTF_8).length + 1;
			final ByteString endKey = Key.toRawKey(startKey).nextPrefix().toByteString();
			Key maxKey = Key.MIN;
			while (true) {
				List<Kvrpcpb.KvPair> kvPairs = database.client.scan(startKey, endKey, PAGE_SIZE);
				countWalked += kvPairs.size();
				for (Kvrpcpb.KvPair pair : kvPairs) {
					ByteString key = pair.getKey();
					if (!callback.handle(key.substring(length).toByteArray())) {
						return countWalked;
					}
					Key currentKey = Key.toRawKey(key);
					if (currentKey.compareTo(maxKey) > 0) {
						maxKey = currentKey;
					}
				}
				if (kvPairs.size() < PAGE_SIZE) {
					return countWalked;
				}
				startKey = maxKey.next().toByteString();
			}
		}

		private static ByteString withKeySpace(ByteBuffer keyPrefix, ByteBuffer key) {
			int keyPrefixSize = keyPrefix.Size();
			int keySize = key.Size();
			byte[] tikvKey = new byte[keyPrefixSize + keySize];
			System.arraycopy(keyPrefix.Bytes, keyPrefix.ReadIndex, tikvKey, 0, keyPrefixSize);
			System.arraycopy(key.Bytes, key.ReadIndex, tikvKey, keyPrefixSize, keySize);
			return ByteString.copyFrom(tikvKey);
		}
	}
}
