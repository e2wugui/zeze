package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsistentHash<E> {
	private static final Logger logger = LogManager.getLogger(ConsistentHash.class);

	private final SortedMap<Long, E> circle = new SortedMap<>();
	private final HashMap<E, Long[]> nodes = new HashMap<>();
	private final Set<E> nodesView = Collections.unmodifiableSet(nodes.keySet());
	private final ReentrantLock lock = new ReentrantLock();

	public Set<E> getNodes() {
		return nodesView;
	}

	public void add(String nodeKey, E node) {
		lock.lock();
		try {
			if (null == node)
				throw new IllegalArgumentException();

			var virtual = new Long[160];
			if (nodes.putIfAbsent(node, virtual) != null)
				return; // 忽略重复加入的node。不报告这个错误，简化外面的使用。

			nodeKey = nodeKey != null ? nodeKey + '-' : "-";
			var md5 = MessageDigest.getInstance("MD5");
			var half = virtual.length >> 1;
			for (int i = 0; i < half; ++i) {
				var hash4 = md5.digest((nodeKey + i).getBytes(StandardCharsets.UTF_8));
				for (int j = 0; j < 2; ++j) {
					virtual[i * half + j] = ByteBuffer.ToLong(hash4, j * 8);
				}
			}
			Arrays.sort(virtual);
			// todo 批量加入
			for (var hash : virtual) {
				var conflict = circle.add(hash, node);
				if (conflict == -1)
					logger.warn("hash conflict! key={} node={}", nodeKey, node);
			}
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		} finally {
			lock.unlock();
		}
	}

	public void remove(E node) {
		lock.lock();
		try {
			if (node == null)
				throw new IllegalArgumentException();
			var virtual = nodes.remove(node);
			if (null == virtual)
				return;
			// todo 批量remove？
			// virtual是排序的，反向遍历，然后删除效率更高。
			for (var i = virtual.length - 1; i >= 0; --i) {
				var hash = virtual[i];
				circle.remove(hash);
			}
		} finally {
			lock.unlock();
		}
	}

	public E get(long hash) {
		lock.lock();
		try {
			// todo 换成新的SrotedMap的方法。原来是ceilingEntry，对不对。
			var e = circle.upperBound(hash);
			if (e == null) {
				e = circle.first();
				if (e == null)
					return null;
			}
			return e.getValue();
		} finally {
			lock.unlock();
		}
	}
}
