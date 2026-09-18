package UnitTest.Zeze.Util;

import java.util.HashMap;
import java.util.HashSet;
import Zeze.Util.StringSpan;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-14回归（契约文档成文，行为不改）：StringSpan与String的跨类型互查是蓄意
 * 单向设计——span.equals("txt")为true且hashCode一致，可作String键容器的查询参数
 * （Mimes.fromFileExtension依赖）；但String.equals不认span，反向不成立：span存入
 * HashSet后再以等值String查询会哈希桶命中而equals静默miss。契约已文档化于类javadoc。
 */
@Fast
public class TestFnd814StringSpanOneWayEquals {

	@Test
	public void testOneWayInteropContract() {
		var span = new StringSpan("file.txt", 5, 3); // "txt"

		// 单向互查（蓄意设计）：span→String成立，String→span不成立
		Assertions.assertTrue(span.equals("txt"));
		Assertions.assertFalse("txt".equals(span));
		// hashCode与String一致：支持作String键容器的查询参数
		Assertions.assertEquals("txt".hashCode(), span.hashCode());

		// 支持的方向：HashMap<String,String>.get(span) 命中String键
		var mimes = new HashMap<String, String>();
		mimes.put("txt", "text/plain");
		Assertions.assertEquals("text/plain", mimes.get(span));

		// 文档明示的陷阱方向：span作key存入后以等值String查询——静默miss
		var set = new HashSet<StringSpan>();
		set.add(span);
		Assertions.assertTrue(set.contains(new StringSpan("a.txt", 2, 3)));
		var stringSet = new HashSet<java.lang.Object>();
		stringSet.add(span);
		Assertions.assertFalse(stringSet.contains("txt"), "反向查询不成立（已文档化，勿把span存入String查询的容器key位）");
	}
}
