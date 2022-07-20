package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import org.tikv.common.BytePairWrapper;
import org.tikv.common.ByteWrapper;
import org.tikv.common.TiConfiguration;
import org.tikv.common.TiSession;
import org.tikv.common.key.Key;
import org.tikv.common.operation.iterator.ConcreteScanIterator;
import org.tikv.common.util.ConcreteBackOffer;
import org.tikv.kvproto.Kvrpcpb;
import org.tikv.raw.RawKVClient;
import org.tikv.shade.com.google.protobuf.ByteString;
import org.tikv.txn.KVClient;
import org.tikv.txn.TwoPhaseCommitter;

public class DatabaseTikv extends Database {
	private final TiConfiguration conf;
	private final TiSession session;
	private final RawKVClient client;
	private final KVClient txnClient;
	private final boolean distTxn;
	private volatile long version;

	public DatabaseTikv(Config.DatabaseConf conf) {
		super(conf);
		distTxn = conf.isDistTxn();
		this.conf = distTxn
				? TiConfiguration.createDefault(conf.getDatabaseUrl())
				: TiConfiguration.createRawDefault(conf.getDatabaseUrl());
		session = TiSession.create(this.conf);
		if (distTxn) {
			client = null;
			txnClient = session.createKVClient();
			version = session.getTimestamp().getVersion();
		} else {
			client = session.createRawClient();
			txnClient = null;
		}
		setDirectOperates(new OperatesTikv());
	}

	@Override
	public TikvTable OpenTable(String name) {
		return new TikvTable(name);
	}

	@Override
	public Transaction BeginTransaction() {
		return distTxn ? new TikvDistTrans() : new TikvTrans();
	}

	@Override
	public void Close() {
		try {
			if (distTxn)
				txnClient.close();
			else
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

	public final class OperatesTikv implements Operates {
		private static final String name = "zeze.OperatesTikv.Schemas";
		private final Table table;

		public OperatesTikv() {
			table = OpenTable(name);
		}

		@Override
		public int ClearInUse(int localId, String global) {
			return 0;
		}

		@Override
		public synchronized KV<Long, Boolean> SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			var value = table.Find(key);
			var dv = new DVTikv();
			if (value != null)
				dv.Decode(value);
			if (dv.Version != version)
				return KV.Create(version, false);

			dv.Version = ++version;
			dv.Data = data;
			try (var txn = BeginTransaction()) {
				table.Replace(txn, key, ByteBuffer.Wrap(dv.Encode()));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return KV.Create(version, true);
		}

		@Override
		public synchronized DataWithVersion GetDataWithVersion(ByteBuffer key) {
			var dv = new DVTikv();
			var value = table.Find(key);
			if (value != null)
				dv.Decode(value);
			return dv;
		}

		@Override
		public void SetInUse(int localId, String global) {
		}
	}

	private static final class DVTikv extends DataWithVersion implements Zeze.Serialize.Serializable {
		public byte[] Encode() {
			int dataSize = Data.Size();
			var bb = ByteBuffer.Allocate(ByteBuffer.writeUIntSize(dataSize) + dataSize + ByteBuffer.writeLongSize(Version));
			Encode(bb);
			return bb.Bytes;
		}

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
	}

	public final class TikvTable implements Table {
		private final byte[] keyPrefix;

		@Override
		public DatabaseTikv getDatabase() {
			return DatabaseTikv.this;
		}

		// 这个属性用来忽略Schemas兼容判断的，而 tikv 不会在测试环境下使用，而且不能直接支持删表操作，所以总是返回false。
		// 即Schemas兼容总是不会被忽略。
		@Override
		public boolean isNew() {
			return false;
		}

		public TikvTable(String name) {
			var nameUtf8 = name.getBytes(StandardCharsets.UTF_8);
			keyPrefix = new byte[nameUtf8.length + 1];
			System.arraycopy(nameUtf8, 0, keyPrefix, 0, nameUtf8.length);
		}

		@Override
		public void Close() {
		}

		@Override
		public ByteBuffer Find(ByteBuffer key) {
			ByteString value;
			if (distTxn) {
				value = txnClient.get(addKeyPrefixBS(key), version);
				if (value == null)
					return null;
			} else {
				var result = client.get(addKeyPrefixBS(key));
				if (result.isEmpty())
					return null;
				value = result.get();
			}
			return value.isEmpty() ? null : ByteBuffer.Wrap(value.toByteArray());
		}

		@Override
		public void Remove(Transaction t, ByteBuffer key) {
			if (distTxn)
				((TikvDistTrans)t).delete(addKeyPrefixBB(key));
			else
				((TikvTrans)t).delete(addKeyPrefixBS(key));
		}

		@Override
		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			if (distTxn)
				((TikvDistTrans)t).put(addKeyPrefixBB(key), value);
			else
				((TikvTrans)t).put(addKeyPrefixBS(key), ByteString.copyFrom(value.Bytes, value.ReadIndex, value.Size()));
		}

		@Override
		public long Walk(TableWalkHandleRaw callback) {
			long countWalked = 0;
			int keyPrefixSize = keyPrefix.length;
			var startKey = ByteString.copyFrom(keyPrefix);
			var endKey = Key.toRawKey(keyPrefix).nextPrefix().toByteString();
			Iterator<Kvrpcpb.KvPair> it;
			if (distTxn)
				it = new ConcreteScanIterator(conf, session.getRegionStoreClientBuilder(), startKey, endKey, version);
			else
				it = client.scan0(startKey, endKey);
			while (it.hasNext()) {
				var kv = it.next();
				var value = kv.getValue();
				if (value.isEmpty()) // deleted
					continue;
				countWalked++;
				if (!callback.handle(kv.getKey().substring(keyPrefixSize).toByteArray(), value.toByteArray()))
					break;
			}
			return countWalked;
		}

		@Override
		public long WalkKey(TableWalkKeyRaw callback) {
			long countWalked = 0;
			int keyPrefixSize = keyPrefix.length;
			var startKey = ByteString.copyFrom(keyPrefix);
			var endKey = Key.toRawKey(keyPrefix).nextPrefix().toByteString();
			Iterator<Kvrpcpb.KvPair> it;
			if (distTxn)
				it = new ConcreteScanIterator(conf, session.getRegionStoreClientBuilder(), startKey, endKey, version);
			else
				it = client.scan0(startKey, endKey);
			while (it.hasNext()) {
				var kv = it.next();
				if (kv.getValue().isEmpty()) // deleted
					continue;
				countWalked++;
				if (!callback.handle(kv.getKey().substring(keyPrefixSize).toByteArray()))
					break;
			}
			return countWalked;
		}

		private ByteString addKeyPrefixBS(ByteBuffer key) {
			int keyPrefixSize = keyPrefix.length;
			int keySize = key.Size();
			var tikvKey = new byte[keyPrefixSize + keySize];
			System.arraycopy(keyPrefix, 0, tikvKey, 0, keyPrefixSize);
			System.arraycopy(key.Bytes, key.ReadIndex, tikvKey, keyPrefixSize, keySize);
			return ByteString.copyFrom(tikvKey);
		}

		private ByteBuffer addKeyPrefixBB(ByteBuffer key) {
			int keyPrefixSize = keyPrefix.length;
			int keySize = key.Size();
			var tikvKey = new byte[keyPrefixSize + keySize];
			System.arraycopy(keyPrefix, 0, tikvKey, 0, keyPrefixSize);
			System.arraycopy(key.Bytes, key.ReadIndex, tikvKey, keyPrefixSize, keySize);
			return ByteBuffer.Wrap(tikvKey);
		}
	}

	public final class TikvTrans implements Transaction {
		private Map<ByteString, ByteString> datas;
		private List<ByteString> deleteKeys;

		private Map<ByteString, ByteString> getDatas() {
			var d = datas;
			if (d == null)
				datas = d = new HashMap<>();
			return d;
		}

		private List<ByteString> getDeleteKeys() {
			var d = deleteKeys;
			if (d == null)
				deleteKeys = d = new ArrayList<>();
			return d;
		}

		public void put(ByteString key, ByteString value) {
			getDatas().put(key, value);
		}

		public void delete(ByteString key) {
			getDatas().put(key, ByteString.EMPTY);
			getDeleteKeys().add(key);
		}

		@Override
		public void Commit() {
			if (datas != null && !datas.isEmpty()) {
				//noinspection ConstantConditions
				client.batchPut(datas);
				datas.clear();
			}
			if (deleteKeys != null && !deleteKeys.isEmpty()) {
				//noinspection ConstantConditions
				client.batchDelete(deleteKeys);
				deleteKeys.clear();
			}
		}

		@Override
		public void Rollback() {
			if (datas != null)
				datas.clear();
			if (deleteKeys != null)
				deleteKeys.clear();
		}

		@Override
		public void close() {
		}
	}

	public final class TikvDistTrans implements Transaction {
		private Map<ByteBuffer, byte[]> datas;

		private Map<ByteBuffer, byte[]> getDatas() {
			var d = datas;
			if (d == null)
				datas = d = new HashMap<>();
			return d;
		}

		public void put(ByteBuffer key, ByteBuffer value) {
			getDatas().put(key, value.Copy());
		}

		public void delete(ByteBuffer key) {
			getDatas().put(key, ByteBuffer.Empty);
		}

		@Override
		public void Commit() {
			var es = datas.entrySet();
			var it = es.iterator();
			if (!it.hasNext())
				return;
			try (var tpc = new TwoPhaseCommitter(session, session.getTimestamp().getVersion())) {
				var bo = ConcreteBackOffer.newCustomBackOff(1000);
				var e = it.next();
				var pKey = e.getKey().Copy();
				tpc.prewritePrimaryKey(bo, pKey, e.getValue());
				tpc.prewriteSecondaryKeys(pKey, new Iterator<>() {
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public BytePairWrapper next() {
						var e = it.next();
						return new BytePairWrapper(e.getKey().Copy(), e.getValue());
					}
				}, 1000);

				long commitTS = session.getTimestamp().getVersion();
				tpc.commitPrimaryKey(bo, pKey, commitTS);
				var it2 = es.iterator();
				if (!it2.hasNext())
					throw new IllegalStateException(); // impossible
				it2.next(); // skip pKey
				tpc.commitSecondaryKeys(new Iterator<>() {
					@Override
					public boolean hasNext() {
						return it2.hasNext();
					}

					@Override
					public ByteWrapper next() {
						return new ByteWrapper(it2.next().getKey().Copy());
					}
				}, commitTS, 1000);
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				version = session.getTimestamp().getVersion();
			}
		}

		@Override
		public void Rollback() {
			if (datas != null)
				datas.clear();
		}

		@Override
		public void close() {
		}
	}
}
