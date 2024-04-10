package Zeze.Util;

import java.util.Arrays;

public class ReplayAttackGrowRange2 extends FastLock implements ReplayAttack {
	private static final int BITS_SHIFT = 6; // 1 << 6 = 64 (bit count for long)
	private static final int BITS_MASK = (1 << BITS_SHIFT) - 1; // 63

	private final long[] bits;
	private final int indexMask;
	private final int windowSize;
	private long maxSerialId = -1;

	public ReplayAttackGrowRange2() {
		this(1024 - BITS_MASK);
	}

	public ReplayAttackGrowRange2(int windowSize) {
		if (windowSize <= 0)
			throw new IllegalArgumentException("windowSize <= 0: " + windowSize);
		int capacity = 1 << (32 - (Integer.numberOfLeadingZeros((windowSize + BITS_MASK - 1) >> BITS_SHIFT))); // 1=>1; 2~65=>2; ...
		bits = new long[capacity];
		indexMask = capacity - 1;
		this.windowSize = windowSize;
	}

	@Override
	public boolean replay(long serialId) {
		if (serialId < 0)
			return true; // invalid
		long delta = maxSerialId - serialId;
		if (delta < 0) { // forward
			long i = maxSerialId >> BITS_SHIFT, j = serialId >> BITS_SHIFT;
			if (i != j) {
				if (j - i > indexMask)
					Arrays.fill(bits, 0); // 向前跳得太远,就全部清掉
				else {
					do
						bits[((int)++i & indexMask)] = 0;
					while (i < j);
				}
			}
			bits[((int)j & indexMask)] |= 1L << ((int)serialId & BITS_MASK);
			maxSerialId = serialId;
			return false;
		}
		if (delta >= windowSize)
			return true; // too old
		int serialIdInt = (int)serialId;
		int index = (serialIdInt >> BITS_SHIFT) & indexMask;
		long bit = 1L << (serialIdInt & BITS_MASK);
		if ((bits[index] & bit) != 0)
			return true; // replay
		bits[index] |= bit;
		return false;
	}

	@Override
	public String toString() {
		int n = maxSerialId < windowSize ? (int)(maxSerialId + 1) : windowSize;
		var sb = new StringBuilder(n + 40);
		for (int i = (int)maxSerialId - n + 1; n > 0; i++, n--)
			sb.append((int)(bits[(i >> BITS_SHIFT) & indexMask] >> (i & BITS_MASK)) & 1);
		return sb.append(" max=").append(maxSerialId)
				.append(" win=").append(windowSize)
				.append(" bits=[").append(bits.length).append(']')
				.toString();
	}
}
