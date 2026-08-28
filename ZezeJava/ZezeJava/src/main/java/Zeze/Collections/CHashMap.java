package Zeze.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.Transaction;
import Zeze.Util.TaskSpec;
import org.jetbrains.annotations.NotNull;

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
			TaskSpec.ofProcedure(module.zeze.newProcedure(() -> initSize(ii, buckets[ii]), "initSize")).call();
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

	public void clear() {
		for (var bucket : buckets)
			bucket.clear(); // 每个桶O(1)：代际号递增使旧映射失效，数据行由延迟任务分批删
		// 与put/remove一致：分片计数缓存在提交时刷新（回滚则不变，与DB一致）
		Transaction.whileCommit(() -> {
			for (var i = 0; i < buckets.length; i++)
				sizes[i] = buckets[i].size();
		});
	}

	public long walk(@NotNull TableWalkHandle<String, V> func) throws Exception {
		long count = 0L;
		var stopped = new boolean[1];
		for (var bucket : buckets) {
			if (stopped[0])
				break; // 早停：不得继续进入下一个分片
			count += bucket.walk((k, v) -> {
				var r = func.handle(k, v);
				if (!r)
					stopped[0] = true;
				return r;
			});
		}
		return func.endWalk(count);
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
