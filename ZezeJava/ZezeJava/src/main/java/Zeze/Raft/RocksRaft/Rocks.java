package Zeze.Raft.RocksRaft;

import java.util.Objects;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import Zeze.Config;
import Zeze.Raft.IRaftRpc;
import Zeze.Raft.LogSequence;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RaftLog;
import Zeze.Raft.UniqueRequestState;
import Zeze.Raft.RocksRaft.Log1.LogBinary;
import Zeze.Raft.RocksRaft.Log1.LogBool;
import Zeze.Raft.RocksRaft.Log1.LogByte;
import Zeze.Raft.RocksRaft.Log1.LogDouble;
import Zeze.Raft.RocksRaft.Log1.LogDecimal;
import Zeze.Raft.RocksRaft.Log1.LogFloat;
import Zeze.Raft.RocksRaft.Log1.LogInt;
import Zeze.Raft.RocksRaft.Log1.LogLong;
import Zeze.Util.AtomicOutputFile;
import Zeze.Raft.RocksRaft.Log1.LogShort;
import Zeze.Raft.RocksRaft.Log1.LogString;
import Zeze.Raft.RocksRaft.Log1.LogQuaternion;
import Zeze.Raft.RocksRaft.Log1.LogVector2;
import Zeze.Raft.RocksRaft.Log1.LogVector2Int;
import Zeze.Raft.RocksRaft.Log1.LogVector3;
import Zeze.Raft.RocksRaft.Log1.LogVector3Int;
import Zeze.Raft.RocksRaft.Log1.LogVector4;
import Zeze.Raft.Server;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Func3;
import Zeze.Util.FuncLong;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.RocksDatabase;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.rocksdb.BackupEngine;
import org.rocksdb.BackupEngineOptions;
import org.rocksdb.Env;
import org.rocksdb.RestoreOptions;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

/**
 * RocksDB 版 Raft 状态机：表模板与记录缓存、checkpoint→backup→zip 快照链、
 * flush 失败补偿（pendingFlush）与原子计数。
 */
public final class Rocks extends StateMachine implements Closeable {
	static final Logger logger = LogManager.getLogger(Rocks.class);
	static final boolean isDebugEnabled = logger.isDebugEnabled();

	/**
	 * flush 落盘失败（RocksDBException 的包装）：与不可恢复的解码/结构性错误区分，
	 * 供调用方登记 {@link #pendingFlushApplies} 并在后续仅重试 flush。
	 */
	public static final class FlushException extends RuntimeException {
		public FlushException(RocksDBException cause) {
			super(cause);
		}

		@Override
		public synchronized RocksDBException getCause() {
			return (RocksDBException)super.getCause();
		}
	}

	// 内存已应用但 flush 失败的待补偿记录（key = RaftLog.Index）：重试时跳过内存增量仅重刷盘；
	// 若同 index 的 term 变化（旧条目被截断），则丢弃并驱逐受污染缓存记录。
	private final LongConcurrentHashMap<PendingFlush> pendingFlushApplies = new LongConcurrentHashMap<>();

	record PendingFlush(long term, List<Record<?>> records) {
	}

	// 取出 index 对应的待补偿记录；不存在或 term 不匹配（旧条目被截断）时返回 null。
	@Nullable List<Record<?>> takePendingFlush(long index, long term) {
		var pending = pendingFlushApplies.remove(index);
		if (pending != null && pending.term == term)
			return pending.records;
		if (pending != null) {
			// term 不匹配：释放补偿持有的在用计数，并驱逐可能已被截断条目污染的缓存记录。
			for (var r : pending.records) {
				r.endAccess();
				r.evictPolluted();
			}
		}
		return null;
	}

	void putPendingFlush(long index, long term, List<Record<?>> records) {
		putPendingFlush(index, term, records, true);
	}

	/**
	 * 登记待重试 flush 的记录。
	 * {@code addReference=false} 表示转移调用方已持有的在用计数（避免先释放再登记的归零间隙）；
	 * {@code addReference=true} 表示额外加计（leader 首次失败时与 perform 收尾释放配对）。
	 */
	void putPendingFlush(long index, long term, List<Record<?>> records, boolean addReference) {
		pendingFlushApplies.put(index, new PendingFlush(term, records));
		if (addReference) {
			for (var r : records)
				r.beginAccess();
		}
	}

	// 应用完成水位（index + term）的单行键：与业务数据及 unique 终态存根同批原子提交，供快照 fence 与增量恢复读取。
	private static final byte[] appliedWatermarkKey = {0};

	@Override
	public @Nullable AppliedWatermark readAppliedWatermark() throws RocksDBException {
		//noinspection DataFlowIssue
		var bytes = appliedWatermarkTable.get(RocksDatabase.getDefaultReadOptions(),
				appliedWatermarkKey, 0, appliedWatermarkKey.length);
		if (bytes == null)
			return null;
		var bb = ByteBuffer.Wrap(bytes);
		return new AppliedWatermark(bb.ReadLong(), bb.ReadLong());
	}

	@Override
	public boolean hasAtomicAppliedState() {
		return true;
	}

	private void putAppliedWatermark(RocksDatabase.Batch batch, RaftLog raftLog) throws RocksDBException {
		var value = ByteBuffer.Allocate(16);
		value.WriteLong(raftLog.getIndex());
		value.WriteLong(raftLog.getTerm());
		//noinspection DataFlowIssue
		appliedWatermarkTable.put(batch, appliedWatermarkKey, 0, appliedWatermarkKey.length,
				value.Bytes, 0, value.WriteIndex);
	}

	// unique 终态存根按天分表的公共前缀（不依赖节点名以兼容 InstallSnapshot），与业务数据同批提交。
	private static final String uniqueAppliedTablePrefix = "Zeze.Raft.RocksRaft.UniqueApplied";

	private static String uniqueAppliedTableName(long createTime) {
		return LogSequence.makeUniqueRequestTableName(uniqueAppliedTablePrefix,
				LogSequence.toUniqueRequestKey(new Date(createTime)));
	}

	// 开库及快照恢复时，将旧节点前缀的终态存根表迁移到公共命名空间。
	private void migrateUniqueAppliedTables() throws RocksDBException {
		//noinspection DataFlowIssue
		for (var tableName : new ArrayList<>(storage.getTableMap().keySet())) {
			if (tableName.startsWith(uniqueAppliedTablePrefix + ".unique."))
				continue;
			var dateIndex = tableName.lastIndexOf(".unique.");
			if (dateIndex <= 0)
				continue;
			var date = LogSequence.parseUniqueRequestDate(tableName.substring(0, dateIndex), tableName);
			if (date == null)
				continue;
			var source = storage.getTableMap().get(tableName);
			var target = Objects.requireNonNull(storage).getOrAddTable(uniqueAppliedTableName(date.getTime()));
			try (var it = source.iterator(); var batch = storage.borrowBatch()) {
				batch.clear();
				for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next()) {
					var key = it.key();
					// 中断后的迁移可重入；已有公共存根可能更新，不能被旧表覆盖。
					if (target.get(key) == null)
						target.put(batch, key, it.value());
					if (batch.getCount() >= 1024) {
						batch.commit(RocksDatabase.getSyncWriteOptions());
						batch.clear();
					}
				}
				it.status(); // 迭代读失败不能被当作扫描结束，否则drop会丢失未读存根。
				if (batch.getCount() > 0)
					batch.commit(RocksDatabase.getSyncWriteOptions());
			}
			// 复制批次同步落盘且迭代器关闭后才删旧CF，崩溃前后至少保留一份完整结果。
			storage.dropTable(tableName);
		}
	}

	private void putUniqueAppliedStub(RocksDatabase.Batch batch, RaftLog raftLog) throws RocksDBException {
		var key = ByteBuffer.Allocate(32);
		//noinspection DataFlowIssue
		raftLog.getLog().getUnique().encode(key);
		var value = ByteBuffer.Allocate(32);
		new UniqueRequestState(raftLog, true).encode(value);
		Objects.requireNonNull(storage).getOrAddTable(uniqueAppliedTableName(raftLog.getLog().getCreateTime()))
				.put(batch, key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);
	}

	/** 终态存根查询（LogSequence.tryGetRequestState双读的状态机侧）。表不存在返回null。 */
	@Override
	public @Nullable UniqueRequestState getUniqueAppliedStub(IRaftRpc raftRpc) throws RocksDBException {
		//noinspection DataFlowIssue
		var table = storage.getTableMap().get(uniqueAppliedTableName(raftRpc.getCreateTime())); // 只读查询不建CF
		if (table == null)
			return null;
		var key = ByteBuffer.Allocate(32);
		raftRpc.getUnique().encode(key);
		var bytes = table.get(RocksDatabase.getDefaultReadOptions(), key.Bytes, 0, key.WriteIndex);
		if (bytes == null)
			return null;
		var state = new UniqueRequestState();
		state.decode(ByteBuffer.Wrap(bytes));
		return state;
	}

	// 按日期分桶 TTL 清理过期的终态存根表。
	@Override
	public void removeExpiredUniqueStubTables(long expired) throws Exception {
		//noinspection DataFlowIssue
		for (var tableName : storage.getTableMap().keySet()) {
			var date = LogSequence.parseUniqueRequestDate(uniqueAppliedTablePrefix, tableName);
			if (date == null || date.getTime() >= expired)
				continue;
			var table = storage.getTableMap().get(tableName);
			if (table != null)
				table.drop();
		}
	}

	public static void registerLog(Supplier<Log> s) {
		Log.register(s);
	}

	static {
		// 生成器仅补集合与BeanKey的具体类型；内建字段日志在这里统一注册。
		Log.register(LogBool::new);
		Log.register(LogByte::new);
		Log.register(LogShort::new);
		Log.register(LogInt::new);
		Log.register(LogLong::new);
		Log.register(LogFloat::new);
		Log.register(LogDouble::new);
		Log.register(LogString::new);
		Log.register(LogBinary::new);
		Log.register(LogDecimal::new);
		Log.register(LogVector2::new);
		Log.register(LogVector2Int::new);
		Log.register(LogVector3::new);
		Log.register(LogVector3Int::new);
		Log.register(LogVector4::new);
		Log.register(LogQuaternion::new);
		Log.register(LogBean::new);
		// Log1.LogBeanKey 在生成代码里面注册。
		// LogSet1<V> LogMap1<K,V> LogMap2<K,V> 在生成代码里面注册。
	}

	private final ConcurrentHashMap<String, TableTemplate<?, ? extends Bean>> tableTemplates = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Table<?, ? extends Bean>> tables = new ConcurrentHashMap<>();
	private final LongConcurrentHashMap<AtomicLong> atomicLongs = new LongConcurrentHashMap<>();
	private final IntHashMap<Long> lastUpdated = new IntHashMap<>();
	private final WriteOptions writeOptions;
	private final RocksMode rocksMode;
	private @Nullable RocksDatabase storage;
	private RocksDatabase.@Nullable Table atomicLongsTable;
	// 单行应用完成水位表，与业务数据及 unique 终态存根同批提交，供快照 fence 与重启增量恢复校验。
	private RocksDatabase.@Nullable Table appliedWatermarkTable;
	private final Lock mutex = new ReentrantLock();

	public Rocks() throws Exception {
		this(null, RocksMode.Pessimism, null, null);
	}

	public Rocks(@Nullable String raftName) throws Exception {
		this(raftName, RocksMode.Pessimism, null, null);
	}

	public Rocks(@Nullable String raftName, @Nullable RaftConfig raftConfig) throws Exception {
		this(raftName, RocksMode.Pessimism, raftConfig, null);
	}

	public Rocks(@Nullable String raftName, @Nullable RaftConfig raftConfig, @Nullable Config config) throws Exception {
		this(raftName, RocksMode.Pessimism, raftConfig, config);
	}

	private static RocksMode validateMode(@Nullable RocksMode mode) {
		if (mode == RocksMode.Pessimism)
			return mode;
		if (mode == null)
			throw new NullPointerException("RocksMode must not be null (only RocksMode.Pessimism is supported)");
		throw new UnsupportedOperationException(
				"RocksMode." + mode + " is not implemented and is deprecated; "
						+ "use RocksMode.Pessimism with explicit PessimismLock (Transaction.addPessimismLock)");
	}

	/**
	 * 使用指定并发模式创建 RocksRaft 状态机。
	 *
	 * @param mode 并发模式，当前仅支持 {@link RocksMode#Pessimism}
	 * @throws NullPointerException {@code mode} 为 {@code null} 时在创建任何资源前立即抛出
	 * @throws UnsupportedOperationException {@code mode} 不是 {@link RocksMode#Pessimism}（例如已废弃的
	 *                                       {@link RocksMode#Optimistic}）时在创建任何资源前立即抛出
	 */
	public Rocks(@Nullable String raftName, RocksMode mode, @Nullable RaftConfig raftConfig, @Nullable Config config) throws Exception {
		this(raftName, validateMode(mode), raftConfig, config, Server::new, new TaskOneByOneByKey());
	}

	/**
	 * 使用指定并发模式、服务端工厂与按 key 串行调度器创建 RocksRaft 状态机。
	 *
	 * @param mode 并发模式，当前仅支持 {@link RocksMode#Pessimism}
	 * @throws NullPointerException {@code mode} 为 {@code null} 时在创建任何资源前立即抛出
	 * @throws UnsupportedOperationException {@code mode} 不是 {@link RocksMode#Pessimism}（例如已废弃的
	 *                                       {@link RocksMode#Optimistic}）时在创建任何资源前立即抛出
	 */
	public Rocks(@Nullable String raftName, RocksMode mode, @Nullable RaftConfig raftConfig, @Nullable Config config,
				 Func3<Raft, String, Config, Server> serverFactory,
				 TaskOneByOneByKey taskOneByOne) throws Exception {
		rocksMode = validateMode(mode);

		addFactory(Changes.TypeId_, () -> new Changes(this));

		// 这个赋值是不必要的，new Raft(...)内部会赋值。
		setRaft(new Raft(this, raftName, raftConfig, config, "Zeze.Raft.Server", serverFactory, taskOneByOne));
		// 写选项与 LogSequence 同源（RaftConfig.SyncWrite），仅在构造完成后的 flush 路径使用。
		writeOptions = getRaft().getLogSequence().getWriteOptions();
		//noinspection DataFlowIssue
		getRaft().addAtFatalKill(() -> {
			if (storage != null)
				storage.close();
		});

		// Raft 在有快照的时候，会调用LoadSnapshot-Restore-OpenDb。
		// 如果Storage没有创建，需要主动打开。
		if (storage == null)
			openDb();

		ShutdownHook.add(this, () -> {
			logger.info("Rocks {} ShutdownHook begin", raftName);
			close();
			logger.info("Rocks {} ShutdownHook end", raftName);
		});
	}

	private void openDb() throws RocksDBException {
		var dbName = Paths.get(Objects.requireNonNull(getDbHome()), "statemachine").toString();

		// DirectOperates 依赖 Db，所以只能在这里打开。
		storage = new RocksDatabase(dbName);

		atomicLongsTable = openTable("Zeze.Raft.RocksRaft.AtomicLongs");
		appliedWatermarkTable = openTable("Zeze.Raft.RocksRaft.AppliedWatermark");
		migrateUniqueAppliedTables();

		for (var table : tables.values())
			table.open();
	}

	public RocksDatabase.Table openTable(String name) throws RocksDBException {
		//noinspection DataFlowIssue
		return storage.getOrAddTable(name);
	}

	public ConcurrentHashMap<String, TableTemplate<?, ? extends Bean>> getTableTemplates() {
		return tableTemplates;
	}

	public ConcurrentHashMap<String, Table<?, ? extends Bean>> getTables() {
		return tables;
	}

	public RocksMode getRocksMode() {
		return rocksMode;
	}

	public @Nullable RocksDatabase getStorage() {
		return storage;
	}

	public @Nullable String getDbHome() {
		//noinspection DataFlowIssue
		return getRaft().getRaftConfig().getDbHome();
	}

	public boolean isLeader() {
		//noinspection DataFlowIssue
		return getRaft().isLeader();
	}

	@SuppressWarnings("unchecked")
	public <K, V extends Bean> @Nullable TableTemplate<K, V> getTableTemplate(String tableTemplateName) {
		return (TableTemplate<K, V>)tableTemplates.get(tableTemplateName);
	}

	/**
	 * 注册表模板。若 value bean 含集合字段，还需为各集合类型调用 {@link #registerLog} 注册解码工厂
	 * （生成代码在 RegisterRocksTables 中自动注册，手写表模板需手动注册）。
	 */
	public <K, V extends Bean> void registerTableTemplate(String tableTemplateName,
														  Class<K> keyClass, Class<V> valueClass) {
		tableTemplates.computeIfAbsent(tableTemplateName, key -> new TableTemplate<>(this, key, keyClass, valueClass));
	}

	// 应用只能递增，这个方法仅 Follower 用来更新计数器。
	private void atomicLongSet(int index, long value) {
		//noinspection DataFlowIssue
		atomicLongs.computeIfAbsent(index, __ -> new AtomicLong()).set(value);
	}

	public void updateAtomicLongs(IntHashMap<Long> to) {
		//noinspection DataFlowIssue
		getRaft().lock();
		try {
			for (var it = atomicLongs.entryIterator(); it.moveToNext(); ) {
				int index = (int)it.key();
				var last = lastUpdated.get(index);
				if (last == null)
					last = 0L;

				long newest = it.value().get();
				if (newest > last) {
					lastUpdated.put(index, newest);
					to.put(index, newest);
				}
			}
		} finally {
			getRaft().unlock();
		}
	}

	public Procedure newProcedure(FuncLong func) {
		return new Procedure(this, func);
	}

	@SuppressWarnings("unchecked")
	public void followerApply(Changes changes, RaftLog holder) {
		try {
			var index = holder.getIndex();
			var pending = takePendingFlush(index, holder.getTerm());
			if (pending != null) {
				// 上次内存已应用但 flush 失败，增量日志不可重放，此处仅重试 flush。
				try {
					flush(pending, changes, true, holder);
				} catch (FlushException e) {
					// 重试再失败时转移原补偿持有的在用计数（addReference=false），避免计数归零间隙被驱逐。
					putPendingFlush(index, holder.getTerm(), pending, false);
					throw e;
				}
				for (var r : pending)
					r.endAccess();
				return;
			}
			var rs = new ArrayList<Record<?>>();
			for (var e : changes.getRecords().entrySet())
				rs.add(((Table<Object, Bean>)Objects.requireNonNull(e.getValue().table)).followerApply(Objects.requireNonNull(e.getKey().key), e.getValue()));
			try {
				flush(rs, changes, true, holder);
			} catch (FlushException e) {
				// 落盘失败转入补偿队列，转移装载持有的在用计数（addReference=false），避免计数归零间隙被驱逐。
				putPendingFlush(index, holder.getTerm(), rs, false);
				throw e;
			}
			// 释放 Table.followerApply 装载时登记的在用保护。
			for (var r : rs)
				r.endAccess();
		} catch (FlushException e) {
			// flush 失败由 pendingFlush 补偿重试，放行给 apply 重试。
			throw e;
		} catch (Throwable e) {
			// 非 flush 异常表示状态机分歧，直接 fatalKill 防止同条目无限重试卡死 lastApplied。
			//noinspection DataFlowIssue
			Rocks.logger.fatal("{} followerApply divergence, fatalKill. logIndex={} term={}",
					getRaft().getName(), holder.getIndex(), holder.getTerm(), e);
			getRaft().fatalKill();
		}
	}

	// raftLog 非 null 时，将业务数据、unique 终态存根与应用完成水位写入同一 WriteBatch 原子提交。
	public void flush(Iterable<Record<?>> rs, Changes changes, boolean followerApply, @Nullable RaftLog raftLog) {
		try {
			//noinspection DataFlowIssue
			try (var batch = storage.borrowBatch()) {
				batch.clear();
				for (var r : rs)
					r.flush(batch);
				var key = ByteBuffer.Allocate(5);
				var value = ByteBuffer.Allocate(9);
				for (var it = changes.getAtomicLongs().iterator(); it.moveToNext(); ) {
					key.WriteIndex = 0;
					key.WriteUInt(it.key());
					value.WriteIndex = 0;
					value.WriteLong(it.value());
					Objects.requireNonNull(atomicLongsTable).put(batch, key.CopyIf(), value.CopyIf());
					if (followerApply)
						atomicLongSet(it.key(), it.value());
				}
				if (raftLog != null) {
					//noinspection DataFlowIssue
					if (raftLog.getLog().getUnique().getRequestId() > 0)
						putUniqueAppliedStub(batch, raftLog);
					putAppliedWatermark(batch, raftLog);
				}
				if (batch.getCount() > 0)
					batch.commit(writeOptions);
			}
		} catch (RocksDBException e) {
			// 包装为 FlushException，通知调用方登记 pendingFlush 仅重试落盘。
			throw new FlushException(e);
		}
	}

	/**
	 * 创建 RocksDB checkpoint 并填写快照水位：优先读取与数据同批提交的 {@link AppliedWatermark}；
	 * 若水位行不存在则回退到 lastApplied（存在未完成的 pendingFlush 时返回 {@code null} 推迟快照）。
	 */
	public @Nullable String checkpoint(SnapshotResult result) throws RocksDBException {
		var checkpointDir = Paths.get(Objects.requireNonNull(getDbHome()), "checkpoint_" + System.currentTimeMillis()).toString();

		// 快速创建 checkpoint，期间阻塞 apply。
		Raft raft = Objects.requireNonNull(getRaft());
		raft.lock();
		try {
			var watermark = readAppliedWatermark();
			if (watermark != null) {
				result.lastIncludedIndex = watermark.index();
				result.lastIncludedTerm = watermark.term();
			} else {
				// 无水位行回退：与 tryApply 同持 raft 锁，检查 pending 窗口互斥。
				if (!pendingFlushApplies.isEmpty()) {
					logger.info("snapshot deferred: {} pending apply(s) unfinished, storage may ahead of lastApplied={}",
							pendingFlushApplies.size(), raft.getLogSequence().getLastApplied());
					return null;
				}
				var lastAppliedLog = raft.getLogSequence().lastAppliedLogTermIndex();
				result.lastIncludedIndex = lastAppliedLog.getIndex();
				result.lastIncludedTerm = lastAppliedLog.getTerm();
			}

			//noinspection DataFlowIssue
			try (var cp = storage.newCheckpoint()) {
				cp.createCheckpoint(checkpointDir);
			} catch (Throwable e) {
				// 失败时清理可能已部分创建的 checkpoint 目录。
				deleteCheckpointDir(checkpointDir);
				throw e;
			}
		} finally {
			raft.unlock();
		}
		return checkpointDir;
	}

	public void restore(String backupDir) throws RocksDBException {
		//noinspection DataFlowIssue
		getRaft().lock();
		try {
			resetTransientState();
			if (storage != null) {
				storage.close();
				storage = null;
			}

			var dbName = Paths.get(Objects.requireNonNull(getDbHome()), "statemachine").toString();
			try (var restoreOptions = new RestoreOptions(false);
				 var backupOptions = new BackupEngineOptions(backupDir);
				 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
				backup.restoreDbFromLatestBackup(dbName, dbName, restoreOptions);
			}

			openDb();
		} finally {
			getRaft().unlock();
		}
	}

	public static void createZipFromDirectory(String sourceDir, String zipFilePath) throws IOException {
		// ZIP内容及中央目录全部写完后才提交；复制或包装流关闭失败时由作用域丢弃temp。
		AtomicOutputFile.writeAtomically(Paths.get(zipFilePath), out -> {
			Path sourcePath = Paths.get(sourceDir);
			try (var zos = new ZipOutputStream(out); var stream = Files.walk(sourcePath)) {
				stream.filter(path -> !Files.isDirectory(path)).forEach(path -> {
					ZipEntry ze = new ZipEntry(sourcePath.relativize(path).toString().replace(File.separatorChar, '/'));
					try {
						zos.putNextEntry(ze);
						Files.copy(path, zos);
						zos.closeEntry();
					} catch (IOException e) {
						throw Task.forceThrow(e);
					}
				});
			}
		});
	}

	public static void extractZipToDirectory(String zipFilePath, String targetDir) throws IOException {
		// 根目录与条目路径必须使用相同的规范形式，否则合法的 "."/".." 目标会被误拒绝。
		Path targetPath = Paths.get(targetDir).toAbsolutePath().normalize();
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
			for (ZipEntry ze; (ze = zis.getNextEntry()) != null; ) {
				Path resolvedPath = resolveSnapshotEntry(targetPath, ze.getName());
				if (ze.isDirectory())
					Files.createDirectories(resolvedPath);
				else {
					Files.createDirectories(resolvedPath.getParent());
					Files.copy(zis, resolvedPath);
				}
			}
		}
	}

	private static Path resolveSnapshotEntry(Path targetPath, String entryName) {
		Path resolvedPath = targetPath.resolve(entryName).normalize();
		if (!resolvedPath.startsWith(targetPath))
			throw new IllegalStateException("Entry with an illegal path: " + entryName);
		return resolvedPath;
	}

	// 重试并校验删除 checkpoint 临时目录；失败仅记告警，留待下次 snapshot 前清扫。
	private static void deleteCheckpointDir(String cpHome) {
		try {
			LogSequence.deletedDirectoryAndCheck(new File(cpHome));
		} catch (Throwable e) {
			Rocks.logger.error("delete checkpoint dir failed, residual remains until next snapshot sweep: {}", cpHome, e);
		}
	}

	// 清扫上次失败残留的 checkpoint_* 目录（受 snapshotting 守卫保护，无并发快照）。
	private void deleteResidualCheckpoints() {
		var files = new File(Objects.requireNonNull(getDbHome())).listFiles();
		if (files == null)
			return;
		for (var file : files) {
			if (file.isDirectory() && file.getName().startsWith("checkpoint_"))
				deleteCheckpointDir(file.getPath());
		}
	}

	@Override
	public SnapshotResult snapshot(String path) throws RocksDBException, IOException {
		deleteResidualCheckpoints();
		long t0 = System.nanoTime();
		SnapshotResult result = new SnapshotResult();
		var cpHome = checkpoint(result);
		if (cpHome == null) {
			// checkpoint 推迟（存在未收尾的 pending 窗口），按未成功返回。
			return result;
		}

		long t1 = System.nanoTime();
		var backupDir = Paths.get(Objects.requireNonNull(getDbHome()), "backup").toString();
		var backupFile = new File(backupDir);
		if (!backupFile.isDirectory() && !backupFile.mkdirs())
			logger.error("create backup directory failed: {}", backupDir);
		try {
			RocksDatabase.backup(cpHome, backupDir);
		} catch (Throwable e) {
			// backup 失败时清理本次 checkpoint 临时目录，避免残留累积占满磁盘。
			deleteCheckpointDir(cpHome);
			throw e;
		}

		long t2 = System.nanoTime();
		deleteCheckpointDir(cpHome);
		createZipFromDirectory(backupDir, path);

		long t3 = System.nanoTime();
		//noinspection DataFlowIssue
		getRaft().getLogSequence().commitSnapshot(path, result.lastIncludedIndex);

		result.success = true;
		result.checkPointNanoTime = t1 - t0;
		result.backupNanoTime = t2 - t1;
		result.zipNanoTime = t3 - t2;
		result.totalNanoTime = System.nanoTime() - t0;
		return result;
	}

	@Override
	public void loadSnapshot(String path) throws RocksDBException, IOException {
		var backupDir = Paths.get(Objects.requireNonNull(getDbHome()), "backup").toString();
		var backupFile = new File(backupDir);
		// 总是从已提交快照全量解压恢复，避免复用可能超前于 firstIndex 的旧 backupDir 导致日志双重应用。
		LogSequence.deletedDirectoryAndCheck(backupFile, 100);
		extractZipToDirectory(path, backupDir);
		restore(backupDir);
	}

	/**
	 * 启动恢复：优先开库校验应用完成水位，有效时跳过快照解压仅增量重放后续日志；
	 * 校验失败或本地库损坏时回退全量 {@link #loadSnapshot}。
	 */
	@Override
	public void recover(@Nullable String snapshotFile) throws Exception {
		try {
			if (storage == null)
				openDb();
			//noinspection DataFlowIssue
			if (getRaft().getLogSequence().tryRecoverFromWatermark())
				return;
		} catch (RocksDBException | IllegalStateException e) {
			// 损坏的本地库或水位不能阻止从已提交快照重建状态机。
			//noinspection DataFlowIssue
			logger.warn("{} incremental recover failed, restore committed snapshot: {}",
					getRaft().getName(), snapshotFile, e);
		}
		super.recover(snapshotFile);
	}

	/**
	 * 重置为空库：无快照全量重放前必须清空旧库，避免非幂等增量日志重复应用。
	 */
	@Override
	public void reset() {
		//noinspection DataFlowIssue
		getRaft().lock();
		try {
			resetTransientState();
			if (storage != null) {
				storage.close();
				storage = null;
			}
			LogSequence.deletedDirectoryAndCheck(
					Paths.get(Objects.requireNonNull(getDbHome()), "statemachine").toFile(), 100);
			openDb();
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		} finally {
			getRaft().unlock();
		}
	}

	/** 清理 reset/restore 共用的状态机瞬态（调用方须持有 raft 锁）。 */
	private void resetTransientState() {
		pendingFlushApplies.clear();
		atomicLongs.clear();
		lastUpdated.clear();
	}

	@Override
	public void close() {
		ShutdownHook.remove(this);
		mutex.lock();
		try {
			try {
				var raft = getRaft();
				if (raft != null)
					raft.shutdown();
			} catch (Exception e) {
				throw Task.forceThrow(e);
			} finally {
				// Raft 排空 apply 前仍可能重试 flush，必须保留已应用的记录。
				pendingFlushApplies.clear();
				setRaft(null);
				// 在关闭 storage 前先关闭各表缓存的周期任务，防止资源泄漏及访问已关闭存储。
				for (var table : tables.values())
					table.close();
				if (storage != null) {
					storage.close();
					storage = null;
				}
			}
		} finally {
			mutex.unlock();
		}
	}
}
