package Zeze.Util;

import java.util.Arrays;

public class ReplayAttackGrowRange extends FastLock implements ReplayAttack {
	private long max;
	private final byte[] range;
	private int position;

	@Override
	public String toString() {
		var sb = new StringBuilder();
		for (var b : range) {
			var bs = Integer.toBinaryString(b & 0xff);
			for (var i = bs.length() - 1; i >= 0; --i)
				sb.append(bs.charAt(i));
		}
		sb.append(" pos=").append(position).append(" max=").append(max);
		return sb.toString();
	}

	public ReplayAttackGrowRange() {
		this(1024);
	}

	public ReplayAttackGrowRange(int limit) {
		int capacity = 1;
		while (limit > capacity)
			capacity <<= 1;
		range = new byte[capacity];
	}

	@Override
	public boolean replay(long serialId) {
		if (serialId <= 0)
			return true; // invalid id
		long grow = serialId - max;
		if (grow > Integer.MAX_VALUE)
			return true; // 跳的太远，拒绝掉。
		int increase = (int)grow;
		if (increase > 0) { // grow clear
			if (increase >= range.length * 8) {
				// clear all
				Arrays.fill(range, (byte)0);
			} else {
				// clear bit(还可以优化）
				for (var i = 1; i < increase; ++i) {
					var pos = (this.position + i) % (range.length * 8);
					var index = pos / 8;
					var bit = 1 << (pos % 8);
					range[index] &= ~bit;
				}
			}
			position += increase;
			if (position >= range.length * 8)
				position %= range.length * 8;

			// set last bit
			{
				var index = position / 8;
				var bit = 1 << (position % 8);
				range[index] |= bit;
				max = serialId;
			}
			return false; // allow
		}
		if (increase <= -range.length * 8)
			return true; // 过期的，拒绝掉。

		var pos = this.position + increase;
		if (pos < 0) // 有范围检查，只需要加一次，否则用while
			pos += range.length * 8;

		var index = pos / 8;
		var bit = 1 << (pos % 8);
		if ((range[index] & bit) != 0)
			return true; // duplicate
		range[index] |= bit;
		return false;
	}

}
