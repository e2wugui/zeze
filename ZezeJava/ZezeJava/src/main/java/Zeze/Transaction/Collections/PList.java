package Zeze.Transaction.Collections;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;
import org.pcollections.PVector;

public abstract class PList<V> extends Collection implements List<V> {
	public @NotNull PVector<V> list = Empty.vector();

	@Override
	public abstract boolean add(@NotNull V item);

	@Override
	public abstract boolean remove(@NotNull Object item);

	@Override
	public abstract void clear();

	@Override
	public abstract @NotNull V set(int index, @NotNull V item);

	@Override
	public abstract void add(int index, @NotNull V item);

	@Override
	public abstract @NotNull V remove(int index);

	@Override
	public abstract boolean addAll(@NotNull java.util.Collection<? extends V> items);

	@Override
	public abstract boolean removeAll(@NotNull java.util.Collection<?> c);

	@Deprecated // unsupported
	@Override
	public boolean retainAll(@NotNull java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public final @NotNull PVector<V> getList() {
		if (isManaged()) {
			var txn = Transaction.getCurrentVerifyRead(this);
			if (txn == null)
				return list;
			//noinspection DataFlowIssue
			Log log = txn.getLog(parent().objectId() + variableId());
			if (log == null)
				return list;
			@SuppressWarnings("unchecked")
			var listLog = (LogList<V>)log;
			return listLog.getValue();
		}
		return list;
	}

	public final void copyTo(V @NotNull [] array, int arrayIndex) {
		var data = getList();
		for (var e : data)
			array[arrayIndex++] = e;
	}

	@Override
	public Object @NotNull [] toArray() {
		return getList().toArray();
	}

	@Override
	public <T> T @NotNull [] toArray(T @NotNull [] a) {
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
	public final boolean contains(@NotNull Object v) {
		return getList().contains(v);
	}

	@Override
	public int indexOf(@NotNull Object o) {
		return getList().indexOf(o);
	}

	@Override
	public int lastIndexOf(@NotNull Object o) {
		return getList().lastIndexOf(o);
	}

	@Override
	public V get(int index) {
		return getList().get(index);
	}

	@Override
	public @NotNull Iterator<V> iterator() {
		return new Iterator<>() {
			private final Iterator<V> it = getList().iterator();
			private int index;

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public V next() {
				var v = it.next();
				index = Math.abs(index) + 1;
				return v;
			}

			@Override
			public void remove() {
				int i = index;
				if (i <= 0)
					throw new IllegalStateException(); // removed or not next
				PList.this.remove(--i);
				index = -i;
			}
		};
	}

	@Override
	public @NotNull ListIterator<V> listIterator() {
		return listIterator(0);
	}

	@Override
	public @NotNull ListIterator<V> listIterator(int index) {
		int size = size();
		if (index < 0 || index > size)
			throw new IndexOutOfBoundsException("invalid index = " + index + " (size = " + size + ')');
		return new ListIterator<>() {
			private int idx = index;
			private int lastWay; // 1:next; 2:previous

			@Override
			public boolean hasNext() {
				return idx < size();
			}

			@Override
			public V next() {
				int i = idx;
				if (i >= size())
					throw new NoSuchElementException();
				V v = get(i++);
				idx = i;
				lastWay = 1;
				return v;
			}

			@Override
			public boolean hasPrevious() {
				return idx > 0;
			}

			@Override
			public V previous() {
				int i = idx;
				if (i <= 0)
					throw new NoSuchElementException();
				V v = get(--i);
				idx = i;
				lastWay = 2;
				return v;
			}

			@Override
			public int nextIndex() {
				return idx;
			}

			@Override
			public int previousIndex() {
				return idx - 1;
			}

			@Override
			public void remove() {
				if (lastWay == 0)
					throw new IllegalStateException();
				if (lastWay == 1) // next
					PList.this.remove(--idx);
				else // previous
					PList.this.remove(idx);
				lastWay = 0;
			}

			@Override
			public void set(V v) {
				if (lastWay == 0)
					throw new IllegalStateException();
				if (lastWay == 1) // next
					PList.this.set(idx - 1, v);
				else // previous
					PList.this.set(idx, v);
			}

			@Override
			public void add(V v) {
				PList.this.add(idx++, v);
			}
		};
	}

	@Override
	public int hashCode() {
		return getList().hashCode();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		return o instanceof PList && getList().equals(((PList<?>)o).getList());
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getList());
		return sb.toString();
	}

	@Override
	public boolean containsAll(@NotNull java.util.Collection<?> c) {
		return getList().containsAll(c);
	}

	@Deprecated // unsupported
	@Override
	public boolean addAll(int index, @NotNull java.util.Collection<? extends V> c) {
		throw new UnsupportedOperationException();
	}

	@Deprecated // unsupported
	@Override
	public @NotNull List<V> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}
}
