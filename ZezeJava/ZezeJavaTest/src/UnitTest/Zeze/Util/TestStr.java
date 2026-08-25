package UnitTest.Zeze.Util;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import Zeze.Util.Str;
import org.junit.jupiter.api.Assertions;

public final class TestStr {
	@Test
	public void testParseSize() {
		assertEquals(1234567890L, Str.parseLongSize("1234_5678,90"));
		assertEquals(2 * 1024, Str.parseIntSize(" 2 k "));
		assertEquals(3 * 1024 * 1024 / 2, Str.parseLongSize(" 1.5M"));
		assertEquals(Integer.MAX_VALUE, Str.parseIntSize(" max "));
		assertEquals(Long.MAX_VALUE, Str.parseLongSize(" max "));
	}

	@Test

	public void testFormat() {
		var params = new HashMap<String, Object>();
		var serverId = 0;
		var host = "127.0.0.1";
		var port = 80;
		params.put("serverId", serverId);
		params.put("host", host);
		params.put("port", port);

		var f = Str.format("begin_{serverId}_{host}_{port}_end", params);
		Assertions.assertEquals("begin_0_127.0.0.1_80_end", f);
		System.out.println(f);
	}

	@Test

	public void testParseVersion() {
		Assertions.assertEquals(0x0001_0000_0000_0000L, Str.parseVersion("1"));
		Assertions.assertEquals(0x0002_0003_0000_0000L, Str.parseVersion("2.3"));
		Assertions.assertEquals(0x0001_0002_0003_0004L, Str.parseVersion("1.2.3.4"));
		Assertions.assertEquals(0x0000_0005_0006_0000L, Str.parseVersion("0.5.6"));
		Assertions.assertEquals(0x0000_0007_0008_0009L, Str.parseVersion(".7.8.9.12"));
		Assertions.assertEquals(0x0000_0000_0013_0000L, Str.parseVersion("..19..1.2"));
		Assertions.assertEquals(0x0000_0000_0000_0000L, Str.parseVersion(""));
		Assertions.assertEquals(0x0000_0000_0000_0000L, Str.parseVersion(".."));
	}
}
