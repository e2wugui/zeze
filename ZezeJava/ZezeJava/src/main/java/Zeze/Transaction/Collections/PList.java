package Zeze.Transaction.Collections;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.Transaction;

import org.jspecify.annotations.Nullable;
import Zeze.Collections.Persistents;
import org.pcollections.PVector;

/** 事务 List 基类：基于 pcollections 持久化向量的 List 视图，托管下的修改经 LogList 记账。 */
public abstract class PList<V> extends Collection implements List<V> {
	PVector<V> list = Persistents.vector();

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

	@Deprecated
	@Override
	public boolean retainAll(java.util.Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public final PVector<V> getList() {
		if (isManaged()) {
			var txn = Transaction.getCurrentVerifyRead(this);
			if (txn == null)
				return list;
			//noinspection DataFlowIssue
			TransactionLog log = txn.getLog(parent().objectId() + variableId());
			if (log == null)
				return list;
			//noinspection unchecked
			var listLog = (LogList<V>)log;
			return listLog.getValue();
		}
		return list;
	}

	public final void copyTo(V[] array, int arrayIndex) {
		for (V v : getList())
			array[arrayIndex++] = v;
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
			private final PVector<V> snapshot = getList(); // 创建时刻的快照，迭代安全
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
					throw new IllegalStateException(); // 已删除或未调用 next
				// 以 1-based 当前坐标配合 lastReturned 做身份校验：连续 remove 引起左移不影响，外部结构性修改则抛 CME。
				var current = getList();
				if (i > current.size() || current.get(--i) != lastReturned)
					throw new ConcurrentModificationException("structural modification during iteration");
				PList.this.remove(i);
				index = -i;
			}
		};
	}

	@Override
	public ListIterator<V> listIterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator<V> listIterator(int index) {
		int size = size();
		if (Integer.compareUnsigned(index, size) > 0)
			throw new IndexOutOfBoundsException("invalid index = " + index + " (size = " + size + ')');
		return new ListIterator<>() {
			private int cursor = index;
			private int lastRet = -1;
			private @Nullable V lastReturned;

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
					lastReturned = v;
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
					lastReturned = v;
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
				int i = lastRet;
				if (i < 0)
					throw new IllegalStateException();
				// 校验当前下标元素与 lastReturned 身份一致，防止外部结构性修改导致误删。
				var current = getList();
				if (i >= current.size() || current.get(i) != lastReturned)
					throw new ConcurrentModificationException("structural modification during iteration");
				try {
					PList.this.remove(i);
					if (i < cursor)
						cursor--;
					lastRet = -1;
					lastReturned = null;
				} catch (IndexOutOfBoundsException e) {
					throw new ConcurrentModificationException();
				}
			}

			@Override
			public void set(V v) {
				int i = lastRet;
				if (i < 0)
					throw new IllegalStateException();
				// 校验当前下标元素与 lastReturned 身份一致，防止外部结构性修改导致误改。
				var current = getList();
				if (i >= current.size() || current.get(i) != lastReturned)
					throw new ConcurrentModificationException("structural modification during iteration");
				try {
					PList.this.set(i, v);
					lastReturned = v; // 更新身份校验基准以支持连续 set
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
		// 按 List 接口契约比较内容，确保与标准 List 之间的对称性。
		return o instanceof java.util.List && getList().equals(o);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getList());
		return sb.toString();
	}

	@Override
	public boolean containsAll(java.util.Collection<?> c) {
		//noinspection SlowListContainsAll
		return getList().containsAll(c);
	}

	@Deprecated
	@Override
	public boolean addAll(int index, java.util.Collection<? extends V> c) {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public List<V> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}
}
