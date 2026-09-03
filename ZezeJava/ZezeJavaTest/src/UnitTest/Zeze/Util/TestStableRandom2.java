package UnitTest.Zeze.Util;

import harness.Fast;
import Zeze.Util.StableRandom2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * StableRandom2 的确定性承诺测试（FND-U2-4）。
 * <p>
 * 类注释承诺"多端同时以相同的种子获取随机值，能得到一致的结果"，local(s1..s5) 即重置线程共享
 * 实例种子的官方入口。但 nextBits/nextBits64 缓存的剩余位（bits/v）不随 setSeed 清空：
 * 复用已用过的实例重设种子后，首个 ≤ 缓存位数的取值消费的是旧种子流的位——同种子不同结果。
 * 修复后：setSeed 清空位缓存，重置后输出完全由新种子决定。
 */
@Fast
public class TestStableRandom2 {

	private static final long S1 = 100, S2 = 101, S3 = 102, S4 = 103, S5 = 104;

	/**
	 * finding 场景：线程局部实例先消耗部分流（缓存剩 32 位），再 setSeed 重置为与全新实例相同的种子。
	 * 修复前 reused.nextInt() 消费旧缓存（283592964），fresh 为 1608752282，分叉。
	 */
	@Test
	public void testSetSeedResetsBitCache() {
		var fresh = new StableRandom2(S1, S2, S3, S4, S5);
		var reused = new StableRandom2(1, 2, 3, 4, 5);
		reused.next(); // 消耗 nextBits(32)：缓存剩 32 位（旧种子流）
		reused.setSeed(S1, S2, S3, S4, S5);
		Assertions.assertEquals(fresh.nextInt(), reused.nextInt()); // 修复前：不等
		for (int i = 0; i < 100; i++) {
			Assertions.assertEquals(fresh.nextInt(), reused.nextInt());
			Assertions.assertEquals(fresh.next(), reused.next());
			Assertions.assertEquals(fresh.nextLong(), reused.nextLong());
			Assertions.assertEquals(fresh.nextBoolean(), reused.nextBoolean());
			Assertions.assertEquals(fresh.nextBits(5), reused.nextBits(5));
			Assertions.assertEquals(fresh.nextBits64(17), reused.nextBits64(17));
		}
	}

	/**
	 * 多种消耗形态后重置：nextBits64（63 位缓存）、nextBoolean（1 位）、边界 nextBits(32)。
	 */
	@Test
	public void testSetSeedAfterVariousUsage() {
		for (int usage = 0; usage < 6; usage++) {
			var fresh = new StableRandom2(S1, S2, S3, S4, S5);
			var reused = new StableRandom2(9, 8, 7, 6, 5);
			switch (usage) {
				case 0 -> reused.nextLong(); // nextBits64(63)：缓存剩 1 位
				case 1 -> reused.nextBoolean(); // nextBits(1)：缓存剩 63 位
				case 2 -> { reused.next(); reused.next(); } // 缓存耗尽后再取一个（剩 32 位）
				case 3 -> reused.nextInt(12345); // 组合取值
				case 4 -> reused.nextBytes(16); // 字节填充路径
				case 5 -> { reused.nextFloat(); reused.nextDouble(3.0); } // 浮点路径
			}
			reused.setSeed(S1, S2, S3, S4, S5);
			Assertions.assertEquals(fresh.next(), reused.next(), "usage#" + usage);
			Assertions.assertEquals(fresh.nextInt(), reused.nextInt(), "usage#" + usage);
			Assertions.assertEquals(fresh.nextLong(), reused.nextLong(), "usage#" + usage);
			Assertions.assertEquals(fresh.getSeed(1), reused.getSeed(1));
		}
	}

	/**
	 * 相同种子的两个全新实例输出一致（承诺本身，回归）。
	 */
	@Test
	public void testSameSeedDeterministic() {
		var a = new StableRandom2(S1, S2, S3, S4, S5);
		var b = new StableRandom2(S1, S2, S3, S4, S5);
		for (int i = 0; i < 1000; i++)
			Assertions.assertEquals(a.nextLong(), b.nextLong());
	}

	/**
	 * nextBits(0) 边界：清缓存后取 0 位不污染后续输出。
	 */
	@Test
	public void testNextBitsZeroAfterSetSeed() {
		var fresh = new StableRandom2(S1, S2, S3, S4, S5);
		var reused = new StableRandom2(0, 0, 0, 0, 0);
		reused.next();
		reused.setSeed(S1, S2, S3, S4, S5);
		Assertions.assertEquals(0, reused.nextBits(0));
		Assertions.assertEquals(fresh.next(), reused.next());
	}
}
