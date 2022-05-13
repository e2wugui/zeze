package Zeze.Transaction;

import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Checkpoint {
	private static final Logger logger = LogManager.getLogger(Checkpoint.class);

	private final HashSet<Database> Databases = new HashSet<> ();
	private HashSet<Database> getDatabases() {
		return Databases;
	}

	private final ReentrantReadWriteLock FlushReadWriteLock = new ReentrantReadWriteLock();

	private volatile boolean IsRunning;
	public boolean isRunning() {
		return IsRunning;
	}
	private void setRunning(boolean value) {
		IsRunning = value;
	}
	private int Period;
	public int getPeriod() {
		return Period;
	}
	private void setPeriod(int value) {
		Period = value;
	}

	private final CheckpointMode Mode;
	public CheckpointMode getCheckpointMode() {
		return Mode;
	}

	private final Thread CheckpointThread;
	private Zeze.Application Zeze;

	public Zeze.Application getZeze() {
		return Zeze;
	}
	public Checkpoint(Zeze.Application zeze, CheckpointMode mode, int serverId) {
		this(zeze, mode, null, serverId);
	}

	public Checkpoint(Zeze.Application zeze, CheckpointMode mode, Iterable<Database> dbs, int serverId) {
		Zeze = zeze;
		Mode = mode;
		if (dbs != null)
			Add(dbs);
		CheckpointThread = new Thread(
				() -> Task.Call(this::Run, "Checkpoint.Run"),
				"CheckpointThread-" + serverId);
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
			this.getDatabases().add(db);
		}
		return this;
	}

	public synchronized void Start(int period) {
		if (isRunning()) {
			return;
		}

		setRunning(true);
		setPeriod(period);
		CheckpointThread.start();
	}

	public void StopAndJoin() {
		synchronized (this) {
			setRunning(false);
			this.notify();
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
		while (isRunning()) {
			try {
				switch (Mode) {
					case Period:
						CheckpointPeriod();
						for (var action : actionCurrent) {
							action.run();
						}
						synchronized (this) {
							if (!actionPending.isEmpty()) {
								continue; // 如果有未决的任务，马上开始下一次 DoCheckpoint。
							}
						}
						break;

					case Table:
							RelativeRecordSet.FlushWhenCheckpoint(this);
						break;

					default:
						break;
				}
				synchronized (this) {
					this.wait(Period);
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

			default:
				break;
		}
		logger.info("final checkpoint end.");
	}

	private ArrayList<Runnable> actionCurrent;
	private volatile ArrayList<Runnable> actionPending = new ArrayList<>();

	/**
	 增加 checkpoint 完成一次以后执行的动作，每次 FlushReadWriteLock.EnterWriteLock()
	 之前的动作在本次checkpoint完成时执行，之后的动作在下一次DoCheckpoint后执行。

	 @param act
	 action
	*/
	public void AddActionAndPulse(Runnable act) {
		final var r = FlushReadWriteLock.readLock();
		r.lock();
		try {
			synchronized (this) {
				actionPending.add(act);
				notify();
			}
		}
		finally {
			r.unlock();
		}
	}

	private void CheckpointPeriod() {
		// encodeN
		for (var db : getDatabases()) {
			db.EncodeN();
		}
		{
		// snapshot
			final var w = FlushReadWriteLock.writeLock();
			w.lock();
			try {
				actionCurrent = actionPending;
				actionPending = new ArrayList<>();
				for (var db : getDatabases()) {
					db.Snapshot();
				}
			}
			finally {
				w.unlock();
			}
		}
		// flush
		var dts = new HashMap<Database, Database.Transaction>();
		Database.Transaction localCacheTransaction = Zeze.getLocalRocksCacheDb().BeginTransaction();
		try {
			for (var db : getDatabases()) {
				dts.put(db, db.BeginTransaction());
			}
			for (var e : dts.entrySet()) {
				e.getKey().Flush(e.getValue(), localCacheTransaction);
			}
			for (var e : dts.entrySet()) {
				e.getValue().Commit();
			}
			if (null != localCacheTransaction)
				localCacheTransaction.Commit();
			// cleanup
			try {
				for (var db : getDatabases()) {
					db.Cleanup();
				}
			} catch (Throwable e) {
				logger.error("CheckpointPeriod Cleanup Exception", e);
				Runtime.getRuntime().halt(54321);
			}
		} catch (Throwable e) {
			for (var t : dts.values()) {
				try {
					t.Rollback();
				} catch (Throwable ex) {
					logger.error("CheckpointPeriod Rollback Exception", ex);
				}
			}
			if (null != localCacheTransaction) {
				try {
					localCacheTransaction.Rollback();
				} catch (Throwable ex) {
					logger.error("CheckpointPeriod Rollback Exception", ex);
				}
			}
			throw e;
		} finally {
			for (var t : dts.values()) {
				try {
					t.close();
				} catch (Throwable ex) {
					logger.error("CheckpointPeriod close Exception transaction=" + t, ex);
				}
			}
			if (null != localCacheTransaction) {
				try {
					localCacheTransaction.close();
				} catch (Throwable ex) {
					logger.error("CheckpointPeriod close Exception transaction=" + localCacheTransaction, ex);
				}
			}
		}
	}

	public void Flush(Transaction trans) {
		var result = trans.getAccessedRecords().values()
				.stream().filter((r) -> r.Dirty).map((r) -> r.Origin)
				.collect(Collectors.toList());
		Flush(result);
	}

	public void Flush(Iterable<Record> rs) {
		var dts = new HashMap<Database, Database.Transaction>();
		Database.Transaction localCacheTransaction = Zeze.getLocalRocksCacheDb().BeginTransaction();

		try {
			// prepare: 编码并且为每一个数据库创建一个数据库事务。
			for (var r : rs) {
				if (r.getTable().GetStorage() != null) {
					var database = r.getTable().GetStorage().getDatabaseTable().getDatabase();
					var t = dts.get(database);
					if (null == t) {
						t = database.BeginTransaction();
						dts.put(database, t);
					}
					r.setDatabaseTransactionTmp(t);
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
				logger.error("Flush Cleanup Exception", e);
				Runtime.getRuntime().halt(54321);
			}
		}
		catch (Throwable e) {
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
		}
		finally {
			for (var t : dts.values()) {
				try {
					t.close();
				} catch (Throwable e) {
					logger.error("Flush close Exception transaction=" + t, e);
				}
			}
			if (null != localCacheTransaction) {
				try {
					localCacheTransaction.close();
				} catch (Throwable e) {
					logger.error("Flush close Exception transaction=" + localCacheTransaction, e);
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
