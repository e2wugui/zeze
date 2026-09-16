package UnitTest.Zeze.Util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Zeze.Util.BloomFilter;
import harness.Fast;
import org.junit.jupiter.api.Test;

/**
 * FND7-69 回归：capacity=0时isPowerOfTwo(0)==true走fast-path，mask=-1，
 * keyHash&mask传出全范围64位索引，违反BitArray的index:[0,capacity)契约。
 * 修复：构造器capacity&lt;=0抛IllegalArgumentException；正容量路径不变。
 */
@Fast
public class TestFnd769BloomFilterZeroCapacity {
	@Test
	public void testNonPositiveCapacityRejected() {
		assertThrows(IllegalArgumentException.class, () -> new BloomFilter(stubBits(0), 4),
				"0容量必须拒绝（isPowerOfTwo(0)==true会使mask=-1越契约）");
		assertThrows(IllegalArgumentException.class, () -> new BloomFilter(stubBits(-1), 4),
				"负容量必须拒绝");
	}

	@Test
	public void testPositiveCapacityUnchanged() {
		// 2的幂容量（fast-path）与非2的幂容量（slow-path）正常工作
		var powerOfTwo = assertDoesNotThrow(() -> new BloomFilter(longArrayBits(8), 2));
		powerOfTwo.addKey(0x12345678L);
		assertTrue(powerOfTwo.testKey(0x12345678L));
		var nonPowerOfTwo = assertDoesNotThrow(() -> new BloomFilter(longArrayBits(6), 2));
		nonPowerOfTwo.addKey(0x12345678L);
		assertTrue(nonPowerOfTwo.testKey(0x12345678L));
	}

	private static BloomFilter.BitArray stubBits(long capacity) {
		return new BloomFilter.BitArray() {
			@Override
			public long getCapacity() {
				return capacity;
			}

			@Override
			public void setBit(long index) {
			}

			@Override
			public boolean getBit(long index) {
				return false;
			}
		};
	}

	private static BloomFilter.BitArray longArrayBits(long capacity) {
		var bits = new long[(int)((capacity + 63) / 64)];
		return new BloomFilter.BitArray() {
			@Override
			public long getCapacity() {
				return capacity;
			}

			@Override
			public void setBit(long index) {
				bits[(int)(index >>> 6)] |= 1L << (index & 63);
			}

			@Override
			public boolean getBit(long index) {
				return (bits[(int)(index >>> 6)] & (1L << (index & 63))) != 0;
			}
		};
	}
}
