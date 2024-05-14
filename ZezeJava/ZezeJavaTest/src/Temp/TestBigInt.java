package Temp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.GenericBean;
import Zeze.Util.Benchmark;
import Zeze.Util.Id128;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestBigInt extends TestCase {
	private static final Logger logger = LogManager.getLogger(TestBigInt.class);

	public void testSerialize() {
		var id128 = new Id128(123, 0x1234_5678_90ab_cdefL);
		var bb = ByteBuffer.Allocate();
		id128.encode(bb);

		var gb = new GenericBean();
		gb.decode(bb);
		assertEquals(1, gb.fields.size()); // field count
		var e = gb.fields.entrySet().iterator().next();
		assertEquals(1, e.getKey().intValue()); // varId
		assertEquals(ArrayList.class, e.getValue().getClass());
		var list = (ArrayList<?>)e.getValue();
		assertEquals(2, list.size());
		var high = list.get(0);
		var low = list.get(1);
		assertEquals(Integer.class, high.getClass());
		assertEquals(Long.class, low.getClass());
		assertEquals(id128.getHigh(), ((Integer)high).intValue());
		assertEquals(id128.getLow(), ((Long)low).longValue());
	}

	public void testOrder() {
		var rand = ThreadLocalRandom.current();
		var id1 = new Id128();
		var id2 = new Id128();
		var bb1 = ByteBuffer.Allocate(24);
		var bb2 = ByteBuffer.Allocate(24);
		for (int i = 0; i < 1_000_000; i++) {
			id1.assign(rand.nextLong() >> rand.nextInt(64), rand.nextLong() >> rand.nextInt(64));
			id2.assign(rand.nextLong() >> rand.nextInt(64), rand.nextLong() >> rand.nextInt(64));
			bb1.Reset();
			bb2.Reset();
			id1.encode(bb1);
			id2.encode(bb2);
			if (Integer.signum(id1.compareTo(id2)) != Integer.signum(bb1.compareTo(bb2))) {
				logger.error("Id128 order error: {}; {},{}; compare={},{}",
						String.format("%016X_%016X, %016X_%016X",
								id1.getHigh(), id1.getLow(), id2.getHigh(), id2.getLow()),
						bb1, bb2, id1.compareTo(id2), bb1.compareTo(bb2));
				fail();
			}
			id2.decode(bb1);
			assertEquals(id1, id2);
		}
	}

	public static void main(String[] args) {
		var count = 10000_0000;
		{
			var bi = new BigInteger("0");
			var b = new Benchmark();
			for (var i = 0; i < count; ++i) {
				var bytes = new byte[4];
				ByteBuffer.intBeHandler.set(bytes, 0, 102400000);
				bi = bi.add(new BigInteger(bytes)); // bi主要消耗在new操作和构造的解析上，主要它的long的构造没有暴露。
			}
			b.report("BigInteger sum=" + bi, count);
		}
		{
			var int128 = new Id128();
			var b = new Benchmark();
			for (var i = 0; i < count; ++i) {
				int128.increment(102400000);
			}
			b.report("Id128      sum=" + int128, count); // 这个效率杠杠的。
		}
		System.out.println(Long.MAX_VALUE);
	}
}
