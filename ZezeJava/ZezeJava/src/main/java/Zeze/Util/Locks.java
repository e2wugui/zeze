package Zeze.Util;


/**
 * 键值语义去重的锁注册表：按 segment 分片并用懒惰回收的 {@link WeakHashSet} 管理锁实例。
 */
public class Locks<T extends Lockey<T>> {
	/** 允许的最大分片数上限。 */
	private static final int MAX_SEGMENTS = 1 << 16;
	private final int segmentShift;
	private final int segmentMask;
	private final Segment<T> [] segments;

	// hash 算法和映射规则参考 ConcurrentHashMap

	/** 返回给定 lockey 对应的分片。 */
	private Segment<T> segmentFor(T lockey) {
		// Wang/Jenkins 单字哈希扩散，避免低/高位冲突聚集在同一分片
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

		// 向上取 2 的幂作为分片数
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

		public boolean contains(T key) {
			lock();
			try {
				// 需要lock，get不是线程安全的
				return locks.get(key) != null;
			} finally {
				unlock();
			}
		}

		public T get(T key) {
			lock();
			try {
				var exist = locks.get(key);
				if (exist != null)
					return exist;
				return locks.add(key.alloc());
			} finally {
				unlock();
			}
		}
	}

	public boolean contains(T lockey) {
		return segmentFor(lockey).contains(lockey);
	}

	public T get(T lockey) {
		return segmentFor(lockey).get(lockey);
	}
}
