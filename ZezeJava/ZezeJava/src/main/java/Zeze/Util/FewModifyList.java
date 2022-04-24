package Zeze.Util;

import java.nio.file.Path;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

public class FewModifyList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable{
	private volatile List<E> read;
	private List<E> write = new ArrayList<>();

	private List<E> prepareRead() {
		if (null != read)
			return read;

		synchronized (write) {
			read = new ArrayList<>();
			read.addAll(write);
			return read;
		}
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
			var r = write.remove(index);
			read = null;
			return r;
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
		throw new UnsupportedOperationException();
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
		return prepareRead().toArray(a);
	}

	@Override
	public boolean add(E e) {
		synchronized (write) {
			var r = write.add(e);
			read = null;
			return r;
		}
	}

	@Override
	public boolean remove(Object o) {
		synchronized (write) {
			var r = write.remove(o);
			read = null;
			return r;
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return prepareRead().containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		synchronized (write) {
			var r = write.addAll(c);
			read = null;
			return r;
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		synchronized (write) {
			var r = write.addAll(index, c);
			read = null;
			return r;
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		synchronized (write) {
			var r = write.removeAll(c);
			read = null;
			return r;
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		synchronized (write) {
			var r = write.retainAll(c);
			read = null;
			return r;
		}
	}

	@Override
	public void clear() {
		synchronized (write) {
			write.clear();
			read = null;
		}
	}
}
