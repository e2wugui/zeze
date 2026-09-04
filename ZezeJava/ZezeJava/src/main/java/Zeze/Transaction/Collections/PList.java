package Zeze.Transaction.Collections;

import java.util.ConcurrentModificationException;
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
	@NotNull PVector<V> list = Empty.vector();

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
		for (V v : getList())
			array[arrayIndex++] = v;
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
			private final @NotNull PVector<V> snapshot = getList(); // 创建时刻的快照，迭代安全
			private final Iterator<V> it = snapshot.iterator();
			private int index;
			private @Nullable V lastReturned;

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public V next() {
				V v = it.next();
				index = Math.abs(index) + 1;
				lastReturned = v;
				return v;
			}

			@Override
			public void remove() {
				int i = index;
				if (i <= 0)
					throw new IllegalStateException(); // removed or not next
				// index是当前列表坐标系的1-based游标：迭代器自身的remove()引起左移后，下一次next()
				// 的+1恰好补偿，链式删除下依然正确。因此不能拿快照同下标比较（先期删除后快照与当前
				// 列表错位，第二次remove必抛假阳性CME）。改用next()记录的返回引用做身份比较：
				// current[i-1]==lastReturned是"按下标删除不会删错元素"的充分条件；不相等说明迭代
				// 期间发生过外部结构性修改，fail-fast（对齐JDK迭代器惯例）。
				var current = getList();
				if (i > current.size() || current.get(--i) != lastReturned)
					throw new ConcurrentModificationException("structural modification during iteration");
				PList.this.remove(i);
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
		if (Integer.compareUnsigned(index, size) > 0)
			throw new IndexOutOfBoundsException("invalid index = " + index + " (size = " + size + ')');
		return new ListIterator<>() {
			private int cursor = index;
			private int lastRet = -1;

			@Override
			public boolean hasNext() {
				return cursor < size();
			}

			@Override
			public V next() {
				try {
					int i = cursor;
					V v = get(i);
					cursor = i + 1;
					lastRet = i;
					return v;
				} catch (IndexOutOfBoundsException e) {
					throw new NoSuchElementException();
				}
			}

			@Override
			public boolean hasPrevious() {
				return cursor > 0;
			}

			@Override
			public V previous() {
				try {
					int i = cursor - 1;
					V v = get(i);
					cursor = i;
					lastRet = i;
					return v;
				} catch (IndexOutOfBoundsException e) {
					throw new NoSuchElementException();
				}
			}

			@Override
			public int nextIndex() {
				return cursor;
			}

			@Override
			public int previousIndex() {
				return cursor - 1;
			}

			@Override
			public void remove() {
				if (lastRet < 0)
					throw new IllegalStateException();
				try {
					PList.this.remove(lastRet);
					if (lastRet < cursor)
						cursor--;
					lastRet = -1;
				} catch (IndexOutOfBoundsException e) {
					throw new ConcurrentModificationException();
				}
			}

			@Override
			public void set(V v) {
				if (lastRet < 0)
					throw new IllegalStateException();
				try {
					PList.this.set(lastRet, v);
				} catch (IndexOutOfBoundsException e) {
					throw new ConcurrentModificationException();
				}
			}

			@Override
			public void add(V v) {
				try {
					int i = cursor;
					PList.this.add(i, v);
					cursor = i + 1;
					lastRet = -1;
				} catch (IndexOutOfBoundsException e) {
					throw new ConcurrentModificationException();
				}
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
		//noinspection SlowListContainsAll
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
