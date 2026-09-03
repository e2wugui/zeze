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
		if (limit > (1 << 30))
			throw new IllegalArgumentException("limit too large: " + limit); // 再倍增会 int 溢出为负，原实现死循环
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
		if (grow > 0) { // grow clear
			int increase = (int)grow; // grow <= Integer.MAX_VALUE，转换无损
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
			long newPosition = (long)position + increase; // int 相加溢出会回绕成负下标
			if (newPosition >= range.length * 8)
				newPosition %= range.length * 8;
			position = (int)newPosition;

			// set last bit
			{
				var index = position / 8;
				var bit = 1 << (position % 8);
				range[index] |= bit;
				max = serialId;
			}
			return false; // allow
		}
		// 过期判断必须用 long：大负 grow 经 (int) 截断会回绕成正数走到上面的前向分支放行（防重放绕过）。
		if (grow <= -(long)range.length * 8)
			return true; // 过期的，拒绝掉。
		int increase = (int)grow; // grow > -(range.length * 8)，在int范围内，转换无损

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
