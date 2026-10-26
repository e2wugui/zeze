package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/** Map 事务结构增量：共享增删、保存点与合并规则，值变化语义由具体家族提供。 */
public abstract class LogMapDelta<K, V> extends LogMap<K, V> {
	@Override
	public final boolean hasDelta() {
		return true;
	}

	private @Nullable HashMap<K, V> replaced;
	private @Nullable Set<K> removed;

	protected LogMapDelta(PMap<K, V> source, org.pcollections.PMap<K, V> value) {
		super(source, value);
	}

	public @Nullable HashMap<K, V> getReplaced() {
		return replaced;
	}

	public @Nullable Set<K> getRemoved() {
		return removed;
	}

	private HashMap<K, V> replacedForWrite() {
		return replaced != null ? replaced : (replaced = new HashMap<>());
	}

	private Set<K> removedForWrite() {
		return removed != null ? removed : (removed = new HashSet<>());
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
			// 只记值真实变化的键：值未变的键混入replaced是幻影增量，误导增量驱动的监听器。
			// 变化判定按系覆写：1系值不可变用equals；2系值可变须用身份（见LogMap2）。
			for (var e : m.entrySet()) {
				K k = e.getKey();
				noteReplacement(k, e.getValue(), old.get(k));
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
		// 空值防御对齐非托管分支（PMap1/PMap2/PSortedMap1/2 的 exist!=null && exist.equals(v)）：
		// value==null 时旧形态 NPE 炸事务、非托管分支同输入安静返回 false——同输入两分支
		// 行为必须一致（20261020-06，对抗验证定谳为理论缺陷，防御对齐修法）。
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
		var another = (LogMapDelta<K, V>)child;
		mergeChangeNote(another);
	}

	private void mergeChangeNote(LogMapDelta<K, V> another) {
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
