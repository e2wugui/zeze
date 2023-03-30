package Zeze.Util;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

public class ConcurrentHashSet<T> extends ConcurrentHashMap<T, Object> implements Iterable<T> {
	private static final Object PRESENT = new Object();

	public boolean add(@NotNull T e) {
		return putIfAbsent(e, PRESENT) == null;
	}

	@Override
	public boolean contains(@NotNull Object e) {
		return get(e) != null;
	}

	@Override
	public @NotNull Iterator<T> iterator() {
		return keySet().iterator();
	}

	@Override
	public @NotNull String toString() {
		return keySet().toString();
	}

	public void addAll(@NotNull Iterable<T> es) {
		for (var e : es)
			add(e);
	}
}
