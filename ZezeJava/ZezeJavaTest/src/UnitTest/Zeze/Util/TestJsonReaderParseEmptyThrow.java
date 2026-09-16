package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;
import Zeze.Util.JsonReader;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FND6-05姊妹漏网：JsonReader.parseInt/parseLong/parseDouble对非数字首字符走
 * else i=0分支静默返回0（0.0），parseNumber已修零消费守卫，三个同源姊妹方法漏网。
 * 暴露面：parseXxxKey对空引号数字key静默解析为0、Json.java列表parser元素首字符
 * 非数字静默0。修复：三方法补同型p==startPos零消费守卫抛NFE。
 */
@Fast
public class TestJsonReaderParseEmptyThrow {

	private static JsonReader reader(String s) {
		return new JsonReader(s.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void testNonDigitFirstCharThrows() {
		for (var s : new String[]{"x", ",", " ", "", "K"}) {
			Assertions.assertAll("input=\"" + s + "\"",
					() -> assertThrows(NumberFormatException.class, () -> reader(s).parseInt(),
							"parseInt 应抛NFE"),
					() -> assertThrows(NumberFormatException.class, () -> reader(s).parseLong(),
							"parseLong 应抛NFE"),
					() -> assertThrows(NumberFormatException.class, () -> reader(s).parseDouble(),
							"parseDouble 应抛NFE"),
					() -> assertThrows(NumberFormatException.class, () -> reader(s).parseNumber(),
							"parseNumber 应抛NFE（FND6-05既有行为，钉住不回退）"));
		}
	}

	@Test
	public void testLegalInputsUnchanged() {
		assertEquals(123, reader("123x").parseInt());
		assertEquals(-45, reader("-45,").parseInt());
		assertEquals(0x1f, reader("0x1f;").parseInt());
		assertEquals(123456789012345L, reader("123456789012345]").parseLong());
		assertEquals(2.5, reader("2.5,").parseDouble());
		assertEquals(0.5, reader(".5,").parseDouble(), "点号开头非零消费，行为不变");
		assertEquals(0, reader("-").parseInt(), "仅符号消费了一个字符，维持返回0不抛");
		assertEquals(Double.POSITIVE_INFINITY, reader("Infinity,").parseDouble());
		assertEquals(Double.NaN, reader("NaN,").parseDouble());
		assertEquals(150_000_000f, (float)reader("1.5e8]").parseDouble());
	}
}
