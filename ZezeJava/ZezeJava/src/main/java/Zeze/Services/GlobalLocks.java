package Zeze.Services;

import Zeze.Net.Binary;
import Zeze.Util.FastLock;
import Zeze.Util.WeakHashSet;

/**
 * 全局锁的分段容器：通过分段弱引用表保证相同值的 key 返回同一个 {@link GlobalLockey} 实例，
 * 并在无强引用时自动回收。
 */
public final class GlobalLocks {
	/** 允许的最大分段数，用于限制构造参数上限。 */
	private static final int MAX_SEGMENTS = 1 << 16;
	private final int segmentShift;
	private final int segmentMask;
	private final Segment[] segments;

	/* ---------------- hash 算法与分段映射规则来自 ConcurrentHashMap -------------- */

	/** 返回给定 lockey 哈希对应的分段。 */
	private Segment segmentFor(GlobalLockey lockey) {
		// 使用单字 Wang/Jenkins 变体扰动哈希，避免 2 的幂次分段下高低位聚集冲突
		int h = lockey.hashCode();
		h += (h << 15) ^ 0xffffcd7d;
		h ^= (h >>> 10);
		h += (h << 3);
		h ^= (h >>> 6);
		h += (h << 2) + (h << 14);
		int hash = h ^ (h >>> 16);

		int index = (hash >>> segmentShift) & segmentMask;
		return segments[index];
	}

	public GlobalLocks() {
		this(1024);
	}

	public GlobalLocks(int concurrencyLevel) {
		if (concurrencyLevel <= 0)
			throw new IllegalArgumentException();

		if (concurrencyLevel > MAX_SEGMENTS)
			concurrencyLevel = MAX_SEGMENTS;

		// 计算不小于 concurrencyLevel 的最小 2 的幂作为分段数
		int sshift = 0;
		int ssize = 1;
		while (ssize < concurrencyLevel) {
			++sshift;
			ssize <<= 1;
		}
		segmentShift = 32 - sshift;
		segmentMask = ssize - 1;
		segments = new Segment[ssize];
		for (int i = 0; i < segments.length; ++i)
			segments[i] = new Segment();
	}

	/* ------------- 实现 --------------- */
	private static final class Segment extends FastLock {
		private final WeakHashSet<GlobalLockey> locks = new WeakHashSet<>();

		public boolean contains(GlobalLockey key) {
			lock();
			try {
				// 需要lock，get不是线程安全的
				return locks.get(key) != null;
			} finally {
				unlock();
			}
		}

		public GlobalLockey get(GlobalLockey key) {
			lock();
			try {
				GlobalLockey exist = locks.get(key);
				if (exist != null)
					return exist;
				locks.add(key);
				return key.alloc();
			} finally {
				unlock();
			}
		}
	}

	public boolean contains(GlobalLockey lockey) {
		return segmentFor(lockey).contains(lockey);
	}

	public GlobalLockey get(GlobalLockey lockey) {
		return segmentFor(lockey).get(lockey);
	}

	public GlobalLockey get(Binary tkey) {
		return get(new GlobalLockey(tkey));
	}

	public boolean contains(Binary tkey) {
		return contains(new GlobalLockey(tkey));
	}
}
