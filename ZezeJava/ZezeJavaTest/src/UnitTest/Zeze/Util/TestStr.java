package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import Zeze.Util.Str;
import org.junit.jupiter.api.Assertions;

@Fast
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

	// FND4-12: 模板契约是“普通文本+{var}占位”，字面'%'是普通字符，不得被当格式符
	// （抛UnknownFormatConversionException或%n等合法符静默注入/吞参错位）。
	@Test
	public void testFormatLiteralPercent() {
		var params = new HashMap<String, Object>();
		params.put("name", "x");
		params.put("f", 1.5);
		params.put("i", 42);
		params.put("b", true);
		params.put("c", 'Z');
		params.put("d", new java.util.Date(0));

		Assertions.assertEquals("50% of x", Str.format("50% of {name}", params));
		Assertions.assertEquals("line1%nx", Str.format("line1%n{name}", params)); // %n不是换行符
		Assertions.assertEquals("a%sbx", Str.format("a%sb{name}", params)); // %s不吞参
		// 类型渲染锁定原Formatter语义：浮点%f定点6位小数；其余与String.valueOf一致
		Assertions.assertEquals("f=1.500000", Str.format("f={f}", params));
		Assertions.assertEquals("i=42", Str.format("i={i}", params));
		Assertions.assertEquals("b=true", Str.format("b={b}", params));
		Assertions.assertEquals("c=Z", Str.format("c={c}", params));
		Assertions.assertEquals("d=" + new java.util.Date(0), Str.format("d={d}", params));
	}

	@Test

	public void testParseVersion() {
		Assertions.assertEquals(0x0001_0000_0000_0000L, Str.parseVersion("1"));
		Assertions.assertEquals(0x0002_0003_0000_0000L, Str.parseVersion("2.3"));
		Assertions.assertEquals(0x0001_0002_0003_0004L, Str.parseVersion("1.2.3.4"));
		Assertions.assertEquals(0x0000_0005_0006_0000L, Str.parseVersion("0.5.6"));
		// FND6-06：4段是既定格式契约，第5个'.'起fail-fast（原静默截断，"1.2.3.4.5"与
		// "1.2.3.4"解析相等，第5段差异被忽略）。
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseVersion("1.2.3.4.5"));
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseVersion(".7.8.9.12"));
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseVersion("..19..1.2"));
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseVersion("1.2.3.4."));
		Assertions.assertEquals(0x0000_0000_0000_0000L, Str.parseVersion(""));
		Assertions.assertEquals(0x0000_0000_0000_0000L, Str.parseVersion(".."));
	}
}
