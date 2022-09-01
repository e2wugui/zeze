package UnitTest.Zeze.Trans;

import junit.framework.TestCase;

public class TestConcurrentDictionary extends TestCase {
	public final void testRemoveInForeach() {
		java.util.concurrent.ConcurrentHashMap<Integer, Integer> cd = new java.util.concurrent.ConcurrentHashMap<>();

		cd.putIfAbsent(1, 1);
		cd.putIfAbsent(2, 2);
		cd.putIfAbsent(3, 3);
		cd.putIfAbsent(4, 4);
		cd.putIfAbsent(5, 5);

		int i = 6;
		for (var e : cd.entrySet()) {
			if (e.getKey() < 3) {
				cd.remove(e.getKey());
				System.out.println("remove key=" + e.getKey());
			} else {
				if (i < 10) {
					cd.putIfAbsent(i, i);
					++i;
				}
				System.out.println("key=" + e.getKey());
			}
		}
	}
}
