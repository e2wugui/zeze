package UnitTest.Zeze.Util;

import java.io.IOException;
import Zeze.Util.StringChecker;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * BOM 行词条的 deep 相对深度（FND2-U2-2）：
 * addLine 对 UTF-8 BOM 行(首字符0xfeff)调用 add(line,1,e)，但 add 循环原先把
 * 绝对下标 i 记入 trie.deep（BOM 行整体多1），replace 的词区间公式 [i-deep, i]
 * 因此左扩1字符——词前一个无辜字符被多替换（或下标越界）。
 * 修复后进入时快照 start=i，deep 记录相对深度 i-start，两种行来源公式统一。
 */
@Fast
public class TestStringCheckerBom {

	/** BOM 行整词替换：区间恰为词本身，词前字符不被多替换。 */
	@Test
	public void testBomWordReplace() throws IOException {
		var c = new StringChecker();
		c.addNewLine("\ufeff敏感词"); // BOM 行：add(line,1,e)
		c.addNewLine("正常词"); // 普通行对照
		c.reload(null);
		Assertions.assertTrue(c.contains("敏感词"));
		// 修复前：deep=绝对下标(多1)，区间左扩1字符，'a' 也被替换 → "****b"
		Assertions.assertEquals("a***b", c.replace("a敏感词b", '*'));
		Assertions.assertEquals("x***y", c.replace("x正常词y", '*'));
		Assertions.assertEquals("a###b", c.replace("a敏感词b", '#'));
	}

	/** BOM 行作为前缀词（zero 积累分支）：iLast = ++i - next.deep 同样依赖相对深度。 */
	@Test
	public void testBomWordAsPrefix() throws IOException {
		var c = new StringChecker();
		c.addNewLine("ab"); // 普通行短词，是长词前缀
		c.addNewLine("\ufeffabc"); // BOM 行长词
		c.reload(null);
		Assertions.assertTrue(c.contains("abc"));
		// 修复前 'b' 节点 deep=绝对下标3，区间 [i-3,i] 把词前字符一并替换 → "****y"
		Assertions.assertEquals("z***y", c.replace("zabcy", '*'));
		// 短词 "ab" 单独命中：只替换词内两个字符
		Assertions.assertEquals("z**", c.replace("zab", '*'));
	}
}
