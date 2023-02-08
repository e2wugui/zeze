package Zeze.Util;

import java.math.BigInteger;

public class ReplayAttackGrowRange implements ReplayAttack {
	private long maxReceiveSerialId;
	private final byte[] replayAttack;
	private int maxBitPosition;

	@Override
	public String toString() {
		var sb = new StringBuilder();
		for (var b : replayAttack) {
			var bs = Integer.toBinaryString(b & 0xff);
			for (var i = bs.length() - 1; i >= 0; --i)
				sb.append(bs.charAt(i));
		}
		sb.append(" pos=").append(maxBitPosition).append(" max=").append(maxReceiveSerialId);
		return sb.toString();
	}

	public ReplayAttackGrowRange() {
		this(1024);
	}

	public ReplayAttackGrowRange(int limit) {
		int capacity = 1;
		while (limit > capacity)
			capacity <<= 1;
		replayAttack = new byte[capacity];
	}

	@Override
	public boolean replay(long serialId) {
		long grow = serialId - maxReceiveSerialId;
		if (grow > replayAttack.length * 8L)
			return true; // 跳的太远，拒绝掉。

		int increase = (int)grow;
		if (increase > 0) { // grow clear
			for (var i = 1; i < increase; ++i) {
				// clear bit
				var pos = (maxBitPosition + i) % (replayAttack.length * 8);
				var index = pos / 8;
				var bit = 1 << (pos % 8);
				replayAttack[index] &= ~bit;
			}
			maxBitPosition += increase;
			if (maxBitPosition >= replayAttack.length * 8)
				maxBitPosition %= replayAttack.length * 8;

			// set last bit
			{
				var index = maxBitPosition / 8;
				var bit = 1 << (maxBitPosition % 8);
				replayAttack[index] |= bit;
				maxReceiveSerialId = serialId;
			}
			return false; // allow
		}
		if (increase <= -replayAttack.length)
			return true; // 过期的，拒绝掉。

		var pos = maxBitPosition + increase;
		if (pos < 0) // 有范围检查，只需要加一次，否则用while
			pos += replayAttack.length;

		var index = pos / 8;
		var bit = 1 << (pos % 8);
		if ((replayAttack[index] & bit) != 0)
			return true; // duplicate
		replayAttack[index] |= bit;
		return false;
	}

}
