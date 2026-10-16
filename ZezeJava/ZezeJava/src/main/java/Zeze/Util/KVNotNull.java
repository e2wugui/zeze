package Zeze.Util;

import java.util.Objects;

/** 不可变的非空键值对；允许 null 值时使用 {@link KV}。 */
public record KVNotNull<K, V>(K key, V value) {
	public KVNotNull {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(value, "value");
	}

	public static <K, V> KVNotNull<K, V> create(K key, V value) {
		return new KVNotNull<>(key, value);
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return key.hashCode() ^ value.hashCode();
	}

	@Override
	public String toString() {
		return "(" + key + ',' + value + ')';
	}
}
