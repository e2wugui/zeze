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

	private final SortedMap<Integer, E> circle = new SortedMap<>();
	private final HashMap<E, Integer[]> nodes = new HashMap<>();
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

			var virtual = new Integer[160];
			if (nodes.putIfAbsent(node, virtual) != null)
				return; // 忽略重复加入的node。不报告这个错误，简化外面的使用。

			nodeKey = nodeKey != null ? nodeKey + '-' : "-";
			var md5 = MessageDigest.getInstance("MD5");
			var half = virtual.length >> 2;
			for (int i = 0; i < half; ++i) {
				var hash4 = md5.digest((nodeKey + i).getBytes(StandardCharsets.UTF_8));
				for (int j = 0; j < 4; ++j) {
					virtual[i * 4 + j] = ByteBuffer.ToInt(hash4, j * 4);
				}
			}
			Arrays.sort(virtual);
			// todo 批量加入
			System.out.print(nodeKey);
			for (var hash : virtual) {
				System.out.print("," + hash);
				var conflict = circle.add(hash, node);
				if (conflict == -1) {
					System.out.println("+++++++++++++++++++++++++++++++++++++");
					logger.warn("hash conflict! key={} node={}", nodeKey, node);
				}
			}
			System.out.println();
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
				circle.remove(hash, node);
			}
		} finally {
			lock.unlock();
		}
	}

	private static byte[] toBytes(final int data) {
		return new byte[] {
				(byte)((data >> 24) & 0xff),
				(byte)((data >> 16) & 0xff),
				(byte)((data >> 8) & 0xff),
				(byte)((data >> 0) & 0xff),
		};
	}

	public E get(int hash) {
		try {
			var md5 = MessageDigest.getInstance("MD5");
			var digest = md5.digest(toBytes(hash));
			hash = 0;
			for (int i = 0; i < 4; ++i)
				hash = hash ^ ByteBuffer.ToInt(digest, i * 4);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		//var hash = Bean.hash64(String.valueOf(_hash));
		System.out.println("get="+hash);
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
