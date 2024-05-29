package Zeze.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FewModifyList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
	private transient volatile @Nullable List<E> read;
	private final @NotNull ArrayList<E> write;
	private transient final ReentrantLock writeLock = new ReentrantLock();

	public FewModifyList() {
		write = new ArrayList<>();
	}

	public FewModifyList(int initialCapacity) {
		write = new ArrayList<>(initialCapacity);
	}

	public FewModifyList(@NotNull Collection<? extends E> m) {
		write = new ArrayList<>(m);
	}

	private @NotNull List<E> prepareRead() {
		var r = read;
		if (r == null) {
			writeLock.lock();
			try {
				if ((r = read) == null)
					read = r = List.copyOf(write);
			} finally {
				writeLock.unlock();
			}
		}
		return r;
	}

	public @NotNull List<E> snapshot() {
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
	public int indexOf(Object o) {
		return prepareRead().indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return prepareRead().lastIndexOf(o);
	}

	@Override
	public @NotNull ListIterator<E> listIterator() {
		return prepareRead().listIterator();
	}

	@Override
	public @NotNull ListIterator<E> listIterator(int index) {
		return prepareRead().listIterator(index);
	}

	@Override
	public @NotNull List<E> subList(int fromIndex, int toIndex) {
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
	public boolean contains(Object o) {
		return prepareRead().contains(o);
	}

	@Override
	public @NotNull Iterator<E> iterator() {
		return prepareRead().iterator();
	}

	@Override
	public Object @NotNull [] toArray() {
		return prepareRead().toArray();
	}

	@Override
	public <T> T @NotNull [] toArray(T @NotNull [] a) {
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
	public boolean remove(Object o) {
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
	public boolean containsAll(@NotNull Collection<?> c) {
		return prepareRead().containsAll(c);
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends E> c) {
		writeLock.lock();
		try {
			if (!write.addAll(c))
				return false;
			read = null;
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean addAll(int index, @NotNull Collection<? extends E> c) {
		writeLock.lock();
		try {
			if (!write.addAll(index, c))
				return false;
			read = null;
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		writeLock.lock();
		try {
			if (!write.removeAll(c))
				return false;
			read = null;
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		writeLock.lock();
		try {
			if (!write.retainAll(c))
				return false;
			read = null;
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void replaceAll(@NotNull UnaryOperator<E> operator) {
		writeLock.lock();
		try {
			write.replaceAll(operator);
			read = null;
		} finally {
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
			read = null;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public @NotNull Spliterator<E> spliterator() {
		return prepareRead().spliterator();
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull FewModifyList<E> clone() throws CloneNotSupportedException {
		if (getClass() == FewModifyList.class)
			return new FewModifyList<>(prepareRead());
		throw new CloneNotSupportedException();
	}

	@Override
	public @NotNull String toString() {
		return prepareRead().toString();
	}
}
