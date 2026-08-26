package UnitTest.Zeze.Util;

import harness.Fast;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import Zeze.Util.StringChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestStringChecker {
	@Test
	public void test() throws IOException {
		StringChecker c = new StringChecker();
		c.addNewLine("A");
		c.addNewLine("AB");
		c.addNewLine("CD");
		c.addNewLine("C");
		c.addNewLine("EF");
		c.addNewLine("FG");
		c.reload(null);
		Assertions.assertEquals("*", c.replace("A", '*'));
		Assertions.assertEquals("B", c.replace("B", '*'));
		Assertions.assertEquals("*", c.replace("C", '*'));
		Assertions.assertEquals("D", c.replace("D", '*'));
		Assertions.assertEquals("**", c.replace("AB", '*'));
		Assertions.assertEquals("**", c.replace("AC", '*'));
		Assertions.assertEquals("*D", c.replace("AD", '*'));
		Assertions.assertEquals("B*", c.replace("BC", '*'));
		Assertions.assertEquals("BD", c.replace("BD", '*'));
		Assertions.assertEquals("**", c.replace("CD", '*'));
		Assertions.assertEquals("***", c.replace("ABC", '*'));
		Assertions.assertEquals("**D", c.replace("ABD", '*'));
		Assertions.assertEquals("***", c.replace("ACD", '*'));
		Assertions.assertEquals("B**", c.replace("BCD", '*'));
		Assertions.assertEquals("****", c.replace("ABCD", '*'));
		Assertions.assertEquals("B*", c.replace("BA", '*'));
		Assertions.assertEquals("**", c.replace("CA", '*'));
		Assertions.assertEquals("D*", c.replace("DA", '*'));
		Assertions.assertEquals("*B", c.replace("CB", '*'));
		Assertions.assertEquals("DB", c.replace("DB", '*'));
		Assertions.assertEquals("D*", c.replace("DC", '*'));
		Assertions.assertEquals("*B*", c.replace("CBA", '*'));
		Assertions.assertEquals("DB*", c.replace("DBA", '*'));
		Assertions.assertEquals("***", c.replace("CDA", '*'));
		Assertions.assertEquals("D*B", c.replace("DCB", '*'));
		Assertions.assertEquals("D*B*", c.replace("DCBA", '*'));
	}

	public static void test2() throws IOException {
		StringChecker c = new StringChecker();
		System.out.println(c.reload("res/forbid_names.txt", StandardCharsets.UTF_8));
		String[] ss = {"trie树结构搭配AC自动机算法", "内存占用大概是txt的30倍大小...敏感词", "比使用JDK正则表达式的匹配替换快1000倍", "搭配AC自动机比普通trie树快10%...TEST..."};
		int h = 0;
		long t = System.nanoTime();
		for (int i = 0; i < 1000000; i++)
			h += c.replace(ss[i % ss.length], '*').hashCode();
		System.out.println(h + " " + (System.nanoTime() - t) / 1_000_000 + "ms");
	}

	public static void main(String[] args) throws Exception {
		test2();
	}
}
