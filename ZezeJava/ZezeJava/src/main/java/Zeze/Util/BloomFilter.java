package Zeze.Util;

import org.jetbrains.annotations.NotNull;

public class BloomFilter {
	public interface BitMap {
		long getBitCount();

		void setBit(long index);

		boolean getBit(long index);
	}

	private final @NotNull BitMap bitMap;
	private final long bitCount;
	private final long mask;
	private final int bitsPerKey;

	public static long calcByteSize(long keyCount, int bitsPerKey) {
		return Math.max(Math.addExact(Math.multiplyExact(keyCount, bitsPerKey), 7) / 8, 8);
	}

	public static long toPowerOfTwo(long v) {
		return 1L << (64 - Long.numberOfLeadingZeros(v - 1)); // 0,1,2,3,4,5,... => 1,1,2,4,4,8,...
	}

	public static boolean isPowerOfTwo(long v) {
		return (v & (v - 1)) == 0; // 0,1,2,4,8,16,...,0x8000_0000_0000_0000L => true
	}

	public BloomFilter(@NotNull BitMap bitMap, int bitsPerKey) {
		this.bitMap = bitMap;
		bitCount = bitMap.getBitCount();
		mask = isPowerOfTwo(bitCount) ? Long.lowestOneBit(bitCount) - 1 : 0;
		this.bitsPerKey = Math.max((int)(bitsPerKey * Math.log(2)), 1); // ln(2) = 0.6931471805599453
	}

	public void addKey(long keyHash) {
		long delta = Long.rotateRight(keyHash, 17);
		if (mask != 0) { // fast-path
			for (int i = 0; i < bitsPerKey; i++) {
				bitMap.setBit(keyHash & mask);
				keyHash += delta;
			}
		} else { // slow-path
			for (int i = 0; i < bitsPerKey; i++) {
				bitMap.setBit(Long.remainderUnsigned(keyHash, bitCount));
				keyHash += delta;
			}
		}
	}

	public boolean testKey(long keyHash) {
		long delta = Long.rotateRight(keyHash, 17);
		if (mask != 0) { // fast-path
			for (int i = 0; i < bitsPerKey; i++) {
				if (!bitMap.getBit(keyHash & mask))
					return false;
				keyHash += delta;
			}
		} else { // slow-path
			for (int i = 0; i < bitsPerKey; i++) {
				if (!bitMap.getBit(Long.remainderUnsigned(keyHash, bitCount)))
					return false;
				keyHash += delta;
			}
		}
		return true;
	}
}
