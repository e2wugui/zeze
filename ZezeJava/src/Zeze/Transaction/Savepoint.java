package Zeze.Transaction;

import java.util.*;

public final class Savepoint {
	private HashMap<Long, Log> Logs = new HashMap<Long, Log> ();
	public HashMap<Long, Log> getLogs() {
		return Logs;
	}
	//private readonly Dictionary<long, Log> Newly = new Dictionary<long, Log>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。

	private HashMap<Long, ChangeNote> ChangeNotes = new HashMap<Long, ChangeNote> ();
	public HashMap<Long, ChangeNote> getChangeNotes() {
		return ChangeNotes;
	}

	/*
	public void PutChangeNote(long key, ChangeNote note)
	{
	    notes[key] = note;
	}
	*/

	public ChangeNote GetOrAddChangeNote(long key, Zeze.Util.Factory<ChangeNote> factory) {
		var exist = ChangeNotes.get(key);
		if (null != exist) {
			return exist;
		}
		ChangeNote newNote = factory.create();
		getChangeNotes().put(key, newNote);
		return newNote;
	}

	public void PutLog(Log log) {
		Logs.put(log.getLogKey(), log);
		//newly[log.LogKey] = log;
	}

	public Log GetLog(long logKey) {
		return Logs.get(logKey);
	}

	public Savepoint Duplicate() {
		Savepoint sp = new Savepoint();
		for (var e : Logs.entrySet()) {
			sp.Logs.put(e.getKey(), e.getValue());
		}
		return sp;
	}

	public void Merge(Savepoint other) {
		for (var e : other.Logs.entrySet()) {
			Logs.put(e.getKey(), e.getValue());
		}

		for (var e : other.ChangeNotes.entrySet()) {
			var cur = this.ChangeNotes.get(e.getKey());
			if (null != cur) {
				cur.Merge(e.getValue());
			}
			else {
				this.ChangeNotes.put(e.getKey(), e.getValue());
			}
		}
	}

	public void Commit() {
		for (var e : getLogs().entrySet()) {
			e.getValue().Commit();
		}
	}

	public void Rollback() {
		// 现在没有实现 Log.Rollback。不需要再做什么，保留接口，以后实现Rollback时再处理。
		/*
		foreach (var e in newly)
		{
		    e.Value.Rollback();
		}
		*/
	}
}