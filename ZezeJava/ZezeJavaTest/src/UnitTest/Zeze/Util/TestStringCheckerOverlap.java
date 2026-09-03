package UnitTest.Zeze.Util;

import harness.Fast;
import java.io.IOException;
import Zeze.Util.StringChecker;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * StringChecker.replace 的重叠词测试（FND-U2-3）。
 * <p>
 * 词表存在"前词后缀 = 后词前缀"的重叠对（如 ab/bc）时，replace 的叶子命中分支
 * 替换后不重置也不沿 fail 转移，构成后词的字符已被消费，重叠词原样漏过（敏感词穿透）；
 * 零值(zero)积累分支的失配回退同样回退到词尾导致后缀起点丢失。
 * 修复后：命中词后沿 fail 链用词尾字符恢复后缀状态，重叠词继续匹配。
 */
@Fast
public class TestStringCheckerOverlap {

	private static @NotNull StringChecker of(@NotNull String... words) throws IOException {
		var c = new StringChecker();
		for (var w : words)
			c.addNewLine(w);
		c.reload(null);
		return c;
	}

	/**
	 * finding 场景：叶子命中（ab 无更长扩展）后 trie 停留，bc 漏替换。
	 * 修复前 "xabcx" → "x**cx"。
	 */
	@Test
	public void testLeafOverlap() throws IOException {
		var c = of("ab", "bc");
		Assertions.assertEquals("x***x", c.replace("xabcx", '*')); // 修复前 x**cx
		Assertions.assertEquals("*****", c.replace("abcbc", '*'));
		Assertions.assertEquals("x*****x", c.replace("xabcbcx", '*'));
		Assertions.assertTrue(c.contains("xabcx")); // contains 布尔语义不受影响
	}

	/**
	 * 词是词的前缀（zero 终止）+ 与第三个词重叠：叶子命中走 zero 积累后的贪心扩展，
	 * 命中后同样需要后缀转移。
	 */
	@Test
	public void testZeroWordOverlap() throws IOException {
		var c = of("abc", "ab", "bcd");
		Assertions.assertEquals("z****z", c.replace("zabcdz", '*')); // 修复前 z***dz：bcd 漏
	}

	/**
	 * zero 积累失配回退路径的重叠：积累词 ab（因 abx 存在而是 zero 终止）在 c 失配，
	 * 回退替换后沿 fail 恢复 b 后缀，bcd 得以匹配。
	 */
	@Test
	public void testZeroAccumulateMismatchOverlap() throws IOException {
		var c = of("abx", "ab", "bcd");
		Assertions.assertEquals("z****n", c.replace("zabcdn", '*')); // 修复前 z**cdn：bcd 漏
	}

	/**
	 * 同前缀连续命中（多词共享首字符）不受影响。
	 */
	@Test
	public void testSamePrefixChain() throws IOException {
		var c = of("aa", "ab");
		Assertions.assertEquals("****", c.replace("aaab", '*'));
		Assertions.assertEquals("**", c.replace("aa", '*'));
		Assertions.assertEquals("**", c.replace("ab", '*'));
	}

	/**
	 * 回归：仓库既有词表（A,AB,CD,C,EF,FG）的关键断言 + EF/FG 重叠（此前无覆盖）。
	 */
	@Test
	public void testExistingDictionaryRegression() throws IOException {
		var c = of("A", "AB", "CD", "C", "EF", "FG");
		Assertions.assertEquals("**", c.replace("AC", '*'));
		Assertions.assertEquals("***", c.replace("ABC", '*'));
		Assertions.assertEquals("****", c.replace("ABCD", '*'));
		Assertions.assertEquals("*D", c.replace("AD", '*'));
		Assertions.assertEquals("B*", c.replace("BC", '*'));
		Assertions.assertEquals("D*B*", c.replace("DCBA", '*'));
		Assertions.assertEquals("***", c.replace("CDA", '*'));
		// EF 与 FG 重叠：修复前 "EFG" → "**G"（FG 漏），修复后 "***"
		Assertions.assertEquals("***", c.replace("EFG", '*'));
		Assertions.assertEquals("****", c.replace("AEFG", '*'));
	}
}
