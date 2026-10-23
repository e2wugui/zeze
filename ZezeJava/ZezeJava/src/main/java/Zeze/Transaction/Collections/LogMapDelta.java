package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import Zeze.Collections.Persistents;

/** Map 事务结构增量：共享增删、保存点与合并规则，值变化语义由具体家族提供。 */
public abstract class LogMapDelta<K, V> extends LogMap<K, V> {
	@Override
	public final boolean hasDelta() {
		return true;
	}

	private final HashMap<K, V> replaced = new HashMap<>();
	private final Set<K> removed = new HashSet<>();

	protected LogMapDelta(PMap<K, V> source, org.pcollections.PMap<K, V> value) {
		super(source, value);
	}

	public HashMap<K, V> getReplaced() {
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
			// 只记值真实变化的键：值未变的键混入replaced是幻影增量，误导增量驱动的监听器。
			// 变化判定按系覆写：1系值不可变用equals；2系值可变须用身份（见LogMap2）。
			for (var e : m.entrySet()) {
				K k = e.getKey();
				if (isValueChanged(old.get(k), e.getValue())) {
					removed.remove(k);
					replaced.put(k, e.getValue());
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
		// 空值防御对齐非托管分支（PMap1/PMap2/PSortedMap1/2 的 exist!=null && exist.equals(v)）：
		// value==null 时旧形态 NPE 炸事务、非托管分支同输入安静返回 false——同输入两分支
		// 行为必须一致（20261020-06，对抗验证定谳为理论缺陷，防御对齐修法）。
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
		setValue(Persistents.map());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void mergeFrom(LogCollection child) {
		super.mergeFrom(child);
		var another = (LogMapDelta<K, V>)child;
		mergeChangeNote(another);
	}

	private void mergeChangeNote(LogMapDelta<K, V> another) {
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
