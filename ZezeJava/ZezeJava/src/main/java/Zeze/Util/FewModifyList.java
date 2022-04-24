package Zeze.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

@SuppressWarnings("CloneableClassWithoutClone")
public class FewModifyList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
	private volatile List<E> read;
	private final List<E> write = new ArrayList<>();

	private List<E> prepareRead() {
		var r = read;
		if (r == null) {
			synchronized (write) {
				if ((r = read) == null)
					read = r = new ArrayList<>(write);
			}
		}
		return r;
	}

	@Override
	public E get(int index) {
		return prepareRead().get(index);
	}

	@Override
	public E set(int index, E element) {
		synchronized (write) {
			var prev = write.set(index, element);
			read = null;
			return prev;
		}
	}

	@Override
	public void add(int index, E element) {
		synchronized (write) {
			write.add(index, element);
			read = null;
		}
	}

	@Override
	public E remove(int index) {
		synchronized (write) {
			var prev = write.remove(index);
			read = null;
			return prev;
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
	public ListIterator<E> listIterator() {
		return prepareRead().listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return prepareRead().listIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return Collections.unmodifiableList(prepareRead().subList(fromIndex, toIndex));
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
	public Iterator<E> iterator() {
		return prepareRead().iterator();
	}

	@Override
	public Object[] toArray() {
		return prepareRead().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		//noinspection SuspiciousToArrayCall
		return prepareRead().toArray(a);
	}

	@Override
	public boolean add(E e) {
		synchronized (write) {
			//noinspection ConstantConditions
			if (!write.add(e))
				return false;
			read = null;
			return true;
		}
	}

	@Override
	public boolean remove(Object o) {
		synchronized (write) {
			if (!write.remove(o))
				return false;
			read = null;
			return true;
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return prepareRead().containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		synchronized (write) {
			if (!write.addAll(c))
				return false;
			read = null;
			return true;
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		synchronized (write) {
			if (!write.addAll(index, c))
				return false;
			read = null;
			return true;
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		synchronized (write) {
			if (!write.removeAll(c))
				return false;
			read = null;
			return true;
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		synchronized (write) {
			if (!write.retainAll(c))
				return false;
			read = null;
			return true;
		}
	}

	@Override
	public void clear() {
		synchronized (write) {
			if (write.isEmpty())
				return;
			write.clear();
			read = null;
		}
	}
}
