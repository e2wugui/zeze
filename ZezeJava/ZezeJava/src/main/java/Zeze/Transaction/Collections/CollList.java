package Zeze.Transaction.Collections;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.pcollections.PVector;

public abstract class CollList<V> extends Collection implements List<V> {
	public PVector<V> _list = org.pcollections.Empty.vector();

	@Override
	public abstract boolean add(V item);

	@Override
	public abstract boolean remove(Object item);

	@Override
	public abstract void clear();

	@Override
	public abstract V set(int index, V item);

	@Override
	public abstract void add(int index, V item);

	@Override
	public abstract V remove(int index);

	@Override
	public abstract boolean addAll(java.util.Collection<? extends V> items);

	@Override
	public abstract boolean removeAll(java.util.Collection<?> c);

	@Override
	public boolean retainAll(java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	protected final PVector<V> getList() {
		if (isManaged()) {
			var txn = Transaction.getCurrent();
			if (txn == null) {
				return _list;
			}
			txn.VerifyRecordAccessed(this, true);
			Log log = txn.GetLog(getParent().getObjectId() + getVariableId());
			if (log == null)
				return _list;
			@SuppressWarnings("unchecked")
			var listLog = (LogList<V>)log;
			return listLog.getValue();
		}
		return _list;
	}

	public final void CopyTo(V[] array, int arrayIndex) {
		var data = getList();
		for (var e : data)
			array[arrayIndex++] = e;
	}

	@Override
	public Object[] toArray() {
		return getList().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		//noinspection SuspiciousToArrayCall
		return getList().toArray(a);
	}

	@Override
	public final boolean isEmpty() {
		return getList().isEmpty();
	}

	@Override
	public final int size() {
		return getList().size();
	}

	@Override
	public final boolean contains(Object v) {
		return getList().contains(v);
	}

	@Override
	public int indexOf(Object o) {
		return getList().indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return getList().lastIndexOf(o);
	}

	@Override
	public V get(int index) {
		return getList().get(index);
	}

	@Override
	public Iterator<V> iterator() {
		return new Iterator<>() {
			private final Iterator<V> it = getList().iterator();
			private V next;

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public V next() {
				return next = it.next();
			}

			@Override
			public void remove() {
				CollList.this.remove(next);
			}
		};
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getList());
		return sb.toString();
	}

	@Override
	public boolean containsAll(java.util.Collection<?> c) {
		return getList().containsAll(c);
	}

	@Override
	public boolean addAll(int index, java.util.Collection<? extends V> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(UnaryOperator<V> operator) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<V> listIterator() {
		//TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<V> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<V> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}
}
