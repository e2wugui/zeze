package Zeze.Transaction.Collections;

import org.pcollections.Empty;
import org.pcollections.PVector;

import Zeze.Transaction.*;

public final class PList1<E> extends PList<E> {
	public PList1(long logKey, LogFactory<PVector<E>> logFactory) {
		super(logKey, logFactory);
	}

	@Override
	public void set(int index, E value) {
		if (value == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV)log).Value : list;
			txn.PutLog(NewLog(oldv.with(index, value)));
		}
		else {
			list = list.with(index, value);
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
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV)log).Value : list;
			txn.PutLog(NewLog(oldv.plus(item)));
		}
		else {
			list = list.plus(item);
		}
	}

	@Override
	public void AddRange(java.util.Collection<E> items) {
		// XXX
		for (var v : items) {
			if (null == v) {
				throw new NullPointerException();
			}
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV)log).Value : list;
			txn.PutLog(NewLog(oldv.plusAll(items)));
		}
		else {
			list = list.plusAll(items);
		}
	}

	@Override
	public void Clear() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV)log).Value : list;
			if (!oldv.isEmpty()) {
				txn.PutLog(NewLog(Empty.vector()));
			}
		}
		else {
			list = Empty.vector();
		}
	}


	@Override
	public void Insert(int index, E item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV)log).Value : list;
			txn.PutLog(NewLog(oldv.plus(index, item)));
		}
		else {
			list = list.plus(index, item);
		}
	}

	@Override
	public boolean Remove(Object item) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV)log).Value : list;
			var newv = oldv.minus(item);
			if (oldv != newv) {
				txn.PutLog(NewLog(newv));
				return true;
			}
			else {
				return false;
			}
		}
		else {
			var newv = list.minus(item);
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
	public void RemoveAt(int index) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV)log).Value : list;
			txn.PutLog(NewLog(oldv.minus(index)));
		}
		else {
			list = list.minus(index);
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {

	}
}