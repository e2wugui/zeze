package Zeze.Util;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("ClassCanBeRecord")
public class KV<K, V> {
	// 不可变二元组（FND5-12复审两轮裁定）：key与value均参与hashCode/equals——任一字段
	// 可变都会破坏HashMap/HashSet键不变式（原地改后同桶查找失效），故两者皆final、
	// 不提供setKey/setValue。null：key在构造点拒绝（NPE变可诊断契约，使FND4-16的
	// “key不被API允许”声明成立）；value允许为null（FND4-16安全化形态保留）。
	private final @NotNull K key;
	private final @Nullable V value;

	public KV(@NotNull K key, @Nullable V value) {
		this.key = key;
		this.value = value;
	}

	public static <K, V> KV<K, V> create(@NotNull K key, @Nullable V value) {
		return new KV<>(key, value);
	}

	public final @NotNull K getKey() {
		return key;
	}

	public final @Nullable V getValue() {
		return value;
	}

	@Override
	public @NotNull String toString() {
		return "(" + key + ',' + value + ')';
	}

	@Override
	public int hashCode() {
		// value 允许为null（create(key,null)是公开API形态），null安全对齐KVList判例。
		return key.hashCode() ^ Objects.hashCode(value);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof KV<?, ?> kv) {
			return key.equals(kv.key) && Objects.equals(value, kv.value);
		}
		return false;
	}
}
