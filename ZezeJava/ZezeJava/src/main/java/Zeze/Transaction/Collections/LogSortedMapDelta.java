package Zeze.Transaction.Collections;

import org.jspecify.annotations.Nullable;
import org.pcollections.Empty;
import org.pcollections.TreePMap;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** 有序 Map 事务结构增量：记账使用源 Map 的比较器，值变化语义由具体家族提供。 */
public abstract class LogSortedMapDelta<K extends Comparable<K>, V> extends LogSortedMap<K, V> {
	@Override
	public final boolean hasDelta() {
		return true;
	}

	private final Map<K, V> replaced;
	private final Set<K> removed;

	protected LogSortedMapDelta(PSortedMap<K, V> source, org.pcollections.PSortedMap<K, V> value) {
		super(source, value);

		replaced = new TreeMap<>(value.comparator());
		removed = new TreeSet<>(value.comparator());
	}

	public Map<K, V> getReplaced() {
		return replaced;
	}

	public Set<K> getRemoved() {
		return removed;
	}

	public @Nullable V get(K key) {
		return getValue().get(key);
	}

	public void add(K key, V value) {
		put(key, value);
	}

	public @Nullable V put(K key, V value) {
		V exist = getValue().get(key);
		setValue(getValue().plus(key, value));
		removed.remove(key);
		replaced.put(key, value);
		return exist;
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		var old = getValue();
		var newMap = old.plusAll(m);
		if (newMap != old) {
			setValue(newMap);
			// 输入可能包含多个比较器等价的键，必须记最终值而非中间输入值。
			// 变化判定按系覆写：1系值不可变用equals；2系值可变须用身份（见LogSortedMap2）。
			for (K k : m.keySet()) {
				V v = newMap.get(k);
				if (isValueChanged(old.get(k), v)) {
					removed.remove(k);
					replaced.put(k, v);
				}
			}
		}
	}

	public @Nullable V remove(K key) {
		V old = getValue().get(key);
		if (old != null) {
			setValue(getValue().minus(key));
			replaced.remove(key);
			removed.add(key);
		}
		return old;
	}

	public boolean remove(K key, V value) {
		V old = getValue().get(key);
		// 空值防御对齐非托管分支（同 LogMap1.remove(K,V)，20261020-06）。
		if (old != null && old.equals(value)) {
			setValue(getValue().minus(key));
			replaced.remove(key);
			removed.add(key);
			return true;
		}
		return false;
	}

	public void clear() {
		for (var ks : getValue().keySet()) {
			replaced.remove(ks);
			removed.add(ks);
		}
		var comparator = getValue().comparator();
		setValue(comparator != null ? TreePMap.empty(comparator) : Empty.sortedMap());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void mergeFrom(LogCollection child) {
		super.mergeFrom(child);
		var another = (LogSortedMapDelta<K, V>)child;
		mergeChangeNote(another);
	}

	private void mergeChangeNote(LogSortedMapDelta<K, V> another) {
		for (var e : another.replaced.entrySet()) {
			K k = e.getKey();
			removed.remove(k);
			replaced.put(k, e.getValue());
		}
		for (K k : another.removed) {
			replaced.remove(k);
			removed.add(k);
		}
	}

	protected abstract boolean isValueChanged(@Nullable V oldValue, V newValue);
}
