package Zeze.Transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
				// ArrayList 扩容时复制已有 Record 引用；全部成功后再发布编码列。
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
				// 编码回调可能重入并清脏，不能把旧快照覆盖到内层已提交的新值上。
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
			// 按下标遍历，批次复用时不创建 Iterator。
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
	// 限制闲置数组容量，不限制单次 flush 条数；五个引用数组在压缩引用下每份约 320KiB。
	// 超限批次归还时丢弃，避免偶发大批次长期占用缓存。
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

	// 在飞flush计数（锁内inc/dec）——Application.stop在终检点后、LocalRocksCacheDb.close前
	// 有界等待它归零。停机拒绝只拦"新提交"，不等待已过门的在飞flush（Immediately模式
	// 业务线程的checkpoint.flush、Reduce降级flush、checkpointRun的runOnce）：它们已打开
	// LocalRocksCacheDb事务，与close/deleteDirectory并发是native UAF类。
	// monitor只在计数增减与等待处短暂持有，flush体内不持有——与rrs锁、Application锁无嵌套。
	private final Object activeFlushMonitor = new Object();
	private int activeFlush; // guarded by activeFlushMonitor
	private boolean acceptingFlushes = true; // guarded by activeFlushMonitor

	// 提交使用权（在飞提交计数+停收门），位编码进单个AtomicInteger：符号位=门开（仍在收），
	// 低31位=在飞计数。进入=一次getAndIncrement，退出=一次getAndDecrement——热路径全程
	// 无monitor、无notify（此前monitor版每提交两次synchronized+无条件notifyAll，JDK21无
	// 偏向锁下空事务内核循环实测-22%，平台线程高并发争用下更甚）。monitor只用于停机侧等待
	// 与停收后归零退出的唤醒；归零退出可能发生在持rrs锁下（许可证随提交路径finally释放），
	// 但monitor内只有notify不等待，且无人以相反顺序持锁，不成环。
	// 唤醒无遗漏：停收CAS与计数增减线性一致地钉在同一字上——若某次退出的prev仍见门开，
	// 停机线程的停收CAS必然晚于它，其停收后的读数已含这次退出，计数已见0不等待；
	// 若prev见门已关，计数归零的那次退出负责进monitor notifyAll。
	private final AtomicInteger commitState = new AtomicInteger(Integer.MIN_VALUE);
	private final Object commitMonitor = new Object();

	boolean tryBeginCommit() {
		// 先自增后验门：门开即成功——热路径恰好一次RMW（get+CAS的合成）。门关时误加的
		// 计数立即回退（仅停机窗口内的罕见路径），回退若使计数归零同样要补唤醒（停机线程
		// 可能正等到它）。自增不会把开位挤掉：门开时符号位=1，低31位计数加1不进位到符号位
		// （2^31在飞提交不可达）。
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
			return; // 门仍开：不可能有等待者（停机线程必先关门，见stopAcceptingCommitsAndWait）
		if ((prev & Integer.MAX_VALUE) == 1) { // 门已关且本次退出使计数归零
			synchronized (commitMonitor) {
				commitMonitor.notifyAll();
			}
		}
	}

	private void stopAcceptingCommitsAndWait() {
		boolean interrupted = false;
		for (; ; ) { // 关门：清符号位（幂等，并发stop安全）
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
			while (commitState.get() != 0) { // 门已关，state即计数
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
		// 登记的databases集合从未被读取（flush按relativeRecordSetMap分组），
		// 保留入口仅为兼容既有调用形态，不再存储。
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
	 * 有界忽略中断地join检查点线程——stopAndJoin的join被中断
	 * （forceThrow）时线程仍存活（可能正要进入final flush），调用方继续关库会与它并发
	 * （close与数据通路并发属native UAF类）。中断只恢复标志，join持续到deadline。
	 *
	 * @param timeoutMillis 最长等待毫秒数，超时记error返回（调用方自行决定是否继续）
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
					interrupted = true; // 出口统一恢复标志；循环内保持清除以便后续join能真正等待
				}
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	/**
	 * 有界等待在飞flush归零（{@link #flush(Iterable, Set, IHistory.PendingHistory)}入口inc、finally dec）。
	 * 停机序列在终检点后、LocalRocksCacheDb.close前调用，超时返回false由调用方告警继续。
	 * 中断只恢复标志不提前返回：等待本身已有deadline兜底。
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
						interrupted = true; // 出口统一恢复标志；循环内保持清除以便后续timedWait能真正等待
					}
				}
				return true;
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
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

	// 常量键的统计句柄一次解析终身复用（对齐 Task.runTimeObservers 惯例；
	// 观察者内部自带代际重绑）
	private static final ZezeCounter.LongObserver RunOnceObserver =
		ZezeCounter.instance.getRunTimeObserver("Checkpoint.runOnce");

	private void run() {
		while (isRunning) {
			try {
				// 先等后刷：首轮同样受period保护。start()启动的本线程在负载下可能被延迟调度，
				// 若立即执行首轮flush，会在任意时刻"补跑"一轮——被延迟的首轮把已提交未断言的
				// 内存值提前刷进镜像，锁忙回退镜像读到新值（TestFlushUnitIsolation.walkMemory
				// 因此约50%失败）。启动时relativeRecordSetMap通常为空，延迟首轮无实际影响。
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
			} catch (Throwable ex) { // logger.error
				// thread worker.
				logger.error("Run Exception", ex);
			}
		}
		logger.info("final checkpoint start.");
		//noinspection SwitchStatementWithTooFewBranches
		switch (mode) {
		case Table:
			// 终检点补轮——停机拒绝生效前已过检查的在途提交可能在终检点轮次进行中
			// 或之后才注册rrs（_lock_等锁醒来），flush后map仍非空时补轮收敛迟到的脏集；
			// 必须有界：flush失败的单元保留在map中，不设界会无限重试。
			for (int round = 0; round < 3; ++round) {
				RelativeRecordSet.flushWhenCheckpoint(this);
				if (relativeRecordSetMap.isEmpty())
					break;
			}
			// 有界轮耗尽仍有残留脏集=持续落库失败（DB硬故障）：进程即将退出而脏数据
			// 被静默丢弃。fatal告警残留规模与内容（含记录键），供运维定位；不阻止退出——
			// 持续失败下无限等待只会把停机变成挂死。
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
		// 在飞计数从首个数据库触碰（LocalRocksCacheDb.beginTransaction）前开始，
		// 覆盖整个落库过程——stop的waitNoActiveFlush据此等待后才能close/delete目录。
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
							   @Nullable PendingHistory history) {
		var dts = new IdentityHashMap<Database, Database.Transaction>();
		Database.Transaction localCacheTransaction = Objects.requireNonNull(zeze.getLocalRocksCacheDb()).beginTransaction();
		FlushBatch batch = null;

		// history.logChanges原子的和数据一起flush。
		try {
			batch = acquireFlushBatch(rs);
			// prepare: 每库一个事务，快照由本批次独占的连续缓冲区持有。
			for (var r : rs) {
				var storage = r.getTable().getStorage();
				if (storage != null) {
					var database = storage.getDatabaseTable().getDatabase();
					//noinspection resource
					dts.computeIfAbsent(database, Database::beginTransaction);
					var oldTable = r.getTable().getOldTable();
					if (oldTable != null)
						//noinspection resource
						dts.computeIfAbsent(oldTable.getDatabase(), Database::beginTransaction);
				}
				batch.add(r);
			}
			var historyTable = history != null ? history.getTable() : null;
			var historyTransaction = history != null
				? dts.computeIfAbsent(historyTable.getDatabase(), Database::beginTransaction)
				: null;

			// 编码
			if (history != null)
				history.encode0();

			batch.encode();
			OnzProcedure.sendFlushAndWait(onzProcedures);
			// 保存到数据库中
			batch.writeTo(dts, localCacheTransaction);
			if (history != null) {
				var storage = historyTable.getStorage();
				//noinspection DataFlowIssue
				history.writeOnly(storage.getDatabaseTable(), historyTransaction);
			}
			// 提交。多库为逐库commit、本地不保证跨库原子：第二库失败时先提交的库已生效，
			// 崩溃窗口由重启后的checkpoint重放收敛（脏标记未清除的记录重新flush）；
			// 需要严格跨库原子应单库部署或引入外部协调者，框架不引入XA。
			for (var t : dts.values())
				t.commit();
			localCacheTransaction.commit();
			if (history != null)
				history.onPersisted(); // tHistory 行已持久化才清容器；失败回滚后保留，重试幂等重写
			// 保存成功，清除脏标记。
			// Immediately 模式的记录不进入 RelativeRecordSet，这里是它唯一的清除点。
			// Table 模式现在也在这里清除。
			batch.markClean();
		} catch (Throwable e) { // rethrow
			for (var t : dts.values()) {
				try {
					t.rollback();
				} catch (Throwable ex) { // logger.error
					logger.error("Flush Rollback Exception", ex);
				}
			}
			try {
				localCacheTransaction.rollback();
			} catch (Throwable ex) { // logger.error
				logger.error("Flush Rollback Exception", ex);
			}
			// 诊断信息（表名清单）组装失败不得顶掉真正的flush失败原因（记录结构异常时
			// extractTableNames可能再抛），退化为占位、原始异常保持为cause。
			String tableNames;
			try {
				tableNames = extractTableNames(rs, history);
			} catch (Throwable diagEx) { // logger.error
				logger.error("extract table names for flush failure diagnostics failed", diagEx);
				tableNames = "[table names unavailable]";
			}
			throw new RuntimeException(tableNames, e);
		} finally {
			for (var t : dts.values()) {
				try {
					t.close();
				} catch (Throwable e) { // logger.error
					logger.error("Flush close Exception transaction={}", t, e);
				}
			}
			try {
				localCacheTransaction.close();
			} catch (Throwable e) { // logger.error
				logger.error("Flush close Exception transaction={}", localCacheTransaction, e);
			}
			if (batch != null)
				releaseFlushBatch(batch);
		}
	}

	// under lock(rs)
	public void flush(RelativeRecordSet rs) {
		// rs.MergeTo == null &&  check outside
		if (rs.getRecordSet() != null)
			flush(rs.getRecordSet(), rs.getOnzProcedures(), rs.getHistory()); // 脏标记在 flush(Iterable) 保存成功后统一清除。
	}
}
