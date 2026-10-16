package Zeze.Util;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

// 基于 ConcurrentHashMap 的并发 Set：元素本身兼作键与值（putIfAbsent 去重）
public class ConcurrentHashSet<T> extends ConcurrentHashMap<T, T> implements Iterable<T> {
	public boolean add(T e) {
		return putIfAbsent(e, e) == null;
	}

	@Override
	public boolean contains(Object e) {
		return containsKey(e);
	}

	@Override
	public Iterator<T> iterator() {
		return keySet().iterator();
	}

	@Override
	public String toString() {
		return keySet().toString();
	}

	public void addAll(Iterable<T> es) {
		for (var e : es)
			add(e);
	}

	public boolean containsAny(Collection<T> coll) {
		var ks = keySet();
		for (var c : coll) {
			if (ks.contains(c))
				return true;
		}
		return false;
	}
}
