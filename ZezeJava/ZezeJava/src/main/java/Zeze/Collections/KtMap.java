package Zeze.Collections;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.PersistentMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.PMap;

/**
 * pcollections {@link PMap} 的 kotlinx PersistentHashMap（CHAMP）内核实现。
 * 仅经 {@link Persistents#map()} 构建使用，替换动机是内存（pcollections HashTreePMap 的
 * IntTree 二叉树每条目摊 60-80B+，CHAMP 内核省数倍）。
 *
 * <p>契约对齐 pcollections（回归测试钉住）：
 * <ul>
 *   <li>no-op 引用恒等：put 同值/删不存在 key/批量全 no-op 返回同一实例
 *       （LogMap1.putAll 等写路径 {@code !=} 判定依赖）；ktt 批量路径以 builder 返回值做
 *       变更跟踪，全 no-op 时不提交新实例；</li>
 *   <li>equals/hashCode/entrySet/keySet/values 走 JDK Map 契约（AbstractMap 白拿），
 *       entrySet 为内核只读视图，条目不支持 setValue；</li>
 *   <li>迭代顺序与 pcollections 同为无序（编码排序由 ByteBuffer.BuildSortedString 兜底）。</li>
 * </ul>
 */
public final class KtMap<K, V> extends AbstractMap<K, V> implements PMap<K, V> {
	private final @NotNull PersistentMap<K, V> kt;

	private KtMap(@NotNull PersistentMap<K, V> kt) {
		this.kt = kt;
	}

	public static <K, V> @NotNull KtMap<K, V> empty() {
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
	public @Nullable V get(@NotNull Object key) {
		return kt.get((K)key);
	}

	@Override
	public boolean containsKey(@NotNull Object key) {
		return kt.containsKey((K)key);
	}

	@Override
	public boolean containsValue(@NotNull Object value) {
		return kt.containsValue(value);
	}

	@Override
	public @NotNull Set<Entry<K, V>> entrySet() {
		return kt.getEntries();
	}

	// ===== 持久化写 =====

	@Override
	public @NotNull PMap<K, V> plus(@NotNull K key, @NotNull V value) {
		// 同实例 put 跳过（纯优化，对 pcollections 基线不可观测）。守卫用身份不用 equals：
		// PMap2/LogMap2 是可变 bean 值、按身份判等（LogMap2.isValueChanged 用 ==），
		// 内容相等的不同 bean 实例必须真实入核（与 pcollections 逐位一致——其 plus 同值
		// 亦恒新实例，LogMap1.putAll 的 newMap != old 判定在基线上恒真）。
		if (kt.get(key) == value)
			return this;
		return new KtMap<>(kt.put(key, value));
	}

	@Override
	public @NotNull PMap<K, V> minus(@NotNull Object key) {
		var n = kt.remove((K)key);
		return n == kt ? this : new KtMap<>(n);
	}

	@Override
	public @NotNull PMap<K, V> plusAll(@NotNull Map<? extends K, ? extends V> m) {
		if (m.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : m.entrySet()) {
				// 同实例守卫（语义见 plus）：全同实例批量返回同一实例。
				if (n.get(e.getKey()) != e.getValue())
					n = n.put(e.getKey(), e.getValue());
			}
			return n == kt ? this : new KtMap<>(n);
		}
		var b = kt.builder();
		boolean changed = false;
		for (var e : m.entrySet()) {
			var old = b.put(e.getKey(), e.getValue());
			changed |= old != e.getValue(); // 身份判定（语义见 plus）
		}
		return changed ? new KtMap<>(b.build()) : this;
	}

	@Override
	public @NotNull PMap<K, V> minusAll(@NotNull Collection<?> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var k : c)
				n = n.remove((K)k);
			return n == kt ? this : new KtMap<>(n);
		}
		var b = kt.builder();
		boolean changed = false;
		for (var k : c)
			changed |= b.remove(k) != null;
		return changed ? new KtMap<>(b.build()) : this;
	}

	// ===== 可变写：不可变实现恒 UOE =====

	@Override
	public @Nullable V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public @Nullable V remove(@NotNull Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(@NotNull Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
}
