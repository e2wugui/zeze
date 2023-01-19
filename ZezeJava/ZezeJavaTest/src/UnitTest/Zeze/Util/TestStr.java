package UnitTest.Zeze.Util;

import Zeze.Util.Str;
import junit.framework.TestCase;

public final class TestStr extends TestCase {
	public void testParseSize() {
		assertEquals(1234567890L, Str.parseLongSize("1234_5678,90"));
		assertEquals(2 * 1024, Str.parseIntSize(" 2 k "));
		assertEquals(3 * 1024 * 1024 / 2, Str.parseLongSize(" 1.5M"));
		assertEquals(Integer.MAX_VALUE, Str.parseIntSize(" max "));
		assertEquals(Long.MAX_VALUE, Str.parseLongSize(" max "));
	}
}
