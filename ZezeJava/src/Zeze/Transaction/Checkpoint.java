package Zeze.Transaction;

import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Checkpoint {
	private static final Logger logger = LogManager.getLogger(Checkpoint.class);

	private HashSet<Database> Databases = new HashSet<Database> ();
	private HashSet<Database> getDatabases() {
		return Databases;
	}

	private ReentrantReadWriteLock FlushReadWriteLock = new ReentrantReadWriteLock();

	private boolean IsRunning;
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
	private Task RunningTask = null;

	private CheckpointMode Mode = CheckpointMode.Period;
	public CheckpointMode getCheckpointMode() {
		return Mode;
	}

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

	public void WaitRun() {
		// 严格来说，这里应该是等待一次正在进行的checkpoint，如果没有在执行中应该不启动新的checkpoint。
		// 但是由于时间窗口的原因，可能开始执行waitrun时，checkpoint还没开始，没办法进行等待。
		// 先使用RunOnce。
		this.RunOnce();
	}

	public void Start(int period) {
		synchronized (this) {
			if (isRunning()) {
				return;
			}

			setRunning(true);
			setPeriod(period);
			RunningTask = Task.Run(() -> Run(), "Checkpoint.Run");
		}
	}

	public void StopAndJoin() {
		synchronized (this) {
			setRunning(false);
			this.notify();
		}
		if (RunningTask != null) {
			try {
				RunningTask.get();
			} catch (InterruptedException | ExecutionException e) {
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
				try {
					source.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
				break;

			case Table:
				RelativeRecordSet.FlushWhenCheckpoint(this);
				break;
		}
	}

	private void Run() {
		while (isRunning()) {
			switch (Mode) {
				case Period:
					CheckpointPeriod();
					for (tangible.Action0Param action : actionCurrent) {
						action.invoke();
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
				try {
					this.wait(Period);
				} catch (InterruptedException skip) {
				}
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

	private ArrayList<tangible.Action0Param> actionCurrent;
	private ArrayList<tangible.Action0Param> actionPending = new ArrayList<tangible.Action0Param>();

	/** 
	 增加 checkpoint 完成一次以后执行的动作，每次 FlushReadWriteLock.EnterWriteLock()
	 之前的动作在本次checkpoint完成时执行，之后的动作在下一次DoCheckpoint后执行。
	 
	 @param act
	*/
	public void AddActionAndPulse(tangible.Action0Param act) {
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
				actionPending = new ArrayList<tangible.Action0Param>();
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
		for (var db : getDatabases()) {
			db.Cleanup();
		}
	}

	public void Flush(Transaction trans) {
		Flush(trans.getAccessedRecords().values().stream().filter((r) -> r.Dirty).map((r) -> r.OriginRecord).toList());
	}

	public void Flush(java.lang.Iterable<Record> rs) {
		var dts = new HashMap<Database, Database.Transaction>();
		// prepare: 编码并且为每一个数据库创建一个数据库事务。
		for (var r : rs) {
			var database = r.getTable().getStorage().getDatabaseTable().getDatabase();
			var t = dts.get(database);
			if (null == t) {
				t = database.BeginTransaction();
				dts.put(database, t);
			}
			r.setDatabaseTransactionTmp(t);
		}
		try {
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
			// 清除编码状态
			for (var r : rs) {
				r.Cleanup();
			}
		}
		catch (RuntimeException e) {
			for (var t : dts.values()) {
				t.Rollback();
			}
			throw e;
		}
		finally {
			for (var t : dts.values()) {
				try {
					t.close();
				} catch (IOException e) {
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