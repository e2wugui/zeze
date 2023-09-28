package UnitTest.Zeze.Util;

import java.util.HashMap;
import Zeze.Util.Str;
import junit.framework.TestCase;
import org.junit.Assert;

public final class TestStr extends TestCase {
	public void testParseSize() {
		assertEquals(1234567890L, Str.parseLongSize("1234_5678,90"));
		assertEquals(2 * 1024, Str.parseIntSize(" 2 k "));
		assertEquals(3 * 1024 * 1024 / 2, Str.parseLongSize(" 1.5M"));
		assertEquals(Integer.MAX_VALUE, Str.parseIntSize(" max "));
		assertEquals(Long.MAX_VALUE, Str.parseLongSize(" max "));
	}

	public void testFormat() {
		var params = new HashMap<String, Object>();
		var serverId = 0;
		var host = "127.0.0.1";
		var port = 80;
		params.put("serverId", serverId);
		params.put("host", host);
		params.put("port", port);

		var f = Str.format("begin_{serverId}_{host}_{port}_end", params);
		Assert.assertEquals("begin_0_127.0.0.1_80_end", f);
		System.out.println(f);
	}
}
