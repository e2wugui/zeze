package Zeze.Util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

public class ConcurrentHashMapOrdered<K, V> {
	private final ConcurrentHashMap<K, V> map;
	private final ConcurrentLinkedQueue<K> queue = new ConcurrentLinkedQueue<>();

	public ConcurrentHashMapOrdered() {
		map = new ConcurrentHashMap<>();
	}

	public ConcurrentHashMapOrdered(int initialCapacity) {
		map = new ConcurrentHashMap<>(initialCapacity);
	}

	public void foreach(BiConsumer<K, V> consumer) {
		for (var it = queue.iterator(); it.hasNext(); ) {
			var k = it.next();
			var v = map.get(k);
			if (null == v) {
				it.remove();
				continue;
			}
			consumer.accept(k, v);
		}
	}

	public V put(K key, V value) {
		var origin = map.put(key, value);
		if (null == origin)
			queue.add(key); // 第一次加入。只保持第一次的顺序，重复put不加入queue。
		return origin;
	}

	public V putIfAbsent(K key, V value) {
		var origin = map.putIfAbsent(key, value);
		if (null == origin)
			queue.add(key);
		return origin;
	}

	public V get(K key) {
		return map.get(key);
	}

	public V remove(K key) {
		return map.remove(key);
	}
}
