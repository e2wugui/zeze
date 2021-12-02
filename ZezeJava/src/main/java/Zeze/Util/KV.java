package Zeze.Util;

/** 
 默认的KeyValuePair有 .net core .net framework 版本api不同的问题，自己定义一个吧。
*/

public class KV<K, V> {
	private K Key;

	public final K getKey() {
		return Key;
	}

	public final void setKey(K value) {
		Key = value;
	}

	private V Value;
	public final V getValue() {
		return Value;
	}
	public final void setValue(V value) {
		Value = value;
	}

	public static <K, V> KV<K, V> Create(K key, V value) {
		KV<K, V> tempVar = new KV<>();
		tempVar.setKey(key);
		tempVar.setValue(value);
		return tempVar;
	}

	@Override
	public String toString() {
		return "(" + Key + "," + Value + ")";
	}
}