package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.Map;

import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** Map 事务结构增量：共享增删、保存点与合并规则，值变化语义由具体家族提供。 */
public abstract class LogMapDelta<K, V> extends LogMap<K, V> {
	/** Map1 的值按 equals 判断变化；不持有编解码元数据。 */
	static final class Map1<K, V> extends LogMapDelta<K, V> {
		Map1(PMap<K, V> source, org.pcollections.PMap<K, V> value) {
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

	/** Map2 的 Bean 值按对象身份判断变化；不持有编解码元数据。 */
	static final class Map2<K, V extends Bean> extends LogMapDelta<K, V> {
		Map2(PMap<K, V> source, org.pcollections.PMap<K, V> value) {
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

	/** 结构增量：value 非 null 为替换项，null 为删除标记，单槽位天然保持互斥。 */
	private @Nullable HashMap<K, @Nullable V> modified;

	protected LogMapDelta(PMap<K, V> source, org.pcollections.PMap<K, V> value) {
		super(source, value);
	}

	public @Nullable HashMap<K, @Nullable V> getModified() {
		return modified;
	}

	private HashMap<K, @Nullable V> modifiedForWrite() {
		return modified != null ? modified : (modified = new HashMap<>());
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
			// 仅记录值实际变化的键，避免无变更项触发监听；判定语义由各子类覆写。
			for (var e : m.entrySet()) {
				K k = e.getKey();
				noteReplacement(k, e.getValue(), old.get(k));
			}
		}
	}

	private void noteReplacement(K key, V value, @Nullable V oldValue) {
		if (isValueChanged(oldValue, value))
			modifiedForWrite().put(key, value);
	}

	public @Nullable V remove(K key) {
		V old = super.remove(key);
		if (old != null)
			modifiedForWrite().put(key, null);
		return old;
	}

	public boolean remove(K key, V value) {
		if (super.remove(key, value)) {
			modifiedForWrite().put(key, null);
			return true;
		}
		return false;
	}

	public void clear() {
		for (var ks : getValue().keySet())
			modifiedForWrite().put(ks, null);
		super.clear();
	}

	@Override
	protected void mergeFrom(LogCollection<?> child) {
		super.mergeFrom(child);
		//noinspection unchecked
		var another = (LogMapDelta<K, V>)child;
		mergeChangeNote(another);
	}

	private void mergeChangeNote(LogMapDelta<K, V> another) {
		if (another.modified != null)
			modifiedForWrite().putAll(another.modified);
	}

	protected abstract boolean isValueChanged(@Nullable V oldValue, V newValue);
}
