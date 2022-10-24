package Zeze.Transaction.Collections;

import java.util.Iterator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;

public class PList2ReadOnly<V extends Bean, VReadOnly> implements Iterable<VReadOnly> {
	private final PList2<V> list;

	public PList2ReadOnly(PList2<V> list) {
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

	public boolean contains(Object v) {
		//noinspection SuspiciousMethodCalls
		return list.contains(v);
	}

	public boolean containsAll(java.util.Collection<?> c) {
		//noinspection SuspiciousMethodCalls
		return list.containsAll(c);
	}

	public int indexOf(Object o) {
		//noinspection SuspiciousMethodCalls
		return list.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		//noinspection SuspiciousMethodCalls
		return list.lastIndexOf(o);
	}

	public void copyTo(V[] array, int arrayIndex) {
		list.copyTo(array, arrayIndex);
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <T> T[] toArray(T[] a) {
		//noinspection SuspiciousToArrayCall
		return list.toArray(a);
	}

	@Override
	public Iterator<VReadOnly> iterator() {
		return new Iterator<>() {
			private final Iterator<V> it = list.iterator();

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@SuppressWarnings("unchecked")
			@Override
			public VReadOnly next() {
				return (VReadOnly)it.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public PList2<V> copy() {
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
		return obj instanceof PList2ReadOnly && list.equals(((PList2ReadOnly<?, ?>)obj).list);
	}

	@Override
	public String toString() {
		return list.toString();
	}
}
