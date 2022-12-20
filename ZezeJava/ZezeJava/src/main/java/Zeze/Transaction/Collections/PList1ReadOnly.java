package Zeze.Transaction.Collections;

import java.util.Iterator;
import Zeze.Serialize.ByteBuffer;

public class PList1ReadOnly<V> implements Iterable<V> {
	private final PList1<V> list;

	public PList1ReadOnly(PList1<V> list) {
		this.list = list;
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public int size() {
		return list.size();
	}

	public V get(int index) {
		return list.get(index);
	}

	public boolean contains(V v) {
		return list.contains(v);
	}

	public boolean containsAll(java.util.Collection<? extends V> c) {
		return list.containsAll(c);
	}

	public int indexOf(V v) {
		return list.indexOf(v);
	}

	public int lastIndexOf(V v) {
		return list.lastIndexOf(v);
	}

	public void copyTo(V[] array, int arrayIndex) {
		list.copyTo(array, arrayIndex);
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	@Override
	public Iterator<V> iterator() {
		return new Iterator<>() {
			private final Iterator<V> it = list.iterator();

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public V next() {
				return it.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public PList1<V> copy() {
		return list.copy();
	}

	public void encode(ByteBuffer bb) {
		list.encode(bb);
	}

	@Override
	public int hashCode() {
		return list.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PList1ReadOnly && list.equals(((PList1ReadOnly<?>)obj).list);
	}

	@Override
	public String toString() {
		return list.toString();
	}
}
