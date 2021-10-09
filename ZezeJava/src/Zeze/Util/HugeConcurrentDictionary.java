package Zeze.Util;

import Zeze.*;
import java.util.*;

// 使用哈希到多个ConcurrentDictionary的方式支持巨大内存。
// 先不实现 IDictionary，要了再来添加。
//C# TO JAVA CONVERTER TODO TASK: The interface type was changed to the closest equivalent Java type, but the methods implemented will need adjustment:
public class HugeConcurrentDictionary<K, V> implements java.lang.Iterable<Map.Entry<K, V>> {
	private java.util.concurrent.ConcurrentHashMap<K, V>[] Buckets;
	private java.util.concurrent.ConcurrentHashMap<K, V>[] getBuckets() {
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

	public HugeConcurrentDictionary(int buckets, int concurrencyLevel, long capacity) {
		BucketCount = buckets;
		ConcurrencyLevel = concurrencyLevel;
		InitialCapacity = capacity;

		Buckets = new java.util.concurrent.ConcurrentHashMap<K, V>[buckets];
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

	public final boolean TryGetValue(K key, tangible.OutObject<V> value) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint hash = (uint)key.GetHashCode();
		int hash = (int)key.hashCode();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint i = hash % (uint)Buckets.Length;
		int i = hash % (int)getBuckets().length;
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		return getBuckets()[i].TryGetValue(key, value);
	}

	public final boolean TryAdd(K key, V value) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint hash = (uint)key.GetHashCode();
		int hash = (int)key.hashCode();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint i = hash % (uint)Buckets.Length;
		int i = hash % (int)getBuckets().length;
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		return getBuckets()[i].TryAdd(key, value);
	}

	public final V GetOrAdd(K key, tangible.Func1Param<K, V> factory) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint hash = (uint)key.GetHashCode();
		int hash = (int)key.hashCode();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint i = hash % (uint)Buckets.Length;
		int i = hash % (int)getBuckets().length;
		return getBuckets()[i].putIfAbsent(key, factory);
	}

	public final boolean TryRemove(Map.Entry<K, V> pair) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint hash = (uint)pair.Key.GetHashCode();
		int hash = (int)pair.getKey().hashCode();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint i = hash % (uint)Buckets.Length;
		int i = hash % (int)getBuckets().length;
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		return getBuckets()[i].TryRemove(pair);
	}

	public final boolean TryRemove(K key, tangible.OutObject<V> r) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint hash = (uint)key.GetHashCode();
		int hash = (int)key.hashCode();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint i = hash % (uint)Buckets.Length;
		int i = hash % (int)getBuckets().length;
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		return getBuckets()[i].TryRemove(key, r);
	}

	public final V get(K key) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint hash = (uint)key.GetHashCode();
		int hash = (int)key.hashCode();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint i = hash % (uint)Buckets.Length;
		int i = hash % (int)getBuckets().length;
		return getBuckets()[i].get(key);
	}
	public final void set(K key, V value) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint hash = (uint)key.GetHashCode();
		int hash = (int)key.hashCode();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint i = hash % (uint)Buckets.Length;
		int i = hash % (int)getBuckets().length;
		getBuckets()[i].put(key, value);
	}

	public final long getCount() {
		long count = 0;
		for (var dict : getBuckets()) {
			count += dict.size();
		}
		return count;
	}

	public final Iterator<Map.Entry<K, V>> iterator() {
		return new Enumerator(this);
	}

	public final Iterator GetEnumerator() {
		return new Enumerator(this);
	}

//C# TO JAVA CONVERTER TODO TASK: The interface type was changed to the closest equivalent Java type, but the methods implemented will need adjustment:
	private static class Enumerator implements Iterator<Map.Entry<K, V>> {
		private Iterator<Map.Entry<K, V>>[] Entrys;
		private Iterator<Map.Entry<K, V>>[] getEntrys() {
			return Entrys;
		}
		private int Index;
		private int getIndex() {
			return Index;
		}
		private void setIndex(int value) {
			Index = value;
		}

		private Map.Entry<K, V> _Current;
		public final Map.Entry<K, V> getCurrent() {
			return _Current;
		}

		private Object IEnumerator.Current -> _Current;

		public Enumerator(HugeConcurrentDictionary<K, V> huge) {
			Entrys = new Iterator<Map.Entry<K, V>>[huge.getBuckets().length];
			for (int i = 0; i < getEntrys().length; ++i) {
				getEntrys()[i] = huge.getBuckets()[i].entrySet().iterator();
			}
		}

		public final boolean MoveNext() {
			while (getIndex() < getEntrys().length) {
				var e = getEntrys()[getIndex()];
//C# TO JAVA CONVERTER TODO TASK: .NET iterators are only converted within the context of 'while' and 'for' loops:
				if (e.MoveNext()) {
//C# TO JAVA CONVERTER TODO TASK: .NET iterators are only converted within the context of 'while' and 'for' loops:
					_Current = e.Current;
					return true;
				}
				setIndex(getIndex() + 1);
			}
			return false;
		}

		public final void Reset() {
			for (var e : getEntrys()) {
				e.Reset();
			}
			setIndex(0);
		}

		public final void Dispose() {
			for (var e : getEntrys()) {
				e.Dispose();
			}
		}
	}
}