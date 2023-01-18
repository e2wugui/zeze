package Zeze.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Application;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Checkpoint {
	private static final Logger logger = LogManager.getLogger(Checkpoint.class);

	private final ArrayList<Database> databases = new ArrayList<>();
	private final ReentrantReadWriteLock flushReadWriteLock = new ReentrantReadWriteLock();
	private final CheckpointMode mode;
	private final Thread checkpointThread;
	private final Zeze.Application zeze;
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition cond = lock.newCondition();
	private int period;
	private volatile boolean isRunning;
	private ArrayList<Runnable> actionCurrent;
	private volatile ArrayList<Runnable> actionPending = new ArrayList<>();
	final ExecutorService flushThreadPool;
	final ConcurrentHashSet<RelativeRecordSet> relativeRecordSetMap = new ConcurrentHashSet<>();

	public Checkpoint(Zeze.Application zeze, CheckpointMode mode, int serverId) {
		this(zeze, mode, null, serverId);
	}

	public Checkpoint(Zeze.Application zeze, CheckpointMode mode, Iterable<Database> dbs, int serverId) {
		this.zeze = zeze;
		var concurrent = this.zeze.getConfig().getCheckpointModeTableFlushConcurrent();
		flushThreadPool = concurrent > 1 ? Executors.newFixedThreadPool(concurrent) : null;

		this.mode = mode;
		if (dbs != null)
			add(dbs);
		checkpointThread = new Thread(() -> Task.call(this::run, "Checkpoint.Run"), "Checkpoint-" + serverId);
		checkpointThread.setDaemon(true);
		checkpointThread.setPriority(Thread.NORM_PRIORITY + 2);
		checkpointThread.setUncaughtExceptionHandler((__, e) -> logger.error("fatal exception", e));
	}

	public CheckpointMode getCheckpointMode() {
		return mode;
	}

	public Application getZeze() {
		return zeze;
	}

	public void enterFlushReadLock() {
		if (mode == CheckpointMode.Period) {
			flushReadWriteLock.readLock().lock();
		}
	}

	public void exitFlushReadLock() {
		if (mode == CheckpointMode.Period) {
			flushReadWriteLock.readLock().unlock();
		}
	}

	public Checkpoint add(Iterable<Database> databases) {
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
		if (null != checkpointThread) {
			try {
				checkpointThread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void runOnce() {
		switch (getCheckpointMode()) {
		case Immediately:
			break;

		case Period:
			final TaskCompletionSource<Integer> source = new TaskCompletionSource<>();
			addActionAndPulse(() -> source.setResult(0));
			source.await();
			break;

		case Table:
			RelativeRecordSet.flushWhenCheckpoint(this, null);
			break;
		}
	}

	private void run() {
		while (isRunning) {
			try {
				switch (mode) {
				case Period:
					checkpointPeriod();
					for (var action : actionCurrent) {
						action.run();
					}
					lock.lock();
					try {
						if (!actionPending.isEmpty()) {
							continue; // 如果有未决的任务，马上开始下一次 DoCheckpoint。
						}
					} finally {
						lock.unlock();
					}
					break;

				case Table:
					RelativeRecordSet.flushWhenCheckpoint(this, flushThreadPool);
					break;

				default:
					break;
				}
				lock.lock();
				try {
					//noinspection ResultOfMethodCallIgnored
					cond.await(period, TimeUnit.MILLISECONDS);
				} finally {
					lock.unlock();
				}
			} catch (Throwable ex) { // logger.error
				// thread worker.
				logger.error("Run Exception", ex);
			}
		}
		logger.info("final checkpoint start.");
		switch (mode) {
		case Period:
			checkpointPeriod();
			break;

		case Table:
			RelativeRecordSet.flushWhenCheckpoint(this, flushThreadPool);
			break;
		}
		if (null != flushThreadPool) {
			flushThreadPool.shutdown();
			while (true) {
				try {
					if (flushThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS))
						break;
				} catch (InterruptedException ex) {
					// skip
				}
			}
		}
		logger.info("final checkpoint end.");
	}

	/**
	 * 增加 checkpoint 完成一次以后执行的动作，每次 FlushReadWriteLock.EnterWriteLock()
	 * 之前的动作在本次checkpoint完成时执行，之后的动作在下一次DoCheckpoint后执行。
	 */
	public void addActionAndPulse(Runnable action) {
		final var r = flushReadWriteLock.readLock();
		r.lock();
		try {
			lock.lock();
			try {
				actionPending.add(action);
				cond.signal();
			} finally {
				lock.unlock();
			}
		} finally {
			r.unlock();
		}
	}

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
			if (localCacheTransaction != null)
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
			if (localCacheTransaction != null) {
				try {
					localCacheTransaction.rollback();
				} catch (Throwable ex) { // logger.error
					logger.error("CheckpointPeriod Rollback Exception", ex);
				}
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
			if (localCacheTransaction != null) {
				try {
					localCacheTransaction.close();
				} catch (Throwable ex) { // logger.error
					logger.error("CheckpointPeriod close Exception transaction={}", localCacheTransaction, ex);
				}
			}
			logger.info("CheckpointPeriod({}) end ({}+{}+{}+{} = {} ms)", zeze.getConfig().getServerId(),
					(time1 - time0) / 1_000_000,
					(time2 - time1) / 1_000_000,
					(time3 - time2) / 1_000_000,
					(time4 - time3) / 1_000_000,
					(System.nanoTime() - time0) / 1_000_000);
		}
	}

	public void flush(Transaction trans) {
		var records = new ArrayList<Record>(trans.getAccessedRecords().size());
		for (var ar : trans.getAccessedRecords().values()) {
			if (ar.dirty)
				records.add(ar.atomicTupleRecord.record);
		}
		flush(records);
	}

	public void flush(Iterable<Record> rs) {
		var dts = new IdentityHashMap<Database, Database.Transaction>();
		Database.Transaction localCacheTransaction = zeze.getLocalRocksCacheDb().beginTransaction();

		try {
			// prepare: 编码并且为每一个数据库创建一个数据库事务。
			for (var r : rs) {
				if (r.getTable().getStorage() != null) {
					var database = r.getTable().getStorage().getDatabaseTable().getDatabase();
					r.setDatabaseTransactionTmp(dts.computeIfAbsent(database, Database::beginTransaction));
					if (null != r.getTable().getOldTable()) {
						database = r.getTable().getOldTable().getDatabase();
						r.setDatabaseTransactionOldTmp(dts.computeIfAbsent(database, Database::beginTransaction));
					}
				}
			}
			// 编码
			for (var r : rs) {
				r.encode0();
			}
			// 保存到数据库中
			for (var r : rs) {
				r.flush(r.getDatabaseTransactionTmp(), localCacheTransaction);
			}
			// 提交。
			for (var t : dts.values()) {
				t.commit();
			}
			if (null != localCacheTransaction)
				localCacheTransaction.commit();
			try {
				// 清除编码状态
				for (var r : rs) {
					r.cleanup();
				}
			} catch (Throwable e) { // halt
				logger.fatal("Flush Cleanup Exception", e);
				LogManager.shutdown();
				Runtime.getRuntime().halt(54321);
			}
		} catch (Throwable e) { // rethrow
			for (var t : dts.values()) {
				try {
					t.rollback();
				} catch (Throwable ex) { // logger.error
					logger.error("Flush Rollback Exception", ex);
				}
			}
			if (null != localCacheTransaction) {
				try {
					localCacheTransaction.rollback();
				} catch (Throwable ex) { // logger.error
					logger.error("Flush Rollback Exception", ex);
				}
			}
			throw e;
		} finally {
			for (var t : dts.values()) {
				try {
					t.close();
				} catch (Throwable e) { // logger.error
					logger.error("Flush close Exception transaction={}", t, e);
				}
			}
			if (null != localCacheTransaction) {
				try {
					localCacheTransaction.close();
				} catch (Throwable e) { // logger.error
					logger.error("Flush close Exception transaction={}", localCacheTransaction, e);
				}
			}
		}
	}

	// under lock(rs)
	public void flush(RelativeRecordSet rs) {
		// rs.MergeTo == null &&  check outside
		if (rs.getRecordSet() != null) {
			flush(rs.getRecordSet());
			for (var r : rs.getRecordSet()) {
				r.setDirty(false);
			}
		}
	}
}
