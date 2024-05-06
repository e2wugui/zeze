package Temp;

import java.math.BigInteger;
import Zeze.Util.Benchmark;

public class TestBigInt {
	public static class Int128 {
		private long high;
		private long low; // 如果有unsigned，就能用到所有bits吧。现在最高位没用。

		public void add(long num) {
			if (num < 0)
				throw new IllegalArgumentException("num < 0"); // 能直接支持减法更好。先避免掉。
			low += num;
			if (low < 0) {
				low += Long.MIN_VALUE;
				high += 1;
				if (high < 0)
					throw new IllegalStateException("overflow");
			}
		}

		@Override
		public String toString() {
			var bi = new BigInteger(String.valueOf(high));
			bi = bi.shiftLeft(63); // low 实际用了63位。
			bi = bi.add(new BigInteger(String.valueOf(low)));
			return bi.toString();
		}
	}

	public static void main(String [] args) {
		var count = 10000_0000;
		{
			var bi = new BigInteger("0");
			var b = new Benchmark();
			for (var i = 0; i < count; ++i) {
				bi = bi.add(new BigInteger("102400000000")); // bi我主要消耗在new操作和构造的解析上，主要它的longcanshu的构造没有暴露。
			}
			b.report("BigInteger sum=" + bi, count);
		}
		{
			var int128 = new Int128();
			var b = new Benchmark();
			for (var i = 0; i < count; ++i) {
				int128.add(102400000000L);
			}
			b.report("int128 sum=" + int128, count); // 这个效率杠杠的。
		}
		System.out.println(Long.MAX_VALUE);
	}
}
