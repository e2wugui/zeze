package Zeze.Util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;

/**
 * 稳定(确定性)的随机算法, 用于多端同时以相同的种子获取随机值,能得到一致的结果(包括随机浮点数)
 * 需要保存64位整数状态(seed), 每次计算能得到32位的随机结果, 同一对象不是线程安全的
 */
public class StableRandom {
	private static final long MULTIPLIER = 6364136223846793005L; // 源自 Donald Knuth
	private static final long ADDEND = 1442695040888963407L; // 源自 Donald Knuth
	private static final @NotNull ThreadLocal<StableRandom> rand =
			ThreadLocal.withInitial(() -> new StableRandom(ThreadLocalRandom.current().nextLong()));

	private long seed;

	/**
	 * @return 当前线程共享的StableRandom
	 */
	public static @NotNull StableRandom local() {
		return rand.get();
	}

	/**
	 * @return 当前线程共享的StableRandom, 并重置其seed
	 */
	public static @NotNull StableRandom local(long seed) {
		return rand.get().setSeed(seed);
	}

	public StableRandom(long seed) {
		this.seed = seed;
	}

	public long getSeed() {
		return seed;
	}

	public @NotNull StableRandom setSeed(long seed) {
		this.seed = seed;
		return this;
	}

	/**
	 * @return random [INT_MIN, INT_MAX]
	 */
	public int next() {
		long s = seed * MULTIPLIER + ADDEND;
		seed = s;
		return (int)(s >> 32);
	}

	/**
	 * @param n [0, 32]
	 * @return random low n bits
	 */
	public int nextBits(int n) {
		return n > 0 ? next() >>> (32 - n) : 0;
	}

	/**
	 * @return random [0, INT_MAX]
	 */
	public int nextInt() {
		return next() & Integer.MAX_VALUE;
	}

	/**
	 * @param bound [0, INT_MAX]
	 * @return random [0, bound)
	 */
	public int nextInt(int bound) {
		return bound > 1 ? (int)(((next() & 0xffff_ffffL) * bound) >> 32) : 0;
	}

	/**
	 * @param min [INT_MIN, INT_MAX]
	 * @param max [INT_MIN, INT_MAX]
	 * @return random [min, max]
	 */
	public int nextInt(int min, int max) {
		if (min == max)
			return min;
		if (min > max) {
			int t = min;
			min = max;
			max = t;
		}
		// 闭区间元素个数可达2^32，int正跨度表示不了：跨度一律long运算。旧的int乘法在
		// 跨度>=2^31时溢出为负——[0,INT_MAX]产生区间外负数，全宽跨度为0恒返回MIN。
		// delta<=2^31-1（旧有效范围）时数值与旧算术逐位一致，种子序列不变。
		long delta = (long)max - min + 1;
		if (delta == 0x1_0000_0000L)
			return next(); // 全宽[INT_MIN,INT_MAX]：32位原始值即均匀覆盖
		return (int)(((next() & 0xffff_ffffL) * delta >> 32) + min);
	}

	/**
	 * @return random [LONG_MIN, LONG_MAX]
	 */
	public long next64() {
		return ((long)next() << 32) + next();
	}

	/**
	 * @param n [0, 64]
	 * @return random low n bits
	 */
	public long nextBits64(int n) {
		if (n < 1)
			return 0;
		return n <= 32 ? nextBits(n) & 0xffff_ffffL : next64() >>> (64 - n);
	}

	/**
	 * @return random [0, LONG_MAX]
	 */
	public long nextLong() {
		return (((long)next() << 32) + next()) & Long.MAX_VALUE;
	}

	/**
	 * @param bound [0, LONG_MAX]
	 * @return random [0, bound)
	 */
	public long nextLong(long bound) {
		if (bound <= 1)
			return 0;
		return (bound <= Integer.MAX_VALUE) ? ((next() & 0xffff_ffffL) * bound) >> 32 : nextLong() % bound;
	}

	/**
	 * @param min [LONG_MIN, LONG_MAX]
	 * @param max [LONG_MIN, LONG_MAX]
	 * @return random [min, max]
	 */
	public long nextLong(long min, long max) {
		if (min == max)
			return min;
		if (min > max) {
			long t = min;
			min = max;
			max = t;
		}
		// 闭区间元素个数可达2^64：跨度long有符号表示不了。delta<=0即无符号跨度>=2^63
		// （0为全宽）——旧的long算术此时溢出：[0,LONG_MAX]的跨度2^63成负数走进32位
		// 乘法产生区间外值，全宽跨度为0恒返回MIN。正跨度路径与旧算术一致，种子序列不变。
		long delta = max - min + 1L;
		if (delta == 0L)
			return next64(); // 全宽[LONG_MIN,LONG_MAX]：64位原始值即均匀覆盖
		if (delta > 0L)
			return (delta <= Integer.MAX_VALUE)
					? (((next() & 0xffff_ffffL) * delta) >> 32) + min
					: nextLong() % delta + min;
		// 无符号跨度(2^63,2^64)：63位的nextLong()覆盖不了，取全64位原始值做无符号取模。
		return Long.remainderUnsigned(next64(), delta) + min;
	}

	/**
	 * @return [0, 1]
	 */
	public float nextFloat() {
		return nextInt() * (1f / Integer.MAX_VALUE);
	}

	/**
	 * @return [0, max]
	 */
	public float nextFloat(int max) {
		return (float)((long)nextInt() * max) * (1f / Integer.MAX_VALUE);
	}

	/**
	 * @return [0, max]
	 */
	public float nextFloat(float max) {
		return nextInt() * max * (1f / Integer.MAX_VALUE);
	}

	/**
	 * @return [0, max]
	 */
	public double nextDouble(double max) {
		return nextLong() * max * (1.0 / Long.MAX_VALUE);
	}

	public boolean nextBoolean() {
		return next() < 0;
	}

	public byte @NotNull [] nextBytes(byte @NotNull [] bytes, int pos, int len) {
		for (len += pos; pos < len; ) {
			for (int r = next(), n = Math.min(len - pos, 4); --n >= 0; r >>= 8)
				bytes[pos++] = (byte)r;
		}
		return bytes;
	}

	public byte @NotNull [] nextBytes(byte @NotNull [] bytes) {
		return nextBytes(bytes, 0, bytes.length);
	}

	public byte @NotNull [] nextBytes(int size) {
		return nextBytes(new byte[size]);
	}

	/**
	 * 按权重列表随机一个索引
	 *
	 * @param weightList 权重列表，需按叠加值排序，如原始权重数组为[40,60]则需转化为[40,100]
	 * @return weightList命中索引，小于0表示没有命中项
	 */
	public int randWeights(@NotNull List<Integer> weightList) {
		int size = weightList.size();
		if (size == 0)
			return -1;
		int weightTotal = weightList.get(size - 1);
		if (weightTotal > 0) {
			if (size == 1)
				return 0; // 仅有一项
			int hit = nextInt(weightTotal);
			for (int i = 0; i < size; i++) {
				if (hit < weightList.get(i))
					return i;
			}
		}
		return -1;
	}

	public void randSelect(int n, int m, @NotNull List<Integer> ret) {
		if (m >= n) {
			for (int j = 0; j < n; j++)
				ret.add(j + 1);
			return;
		}
		int step = n / m;
		int mod = n % m;
		int r = 0;
		if (mod != 0)
			r = nextInt(m);
		int max = 0;
		for (int i = 0; i < m; i++) {
			int min = max + 1;
			max = min + step - 1 + ((i == r) ? mod : 0);
			ret.add(nextInt(min, max));
		}
	}

	public void randSelect(@NotNull IntList randList, int n) {
		int size = randList.size();
		if (n >= size)
			return;
		for (int i = 0; i < n; i++) {
			int r = nextInt(i, size - 1);
			int v = randList.get(r);
			if (r != i) {
				randList.set(r, randList.get(i));
				randList.set(i, v);
			}
		}
		randList.resize(n);
	}
}
