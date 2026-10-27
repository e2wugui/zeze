package Zeze.Collections;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.pcollections.PMap;

import Zeze.Transaction.Collections.PersistentHashMap;

/**
 * pcollections {@link PMap} 的自移植 PersistentHashMap（CHAMP）内核实现（myktt 档）。
 * 内核为仓库内从 kotlinx.collections.immutable 0.5.2 私有移植的
 * {@link PersistentHashMap}，适配层与 {@link KtMap} 同构：同一契约两副桥接——KtMap 桥接
 * 外部 kotlinx 依赖，本类桥接自移植内核，使 myktt 档零 kotlinx 依赖可运行/对照。
 * 仅经 {@link Persistents#map()} 构建使用。
 *
 * <p>契约与 {@link KtMap} 逐位一致（回归测试同一套钉住）：
 * <ul>
 *   <li>no-op 引用恒等：put 同值/删不存在 key/批量全 no-op 返回同一实例
 *       （LogMap1.putAll 等写路径 {@code !=} 判定依赖）；内核 put 亦按身份跳过同实例值
 *       （相等异实例真实入核），builder 批量路径以返回值做变更跟踪，全 no-op 时不提交新实例；</li>
 *   <li>equals/hashCode/entrySet/keySet/values 走 JDK Map 契约（AbstractMap 白拿），
 *       entrySet 为内核只读视图，条目不支持 setValue；</li>
 *   <li>迭代顺序与 pcollections 同为无序（编码排序由 ByteBuffer.BuildSortedString 兜底）。</li>
 * </ul>
 */
public final class MyKtMap<K, V> extends AbstractMap<K, V> implements PMap<K, V> {
	private final PersistentHashMap<K, V> kt;

	private MyKtMap(PersistentHashMap<K, V> kt) {
		this.kt = kt;
	}

	public static <K, V> MyKtMap<K, V> empty() {
		return new MyKtMap<>(PersistentHashMap.empty());
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
		return kt.entrySet();
	}

	// ===== 持久化写 =====

	@Override
	public PMap<K, V> plus(K key, V value) {
		// 同实例 put 跳过（纯优化，内核 Node.put 亦有身份跳过）。守卫用身份不用 equals
		// （语义详见 KtMap.plus）：PMap2/LogMap2 是可变 bean 值、按身份判等，内容相等的
		// 不同 bean 实例必须真实入核。
		if (kt.get(key) == value)
			return this;
		return new MyKtMap<>(kt.putting(key, value));
	}

	@Override
	public PMap<K, V> minus(Object key) {
		var n = kt.removing(key);
		return n == kt ? this : new MyKtMap<>(n);
	}

	@Override
	public PMap<K, V> plusAll(Map<? extends K, ? extends V> m) {
		if (m.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : m.entrySet()) {
				// 同实例守卫（语义见 plus）：全同实例批量返回同一实例。
				if (n.get(e.getKey()) != e.getValue())
					n = n.putting(e.getKey(), e.getValue());
			}
			return n == kt ? this : new MyKtMap<>(n);
		}
		var b = kt.builder();
		boolean changed = false;
		for (var e : m.entrySet()) {
			var old = b.put(e.getKey(), e.getValue());
			changed |= old != e.getValue(); // 身份判定（语义见 plus）
		}
		return changed ? new MyKtMap<>(b.build()) : this;
	}

	@Override
	public PMap<K, V> minusAll(Collection<?> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var k : c)
				n = n.removing(k);
			return n == kt ? this : new MyKtMap<>(n);
		}
		var b = kt.builder();
		boolean changed = false;
		for (var k : c)
			changed |= b.remove(k) != null;
		return changed ? new MyKtMap<>(b.build()) : this;
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
