package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.tikv.common.BytePairWrapper;
import org.tikv.common.ByteWrapper;
import org.tikv.common.TiConfiguration;
import org.tikv.common.TiSession;
import org.tikv.common.exception.RawCASConflictException;
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

	public DatabaseTikv(Application zeze, Config.DatabaseConf conf) {
		super(zeze, conf);
		distTxn = conf.isDistTxn();
		if (distTxn) {
			config = TiConfiguration.createDefault(getDatabaseUrl());
			session = TiSession.create(config);
			client = null;
			txnClient = session.createKVClient();
		} else {
			config = TiConfiguration.createRawDefault(getDatabaseUrl());
			session = TiSession.create(config);
			client = session.createRawClient();
			txnClient = null;
		}
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesTikv());
	}

	@Override
	public @NotNull AbstractKVTable openTable(@NotNull String name, int id) {
		return new TikvTable(name);
	}

	@Override
	public @NotNull Transaction beginTransaction() {
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
		private final TikvTable table;

		public OperatesTikv() {
			var schemaTableName = "Zeze_OperatesTikv_Schemas";
			table = new TikvTable(schemaTableName);
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key, @NotNull ByteBuffer data, long version) {
			lock();
			try {
				return distTxn ? saveDataWithSameVersionDistTxn(key, data, version)
						: saveDataWithSameVersionRaw(key, data, version);
			} finally {
				unlock();
			}
		}

		// raw 模式：读-判-写必须是服务端原子条件写（FND5-02，对齐 Mongo 判例）：原先 find 后
		// batchPut 裸写，tryLock 又默认恒真，同 serverId 双进程并发 schemasCompatible 时都基于
		// 版本 N 写 N+1，后写覆盖前写，checkCompatible 在陈旧快照上重复 renameTable 丢表。
		// compareAndSet 以完整旧值为条件原子替换（旧值null=仅键不存在时写入），冲突返回 false
		// 交给 Application.schemasCompatible 既有 CAS 重试环路。
		private KV<Long, Boolean> saveDataWithSameVersionRaw(
				@NotNull ByteBuffer key, @NotNull ByteBuffer data, long version) {
			var keyBs = table.prefixedKey(key);
			@SuppressWarnings("DataFlowIssue")
			var oldValue = client.get(keyBs);
			var oldRaw = oldValue.isEmpty() || oldValue.get().isEmpty() ? null : oldValue.get();
			if (oldRaw != null) {
				var oldDv = new DataWithVersion();
				oldDv.decode(ByteBuffer.Wrap(oldRaw.toByteArray()));
				if (oldDv.version != version)
					return KV.create(version, false);
			}

			var dv = new DataWithVersion();
			dv.version = version + 1;
			dv.data = data;
			var bb = ByteBuffer.Allocate(5 + 9 + dv.data.size());
			dv.encode(bb);
			var newBs = ByteString.copyFrom(bb.Bytes, bb.ReadIndex, bb.size());
			try {
				client.compareAndSet(keyBs, Optional.ofNullable(oldRaw), newBs);
			} catch (RawCASConflictException e) {
				return KV.create(version, false);
			}
			return KV.create(dv.version, true);
		}

		// distTxn 模式：读必须与提交共用同一 start_ts（FND5-02）：TiKV 乐观事务的冲突检测
		// 只覆盖事务 start_ts 内读过的 key，原先读用独立快照（现取 TSO）、提交事务另取新
		// start_ts，并发提交发生在两次取 ts 之间时 prewrite 检测不到，照样覆盖丢更新。
		// 这里 start_ts → 同 ts 快照读 → 同 start_ts 两阶段提交，标准 OCC。
		private KV<Long, Boolean> saveDataWithSameVersionDistTxn(
				@NotNull ByteBuffer key, @NotNull ByteBuffer data, long version) {
			var keyBs = table.prefixedKey(key);
			var startTs = session.getTimestamp().getVersion();
			@SuppressWarnings("DataFlowIssue")
			var oldRead = txnClient.get(keyBs, startTs);
			var oldRaw = oldRead == null || oldRead.isEmpty() ? null : oldRead;
			if (oldRaw != null) {
				var oldDv = new DataWithVersion();
				oldDv.decode(ByteBuffer.Wrap(oldRaw.toByteArray()));
				if (oldDv.version != version)
					return KV.create(version, false);
			}

			var dv = new DataWithVersion();
			dv.version = version + 1;
			dv.data = data;
			var bb = ByteBuffer.Allocate(5 + 9 + dv.data.size());
			dv.encode(bb);
			try (var txn = (TikvDistTrans)beginTransaction()) {
				table.replace(txn, key, bb);
				txn.commit(startTs);
			} catch (Exception e) {
				// 失败重读诊断（fresh TSO）：版本已变=并发 CAS 输家（含 primary 已提交、secondary
				// 应答丢失的半成情形），返回 false 由上层环路重读收敛；版本未变=瞬态/未知错误，
				// 维持抛出不吞真实故障。不按异常类型区分：2PC 冲突异常的包归属跨客户端版本不稳。
				var current = txnClient.get(keyBs, session.getTimestamp().getVersion());
				var curDv = new DataWithVersion();
				if (current != null && !current.isEmpty()) {
					curDv.decode(ByteBuffer.Wrap(current.toByteArray()));
					if (curDv.version != version)
						return KV.create(version, false);
				}
				throw Task.forceThrow(e);
			}
			return KV.create(dv.version, true);
		}

		@Override
		public DataWithVersion getDataWithVersion(@NotNull ByteBuffer key) {
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
		public void setInUse(int localId, @NotNull String global) {
		}

		@Override
		public int clearInUse(int localId, @NotNull String global) {
			return 0;
		}
	}

	private final class TikvTable extends AbstractKVTable {
		private final byte[] keyPrefix;
		private final String name;

		@Override
		public @NotNull DatabaseTikv getDatabase() {
			return DatabaseTikv.this;
		}

		// 这个属性用来忽略Schemas兼容判断的，而 tikv 不会在测试环境下使用，而且不能直接支持删表操作，所以总是返回false。
		// 即Schemas兼容总是不会被忽略。
		@Override
		public boolean isNew() {
			return false;
		}

		public TikvTable(String name) {
			this.name = name;
			var nameUtf8 = name.getBytes(StandardCharsets.UTF_8);
			keyPrefix = new byte[nameUtf8.length + 1];
			System.arraycopy(nameUtf8, 0, keyPrefix, 0, nameUtf8.length);
		}

		@Override
		public void close() {
		}

		@Override
		public ByteBuffer find(@NotNull ByteBuffer key) {
			checkKvKeyLength(name, key);
			ByteString value;
			if (distTxn) {
				// 快照读必须现取 TSO 时间戳：本进程缓存的快照永远看不到其他进程已提交的数据
				// （多进程接管记录时 cache miss 读到旧值，之后基于旧值覆盖写会丢失对方的更新）。
				// find 仅在本地 cache miss、记录失效重载、selectFromDatabase 时被调用，
				// 一次 TSO 往返（PD get_tso，无批量，约一次小 RPC）的代价换取读到最新已提交数据。
				value = txnClient.get(addKeyPrefixBS(key), session.getTimestamp().getVersion());
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
		public void remove(@NotNull Transaction t, @NotNull ByteBuffer key) {
			checkKvKeyLength(name, key);
			if (distTxn)
				((TikvDistTrans)t).delete(addKeyPrefixBB(key));
			else
				((TikvTrans)t).delete(addKeyPrefixBS(key));
		}

		@Override
		public void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			checkKvKeyLength(name, key);
			if (distTxn)
				((TikvDistTrans)t).put(addKeyPrefixBB(key), value);
			else
				((TikvTrans)t).put(addKeyPrefixBS(key), ByteString.copyFrom(value.Bytes, value.ReadIndex, value.size()));
		}

		@Override
		public long walk(@NotNull TableWalkHandleRaw callback) throws Exception {
			long countWalked = 0;
			int keyPrefixSize = keyPrefix.length;
			var startKey = ByteString.copyFrom(keyPrefix);
			var endKey = Key.toRawKey(keyPrefix).nextPrefix().toByteString();
			Iterator<Kvrpcpb.KvPair> it;
			if (distTxn) // 与 find 相同：现取 TSO 快照，保证看到其他进程已提交的数据
				it = new ConcreteScanIterator(config, session.getRegionStoreClientBuilder(), startKey, endKey,
						session.getTimestamp().getVersion());
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
		public long walkKey(@NotNull TableWalkKeyRaw callback) throws Exception {
			long countWalked = 0;
			int keyPrefixSize = keyPrefix.length;
			var startKey = ByteString.copyFrom(keyPrefix);
			var endKey = Key.toRawKey(keyPrefix).nextPrefix().toByteString();
			Iterator<Kvrpcpb.KvPair> it;
			if (distTxn) // 与 find 相同：现取 TSO 快照，保证看到其他进程已提交的数据
				it = new ConcreteScanIterator(config, session.getRegionStoreClientBuilder(), startKey, endKey,
						session.getTimestamp().getVersion());
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
		public long walkDesc(@NotNull TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long walkKeyDesc(@NotNull TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandleRaw callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkKeyRaw callback) {
			throw new UnsupportedOperationException();
		}

		// OperatesTikv 条件写用：带长度校验的完整 key（FND5-02）。
		ByteString prefixedKey(@NotNull ByteBuffer key) {
			checkKvKeyLength(name, key);
			return addKeyPrefixBS(key);
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
			commit(session.getTimestamp().getVersion());
		}

		// startTs 由调用方提供时必须是刚从 TSO 现取的（FND5-02：OperatesTikv 的快照读与提交
		// 共用同一 start_ts，冲突检测才覆盖该读）；无参 commit() 维持内部现取的既有语义。
		void commit(long startTs) {
			if (datas == null || datas.isEmpty())
				return;
			var es = datas.entrySet(); // 注意要求对es两次遍历的顺序一致
			var it = es.iterator();
			if (!it.hasNext())
				return;
			// start_ts 必须每个事务从 TSO 现取（全局唯一），不能使用共享的 version：
			// 并发 flush（MultiThreadMerge+parallelStream）下两个事务会拿到同一个 start_ts，
			// TiKV 以 start_ts 标识事务（MVCC 可见性、resolveLock 锁归属），共用会导致
			// 跨事务写入被混同甚至被另一事务的锁恢复误回滚，原子性破坏。
			long ver = startTs;
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
				throw Task.forceThrow(e);
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
