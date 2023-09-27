package Zeze.Util;

import org.jetbrains.annotations.NotNull;

public class BloomFilter {
	public interface BitArray {
		long getCapacity(); // maxIndex+1

		void setBit(long index); // index:[0,capacity)

		boolean getBit(long index); // index:[0,capacity)
	}

	private final @NotNull BloomFilter.BitArray bitArray;
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

	public BloomFilter(@NotNull BloomFilter.BitArray bitArray, int maxKeyCount) {
		this.bitArray = bitArray;
		capacity = bitArray.getCapacity();
		mask = isPowerOfTwo(capacity) ? capacity - 1 : 0;
		this.bitsPerKey = Math.max((int)(capacity * Math.log(2) / maxKeyCount), 1); // ln(2) = 0.6931471805599453
	}

	public int getBitsPerKey() {
		return bitsPerKey;
	}

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
}
