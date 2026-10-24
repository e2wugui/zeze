package Zeze.Transaction.Collections;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/** Persistent Map state used by both tracked and untracked transactions. */
public abstract class LogMap<K, V> extends LogCollection<PMap<K, V>> {
	private org.pcollections.PMap<K, V> value;

	protected LogMap(PMap<K, V> source, org.pcollections.PMap<K, V> value) {
		super(source);
		this.value = value;
	}

	public final org.pcollections.PMap<K, V> getValue() {
		return value;
	}

	final void setValue(org.pcollections.PMap<K, V> value) {
		this.value = value;
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
		remove(key);
		return true;
	}

	public void clear() {
		value = Zeze.Collections.Persistents.map();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void mergeFrom(LogCollection<?> child) {
		value = ((LogMap<K, V>)child).value;
	}

	@Override
	public final void commit() {
		source.map = value;
	}
}
