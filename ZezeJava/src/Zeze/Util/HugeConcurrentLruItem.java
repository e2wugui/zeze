package Zeze.Util;

class HugeConcurrentLruItem<K, V> {
	V Value;
	HugeConcurrentDictionary<K, HugeConcurrentLruItem<K, V>> LruNode;

	HugeConcurrentLruItem(V value, HugeConcurrentDictionary<K, HugeConcurrentLruItem<K, V>> lruNode) {
		Value = value;
		LruNode = lruNode;
	}
}
