package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.IdentityHashMap;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConsistentHash<E> extends FastLock {
	private final SortedMap<Integer, E> circle;
	private final IdentityHashMap<E, Integer[]> nodes = new IdentityHashMap<>();

	public ConsistentHash(@Nullable SortedMap.Selector<Integer, E> selector) {
		circle = new SortedMap<>(selector);
	}

	public int size() {
		lock();
		try {
			return nodes.size();
		} finally {
			unlock();
		}
	}

	public @NotNull E @NotNull [] toArray(E @NotNull [] array) {
		lock();
		try {
			return nodes.keySet().toArray(array);
		} finally {
			unlock();
		}
	}

	public void add(@NotNull String nodeKey, @NotNull E node) {
		//noinspection ConstantValue
		if (node == null)
			throw new NullPointerException("node");
		nodeKey += '-';
		var virtual = new Integer[160];
		try {
			var md5 = MessageDigest.getInstance("MD5");
			for (int i = 0, n = virtual.length; i < n; ) {
				var md5Hash = md5.digest((nodeKey + i / 4).getBytes(StandardCharsets.UTF_8));
				virtual[i++] = ByteBuffer.ToInt(md5Hash, 0);
				virtual[i++] = ByteBuffer.ToInt(md5Hash, 4);
				virtual[i++] = ByteBuffer.ToInt(md5Hash, 8);
				virtual[i++] = ByteBuffer.ToInt(md5Hash, 12);
			}
		} catch (NoSuchAlgorithmException e) {
			Task.forceThrow(e);
		}

		lock();
		try {
			if (nodes.putIfAbsent(node, virtual) == null) // 忽略重复加入的node。不报告这个错误，简化外面的使用。
				circle.addAll(virtual, node);
		} finally {
			unlock();
		}
	}

	public void remove(@NotNull E node) {
		//noinspection ConstantValue
		if (node == null)
			throw new NullPointerException("node");
		lock();
		try {
			var virtual = nodes.remove(node);
			if (null == virtual)
				return;
			// 批量remove？
			// virtual是排序的，反向遍历，然后删除效率更高。
			for (var i = virtual.length - 1; i >= 0; --i) {
				var hash = virtual[i];
				circle.remove(hash, node);
			}
		} finally {
			unlock();
		}
	}

	public @Nullable E get(int hash) {
		hash = ByteBuffer.calc_hashnr(((long)hash << 32) ^ hash);
		lock();
		try {
			// 换成新的SortedMap的方法。原来是ceilingEntry，对不对。
			var e = circle.upperBound(hash);
			if (e == null) {
				e = circle.first();
				if (e == null)
					return null;
			}
			return e.getValue();
		} finally {
			unlock();
		}
	}

	@Override
	public @NotNull String toString() {
		lock();
		try {
			return circle.toString();
		} finally {
			unlock();
		}
	}
}
