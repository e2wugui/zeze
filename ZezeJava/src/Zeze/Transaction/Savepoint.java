package Zeze.Transaction;

import Zeze.*;
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

	public ChangeNote GetOrAddChangeNote(long key, tangible.Func0Param<ChangeNote> factory) {
		if (getChangeNotes().containsKey(key) && (var exist = getChangeNotes().get(key)) == var exist) {
			return exist;
		}
		ChangeNote newNote = factory.invoke();
		getChangeNotes().put(key, newNote);
		return newNote;
	}

	public void PutLog(Log log) {
		getLogs().put(log.LogKey, log);
		//newly[log.LogKey] = log;
	}

	public Log GetLog(long logKey) {
		return (getLogs().containsKey(logKey) && (var log = getLogs().get(logKey)) == var log) ? log : null;
	}

	public Savepoint Duplicate() {
		Savepoint sp = new Savepoint();
		for (var e : getLogs().entrySet()) {
			sp.getLogs().put(e.getKey(), e.getValue());
		}
		return sp;
	}

	public void Merge(Savepoint other) {
		for (var e : other.getLogs().entrySet()) {
			getLogs().put(e.getKey(), e.getValue());
		}

		for (var e : other.getChangeNotes().entrySet()) {
			if (this.getChangeNotes().containsKey(e.getKey()) && (var cur = this.getChangeNotes().get(e.getKey())) == var cur) {
				cur.Merge(e.getValue());
			}
			else {
				this.getChangeNotes().put(e.getKey(), e.getValue());
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