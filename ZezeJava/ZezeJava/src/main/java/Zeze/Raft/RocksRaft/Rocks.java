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
	public void followerApply(Changes changes) {
		var rs = new ArrayList<Record<?>>();
		for (var e : changes.getRecords().entrySet())
			rs.add(((Table<Object, Bean>)e.getValue().table).followerApply(e.getKey().key, e.getValue()));
		flush(rs, changes, true);
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
					key.WriteInt(it.key());
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
			Task.forceThrow(e);
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
			}
		} finally {
			raft.unlock();
		}
		return checkpointDir;
	}

	public void restore(String backupDir) throws RocksDBException {
		getRaft().lock();
		try {
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
					ZipEntry ze = new ZipEntry(sourcePath.relativize(path).toString());
					try {
						zos.putNextEntry(ze);
						Files.copy(path, zos);
						zos.closeEntry();
					} catch (IOException e) {
						Task.forceThrow(e);
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
		RocksDatabase.backup(cpHome, backupDir);

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

	@Override
	public void close() { // 简单保护一下。
		ShutdownHook.remove(this);
		mutex.lock();
		try {
			try {
				Raft raft = getRaft();
				if (raft != null)
					raft.shutdown();
			} catch (Exception e) {
				Task.forceThrow(e);
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
