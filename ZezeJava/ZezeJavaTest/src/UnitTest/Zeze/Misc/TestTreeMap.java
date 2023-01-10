package UnitTest.Zeze.Misc;

import java.util.TreeMap;
import org.junit.Test;

public class TestTreeMap {
	@Test
	public void testDescTail() {
		var tree = new TreeMap<Integer, Integer>();
		for (int i = 0; i < 10; ++i)
			tree.put(i, i);
		System.out.println(tree.tailMap(7));
		System.out.println(tree.descendingMap().tailMap(3));
	}
}
