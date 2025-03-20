package Zeze.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;

public class CHashMap<V extends Bean> {
	private final LinkedMap<V>[] buckets;
	private final long[] sizes;
	private final String name;

	@SuppressWarnings("unchecked")
	CHashMap(LinkedMap.Module module, String name, Class<V> valueClass, int concurrencyLevel, int nodeSize) {
		if (concurrencyLevel < 1)
			throw new IllegalArgumentException("concurrencyLevel < 1");
		this.name = name;
		buckets = new LinkedMap[concurrencyLevel];
		sizes = new long[concurrencyLevel];
		for (var i = 0; i < buckets.length; ++i) {
			buckets[i] = module._open(name + "@" + i, valueClass, nodeSize);
			var ii = i;
			Task.call(module.zeze.newProcedure(() -> initSize(ii, buckets[ii]), "initSize"));
		}
	}

	public String getName() {
		return name;
	}

	private long initSize(int index, LinkedMap<V> bucket) {
		sizes[index] = bucket.size();
		return 0;
	}

	public V get(String key) {
		var index = Integer.remainderUnsigned(ByteBuffer.calc_hashnr(key), buckets.length);
		return buckets[index].get(key);
	}

	public V getOrAdd(String key) {
		var index = Integer.remainderUnsigned(ByteBuffer.calc_hashnr(key), buckets.length);
		var bucket = buckets[index];
		var result = bucket.getOrAdd(key);
		Transaction.whileCommit(() -> sizes[index] = bucket.size());
		return result;
	}

	public V put(String key, V value) {
		var index = Integer.remainderUnsigned(ByteBuffer.calc_hashnr(key), buckets.length);
		var bucket = buckets[index];
		var result = bucket.put(key, value);
		Transaction.whileCommit(() -> sizes[index] = bucket.size());
		return result;
	}

	public V remove(String key) {
		var index = Integer.remainderUnsigned(ByteBuffer.calc_hashnr(key), buckets.length);
		var bucket = buckets[index];
		var result = bucket.remove(key);
		Transaction.whileCommit(() -> sizes[index] = bucket.size());
		return result;
	}

	public long size() {
		// 避免锁住所有桶。
		var total = 0L;
		for (var size : sizes) {
			total += size;
		}
		return total;
	}

	public boolean isEmpty() {
		// 避免锁住所有桶。这里不直接使用size()，是为了更快退出循环。
		for (var size : sizes) {
			if (size > 0)
				return false;
		}
		return true;
	}
}
