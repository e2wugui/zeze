package Zeze.Transaction;

/**
 * <p>
 * Locks原来使用 单个容器管理锁，效率太低：
 * <p>
 * 1. 每次查询都会试图去回收; 以前java版实现一个懒惰的WeakHashSet。c# ConditionalWeakTable 使用 this==another 吧，没有调用 Equals，不能使用。
 * 2. 并发访问效率低. 通过增加segment解决。
 */
public final class Locks {
	/**
	 * The maximum number of segments to allow; used to bound constructor arguments.
	 */
	private static final int MAX_SEGMENTS = 1 << 16; // slightly conservative
	private final int segmentShift;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private readonly uint segmentMask;
	private final int segmentMask;
	private final Segment[] segments;

	/* ---------------- hash算法和映射规则都是来自 ConcurrentHashMap. -------------- */
	/**
	 * Returns the segment that should be used for key with given hash.
	 * 
	 * @param lockey the Lockey
	 * @return the segment
	 */
	private Segment segmentFor(Lockey lockey) {
		/**
		 * Applies a supplemental hash function to a given hashCode, which defends
		 * against poor quality hash functions. This is critical because
		 * ConcurrentHashMap uses power-of-two length hash tables, that otherwise
		 * encounter collisions for hashCodes that do not differ in lower or upper bits.
		 */
		// Spread bits to regularize both segment and index locations,
		// using variant of single-word Wang/Jenkins hash.
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint h = (uint)lockey.GetHashCode();
		int h = (int)lockey.hashCode();
		h += (h << 15) ^ 0xffffcd7d;
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
		h ^= (h >>> 10);
		h += (h << 3);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
		h ^= (h >>> 6);
		h += (h << 2) + (h << 14);
//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint hash = h ^ (h >> 16);
		int hash = h ^ (h >>> 16);

//C# TO JAVA CONVERTER WARNING: The right shift operator was replaced by Java's logical right shift operator since the left operand was originally of an unsigned type, but you should confirm this replacement:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: uint index = (hash >> segmentShift) & segmentMask;
		int index = (hash >>> segmentShift) & segmentMask;
		return segments[index];
	}

	public Locks() {
		this(1024);
	}

	public Locks(int concurrencyLevel) {
		if (concurrencyLevel <= 0) {
			throw new IllegalArgumentException();
		}

		if (concurrencyLevel > MAX_SEGMENTS) {
			concurrencyLevel = MAX_SEGMENTS;
		}

		// Find power-of-two sizes best matching arguments
		int sshift = 0;
		int ssize = 1;
		while (ssize < concurrencyLevel) {
			++sshift;
			ssize <<= 1;
		}
		this.segmentShift = 32 - sshift;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: this.segmentMask = (uint)(ssize - 1);
		this.segmentMask = (int)(ssize - 1);
		this.segments = new Segment[ssize];
		for (int i = 0; i < this.segments.length; ++i) {
			this.segments[i] = new Segment();
		}
	}

	/* ------------- 实现 --------------- */
	private final static class Segment {
		private final Zeze.Util.WeakHashSet<Lockey> locks = new Zeze.Util.WeakHashSet<Lockey>();

		public Segment() {
		}

		public boolean Contains(Lockey key) {
			// 需要sync，get不是线程安全的
			synchronized (this) {
				return locks.get(key) != null;
			}
		}

		public Lockey Get(Lockey key) {
			synchronized (this) {
				Lockey exist = locks.get(key);
				if (null != exist) {
					return exist;
				}

				locks.add(key);
				return key.Alloc();
			}
		}
	}

	public boolean Contains(Lockey lockey) {
		return this.segmentFor(lockey).Contains(lockey);
	}

	public Lockey Get(Lockey lockey) {
		return this.segmentFor(lockey).Get(lockey);
	}

	public Lockey Get(TableKey tkey) {
		return Get(new Lockey(tkey));
	}

	public boolean Contains(TableKey tkey) {
		return Contains(new Lockey(tkey));
	}
}