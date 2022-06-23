package Zeze.Raft.RocksRaft;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Util.FuncLong;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.BackupEngine;
import org.rocksdb.BackupEngineOptions;
import org.rocksdb.Checkpoint;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.Env;
import org.rocksdb.RestoreOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

public final class Rocks extends StateMachine implements Closeable {
	public static final Logger logger = LogManager.getLogger(Rocks.class);

	public static void RegisterLog(Supplier<Log> s) {
		Log.Register(s);
	}

	static {
		Log.Register(LogBool::new);
		Log.Register(LogByte::new);
		Log.Register(LogShort::new);
		Log.Register(LogInt::new);
		Log.Register(LogLong::new);
		Log.Register(LogFloat::new);
		Log.Register(LogDouble::new);
		Log.Register(LogString::new);
		Log.Register(LogBinary::new);
		Log.Register(LogBean::new);
		// Log1.LogBeanKey 在生成代码里面注册。
		// LogSet1<V> LogMap1<K,V> LogMap2<K,V> 在生成代码里面注册。
	}

	private final ConcurrentHashMap<String, TableTemplate<?, ? extends Bean>> TableTemplates = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Table<?, ? extends Bean>> Tables = new ConcurrentHashMap<>();
	private final LongConcurrentHashMap<AtomicLong> AtomicLongs = new LongConcurrentHashMap<>();
	private final IntHashMap<Long> LastUpdated = new IntHashMap<>();
	private final ConcurrentHashMap<String, ColumnFamilyHandle> Columns = new ConcurrentHashMap<>();
	private final WriteOptions writeOptions;
	private final RocksMode rocksMode;
	private RocksDB Storage;
	private ColumnFamilyHandle AtomicLongsColumnFamily;

	public Rocks() throws Throwable {
		this(null, RocksMode.Pessimism, null, null, false);
	}

	public Rocks(String raftName) throws Throwable {
		this(raftName, RocksMode.Pessimism, null, null, false);
	}

	public Rocks(String raftName, RaftConfig raftConfig) throws Throwable {
		this(raftName, RocksMode.Pessimism, raftConfig, null, false);
	}

	public Rocks(String raftName, RaftConfig raftConfig, Zeze.Config config) throws Throwable {
		this(raftName, RocksMode.Pessimism, raftConfig, config, false);
	}

	public Rocks(String raftName, RocksMode mode, RaftConfig raftConfig, Zeze.Config config,
				 boolean RocksDbWriteOptionSync) throws Throwable {
		rocksMode = mode;

		AddFactory(new Changes(this).getTypeId(), () -> new Changes(this));

		writeOptions = RocksDbWriteOptionSync
				? DatabaseRocksDb.getSyncWriteOptions()
				: DatabaseRocksDb.getDefaultWriteOptions();
		// 这个赋值是不必要的，new Raft(...)内部会赋值。有点奇怪。
		setRaft(new Raft(this, raftName, raftConfig, config));
		getRaft().addAtFatalKill(() -> {
			if (Storage != null)
				Storage.close();
		});
		getRaft().getLogSequence().setWriteOptions(writeOptions);

		// Raft 在有快照的时候，会调用LoadSnapshot-Restore-OpenDb。
		// 如果Storage没有创建，需要主动打开。
		if (Storage == null)
			OpenDb();
	}

	private void OpenDb() throws RocksDBException {
		var dbName = Paths.get(getDbHome(), "rocksraft").toString();

		// DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
		var columnFamilies = getColumnFamilies(dbName);
		var outHandles = new ArrayList<ColumnFamilyHandle>();
		Storage = RocksDB.open(DatabaseRocksDb.getCommonDbOptions(), dbName, columnFamilies, outHandles);

		Columns.clear();
		for (int i = 0; i < columnFamilies.size(); i++) {
			ColumnFamilyDescriptor col = columnFamilies.get(i);
			Columns.put(new String(col.getName(), StandardCharsets.UTF_8), outHandles.get(i));
		}

		AtomicLongsColumnFamily = OpenFamily("Zeze.Raft.RocksRaft.AtomicLongs");

		for (var table : Tables.values())
			table.Open();
	}

	private ArrayList<ColumnFamilyDescriptor> getColumnFamilies(String dir) throws RocksDBException {
		// 参考 Zeze.Transaction.DatabaseRocksDb
		var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
		if (new File(dir).isDirectory()) {
			for (var cf : RocksDB.listColumnFamilies(DatabaseRocksDb.getCommonOptions(), dir))
				columnFamilies.add(new ColumnFamilyDescriptor(cf, DatabaseRocksDb.getDefaultCfOptions()));
		}
		if (columnFamilies.isEmpty())
			columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(), DatabaseRocksDb.getDefaultCfOptions()));
		return columnFamilies;
	}

	public ColumnFamilyHandle OpenFamily(String name) {
		return Columns.computeIfAbsent(name, k -> {
			try {
				return Storage.createColumnFamily(new ColumnFamilyDescriptor(
						k.getBytes(StandardCharsets.UTF_8), DatabaseRocksDb.getDefaultCfOptions()));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public ConcurrentHashMap<String, TableTemplate<?, ? extends Bean>> getTableTemplates() {
		return TableTemplates;
	}

	public ConcurrentHashMap<String, Table<?, ? extends Bean>> getTables() {
		return Tables;
	}

	public RocksMode getRocksMode() {
		return rocksMode;
	}

	public RocksDB getStorage() {
		return Storage;
	}

	public String getDbHome() {
		return getRaft().getRaftConfig().getDbHome();
	}

	public boolean isLeader() {
		return getRaft().isLeader();
	}

	@SuppressWarnings("unchecked")
	public <K, V extends Bean> TableTemplate<K, V> GetTableTemplate(String tableTemplateName) {
		return (TableTemplate<K, V>)TableTemplates.get(tableTemplateName);
	}

	public <K, V extends Bean> void RegisterTableTemplate(String tableTemplateName,
														  Class<K> keyClass, Class<V> valueClass) {
		TableTemplates.computeIfAbsent(tableTemplateName, key -> new TableTemplate<>(this, key, keyClass, valueClass));
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
	private void AtomicLongSet(int index, long value) {
		AtomicLongs.computeIfAbsent(index, __ -> new AtomicLong()).set(value);
	}

	public void UpdateAtomicLongs(IntHashMap<Long> to) {
		getRaft().lock();
		try {
			for (var it = AtomicLongs.entryIterator(); it.moveToNext(); ) {
				int index = (int)it.key();
				var last = LastUpdated.get(index);
				if (last == null)
					last = 0L;

				long newest = it.value().get();
				if (newest > last) {
					LastUpdated.put(index, newest);
					to.put(index, newest);
				}
			}
		} finally {
			getRaft().unlock();
		}
	}

	public Procedure NewProcedure(FuncLong func) {
		return new Procedure(this, func);
	}

	@SuppressWarnings("unchecked")
	public void FollowerApply(Changes changes) {
		var rs = new ArrayList<Record<?>>();
		for (var e : changes.getRecords().entrySet())
			rs.add(e.getValue().Table.FollowerApply(e.getKey().Key, e.getValue()));
		Flush(rs, changes, true);
	}

	public void Flush(Iterable<Record<?>> rs, Changes changes) {
		Flush(rs, changes, false);
	}

	public void Flush(Iterable<Record<?>> rs, Changes changes, boolean FollowerApply) {
		try (WriteBatch batch = new WriteBatch()) {
			for (var r : rs)
				r.Flush(batch);
			for (var it = changes.getAtomicLongs().iterator(); it.moveToNext(); ) {
				var key = ByteBuffer.Allocate();
				var value = ByteBuffer.Allocate();
				key.WriteInt(it.key());
				value.WriteLong(it.value());
				batch.put(AtomicLongsColumnFamily, key.Copy(), value.Copy());
				if (FollowerApply)
					AtomicLongSet(it.key(), it.value());
			}
			if (batch.count() > 0)
				Storage.write(writeOptions, batch);
		} catch (RocksDBException e) {
			throw new RuntimeException(e);
		}
	}

	public String Checkpoint(SnapshotResult result) throws RocksDBException {
		var checkpointDir = Paths.get(getDbHome(), "checkpoint_" + System.currentTimeMillis()).toString();

		// fast checkpoint, will stop application apply.
		Raft raft = getRaft();
		raft.lock();
		try {
			var lastAppliedLog = raft.getLogSequence().LastAppliedLogTermIndex();
			result.LastIncludedIndex = lastAppliedLog.getIndex();
			result.LastIncludedTerm = lastAppliedLog.getTerm();

			try (var cp = Checkpoint.create(Storage)) {
				cp.createCheckpoint(checkpointDir);
			}
		} finally {
			raft.unlock();
		}
		return checkpointDir;
	}

	public boolean Backup(String checkpointDir, String backupDir) throws RocksDBException {
		var outHandles = new ArrayList<ColumnFamilyHandle>();
		try (var src = RocksDB.open(DatabaseRocksDb.getCommonDbOptions(), checkpointDir,
				getColumnFamilies(checkpointDir), outHandles);
			 var backupOptions = new BackupEngineOptions(backupDir);
			 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
			backup.createNewBackup(src, true);
		}
		return true;
	}

	public boolean Restore(String backupDir) throws RocksDBException {
		getRaft().lock();
		try {
			if (Storage != null) {
				Storage.close(); // close current
				Storage = null;
			}

			var dbName = Paths.get(getDbHome(), "statemachine").toString();
			try (var restoreOptions = new RestoreOptions(false);
				 var backupOptions = new BackupEngineOptions(backupDir);
				 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
				backup.restoreDbFromLatestBackup(dbName, dbName, restoreOptions);
			}

			OpenDb(); // reopen
			return true;
		} finally {
			getRaft().unlock();
		}
	}

	public static void createZipFromDirectory(String sourceDir, String zipFilePath) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
			Path sourcePath = Paths.get(sourceDir);
			Files.walk(sourcePath)
					.filter(path -> !Files.isDirectory(path))
					.forEach(path -> {
						ZipEntry ze = new ZipEntry(sourcePath.relativize(path).toString());
						try {
							zos.putNextEntry(ze);
							Files.copy(path, zos);
							zos.closeEntry();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
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
	public SnapshotResult Snapshot(String path) throws RocksDBException, IOException {
		SnapshotResult result = new SnapshotResult();
		var cpHome = Checkpoint(result);
		var backupDir = Paths.get(getDbHome(), "backup").toString();
		var backupFile = new File(backupDir);
		if (!backupFile.isDirectory() && !backupFile.mkdirs())
			logger.error("create backup directory failed: {}", backupDir);
		Backup(cpHome, backupDir);

		LogSequence.deleteDirectory(new File(cpHome));
		createZipFromDirectory(backupDir, path);
		getRaft().getLogSequence().CommitSnapshot(path, result.LastIncludedIndex);
		result.success = true;
		return result;
	}

	@Override
	public void LoadSnapshot(String path) throws RocksDBException, IOException {
		var backupDir = Paths.get(getDbHome(), "backup").toString();
		if (Files.getLastModifiedTime(Paths.get(path)).compareTo(Files.getLastModifiedTime(Paths.get(backupDir))) > 0) {
			LogSequence.deleteDirectory(new File(backupDir));
			extractZipToDirectory(path, backupDir);
		}
		Restore(backupDir);
	}

	private final Lock mutex = new ReentrantLock();

	@Override
	public void close() { // 简单保护一下。
		mutex.lock();
		try {
			try {
				Raft raft = getRaft();
				if (raft != null)
					raft.Shutdown();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			} finally {
				setRaft(null);
				if (Storage != null) {
					Storage.close();
					Storage = null;
				}
			}
		} finally {
			mutex.unlock();
		}
	}
}
