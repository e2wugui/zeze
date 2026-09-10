package UnitTest.Zeze.Util;

import Zeze.Util.Ranges;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-17 回归：Range.toString 必须与解析对称——解析把闭区间 "1-3" +1 转半开 [1,4)，
 * toString 必须打印闭端点 "1-3"（原实现打印半开上界 "1-4"，每往返一次右边界扩 1）。
 * 本类同时锁定 Ranges 集合级的往返不变式。
 */
@Fast
public class TestRangesRoundTrip {

	@Test
	public void testToStringMatchesParse() {
		Assertions.assertEquals("1-3", new Ranges.Range(new String[]{"1", "3"}).toString());
		Assertions.assertEquals("5", new Ranges.Range(new String[]{"5"}).toString());
		Assertions.assertEquals("1-3", new Ranges.Range(1, 4).toString()); // 半开[1,4) 打印闭端点
	}

	@Test
	public void testRoundTripInvariant() {
		// Ranges.toString 输出与解析同构的逗号分隔闭区间串，集合级往返不变式
		for (var s : new String[]{"1-3", "5", "100-200", "1,3,5-8"})
			Assertions.assertEquals(s, new Ranges(s).toString(), "round trip: " + s);
	}
}
