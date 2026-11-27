package Zeze.Util;

import java.util.Arrays;

/** 防重放滑窗（byte 位图版）：基于环形位图记录窗口内已见 serialId。 */
public class ReplayAttackGrowRange extends FastLock implements ReplayAttack {
	// 哨兵初值 0（配合 position=0）：serialId=0 走后向分支首次置位放行
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
		// 窗口位数 capacity*8 全程按 int 计算，限制 limit<=2^27 避免溢出
		if (limit > (1 << 27))
			throw new IllegalArgumentException("limit too large: " + limit);
		int capacity = 1;
		while (limit > capacity)
			capacity <<= 1;
		range = new byte[capacity];
	}

	@Override
	public boolean replay(long serialId) {
		if (serialId < 0)
			return true; // 非法 id（契约要求 serialId >= 0）
		long grow = serialId - max;
		if (grow > Integer.MAX_VALUE)
			return true; // 跳幅过大，拒绝
		if (grow > 0) {
			int increase = (int)grow;
			if (increase >= range.length * 8) {
				Arrays.fill(range, (byte)0);
			} else {
				// 逐位清理跳过的区间（可进一步按字节优化）
				for (var i = 1; i < increase; ++i) {
					var pos = (this.position + i) % (range.length * 8);
					var index = pos / 8;
					var bit = 1 << (pos % 8);
					range[index] = (byte)(range[index] & ~bit);
				}
			}
			long newPosition = (long)position + increase; // 防 int 相加溢出回绕为负下标
			if (newPosition >= range.length * 8L)
				newPosition %= range.length * 8L;
			position = (int)newPosition;

			{
				var index = position / 8;
				var bit = 1 << (position % 8);
				range[index] = (byte)(range[index] | bit);
				max = serialId;
			}
			return false;
		}
		// 过期判断必须按 long 比较，避免大负数截断回绕
		if (grow <= -range.length * 8L)
			return true; // 已过期，拒绝
		int increase = (int)grow;

		var pos = this.position + increase;
		if (pos < 0) // 已做范围检查，补加一次窗口大小即可
			pos += range.length * 8;

		var index = pos / 8;
		var bit = 1 << (pos % 8);
		if ((range[index] & bit) != 0)
			return true; // 重复
		range[index] = (byte)(range[index] | bit);
		return false;
	}
}
