package Zeze.Transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import Zeze.Application;
import Zeze.History.History;
import Zeze.Onz.OnzProcedure;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.FastLock;
import Zeze.Util.Task;
import Zeze.Util.ZezeCounter;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Checkpoint {
	static final @NotNull Logger logger = LogManager.getLogger(Checkpoint.class);

	final @NotNull Application zeze;
	private final @NotNull CheckpointMode mode;
	private final @NotNull Thread checkpointThread;
	private final ArrayList<Database> databases = new ArrayList<>();
	//private final ReentrantReadWriteLock flushReadWriteLock = new ReentrantReadWriteLock();
	private final FastLock lock = new FastLock();
	private final Condition cond = lock.newCondition();
	private int period;
	private volatile boolean isRunning;
	//private ArrayList<Runnable> actionCurrent;
	//private volatile @NotNull ArrayList<Runnable> actionPending = new ArrayList<>();
	final ConcurrentHashSet<RelativeRecordSet> relativeRecordSetMap = new ConcurrentHashSet<>();

	// R3-X①：在飞flush计数（锁内inc/dec）——Application.stop在终检点后、LocalRocksCacheDb.close前
	// 有界等待它归零。FND7-54的停机拒绝只拦"新提交"，不等待已过门的在飞flush（Immediately模式
	// 业务线程的checkpoint.flush、Reduce降级flush、checkpointRun的runOnce）：它们已打开
	// LocalRocksCacheDb事务，与close/deleteDirectory并发是native UAF类（对齐ad5801593的教训）。
	// monitor只在计数增减与等待处短暂持有，flush体内不持有——与rrs锁、Application锁无嵌套。
	private final @NotNull Object activeFlushMonitor = new Object();
	private int activeFlush; // guarded by activeFlushMonitor

	public Checkpoint(@NotNull Application zeze, @NotNull CheckpointMode mode, int serverId) {
		this(zeze, mode, null, serverId);
	}

	public Checkpoint(@NotNull Application zeze, @NotNull CheckpointMode mode, @Nullable Iterable<Database> dbs,
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

	public @NotNull Application getZeze() {
		return zeze;
	}

	public @NotNull CheckpointMode getCheckpointMode() {
		return mode;
	}

	/*
	public void enterFlushReadLock() {
		if (mode == CheckpointMode.Period)
			flushReadWriteLock.readLock().lock();
	}

	public void exitFlushReadLock() {
		if (mode == CheckpointMode.Period)
			flushReadWriteLock.readLock().unlock();
	}
	*/

	public @NotNull Checkpoint add(@NotNull Iterable<Database> databases) {
		for (var db : databases) {
			if (!this.databases.contains(db))
				this.databases.add(db);
		}
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
	 * R3-X①（FND7-56边界收窄）：有界忽略中断地join检查点线程——stopAndJoin的join被中断
	 * （forceThrow）时线程仍存活（可能正要进入final flush），调用方继续关库会与它并发
	 * （ad5801593的close与数据通路并发native UAF类）。中断只恢复标志，join持续到deadline。
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
					//noinspection ResultOfMethodCallIgnored
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
	 * R3-X①：有界等待在飞flush归零（{@link #flush(Iterable, Set, History)}入口inc、finally dec）。
	 * 停机序列在终检点后、LocalRocksCacheDb.close前调用，超时返回false由调用方告警继续。
	 * 中断只恢复标志不提前返回：等待本身已有deadline兜底。
	 */
	public boolean waitNoActiveFlush(long timeoutMillis) {
		var deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
		boolean interrupted = false;
		try {
			synchronized (activeFlushMonitor) {
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

//		case Period:
//			final TaskCompletionSource<Integer> source = new TaskCompletionSource<>();
//			addActionAndPulse(() -> source.setResult(0));
//			source.await();
//			break;

		case Table:
			RelativeRecordSet.flushWhenCheckpoint(this);
			break;
		}
		if (timeBegin != 0)
			ZezeCounter.instance.addTaskRunTime("Checkpoint.runOnce", System.nanoTime() - timeBegin);
	}

	private void run() {
		while (isRunning) {
			try {
				// 先等后刷：首轮同样受period保护。start()启动的本线程在负载下可能被延迟调度，
				// 若立即执行首轮flush，会在任意时刻"补跑"一轮（TestFlushUnitIsolation.walkMemory
				// 曾因此约50%失败：被延迟的首轮把已提交未断言的内存值提前刷进镜像，锁忙回退镜像
				// 读到新值）。启动时relativeRecordSetMap通常为空，延迟首轮无实际影响。
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
//				case Period:
//					checkpointPeriod();
//					for (var action : actionCurrent)
//						action.run();
//					lock.lock();
//					try {
//						if (!actionPending.isEmpty())
//							continue; // 如果有未决的任务，马上开始下一次 DoCheckpoint。
//					} finally {
//						lock.unlock();
//					}
//					break;

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
//		case Period:
//			checkpointPeriod();
//			break;

		case Table:
			// FND7-54：终检点补轮——停机拒绝生效前已过检查的在途提交可能在终检点轮次进行中
			// 或之后才注册rrs（_lock_等锁醒来），flush后map仍非空时补轮收敛迟到的脏集；
			// 必须有界：flush失败的单元保留在map中，不设界会无限重试。
			for (int round = 0; round < 3; ++round) {
				RelativeRecordSet.flushWhenCheckpoint(this);
				if (relativeRecordSetMap.isEmpty())
					break;
			}
			break;
		}
		logger.info("final checkpoint end.");
	}

	/**
	 * 增加 checkpoint 完成一次以后执行的动作，每次 FlushReadWriteLock.EnterWriteLock()
	 * 之前的动作在本次checkpoint完成时执行，之后的动作在下一次DoCheckpoint后执行。
	 */
//	public void addActionAndPulse(@NotNull Runnable action) {
//		final var r = flushReadWriteLock.readLock();
//		r.lock();
//		try {
//			lock.lock();
//			try {
//				actionPending.add(action);
//				cond.signal();
//			} finally {
//				lock.unlock();
//			}
//		} finally {
//			r.unlock();
//		}
//	}

	/*
	private void checkpointPeriod() {
		logger.info("CheckpointPeriod({}) begin", zeze.getConfig().getServerId());
		long time0 = System.nanoTime();
		// encodeN
		for (var db : databases)
			db.encodeN();
		long time1 = System.nanoTime();
		// snapshot
		final var w = flushReadWriteLock.writeLock();
		w.lock();
		try {
			actionCurrent = actionPending;
			actionPending = new ArrayList<>();
			for (var db : databases)
				db.snapshot();
		} finally {
			w.unlock();
		}
		long time2 = System.nanoTime(), time3 = time2, time4 = time2;
		// flush
		var dts = new HashMap<Database, Database.Transaction>();
		Database.Transaction localCacheTransaction = zeze.getLocalRocksCacheDb().beginTransaction();
		try {
			for (var db : databases)
				dts.computeIfAbsent(db, Database::beginTransaction);
			for (var db : databases)
				db.flush(dts.get(db), dts, localCacheTransaction);
			time3 = System.nanoTime();
			for (var v : dts.values())
				v.commit();
			localCacheTransaction.commit();
			time4 = System.nanoTime();
			// cleanup
			try {
				for (var db : databases)
					db.cleanup();
			} catch (Throwable e) { // halt
				logger.fatal("CheckpointPeriod Cleanup Exception", e);
				LogManager.shutdown();
				Runtime.getRuntime().halt(54321);
			}
		} catch (Throwable e) { // rethrow
			for (var t : dts.values()) {
				try {
					t.rollback();
				} catch (Throwable ex) { // logger.error
					logger.error("CheckpointPeriod Rollback Exception", ex);
				}
			}
			try {
				localCacheTransaction.rollback();
			} catch (Throwable ex) { // logger.error
				logger.error("CheckpointPeriod Rollback Exception", ex);
			}
			throw e;
		} finally {
			for (var t : dts.values()) {
				try {
					t.close();
				} catch (Throwable ex) { // logger.error
					logger.error("CheckpointPeriod close Exception transaction={}", t, ex);
				}
			}
			try {
				localCacheTransaction.close();
			} catch (Throwable ex) { // logger.error
				logger.error("CheckpointPeriod close Exception transaction={}", localCacheTransaction, ex);
			}
			logger.info("CheckpointPeriod({}) end ({}+{}+{}+{} = {} ms)", zeze.getConfig().getServerId(),
					(time1 - time0) / 1_000_000,
					(time2 - time1) / 1_000_000,
					(time3 - time2) / 1_000_000,
					(time4 - time3) / 1_000_000,
					(System.nanoTime() - time0) / 1_000_000);
		}
	}
	*/
	public void flush(@NotNull Transaction trans, @Nullable OnzProcedure onzProcedure, @Nullable History history) {
		var records = new ArrayList<Record>(trans.getAccessedRecords().size());
		for (var ar : trans.getAccessedRecords().values()) {
			if (ar.dirty)
				records.add(ar.atomicTupleRecord.record);
		}
		flush(records, onzProcedure != null ? Set.of(onzProcedure) : Set.of(), history);
	}

	private String extractTableNames(Iterable<Record> rs, History history) {
		var tables = new HashSet<String>();
		for (var r : rs)
			tables.add(r.getTable().getName());
		if (null != history)
			tables.add(zeze.getHistoryModule().getHistoryTable().getName());
		return tables.toString();
	}

	public void flush(@NotNull Iterable<Record> rs, @Nullable Set<OnzProcedure> onzProcedures,
					  @Nullable History history) {
		// R3-X①：在飞计数从首个数据库触碰（LocalRocksCacheDb.beginTransaction）前开始，
		// 覆盖整个落库过程——stop的waitNoActiveFlush据此等待后才能close/delete目录。
		synchronized (activeFlushMonitor) {
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

	private void flushInternal(@NotNull Iterable<Record> rs, @Nullable Set<OnzProcedure> onzProcedures,
							   @Nullable History history) {
		var dts = new IdentityHashMap<Database, Database.Transaction>();
		Database.Transaction localCacheTransaction = zeze.getLocalRocksCacheDb().beginTransaction();

		// history.logChanges原子的和数据一起flush。
		try {
			// prepare: 编码并且为每一个数据库创建一个数据库事务。
			for (var r : rs) {
				var storage = r.getTable().getStorage();
				if (storage != null) {
					var database = storage.getDatabaseTable().getDatabase();
					r.setDatabaseTransactionTmp(dts.computeIfAbsent(database, Database::beginTransaction));
					if (r.getTable().getOldTable() != null) {
						database = r.getTable().getOldTable().getDatabase();
						r.setDatabaseTransactionOldTmp(dts.computeIfAbsent(database, Database::beginTransaction));
					}
				}
			}
			var historyTable = zeze.getHistoryModule().getHistoryTable();
			var historyTransaction = history != null
					? dts.computeIfAbsent(historyTable.getDatabase(), Database::beginTransaction)
					: null;

			// 编码
			if (history != null)
				history.encode0();

			for (var r : rs)
				r.encode0();
			OnzProcedure.sendFlushAndWait(onzProcedures);
			// 保存到数据库中
			for (var r : rs) {
				var t = r.getDatabaseTransactionTmp();
				r.flush(t, localCacheTransaction);
			}
			if (history != null) {
				var storage = ((Table)historyTable).getStorage();
				//noinspection DataFlowIssue
				history.writeOnly(storage.getDatabaseTable(), historyTransaction);
			}
			// 提交。
			for (var t : dts.values())
				t.commit();
			localCacheTransaction.commit();
			if (history != null)
				history.commitDone(); // tHistory 行已持久化才清容器；失败回滚后保留，重试幂等重写（FND3-51）
			try {
				// 清除编码状态
				for (var r : rs)
					r.cleanup();
			} catch (Throwable e) { // halt
				logger.fatal("Flush Cleanup Exception", e);
				LogManager.shutdown();
				Runtime.getRuntime().halt(54321);
			}
			// 保存成功，清除脏标记。
			// Immediately 模式的记录不进入 RelativeRecordSet，这里是它唯一的清除点。
			// Table 模式现在也在这里清除。
			for (var r : rs)
				r.setDirty(false);
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
			throw new RuntimeException(extractTableNames(rs, history), e);
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
		}
	}

	// under lock(rs)
	public void flush(@NotNull RelativeRecordSet rs) {
		// rs.MergeTo == null &&  check outside
		if (rs.getRecordSet() != null)
			flush(rs.getRecordSet(), rs.getOnzProcedures(), rs.getHistory()); // 脏标记在 flush(Iterable) 保存成功后统一清除。
	}
}
