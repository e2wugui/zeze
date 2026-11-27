package Zeze.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/**
 * 写少读多的 List：写侧 ArrayList 加锁修改，读侧走不可变快照（写后首次读重建），允许 null 元素。
 */
public class FewModifyList<E extends @Nullable Object> implements List<E>, RandomAccess, Cloneable {
	private transient volatile @Nullable List<E> read;
	private final ArrayList<E> write;
	private transient final ReentrantLock writeLock = new ReentrantLock();

	public FewModifyList() {
		write = new ArrayList<>();
	}

	public FewModifyList(int initialCapacity) {
		write = new ArrayList<>(initialCapacity);
	}

	public FewModifyList(Collection<? extends E> m) {
		write = new ArrayList<>(m);
	}

	private List<E> prepareRead() {
		var r = read;
		if (r == null) {
			writeLock.lock();
			try {
				if ((r = read) == null) {
					// 用 ArrayList 拷贝而非 List.copyOf，以支持 null 元素并保持只读快照语义。
					read = r = Collections.unmodifiableList(new ArrayList<>(write));
				}
			} finally {
				writeLock.unlock();
			}
		}
		return r;
	}

	/** 只读快照：不受本List后续写入影响；元素对象仍共享。 */
	public List<E> snapshot() {
		return prepareRead();
	}

	@Override
	public E get(int index) {
		return prepareRead().get(index);
	}

	@Override
	public E set(int index, E element) {
		writeLock.lock();
		try {
			var prev = write.set(index, element);
			read = null;
			return prev;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void add(int index, E element) {
		writeLock.lock();
		try {
			write.add(index, element);
			read = null;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public E remove(int index) {
		writeLock.lock();
		try {
			var prev = write.remove(index);
			read = null;
			return prev;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public E removeFirst() {
		writeLock.lock();
		try {
			var prev = write.removeFirst();
			read = null;
			return prev;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public E removeLast() {
		writeLock.lock();
		try {
			var prev = write.removeLast();
			read = null;
			return prev;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public int indexOf(@Nullable Object o) {
		return prepareRead().indexOf(o);
	}

	@Override
	public int lastIndexOf(@Nullable Object o) {
		return prepareRead().lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return prepareRead().listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return prepareRead().listIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return prepareRead().subList(fromIndex, toIndex);
	}

	@Override
	public int size() {
		return prepareRead().size();
	}

	@Override
	public boolean isEmpty() {
		return prepareRead().isEmpty();
	}

	@Override
	public boolean contains(@Nullable Object o) {
		return prepareRead().contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return prepareRead().iterator();
	}

	@Override
	public Object [] toArray() {
		return prepareRead().toArray();
	}

	@Override
	public <T> T [] toArray(T [] a) {
		return prepareRead().toArray(a);
	}

	@Override
	public boolean add(E e) {
		writeLock.lock();
		try {
			if (!write.add(e))
				return false;
			read = null;
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean remove(@Nullable Object o) {
		writeLock.lock();
		try {
			if (!write.remove(o))
				return false;
			read = null;
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		//noinspection SlowListContainsAll
		return prepareRead().containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		var snapshot = new ArrayList<>(c);
		writeLock.lock();
		try {
			if (!write.addAll(snapshot))
				return false;
			read = null;
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		var snapshot = new ArrayList<>(c);
		writeLock.lock();
		try {
			if (!write.addAll(index, snapshot))
				return false;
			read = null;
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		var membership = c instanceof FewModifyList<?> other ? other.snapshot() : c;
		writeLock.lock();
		try {
			return write.removeAll(membership);
		} finally {
			read = null;
			writeLock.unlock();
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		var membership = c instanceof FewModifyList<?> other ? other.snapshot() : c;
		writeLock.lock();
		try {
			return write.retainAll(membership);
		} finally {
			read = null;
			writeLock.unlock();
		}
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		writeLock.lock();
		try {
			return write.removeIf(filter);
		} finally {
			read = null;
			writeLock.unlock();
		}
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		writeLock.lock();
		try {
			write.replaceAll(operator);
		} finally {
			read = null;
			writeLock.unlock();
		}
	}

	@Override
	public void clear() {
		writeLock.lock();
		try {
			if (write.isEmpty())
				return;
			write.clear();
			read = null;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void sort(@Nullable Comparator<? super E> c) {
		writeLock.lock();
		try {
			write.sort(c);
		} finally {
			read = null;
			writeLock.unlock();
		}
	}

	@Override
	public Spliterator<E> spliterator() {
		return prepareRead().spliterator();
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public FewModifyList<E> clone() throws CloneNotSupportedException {
		if (getClass() == FewModifyList.class)
			return new FewModifyList<>(prepareRead());
		throw new CloneNotSupportedException();
	}

	@Override
	public String toString() {
		return prepareRead().toString();
	}

	/** 基于调用时刻快照比较内容；hashCode 随内容变化，勿直接作 Map/Set 键（需要稳定键请用 {@link #snapshot()}）。 */
	@Override
	public boolean equals(@Nullable Object obj) {
		return this == obj || obj instanceof List && prepareRead().equals(obj);
	}

	@Override
	public int hashCode() {
		return prepareRead().hashCode();
	}
}
