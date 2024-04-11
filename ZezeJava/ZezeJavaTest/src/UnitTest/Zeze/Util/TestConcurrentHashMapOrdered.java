package UnitTest.Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import Zeze.Util.ConcurrentHashMapOrdered;
import org.junit.Assert;
import org.junit.Test;

public class TestConcurrentHashMapOrdered {
	@Test
	public void testConcurrentHashMapOrdered() {
		var orderedMap = new ConcurrentHashMapOrdered<String, String>();
		var keys = List.of("3", "2", "1");
		for (var key : keys)
			orderedMap.put(key, key);
		//orderedMap.dumpMap();
		var foreachKeys = new ArrayList<String>();
		orderedMap.foreach((key, value) -> foreachKeys.add(key));
		var itKeys = new ArrayList<String>();
		for (var it = orderedMap.iterator(); it.hasNext(); ) {
			it.next();
			itKeys.add(it.key());
		}
		Assert.assertEquals(foreachKeys, keys);
		Assert.assertEquals(itKeys, keys);
	}
}
