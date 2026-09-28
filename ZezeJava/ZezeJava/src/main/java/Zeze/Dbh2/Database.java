package Zeze.Dbh2;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Builtin.Dbh2.Master.BGetDataWithVersion;
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
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Transaction.TableWalkKeyRaw;
import Zeze.Util.KV;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
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
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesDbh2());

		masterAgent = dbh2AgentManager.openDatabase(masterName, databaseName);
	}

	private final class OperatesDbh2 implements Operates {
		@Override
		public void setInUse(int localId, @NotNull String global) {
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
		public int clearInUse(int localId, @NotNull String global) {
			var r = new ClearInUse();
			r.Argument.setLocalId(localId);
			r.Argument.setGlobal(global);
			r.SendForWait(masterAgent.getService().GetSocket()).await();

			// clear: 不检查结果，直接返回
			return IModule.getErrorCode(r.getResultCode());
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key, @NotNull ByteBuffer data, long version) {
			var r = new SaveDataWithSameVersion();
			r.Argument.setKey(new Binary(key));
			r.Argument.setData(new Binary(data));
			r.Argument.setVersion(version);
			r.SendForWait(masterAgent.getService().GetSocket()).await();
			var error = IModule.getErrorCode(r.getResultCode());
			return switch (error) {
				case BSaveDataWithSameVersion.eSuccess -> KV.create(r.Result.getVersion(), true);
				case BSaveDataWithSameVersion.eVersionMismatch -> KV.create(0L, false);
				default -> throw new RuntimeException("SaveDataWithSameVersion error=" + error);
			};
		}

		@Override
		public DataWithVersion getDataWithVersion(@NotNull ByteBuffer key) {
			var r = new GetDataWithVersion();
			r.Argument.setKey(new Binary(key));
			r.SendForWait(masterAgent.getService().GetSocket()).await();
			var error = IModule.getErrorCode(r.getResultCode());
			if (error == BGetDataWithVersion.eDataNotExists)
				return null;
			// 其他错误（超时/断连等瞬时故障）不能当作"无数据"返回null：
			// 调用方（Application全局数据初始化）会把null当首次初始化并以version=0写回，
			// 与Master端CAS叠加可把已存储的全局数据用默认值覆写。
			if (r.getResultCode() != 0)
				throw new RuntimeException("GetDataWithVersion error=" + error);
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
	public @NotNull Table openTable(@NotNull String name, int id) {
		if (name.contains("@"))
			throw new RuntimeException("'@' is reserve.");

		var special = "___"; // prefix table 使用这个最后
		var idx = name.indexOf(special);
		if (idx >= 0) {
			var tableName = name.substring(idx + special.length());
			return new Dbh2PrefixTable(tables.computeIfAbsent(tableName, __ -> new Dbh2Table(tableName, true)), id);
		}
		// 这里不使用tables。
		return new Dbh2Table(name, false);
	}

	@Override
	public @NotNull Transaction beginTransaction() {
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
		public long walk(@NotNull TableWalkHandleRaw callback) throws Exception {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName,
					table.getName(), callback, false, prefix);
		}

		@Override
		public long walkKey(@NotNull TableWalkKeyRaw callback) throws Exception {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName,
					table.getName(), callback, false, prefix);
		}

		@Override
		public long walkDesc(@NotNull TableWalkHandleRaw callback) throws Exception {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName,
					table.getName(), callback, true, prefix);
		}

		@Override
		public long walkKeyDesc(@NotNull TableWalkKeyRaw callback) throws Exception {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName,
					table.getName(), callback, true, prefix);
		}

		// 【注意】
		// 由于prefix使用了exclusiveStartKey的api实现，
		// 所以如果Dbh2PrefixTable的key是空的，那么遍历的时候，这条记录会被忽略。
		// 目前zeze不会使用空的key，所以没问题。

		@Override
		public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey,
										 int proposeLimit, @NotNull TableWalkHandleRaw callback) throws Exception {
			return removePrefix(dbh2AgentManager.walk(masterAgent, masterName, databaseName, table.getName(),
					addPrefix(exclusiveStartKey), proposeLimit, callback, false, prefix));
		}

		@Override
		public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey,
											int proposeLimit, @NotNull TableWalkKeyRaw callback) throws Exception {
			return removePrefix(dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, table.getName(),
					addPrefix(exclusiveStartKey), proposeLimit, callback, false, prefix));
		}

		@Override
		public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey,
											 int proposeLimit, @NotNull TableWalkHandleRaw callback) throws Exception {
			// desc空起点游标必须传null（walkPage转Binary.Empty）：addPrefix(null)=裸4字节prefix，
			// 服务端seekForPrev(裸prefix)落在目标前缀区间下方的异前缀key上，恒返回空。
			// 非空游标照旧加前缀（服务端seekForPrev全键定位）。
			return removePrefix(dbh2AgentManager.walk(masterAgent, masterName, databaseName, table.getName(),
					exclusiveStartKey != null ? addPrefix(exclusiveStartKey) : null, proposeLimit, callback, true, prefix));
		}

		@Override
		public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey,
												int proposeLimit, @NotNull TableWalkKeyRaw callback) throws Exception {
			// 同walkDesc：desc空起点游标传null，不走裸prefix；非空游标照旧加前缀。
			return removePrefix(dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, table.getName(),
					exclusiveStartKey != null ? addPrefix(exclusiveStartKey) : null, proposeLimit, callback, true, prefix));
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
		// 仅prefix路径通过tables.computeIfAbsent入表的实例为true；非prefix实例不入表，
		// close()不能删共享map条目（否则误摘仍被prefix表持有的实例，引用计数与map错位级联）。
		private final boolean registered;
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

		// 建表重试总预算：非final便于测试收缩。master/manager空窗（重启、扩容、选举）以十秒计，
		// 5分钟覆盖滚动重启窗口后仍有界。
		static volatile long createTableRetryBudgetMs = 5 * 60_000L;

		public Dbh2Table(String tableName, boolean registered) {
			this.name = tableName;
			this.registered = registered;
			// 第一次立即发起，重试才有延迟。
			createTableWithRetry(1_000, System.currentTimeMillis() + createTableRetryBudgetMs);
		}

		// 建表异步重试：eTableNotFound/eTooFewManager是master/manager空窗的暂时性失败
		//（master侧createTable幂等：存在即返回，重试无重复建桶副作用），1s起指数退避封顶30s，
		// 超总预算才setException；其他错误码（配置类，如eDatabaseNotFound）立即失败。
		// Procedure.Exception（-1）：master侧handler异常逃逸（createBucketRafts超时/建桶rpc失败等）
		// 被noProcedure派发层统一翻成的码——扩容滚动窗口的现实暂时性失败，码面无法区分暂时/永久，
		// 重试并以总预算兜底。
		private void createTableWithRetry(long retryDelayMs, long deadlineMs) {
			dbh2AgentManager.createTableAsync(
					Database.this.masterAgent, Database.this.masterName,
					Database.this.databaseName, name,
					(rc, _isNew) -> {
						if (rc == 0) {
							isNew = _isNew;
							ready.setResult(0);
							return;
						}
						if (rc != MasterAgent.eTableNotFound && rc != MasterAgent.eTooFewManager
								&& rc != (int)Procedure.Exception) {
							ready.setException(new RuntimeException("rc=" + rc));
							return;
						}
						if (System.currentTimeMillis() >= deadlineMs) {
							ready.setException(new RuntimeException("createTable retry budget exhausted: rc=" + rc));
							return;
						}
						TaskSpec.ofAction(() -> createTableWithRetry(
								Math.min(retryDelayMs * 2, 30_000L), deadlineMs)).schedule(retryDelayMs);
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
		public Zeze.Transaction.@NotNull Database getDatabase() {
			return Database.this;
		}

		@Override
		public ByteBuffer find(@NotNull ByteBuffer key) {
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
		public void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			var txn = (Dbh2Transaction)t;
			try {
				txn.replace(name, key, value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void remove(@NotNull Transaction t, @NotNull ByteBuffer key) {
			var txn = (Dbh2Transaction)t;
			try {
				txn.remove(name, key);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public long walk(@NotNull TableWalkHandleRaw callback) throws Exception {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name, callback, false, null);
		}

		@Override
		public long walkKey(@NotNull TableWalkKeyRaw callback) throws Exception {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name, callback, false, null);
		}

		@Override
		public long walkDesc(@NotNull TableWalkHandleRaw callback) throws Exception {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name, callback, true, null);
		}

		@Override
		public long walkKeyDesc(@NotNull TableWalkKeyRaw callback) throws Exception {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name, callback, true, null);
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandleRaw callback) throws Exception {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, false, null);
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkKeyRaw callback) throws Exception {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, false, null);
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandleRaw callback) throws Exception {
			return dbh2AgentManager.walk(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, true, null);
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkKeyRaw callback) throws Exception {
			return dbh2AgentManager.walkKey(masterAgent, masterName, databaseName, name,
					exclusiveStartKey, proposeLimit, callback, true, null);
		}

		@Override
		public void close() {
			if (registered)
				tables.remove(name);
		}
	}

	public static class Dbh2Operates implements Zeze.Transaction.Database.Operates {
		@Override
		public void setInUse(int localId, @NotNull String global) {
		}

		@Override
		public int clearInUse(int localId, @NotNull String global) {
			return 0;
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key, @NotNull ByteBuffer data, long version) {
			return null;
		}

		@Override
		public DataWithVersion getDataWithVersion(@NotNull ByteBuffer key) {
			return null;
		}
	}
}
