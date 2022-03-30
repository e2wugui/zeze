package UnitTest.Zeze.Trans;

import Zeze.Util.OutObject;
import junit.framework.TestCase;

public class TestConcurrentDictionary extends TestCase {
	
	public final void testRemoveInForeach() {
		java.util.concurrent.ConcurrentHashMap<Integer, Integer> cd = new java.util.concurrent.ConcurrentHashMap<Integer, Integer>();

		cd.putIfAbsent(1, 1);
		cd.putIfAbsent(2, 2);
		cd.putIfAbsent(3, 3);
		cd.putIfAbsent(4, 4);
		cd.putIfAbsent(5, 5);

		int i = 6;
		for (var e : cd.entrySet()) {
			if (e.getKey() < 3) {
				Integer v = null;
				OutObject<Integer> tempOut__ = new OutObject<Integer>();
				cd.remove(e.getKey(), tempOut__);
				v = tempOut__.Value;
				System.out.println("remove key=" + e.getKey());
			}
			else {
				if (i < 10) {
					cd.putIfAbsent(i, i);
					++i;
				}
				System.out.println("key=" + e.getKey());
			}
		}
	}
}