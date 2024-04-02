package Zeze.Dbh2;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion;
import Zeze.Builtin.Dbh2.Master.ClearInUse;
import Zeze.Builtin.Dbh2.Master.GetDataWithVersion;
import Zeze.Builtin.Dbh2.Master.SaveDataWithSameVersion;
import Zeze.Builtin.Dbh2.Master.SetInUse;
import Zeze.Builtin.Dbh2.Master.TryLock;
import Zeze.Builtin.Dbh2.Master.UnLock;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Transaction.TableWalkKeyRaw;
import Zeze.Util.KV;
import Zeze.Util.TaskCompletionSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import Zeze.Builtin.Dbh2.Master.BSetInUse;

/**
 * 适配zeze-Database
 */
public class Database extends Zeze.Transaction.Database {
	private final String masterName;
	private final String databaseName;
	private final MasterAgent masterAgent;
	private final Dbh2AgentManager dbh2AgentManager;

	public Database(@Nullable Application zeze, Dbh2AgentManager dbh2AgentManager, Config.DatabaseConf conf) {
		super(zeze, conf);

		this.dbh2AgentManager = dbh2AgentManager;
		// dbh2://ip:port/databaseName?user=xxx&passwd=xxx
		try {
			var url = new URI(getDatabaseUrl());
			masterName = url.getHost() + "_" + url.getPort();
			databaseName = new java.io.File(url.getPath()).getName();
			if (databaseName.contains("@"))
				throw new RuntimeException("databaseName: '@' is reserve.");
			/*
			var query = HttpExchange.parseQuery(url.getQuery());
			user = query.get("user");
			passwd = query.get("passwd");
			*/
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesDbh2());

		masterAgent = dbh2AgentManager.openDatabase(masterName, databaseName);
	}

	private final class OperatesDbh2 implements Operates {
		@Override
		public void setInUse(int localId, String global) {
			var r = new SetInUse();
			r.Argument.setLocalId(localId);
			r.Argument.setGlobal(global);

			r.SendForWait(masterAgent.getService().GetSocket()).await();

			switch (IModule.getErrorCode(r.getResultCode())) {
			case BSetInUse.eSuccess:
				return; // success
			case BSetInUse.eDefaultError:
				throw new IllegalStateException("Unknown Error");
			case BSetInUse.eInstanceAlreadyExists:
				throw new IllegalStateException("Instance Exist.");
			case BSetInUse.eInsertInstanceError:
				throw new IllegalStateException("Insert LocalId Failed");
			case BSetInUse.eGlobalNotSame:
				throw new IllegalStateException("Global Not Equals");
			case BSetInUse.eInsertGlobalError:
				throw new IllegalStateException("Insert Global Failed");
			case BSetInUse.eTooManyInstanceWithoutGlobal:
				throw new IllegalStateException("Instance Greater Than One But No Global");
			default:
				throw new IllegalStateException("Unknown ReturnValue");
			}
		}

		@Override
		public int clearInUse(int localId, String global) {
			var r = new ClearInUse();
			r.Argument.setLocalId(localId);
			r.Argument.setGlobal(global);
			r.SendForWait(masterAgent.getService().GetSocket()).await();

			// clear: 不检查结果，直接返回
			return IModule.getErrorCode(r.getResultCode());
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			var r = new SaveDataWithSameVersion();
			r.Argument.setKey(new Binary(key));
			r.Argument.setData(new Binary(data));
			r.Argument.setVersion(version);
			r.SendForWait(masterAgent.getService().GetSocket()).await();
			var error = IModule.getErrorCode(r.getResultCode());
			switch (error) {
			case BSaveDataWithSameVersion.eSuccess:
				return KV.create(r.Result.getVersion(), true);
			case BSaveDataWithSameVersion.eVersionMismatch:
				return KV.create(0L, false);
			default:
				throw new RuntimeException("SaveDataWithSameVersion error=" + error);
			}
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			var r = new GetDataWithVersion();
			r.Argument.setKey(new Binary(key));
			r.SendForWait(masterAgent.getService().GetSocket()).await();
			if (r.getResultCode() != 0) {
				// skip error
				return null;
			}
			var result = new DataWithVersion();
			result.data = ByteBuffer.Wrap(r.Result.getData());
			result.version = r.Result.getVersion();
			return result;
		}

		@Override
		public boolean tryLock() {
			var r = new TryLock();
			r.SendForWait(masterAgent.getService().GetSocket()).await();
			if (r.getResultCode() != 0)
				logger.info("TryLock error={}", IModule.getErrorCode(r.getResultCode()));
			return r.getResultCode() == 0;
		}

		@Override
		public void unlock() {
			var r = new UnLock();
			r.SendForWait(masterAgent.getService().GetSocket()).await();
			if (r.getResultCode() != 0)
				logger.warn("UnLock error={}", IModule.getErrorCode(r.getResultCode()));
		}
	}

	private final ConcurrentHashMap<String, Dbh2Table> tables = new ConcurrentHashMap<>();

	@Override
	public Table openTable(String name, int id) {
		if (name.contains("@"))
			throw new RuntimeException("'@' is reserve.");

		var special = "___"; // prefix table 使用这个最后
		var idx = name.indexOf(special);
		if (idx >= 0) {
			var tableName = name.substring(idx + special.length());
			return new Dbh2PrefixTable(tables.computeIfAbsent(tableName, Dbh2Table::new), id);
		}
		// 这里不使用tables，保留的旧的逻辑不变。
		return new Dbh2Table(name);
	}

	@Override
	public Transaction beginTransaction() {
		return new Dbh2Transaction();
	}

	private static ByteBuffer removePrefix(@Nullable ByteBuffer key) {
		if (null == key)
			return null;
		key.ReadIndex += Dbh2PrefixTable.TABLE_PREFIX;
		return key;
	}

	public class Dbh2PrefixTable extends AbstractKVTable {
		private static final int TABLE_PREFIX = 4; // sizeof(int)

		private final Dbh2Table table;
		private final byte[] prefix = new byte[TABLE_PREFIX];

		public Dbh2PrefixTable(Dbh2Table table, int id) {
			this.table = table;
			table.addRef();
			ByteBuffer.intLeHandler.set(this.prefix, 0, id);
		}

		@Override
		public int keyOffsetInRawKey() {
			return TABLE_PREFIX;
		}

		@Override
		public boolean isNew() {
			return false;
		}

		@Override
		public Zeze.Transaction.@NotNull Database getDatabase() {
			return Database.this;
		}

		@Override
		public void close() {
			table.decRef();
		}

		private ByteBuffer addPrefix(@Nullable ByteBuffer key) {
			if (null == key)
				return ByteBuffer.Wrap(prefix);
			var prefixKey = ByteBuffer.Allocate(TABLE_PREFIX + key.size());
			prefixKey.Append(prefix);
			prefixKey.Append(key.Bytes, key.ReadIndex, key.size());
			return prefixKey;
		}

		@Override
		public @Nullable ByteBuffer find(@NotNull ByteBuffer key) {
			return table.find(addPrefix(key));
		}

		@Override
		public void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			var txn = (Dbh2Transaction)t;
			txn.replace(table.getName(), addPrefix(key), value);
		}

		@Override
		public void remove(@NotNull Transaction t, @NotNull ByteBuffer key) {
			var txn = (Dbh2Transaction)t;
			txn.remove(table.getName(), addPrefix(key));
		}

		@Override
		public long walk(@NotNull TableWalkHandleRaw callback) {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName,
					table.getName(), callback, false, prefix);
		}

		@Override
		public long walkKey(@NotNull TableWalkKeyRaw callback) {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName,
					table.getName(), callback, false, prefix);
		}

		@Override
		public long walkDesc(@NotNull TableWalkHandleRaw callback) {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName,
					table.getName(), callback, true, prefix);
		}

		@Override
		public long walkKeyDesc(@NotNull TableWalkKeyRaw callback) {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName,
					table.getName(), callback, true, prefix);
		}

		// 【注意】
		// 由于prefix使用了exclusiveStartKey的api实现，
		// 所以如果Dbh2PrefixTable的key是空的，那么遍历的时候，这条记录会被忽略。
		// 目前zeze不会使用空的key，所以没问题。

		@Override
		public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey,
										 int proposeLimit, @NotNull TableWalkHandleRaw callback) {
			return removePrefix(dbh2AgentManager.walk(masterAgent, masterName, databaseName, table.getName(),
					addPrefix(exclusiveStartKey), proposeLimit, callback, false, prefix));
		}

		@Override
		public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey,
											int proposeLimit, @NotNull TableWalkKeyRaw callback) {
			return removePrefix(dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, table.getName(),
					addPrefix(exclusiveStartKey), proposeLimit, callback, false, prefix));
		}

		@Override
		public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey,
											 int proposeLimit, @NotNull TableWalkHandleRaw callback) {
			return removePrefix(dbh2AgentManager.walk(masterAgent, masterName, databaseName, table.getName(),
					addPrefix(exclusiveStartKey), proposeLimit, callback, true, prefix));
		}

		@Override
		public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey,
												int proposeLimit, @NotNull TableWalkKeyRaw callback) {
			return removePrefix(dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, table.getName(),
					addPrefix(exclusiveStartKey), proposeLimit, callback, true, prefix));
		}
	}

	public class Dbh2Transaction implements Zeze.Transaction.Database.Transaction {
		private final BPrepareBatches.Data batches = new BPrepareBatches.Data();

		public void commitBreakAfterPrepareForDebugOnly() {
			dbh2AgentManager.commitBreakAfterPrepareForDebugOnly(batches);
		}

		@Override
		public void commit() {
			dbh2AgentManager.commit(batches);
		}

		public void replace(String tableName, ByteBuffer key, ByteBuffer value) {
			if (value.size() <= 0)
				throw new RuntimeException("value.size <= 0.");
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			var bValue = new Binary(value.Bytes, value.ReadIndex, value.size());
			var agent = dbh2AgentManager.locateBucket(masterAgent, masterName, databaseName, tableName, bKey);
			var batch = batches.getDatas().computeIfAbsent(agent,
					_agent_ -> new BPrepareBatch.Data(masterName, databaseName, tableName, null));
			batch.getBatch().getPuts().put(bKey, bValue);
		}

		public void remove(String tableName, ByteBuffer key) {
			var bKey = new Binary(key.Bytes, key.ReadIndex, key.size());
			var agent = dbh2AgentManager.locateBucket(masterAgent, masterName, databaseName, tableName, bKey);
			var batch = batches.getDatas().computeIfAbsent(agent,
					_agent_ -> new BPrepareBatch.Data(masterName, databaseName, tableName, null));
			batch.getBatch().getDeletes().add(bKey);
		}

		@Override
		public void rollback() {
		}

		@Override
		public void close() throws Exception {
		}
	}

	public class Dbh2Table extends Zeze.Transaction.Database.AbstractKVTable {
		private final String name;
		private boolean isNew;
		private final TaskCompletionSource<Integer> ready = new TaskCompletionSource<>();
		private final AtomicInteger ref = new AtomicInteger();

		public void addRef() {
			ref.incrementAndGet();
		}

		public void decRef() {
			if (ref.decrementAndGet() == 0) {
				close();
			}
		}

		public Dbh2Table(String tableName) {
			this.name = tableName;
			dbh2AgentManager.createTableAsync(
					Database.this.masterAgent, Database.this.masterName,
					Database.this.databaseName, tableName,
					(rc, _isNew) -> {
						isNew = _isNew;
						if (rc == 0)
							ready.setResult(0);
						else
							ready.setException(new RuntimeException("rc=" + rc));
					});
		}

		@Override
		public void waitReady() {
			ready.await();
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
			// 最多执行两次。
			for (int i = 0; i < 2; ++i) {
				var raft = dbh2AgentManager.locateBucket(
						Database.this.masterAgent, Database.this.masterName,
						Database.this.databaseName, name,
						bKey);
				var agent = dbh2AgentManager.openBucket(raft);
				var kv = agent.get(Database.this.databaseName, name, bKey);
				if (kv.getKey())
					return kv.getValue();

				// miss match bucket
				dbh2AgentManager.reload(
						Database.this.masterAgent, Database.this.masterName,
						Database.this.databaseName, name);
			}
			throw new RuntimeException("fail too many try.");
		}

		@Override
		public void replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			var txn = (Dbh2Transaction)t;
			try {
				txn.replace(name, key, value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void remove(Transaction t, ByteBuffer key) {
			var txn = (Dbh2Transaction)t;
			try {
				txn.remove(name, key);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public long walk(TableWalkHandleRaw callback) {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name, callback, false, null);
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name, callback, false, null);
		}

		@Override
		public long walkDesc(TableWalkHandleRaw callback) {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name, callback, true, null);
		}

		@Override
		public long walkKeyDesc(TableWalkKeyRaw callback) {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name, callback, true, null);
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, false, null);
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, false, null);
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, true, null);
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, true, null);
		}

		@Override
		public void close() {
			tables.remove(name);
		}
	}

	public static class Dbh2Operates implements Zeze.Transaction.Database.Operates {
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
