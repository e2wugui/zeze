package Zeze.Transaction.Collections;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/** Persistent SortedMap state used by both tracked and untracked transactions. */
public class LogSortedMap<K extends Comparable<K>, V> extends LogCollection<PSortedMap<K, V>> {
	private org.pcollections.PSortedMap<K, V> value;

	protected LogSortedMap(PSortedMap<K, V> source, org.pcollections.PSortedMap<K, V> value) {
		super(source);
		this.value = value;
	}

	@Override
	public boolean hasDelta() {
		return false;
	}

	@Override
	public LogSortedMap<K, V> beginSavepoint() {
		return new LogSortedMap<>(source, getValue());
	}

	public final org.pcollections.PSortedMap<K, V> getValue() {
		return value;
	}

	public @Nullable V get(K key) {
		return value.get(key);
	}

	public void add(K key, V item) {
		put(key, item);
	}

	public @Nullable V put(K key, V item) {
		V old = value.get(key);
		value = value.plus(key, item);
		return old;
	}

	public void putAll(Map<? extends K, ? extends V> items) {
		value = value.plusAll(items);
	}

	public @Nullable V remove(K key) {
		V old = value.get(key);
		if (old != null) value = value.minus(key);
		return old;
	}

	public boolean remove(K key, V expected) {
		V old = value.get(key);
		if (old == null || !old.equals(expected)) return false;
		value = value.minus(key);
		return true;
	}

	public void clear() {
		var comparator = value.comparator();
		value = comparator != null ? org.pcollections.TreePMap.empty(comparator) : org.pcollections.Empty.sortedMap();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void mergeFrom(LogCollection<?> child) {
		value = ((LogSortedMap<K, V>)child).value;
	}

	@Override
	public final void commit() {
		source.map = value;
	}
}
