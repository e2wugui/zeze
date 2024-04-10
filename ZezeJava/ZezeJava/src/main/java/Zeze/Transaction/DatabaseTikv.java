package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import Zeze.Util.Task;
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
	private final TiConfiguration config;
	private final TiSession session;
	private final RawKVClient client;
	private final KVClient txnClient;
	private final boolean distTxn; // 是否启用分布式事务. 注意两种模式的数据不能互通
	private volatile long version; // only for distTxn

	public DatabaseTikv(Application zeze, Config.DatabaseConf conf) {
		super(zeze, conf);
		distTxn = conf.isDistTxn();
		if (distTxn) {
			config = TiConfiguration.createDefault(getDatabaseUrl());
			session = TiSession.create(config);
			client = null;
			txnClient = session.createKVClient();
			version = session.getTimestamp().getVersion();
		} else {
			config = TiConfiguration.createRawDefault(getDatabaseUrl());
			session = TiSession.create(config);
			client = session.createRawClient();
			txnClient = null;
		}
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesTikv());
	}

	@Override
	public AbstractKVTable openTable(String name, int id) {
		return new TikvTable(name);
	}

	@Override
	public Transaction beginTransaction() {
		return distTxn ? new TikvDistTrans() : new TikvTrans();
	}

	@Override
	public void close() {
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
		super.close();
	}

	private final class OperatesTikv implements Operates {
		private final AbstractKVTable table;

		public OperatesTikv() {
			var schemaTableName = "zeze.OperatesTikv.Schemas";
			table = openTable(schemaTableName, Bean.hash32(schemaTableName));
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			lock();
			try {
				var value = table.find(key);
				var dv = new DataWithVersion();
				if (value != null)
					dv.decode(value);
				if (dv.version != version)
					return KV.create(version, false);

				dv.version = ++version;
				dv.data = data;
				var bb = ByteBuffer.Allocate(5 + 9 + dv.data.size());
				dv.encode(bb);
				try (var txn = beginTransaction()) {
					table.replace(txn, key, bb);
					txn.commit();
				} catch (Exception e) {
					Task.forceThrow(e);
				}
				return KV.create(version, true);
			} finally {
				unlock();
			}
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			lock();
			try {
				var dv = new DataWithVersion();
				var value = table.find(key);
				if (value != null)
					dv.decode(value);
				return dv;
			} finally {
				unlock();
			}
		}

		@Override
		public void setInUse(int localId, String global) {
		}

		@Override
		public int clearInUse(int localId, String global) {
			return 0;
		}
	}

	private final class TikvTable extends AbstractKVTable {
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
		public void close() {
		}

		@Override
		public ByteBuffer find(ByteBuffer key) {
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
		public void remove(Transaction t, ByteBuffer key) {
			if (distTxn)
				((TikvDistTrans)t).delete(addKeyPrefixBB(key));
			else
				((TikvTrans)t).delete(addKeyPrefixBS(key));
		}

		@Override
		public void replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			if (distTxn)
				((TikvDistTrans)t).put(addKeyPrefixBB(key), value);
			else
				((TikvTrans)t).put(addKeyPrefixBS(key), ByteString.copyFrom(value.Bytes, value.ReadIndex, value.size()));
		}

		@Override
		public long walk(TableWalkHandleRaw callback) {
			long countWalked = 0;
			int keyPrefixSize = keyPrefix.length;
			var startKey = ByteString.copyFrom(keyPrefix);
			var endKey = Key.toRawKey(keyPrefix).nextPrefix().toByteString();
			Iterator<Kvrpcpb.KvPair> it;
			if (distTxn)
				it = new ConcreteScanIterator(config, session.getRegionStoreClientBuilder(), startKey, endKey, version);
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
		public long walkKey(TableWalkKeyRaw callback) {
			long countWalked = 0;
			int keyPrefixSize = keyPrefix.length;
			var startKey = ByteString.copyFrom(keyPrefix);
			var endKey = Key.toRawKey(keyPrefix).nextPrefix().toByteString();
			Iterator<Kvrpcpb.KvPair> it;
			if (distTxn)
				it = new ConcreteScanIterator(config, session.getRegionStoreClientBuilder(), startKey, endKey, version);
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

		@Override
		public long walkDesc(TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long walkKeyDesc(TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
			/*
			int keyPrefixSize = keyPrefix.length;
			var startKey = ByteString.copyFrom(keyPrefix);
			var endKey = Key.toRawKey(keyPrefix).nextPrefix().toByteString();
			Iterator<Kvrpcpb.KvPair> it;
			if (distTxn)
				it = new ConcreteScanIterator(config, session.getRegionStoreClientBuilder(), startKey, endKey, version);
			else
				it = client.scan0(startKey, endKey);

			// 参考rocksdb，看看tikv能不能支持it.seek.
			byte[] lastKey = null;
			while (it.hasNext()) {
				var kv = it.next();
				var value = kv.getValue();
				if (value.isEmpty()) // deleted
					continue;
				if (!callback.handle(kv.getKey().substring(keyPrefixSize).toByteArray(), value.toByteArray()))
					break;
			}
			//noinspection ConstantValue
			return null == lastKey ? null : ByteBuffer.Wrap(lastKey);
			*/
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		private ByteString addKeyPrefixBS(ByteBuffer key) {
			int keyPrefixSize = keyPrefix.length;
			int keySize = key.size();
			var tikvKey = new byte[keyPrefixSize + keySize];
			System.arraycopy(keyPrefix, 0, tikvKey, 0, keyPrefixSize);
			System.arraycopy(key.Bytes, key.ReadIndex, tikvKey, keyPrefixSize, keySize);
			return ByteString.copyFrom(tikvKey);
		}

		private ByteBuffer addKeyPrefixBB(ByteBuffer key) {
			int keyPrefixSize = keyPrefix.length;
			int keySize = key.size();
			var tikvKey = new byte[keyPrefixSize + keySize];
			System.arraycopy(keyPrefix, 0, tikvKey, 0, keyPrefixSize);
			System.arraycopy(key.Bytes, key.ReadIndex, tikvKey, keyPrefixSize, keySize);
			return ByteBuffer.Wrap(tikvKey);
		}
	}

	private final class TikvTrans implements Transaction {
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
		public void commit() {
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
		public void rollback() {
			if (datas != null)
				datas.clear();
			if (deleteKeys != null)
				deleteKeys.clear();
		}

		@Override
		public void close() {
		}
	}

	private final class TikvDistTrans implements Transaction {
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
		public void commit() {
			var es = datas.entrySet(); // 注意要求对es两次遍历的顺序一致
			var it = es.iterator();
			if (!it.hasNext())
				return;
			long ver = version;
			try (var tpc = new TwoPhaseCommitter(session, ver)) {
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

				ver = session.getTimestamp().getVersion();
				tpc.commitPrimaryKey(bo, pKey, ver);
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
				}, ver, 1000);
			} catch (Exception e) {
				Task.forceThrow(e);
			} finally {
				version = ver;
			}
		}

		@Override
		public void rollback() {
			if (datas != null)
				datas.clear();
		}

		@Override
		public void close() {
		}
	}
}
