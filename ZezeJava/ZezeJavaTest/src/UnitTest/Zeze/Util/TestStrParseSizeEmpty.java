package UnitTest.Zeze.Util;

import Zeze.Util.Str;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND6-05：Str.parseLongSize/parseIntSize 对空数字串（空值/纯单位如"K"）静默返回0——
 * buf 仅含终结符' '，JsonReader.parseNumber 的非数字首字符分支走 else i=0 返回
 * Integer 0，配置错误被吞（0容量缓存/0缓冲静默劣化）；非法字符"x"反而正确抛NFE，
 * 行为分叉。修复：parseNumber 零消费（p==startPos）抛NFE，Str 侧NFE直通不伪装成溢出。
 */
@Fast
public class TestStrParseSizeEmpty {

	@Test
	public void testEmptyOrUnitOnlyRejected() {
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize(""),
				"空串必须报数字格式错误而非静默0");
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize("K"),
				"纯单位串必须报数字格式错误而非静默0");
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize(" "),
				"纯间隔符串必须报数字格式错误");
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseIntSize(""),
				"parseIntSize同型（委托parseLongSize）");
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseIntSize("M"),
				"parseIntSize纯单位同型");
	}

	@Test
	public void testValidSizesUnchanged() {
		Assertions.assertEquals(1024L, Str.parseLongSize("1024"));
		Assertions.assertEquals(1024L, Str.parseLongSize("1K"));
		Assertions.assertEquals(1024L * 1024L, Str.parseLongSize("1M"));
		Assertions.assertEquals(1L << 30, Str.parseLongSize("1G"));
		Assertions.assertEquals(0L, Str.parseLongSize("0"));
		Assertions.assertEquals(0L, Str.parseLongSize("0K"), "0带单位仍是合法的0，不受守卫影响");
		Assertions.assertEquals(2048, Str.parseIntSize("2K"));
		Assertions.assertEquals(5, Str.parseIntSize("5"));
	}

	@Test
	public void testInvalidCharStillNFE() {
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize("x"));
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize("1Q"));
	}

	@Test
	public void testNullDefaultUnchanged() {
		Assertions.assertEquals(-1L, Str.parseLongSize(null, -1L), "null走默认值语义不变");
		Assertions.assertEquals(42, Str.parseIntSize(null, 42));
	}
}
