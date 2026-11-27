package Zeze.Transaction.Collections;

import java.util.Iterator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** PList2 的只读视图：元素以只读类型暴露，修改入口抛 UnsupportedOperationException。 */
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

	@SuppressWarnings("unchecked")
	public VReadOnly get(int index) {
		return (VReadOnly)list.get(index);
	}

	public boolean contains(VReadOnly v) {
		//noinspection SuspiciousMethodCalls
		return list.contains(v);
	}

	public boolean containsAll(java.util.Collection<? extends VReadOnly> c) {
		//noinspection SlowListContainsAll,SuspiciousMethodCalls
		return list.containsAll(c);
	}

	public int indexOf(VReadOnly o) {
		//noinspection SuspiciousMethodCalls
		return list.indexOf(o);
	}

	public int lastIndexOf(VReadOnly o) {
		//noinspection SuspiciousMethodCalls
		return list.lastIndexOf(o);
	}

	// 按 VReadOnly 暴露元素，避免向调用方泄露可写的受管 Bean 引用。
	public void copyTo(VReadOnly [] array, int arrayIndex) {
		for (var v : this)
			array[arrayIndex++] = v;
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
	public boolean equals(@Nullable Object obj) {
		return obj instanceof PList2ReadOnly && list.equals(((PList2ReadOnly<?, ?>)obj).list);
	}

	@Override
	public String toString() {
		return list.toString();
	}
}
