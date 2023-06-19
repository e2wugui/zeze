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
	 * @param max [0, INT_MAX]
	 * @return random [0, max)
	 */
	public int nextInt(int max) {
		return max > 1 ? (int)(((next() & 0xffff_ffffL) * max) >> 32) : 0;
	}

	/**
	 * min和max的差值不能超过(INT_MAX-1)
	 *
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
		return (int)(((next() & 0xffff_ffffL) * (max - min + 1)) >> 32) + min;
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
	 * @param max [0, LONG_MAX]
	 * @return random [0, max)
	 */
	public long nextLong(long max) {
		if (max <= 1)
			return 0;
		return (max <= Integer.MAX_VALUE) ? ((next() & 0xffff_ffffL) * max) >> 32 : nextLong() % max;
	}

	/**
	 * min和max的差值不能超过(LONG_MAX-1)
	 *
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
		long delta = max - min + 1L;
		return ((delta <= Integer.MAX_VALUE) ? ((next() & 0xffff_ffffL) * delta) >> 32 : nextLong() % delta) + min;
	}

	public float nextFloat() {
		return nextInt() * (1f / (1L << 31));
	}

	public float nextFloat(int max) {
		return (float)(nextInt() * max) * (1f / (1L << 31));
	}

	public float nextFloat(float max) {
		return nextInt() * max * (1f / (1L << 31));
	}

	public double nextDouble(double max) {
		return nextInt() * max * (1.0 / (1L << 31));
	}

	public boolean nextBoolean() {
		return next() < 0;
	}

	public byte[] nextBytes(byte[] bytes, int pos, int len) {
		for (len += pos; pos < len; ) {
			for (int r = next(), n = Math.min(len - pos, 4); --n >= 0; r >>= 8)
				bytes[pos++] = (byte)r;
		}
		return bytes;
	}

	public byte[] nextBytes(byte[] bytes) {
		return nextBytes(bytes, 0, bytes.length);
	}

	public byte[] nextBytes(int size) {
		return nextBytes(new byte[size]);
	}

	/**
	 * 按权重列表随机一个索引
	 *
	 * @param weightList 权重列表，需按叠加值排序，如原始权重数组为[40,60]则需转化为[40,100]
	 * @return weightList命中索引，小于0表示没有命中项
	 */
	public int randWeights(List<Integer> weightList) {
		if (weightList == null)
			return -1;
		int size = weightList.size();
		if (size <= 0)
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

	public void randSelect(int n, int m, List<Integer> ret) {
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

	public void randSelect(IntList randList, int n) {
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
