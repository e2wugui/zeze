package Zeze.Transaction;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import org.tikv.common.TiConfiguration;
import org.tikv.common.TiSession;
import org.tikv.common.key.Key;
import org.tikv.kvproto.Kvrpcpb;
import org.tikv.raw.RawKVClient;
import org.tikv.shade.com.google.protobuf.ByteString;
import org.tikv.shade.com.google.protobuf.Internal;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

	public final static class OperatesTikv implements Operates {
		private final DatabaseTikv DatabaseReal;
		private static final String name = "zeze.OperatesTikv.Schemas";
		private final Table table;

		public DatabaseTikv getDatabaseReal() {
			return DatabaseReal;
		}

		public Database getDatabase() {
			return getDatabaseReal();
		}

		public OperatesTikv(DatabaseTikv database) {
			DatabaseReal = database;
			table = database.OpenTable(name);
		}

		public int ClearInUse(int localId, String global) {
			return 0;
		}

		@Override
		public KV<Long, Boolean> SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			synchronized(this) {
				ByteBuffer find = table.Find(key);
				byte[] bs = find == null ? null : find.Copy();
				DVTikv dv = DVTikv.Decode(bs);
				if (dv.Version != version) {
					return Zeze.Util.KV.Create(version, false);
				}

				version++;
				dv.Version = version;
				dv.Data = data;
				table.Replace(getDatabase().BeginTransaction(), key, ByteBuffer.Wrap(dv.Encode()));
				return Zeze.Util.KV.Create(version, true);
			}
		}

		public DataWithVersion GetDataWithVersion(ByteBuffer key) {
			synchronized(this) {
				ByteBuffer find = table.Find(key);
				byte[] bs = find == null ? null : find.Copy();
				return DVTikv.Decode(bs);
			}
		}

		private static class DVTikv extends DataWithVersion implements Zeze.Serialize.Serializable {
			public final void Decode(ByteBuffer bb) {
				Data = ByteBuffer.Wrap(bb.ReadBytes());
				Version = bb.ReadLong();
			}

			public final void Encode(ByteBuffer bb) {
				bb.WriteByteBuffer(Data);
				bb.WriteLong(Version);
			}

			public static DVTikv Decode(byte[] bytes) {
				if (null == bytes) {
					return new DVTikv();
				}
				var dv = new DVTikv();
				dv.Decode(ByteBuffer.Wrap(bytes));
				return dv;
			}

			public final byte[] Encode() {
				var bb = ByteBuffer.Allocate();
				this.Encode(bb);
				return bb.Copy();
			}
		}


		public void SetInUse(int localId, String global) {

		}
	}

	@Override
	public void Close() {
		try {
			client.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
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


		public final void Commit() {
			if (!datas.isEmpty()) {
				database.client.batchPutAtomic(datas);
				datas.clear();
			}
			if (!deleteKeys.isEmpty()) {
				database.client.batchDeleteAtomic(deleteKeys);
				deleteKeys.clear();
			}
		}

		public final void Rollback() {
			datas.clear();
			deleteKeys.clear();
		}

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

	public final static class TikvTable implements Table {
		private final DatabaseTikv database;

		public DatabaseTikv getDatabaseReal() {
			return database;
		}

		public Database getDatabase() {
			return database;
		}

		private final String name;
		private final ByteBuffer keyPrefix;

		public String getName() {
			return name;
		}

		// 这个属性用来忽略Schemas兼容判断的，而 tikv 不会在测试环境下使用，而且不能直接支持删表操作，所以总是返回false。
		// 即Schemas兼容总是不会被忽略。
		public boolean isNew() {
			return false;
		}

		public TikvTable(DatabaseTikv database, String name) {
			this.database = database;
			this.name = name;
			var nameUtf8 = name.getBytes(StandardCharsets.UTF_8);
			keyPrefix = ByteBuffer.Allocate(nameUtf8.length + 1);
			keyPrefix.Append(nameUtf8);
			keyPrefix.WriteByte((byte) 0);
		}

		public void Close() {

		}

		public ByteBuffer Find(ByteBuffer key) {
			byte[] value;
			try {
				value = database.client.get(withKeySpace(keyPrefix, key)).toByteArray();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if (null == value || value == Internal.EMPTY_BYTE_ARRAY) {
				return null;
			}
			return ByteBuffer.Wrap(value);
		}

		public void Remove(Transaction t, ByteBuffer key) {
			TikvTrans trans = ((TikvTrans) t);
			trans.delete(withKeySpace(keyPrefix, key));
		}

		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			TikvTrans trans = ((TikvTrans) t);
			trans.put(withKeySpace(keyPrefix, key), ByteString.copyFrom(value.Copy()));
		}


		public long Walk(TableWalkHandleRaw callback) {
			long countWalked = 0;
			ByteString startKey = ByteString.copyFrom(keyPrefix.Copy());
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


		private static ByteString withKeySpace(ByteBuffer keyPrefix, ByteBuffer key) {
			int size = keyPrefix.Size();
			var tikvKey = ByteBuffer.Allocate(size + key.Size());
			tikvKey.Append(keyPrefix.Copy(), keyPrefix.ReadIndex, size);
			tikvKey.Append(key.Copy(), key.ReadIndex, key.Size());
			return ByteString.copyFrom(tikvKey.Copy());
		}
	}


}
