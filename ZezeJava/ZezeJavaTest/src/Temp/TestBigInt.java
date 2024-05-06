package Temp;

import java.math.BigInteger;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Benchmark;
import org.jetbrains.annotations.NotNull;

public class TestBigInt {
	public static class Id128 implements Comparable<Id128>, Serializable {
		private long high; // unsigned
		private long low; // unsigned

		// 非常快，但是low只用了63位，为了对接gcc的__int128，废弃。
		/*
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
		*/

		public Id128() {

		}

		/**
		 *
		 * @param high high is unsigned
		 * @param low low is unsigned
		 */
		public Id128(long high, long low) {
			this.high = high;
			this.low = low;
		}

		/**
		 * 增加id的值。直接改变现有变量。
		 * @param num num is unsigned
		 */
		public void increment(long num) {
			high += unsignedCarry(low, num);
			low += num;
		}

		public static long unsignedCarry(long a, long b) {
			// HD 2-13
			return ((a >>> 1) + (b >>> 1) + ((a & b) & 1)) >>> 63;
		}

		/**
		 * 增加id的值，返回一个新对象。
		 * @param num num is unsigned
		 * @return new Id128 instance that added.
		 */
		public Id128 add(long num) {
			var result = new Id128(high, low);
			result.increment(num);
			return result;
		}

		@Override
		public String toString() {
			var bi = new BigInteger(Long.toUnsignedString(high));
			bi = bi.shiftLeft(64);
			bi = bi.add(new BigInteger(Long.toUnsignedString(low)));
			return bi.toString();// + " " + Long.toUnsignedString(high) + ", " + Long.toUnsignedString(low);
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteLong8(low);
			bb.WriteLong8(high);
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			low = bb.ReadLong8();
			high = bb.ReadLong8();
		}

		@Override
		public int compareTo(@NotNull Id128 o) {
			var c = Long.compare(high, o.high);
			if (c != 0)
				return c;
			return Long.compare(low, o.low);
		}
	}

	public static void main(String [] args) {
		var count = 10000_0000;
		{
			var bi = new BigInteger("0");
			var b = new Benchmark();
			for (var i = 0; i < count; ++i) {
				bi = bi.add(new BigInteger("102400000")); // bi主要消耗在new操作和构造的解析上，主要它的long的构造没有暴露。
			}
			b.report("BigInteger sum=" + bi, count);
		}
		{
			var int128 = new Id128();
			var b = new Benchmark();
			for (var i = 0; i < count; ++i) {
				int128.increment(1024000000000L);
			}
			b.report("int128 sum=" + int128, count); // 这个效率杠杠的。
		}
		System.out.println(Long.MAX_VALUE);
	}
}
