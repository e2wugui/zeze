package Zeze.Transaction.Collections;

import org.pcollections.Empty;
import org.pcollections.PVector;

import Zeze.Transaction.*;

import java.util.Collection;

public final class PList2<E extends Bean> extends PList<E> {
	public PList2(long logKey, LogFactory<PVector<E>> logFactory) {
		super(logKey, logFactory);
	}

	@SuppressWarnings("unchecked")
	@Override
	public E set(int index, E value) {
		if (value == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			value.InitRootInfo(RootInfo, getParent());
			value.setVariableId(this.getVariableId());
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			var oldV = null != log ? ((LogV<E>)log).Value : list;
			var oldE = oldV.get(index);
			txn.PutLog(NewLog(oldV.plus(index, value)));
			return oldE;
		}
		else {
			var oldE = list.get(index);
			list = list.plus(index, value);
			return oldE;
		}
	}

	@Override
	public boolean add(E item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			item.InitRootInfo(RootInfo, getParent());
			item.setVariableId(this.getVariableId());
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldV = null != log ? ((LogV<E>)log).Value : list;
			txn.PutLog(NewLog(oldV.plus(item)));
		}
		else {
			list = list.plus(item);
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> items) {
		if (this.isManaged()) {
			for (var item : items) {
				item.InitRootInfo(RootInfo, getParent());
				item.setVariableId(this.getVariableId());
			}
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldV = null != log ? ((LogV<E>)log).Value : list;
			txn.PutLog(NewLog(oldV.plusAll(items)));
		}
		else {
			list = list.plusAll(items);
		}
		return true;
	}

	@Override
	public void clear() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldV = null != log ? ((LogV<E>)log).Value : list;
			if (!oldV.isEmpty()) {
				txn.PutLog(NewLog(Empty.vector()));
			}
		}
		else {
			this.list = Empty.vector();
		}
	}

	@Override
	public void add(int index, E item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (this.isManaged()) {
			item.InitRootInfo(RootInfo, getParent());
			item.setVariableId(this.getVariableId());
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldV = null != log ? ((LogV<E>)log).Value : list;
			txn.PutLog(NewLog(oldV.plus(index, item)));
		}
		else {
			list = list.plus(index, item);
		}
	}

	@Override
	public boolean remove(Object item) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldV = null != log ? ((LogV<E>)log).Value : list;
			var newV = oldV.minus(item);
			if (oldV != newV) {
				txn.PutLog(NewLog(newV));
				return true;
			}
			return false;
		}
		else {
			var oldV = list;
			list = list.minus(item);
			return oldV != list;
		}
	}

	@Override
	public E remove(int index) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldV = null != log ? ((LogV<E>)log).Value : list;
			var exist = oldV.get(index);
			txn.PutLog(NewLog(oldV.minus(index)));
			return exist;
		}
		else {
			var exist = list.get(index);
			list = list.minus(index);
			return exist;
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldV = null != log ? ((LogV<E>)log).Value : list;
			var newV = oldV.minusAll(c);
			if (oldV != newV) {
				txn.PutLog(NewLog(newV));
				return true;
			}
			return false;
		}
		else {
			var oldV = list;
			list = list.minusAll(c);
			return oldV != list;
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo tableKey) {
		for (var e : list) {
			e.InitRootInfo(tableKey, getParent());
		}
	}
}
