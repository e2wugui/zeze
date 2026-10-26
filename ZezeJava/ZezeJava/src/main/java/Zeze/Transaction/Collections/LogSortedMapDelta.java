package Zeze.Transaction.Collections;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import Zeze.Transaction.Bean;

/** 有序 Map 事务结构增量：记账使用源 Map 的比较器，值变化语义由具体家族提供。 */
public abstract class LogSortedMapDelta<K extends Comparable<K>, V> extends LogSortedMap<K, V> {
	/** SortedMap1 的值按 equals 判断变化；不持有编解码元数据。 */
	static final class Map1<K extends Comparable<K>, V> extends LogSortedMapDelta<K, V> {
		Map1(PSortedMap<K, V> source, org.pcollections.PSortedMap<K, V> value) {
			super(source, value);
		}

		@Override
		protected boolean isValueChanged(@Nullable V oldValue, V newValue) {
			return !java.util.Objects.equals(oldValue, newValue);
		}

		@Override
		public Map1<K, V> beginSavepoint() {
			return new Map1<>(source, getValue());
		}
	}

	/** SortedMap2 的 Bean 值按对象身份判断变化；不持有编解码元数据。 */
	static final class Map2<K extends Comparable<K>, V extends Bean> extends LogSortedMapDelta<K, V> {
		Map2(PSortedMap<K, V> source, org.pcollections.PSortedMap<K, V> value) {
			super(source, value);
		}

		@Override
		protected boolean isValueChanged(@Nullable V oldValue, V newValue) {
			return oldValue != newValue;
		}

		@Override
		public Map2<K, V> beginSavepoint() {
			return new Map2<>(source, getValue());
		}
	}

	@Override
	public final boolean hasDelta() {
		return true;
	}

	private @Nullable Map<K, V> replaced;
	private @Nullable Set<K> removed;

	protected LogSortedMapDelta(PSortedMap<K, V> source, org.pcollections.PSortedMap<K, V> value) {
		super(source, value);
	}

	public @Nullable Map<K, V> getReplaced() {
		return replaced;
	}

	public @Nullable Set<K> getRemoved() {
		return removed;
	}

	private Map<K, V> replacedForWrite() {
		return replaced != null ? replaced : (replaced = new TreeMap<>(getValue().comparator()));
	}

	private Set<K> removedForWrite() {
		return removed != null ? removed : (removed = new TreeSet<>(getValue().comparator()));
	}

	public @Nullable V put(K key, V value) {
		V exist = super.put(key, value);
		noteReplacement(key, value, exist);
		return exist;
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		var old = getValue();
		super.putAll(m);
		var newMap = getValue();
		if (newMap != old) {
			// 输入可能包含多个比较器等价的键，必须记最终值而非中间输入值。
			// 变化判定按系覆写：1系值不可变用equals；2系值可变须用身份（见LogSortedMap2）。
			for (K k : m.keySet()) {
				V v = newMap.get(k);
				noteReplacement(k, v, old.get(k));
			}
		}
	}

	private void noteReplacement(K key, V value, @Nullable V oldValue) {
		if (isValueChanged(oldValue, value)) {
			if (removed != null) removed.remove(key);
			replacedForWrite().put(key, value);
		}
	}

	public @Nullable V remove(K key) {
		V old = super.remove(key);
		if (old != null) {
			if (replaced != null) replaced.remove(key);
			removedForWrite().add(key);
		}
		return old;
	}

	public boolean remove(K key, V value) {
		// 空值防御对齐非托管分支（同 LogMap1.remove(K,V)，20261020-06）。
		if (super.remove(key, value)) {
			if (replaced != null) replaced.remove(key);
			removedForWrite().add(key);
			return true;
		}
		return false;
	}

	public void clear() {
		for (var ks : getValue().keySet()) {
			if (replaced != null) replaced.remove(ks);
			removedForWrite().add(ks);
		}
		super.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void mergeFrom(LogCollection<?> child) {
		super.mergeFrom(child);
		var another = (LogSortedMapDelta<K, V>)child;
		mergeChangeNote(another);
	}

	private void mergeChangeNote(LogSortedMapDelta<K, V> another) {
		if (another.replaced != null) {
			for (var e : another.replaced.entrySet()) {
				K k = e.getKey();
				if (removed != null) removed.remove(k);
				replacedForWrite().put(k, e.getValue());
			}
		}
		if (another.removed != null) {
			for (K k : another.removed) {
				if (replaced != null) replaced.remove(k);
				removedForWrite().add(k);
			}
		}
	}

	protected abstract boolean isValueChanged(@Nullable V oldValue, V newValue);
}
