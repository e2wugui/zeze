package Zeze.Util;

import org.jetbrains.annotations.NotNull;

/**
 * 键值语义去重的锁注册表：
 * <p>
 * 1. 用懒惰回收的 WeakHashSet 管理锁实例（C# ConditionalWeakTable 按 this==another 比较、不调用 Equals，不能使用）。
 * 2. segment 分片提高并发访问效率。
 */
public class Locks<T extends Lockey<T>> {
	/**
	 * The maximum number of segments to allow; used to bound constructor arguments.
	 */
	private static final int MAX_SEGMENTS = 1 << 16; // slightly conservative
	private final int segmentShift;
	private final int segmentMask;
	private final @NotNull Segment<T> @NotNull [] segments;

	/* ---------------- hash算法和映射规则都是来自 ConcurrentHashMap. -------------- */

	/**
	 * Returns the segment that should be used for key with given hash.
	 */
	private @NotNull Segment<T> segmentFor(T lockey) {
		/*
		 * Applies a supplemental hash function to a given hashCode, which defends
		 * against poor quality hash functions. This is critical because
		 * ConcurrentHashMap uses power-of-two length hash tables, that otherwise
		 * encounter collisions for hashCodes that do not differ in lower or upper bits.
		 */
		// Spread bits to regularize both segment and index locations,
		// using variant of single-word Wang/Jenkins hash.
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

	public Locks() {
		this(1024);
	}

	@SuppressWarnings("unchecked")
	public Locks(int concurrencyLevel) {
		if (concurrencyLevel <= 0)
			throw new IllegalArgumentException();

		if (concurrencyLevel > MAX_SEGMENTS)
			concurrencyLevel = MAX_SEGMENTS;

		// Find power-of-two sizes best matching arguments
		int sShift = 0;
		int sSize = 1;
		while (sSize < concurrencyLevel) {
			++sShift;
			sSize <<= 1;
		}
		segmentShift = 32 - sShift;
		segmentMask = sSize - 1;
		segments = new Segment[sSize];
		for (int i = 0; i < segments.length; ++i)
			segments[i] = new Segment<>();
	}

	/* ------------- 实现 --------------- */
	private static final class Segment<T extends Lockey<T>> extends FastLock {
		private final WeakHashSet<T> locks = new WeakHashSet<>();

		public boolean contains(@NotNull T key) {
			lock();
			try {
				// 需要lock，get不是线程安全的
				return locks.get(key) != null;
			} finally {
				unlock();
			}
		}

		public @NotNull T get(@NotNull T key) {
			lock();
			try {
				var exist = locks.get(key);
				if (exist != null)
					return exist;
				locks.add(key);
				return key.alloc();
			} finally {
				unlock();
			}
		}
	}

	public boolean contains(@NotNull T lockey) {
		return segmentFor(lockey).contains(lockey);
	}

	public @NotNull T get(@NotNull T lockey) {
		return segmentFor(lockey).get(lockey);
	}
}
