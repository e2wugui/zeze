package Zeze.Util;

public class KV<K, V> {
	private K key;
	private V value;

	public final K getKey() {
		return key;
	}

	public final void setKey(K key) {
		this.key = key;
	}

	public final V getValue() {
		return value;
	}

	public final void setValue(V value) {
		this.value = value;
	}

	public static <K, V> KV<K, V> create(K key, V value) {
		var kv = new KV<K, V>();
		kv.setKey(key);
		kv.setValue(value);
		return kv;
	}

	@Override
	public String toString() {
		return "(" + key + ',' + value + ')';
	}
}
