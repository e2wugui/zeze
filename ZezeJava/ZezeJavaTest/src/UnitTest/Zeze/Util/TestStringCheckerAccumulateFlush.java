package UnitTest.Zeze.Util;

import Zeze.Util.StringChecker;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * U5-F1：Trie.replace 贪心积累区间被无条件覆盖——同一成功转移段内先前命中的词条漏替换。
 * <p>
 * 修复：覆盖积累区间前先 flush 与新区间不重叠的旧区间；重叠（新词起点在旧区间内）时
 * 仍由新词覆盖，维持贪心最长匹配语义。
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
	 * 新词与旧积累区间重叠（新词起点在旧区间内）时不 flush：新词覆盖旧词，
	 * 语义与修复前的重叠处理保持一致。
	 */
	@Test
	public void testOverlapStillReplacedByLongerWord() throws IOException {
		var c = of("ab", "bcde", "abcdeX");
		Assertions.assertEquals("a****", c.replace("abcde", '*'));
	}
}
