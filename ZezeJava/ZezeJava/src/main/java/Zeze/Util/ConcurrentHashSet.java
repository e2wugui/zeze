package Zeze.Util;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashSet<T> extends ConcurrentHashMap<T, Object> implements Iterable<T> {
	private static final Object PRESENT = new Object();

	public boolean add(T e) {
		return putIfAbsent(e, PRESENT) == null;
	}

	@Override
	public boolean contains(Object e) {
		return get(e) != null;
	}

	@Override
	public Iterator<T> iterator() {
		return keySet().iterator();
	}

	@Override
	public String toString() {
		return keySet().toString();
	}
}
