package Zeze.Transaction;

import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Checkpoint {
	private static final Logger logger = LogManager.getLogger(Checkpoint.class);

	private final HashSet<Database> Databases = new HashSet<Database> ();
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

	private CheckpointMode Mode = CheckpointMode.Period;
	public CheckpointMode getCheckpointMode() {
		return Mode;
	}

	private Thread CheckpointThread;

	public Checkpoint(CheckpointMode mode) {
		Mode = mode;
	}

	public Checkpoint(CheckpointMode mode, java.lang.Iterable<Database> dbs) {
		Mode = mode;
		Add(dbs);
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

	public Checkpoint Add(java.lang.Iterable<Database> databases) {
		for (var db : databases) {
			this.getDatabases().add(db);
		}
		return this;
	}

	public void Start(int period) {
		synchronized (this) {
			if (isRunning()) {
				return;
			}

			setRunning(true);
			setPeriod(period);
			CheckpointThread = new Thread(
					() -> Task.Call(() -> Run(), "Checkpoint.Run"),
					"ChectpointThread");
			CheckpointThread.start();
		}
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
				final TaskCompletionSource<Integer> source = new TaskCompletionSource<Integer>();
				AddActionAndPulse(() -> source.SetResult(0));
				source.Wait();
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
				logger.error(ex);
			}
		}
		//logger.Fatal("final checkpoint start.");
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
		logger.fatal("final checkpoint end.");
	}

	private ArrayList<Runnable> actionCurrent;
	private volatile ArrayList<Runnable> actionPending = new ArrayList<>();

	/** 
	 增加 checkpoint 完成一次以后执行的动作，每次 FlushReadWriteLock.EnterWriteLock()
	 之前的动作在本次checkpoint完成时执行，之后的动作在下一次DoCheckpoint后执行。
	 
	 @param act
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
		try {
			for (var db : getDatabases()) {
				dts.put(db, db.BeginTransaction());
			}
			for (var e : dts.entrySet()) {
				e.getKey().Flush(e.getValue());
			}
			for (var e : dts.entrySet()) {
				e.getValue().Commit();
			}
			// cleanup
			try {
				for (var db : getDatabases()) {
					db.Cleanup();
				}
			} catch (Throwable fatal) {
				logger.error(fatal);
				Runtime.getRuntime().halt(54321);
			}
		} catch (Throwable e) {
			for (var t : dts.values()) {
				try {
					t.Rollback();
				} catch (Throwable ex) {
					logger.error(ex);
				}
			}
			throw e;
		} finally {
			for (var t : dts.values()) {
				try {
					t.close();
				} catch (Throwable ex) {
					logger.error(ex);
				}
			}
		}
	}

	public void Flush(Transaction trans) {
		var result = trans.getAccessedRecords().values()
				.stream().filter((r) -> r.Dirty).map((r) -> r.OriginRecord)
				.collect(Collectors.toList());
		Flush(result);
	}

	public void Flush(java.lang.Iterable<Record> rs) {
		var dts = new HashMap<Database, Database.Transaction>();
		try {
			// prepare: 编码并且为每一个数据库创建一个数据库事务。
			for (var r : rs) {
				var database = r.getTable().GetStorage().getDatabaseTable().getDatabase();
				var t = dts.get(database);
				if (null == t) {
					t = database.BeginTransaction();
					dts.put(database, t);
				}
				r.setDatabaseTransactionTmp(t);
			}
			// 编码
			for (var r : rs) {
				r.Encode0();
			}
			// 保存到数据库中
			for (var r : rs) {
				r.Flush(r.getDatabaseTransactionTmp());
			}
			// 提交。
			for (var t : dts.values()) {
				t.Commit();
			}
			try {
				// 清除编码状态
				for (var r : rs) {
					r.Cleanup();
				}
			} catch (Throwable fatal) {
				logger.error(fatal);
				Runtime.getRuntime().halt(54321);
			}
		}
		catch (Throwable e) {
			for (var t : dts.values()) {
				try {
					t.Rollback();
				} catch (Throwable ex) {
					logger.error(ex);
				}
			}
			throw e;
		}
		finally {
			for (var t : dts.values()) {
				try {
					t.close();
				} catch (Throwable e) {
					logger.error("Checkpoint.Flush: close transacton{}", t, e);
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