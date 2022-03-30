package Zeze.Util;

public class KV<K, V> {
	private K Key;
	private V Value;

	public final K getKey() {
		return Key;
	}

	public final void setKey(K value) {
		Key = value;
	}

	public final V getValue() {
		return Value;
	}

	public final void setValue(V value) {
		Value = value;
	}

	public static <K, V> KV<K, V> Create(K key, V value) {
		KV<K, V> kv = new KV<>();
		kv.setKey(key);
		kv.setValue(value);
		return kv;
	}

	@Override
	public String toString() {
		return "(" + Key + ',' + Value + ')';
	}
}
