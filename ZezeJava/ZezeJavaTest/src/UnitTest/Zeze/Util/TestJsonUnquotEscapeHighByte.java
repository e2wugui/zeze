package UnitTest.Zeze.Util;

import Zeze.Util.JsonReader;
import harness.Fast;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * U3-F5：parseStringNoQuot 的非 'u' 转义字符 ≥0x80 时缺 &amp;0xff，符号扩展为 U+FFxx，
 * 与带引号的 parseString（已掩码）解码不一致。修复：补掩码对齐。
 */
@Fast
public final class TestJsonUnquotEscapeHighByte {

	/**
	 * 无引号键 {\&lt;0xC3&gt;x:1} 与带引号键 "\&lt;0xC3&gt;x" 必须解码出同一个键。
	 */
	@Test
	public void testNoQuotKeyHighByteEscape() throws ReflectiveOperationException {
		var m = new JsonReader().buf(new byte[]{'{', '\\', (byte)0xC3, 'x', ':', '1', '}'})
				.parseMap(new HashMap<>());
		assertEquals(Set.of("\u00C3x"), m.keySet(), "修复前为 \\uFFC3x（符号扩展）");
		assertEquals(1, m.get("\u00C3x"));

		var m2 = new JsonReader().buf(new byte[]{'{', '"', '\\', (byte)0xC3, 'x', '"', ':', '2', '}'})
				.parseMap(new HashMap<>());
		assertEquals(Set.of("\u00C3x"), m2.keySet());
		assertEquals(2, m2.get("\u00C3x"));

		// 两路径解码一致
		assertEquals(m.keySet(), m2.keySet());
		assertEquals(Map.of("\u00C3x", 1), m);
	}
}
