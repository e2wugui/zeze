package UnitTest.Zeze.Util;

import Zeze.Util.JsonReader;
import harness.Fast;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * U3-F3 姊妹/U3-F1：skipVar 值后块注释的关闭 '/' 被外层循环二次当作注释起点，
 * 走行注释分支吞掉注释后的同行内容（数组丢元素/映射丢条目甚至 AIOOBE）。
 * 修复：该分支 skipComment 后补一次 pos++。
 */
@Fast
public final class TestJsonSkipVarCommentAfterValue {

	@Test
	public void testArrayElementAfterBlockComment() throws ReflectiveOperationException {
		var list = new JsonReader().buf("[1/*c*/,2]").parseArray(new ArrayList<>());
		assertEquals(List.of(1, 2), list);
		// 注释与后继元素之间无逗号间隔的形式同样收敛到 ',' 分支
		var list2 = new JsonReader().buf("[1/*x*/ ,2]").parseArray(new ArrayList<>());
		assertEquals(List.of(1, 2), list2);
	}

	@Test
	public void testMapEntryAfterBlockComment() throws ReflectiveOperationException {
		var m = new JsonReader().buf("{\"a\":1/*c*/, \"b\":2}").parseMap(new HashMap<>());
		assertEquals(Map.of("a", 1, "b", 2), m);
	}

	/**
	 * 行注释与数组/对象内部的注释分支不回归。
	 */
	@Test
	public void testOtherCommentFormsUnchanged() throws ReflectiveOperationException {
		var m = new JsonReader().buf("{\"a\":1//c\n,\"b\":2}").parseMap(new HashMap<>());
		assertEquals(Map.of("a", 1, "b", 2), m);
		var m2 = new JsonReader().buf("{\"a\":/*c*/1}").parseMap(new HashMap<>());
		assertEquals(Map.of("a", 1), m2);
		var list = new JsonReader().buf("[/*c*/1]").parseArray(new ArrayList<>());
		assertEquals(List.of(1), list);
	}
}
