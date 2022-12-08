package UnitTest.Zeze.Util;

import Zeze.Util.ConsistentHash;
import org.junit.Assert;
import org.junit.Test;

public class TestConsistentHash {
	@Test
	public void testConsistentHash() {
		var consistentHash = new ConsistentHash<Integer>();

		Assert.assertEquals(consistentHash.get(Integer.hashCode(1)), null);

		consistentHash.add("1", 1);
		consistentHash.add("2", 2);
		consistentHash.add("3", 3);
		consistentHash.add("4", 4);

		//*
		System.out.println(consistentHash.get("1".hashCode()));
		System.out.println(consistentHash.get("2".hashCode()));
		System.out.println(consistentHash.get("3".hashCode()));
		System.out.println(consistentHash.get("4".hashCode()));
		/*/
		Assert.assertEquals(consistentHash.get("1".hashCode()), Integer.valueOf(2));
		Assert.assertEquals(consistentHash.get("2".hashCode()), Integer.valueOf(3));
		Assert.assertEquals(consistentHash.get("3".hashCode()), Integer.valueOf(1));
		Assert.assertEquals(consistentHash.get("4".hashCode()), Integer.valueOf(2));
		// */
		consistentHash.remove(1);
		consistentHash.remove(2);
		consistentHash.remove(3);
		consistentHash.remove(4);

		Assert.assertEquals(consistentHash.get(Integer.hashCode(1)), null);
	}
}
