package Zeze.Util;

import java.util.LinkedHashMap;
import java.util.Map;

// 简单 LRU：LinkedHashMap accessOrder 模式按容量淘汰最旧条目
public class Lru<K, V> extends LinkedHashMap<K, V> {
	private final int capacity;
	public Lru(int capacity) {
		super(capacity, 0.75f, true);
		this.capacity = capacity;
	}
	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > capacity;
	}
}
