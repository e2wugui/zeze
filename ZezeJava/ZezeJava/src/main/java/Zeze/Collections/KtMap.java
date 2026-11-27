package Zeze.Collections;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.PersistentMap;

import org.jspecify.annotations.Nullable;
import org.pcollections.PMap;

/**
 * 基于 kotlinx PersistentHashMap（CHAMP）的 {@link PMap} 实现，经 {@link Persistents#map()} 构建。
 * 写路径保证无变化操作返回同一实例（支持写日志 {@code !=} 变更检测），迭代无序，entrySet 只读。
 */
public final class KtMap<K, V extends @Nullable Object> extends AbstractMap<K, V> implements PMap<K, V> {
	private final PersistentMap<K, V> kt;

	private KtMap(PersistentMap<K, V> kt) {
		this.kt = kt;
	}

	public static <K, V extends @Nullable Object> KtMap<K, V> empty() {
		return new KtMap<>(ExtensionsKt.<K, V>persistentHashMapOf());
	}

	// ===== 读 =====

	@Override
	public int size() {
		return kt.size();
	}

	@Override
	public boolean isEmpty() {
		return kt.isEmpty();
	}

	@Override
	public @Nullable V get(Object key) {
		return kt.get(key);
	}

	@Override
	public boolean containsKey(Object key) {
		return kt.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return kt.containsValue(value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return kt.getEntries();
	}

	// ===== 持久化写 =====

	@Override
	public PMap<K, V> plus(K key, V value) {
		// 按引用身份判等（不用 equals）：PMap2/LogMap2 依赖可变 bean 实例身份追踪变更，不同实例须入核。
		if (kt.get(key) == value && (value != null || kt.containsKey(key)))
			return this;
		return new KtMap<>(kt.putting(key, value));
	}

	@Override
	public PMap<K, V> minus(Object key) {
		var n = kt.removing((K)key);
		return n == kt ? this : new KtMap<>(n);
	}

	@Override
	public PMap<K, V> plusAll(Map<? extends K, ? extends V> m) {
		if (m.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : m.entrySet()) {
				// 同实例守卫（语义见 plus）：全同实例批量返回同一实例。
				if (n.get(e.getKey()) != e.getValue() || e.getValue() == null && !n.containsKey(e.getKey()))
					n = n.putting(e.getKey(), e.getValue());
			}
			return n == kt ? this : new KtMap<>(n);
		}
		var b = kt.builder();
		boolean changed = false;
		for (var e : m.entrySet()) {
			int oldSize = b.size();
			var old = b.put(e.getKey(), e.getValue());
			changed |= old != e.getValue() || b.size() != oldSize; // 身份或映射存在性发生变化
		}
		return changed ? new KtMap<>(b.build()) : this;
	}

	@Override
	public PMap<K, V> minusAll(Collection<?> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var k : c)
				n = n.removing((K)k);
			return n == kt ? this : new KtMap<>(n);
		}
		var b = kt.builder();
		boolean changed = false;
		for (var k : c) {
			int oldSize = b.size();
			b.remove(k);
			changed |= b.size() != oldSize;
		}
		return changed ? new KtMap<>(b.build()) : this;
	}

	// ===== 可变写：不可变实现恒 UOE =====

	@Override
	public @Nullable V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public @Nullable V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
}
