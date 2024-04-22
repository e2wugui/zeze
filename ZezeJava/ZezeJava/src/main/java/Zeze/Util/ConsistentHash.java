package Zeze.Util;

import java.util.HashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 稳定公平的一致性hash
 * 支持并发
 *
 * @param <E> 要求实现equals和compareTo
 */
public class ConsistentHash<E extends Comparable<E>> extends FastRWLock {
	private final @NotNull SortedMap<Integer, E> circle;
	private final HashMap<E, String> nodes = new HashMap<>(); // <node,nodeKey>

	public ConsistentHash(@Nullable SortedMap.HashFunc<Integer, E> hashFunc) {
		circle = new SortedMap<>(hashFunc);
	}

	public boolean isEmpty() {
		readLock();
		try {
			return nodes.isEmpty();
		} finally {
			readUnlock();
		}
	}

	public int size() {
		readLock();
		try {
			return nodes.size();
		} finally {
			readUnlock();
		}
	}

	public int circleSize() {
		readLock();
		try {
			return circle.size();
		} finally {
			readUnlock();
		}
	}

	public int circleKeySize() {
		readLock();
		try {
			return circle.keySize();
		} finally {
			readUnlock();
		}
	}

	public @Nullable E get(long hash) {
		int hash32 = ByteBuffer.calc_hashnr(hash);
		readLock();
		try {
			var e = circle.lowerBound(hash32);
			if (e == null) {
				e = circle.first();
				if (e == null)
					return null;
			}
			return e.getValue();
		} finally {
			readUnlock();
		}
	}

	public @NotNull E @NotNull [] toArray(E @NotNull [] array) {
		readLock();
		try {
			return nodes.keySet().toArray(array);
		} finally {
			readUnlock();
		}
	}

	@Override
	public @NotNull String toString() {
		readLock();
		try {
			return circle.toString();
		} finally {
			readUnlock();
		}
	}

	// 以上是只读方法; 以下是修改方法

	public void clear() {
		writeLock();
		try {
			circle.clear();
			nodes.clear();
		} finally {
			writeUnlock();
		}
	}

	private static @NotNull Integer @NotNull [] genVirtualIds(@NotNull String nodeKey) {
		var virtual = new Integer[160];
		var r = new StableRandom(Bean.hash64(nodeKey));
		for (int i = 0, n = virtual.length; i < n; i++)
			virtual[i] = r.next();
		return virtual;
	}

	public void add(@NotNull String nodeKey, @NotNull E node) {
		//noinspection ConstantValue
		if (node == null)
			throw new NullPointerException("node");
		var virtualIds = genVirtualIds(nodeKey);

		writeLock();
		try {
			if (nodes.putIfAbsent(node, nodeKey) == null) // 忽略重复加入的node。不报告这个错误，简化外面的使用。
				circle.addAll(virtualIds, node);
		} finally {
			writeUnlock();
		}
	}

	public void remove(@NotNull E node) {
		//noinspection ConstantValue
		if (node == null)
			throw new NullPointerException("node");

		writeLock();
		try {
			var nodeKey = nodes.remove(node);
			if (nodeKey != null)
				circle.removeAll(genVirtualIds(nodeKey), node);
		} finally {
			writeUnlock();
		}
	}
}
