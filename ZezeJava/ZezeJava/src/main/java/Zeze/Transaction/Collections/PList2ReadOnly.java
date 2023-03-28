package Zeze.Transaction.Collections;

import java.util.Iterator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PList2ReadOnly<V extends Bean, VReadOnly> implements Iterable<VReadOnly> {
	private final @NotNull PList2<V> list;

	public PList2ReadOnly(@NotNull PList2<V> list) {
		this.list = list;
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public int size() {
		return list.size();
	}

	@SuppressWarnings("unchecked")
	public @NotNull VReadOnly get(int index) {
		return (VReadOnly)list.get(index);
	}

	public boolean contains(@NotNull VReadOnly v) {
		//noinspection SuspiciousMethodCalls
		return list.contains(v);
	}

	public boolean containsAll(@NotNull java.util.Collection<? extends VReadOnly> c) {
		//noinspection SuspiciousMethodCalls
		return list.containsAll(c);
	}

	public int indexOf(@NotNull VReadOnly o) {
		//noinspection SuspiciousMethodCalls
		return list.indexOf(o);
	}

	public int lastIndexOf(@NotNull VReadOnly o) {
		//noinspection SuspiciousMethodCalls
		return list.lastIndexOf(o);
	}

	public void copyTo(V @NotNull [] array, int arrayIndex) {
		list.copyTo(array, arrayIndex);
	}

	public Object @NotNull [] toArray() {
		return list.toArray();
	}

	public <T> T @NotNull [] toArray(T @NotNull [] a) {
		return list.toArray(a);
	}

	@Override
	public @NotNull Iterator<VReadOnly> iterator() {
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

	public @NotNull PList2<V> copy() {
		return list.copy();
	}

	public void encode(@NotNull ByteBuffer bb) {
		list.encode(bb);
	}

	@Override
	public int hashCode() {
		return list.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof PList2ReadOnly && list.equals(((PList2ReadOnly<?, ?>)obj).list);
	}

	@Override
	public @NotNull String toString() {
		return list.toString();
	}
}
