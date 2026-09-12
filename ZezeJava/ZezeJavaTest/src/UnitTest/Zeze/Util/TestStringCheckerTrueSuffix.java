package UnitTest.Zeze.Util;

import harness.Fast;
import java.io.IOException;
import Zeze.Util.StringChecker;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-09 真后缀词条测试：词条 W1 是另一词条 W2 的真后缀时，
 * W1 的命中经由 W2 前缀路径的 fail 链到达——旧实现（自引用叶表示）成功转移后
 * 只查转移目标自身的终止性，fail 链输出从未被检查，W1 系统性漏检/漏替换。
 * 修复：词尾显式化为真实节点+end标志，contains/replace 按标准AC沿fail链检查输出。
 * 同时保持既有行为（最长词优先贪心、重叠词后缀恢复）不回归——见 TestStringCheckerOverlap。
 */
@Fast
public class TestStringCheckerTrueSuffix {

	private static @NotNull StringChecker of(@NotNull String... words) throws IOException {
		var c = new StringChecker();
		for (var w : words)
			c.addNewLine(w);
		c.reload(null);
		return c;
	}

	/** 真后缀漏检主场景：bc 是 abcd 的真后缀。 */
	@Test
	public void testTrueSuffixContains() throws IOException {
		var c = of("abcd", "bc");
		// 旧实现：走到"abc"后文本结束/失配，"bc"经fail链的命中信号丢失
		Assertions.assertTrue(c.contains("abc"), "bc是abc的子串");
		Assertions.assertTrue(c.contains("zbc"));
		Assertions.assertTrue(c.contains("bc"));
		Assertions.assertTrue(c.contains("abcd"));
		Assertions.assertFalse(c.contains("abx"));
		Assertions.assertFalse(c.contains("xyz"));
	}

	/** 真后缀替换穿透主场景。 */
	@Test
	public void testTrueSuffixReplace() throws IOException {
		var c = of("abcd", "bc");
		Assertions.assertEquals("a**", c.replace("abc", '*')); // 旧实现返回"abc"（穿透）
		Assertions.assertEquals("**", c.replace("bc", '*'));
		Assertions.assertEquals("z****z", c.replace("zabcdz", '*')); // 最长词优先
		Assertions.assertEquals("**x**", c.replace("bcxbc", '*'));
	}

	/** 单字词+其作为长词后缀/中缀的组合。 */
	@Test
	public void testSingleCharWord() throws IOException {
		var c = of("ab", "b");
		Assertions.assertTrue(c.contains("xbx"));
		Assertions.assertEquals("x**x", c.replace("xabx", '*'));
	}

	/** 中缀经fail链到达：b 是 abc 的中缀（非后缀）。 */
	@Test
	public void testInfixWord() throws IOException {
		var c = of("abc", "b");
		Assertions.assertTrue(c.contains("abc"));
		Assertions.assertEquals("***", c.replace("abc", '*'));
	}

	/** 词既是长词的前缀又是真后缀的复合场景。 */
	@Test
	public void testPrefixAndSuffixWord() throws IOException {
		var c = of("abcd", "bc", "bce");
		Assertions.assertTrue(c.contains("abc"));
		Assertions.assertTrue(c.contains("abce"));
		Assertions.assertEquals("a***", c.replace("abce", '*')); // bce(3字)优先于bc(2字)
		Assertions.assertEquals("z****f", c.replace("zabcdf", '*'));
	}
}
