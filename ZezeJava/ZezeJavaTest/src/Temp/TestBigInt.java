package Temp;

import java.math.BigInteger;
import Zeze.Util.Benchmark;
import Zeze.Util.Id128;

public class TestBigInt {
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
