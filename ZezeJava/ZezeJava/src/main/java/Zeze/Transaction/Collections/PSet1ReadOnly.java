package Zeze.Transaction.Collections;

import java.util.Iterator;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PSet1ReadOnly<V> implements Iterable<V> {
	private final @NotNull PSet1<V> set;

	public PSet1ReadOnly(@NotNull PSet1<V> set) {
		this.set = set;
	}

	public boolean isEmpty() {
		return set.isEmpty();
	}

	public int size() {
		return set.size();
	}

	public boolean contains(V v) {
		return set.contains(v);
	}

	public boolean containsAll(@NotNull java.util.Collection<? extends V> c) {
		return set.containsAll(c);
	}

	public void copyTo(V @NotNull [] array, int arrayIndex) {
		set.copyTo(array, arrayIndex);
	}

	public Object @NotNull [] toArray() {
		return set.toArray();
	}

	public <T> T @NotNull [] toArray(T @NotNull [] a) {
		return set.toArray(a);
	}

	@Override
	public @NotNull Iterator<V> iterator() {
		return new Iterator<>() {
			private final Iterator<V> it = set.iterator();

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

	public @NotNull PSet1<V> copy() {
		return set.copy();
	}

	public void encode(@NotNull ByteBuffer bb) {
		set.encode(bb);
	}

	@Override
	public int hashCode() {
		return set.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof PSet1ReadOnly && set.equals(((PSet1ReadOnly<?>)obj).set);
	}

	@Override
	public @NotNull String toString() {
		return set.toString();
	}
}
