package Zeze.Transaction;

import java.util.ArrayList;
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

	private final ArrayList<Database> Databases = new ArrayList<>();
	private final ReentrantReadWriteLock FlushReadWriteLock = new ReentrantReadWriteLock();
	private final CheckpointMode Mode;
	private final Thread CheckpointThread;
	private final Zeze.Application Zeze;
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition cond = lock.newCondition();
	private int Period;
	private volatile boolean IsRunning;
	private ArrayList<Runnable> actionCurrent;
	private volatile ArrayList<Runnable> actionPending = new ArrayList<>();
	final ExecutorService FlushThreadPool;
	final ConcurrentHashSet<RelativeRecordSet> RelativeRecordSetMap = new ConcurrentHashSet<>();

	public Checkpoint(Zeze.Application zeze, CheckpointMode mode, int serverId) {
		this(zeze, mode, null, serverId);
	}

	public Checkpoint(Zeze.Application zeze, CheckpointMode mode, Iterable<Database> dbs, int serverId) {
		Zeze = zeze;
		var concurrent = Zeze.getConfig().getCheckpointModeTableFlushConcurrent();
		FlushThreadPool = concurrent > 1 ? Executors.newFixedThreadPool(concurrent) : null;

		Mode = mode;
		if (dbs != null)
			Add(dbs);
		CheckpointThread = new Thread(() -> Task.Call(this::Run, "Checkpoint.Run"), "Checkpoint-" + serverId);
		CheckpointThread.setDaemon(true);
		CheckpointThread.setPriority(Thread.NORM_PRIORITY + 2);
		CheckpointThread.setUncaughtExceptionHandler((__, e) -> logger.error("fatal exception", e));
	}

	public CheckpointMode getCheckpointMode() {
		return Mode;
	}

	public Application getZeze() {
		return Zeze;
	}

	public void EnterFlushReadLock() {
		if (Mode == CheckpointMode.Period) {
			FlushReadWriteLock.readLock().lock();
		}
	}

	public void ExitFlushReadLock() {
		if (Mode == CheckpointMode.Period) {
			FlushReadWriteLock.readLock().unlock();
		}
	}

	public Checkpoint Add(Iterable<Database> databases) {
		for (var db : databases) {
			if (!Databases.contains(db))
				Databases.add(db);
		}
		return this;
	}

	public void Start(int period) {
		lock.lock();
		try {
			if (IsRunning)
				return;

			IsRunning = true;
			Period = period;
			CheckpointThread.start();
		} finally {
			lock.unlock();
		}
	}

	public void StopAndJoin() {
		lock.lock();
		try {
			IsRunning = false;
			cond.signal();
		} finally {
			lock.unlock();
		}
		if (null != CheckpointThread) {
			try {
				CheckpointThread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void RunOnce() {
		switch (getCheckpointMode()) {
		case Immediately:
			break;

		case Period:
			final TaskCompletionSource<Integer> source = new TaskCompletionSource<>();
			AddActionAndPulse(() -> source.SetResult(0));
			source.await();
			break;

		case Table:
			RelativeRecordSet.FlushWhenCheckpoint(this);
			break;
		}
	}

	private void Run() {
		while (IsRunning) {
			try {
				switch (Mode) {
				case Period:
					CheckpointPeriod();
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
					RelativeRecordSet.FlushWhenCheckpoint(this);
					break;

				default:
					break;
				}
				lock.lock();
				try {
					//noinspection ResultOfMethodCallIgnored
					cond.await(Period, TimeUnit.MILLISECONDS);
				} finally {
					lock.unlock();
				}
			} catch (Throwable ex) {
				logger.error("Run Exception", ex);
			}
		}
		logger.info("final checkpoint start.");
		switch (Mode) {
		case Period:
			CheckpointPeriod();
			break;

		case Table:
			RelativeRecordSet.FlushWhenCheckpoint(this);
			break;
		}
		if (null != FlushThreadPool) {
			FlushThreadPool.shutdown();
			while (true) {
				try {
					if (FlushThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS))
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
	public void AddActionAndPulse(Runnable action) {
		final var r = FlushReadWriteLock.readLock();
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

	private void CheckpointPeriod() {
		logger.info("CheckpointPeriod({}) begin", Zeze.getConfig().getServerId());
		long time0 = System.nanoTime();
		// encodeN
		for (var db : Databases)
			db.EncodeN();
		long time1 = System.nanoTime();
		// snapshot
		final var w = FlushReadWriteLock.writeLock();
		w.lock();
		try {
			actionCurrent = actionPending;
			actionPending = new ArrayList<>();
			for (var db : Databases)
				db.Snapshot();
		} finally {
			w.unlock();
		}
		long time2 = System.nanoTime(), time3 = time2, time4 = time2;
		// flush
		var n = Databases.size();
		var dts = new Database.Transaction[n];
		Database.Transaction localCacheTransaction = Zeze.getLocalRocksCacheDb().BeginTransaction();
		try {
			for (int i = 0; i < n; i++)
				dts[i] = Databases.get(i).BeginTransaction();
			for (int i = 0; i < n; i++)
				Databases.get(i).Flush(dts[i], localCacheTransaction);
			time3 = System.nanoTime();
			for (var v : dts)
				v.Commit();
			if (localCacheTransaction != null)
				localCacheTransaction.Commit();
			time4 = System.nanoTime();
			// cleanup
			try {
				for (var db : Databases)
					db.Cleanup();
			} catch (Throwable e) {
				logger.fatal("CheckpointPeriod Cleanup Exception", e);
				LogManager.shutdown();
				Runtime.getRuntime().halt(54321);
			}
		} catch (Throwable e) {
			for (var t : dts) {
				try {
					t.Rollback();
				} catch (Throwable ex) {
					logger.error("CheckpointPeriod Rollback Exception", ex);
				}
			}
			if (localCacheTransaction != null) {
				try {
					localCacheTransaction.Rollback();
				} catch (Throwable ex) {
					logger.error("CheckpointPeriod Rollback Exception", ex);
				}
			}
			throw e;
		} finally {
			for (var t : dts) {
				try {
					t.close();
				} catch (Throwable ex) {
					logger.error("CheckpointPeriod close Exception transaction={}", t, ex);
				}
			}
			if (localCacheTransaction != null) {
				try {
					localCacheTransaction.close();
				} catch (Throwable ex) {
					logger.error("CheckpointPeriod close Exception transaction={}", localCacheTransaction, ex);
				}
			}
			logger.info("CheckpointPeriod({}) end ({}+{}+{}+{} = {} ms)", Zeze.getConfig().getServerId(),
					(time1 - time0) / 1_000_000,
					(time2 - time1) / 1_000_000,
					(time3 - time2) / 1_000_000,
					(time4 - time3) / 1_000_000,
					(System.nanoTime() - time0) / 1_000_000);
		}
	}

	public void Flush(Transaction trans) {
		var records = new ArrayList<Record>(trans.getAccessedRecords().size());
		for (var ar : trans.getAccessedRecords().values()) {
			if (ar.Dirty)
				records.add(ar.AtomicTupleRecord.Record);
		}
		Flush(records);
	}

	public void Flush(Iterable<Record> rs) {
		var dts = new IdentityHashMap<Database, Database.Transaction>();
		Database.Transaction localCacheTransaction = Zeze.getLocalRocksCacheDb().BeginTransaction();

		try {
			// prepare: 编码并且为每一个数据库创建一个数据库事务。
			for (var r : rs) {
				if (r.getTable().GetStorage() != null) {
					var database = r.getTable().GetStorage().getDatabaseTable().getDatabase();
					r.setDatabaseTransactionTmp(dts.computeIfAbsent(database, Database::BeginTransaction));
				}
			}
			// 编码
			for (var r : rs) {
				r.Encode0();
			}
			// 保存到数据库中
			for (var r : rs) {
				r.Flush(r.getDatabaseTransactionTmp(), localCacheTransaction);
			}
			// 提交。
			for (var t : dts.values()) {
				t.Commit();
			}
			if (null != localCacheTransaction)
				localCacheTransaction.Commit();
			try {
				// 清除编码状态
				for (var r : rs) {
					r.Cleanup();
				}
			} catch (Throwable e) {
				logger.fatal("Flush Cleanup Exception", e);
				LogManager.shutdown();
				Runtime.getRuntime().halt(54321);
			}
		} catch (Throwable e) {
			for (var t : dts.values()) {
				try {
					t.Rollback();
				} catch (Throwable ex) {
					logger.error("Flush Rollback Exception", ex);
				}
			}
			if (null != localCacheTransaction) {
				try {
					localCacheTransaction.Rollback();
				} catch (Throwable ex) {
					logger.error("Flush Rollback Exception", ex);
				}
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
			if (null != localCacheTransaction) {
				try {
					localCacheTransaction.close();
				} catch (Throwable e) {
					logger.error("Flush close Exception transaction={}", localCacheTransaction, e);
				}
			}
		}
	}

	// under lock(rs)
	public void Flush(RelativeRecordSet rs) {
		// rs.MergeTo == null &&  check outside
		if (rs.getRecordSet() != null) {
			Flush(rs.getRecordSet());
			for (var r : rs.getRecordSet()) {
				r.setDirty(false);
			}
		}
	}
}
