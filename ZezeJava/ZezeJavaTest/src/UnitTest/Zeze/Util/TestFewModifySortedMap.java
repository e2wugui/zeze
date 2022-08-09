package UnitTest.Zeze.Util;

import Zeze.Util.FewModifySortedMap;
import junit.framework.TestCase;

public class TestFewModifySortedMap extends TestCase {
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
	}

	private void print(FewModifySortedMap<String, String> sortedMap, String key) {
		System.out.println("key=" + key);
		var head = sortedMap.headMap(key);
		System.out.println("head:");
		System.out.println(head);
		var tail = sortedMap.tailMap(key);
		System.out.println("tail");
		System.out.println(tail);
	}
}
