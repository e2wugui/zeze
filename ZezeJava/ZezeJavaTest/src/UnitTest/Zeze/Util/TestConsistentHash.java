package UnitTest.Zeze.Util;

import Zeze.Util.ConsistentHash;
import net.jodah.failsafe.internal.util.Assert;
import org.junit.Test;

public class TestConsistentHash {
	@Test
	public void testConsistentHash() {
		var consistentHash = new ConsistentHash<Integer>();

		Assert.isTrue(consistentHash.get(Integer.hashCode(1)) == null, "empty nodes");

		consistentHash.add("1", 1);
		consistentHash.add("2", 2);
		consistentHash.add("3", 3);
		consistentHash.add("4", 4);

		System.out.println(consistentHash.get("1".hashCode()));
		System.out.println(consistentHash.get("2".hashCode()));
		System.out.println(consistentHash.get("3".hashCode()));
		System.out.println(consistentHash.get("4".hashCode()));

		consistentHash.remove(1);
		consistentHash.remove(2);
		consistentHash.remove(3);
		consistentHash.remove(4);

		Assert.isTrue(consistentHash.get(Integer.hashCode(1)) == null, "empty nodes");
	}
}
