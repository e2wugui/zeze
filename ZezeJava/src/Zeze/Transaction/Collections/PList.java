package Zeze.Transaction.Collections;

import Zeze.Transaction.*;
import java.util.*;

import org.pcollections.Empty;
import org.pcollections.PVector;

public abstract class PList<E> extends PCollection implements Iterable<E> {
	private final LogFactory<PVector<E>> _logFactory;

	protected PVector<E> list;

	protected PList(long logKey, LogFactory<PVector<E>> logFactory) {
		super(logKey);
		this._logFactory = logFactory;
		list = Empty.vector();
	}

	public final Log NewLog(PVector<E> value) {
		return _logFactory.create(value);
	}

	public abstract static class LogV<E> extends Log {
		public PVector<E> Value;

		public LogV(Bean bean, PVector<E> last) {
			super(bean);
			this.Value = last;
		}

		public final void Commit(PList<E> variable) {
			variable.list = Value;
		}
	}

	@SuppressWarnings("unchecked")
	protected final PVector<E> getData() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			if (txn == null) {
				return list;
			}
			txn.VerifyRecordAccessed(this, true);
			var log = txn.GetLog(LogKey);
			if (null == log)
				return list;
			return ((LogV<E>)log).Value;
		}
		return list;
	}
	public final int size() {
		return getData().size();
	}

	@Override
	public String toString() {
		return String.format("PList%1$s", getData());
	}

	public E get(int index) {
		return getData().get(index);
	}

	public final boolean isReadOnly() {
		return false;
	}

	public abstract void add(E item);
	public abstract void addAll(java.util.Collection<E> items);
	public abstract void clear();
	public abstract void add(int index, E item);
	public abstract boolean remove(E item);
	public abstract void remove(int index);
	public abstract E set(int index, E value);

	public final boolean contains(Object item) {
		return getData().contains(item);
	}

	public final void CopyTo(E[] array, int arrayIndex) {
		var data = getData();
		for (var e : data)
			array[arrayIndex++] = e;
	}

	@Override
	public final Iterator<E> iterator() {
		return getData().iterator();
	}

	public final int indexOf(Object item) {
		return getData().indexOf(item);
	}
}