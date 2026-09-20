package UnitTest.Zeze.Util;
import harness.Fast;
import org.junit.jupiter.api.Test;

import Zeze.Util.FewModifySortedMap;

@Fast
public class TestFewModifySortedMap {
	@Test
	public void test1() {
		var sortedMap = new FewModifySortedMap<String, String>();
		sortedMap.putIfAbsent("/", "");
		sortedMap.putIfAbsent("/a/b", "");
		sortedMap.putIfAbsent("/a/b/c/d", "");

		print(sortedMap, "/");
		print(sortedMap, "/a/b");
		print(sortedMap, "/a/b/c");
		print(sortedMap, "/a/b/c/d");
		print(sortedMap, "/a/b/c/d/e");

		print(sortedMap, "/e");

		// 原先纯打印零断言（2026-09-20审核）：headMap/tailMap边界语义的最小守护。
		// 实测语义为子树形：tailMap(key)=key的后代（含相等），headMap(key)=key的严格祖先。
		org.junit.jupiter.api.Assertions.assertEquals(0, sortedMap.headMap("/").size(), "headMap('/')必空");
		org.junit.jupiter.api.Assertions.assertEquals(3, sortedMap.tailMap("/").size(), "tailMap('/')含全部后代");
		org.junit.jupiter.api.Assertions.assertEquals(1, sortedMap.headMap("/a/b").size(), "'/'是'/a/b'的唯一祖先");
		org.junit.jupiter.api.Assertions.assertEquals(1, sortedMap.tailMap("/a/b/c").size(), "'/a/b/c/d'是其后代");
		org.junit.jupiter.api.Assertions.assertEquals(0, sortedMap.tailMap("/e").size(), "'/e'不存在时子树为空");
	}

	private static void print(FewModifySortedMap<String, String> sortedMap, String key) {
		System.out.println("key=" + key);
		var head = sortedMap.headMap(key);
		System.out.println("head:");
		System.out.println(head);
		var tail = sortedMap.tailMap(key);
		System.out.println("tail");
		System.out.println(tail);
	}
}
