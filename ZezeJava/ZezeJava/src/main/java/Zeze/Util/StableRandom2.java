package Zeze.Util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;

/**
 * 稳定(确定性)的随机算法, 用于多端同时以相同的种子获取随机值,能得到一致的结果(包括随机浮点数)
 * 需要保存5个64位整数状态(seed), 每次计算能得到64位的随机结果, 同一对象不是线程安全的
 * 基于lfsr258算法
 */
public class StableRandom2 {
	private static final @NotNull ThreadLocal<StableRandom2> rand =
			ThreadLocal.withInitial(() -> {
				var r = ThreadLocalRandom.current();
				return new StableRandom2(r.nextLong(), r.nextLong(), r.nextLong(), r.nextLong(), r.nextLong());
			});

	private int bits;
	private long v;
	private long s1, s2, s3, s4, s5;

	/**
	 * @return 当前线程共享的StableRandom2
	 */
	public static @NotNull StableRandom2 local() {
		return rand.get();
	}

	/**
	 * @return 当前线程共享的StableRandom2, 并重置其seed
	 */
	public static @NotNull StableRandom2 local(long s1, long s2, long s3, long s4, long s5) {
		return rand.get().setSeed(s1, s2, s3, s4, s5);
	}

	private static long ensureLarger(long v, long base) {
		while (Long.compareUnsigned(v, base) <= 0)
			v = (v * 6364136223846793005L) + 1442695040888963407L; // 常量源自 Donald Knuth
		return v;
	}

	public StableRandom2(long s1, long s2, long s3, long s4, long s5) {
		setSeed(s1, s2, s3, s4, s5);
	}

	public long getSeed(int i) {
		switch (i) {
		case 1:
			return s1;
		case 2:
			return s2;
		case 3:
			return s3;
		case 4:
			return s4;
		case 5:
			return s5;
		default:
			throw new IllegalArgumentException(String.valueOf(i));
		}
	}

	public @NotNull StableRandom2 setSeed(long s1, long s2, long s3, long s4, long s5) {
		this.s1 = ensureLarger(s1, 1);
		this.s2 = ensureLarger(s2, 0x1ff);
		this.s3 = ensureLarger(s3, 0xfff);
		this.s4 = ensureLarger(s4, 0x1ffff);
		this.s5 = ensureLarger(s5, 0x7fffff);
		return this;
	}

	/**
	 * @return random [INT_MIN, INT_MAX]
	 */
	public int next() {
		return nextBits(32);
	}

	/**
	 * @param n [0, 32]
	 * @return random low n bits
	 */
	public int nextBits(int n) {
		long v;
		int b = bits;
		if (b < n) {
			b = 64;
			v = next64();
		} else
			v = this.v;
		bits = b - n;
		this.v = v >> n;
		return (int)(v & ((1L << n) - 1));
	}

	/**
	 * @return random [0, INT_MAX]
	 */
	public int nextInt() {
		return nextBits(31);
	}

	/**
	 * @param bound [0, INT_MAX]
	 * @return random [0, bound)
	 */
	public int nextInt(int bound) {
		return bound > 1 ? (int)(((next() & 0xffff_ffffL) * bound) >> 32) : 0;
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
		long s1 = this.s1, s2 = this.s2, s3 = this.s3, s4 = this.s4, s5 = this.s5;
		//@formatter:off
		this.s1 = s1 = (((s1 <<  1) ^ s1) >>> 53) ^ ((s1 & 0xffff_ffff_ffff_fffeL) << 10);
		this.s2 = s2 = (((s2 << 24) ^ s2) >>> 50) ^ ((s2 & 0xffff_ffff_ffff_fe00L) <<  5);
		this.s3 = s3 = (((s3 <<  3) ^ s3) >>> 23) ^ ((s3 & 0xffff_ffff_ffff_f000L) << 29);
		this.s4 = s4 = (((s4 <<  5) ^ s4) >>> 24) ^ ((s4 & 0xffff_ffff_fffe_0000L) << 23);
		this.s5 = s5 = (((s5 <<  3) ^ s5) >>> 33) ^ ((s5 & 0xffff_ffff_ff80_0000L) <<  8);
		//@formatter:on
		return s1 ^ s2 ^ s3 ^ s4 ^ s5;
	}

	/**
	 * @param n [0, 64]
	 * @return random low n bits
	 */
	public long nextBits64(int n) {
		if (n >= 64)
			return next64();
		long v;
		int b = bits;
		if (b < n) {
			b = 64;
			v = next64();
		} else
			v = this.v;
		bits = b - n;
		this.v = v >> n;
		return (v & ((1L << n) - 1));
	}

	/**
	 * @return random [0, LONG_MAX]
	 */
	public long nextLong() {
		return nextBits64(63);
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
		return nextBits(1) != 0;
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
