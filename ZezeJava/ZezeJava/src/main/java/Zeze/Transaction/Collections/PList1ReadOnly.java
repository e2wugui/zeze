package Zeze.Transaction.Collections;

import java.util.Iterator;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PList1ReadOnly<V> implements Iterable<V> {
	private final @NotNull PList1<V> list;

	public PList1ReadOnly(@NotNull PList1<V> list) {
		this.list = list;
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public int size() {
		return list.size();
	}

	public @NotNull V get(int index) {
		return list.get(index);
	}

	public boolean contains(@NotNull V v) {
		return list.contains(v);
	}

	public boolean containsAll(@NotNull java.util.Collection<? extends V> c) {
		return list.containsAll(c);
	}

	public int indexOf(@NotNull V v) {
		return list.indexOf(v);
	}

	public int lastIndexOf(@NotNull V v) {
		return list.lastIndexOf(v);
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
	public @NotNull Iterator<V> iterator() {
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

	public @NotNull PList1<V> copy() {
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
		return obj instanceof PList1ReadOnly && list.equals(((PList1ReadOnly<?>)obj).list);
	}

	@Override
	public @NotNull String toString() {
		return list.toString();
	}
}
