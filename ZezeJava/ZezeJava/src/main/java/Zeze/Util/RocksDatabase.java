package Zeze.Util;

import java.io.Closeable;
import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.rocksdb.*;

/** RocksDB 封装：列族 Table 管理、Batch/Batch2 批量写、备份恢复与 close-safe 停机排空。 */
public class RocksDatabase extends ReentrantLock implements Closeable {
	static {
		RocksDB.loadLibrary();
	}

	private static final Logger logger = LogManager.getLogger(RocksDatabase.class);
	private static final LRUCache dbCache = new LRUCache(Str.parseLongSize(System.getProperty("rocksdbCache"), 64 << 20));
	private static final TableFormatConfig tableCfg = new BlockBasedTableConfig().setBlockCache(dbCache);
	private static final long dbBuffer = Str.parseLongSize(System.getProperty("rocksdbBuffer"), 64 << 20);
	private static final Options commonOptions = new Options()
		.setCreateIfMissing(true)
		.setTableFormatConfig(tableCfg)
		.setDbWriteBufferSize(dbBuffer) // 全部列族共享的写缓冲总字节数
		.setKeepLogFileNum(5); // 保留的历史日志文件数量
	private static final DBOptions commonDbOptions = new DBOptions()
		.setCreateIfMissing(true)
		.setDbWriteBufferSize(dbBuffer) // 全部列族共享的写缓冲总字节数
		.setKeepLogFileNum(5) // 保留的历史日志文件数量
		.setMaxWriteBatchGroupSizeBytes(100 * 1024 * 1024);
	private static final ColumnFamilyOptions commonCfOptions = new ColumnFamilyOptions()
		.setTableFormatConfig(tableCfg);
	private static final ReadOptions defaultReadOptions = new ReadOptions();
	private static final WriteOptions defaultWriteOptions = new WriteOptions();
	private static final WriteOptions syncWriteOptions = new WriteOptions().setSync(true);
	private static final TransactionDBOptions transactionDbOptions = new TransactionDBOptions();
	/** 关闭状态标记，供读写门控与停机排空协同（详见 {@link #close()}）。 */
	private volatile boolean closing = false;
	private final java.util.concurrent.atomic.LongAdder inflightOps = new java.util.concurrent.atomic.LongAdder();
	// 仅登记尚未关闭的迭代器，在 native close 完成后注销
	private final java.util.Set<RocksIterator> openIterators = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private static final long CLOSE_DRAIN_BUDGET_MS = Long.getLong("rocksdb.closeDrainBudgetMs", 30_000);

	private static final MethodHandle mhWriteBatchPutCf;
	private static final MethodHandle mhWriteBatchDeleteCf;
	private static final MethodHandle mhWriteBatchNativeNew;
	private static final MethodHandle mhWriteBatchNew;
	private static final ZezeCounter.LongObserver rocksDbGetObserver;
	private static final ZezeCounter.LongObserver rocksDbPutObserver;
	private static final ZezeCounter.LongObserver rocksDbDeleteObserver;
	private static final ZezeCounter.LongObserver rocksDbDeleteRangeObserver;
	private static final ZezeCounter.LongObserver rocksDbWriteObserver;
	private static final ZezeCounter.LongObserver rocksDbTxnPutObserver;
	private static final ZezeCounter.LongObserver rocksDbTxnDeleteObserver;
	private static final ZezeCounter.LongObserver rocksDbCompactObserver;

	static {
		try {
			var lookup = MethodHandles.lookup();
			var clsWriteBatch = WriteBatch.class;
			// native void put(long handle, byte[] key, int keyLen, byte[] value, int valueLen, long cfHandle);
			var m = clsWriteBatch.getDeclaredMethod("put",
				long.class, byte[].class, int.class, byte[].class, int.class, long.class);
			m.setAccessible(true);
			mhWriteBatchPutCf = lookup.unreflect(m);
			// native void delete(long handle, byte[] key, int keyLen, long cfHandle);
			m = clsWriteBatch.getDeclaredMethod("delete", long.class, byte[].class, int.class, long.class);
			m.setAccessible(true);
			mhWriteBatchDeleteCf = lookup.unreflect(m);
			// native static long newWriteBatch(byte[] serialized, int serializedLength)
			m = clsWriteBatch.getDeclaredMethod("newWriteBatch", byte[].class, int.class);
			m.setAccessible(true);
			mhWriteBatchNativeNew = lookup.unreflect(m);
			// WriteBatch(long nativeHandle, boolean owningNativeHandle)
			var c = clsWriteBatch.getDeclaredConstructor(long.class, boolean.class);
			c.setAccessible(true);
			mhWriteBatchNew = lookup.unreflectConstructor(c);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}

		var creator = ZezeCounter.instance.allocRunTimeObserverCreator("rocksdb_operation", "operation");
		rocksDbGetObserver = creator.labelValues("get");
		rocksDbPutObserver = creator.labelValues("put");
		rocksDbDeleteObserver = creator.labelValues("delete");
		rocksDbDeleteRangeObserver = creator.labelValues("delete_range");
		rocksDbWriteObserver = creator.labelValues("write");
		rocksDbTxnPutObserver = creator.labelValues("txn_put");
		rocksDbTxnDeleteObserver = creator.labelValues("txn_delete");
		rocksDbCompactObserver = creator.labelValues("compact");
	}

	public static Options getCommonOptions() {
		return commonOptions;
	}

	public static DBOptions getCommonDbOptions() {
		return commonDbOptions;
	}

	public static ColumnFamilyOptions getCommonCfOptions() {
		return commonCfOptions;
	}

	public static ReadOptions getDefaultReadOptions() {
		return defaultReadOptions;
	}

	public static WriteOptions getDefaultWriteOptions() {
		return defaultWriteOptions;
	}

	public static WriteOptions getSyncWriteOptions() {
		return syncWriteOptions;
	}

	/** 仅当迭代器失效且 status 正常时视作 EOF；若 status 异常则通过 Task.forceThrow 抛出。 */
	public static boolean isValidOrThrow(RocksIteratorInterface iterator) {
		if (iterator.isValid())
			return true;
		try {
			iterator.status();
			return false;
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		}
	}

	public enum DbType {
		eRocksDb,
		eOptimisticTransactionDb,
		eTransactionDb,
	}

	private final String homePath;
	private final RocksDB rocksDb;
	private final @Nullable OptimisticTransactionDB optimisticTransactionDb;
	private final @Nullable TransactionDB transactionDb;

	private final ConcurrentHashMap<String, Table> tableMap = new ConcurrentHashMap<>();
	private @Nullable Map<String, Table> tableMapView;
	private final ArrayList<Batch> batchPool = new ArrayList<>();

	public RocksDatabase(String homePath) throws RocksDBException {
		this(homePath, DbType.eRocksDb);
	}

	public RocksDatabase(String homePath, DbType dbType) throws RocksDBException {
		this.homePath = homePath;
		var cfds = getCfDescriptors(homePath);
		int cfCount = cfds.size();
		var cfhs = new ArrayList<ColumnFamilyHandle>(cfCount);
		rocksDb = open(dbType, commonDbOptions, homePath, cfds, cfhs);
		optimisticTransactionDb = dbType == DbType.eOptimisticTransactionDb ? (OptimisticTransactionDB)rocksDb : null;
		transactionDb = dbType == DbType.eTransactionDb ? (TransactionDB)rocksDb : null;
		for (int i = 0; i < cfCount; i++) {
			var name = new String(cfds.get(i).getName(), StandardCharsets.UTF_8);
			tableMap.put(name, new Table(name, cfhs.get(i)));
		}
	}

	public static ArrayList<ColumnFamilyDescriptor> getCfDescriptors(String homePath)
		throws RocksDBException {
		var cfds = new ArrayList<ColumnFamilyDescriptor>();
		for (byte[] cfn : RocksDB.listColumnFamilies(commonOptions, homePath))
			cfds.add(new ColumnFamilyDescriptor(cfn, commonCfOptions));
		if (cfds.isEmpty())
			cfds.add(new ColumnFamilyDescriptor("default".getBytes(StandardCharsets.UTF_8), commonCfOptions));
		return cfds;
	}

	public Transaction beginOptimisticTransaction() {
		enterOp();
		try {
			//noinspection DataFlowIssue
			return optimisticTransactionDb.beginTransaction(getDefaultWriteOptions());
		} finally {
			exitOp();
		}
	}

	public Transaction beginTransaction() {
		enterOp();
		try {
			//noinspection DataFlowIssue
			return transactionDb.beginTransaction(getDefaultWriteOptions());
		} finally {
			exitOp();
		}
	}

	// RocksDB用完时需确保调用close回收堆外内存
	public static RocksDB open(DBOptions options, String path,
										List<ColumnFamilyDescriptor> cfds,
										List<ColumnFamilyHandle> cfhs) throws RocksDBException {
		return open(DbType.eRocksDb, options, path, cfds, cfhs);
	}

	private static RocksDB realOpen(DbType dbType, DBOptions options, String path,
											 List<ColumnFamilyDescriptor> cfds,
											 List<ColumnFamilyHandle> cfhs) throws RocksDBException {
		return switch (dbType) {
			case eRocksDb -> RocksDB.open(options, path, cfds, cfhs);
			case eOptimisticTransactionDb -> OptimisticTransactionDB.open(options, path, cfds, cfhs);
			case eTransactionDb -> TransactionDB.open(options, transactionDbOptions, path, cfds, cfhs);
		};
	}

	public static RocksDB open(DbType dbType, DBOptions options, String path,
										List<ColumnFamilyDescriptor> cfds,
										List<ColumnFamilyHandle> cfhs) throws RocksDBException {
		logger.info("RocksDB.open: '{}', {}", path, dbType);
		var file = new File(path);
		if (!file.isDirectory()) {
			//noinspection ResultOfMethodCallIgnored
			file.mkdirs();
		}
		for (int i = 0; ; ) {
			try {
				var rocksDb = realOpen(dbType, options, path, cfds, cfhs);
				if (cfds.size() != cfhs.size()) {
					// 句柄数不匹配时先释放已创建句柄与库实例再抛出，防止泄漏
					int cfhCount = cfhs.size();
					for (var cfh : cfhs) {
						try {
							cfh.close();
						} catch (Throwable ignored) {
						}
					}
					cfhs.clear();
					rocksDb.close();
					throw new IllegalStateException("RocksDB.open unmatched: " + cfds.size() + " != " + cfhCount);
				}
				return rocksDb;
			} catch (RocksDBException e) {
				// 释放失败尝试已创建的句柄并清空，避免污染下次重试
				for (var cfh : cfhs) {
					try {
						cfh.close();
					} catch (Throwable ignored) {
					}
				}
				cfhs.clear();
				logger.warn("RocksDB.open failed: '{}'", path, e);
				if (++i >= 10)
					throw e;
				try {
					//noinspection BusyWait
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
					Thread.currentThread().interrupt();
					throw e;
				}
			}
		}
	}

	// RocksDB用完时需确保调用close回收堆外内存
	public static RocksDB open(String path) throws RocksDBException {
		return open(commonOptions, path);
	}

	// RocksDB用完时需确保调用close回收堆外内存
	public static RocksDB open(Options options, String path) throws RocksDBException {
		logger.info("RocksDB.open: '{}'", path);
		var file = new File(path);
		if (!file.isDirectory()) {
			//noinspection ResultOfMethodCallIgnored
			file.mkdirs();
		}
		for (int i = 0; ; ) {
			try {
				return RocksDB.open(options, path);
			} catch (RocksDBException e) {
				logger.warn("RocksDB.open failed: '{}'", path, e);
				if (++i >= 10)
					throw e;
				try {
					//noinspection BusyWait
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
					Thread.currentThread().interrupt();
					throw e;
				}
			}
		}
	}

	public String getHomePath() {
		return homePath;
	}

	public RocksDB getRocksDb() {
		return rocksDb;
	}

	public @Nullable OptimisticTransactionDB getOptimisticTransactionDb() {
		return optimisticTransactionDb;
	}

	public @UnmodifiableView Map<String, Table> getTableMap() {
		var view = tableMapView;
		if (view == null)
			tableMapView = view = Collections.unmodifiableMap(tableMap);
		return view;
	}

	public @Nullable Table getTable(String name) {
		return tableMap.get(name);
	}

	public Table getOrAddTable(String name) throws RocksDBException {
		return getOrAddTable(name, null);
	}

	public Table getOrAddTable(String name, @Nullable OutObject<Boolean> isNew)
		throws RocksDBException {
		lock();
		try {
			if (closing)
				throw new IllegalStateException("RocksDatabase is closing");
			var table = tableMap.get(name);
			if (table != null) {
				if (isNew != null)
					isNew.value = false;
				return table;
			}
			if (isNew != null)
				isNew.value = true;
			table = new Table(name, rocksDb.createColumnFamily(
				new ColumnFamilyDescriptor(name.getBytes(StandardCharsets.UTF_8), commonCfOptions)));
			tableMap.put(name, table);
			return table;
		} finally {
			unlock();
		}
	}

	public Table [] getOrAddTables(String [] names) throws RocksDBException {
		return getOrAddTables(names, null);
	}

	public Table [] getOrAddTables(String [] names,
													 boolean @Nullable [] isNews) throws RocksDBException {
		lock();
		try {
			if (closing)
				throw new IllegalStateException("RocksDatabase is closing");
			var n = names.length;
			var tables = new Table[n];
			var newIndexes = new IntList();
			var newNames = new ArrayList<byte[]>();
			for (int i = 0; i < n; i++) {
				var table = tableMap.get(names[i]);
				if (table != null)
					tables[i] = table;
				else {
					newIndexes.add(i);
					newNames.add(names[i].getBytes(StandardCharsets.UTF_8));
				}
				if (isNews != null && i < isNews.length)
					isNews[i] = table == null;
			}
			n = newIndexes.size();
			if (n > 0) {
				var cfhs = rocksDb.createColumnFamilies(commonCfOptions, newNames);
				if (cfhs.size() != newNames.size()) {
					// 句柄数不匹配时先释放已创建的列族句柄再抛出，防止泄漏
					int cfhCount = cfhs.size();
					for (var cfh : cfhs) {
						try {
							cfh.close();
						} catch (Throwable ignored) {
						}
					}
					throw new IllegalStateException("createColumnFamilies unmatched: "
						+ cfhCount + " != " + newNames.size());
				}
				for (int i = 0; i < n; i++) {
					var idx = newIndexes.get(i);
					var table = new Table(names[idx], cfhs.get(i));
					tableMap.put(table.name, table);
					tables[idx] = table;
				}
			}
			return tables;
		} finally {
			unlock();
		}
	}

	/** 在飞使用检查：返回仍持有该列族资源的在飞计数（0 表示可销毁），实现须无锁无阻塞。 */
	@FunctionalInterface
	public interface InFlight {
		int inFlight();
	}

	public void dropTable(String name) throws RocksDBException {
		destroyColumnFamily(name, null);
	}

	/**
	 * 列族销毁统一入口：除名并置 {@code destroyPending} -> 锁外等待在飞检查归零 -> 删除列族并释放句柄。
	 * 长期租约使用方须遵循“先计数后查标记”与本方法的“先置标记后读计数”配对。
	 * 排空被中断时回滚除名与标记并抛出异常，保证正常返回即已完成销毁（或本就不存在）。
	 *
	 * @param inFlight 使用方提供的在飞检查；null 表示无长期租约使用方
	 */
	public void destroyColumnFamily(String name, @Nullable InFlight inFlight) throws RocksDBException {
		enterOp();
		try {
			destroyColumnFamilyCore(name, inFlight);
		} finally {
			exitOp();
		}
	}

	private void destroyColumnFamilyCore(String name, @Nullable InFlight inFlight) throws RocksDBException {
		Table table;
		lock();
		try {
			table = tableMap.remove(name);
			if (table == null)
				return;
			table.destroyPending = true; // 先置标记再读在飞计数，与使用方“先计数后查标记”对称
		} finally {
			unlock();
		}
		while (null != inFlight && inFlight.inFlight() != 0) { // 锁外排空，避免阻塞在飞释放路径
			try {
				//noinspection BusyWait
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.warn("destroyColumnFamily drain interrupted, drop aborted and rollback. table={}", name);
				lock();
				try {
					table.destroyPending = false; // 中断回滚，原样保留列族与句柄
					tableMap.putIfAbsent(name, table);
				} finally {
					unlock();
				}
				throw new IllegalStateException("destroyColumnFamily drain interrupted: " + name);
			}
		}
		var cfh = table.getCfHandle();
		try {
			rocksDb.dropColumnFamily(cfh);
		} finally {
			// 已从 tableMap 除名，即使 drop 失败也须销毁 native 句柄防泄漏
			try {
				rocksDb.destroyColumnFamilyHandle(cfh);
			} catch (Throwable ignored) {
			}
		}
	}

	public int dropTables(String [] names) throws Exception {
		lock();
		try {
			var cfhs = new ArrayList<ColumnFamilyHandle>();
			try {
				for (var name : names) {
					var table = tableMap.remove(name);
					if (table != null)
						cfhs.add(table.getCfHandle());
				}
				if (cfhs.isEmpty())
					return 0;
				rocksDb.dropColumnFamilies(cfhs);
				return cfhs.size();
			} finally {
				// 同 dropTable——drop 失败也逐个关闭句柄，单个销毁失败不影响其余
				for (var cfh : cfhs) {
					try {
						rocksDb.destroyColumnFamilyHandle(cfh);
					} catch (Throwable ignored) {
					}
				}
			}
		} finally {
			unlock();
		}
	}

	// Batch用完时需确保调用close回收堆外内存,推荐使用try(var b = newBatch()) {...}
	public Batch newBatch() {
		return new Batch();
	}

	// Batch用完时需确保调用close归还pool,推荐使用try(var b = borrowBatch()) {...}
	public Batch borrowBatch() {
		lock();
		try {
			if (closing)
				throw new IllegalStateException("RocksDatabase is closing");
			int n = batchPool.size();
			if (n > 0)
				return batchPool.remove(n - 1);
		} finally {
			unlock();
		}
		return new Batch() {
			@Override
			public void close() {
				RocksDatabase.this.lock();
				try {
					if (closing) {
						super.close(); // 关闭后不再回池，直接销毁 native 句柄
						return;
					}
					if (!batchPool.contains(this)) { // 防止重复 close 导致同一 Batch 多次入池
						clear();
						batchPool.add(this);
					}
				} finally {
					RocksDatabase.this.unlock();
				}
			}
		};
	}

	public Batch2 newBatch2() {
		return new Batch2();
	}

	public Batch2 newBatch2(int capacity) {
		return new Batch2(capacity);
	}

	// Checkpoint用完时需确保调用close回收堆外内存,推荐使用try(var c = newCheckpoint()) {...}
	public Checkpoint newCheckpoint() {
		enterOp();
		try {
			return Checkpoint.create(rocksDb);
		} finally {
			exitOp();
		}
	}

	public boolean isClosed() {
		return !rocksDb.isOwningHandle();
	}

	public boolean isClosing() {
		return closing;
	}

	/**
	 * 进入 JNI 数据通路的统一门控：先递增计数再检查 closing（与 close 的先置位后读计数对称），
	 * 关闭后迟到调用抛出 {@link IllegalStateException}。
	 */
	void enterOp() {
		inflightOps.increment();
		if (closing) {
			inflightOps.decrement();
			throw new IllegalStateException("RocksDatabase closing: " + homePath);
		}
	}

	void exitOp() {
		inflightOps.decrement();
	}

	/**
	 * close-safe 关闭：置 closing 与全部表 destroyPending 拒绝新调用并促使长租约尽快放弃，
	 * 在锁外有界排空在飞操作与已登记迭代器（默认 30s，由 rocksdb.closeDrainBudgetMs 配置），
	 * 最后持锁释放批量池、列族句柄与库句柄；若排空超时则告警后继续释放。
	 */
	@Override
	public void close() {
		closing = true;
		// 通知全部表的租约检查点尽快放弃，加速排空
		lock();
		try {
			for (var table : tableMap.values())
				table.destroyPending = true;
		} finally {
			unlock();
		}
		var deadline = System.currentTimeMillis() + CLOSE_DRAIN_BUDGET_MS;
		while (System.currentTimeMillis() < deadline) {
			if (inflightOps.sum() == 0 && openIterators.isEmpty())
				break;
			try {
				//noinspection BusyWait
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		if (inflightOps.sum() != 0 || !openIterators.isEmpty())
			logger.warn("rocksdb close drain timeout ({}ms), continue. home={} inflight={} openIterators={}",
				CLOSE_DRAIN_BUDGET_MS, homePath, inflightOps.sum(), openIterators.size());
		lock();
		try {
			for (var b : batchPool)
				b.batch.close();
			batchPool.clear();
			// 在 db.close() 前逐个释放列族句柄（不删数据），防止堆外句柄泄漏
			for (var table : tableMap.values()) {
				try {
					rocksDb.destroyColumnFamilyHandle(table.getCfHandle());
				} catch (Throwable e) {
					logger.error("destroy column family handle '{}'", table.getName(), e);
				}
			}
			tableMap.clear();
			rocksDb.close();
		} finally {
			unlock();
		}
	}

	public static void backup(String checkpointDir, String backupDir) throws RocksDBException {
		backup(DbType.eRocksDb, checkpointDir, backupDir);
	}

	public static void backup(DbType dbType, String checkpointDir, String backupDir)
		throws RocksDBException {
		var cfhs = new ArrayList<ColumnFamilyHandle>();
		try (var src = realOpen(dbType, commonDbOptions, checkpointDir, getCfDescriptors(checkpointDir), cfhs);
			 var backupOptions = new BackupEngineOptions(backupDir);
			 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
			backup.createNewBackup(src, true);
			// 仅保留最新一份备份，避免增量备份链无界增长
			backup.purgeOldBackups(1);
		} finally {
			// src.close() 不会自动释放列族句柄，需显式关闭
			for (var cfh : cfhs) {
				try {
					cfh.close();
				} catch (Throwable ignored) {
				}
			}
		}
	}

	public static void restore(String backupDir, String dbName) throws RocksDBException {
		try (var restoreOptions = new RestoreOptions(false);
			 var backupOptions = new BackupEngineOptions(backupDir);
			 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
			backup.restoreDbFromLatestBackup(dbName, dbName, restoreOptions);
		}
	}

	private static void writeVarInt(ByteBuffer bb, int i) {
		long v = i & 0xffff_ffffL;
		if (v < (1 << 7))
			bb.WriteByte((byte)v);
		else if (v < (1 << 14)) {
			bb.EnsureWrite(2);
			var bytes = bb.Bytes;
			var wi = bb.WriteIndex;
			bytes[wi] = (byte)(v | 0x80);
			bytes[wi + 1] = (byte)(v >> 7);
			bb.WriteIndex = wi + 2;
		} else if (v < (1 << 21)) {
			bb.EnsureWrite(3);
			var bytes = bb.Bytes;
			var wi = bb.WriteIndex;
			bytes[wi] = (byte)(v | 0x80);
			bytes[wi + 1] = (byte)((v >> 7) | 0x80);
			bytes[wi + 2] = (byte)(v >> 14);
			bb.WriteIndex = wi + 3;
		} else if (v < (1 << 28)) {
			bb.EnsureWrite(4);
			var bytes = bb.Bytes;
			var wi = bb.WriteIndex;
			bytes[wi] = (byte)(v | 0x80);
			bytes[wi + 1] = (byte)((v >> 7) | 0x80);
			bytes[wi + 2] = (byte)((v >> 14) | 0x80);
			bytes[wi + 3] = (byte)(v >> 21);
			bb.WriteIndex = wi + 4;
		} else {
			bb.EnsureWrite(5);
			var bytes = bb.Bytes;
			var wi = bb.WriteIndex;
			bytes[wi] = (byte)(v | 0x80);
			bytes[wi + 1] = (byte)((v >> 7) | 0x80);
			bytes[wi + 2] = (byte)((v >> 14) | 0x80);
			bytes[wi + 3] = (byte)((v >> 21) | 0x80);
			bytes[wi + 4] = (byte)(v >> 28);
			bb.WriteIndex = wi + 5;
		}
	}

	public final class Table {
		private final String name;
		private final ColumnFamilyHandle cfHandle;
		private final int id;
		// 终结标记：销毁列族或关库时置位，供长期租约检查点提前放弃
		private volatile boolean destroyPending;

		public Table(String name, ColumnFamilyHandle cfHandle) {
			this.name = name;
			this.cfHandle = cfHandle;
			id = cfHandle.getID();
		}

		/** 是否处于销毁/关库路径上，供长期租约检查点提前放弃。 */
		public boolean isDestroyPending() {
			return destroyPending;
		}

		public long getKeyNumbers() throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				return rocksDb.getLongProperty(cfHandle, "rocksdb.estimate-num-keys");
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public RocksDatabase getRocksDb() {
			return RocksDatabase.this;
		}

		public String getName() {
			return name;
		}

		public ColumnFamilyHandle getCfHandle() {
			return cfHandle;
		}

		public int getId() {
			return id;
		}

		public byte @Nullable [] get(byte[] key) throws RocksDBException {
			return get(defaultReadOptions, key);
		}

		public byte @Nullable [] get(byte[] key, int offset, int size) throws RocksDBException {
			return get(defaultReadOptions, key, offset, size);
		}

		public byte @Nullable [] get(ReadOptions options, byte[] key) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				var r = rocksDb.get(cfHandle, options, key);
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbGetObserver.observe(System.nanoTime() - timeBegin);
				return r;
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public byte @Nullable [] get(ReadOptions options, byte[] key, int offset, int size)
			throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				var r = rocksDb.get(cfHandle, options, key, offset, size);
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbGetObserver.observe(System.nanoTime() - timeBegin);
				return r;
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void put(byte[] key, byte[] value) throws RocksDBException {
			put(defaultWriteOptions, key, value);
		}

		public void put(byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			put(defaultWriteOptions, key, keyOff, keyLen, value, valueOff, valueLen);
		}

		public void put(WriteOptions options, byte[] key, byte[] value) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				rocksDb.put(cfHandle, options, key, value);
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbPutObserver.observe(System.nanoTime() - timeBegin);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void put(WriteOptions options, byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				rocksDb.put(cfHandle, options, key, keyOff, keyLen, value, valueOff, valueLen);
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbPutObserver.observe(System.nanoTime() - timeBegin);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void delete(byte[] key) throws RocksDBException {
			delete(defaultWriteOptions, key);
		}

		public void delete(byte[] key, int keyOff, int keyLen) throws RocksDBException {
			delete(defaultWriteOptions, key, keyOff, keyLen);
		}

		public void delete(WriteOptions options, byte[] key) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				rocksDb.delete(cfHandle, options, key);
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbDeleteObserver.observe(System.nanoTime() - timeBegin);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void delete(WriteOptions options, byte[] key, int keyOff, int keyLen) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				rocksDb.delete(cfHandle, options, key, keyOff, keyLen);
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbDeleteObserver.observe(System.nanoTime() - timeBegin);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void deleteRange(byte[] first, byte[] last) throws RocksDBException {
			deleteRange(defaultWriteOptions, first, last);
		}

		public void deleteRange(WriteOptions options, byte[] first, byte[] last) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				rocksDb.deleteRange(cfHandle, options, first, last);
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbDeleteRangeObserver.observe(System.nanoTime() - timeBegin);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void put(Transaction t, Binary key, Binary value) throws RocksDBException {
			put(t, key.bytesUnsafe(), key.getOffset(), key.size(),
				value.bytesUnsafe(), value.getOffset(), value.size());
		}

		public void put(Transaction t, byte[] key, byte[] value) throws RocksDBException {
			put(t, key, 0, key.length, value, 0, value.length);
		}

		public void put(Transaction t, byte[] key, int keyLen, byte[] value, int valueLen)
			throws RocksDBException {
			put(t, key, 0, keyLen, value, 0, valueLen);
		}

		public void put(Transaction t, byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				// 仅在keyOff不等于0时拷贝
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				key = Database.copyIf(key, keyOff, keyLen);
				value = Database.copyIf(value, valueOff, valueLen);
				t.put(cfHandle, key, value);
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbTxnPutObserver.observe(System.nanoTime() - timeBegin);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void delete(Transaction t, Binary key) throws RocksDBException {
			delete(t, key.bytesUnsafe(), key.getOffset(), key.size());
		}

		public void delete(Transaction t, byte[] key) throws RocksDBException {
			delete(t, key, 0, key.length);
		}

		public void delete(Transaction t, byte[] key, int keyLen) throws RocksDBException {
			delete(t, key, 0, keyLen);
		}

		public void delete(Transaction t, byte[] key, int keyOff, int keyLen) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				// 仅在keyOff不等于0时拷贝
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				key = Database.copyIf(key, keyOff, keyLen);
				t.delete(cfHandle, key);
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbTxnDeleteObserver.observe(System.nanoTime() - timeBegin);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void put(Batch batch, Binary key, Binary value) throws RocksDBException {
			put(batch, key.bytesUnsafe(), key.getOffset(), key.size(),
				value.bytesUnsafe(), value.getOffset(), value.size());
		}

		public void put(Batch batch, byte[] key, byte[] value) throws RocksDBException {
			batch.put(cfHandle, key, value);
		}

		public void put(Batch batch, byte[] key, int keyLen, byte[] value, int valueLen)
			throws RocksDBException {
			batch.put(cfHandle, key, keyLen, value, valueLen);
		}

		public void put(Batch batch, byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			if (keyOff != 0)
				key = Arrays.copyOfRange(key, keyOff, keyOff + keyLen);
			if (valueOff != 0)
				value = Arrays.copyOfRange(value, valueOff, valueOff + valueLen);
			batch.put(cfHandle, key, keyLen, value, valueLen);
		}

		public void delete(Batch batch, Binary key) throws RocksDBException {
			delete(batch, key.bytesUnsafe(), key.getOffset(), key.size());
		}

		public void delete(Batch batch, byte[] key) throws RocksDBException {
			batch.delete(cfHandle, key);
		}

		public void delete(Batch batch, byte[] key, int keyLen) throws RocksDBException {
			batch.delete(cfHandle, key, keyLen);
		}

		public void delete(Batch batch, byte[] key, int keyOff, int keyLen) throws RocksDBException {
			if (keyOff == 0)
				batch.delete(cfHandle, key, keyLen);
			else
				batch.delete(cfHandle, Arrays.copyOfRange(key, keyOff, keyOff + keyLen));
		}

		public void put(Batch2 batch, byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) {
			var bb = batch.bb;
			if (id == 0)
				bb.WriteByte(1); // kTypeValue
			else {
				bb.WriteByte(5); // kTypeColumnFamilyValue
				writeVarInt(bb, id);
			}
			writeVarInt(bb, keyLen);
			bb.Append(key, keyOff, keyLen);
			writeVarInt(bb, valueLen);
			bb.Append(value, valueOff, valueLen);
			batch.count++;
		}

		public void delete(Batch2 batch, byte[] key, int keyOff, int keyLen) {
			var bb = batch.bb;
			if (id == 0)
				bb.WriteByte(0); // kTypeDeletion
			else {
				bb.WriteByte(4); // kTypeColumnFamilyDeletion
				writeVarInt(bb, id);
			}
			writeVarInt(bb, keyLen);
			bb.Append(key, keyOff, keyLen);
			batch.count++;
		}

		// RocksIterator用完时需确保调用close回收堆外内存,推荐使用try(var it = iterator()) {...}
		public RocksIterator iterator() {
			RocksDatabase.this.enterOp();
			try {
				var nativeIterator = rocksDb.newIterator(cfHandle, defaultReadOptions);
				// 登记完成后才释放创建租约，close 的两种排空保护连续交接。
				try {
					var it = new RegisteredIterator(nativeIterator);
					RocksDatabase.this.openIterators.add(it);
					return it;
				} catch (Throwable e) {
					nativeIterator.close();
					throw e;
				}
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		// 有数据时也可以直接删除整个列族。
		public void drop() throws Exception {
			dropTable(name);
		}

		public void compact() throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				rocksDb.compactRange(cfHandle);
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbCompactObserver.observe(System.nanoTime() - timeBegin);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void clear() throws RocksDBException {
			try (var it = iterator()) {
				// 新建迭代器未定位，须先 seekToFirst 再删至末尾
				it.seekToFirst();
				deleteToEnd(it);
			}
		}

		public void deleteToEnd(RocksIterator it) throws RocksDBException {
			if (RocksDatabase.isValidOrThrow(it)) {
				var first = it.key();
				it.seekToLast();
				if (RocksDatabase.isValidOrThrow(it)) {
					var last = it.key();
					// deleteRange 不包含last，需要制造一个比当前last后面的key。
					last = Arrays.copyOf(last, last.length + 1);
					deleteRange(first, last);
				}
			}
		}

	}

	/** 保持 RocksIterator 返回类型；隐藏的原始包装器是 native 句柄的唯一释放出口。 */
	private final class RegisteredIterator extends RocksIterator {
		private final RocksIterator nativeIterator;

		RegisteredIterator(RocksIterator nativeIterator) {
			super(rocksDb, nativeIterator.getNativeHandle());
			this.nativeIterator = nativeIterator;
		}

		@Override
		protected void disposeInternal() {
			try {
				// 不调用 super.disposeInternal()，否则两个包装器会重复释放同一 native 句柄。
				nativeIterator.close();
			} finally {
				// 不能在 owningHandle 变为 false 时提前摘除：此时 native close 可能仍在进行。
				openIterators.remove(this);
			}
		}
	}

	public class Batch implements Closeable {
		private final WriteBatch batch = new WriteBatch();

		public WriteBatch getWriteBatch() {
			return batch;
		}

		public int getCount() {
			return batch.count();
		}

		public boolean isClosed() {
			return !batch.isOwningHandle();
		}

		public void put(ColumnFamilyHandle cfh, byte[] key, byte[] value) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				batch.put(cfh, key, value);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void put(ColumnFamilyHandle cfh, byte[] key, int keyLen, byte[] value, int valueLen)
			throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				try {
					mhWriteBatchPutCf.invokeExact(batch, batch.getNativeHandle(), key, keyLen, value, valueLen,
						cfh.getNativeHandle());
				} catch (Throwable e) { // rethrow
					throw Task.forceThrow(e);
				}
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void delete(ColumnFamilyHandle cfh, byte[] key) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				batch.delete(cfh, key);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void delete(ColumnFamilyHandle cfh, byte[] key, int keyLen) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				try {
					mhWriteBatchDeleteCf.invokeExact(batch, batch.getNativeHandle(), key, keyLen, cfh.getNativeHandle());
				} catch (Throwable e) { // rethrow
					throw Task.forceThrow(e);
				}
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public void commit() throws RocksDBException {
			commit(defaultWriteOptions);
		}

		public void commit(WriteOptions options) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				rocksDb.write(options, batch);
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbWriteObserver.observe(System.nanoTime() - timeBegin);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		// clear后可以再次put,delete,commit. 复用Batch性能更高
		public void clear() {
			batch.clear();
		}

		@Override
		public void close() {
			batch.close();
		}
	}

	/** 在 JVM 堆内序列化批量操作，提交时一次性构造 WriteBatch 写入以减少 JNI 调用；通过 Table 写入。 */
	public class Batch2 {
		private final ByteBuffer bb;
		private int count;

		public Batch2() {
			this(32);
		}

		public Batch2(int capacity) {
			bb = ByteBuffer.Allocate(capacity);
			bb.EnsureWrite(12);
			bb.WriteIndex = 12;
		}

		public void commit() throws RocksDBException {
			commit(defaultWriteOptions);
		}

		public void commit(WriteOptions options) throws RocksDBException {
			RocksDatabase.this.enterOp();
			try {
				var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
				ByteBuffer.intLeHandler.set(bb.Bytes, 8, count);
				try (var wb = (WriteBatch)mhWriteBatchNew.invokeExact(
					(long)mhWriteBatchNativeNew.invokeExact(bb.Bytes, bb.WriteIndex), true)) {
					rocksDb.write(options, wb);
				} catch (Throwable e) { // rethrow
					throw Task.forceThrow(e);
				}
				if (timeBegin != 0) // 统计禁用时零开销
					rocksDbWriteObserver.observe(System.nanoTime() - timeBegin);
			} finally {
				RocksDatabase.this.exitOp();
			}
		}

		public byte[] copy() {
			ByteBuffer.intLeHandler.set(bb.Bytes, 8, count);
			return Arrays.copyOf(bb.Bytes, bb.WriteIndex);
		}

		// clear后可以再次put,delete,commit. 复用Batch性能更高
		public void clear() {
			bb.WriteIndex = 12;
			count = 0;
		}
	}
}
