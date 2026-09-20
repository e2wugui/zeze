package UnitTest.Zeze.Util;

import Zeze.Util.JsonReader;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * U3-F3：Infinity/NaN 词法恰在缓冲区末尾时被 AIOOBE 吞掉、静默返回 0。
 * <p>
 * 词法 do-while 加缓冲区上界判断：++p 越界即按词尾收尾 return 词法值。
 * parseInt/parseLong/parseDouble/parseNumber 四处同型一并修。
 */
@Fast
public final class TestJsonLexemeAtBufferEnd {

	@Test
	public void testParseDoubleAtBufferEnd() {
		var jr = new JsonReader().buf("Infinity");
		assertEquals(Double.POSITIVE_INFINITY, jr.parseDouble());
		assertTrue(jr.end(), "pos 必须停在词尾（缓冲区末尾）");

		jr = new JsonReader().buf("-Infinity");
		assertEquals(Double.NEGATIVE_INFINITY, jr.parseDouble());

		jr = new JsonReader().buf("NaN");
		assertTrue(Double.isNaN(jr.parseDouble()));
	}

	@Test
	public void testParseNumberAtBufferEnd() {
		var jr = new JsonReader().buf("Infinity");
		assertEquals(Double.POSITIVE_INFINITY, (Double)jr.parseNumber());
		jr = new JsonReader().buf("NaN");
		assertTrue(((Double)jr.parseNumber()).isNaN());
	}

	@Test
	public void testParseIntLongAtBufferEnd() {
		assertEquals(Integer.MAX_VALUE, new JsonReader().buf("Infinity").parseInt());
		assertEquals(Integer.MIN_VALUE, new JsonReader().buf("-Infinity").parseInt());
		assertEquals(Long.MAX_VALUE, new JsonReader().buf("Infinity").parseLong());
		assertEquals(Long.MIN_VALUE, new JsonReader().buf("-Infinity").parseLong());
	}

	/**
	 * 词法后跟定界符的常规位置不回归：值正确且 pos 停在词尾（定界符上）。
	 */
	@Test
	public void testDelimitedLexemeUnchanged() {
		var jr = new JsonReader().buf("Infinity,");
		assertEquals(Double.POSITIVE_INFINITY, jr.parseDouble());
		assertEquals(8, jr.pos(), "pos 停在定界符 ',' 上");
	}
}
