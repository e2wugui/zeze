package Zeze.Util;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/** 不可变二元组：key 非空，value 可为 null，两者均参与 hashCode/equals。 */
@SuppressWarnings("ClassCanBeRecord")
public class KV<K, V extends @Nullable Object> {
	private final K key;
	private final V value;

	public KV(K key, V value) {
		this.key = Objects.requireNonNull(key, "key");
		this.value = value;
	}

	public static <K, V extends @Nullable Object> KV<K, V> create(K key, V value) {
		return new KV<>(key, value);
	}

	public final K getKey() {
		return key;
	}

	public final V getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "(" + key + ',' + value + ')';
	}

	@Override
	public int hashCode() {
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
