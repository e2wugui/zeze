package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConsistentHash<E> extends FastLock {
	private static final Logger logger = LogManager.getLogger(ConsistentHash.class);

	private final SortedMap<Integer, E> circle = new SortedMap<>();
	private final HashMap<E, Integer[]> nodes = new HashMap<>();
	private final @NotNull Set<E> nodesView = Collections.unmodifiableSet(nodes.keySet());

	public @NotNull Set<E> getNodes() {
		return nodesView;
	}

	public void add(@Nullable String nodeKey, @NotNull E node) {
		//noinspection ConstantValue
		if (node == null)
			throw new NullPointerException("node");
		var virtual = new Integer[160];
		try {
			nodeKey = nodeKey != null ? nodeKey + '-' : "-";
			var md5 = MessageDigest.getInstance("MD5");
			for (int i = 0, half = virtual.length / 4; i < half; ++i) {
				var hash4 = md5.digest((nodeKey + i).getBytes(StandardCharsets.UTF_8));
				for (int j = 0; j < 4; ++j)
					virtual[i * 4 + j] = ByteBuffer.ToInt(hash4, j * 4);
			}
		} catch (NoSuchAlgorithmException e) {
			Task.forceThrow(e);
		}

		List<SortedMap.Entry<Integer, E>> conflicts;
		lock();
		try {
			if (nodes.putIfAbsent(node, virtual) != null)
				return; // 忽略重复加入的node。不报告这个错误，简化外面的使用。
			conflicts = circle.addAll(virtual, node);
		} finally {
			unlock();
		}
		for (var conflict : conflicts)
			logger.warn("hash conflict! key={} node={}", conflict.key, conflict.value);
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
}
