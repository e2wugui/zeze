package Zeze.Util;

import Zeze.*;

/** 
 默认的KeyValuePair有 .net core .net framework 版本api不同的问题，自己定义一个吧。
*/

public class KV<K, V> {
	private K Key;
	public final K getKey() {
		return Key;
	}
	public final K void setKey(K value) {
		Key = value;
	}
	private V Value;
	public final V getValue() {
		return Value;
	}
	public final V void setValue(V value) {
		Value = value;
	}
}