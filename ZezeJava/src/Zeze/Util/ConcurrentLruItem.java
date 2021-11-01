package Zeze.Util;

import java.util.concurrent.ConcurrentHashMap;

class ConcurrentLruItem<K, V> {
	V Value;
	ConcurrentHashMap<K, ConcurrentLruItem<K, V>> LruNode;

	ConcurrentLruItem(V value, ConcurrentHashMap<K, ConcurrentLruItem<K, V>> lruNode) {
		Value = value;
		LruNode = lruNode;
	}
}
