package UnitTest.Zeze.Util;

import Zeze.Util.Str;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-53回归：Str.parseLongSize遇K/M/G/T/P/E单位字符即置scale并break loop，
 * 单位之后的字符从不检查——与前缀非法字符的fail-fast自相矛盾：
 * "1e3"（科学计数法意图1000）被解析为1×2^60≈1.15e18，"10Mx"、"1k2"等笔误
 * 静默按10M、1k接受，错误值直接决定内存分配与限幅行为（socket缓冲上限、
 * RocksDB cache尺寸等）且无告警。
 * 修复：单位后仅允许间隔符（\t空格_,，'）到串尾，其余抛NumberFormatException。
 */
@Fast
public class TestFnd753StrParseLongSizeUnitTail {

	@Test
	public void testTrailingGarbageRejected() {
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize("1e3"),
				"科学计数法意图不得按1E静默接受");
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize("10Mx"));
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize("1k2"));
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize("10MM"));
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize("1G2K"));
		Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize("64KB"),
				"KB双字母单位不支持（本方法单位为单字符），不得按64K静默接受");
	}

	@Test
	public void testValidSizesUnchanged() {
		Assertions.assertEquals(10L << 20, Str.parseLongSize("10M"));
		Assertions.assertEquals(1L << 10, Str.parseLongSize("1 k"));
		Assertions.assertEquals(1L << 10, Str.parseLongSize("1k"));
		Assertions.assertEquals(2L << 30, Str.parseLongSize("2G"));
		Assertions.assertEquals(10L << 10, Str.parseLongSize("1_0k"), "数字中的间隔符允许");
		Assertions.assertEquals(1L << 20, Str.parseLongSize("1M_"), "单位后的间隔符允许");
		Assertions.assertEquals(16L << 10, Str.parseLongSize(" 16K "), "首尾空白已trim");
		Assertions.assertEquals(Long.MAX_VALUE, Str.parseLongSize("max"));
		Assertions.assertEquals(-1, Str.parseLongSize(null));
	}

	@Test
	public void testErrorMessageHintsSingleLetterUnit() {
		// R3-U2（B）：用户既有部署配置里的双字母单位习惯（redis的100mb、SI的64KB）从静默
		// 按单字符接受变为启动fail-fast——异常消息必须自带等效合法写法，迁移零思考成本。
		var ex = Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize("64KB"));
		Assertions.assertTrue(ex.getMessage().contains("'64K'"),
				() -> "异常消息应提示等效单字符单位写法'64K'，实际: " + ex.getMessage());
		var ex2 = Assertions.assertThrows(NumberFormatException.class, () -> Str.parseLongSize("10Mx"));
		Assertions.assertTrue(ex2.getMessage().contains("'10M'"),
				() -> "异常消息应提示等效单字符单位写法'10M'，实际: " + ex2.getMessage());
	}
}
