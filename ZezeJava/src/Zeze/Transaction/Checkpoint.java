package Zeze.Transaction;

import Zeze.*;
import java.util.*;

public final class Checkpoint {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private HashSet<Database> Databases = new HashSet<Database> ();
	private HashSet<Database> getDatabases() {
		return Databases;
	}

	private ReaderWriterLockSlim FlushReadWriteLock = new ReaderWriterLockSlim();
	private ReaderWriterLockSlim getFlushReadWriteLock() {
		return FlushReadWriteLock;
	}

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

	private CheckpointMode CheckpointMode = CheckpointMode.values()[0];
	public CheckpointMode getCheckpointMode() {
		return CheckpointMode;
	}

	public Checkpoint(CheckpointMode mode) {
		CheckpointMode = mode;
	}

	public Checkpoint(CheckpointMode mode, java.lang.Iterable<Database> dbs) {
		CheckpointMode = mode;
		Add(dbs);
	}

	public void EnterFlushReadLock() {
		if (getCheckpointMode() == CheckpointMode.Period) {
			getFlushReadWriteLock().EnterReadLock();
		}
	}

	public void ExitFlushReadLock() {
		if (getCheckpointMode() == CheckpointMode.Period) {
			getFlushReadWriteLock().ExitReadLock();
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
			RunningTask = Zeze.Util.Task.Run(::Run, "Checkpoint.Run");
		}
	}

	public void StopAndJoin() {
		synchronized (this) {
			setRunning(false);
			Monitor.Pulse(this);
		}
		if (RunningTask != null) {
			RunningTask.Wait();
		}
	}

	public void RunOnce() {
		switch (getCheckpointMode()) {
			case Immediately:
				break;

			case Period:
				TaskCompletionSource<Integer> source = new TaskCompletionSource<Integer>();
				AddActionAndPulse(() -> source.SetResult(0));
				source.Task.Wait();
				break;

			case Table:
				RelativeRecordSet.FlushWhenCheckpoint(this);
				break;
		}
	}

	private void Run() {
		while (isRunning()) {
			switch (getCheckpointMode()) {
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
			}
			synchronized (this) {
				Monitor.Wait(this, getPeriod());
			}
		}
		//logger.Fatal("final checkpoint start.");
		switch (getCheckpointMode()) {
			case Period:
				CheckpointPeriod();
				break;

			case Table:
				RelativeRecordSet.FlushWhenCheckpoint(this);
				break;
		}
		logger.Fatal("final checkpoint end.");
	}

	private ArrayList<tangible.Action0Param> actionCurrent;
	private ArrayList<tangible.Action0Param> actionPending = new ArrayList<tangible.Action0Param>();

	/** 
	 增加 checkpoint 完成一次以后执行的动作，每次 FlushReadWriteLock.EnterWriteLock()
	 之前的动作在本次checkpoint完成时执行，之后的动作在下一次DoCheckpoint后执行。
	 
	 @param act
	*/
	public void AddActionAndPulse(tangible.Action0Param act) {
		getFlushReadWriteLock().EnterReadLock();
		try {
			synchronized (this) {
				actionPending.add(act);
				Monitor.Pulse(this);
			}
		}
		finally {
			getFlushReadWriteLock().ExitReadLock();
		}
	}

	private void CheckpointPeriod() {
		// encodeN
		for (var db : getDatabases()) {
			db.EncodeN();
		}
		{
		// snapshot
			getFlushReadWriteLock().EnterWriteLock();
			try {
				actionCurrent = actionPending;
				actionPending = new ArrayList<tangible.Action0Param>();
				for (var db : getDatabases()) {
					db.Snapshot();
				}
			}
			finally {
				getFlushReadWriteLock().ExitWriteLock();
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
//C# TO JAVA CONVERTER TODO TASK: There is no Java equivalent to LINQ query syntax:
		Flush(from ra in trans.getAccessedRecords().values() where ra.Dirty select ra.OriginRecord);
	}

	public void Flush(java.lang.Iterable<Record> rs) {
		var dts = new HashMap<Database, Database.Transaction>();
		// prepare: 编码并且为每一个数据库创建一个数据库事务。
		for (var r : rs) {
			Database database = r.getTable().Storage.DatabaseTable.Database;
			TValue t;
			if (false == (dts.containsKey(database) && (t = dts.get(database)) == t)) {
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
				t.Dispose();
			}
		}
	}

	// under lock(rs)
	public void Flush(RelativeRecordSet rs) {
		// rs.MergeTo == null &&  check outside
		if (rs.getRecordSet() != null) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java equivalent to LINQ query syntax:
			Flush(from r in rs.getRecordSet() select r);
			for (var r : rs.getRecordSet()) {
				r.Dirty = false;
			}
		}
	}
}