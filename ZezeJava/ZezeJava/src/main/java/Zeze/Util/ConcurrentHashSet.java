package Zeze.Util;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

public class ConcurrentHashSet<T> extends ConcurrentHashMap<T, T> implements Iterable<T> {
	public boolean add(@NotNull T e) {
		return putIfAbsent(e, e) == null;
	}

	/*
	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return keySet().containsAll(c);
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends T> c) {
		var result = false;
		for (var o : c)
			result |= putIfAbsent(o, PRESENT) == null;
		return result;
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		var result = false;
		for (var e : entrySet()) {
			if (c.contains(e.getKey()))
				result |= null != remove(e.getKey());
		}
		return result;
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		var result = false;
		for (var o : c)
			result |= remove(o) != null;
		return result;
	}

	@Override
	public Spliterator<T> spliterator() {
		return Set.super.spliterator();
	}
	*/
	@Override
	public boolean contains(@NotNull Object e) {
		return containsKey(e);
	}

	@Override
	public @NotNull Iterator<T> iterator() {
		return keySet().iterator();
	}

	/*
	@NotNull
	@Override
	public Object[] toArray() {
		return keySet().toArray();
	}

	@NotNull
	@Override
	public <T1> T1[] toArray(@NotNull T1[] a) {
		return keySet().toArray(a);
	}
	*/
	@Override
	public @NotNull String toString() {
		return keySet().toString();
	}

	public void addAll(@NotNull Iterable<T> es) {
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
