package Zeze.Util;

import java.util.Comparator;
import java.util.TreeMap;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestFewModifySortedMapComparatorCopy {
	private record Key(int order) implements Comparable<Key> {
		@Override
		public int compareTo(Key other) {
			return Integer.compare(order, other.order);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Key;
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}

	@Test
	public void putAllPreservesKeysDistinguishedByTheDestinationComparator() {
		Comparator<Key> comparator = Comparator.reverseOrder();
		var source = new TreeMap<Key, String>(comparator);
		var first = new Key(1);
		var second = new Key(2);
		source.put(first, "one");
		source.put(second, "two");
		var target = new FewModifySortedMap<Key, String>(comparator);

		target.putAll(source);

		assertEquals(2, target.size());
		assertEquals("one", target.get(first));
		assertEquals("two", target.get(second));
		assertEquals("two", target.firstEntry().getValue());
	}
}
