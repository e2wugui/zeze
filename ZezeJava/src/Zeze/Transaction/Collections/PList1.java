package Zeze.Transaction.Collections;

import org.pcollections.Empty;
import org.pcollections.PVector;

import Zeze.Transaction.*;

import java.util.Collection;

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
	public boolean add(E item) {
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
			return true;
		}
		else {
			list = list.plus(item);
			return true;
		}
	}

	@Override
	public boolean addAll(java.util.Collection<? extends E> items) {
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
			return true;
		}
		else {
			list = list.plusAll(items);
			return true;
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
	public boolean removeAll(Collection<?> c) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV<E>)log).Value : list;
			var newv = oldv.minusAll(c);
			if (oldv != newv) {
				txn.PutLog(NewLog(newv));
				return true;
			}
			else {
				return false;
			}
		}
		else {
			var oldlist = list;
			list = list.minusAll(c);
			return oldlist != list;
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
	public E remove(int index) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV<E>)log).Value : list;
			var exist = oldv.get(index);
			txn.PutLog(NewLog(oldv.minus(index)));
			return exist;
		}
		else {
			var exist = list.get(index);
			list = list.minus(index);
			return exist;
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {

	}
}