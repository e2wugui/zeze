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

	@Override
	public int hashCode() {
		return key.hashCode() ^ value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof KV) {
			var kv = (KV)obj;
			return key.equals(kv.key) && value.equals(kv.value);
		}
		return false;
	}
}
