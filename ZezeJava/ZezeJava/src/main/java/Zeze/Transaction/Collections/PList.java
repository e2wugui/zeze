package Zeze.Transaction.Collections;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.pcollections.Empty;
import org.pcollections.PVector;

public abstract class PList<V> extends Collection implements List<V> {
	public PVector<V> list = Empty.vector();

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

	@Deprecated // unsupported
	@Override
	public boolean retainAll(java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	protected final PVector<V> getList() {
		if (isManaged()) {
			var txn = Transaction.getCurrentVerifyRead(this);
			if (txn == null)
				return list;
			Log log = txn.getLog(parent().objectId() + variableId());
			if (log == null)
				return list;
			@SuppressWarnings("unchecked")
			var listLog = (LogList<V>)log;
			return listLog.getValue();
		}
		return list;
	}

	public final void copyTo(V[] array, int arrayIndex) {
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
				PList.this.remove(next);
			}
		};
	}

	@Override
	public int hashCode() {
		return list.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof PList && list.equals(((PList<?>)o).list);
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

	@Deprecated // unsupported
	@Override
	public boolean addAll(int index, java.util.Collection<? extends V> c) {
		throw new UnsupportedOperationException();
	}

	@Deprecated // unsupported
	@Override
	public void replaceAll(UnaryOperator<V> operator) {
		throw new UnsupportedOperationException();
	}

	@Deprecated // unsupported
	@Override
	public ListIterator<V> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Deprecated // unsupported
	@Override
	public ListIterator<V> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Deprecated // unsupported
	@Override
	public List<V> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}
}
