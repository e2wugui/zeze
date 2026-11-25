package Zeze.Util;

import Zeze.Util.StringChecker;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * U5-F1：Trie.replace 贪心积累区间被无条件覆盖——同一成功转移段内先前命中的词条漏替换。
 * <p>
 * 修复：与旧积累区间不重叠时先 flush 旧区间；重叠（新词起点落在旧区间内）时对两区间取并集，
 * 保留更早左边界，确保前词前段与重叠后缀词全部被替换。
 */
@Fast
public class TestStringCheckerAccumulateFlush {

	private static StringChecker of(String... words) throws IOException {
		var c = new StringChecker();
		for (var w : words)
			c.addNewLine(w);
		c.reload(null);
		return c;
	}

	/**
	 * finding 场景：积累词 ab（因 abcdezq 存在而走积累分支）后，同段内命中更长路径上
	 * 的 cde（经 fail 链），旧区间 [0,2) 被静默覆盖——修复前只替换 cde、ab 原样漏过。
	 */
	@Test
	public void testAccumulatedWordFlushedBeforeOverwrite() throws IOException {
		var c = of("ab", "cde", "abcdezq");
		Assertions.assertEquals("*****z", c.replace("abcdez", '*'), "修复前 ab***z：ab 漏替换");
		// 后继无关字符同样只影响自身
		Assertions.assertEquals("z*****zw", c.replace("zabcdezw", '*'));
		// 完整长词命中时最终区间覆盖全程，不受 flush 影响
		Assertions.assertEquals("*******", c.replace("abcdezq", '*'));
		Assertions.assertTrue(c.contains("abcdez"));
	}

	/**
	 * 新词与旧积累区间重叠（新词起点落在旧区间内）时取区间并集，全覆盖替换，
	 * 与 TestStringCheckerOverlap 的重叠词全替换契约一致。
	 */
	@Test
	public void testOverlapStillReplacedByLongerWord() throws IOException {
		var c = of("ab", "bcde", "abcdeX");
		Assertions.assertEquals("*****", c.replace("abcde", '*'));
	}

	/**
	 * 重叠后缀命中不得丢弃已命中前词的前段：仅新增一条未命中的长词不能让原敏感词泄漏。
	 */
	@Test
	public void testOverlappingSuffixPreservesEarlierPrefix() throws IOException {
		var c = of("12345", "56", "1234567");
		Assertions.assertEquals("******", c.replace("123456", '*'));
		Assertions.assertEquals("x******y", c.replace("x123456y", '*'));

		var c2 = of("abcdx", "ab", "bcd");
		Assertions.assertEquals("z****n", c2.replace("zabcdn", '*'));

		// 更大新词包住旧积累区间（start < iLast）同样全覆盖
		var c3 = of("bc", "abcd", "abcdx");
		Assertions.assertEquals("****", c3.replace("abcd", '*'));
	}

	/**
	 * 纯叶子命中时清理已挂起的积累区间，避免残留区间延续影响后续匹配。
	 */
	@Test
	public void testLeafHitClearsPendingAccumulatedRange() throws IOException {
		var c = of("ab", "abc", "x");
		Assertions.assertEquals("***z", c.replace("abcz", '*'));
		Assertions.assertEquals("****", c.replace("abcx", '*'));
		Assertions.assertEquals("***y*", c.replace("abcyx", '*'));

		var c2 = of("ab", "abc", "bcd");
		Assertions.assertEquals("****z", c2.replace("abcdz", '*'));
	}
}
