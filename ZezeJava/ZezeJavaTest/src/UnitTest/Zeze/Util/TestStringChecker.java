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
		Assert.assertEquals(c.replace("A", '*'), "*");
		Assert.assertEquals(c.replace("B", '*'), "B");
		Assert.assertEquals(c.replace("C", '*'), "*");
		Assert.assertEquals(c.replace("D", '*'), "D");
		Assert.assertEquals(c.replace("AB", '*'), "**");
		Assert.assertEquals(c.replace("AC", '*'), "**");
		Assert.assertEquals(c.replace("AD", '*'), "*D");
		Assert.assertEquals(c.replace("BC", '*'), "B*");
		Assert.assertEquals(c.replace("BD", '*'), "BD");
		Assert.assertEquals(c.replace("CD", '*'), "**");
		Assert.assertEquals(c.replace("ABC", '*'), "***");
		Assert.assertEquals(c.replace("ABD", '*'), "**D");
		Assert.assertEquals(c.replace("ACD", '*'), "***");
		Assert.assertEquals(c.replace("BCD", '*'), "B**");
		Assert.assertEquals(c.replace("ABCD", '*'), "****");
		Assert.assertEquals(c.replace("BA", '*'), "B*");
		Assert.assertEquals(c.replace("CA", '*'), "**");
		Assert.assertEquals(c.replace("DA", '*'), "D*");
		Assert.assertEquals(c.replace("CB", '*'), "*B");
		Assert.assertEquals(c.replace("DB", '*'), "DB");
		Assert.assertEquals(c.replace("DC", '*'), "D*");
		Assert.assertEquals(c.replace("CBA", '*'), "*B*");
		Assert.assertEquals(c.replace("DBA", '*'), "DB*");
		Assert.assertEquals(c.replace("CDA", '*'), "***");
		Assert.assertEquals(c.replace("DCB", '*'), "D*B");
		Assert.assertEquals(c.replace("DCBA", '*'), "D*B*");
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
