package Zeze.Transaction.Collections;

import Zeze.*;
import Zeze.Transaction.*;

public final class PList1<E> extends PList<E> {
	public PList1(long logKey, tangible.Func1Param<ImmutableList<E>, Log> logFactory) {
		super(logKey, logFactory);
	}

	@Override
	public E get(int index) {
		return getData()[index];
	}
	@Override
	public void set(int index, E value) {
		if (value == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : list;
			txn.PutLog(NewLog(oldv.SetItem(index, value)));
		}
		else {
			list = list.SetItem(index, value);
		}
	}

	@Override
	public void Add(E item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : list;
			txn.PutLog(NewLog(oldv.Add(item)));
		}
		else {
			list = list.Add(item);
		}
	}

	@Override
	public void AddRange(java.lang.Iterable<E> items) {
		// XXX
		for (var v : items) {
			if (null == v) {
				throw new NullPointerException();
			}
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : list;
			txn.PutLog(NewLog(oldv.AddRange(items)));
		}
		else {
			list = list.AddRange(items);
		}
	}

	@Override
	public void clear() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : list;
			if (!oldv.IsEmpty) {
				txn.PutLog(NewLog(ImmutableList<E>.Empty));
			}
		}
		else {
			list = ImmutableList<E>.Empty;
		}
	}


	@Override
	public void add(int index, E item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : list;
			txn.PutLog(NewLog(oldv.Insert(index, item)));
		}
		else {
			list = list.Insert(index, item);
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
			var oldv = tempVar ? log.Value : list;
			var newv = oldv.Remove(item);
			if (oldv != newv) {
				txn.PutLog(NewLog(newv));
				return true;
			}
			else {
				return false;
			}
		}
		else {
			var newv = list.Remove(item);
			if (newv != list) {
				list = newv;
				return true;
			}
			else {
				return false;
			}
		}
	}

	@Override
	public void remove(int index) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : list;
			txn.PutLog(NewLog(oldv.RemoveAt(index)));
		}
		else {
			list = list.RemoveAt(index);
		}
	}

	@Override
	public void RemoveRange(int index, int count) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			boolean tempVar = txn.GetLog(LogKey) instanceof LogV;
			LogV log = tempVar ? (LogV)txn.GetLog(LogKey) : null;
			var oldv = tempVar ? log.Value : list;
			txn.PutLog(NewLog(oldv.RemoveRange(index, count)));
		}
		else {
			list = list.RemoveRange(index, count);
		}
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {

	}
}