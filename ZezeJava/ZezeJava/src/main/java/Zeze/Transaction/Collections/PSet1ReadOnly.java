package Zeze.Transaction.Collections;

import java.util.Iterator;
import Zeze.Serialize.ByteBuffer;
import org.jspecify.annotations.Nullable;

/** PSet1 的只读视图：仅暴露查询与遍历，修改入口抛 UnsupportedOperationException。 */
public class PSet1ReadOnly<V> implements Iterable<V> {
	private final PSet1<V> set;

	public PSet1ReadOnly(PSet1<V> set) {
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

	public boolean containsAll(java.util.Collection<? extends V> c) {
		return set.containsAll(c);
	}

	public void copyTo(V [] array, int arrayIndex) {
		set.copyTo(array, arrayIndex);
	}

	public Object [] toArray() {
		return set.toArray();
	}

	public <T> T [] toArray(T [] a) {
		return set.toArray(a);
	}

	@Override
	public Iterator<V> iterator() {
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

	public PSet1<V> copy() {
		return set.copy();
	}

	public void encode(ByteBuffer bb) {
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
	public String toString() {
		return set.toString();
	}
}
