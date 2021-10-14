package Zeze.Transaction.Collections;

import org.pcollections.Empty;
import org.pcollections.PVector;

import Zeze.Transaction.*;

public final class PList1<E> extends PList<E> {
	public PList1(long logKey, LogFactory<PVector<E>> logFactory) {
		super(logKey, logFactory);
	}

	@Override
	public E set(int index, E value) {
		if (value == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV<E>)log).Value : list;
			var olde = oldv.get(index);
			txn.PutLog(NewLog(oldv.with(index, value)));
			return olde;
		}
		else {
			var olde = list.get(index);
			list = list.with(index, value);
			return olde;
		}
	}

	@Override
	public void add(E item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV<E>)log).Value : list;
			txn.PutLog(NewLog(oldv.plus(item)));
		}
		else {
			list = list.plus(item);
		}
	}

	@Override
	public void addAll(java.util.Collection<E> items) {
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
			var oldv = null != log ? ((LogV<E>)log).Value : list;
			txn.PutLog(NewLog(oldv.plusAll(items)));
		}
		else {
			list = list.plusAll(items);
		}
	}

	@Override
	public void clear() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV<E>)log).Value : list;
			if (!oldv.isEmpty()) {
				txn.PutLog(NewLog(Empty.vector()));
			}
		}
		else {
			list = Empty.vector();
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
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV<E>)log).Value : list;
			txn.PutLog(NewLog(oldv.plus(index, item)));
		}
		else {
			list = list.plus(index, item);
		}
	}

	@Override
	public boolean remove(Object item) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV<E>)log).Value : list;
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
	public void remove(int index) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV<E>)log).Value : list;
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