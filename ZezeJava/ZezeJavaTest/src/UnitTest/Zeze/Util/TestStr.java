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

	public void testParseVersion() {
		Assert.assertEquals(0x0001_0002_0003_0004L, Str.parseVersion("1.2.3.4"));
		Assert.assertEquals(0x0000_0005_0006_0000L, Str.parseVersion("0.5.6"));
		Assert.assertEquals(0x0000_0007_0008_0009L, Str.parseVersion(".7.8.9.12"));
		Assert.assertEquals(0x0000_0000_0013_0000L, Str.parseVersion("..19..1.2"));
		Assert.assertEquals(0x0000_0000_0000_0000L, Str.parseVersion(""));
		Assert.assertEquals(0x0000_0000_0000_0000L, Str.parseVersion(".."));
	}
}
