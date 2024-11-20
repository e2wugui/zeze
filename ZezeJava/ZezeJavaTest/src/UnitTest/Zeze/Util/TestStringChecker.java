package UnitTest.Zeze.Util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import Zeze.Util.StringChecker;
import org.junit.Assert;
import org.junit.Test;

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
		Assert.assertEquals("*", c.replace("A", '*'));
		Assert.assertEquals("B", c.replace("B", '*'));
		Assert.assertEquals("*", c.replace("C", '*'));
		Assert.assertEquals("D", c.replace("D", '*'));
		Assert.assertEquals("**", c.replace("AB", '*'));
		Assert.assertEquals("**", c.replace("AC", '*'));
		Assert.assertEquals("*D", c.replace("AD", '*'));
		Assert.assertEquals("B*", c.replace("BC", '*'));
		Assert.assertEquals("BD", c.replace("BD", '*'));
		Assert.assertEquals("**", c.replace("CD", '*'));
		Assert.assertEquals("***", c.replace("ABC", '*'));
		Assert.assertEquals("**D", c.replace("ABD", '*'));
		Assert.assertEquals("***", c.replace("ACD", '*'));
		Assert.assertEquals("B**", c.replace("BCD", '*'));
		Assert.assertEquals("****", c.replace("ABCD", '*'));
		Assert.assertEquals("B*", c.replace("BA", '*'));
		Assert.assertEquals("**", c.replace("CA", '*'));
		Assert.assertEquals("D*", c.replace("DA", '*'));
		Assert.assertEquals("*B", c.replace("CB", '*'));
		Assert.assertEquals("DB", c.replace("DB", '*'));
		Assert.assertEquals("D*", c.replace("DC", '*'));
		Assert.assertEquals("*B*", c.replace("CBA", '*'));
		Assert.assertEquals("DB*", c.replace("DBA", '*'));
		Assert.assertEquals("***", c.replace("CDA", '*'));
		Assert.assertEquals("D*B", c.replace("DCB", '*'));
		Assert.assertEquals("D*B*", c.replace("DCBA", '*'));
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
