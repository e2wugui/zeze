package Zeze.Util;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// 使用哈希到多个ConcurrentDictionary的方式支持巨大内存。
// 先不实现 IDictionary，要了再来添加。
public class HugeConcurrentDictionary<K, V> implements java.lang.Iterable<Map.Entry<K, V>> {
	private ConcurrentHashMap<K, V>[] Buckets;
	private ConcurrentHashMap<K, V>[] getBuckets() {
		return Buckets;
	}

	private int BucketCount;
	public final int getBucketCount() {
		return BucketCount;
	}
	private int ConcurrencyLevel;
	public final int getConcurrencyLevel() {
		return ConcurrencyLevel;
	}
	private long InitialCapacity;
	public final long getInitialCapacity() {
		return InitialCapacity;
	}

	@SuppressWarnings("unchecked")
	public HugeConcurrentDictionary(int buckets, int concurrencyLevel, long capacity) {
		BucketCount = buckets;
		ConcurrencyLevel = concurrencyLevel;
		InitialCapacity = capacity;

		Buckets = (ConcurrentHashMap<K, V>[])new ConcurrentHashMap[buckets];
		long bucketsCapacity = capacity / getBuckets().length;
		if (bucketsCapacity > Integer.MAX_VALUE) {
			throw new RuntimeException("capacity / buckets > int.MaxValue. Please Increace buckets.");
		}
		for (int i = 0; i < getBuckets().length; ++i) {
			getBuckets()[i] = new java.util.concurrent.ConcurrentHashMap<K, V>(concurrencyLevel, (int)bucketsCapacity);
		}
	}

	public final void Clear() {
		for (var b : getBuckets()) {
			b.clear();
		}
	}

	private int hashIndex(K key) {
		return Integer.remainderUnsigned(key.hashCode(), getBuckets().length);
	}

	public final V get(K key) {
		return getBuckets()[hashIndex(key)].get(key);
	}

	public final V putIfAbsent(K key, V value) {
		return getBuckets()[hashIndex(key)].putIfAbsent(key, value);
	}

	public final V GetOrAdd(K key, Function<? super K, ? extends V> factory) {
		return getBuckets()[hashIndex(key)].computeIfAbsent(key, factory);
	}

	public final boolean remove(K key, V value) {
		return getBuckets()[hashIndex(key)].remove(key, value);
	}

	public final V remove(K key) {
		return getBuckets()[hashIndex(key)].remove(key);
	}


	public final V put(K key, V value) {
		return getBuckets()[hashIndex(key)].put(key, value);
	}

	public final long size() {
		long count = 0;
		for (var dict : getBuckets()) {
			count += dict.size();
		}
		return count;
	}

	public final Iterator<Map.Entry<K, V>> iterator() {
		return new Enumerator();
	}

	private class Enumerator implements Iterator<Map.Entry<K, V>> {
		private Iterator<Map.Entry<K, V>>[] Entrys;
		private int Index;

		@SuppressWarnings("unchecked")
		public Enumerator() {
			Entrys = (Iterator<Map.Entry<K, V>>[])new Iterator[getBuckets().length];
			for (int i = 0; i < Entrys.length; ++i) {
				Entrys[i] = getBuckets()[i].entrySet().iterator();
			}
		}

		@Override
		public boolean hasNext() {
			while (Index < Entrys.length) {
				var e = Entrys[Index];
				if (e.hasNext())
					return true;
				++Index;
			}
			return false;
		}

		@Override
		public Entry<K, V> next() {
			return Entrys[Index].next();
		}
	}
}