package Zeze.Util;

import Zeze.*;

public class KV {
	public static <TK, TV> KV<TK, TV> Create(TK key, TV value) {
		KV<TK, TV> tempVar = new KV<TK, TV>();
		tempVar.setKey(key);
		tempVar.setValue(value);
		return tempVar;
	}
}