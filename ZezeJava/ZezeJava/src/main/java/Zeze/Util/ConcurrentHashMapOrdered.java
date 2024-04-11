package Zeze.Util;

import java.util.Iterator;
import java.util.NoSuchElementException;
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

	public class OrderedIterator implements Iterator<V> {
		private final Iterator<K> queueIt;
		private transient K key;
		private transient V value;

		public OrderedIterator(Iterator<K> queueIt) {
			this.queueIt = queueIt;
		}

		public K key() {
			return key;
		}

		@Override
		public boolean hasNext() {
			while (null == value) {
				var has = queueIt.hasNext();
				if (!has)
					return false;
				key = queueIt.next();
				value = map.get(key);
				if (null == value)
					queueIt.remove();
			}
			return true;
		}

		@Override
		public V next() {
			if (null == value && !hasNext())
				throw new NoSuchElementException();

			var next = value;
			value = null;
			return next;
		}
	}

	public OrderedIterator iterator() {
		return new OrderedIterator(queue.iterator());
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

	public void dumpMap() {
		System.out.println(map);
	}
}
