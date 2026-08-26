package UnitTest.Zeze.Misc;

import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestTreeMap {
	@Test
	public void testDescTail() {
		var tree = new TreeMap<Integer, Integer>();
		for (int i = 0; i < 10; ++i)
			tree.put(i, i);
		var tail7 = tree.tailMap(7);
		Assertions.assertEquals("{7=7, 8=8, 9=9}", tail7.toString());
		System.out.println(tail7);
		var descTail3 = tree.descendingMap().tailMap(3);
		Assertions.assertEquals("{3=3, 2=2, 1=1, 0=0}", descTail3.toString());
		System.out.println();
	}
}
