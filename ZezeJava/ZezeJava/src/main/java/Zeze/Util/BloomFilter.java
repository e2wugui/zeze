package Zeze.Util;

import org.jetbrains.annotations.NotNull;

public class BloomFilter {
	public interface BitArray {
		long getCapacity(); // maxIndex+1 如果是2的N次幂,性能会好很多

		void setBit(long index); // index:[0,capacity)

		boolean getBit(long index); // index:[0,capacity)
	}

	private final @NotNull BitArray bitArray;
	private final long capacity; // maxIndex+1
	private final long mask; // capacity-1 if isPowerOfTwo(capacity) else 0
	private final int bitsPerKey; // set/test bit count per key

	public static int toPowerOfTwo(int v) {
		return 1 << (32 - Integer.numberOfLeadingZeros(v - 1)); // 0,1,2,3,4,5,... => 1,1,2,4,4,8,...
	}

	public static long toPowerOfTwo(long v) {
		return 1L << (64 - Long.numberOfLeadingZeros(v - 1)); // 0,1,2,3,4,5,... => 1,1,2,4,4,8,...
	}

	public static boolean isPowerOfTwo(long v) {
		return (v & (v - 1)) == 0; // 0,1,2,4,8,16,...,0x8000_0000_0000_0000L => true
	}

	/**
	 * @param bitsPerKey 每个key的hash计算次数和设置/判断bit的次数. 这个次数可通过下面main方法测量出一个合适的值
	 */
	public BloomFilter(@NotNull BitArray bitArray, int bitsPerKey) {
		this.bitArray = bitArray;
		capacity = bitArray.getCapacity();
		mask = isPowerOfTwo(capacity) ? capacity - 1 : 0;
		this.bitsPerKey = bitsPerKey;
	}

	public int getBitsPerKey() {
		return bitsPerKey;
	}

	public long getTotalBits() {
		long n = 0;
		for (int i = 0; i < capacity; i++)
			if (bitArray.getBit(i))
				n++;
		return n;
	}

	/**
	 * 当前实现参考自LevelDB,如果能提供更多hash值效果会更好一点.
	 * addKey的算法要跟testKey保持一致.
	 *
	 * @param keyHash 传入的hash值越均匀越好(完整64位)
	 */
	public void addKey(long keyHash) {
		long delta = Long.rotateRight(keyHash, 17);
		if (mask != 0) { // fast-path
			for (int i = 0; i < bitsPerKey; i++) {
				bitArray.setBit(keyHash & mask);
				keyHash += delta;
			}
		} else { // slow-path
			for (int i = 0; i < bitsPerKey; i++) {
				bitArray.setBit(Long.remainderUnsigned(keyHash, capacity));
				keyHash += delta;
			}
		}
	}

	public boolean testKey(long keyHash) {
		long delta = Long.rotateRight(keyHash, 17);
		if (mask != 0) { // fast-path
			for (int i = 0; i < bitsPerKey; i++) {
				if (!bitArray.getBit(keyHash & mask))
					return false;
				keyHash += delta;
			}
		} else { // slow-path
			for (int i = 0; i < bitsPerKey; i++) {
				if (!bitArray.getBit(Long.remainderUnsigned(keyHash, capacity)))
					return false;
				keyHash += delta;
			}
		}
		return true;
	}

	// 测量工具
	public static void main(String[] args) {
		final long BYTE_SIZE = 4L * 1024; // hash空间字节容量
		final double FAULT_RATE = 0.001; // 错误率(假key判真的概率)
		for (int bitsPerKey = 1; bitsPerKey <= 16; bitsPerKey++) {
			System.out.print("bitsPerKey = " + bitsPerKey + ": "); // 几次hash
			long keys = (long)(Math.log(1 - Math.pow(FAULT_RATE, 1.0 / bitsPerKey))
					/ Math.log((double)(BYTE_SIZE * 8 - 1) / (BYTE_SIZE * 8))
					/ bitsPerKey);
			System.out.println(keys); // 计算最多能存放key的数量期望值
		}
	}
}
