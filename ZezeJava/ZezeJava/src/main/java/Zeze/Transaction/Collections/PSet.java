package Zeze.Transaction.Collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.pcollections.Empty;

public abstract class PSet<E> extends PCollection implements Set<E> {
	private final LogFactory<org.pcollections.PSet<E>> _logFactory;

	protected org.pcollections.PSet<E> set;

	protected PSet(long logKey, LogFactory<org.pcollections.PSet<E>> logFactory) {
		super(logKey);
		this._logFactory = logFactory;
		set = Empty.set();
	}

	public final Log NewLog(org.pcollections.PSet<E> value) {
		return _logFactory.create(value);
	}

	public abstract static class LogV<E> extends Log {
		public final org.pcollections.PSet<E> Value;

		protected LogV(Bean bean, org.pcollections.PSet<E> value) {
			super("bean");
			setBean(bean);
			Value = value;
		}

		protected final void Commit(PSet<E> variable) {
			variable.set = Value;
		}
	}

	protected final org.pcollections.PSet<E> getData() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			if (txn == null) {
				return set;
			}
			txn.VerifyRecordAccessed(this, true);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var olds = null != log ? ((LogV<E>)log).Value : set;
			return olds;
		}
		return set;
	}

	@Override
	public String toString() {
		return getData().stream().map(Object::toString).collect(Collectors.joining(",", "{", "}"));
	}

	@Override
	public final int size() {
		return getData().size();
	}

	@Override
	public final boolean isEmpty() {
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
	public boolean retainAll(Collection<?> c) {
		return false;
	}

	public final boolean isReadOnly() {
		return false;
	}

	@Override
	public final boolean contains(Object item) {
		return getData().contains(item);
	}

	public final void CopyTo(E[] array, int arrayIndex) {
		int index = arrayIndex;
		for (var e : getData()) {
			array[index++] = e;
		}
	}

	@Override
	public Iterator<E> iterator() {
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
				PSet.this.remove(next);
			}
		};
	}
}
