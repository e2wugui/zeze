package Zeze.Transaction.Collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.pcollections.Empty;
import org.pcollections.PVector;

public abstract class PList<E> extends PCollection implements List<E> {
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
		public final PVector<E> Value;

		public LogV(Bean bean, PVector<E> last) {
			super("bean");
			setBelong(bean);
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

	@Override
	public final int size() {
		return getData().size();
	}

	@Override
	public String toString() {
		return "PList" + getData();
	}

	@Override
	public E get(int index) {
		return getData().get(index);
	}

	public final boolean isReadOnly() {
		return false;
	}

	@Override
	public abstract boolean add(E item);

	@Override
	public abstract boolean addAll(java.util.Collection<? extends E> items);

	@Override
	public abstract void clear();

	@Override
	public abstract void add(int index, E item);

	@Override
	public abstract boolean remove(Object item);

	@Override
	public abstract E remove(int index);

	@Override
	public abstract boolean removeAll(Collection<?> c);

	@Override
	public abstract E set(int index, E value);

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
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
		return new Iterator<>() {
			private final Iterator<E> it = getData().iterator();
			private E next;

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public E next() {
				return next = it.next();
			}

			@Override
			public void remove() {
				PList.this.remove(next);
			}
		};
	}

	@Override
	public final int indexOf(Object item) {
		return getData().indexOf(item);
	}

	@Override
	public boolean isEmpty() {
		return getData().isEmpty();
	}

	@Override
	public Object[] toArray() {
		return getData().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		//noinspection SuspiciousToArrayCall
		return getData().toArray(a);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return getData().containsAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<E> listIterator() {
		//TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public int lastIndexOf(Object o) {
		return getData().lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}
}
