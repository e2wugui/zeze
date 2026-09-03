package Zeze.Raft.RocksRaft;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import Zeze.Raft.LogSequence;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RaftLog;
import Zeze.Raft.RocksRaft.Log1.LogBinary;
import Zeze.Raft.RocksRaft.Log1.LogBool;
import Zeze.Raft.RocksRaft.Log1.LogByte;
import Zeze.Raft.RocksRaft.Log1.LogDouble;
import Zeze.Raft.RocksRaft.Log1.LogFloat;
import Zeze.Raft.RocksRaft.Log1.LogInt;
import Zeze.Raft.RocksRaft.Log1.LogLong;
import Zeze.Raft.RocksRaft.Log1.LogShort;
import Zeze.Raft.RocksRaft.Log1.LogString;
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
import org.jetbrains.annotations.NotNull;
import org.rocksdb.BackupEngine;
import org.rocksdb.BackupEngineOptions;
import org.rocksdb.Env;
import org.rocksdb.RestoreOptions;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

public final class Rocks extends StateMachine implements Closeable {
	static final Logger logger = LogManager.getLogger(Rocks.class);
	static final boolean isDebugEnabled = logger.isDebugEnabled();

	/**
	 * flush 落盘失败（RocksDBException 的包装）。
	 * apply 流程是"先改内存、后flush"，flush 失败时内存已是最终状态而 lastApplied
	 * 未推进，之后会重试（FND-R2-4）。用专门的异常类型把"可重试的落盘失败"与
	 * decode/结构性错误区分开：前者由 pendingFlushApplies 记录已应用的内存状态、
	 * 重试时只重试flush；后者不可恢复。
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

	// FND-R2-4："内存已应用但flush失败"的日志条目（key=RaftLog.Index，value含条目term）。
	// leaderApply/followerApply 的内存变更与flush非原子：flush失败时内存已变更而
	// lastApplied未推进，重试若重新走增量日志重放（如list的OP_ADD按索引追加）会在
	// 已应用的状态上双重应用并被后续提交复制出去。这里记录已应用的记录集合，
	// 重试时跳过内存变更只重试flush；term不匹配说明同index已被新term条目复用
	// （旧条目被截断），丢弃过期记录按全新条目应用。
	// 仅存在于apply失败到重试成功之间的短窗口，reset/restore/close时清空。
	private final LongConcurrentHashMap<PendingFlush> pendingFlushApplies = new LongConcurrentHashMap<>();

	static final class PendingFlush {
		final long term;
		final List<Record<?>> records;

		PendingFlush(long term, List<Record<?>> records) {
			this.term = term;
			this.records = records;
		}
	}

	// 取出index对应的待补偿记录；没有或term不匹配（同index已被新term条目复用）返回null。
	List<Record<?>> takePendingFlush(long index, long term) {
		var pending = pendingFlushApplies.remove(index);
		return pending != null && pending.term == term ? pending.records : null;
	}

	void putPendingFlush(long index, long term, List<Record<?>> records) {
		pendingFlushApplies.put(index, new PendingFlush(term, records));
	}

	public static void registerLog(Supplier<Log> s) {
		Log.register(s);
	}

	static {
		Log.register(LogBool::new);
		Log.register(LogByte::new);
		Log.register(LogShort::new);
		Log.register(LogInt::new);
		Log.register(LogLong::new);
		Log.register(LogFloat::new);
		Log.register(LogDouble::new);
		Log.register(LogString::new);
		Log.register(LogBinary::new);
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
	private RocksDatabase storage;
	private RocksDatabase.Table atomicLongsTable;
	private final Lock mutex = new ReentrantLock();

	public Rocks() throws Exception {
		this(null, RocksMode.Pessimism, null, null, false);
	}

	public Rocks(String raftName) throws Exception {
		this(raftName, RocksMode.Pessimism, null, null, false);
	}

	public Rocks(String raftName, RaftConfig raftConfig) throws Exception {
		this(raftName, RocksMode.Pessimism, raftConfig, null, false);
	}

	public Rocks(String raftName, RaftConfig raftConfig, Zeze.Config config) throws Exception {
		this(raftName, RocksMode.Pessimism, raftConfig, config, false);
	}

	public Rocks(String raftName, RocksMode mode, RaftConfig raftConfig, Zeze.Config config,
				 boolean RocksDbWriteOptionSync) throws Exception {
		this(raftName, mode, raftConfig, config, RocksDbWriteOptionSync, Server::new, new TaskOneByOneByKey());
	}

	public Rocks(String raftName, RocksMode mode, RaftConfig raftConfig, Zeze.Config config,
				 boolean RocksDbWriteOptionSync, Func3<Raft, String, Config, Server> serverFactory,
				 TaskOneByOneByKey taskOneByOne) throws Exception {
		rocksMode = mode;

		addFactory(Changes.TypeId_, () -> new Changes(this));

		writeOptions = RocksDbWriteOptionSync
				? RocksDatabase.getSyncWriteOptions()
				: RocksDatabase.getDefaultWriteOptions();
		// 这个赋值是不必要的，new Raft(...)内部会赋值。有点奇怪。
		setRaft(new Raft(this, raftName, raftConfig, config, "Zeze.Raft.Server", serverFactory, taskOneByOne));
		getRaft().addAtFatalKill(() -> {
			if (storage != null)
				storage.close();
		});
		getRaft().getLogSequence().setWriteOptions(writeOptions);

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
		var dbName = Paths.get(getDbHome(), "statemachine").toString();

		// DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
		storage = new RocksDatabase(dbName);

		atomicLongsTable = openTable("Zeze.Raft.RocksRaft.AtomicLongs");

		for (var table : tables.values())
			table.open();
	}

	public @NotNull RocksDatabase.Table openTable(String name) throws RocksDBException {
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

	public RocksDatabase getStorage() {
		return storage;
	}

	public String getDbHome() {
		return getRaft().getRaftConfig().getDbHome();
	}

	public boolean isLeader() {
		return getRaft().isLeader();
	}

	@SuppressWarnings("unchecked")
	public <K, V extends Bean> TableTemplate<K, V> getTableTemplate(String tableTemplateName) {
		return (TableTemplate<K, V>)tableTemplates.get(tableTemplateName);
	}

	public <K, V extends Bean> void registerTableTemplate(String tableTemplateName,
														  Class<K> keyClass, Class<V> valueClass) {
		tableTemplates.computeIfAbsent(tableTemplateName, key -> new TableTemplate<>(this, key, keyClass, valueClass));
	}

/*
	public AtomicLong AtomicLong(int index) {
		return AtomicLongs.computeIfAbsent(index, __ -> new AtomicLong());
	}

	public long AtomicLongIncrementAndGet(int index) {
		return AtomicLongs.computeIfAbsent(index, __ -> new AtomicLong()).incrementAndGet();
	}

	public long AtomicLongGet(int index) {
		return AtomicLongs.computeIfAbsent(index, __ -> new AtomicLong()).get();
	}
*/

	// 应用只能递增，这个方法仅 Follower 用来更新计数器。
	private void atomicLongSet(int index, long value) {
		atomicLongs.computeIfAbsent(index, __ -> new AtomicLong()).set(value);
	}

	public void updateAtomicLongs(IntHashMap<Long> to) {
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
		var index = holder.getIndex();
		var pending = takePendingFlush(index, holder.getTerm());
		if (pending != null) {
			// 上次followerApply已完成内存变更但flush失败（FND-R2-4）：内存已是最终状态。
			// 增量日志重放不幂等（如list的OP_ADD按索引追加），重放会双重应用，
			// 这里跳过内存变更，仅重试flush。
			try {
				flush(pending, changes, true);
			} catch (FlushException e) {
				putPendingFlush(index, holder.getTerm(), pending);
				throw e;
			}
			return;
		}
		var rs = new ArrayList<Record<?>>();
		for (var e : changes.getRecords().entrySet())
			rs.add(((Table<Object, Bean>)e.getValue().table).followerApply(e.getKey().key, e.getValue()));
		try {
			flush(rs, changes, true);
		} catch (FlushException e) {
			// 内存已变更但落盘失败：记录已应用的记录集合，等下次apply重试时只flush。
			putPendingFlush(index, holder.getTerm(), rs);
			throw e;
		}
	}

	public void flush(Iterable<Record<?>> rs, Changes changes) {
		flush(rs, changes, false);
	}

	public void flush(Iterable<Record<?>> rs, Changes changes, boolean followerApply) {
		try {
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
					atomicLongsTable.put(batch, key.CopyIf(), value.CopyIf());
					if (followerApply)
						atomicLongSet(it.key(), it.value());
				}
				if (batch.getCount() > 0)
					batch.commit(writeOptions);
			}
		} catch (RocksDBException e) {
			// 专门的异常类型：调用方（leaderApply/followerApply）据此记录已应用的内存状态，
			// 重试时只重试flush，保证apply对flush失败幂等（FND-R2-4）。
			throw new FlushException(e);
		}
	}

	public String checkpoint(SnapshotResult result) throws RocksDBException {
		var checkpointDir = Paths.get(getDbHome(), "checkpoint_" + System.currentTimeMillis()).toString();

		// fast checkpoint, will stop application apply.
		Raft raft = getRaft();
		raft.lock();
		try {
			var lastAppliedLog = raft.getLogSequence().lastAppliedLogTermIndex();
			result.lastIncludedIndex = lastAppliedLog.getIndex();
			result.lastIncludedTerm = lastAppliedLog.getTerm();

			try (var cp = storage.newCheckpoint()) {
				cp.createCheckpoint(checkpointDir);
			} catch (Throwable e) {
				// 【FND-R2-5】createCheckpoint中途失败也可能已创建部分目录，删除后再抛。
				LogSequence.deleteDirectory(new File(checkpointDir));
				throw e;
			}
		} finally {
			raft.unlock();
		}
		return checkpointDir;
	}

	public void restore(String backupDir) throws RocksDBException {
		getRaft().lock();
		try {
			pendingFlushApplies.clear(); // 状态机回退到快照边界，"已应用未flush"记录作废（FND-R2-4）
			if (storage != null) {
				storage.close(); // close current
				storage = null;
			}

			var dbName = Paths.get(getDbHome(), "statemachine").toString();
			try (var restoreOptions = new RestoreOptions(false);
				 var backupOptions = new BackupEngineOptions(backupDir);
				 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
				backup.restoreDbFromLatestBackup(dbName, dbName, restoreOptions);
			}

			openDb(); // reopen
		} finally {
			getRaft().unlock();
		}
	}

	public static void createZipFromDirectory(String sourceDir, String zipFilePath) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
			Path sourcePath = Paths.get(sourceDir);
			try (var stream = Files.walk(sourcePath)) {
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
		}
	}

	public static void extractZipToDirectory(String zipFilePath, String targetDir) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
			Path targetPath = Paths.get(targetDir).toAbsolutePath();
			for (ZipEntry ze; (ze = zis.getNextEntry()) != null; ) {
				Path resolvedPath = targetPath.resolve(ze.getName()).normalize();
				if (!resolvedPath.startsWith(targetPath)) {
					// see: https://snyk.io/research/zip-slip-vulnerability
					throw new IllegalStateException("Entry with an illegal path: " + ze.getName());
				}
				if (ze.isDirectory())
					Files.createDirectories(resolvedPath);
				else {
					Files.createDirectories(resolvedPath.getParent());
					Files.copy(zis, resolvedPath);
				}
			}
		}
	}

	@Override
	public SnapshotResult snapshot(String path) throws RocksDBException, IOException {
		long t0 = System.nanoTime();
		SnapshotResult result = new SnapshotResult();
		var cpHome = checkpoint(result);

		long t1 = System.nanoTime();
		var backupDir = Paths.get(getDbHome(), "backup").toString();
		var backupFile = new File(backupDir);
		if (!backupFile.isDirectory() && !backupFile.mkdirs())
			logger.error("create backup directory failed: {}", backupDir);
		try {
			RocksDatabase.backup(cpHome, backupDir);
		} catch (Throwable e) {
			// 【FND-R2-5】backup失败（磁盘满/权限等）时清理checkpoint目录：
			// checkpoint_<timestamp>是状态机RocksDB的完整物理拷贝，快照每次失败重试
			// 都会新增一份，残留累积会渐进占满DbHome。成功路径的删除保持在下面原位。
			LogSequence.deleteDirectory(new File(cpHome));
			throw e;
		}

		long t2 = System.nanoTime();
		LogSequence.deleteDirectory(new File(cpHome));
		createZipFromDirectory(backupDir, path);

		long t3 = System.nanoTime();
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
		var backupDir = Paths.get(getDbHome(), "backup").toString();
		var backupFile = new File(backupDir);
		if (!backupFile.isDirectory() || new File(path).lastModified() > backupFile.lastModified()) {
			LogSequence.deletedDirectoryAndCheck(backupFile, 100);
			extractZipToDirectory(path, backupDir);
		}
		restore(backupDir);
	}

	/**
	 * 没有快照的时候，Raft 重启后会从头重放全部日志，状态机必须从空库开始，
	 * 否则 list 等按索引增量 apply 的非幂等日志会在残留的旧数据上被重复应用。
	 * 参考 loadSnapshot 的 restore+openDb 机械：关句柄→删数据→重开空库。
	 * 【注意】Raft 构造过程中（无快照）调用到这里时 storage==null（openDb 尚未执行），
	 * 此时只删除旧数据库目录即可，随后的 openDb 会创建空库。
	 */
	@Override
	public void reset() {
		getRaft().lock();
		try {
			pendingFlushApplies.clear(); // 清库重放，"已应用未flush"记录作废（FND-R2-4）
			if (storage != null) {
				storage.close(); // close current
				storage = null;
			}
			atomicLongs.clear();
			lastUpdated.clear();
			LogSequence.deletedDirectoryAndCheck(
					Paths.get(getDbHome(), "statemachine").toFile(), 100);
			openDb(); // reopen empty
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		} finally {
			getRaft().unlock();
		}
	}

	@Override
	public void close() { // 简单保护一下。
		ShutdownHook.remove(this);
		mutex.lock();
		try {
			pendingFlushApplies.clear(); // 关闭，不再有重试（FND-R2-4）
			try {
				Raft raft = getRaft();
				if (raft != null)
					raft.shutdown();
			} catch (Exception e) {
				throw Task.forceThrow(e);
			} finally {
				setRaft(null);
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
