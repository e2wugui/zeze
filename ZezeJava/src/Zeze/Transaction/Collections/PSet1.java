package Zeze.Transaction.Collections;

import Zeze.*;
import Zeze.Transaction.*;

public final class PSet1<E> extends PSet<E> {
	public PSet1(long logKey, tangible.Func1Param<ImmutableHashSet<E>, Log> logFactory) {
		super(logKey, logFactory);
	}

	@Override
	public boolean Add(E item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : set;
			var newv = oldv.Add(item);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				((ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this))).LogAdd(item);
				return true;
			}
			else {
				return false;
			}
		}
		else {
			set = set.Add(item);
			return true;
		}
	}

	@Override
	public void clear() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : set;
			if (!oldv.IsEmpty) {
				txn.PutLog(NewLog(ImmutableHashSet<E>.Empty));
				ChangeNoteSet<E> note = (ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this));
				for (var item : oldv) {
					note.LogRemove(item);
				}
			}
		}
		else {
			set = ImmutableHashSet<E>.Empty;
		}
	}



	@Override
	public void ExceptWith(java.lang.Iterable<E> other) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : set;
			var newv = oldv.Except(other);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				ChangeNoteSet<E> note = (ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this));
				for (var item : other) {
					note.LogRemove(item);
				}
			}
		}
		else {
			set = set.Except(other);
		}
	}


	@Override
	public void IntersectWith(java.lang.Iterable<E> other) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : set;
			var newv = oldv.Intersect(other);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				ChangeNoteSet<E> note = (ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this));
				for (var old : oldv) {
					if (false == other.Contains(old)) {
						note.LogRemove(old);
					}
				}
			}
		}
		else {
			set = set.Intersect(other);
		}
	}

	@Override
	public boolean remove(Object objectValue) {
		E item = (E)objectValue;
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : set;
			var newv = oldv.Remove(item);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				((ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this))).LogRemove(item);
				return true;
			}
			else {
				return false;
			}
		}
		else {
			var old = set;
			set = set.Remove(item);
			return old != set;
		}
	}

	@Override
	public void SymmetricExceptWith(java.lang.Iterable<E> other) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : set;
			var newv = oldv.SymmetricExcept(other);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				ChangeNoteSet<E> note = (ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this));
				// this: 1,2 other: 2,3 result: 1,3
				for (var item : other) {
					if (oldv.Contains(item)) {
						note.LogRemove(item);
					}
					else {
						note.LogAdd(item);
					}
				}
			}
		}
		else {
			set = set.SymmetricExcept(other);
		}
	}

	@Override
	public void UnionWith(java.lang.Iterable<E> other) {
		for (E v : other) {
			if (null == v) {
				throw new NullPointerException();
			}
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : set;
			var newv = oldv.Union(other);
			if (newv != oldv) {
				txn.PutLog(NewLog(newv));
				ChangeNoteSet<E> note = (ChangeNoteSet<E>)txn.GetOrAddChangeNote(this.getObjectId(), () -> new ChangeNoteSet<E>(this));
				for (var item : other) {
					if (false == oldv.Contains(item)) {
						note.LogAdd(item);
					}
				}
			}
		}
		else {
			set = set.Union(other);
		}
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo tableKey) {
	}
}