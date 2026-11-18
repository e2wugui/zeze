package Zeze.Transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;

import Zeze.Application;
import Zeze.Onz.OnzProcedure;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.FastLock;
import Zeze.Util.Task;
import Zeze.Util.ZezeCounter;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import Zeze.Transaction.IHistory.PendingHistory;

/**
 * 检查点：按模式（Immediately/Table）把脏记录集（RelativeRecordSet）成批落库，
 * 协调多数据库事务与本地 Rocks 镜像的最终持久化，并管理检查点线程的启停与终检点。
 */
public final class Checkpoint {
	static final Logger logger = LogManager.getLogger(Checkpoint.class);

	/** 独占记录列表及同下标编码列；列表和编码数组随批次一起复用。 */
	private static final class FlushBatch {
		private final ArrayList<Record> records;
		private @Nullable ByteBuffer[] keys;
		private @Nullable ByteBuffer[] values;
		private @Nullable SQLStatement[] sqlKeys;
		private @Nullable SQLStatement[] sqlValues;

		FlushBatch(int capacity) {
			records = new ArrayList<>(capacity);
			keys = new ByteBuffer[capacity];
			values = new ByteBuffer[capacity];
			sqlKeys = new SQLStatement[capacity];
			sqlValues = new SQLStatement[capacity];
		}

		// 仅在准备阶段收集记录；编码开始后不再添加。
		void add(Record record) {
			int size = records.size();
			if (size == keys.length) {
				int capacity = Math.max(size + 1, size + size / 2);
				// 上批已清空，本批尚未编码；新编码列不需要复制。
				var newKeys = new ByteBuffer[capacity];
				var newValues = new ByteBuffer[capacity];
				var newSqlKeys = new SQLStatement[capacity];
				var newSqlValues = new SQLStatement[capacity];
				// 扩容全部成功后再发布编码列。
				records.ensureCapacity(capacity);
				keys = newKeys;
				values = newValues;
				sqlKeys = newSqlKeys;
				sqlValues = newSqlValues;
			}
			records.add(record);
		}

		void encode() {
			for (int index = 0, size = records.size(); index < size; index++) {
				var record = records.get(index);
				if (record.getDirty()) {
					var table = (TableX<?, ?>)record.getTable();
					var key = record.getObjectKey();
					var value = record.getDirtyValue();
					if (table.isRelationalMapping() && table.getDatabase() instanceof DatabaseRelationalMapping) {
						sqlKeys[index] = table.encodeSqlKey(key);
						sqlValues[index] = TableX.encodeSqlValue(value);
					}
					// KV 主库与本地镜像共用编码；SQL 主库另用关系编码。
					keys[index] = table.encodeKey(key);
					values[index] = TableX.encodeValue(value);
				}
			}
		}

		void writeTo(IdentityHashMap<Database, Database.Transaction> transactions, Database.Transaction local) {
			for (int index = 0, size = records.size(); index < size; index++) {
				var record = records.get(index);
				if (!record.getDirty())
					continue;
				var table = record.getTable();
				var key = Objects.requireNonNull(keys[index], "flush key not encoded");
				var value = values[index];
				var storage = table.getStorage();
				Database.@Nullable Table oldTable = null;
				if (storage != null) {
					var databaseTable = storage.getDatabaseTable();
					var transaction = Objects.requireNonNull(transactions.get(databaseTable.getDatabase()),
						"unprepared snapshot transaction");
					var sqlKey = sqlKeys[index];
					if (sqlKey != null) {
						var sqlValue = sqlValues[index];
						if (sqlValue != null)
							databaseTable.replace(transaction, sqlKey, sqlValue);
						else {
							databaseTable.remove(transaction, sqlKey);
							oldTable = table.getOldTable();
						}
					} else if (value != null)
						databaseTable.replace(transaction, key, value);
					else {
						databaseTable.remove(transaction, key);
						oldTable = table.getOldTable();
					}
				} else if (value != null) {
					// 内存表以本地镜像作为唯一存储，新写仍使用 KV 写入预算。
					Database.checkKvKeyLength(table.getName(), key);
				}
				// 镜像按自己的二进制 value 决定更新/删除；旧表也始终使用二进制 key。
				if (value != null)
					table.getLocalRocksCacheTable().replace(local, key, value);
				else {
					table.getLocalRocksCacheTable().remove(local, key);
					if (oldTable != null) {
						var transaction = Objects.requireNonNull(transactions.get(oldTable.getDatabase()),
							"unprepared snapshot transaction");
						oldTable.remove(transaction, key);
					}
				}
			}
		}

		void markClean() {
			//noinspection ForLoopReplaceableByForEach
			for (int index = 0, size = records.size(); index < size; index++)
				records.get(index).setDirty(false);
		}

		void clear() {
			int size = records.size();
			records.clear();
			Arrays.fill(keys, 0, size, null);
			Arrays.fill(values, 0, size, null);
			Arrays.fill(sqlKeys, 0, size, null);
			Arrays.fill(sqlValues, 0, size, null);
		}
	}

	final Application zeze;
	private final CheckpointMode mode;
	private final Thread checkpointThread;
	private final FastLock lock = new FastLock();
	private final Condition cond = lock.newCondition();
	private int period;
	private volatile boolean isRunning;
	final ConcurrentHashSet<RelativeRecordSet> relativeRecordSetMap = new ConcurrentHashSet<>();
	// 仅缓存已清空的缓冲区；取出后归当前批次独占，并发/重入不会共享。
	private static final int MAX_RETAINED_FLUSH_BATCHES = Math.max(1, Runtime.getRuntime().availableProcessors());
	// 仅限制闲置数组容量；超限批次归还时丢弃，不限制单次 flush 条数。
	private static final int MAX_RETAINED_FLUSH_RECORDS = 16_384;
	// 预分配到缓存上限，借还不分配节点；monitor 只保护 deque，不覆盖编码或 I/O。
	private final ArrayDeque<FlushBatch> flushBatches = new ArrayDeque<>(MAX_RETAINED_FLUSH_BATCHES);

	private FlushBatch acquireFlushBatch(Iterable<Record> rs) {
		FlushBatch batch;
		synchronized (flushBatches) {
			batch = flushBatches.pollLast();
		}
		return batch != null ? batch : new FlushBatch(rs instanceof Collection<?> collection ? collection.size() : 10);
	}

	private void releaseFlushBatch(FlushBatch batch) {
		batch.clear(); // 成功或失败都先清引用，再放回缓存。
		if (batch.keys.length > MAX_RETAINED_FLUSH_RECORDS)
			return;
		synchronized (flushBatches) {
			if (flushBatches.size() < MAX_RETAINED_FLUSH_BATCHES)
				flushBatches.addLast(batch);
		}
	}

	// 关库前等待已开始的 flush，避免关闭仍被事务使用的 RocksDB。
	// monitor 仅保护计数、停收与等待；持有它时不执行 flush，也不获取 RRS/Application 锁。
	private final Object activeFlushMonitor = new Object();
	private int activeFlush; // guarded by activeFlushMonitor
	private boolean acceptingFlushes = true; // guarded by activeFlushMonitor

	// 符号位表示仍接收提交，低 31 位为在途计数；热路径仅做原子增减。
	// 停收与计数共用一个原子状态：关门前退出的计数由停机线程直接检查，关门后归零的退出负责唤醒。
	// commitMonitor 仅用于停机等待及归零通知；持 RRS 锁的退出路径只通知、不等待。
	private final AtomicInteger commitState = new AtomicInteger(Integer.MIN_VALUE);
	private final Object commitMonitor = new Object();

	boolean tryBeginCommit() {
		// 先增计数再验门；停收后回退计数，归零时同样需要唤醒等待者。
		int prev = commitState.getAndIncrement();
		if (prev < 0)
			return true;
		if (commitState.getAndDecrement() == 1) {
			synchronized (commitMonitor) {
				commitMonitor.notifyAll();
			}
		}
		return false;
	}

	void endCommit() {
		int prev = commitState.getAndDecrement();
		if (prev < 0)
			return; // 停机线程先关门再等待。
		if ((prev & Integer.MAX_VALUE) == 1) { // 门已关且本次退出使计数归零
			synchronized (commitMonitor) {
				commitMonitor.notifyAll();
			}
		}
	}

	private void stopAcceptingCommitsAndWait() {
		boolean interrupted = false;
		for (; ; ) { // 清符号位关门，允许并发重复调用。
			int s = commitState.get();
			if (s < 0) {
				if (commitState.compareAndSet(s, s & Integer.MAX_VALUE))
					break;
			} else {
				break;
			}
		}
		// 使用权覆盖日志应用至脏集登记/同步落库。终检点不能越过仍可能登记的提交。
		synchronized (commitMonitor) {
			while (commitState.get() != 0) {
				try {
					commitMonitor.wait();
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		}
		if (interrupted)
			Thread.currentThread().interrupt();
	}

	public Checkpoint(Application zeze, CheckpointMode mode, int serverId) {
		this(zeze, mode, null, serverId);
	}

	public Checkpoint(Application zeze, CheckpointMode mode, @Nullable Iterable<Database> dbs,
					  int serverId) {
		this.zeze = zeze;
		this.mode = mode;
		if (dbs != null)
			add(dbs);
		checkpointThread = new Thread(() -> TaskSpec.ofAction(this::run).name("Checkpoint.Run").call(), "Checkpoint-" + serverId);
		checkpointThread.setDaemon(true);
		checkpointThread.setPriority(Thread.NORM_PRIORITY + 2);
		checkpointThread.setUncaughtExceptionHandler((__, e) -> logger.error("uncaught exception", e));
	}

	public Application getZeze() {
		return zeze;
	}

	public CheckpointMode getCheckpointMode() {
		return mode;
	}

	public Checkpoint add(Iterable<Database> databases) {
		// 兼容旧调用入口；flush 按 relativeRecordSetMap 分组，不保存 databases。
		return this;
	}

	public void start(int period) {
		lock.lock();
		try {
			if (isRunning)
				return;

			isRunning = true;
			this.period = period;
			checkpointThread.start();
		} finally {
			lock.unlock();
		}
	}

	public void stopAndJoin() {
		stopAcceptingCommitsAndWait();
		lock.lock();
		try {
			isRunning = false;
			cond.signal();
		} finally {
			lock.unlock();
		}
		try {
			checkpointThread.join();
		} catch (InterruptedException e) {
			throw Task.forceThrow(e);
		}
	}

	/**
	 * 有界等待检查点线程退出，避免关库与最终 flush 并发；中断不提前返回，退出时恢复标志。
	 *
	 * @param timeoutMillis 最长等待毫秒数，超时记录错误并返回
	 */
	public void joinIgnoreInterrupt(long timeoutMillis) {
		var deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
		boolean interrupted = false;
		try {
			while (checkpointThread.isAlive()) {
				var remaining = deadline - System.nanoTime();
				if (remaining <= 0) {
					logger.error("join checkpoint thread timeout ({}ms), continue", timeoutMillis);
					return;
				}
				try {
					checkpointThread.join(remaining / 1_000_000L + 1);
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	/**
	 * 关库前有界等待在途 flush 归零，超时返回 false。
	 * 中断不提前返回，退出时恢复标志。
	 */
	public boolean waitNoActiveFlush(long timeoutMillis) {
		var deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
		boolean interrupted = false;
		try {
			synchronized (activeFlushMonitor) {
				acceptingFlushes = false;
				while (activeFlush > 0) {
					var remaining = deadline - System.nanoTime();
					if (remaining <= 0)
						return false;
					try {
						TimeUnit.NANOSECONDS.timedWait(activeFlushMonitor, remaining);
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
				return true;
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	/**
	 * 致命退出前停止接收提交，等待已获准提交完成，再重试刷脏。
	 * 调用方必须先启动独立 halt watchdog，为锁等待和保存重试提供截止时间。
	 */
	public void saveBeforeFatalExit() {
		stopAcceptingCommitsAndWait();
		while (true) {
			try {
				runOnce();
			} catch (Throwable ex) {
				logger.error("fatal checkpoint failed, retry within protected window", ex);
			}
			if (relativeRecordSetMap.isEmpty()) {
				if (waitNoActiveFlush(Application.HALT_DEADLINE_MS))
					return;
			}
			try {
				//noinspection BusyWait
				Thread.sleep(10);
			} catch (InterruptedException ignored) {
				// 保存时限由独立 halt watchdog 保证，中断不提前结束保存。
			}
		}
	}

	public void runOnce() {
		var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
		switch (getCheckpointMode()) {
		case Immediately:
			break;

		case Table:
			RelativeRecordSet.flushWhenCheckpoint(this);
			break;
		}
		if (timeBegin != 0)
			RunOnceObserver.observe(System.nanoTime() - timeBegin);
	}

	// 复用统计句柄，代际重绑由观察者内部处理。
	private static final ZezeCounter.LongObserver RunOnceObserver =
		ZezeCounter.instance.getRunTimeObserver("Checkpoint.runOnce");

	private void run() {
		while (isRunning) {
			try {
				// 首轮也等待 period，避免线程延迟启动时立即补刷。
				lock.lock();
				try {
					//noinspection ResultOfMethodCallIgnored
					cond.await(period, TimeUnit.MILLISECONDS);
				} finally {
					lock.unlock();
				}
				if (!isRunning)
					break; // stopAndJoin的signal唤醒：flush交给循环外的final checkpoint。
				//noinspection SwitchStatementWithTooFewBranches
				switch (mode) {
				case Table:
					RelativeRecordSet.flushWhenCheckpoint(this);
					break;

				default:
					break;
				}
			} catch (Throwable ex) {
				logger.error("Run Exception", ex);
			}
		}
		logger.info("final checkpoint start.");
		//noinspection SwitchStatementWithTooFewBranches
		switch (mode) {
		case Table:
			// 终检点有界补刷残留脏集；失败单元留在 map，不能无限重试。
			for (int round = 0; round < 3; ++round) {
				RelativeRecordSet.flushWhenCheckpoint(this);
				if (relativeRecordSetMap.isEmpty())
					break;
			}
			// 残留脏集会随进程退出丢失，记录详细诊断后继续停机。
			if (!relativeRecordSetMap.isEmpty())
				logger.fatal("final checkpoint end with {} dirty record set(s) unresolved: {}",
					relativeRecordSetMap.size(), relativeRecordSetMap);
			break;
		}
		logger.info("final checkpoint end.");
	}

	public void flush(Transaction trans, @Nullable OnzProcedure onzProcedure, @Nullable PendingHistory history) {
		var records = new ArrayList<Record>(trans.getAccessedRecords().size());
		for (var ar : trans.getAccessedRecords().values()) {
			if (ar.dirty)
				records.add(ar.atomicTupleRecord.record());
		}
		flush(records, onzProcedure != null ? Set.of(onzProcedure) : Set.of(), history);
	}

	private String extractTableNames(Iterable<Record> rs, IHistory.@Nullable PendingHistory history) {
		var tables = new HashSet<String>();
		for (var r : rs)
			tables.add(r.getTable().getName());
		if (null != history)
			tables.add(history.getTable().getName());
		return tables.toString();
	}

	public void flush(Iterable<Record> rs, @Nullable Set<OnzProcedure> onzProcedures,
					  @Nullable PendingHistory history) {
		// 必须在创建数据库事务前登记在途 flush，供关库前等待。
		synchronized (activeFlushMonitor) {
			if (!acceptingFlushes)
				throw new IllegalStateException("checkpoint flush rejected after drain started");
			++activeFlush;
		}
		try {
			flushInternal(rs, onzProcedures, history);
		} finally {
			synchronized (activeFlushMonitor) {
				--activeFlush;
				activeFlushMonitor.notifyAll();
			}
		}
	}

	private void flushInternal(Iterable<Record> rs, @Nullable Set<OnzProcedure> onzProcedures,
							   IHistory.@Nullable PendingHistory history) {
		FlushBatch batch = null;
		// History 与记录同批落库。
		try {
			// 快照编码和 Onz 通知只执行一次；落库重试使用新事务重放整批。
			var databases = new LinkedHashSet<Database>();
			batch = acquireFlushBatch(rs);
			for (var r : rs) {
				var storage = r.getTable().getStorage();
				if (storage != null) {
					databases.add(storage.getDatabaseTable().getDatabase());
					var oldTable = r.getTable().getOldTable();
					if (oldTable != null)
						databases.add(oldTable.getDatabase());
				}
				batch.add(r);
			}
			var historyTable = history != null ? history.getTable() : null;
			if (historyTable != null)
				databases.add(historyTable.getDatabase());

			if (history != null)
				history.encode0();

			batch.encode();
			OnzProcedure.sendFlushAndWait(onzProcedures);

			// 仅在全部事务回滚成功且后端判定为暂时性写冲突时重试。
			// writeTo 按键 replace/remove，重放整批幂等。
			for (int attempt = 1; ; attempt++) {
				var dts = new IdentityHashMap<Database, Database.Transaction>();
				//noinspection DataFlowIssue
				var localCacheTransaction = zeze.getLocalRocksCacheDb().beginTransaction();
				try {
					for (var database : databases)
						dts.put(database, database.beginTransaction());
					batch.writeTo(dts, localCacheTransaction);
					if (history != null) {
						var storage = historyTable.getStorage();
						//noinspection DataFlowIssue
						history.writeOnly(storage.getDatabaseTable(), dts.get(historyTable.getDatabase()));
					}
					// 逐库 commit 不保证跨库原子；内存脏集仅能在进程存活时重刷，重启不保证恢复。
					// 需要原子提交的数据应放在同一库，或使用外部协调者。
					for (var t : dts.values())
						t.commit();
					localCacheTransaction.commit();
					if (history != null)
						history.onPersisted(); // 持久化成功后才清容器，失败时保留供重试。
					// Immediately 和 Table 模式均在全部保存成功后清脏。
					batch.markClean();
					return;
				} catch (Throwable e) {
					if (rollbackAllQuietly(dts, localCacheTransaction)
							&& attempt < MAX_TRANSIENT_FLUSH_ATTEMPTS
							&& isTransientWriteConflict(databases, e)) {
						logger.warn("checkpoint flush attempt {} transient write conflict, retrying", attempt, e);
						sleepBeforeRetry(attempt);
						continue;
					}
					throw e;
				} finally {
					for (var t : dts.values()) {
						try {
							t.close();
						} catch (Throwable e) {
							logger.error("Flush close Exception transaction={}", t, e);
						}
					}
					try {
						localCacheTransaction.close();
					} catch (Throwable e) {
						logger.error("Flush close Exception transaction={}", localCacheTransaction, e);
					}
				}
			}
		} catch (Throwable e) {
			// 诊断信息组装失败不能覆盖原始 flush 异常。
			String tableNames;
			try {
				tableNames = extractTableNames(rs, history);
			} catch (Throwable diagEx) {
				logger.error("extract table names for flush failure diagnostics failed", diagEx);
				tableNames = "[table names unavailable]";
			}
			throw new RuntimeException(tableNames, e);
		} finally {
			if (batch != null)
				releaseFlushBatch(batch);
		}
	}

	private static final int MAX_TRANSIENT_FLUSH_ATTEMPTS = 4;

	/** 任一事务回滚失败则返回 false，状态未知时禁止重放。 */
	private static boolean rollbackAllQuietly(IdentityHashMap<Database, Database.Transaction> dts,
											   Database.Transaction localCacheTransaction) {
		boolean clean = true;
		for (var t : dts.values()) {
			try {
				t.rollback();
			} catch (Throwable ex) {
				clean = false;
				logger.error("Flush Rollback Exception", ex);
			}
		}
		try {
			localCacheTransaction.rollback();
		} catch (Throwable ex) {
			clean = false;
			logger.error("Flush Rollback Exception", ex);
		}
		return clean;
	}

	private static boolean isTransientWriteConflict(Collection<Database> databases, Throwable e) {
		for (var database : databases)
			if (database.isTransientWriteConflict(e))
				return true;
		return false;
	}

	private static void sleepBeforeRetry(int attempt) {
		try {
			TimeUnit.MILLISECONDS.sleep(1L << attempt);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt(); // 重放照常进行，中断状态交由外层处置
		}
	}

	// 调用方须持有 rs 锁，并在外部确认 rs 未被合并。
	public void flush(RelativeRecordSet rs) {
		if (rs.getRecordSet() != null)
			flush(rs.getRecordSet(), rs.getOnzProcedures(), rs.getHistory());
	}
}
